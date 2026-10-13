-- V270 — تصحیح گروهی:
-- ۱) «نهایی‌سازی نمره‌های گروهی»: سؤال‌های عینیِ بدون نمرهٔ ذخیره‌شده، خودکار (با کلید) نمره می‌گیرند؛
--    فقط سؤال‌های تشریحیِ بدون نمره مانع می‌شوند و پیام خطا شمارهٔ آن سؤال‌ها را می‌گوید.
-- ۲) «تأیید نمره‌های خودکار» در آزمونِ دارای تشریحی: نمرهٔ سؤال‌های عینی را «ذخیره‌شده» ثبت می‌کند
--    (native_grade_progress) تا بعد از تصحیح تشریحی‌ها، نهایی‌سازی بدون مانع انجام شود.
begin;

create or replace function public.native_finalize_bulk_grades_v1(p_exam text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam record;
    v_keys jsonb;
    v_n integer;
    v_i integer;
    v_q jsonb;
    v_key jsonb;
    v_ans record;
    v_grades jsonb;
    v_g numeric;
    v_total numeric;
    v_resp jsonb;
    v_missing_q integer[] := '{}';
    v_missing_answers integer := 0;
    v_answer_incomplete boolean;
    v_updated integer;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select * into v_exam from public.exams e where e.id = p_exam and e.teacher_id = v_uid limit 1;
    if v_exam.id is null then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;
    if jsonb_typeof(coalesce(v_exam.questions, 'null'::jsonb)) <> 'array' then
        return jsonb_build_object('error', 'ساختار سؤال‌های آزمون نامعتبر است');
    end if;
    v_n := jsonb_array_length(v_exam.questions);

    select coalesce(k.answers, '[]'::jsonb) into v_keys from public.exam_keys k where k.exam_id = v_exam.id limit 1;
    v_keys := coalesce(v_keys, '[]'::jsonb);
    if jsonb_typeof(v_keys) <> 'array' then v_keys := '[]'::jsonb; end if;

    for v_ans in
        select a.id, a.responses, a.grades
        from public.answers a
        where a.exam_id = v_exam.id and not coalesce(a.graded, false)
        for update
    loop
        v_answer_incomplete := false;
        v_grades := case when jsonb_typeof(coalesce(v_ans.grades, 'null'::jsonb)) = 'array' then v_ans.grades else '[]'::jsonb end;
        -- آرایهٔ نمره را به طول تعداد سؤال‌ها می‌رسانیم
        while jsonb_array_length(v_grades) < v_n loop v_grades := v_grades || '0'::jsonb; end loop;

        for v_i in 0 .. v_n - 1 loop
            if exists (select 1 from public.native_grade_progress p where p.answer_id = v_ans.id and p.question_index = v_i) then
                continue;
            end if;
            v_q := coalesce(v_exam.questions -> v_i, '{}'::jsonb);
            select coalesce(item, '{}'::jsonb) into v_key
            from jsonb_array_elements(v_keys) with ordinality as key_row(item, ord)
            where coalesce((item ->> 'i')::integer, (ord - 1)::integer) = v_i
            order by ord limit 1;
            v_key := coalesce(v_key, '{}'::jsonb);
            v_resp := case when jsonb_typeof(coalesce(v_ans.responses, 'null'::jsonb)) = 'array' then v_ans.responses -> v_i else null end;
            v_g := public.native_auto_grade_question_v1(v_q ->> 'type', coalesce(nullif(v_q ->> 'score', '')::numeric, 0), v_key, v_resp);
            if v_g is null then
                -- تشریحی (یا بدون کلید) و بدون نمرهٔ ذخیره‌شده → مانع نهایی‌سازی
                v_answer_incomplete := true;
                if not (v_i + 1 = any (v_missing_q)) then v_missing_q := v_missing_q || (v_i + 1); end if;
            else
                v_grades := jsonb_set(v_grades, array[v_i::text], to_jsonb(v_g), true);
                insert into public.native_grade_progress(answer_id, question_index, teacher_id, updated_at)
                values (v_ans.id, v_i, v_uid, now())
                on conflict (answer_id, question_index) do nothing;
            end if;
        end loop;

        if v_answer_incomplete then
            v_missing_answers := v_missing_answers + 1;
        else
            select coalesce(sum(value::numeric), 0) into v_total from jsonb_array_elements_text(v_grades) value;
            update public.answers
            set grades = v_grades, total_grade = v_total
            where id = v_ans.id and exam_id = v_exam.id;
        end if;
    end loop;

    if v_missing_answers > 0 then
        return jsonb_build_object(
            'error', 'سؤال تشریحی ' || array_to_string(v_missing_q, '، ') || ' هنوز برای ' || v_missing_answers || ' پاسخ نمره ندارد؛ در تب «تصحیح سؤال‌به‌سؤال» نمره بدهید و ذخیره کنید.',
            'incomplete_answers', v_missing_answers,
            'missing_questions', to_jsonb(v_missing_q)
        );
    end if;

    update public.answers
    set graded = true, auto_graded = false, graded_at = now()::text
    where exam_id = v_exam.id and not coalesce(graded, false);
    get diagnostics v_updated = row_count;
    return jsonb_build_object('ok', true, 'finalized', v_updated);
end;
$$;
revoke all on function public.native_finalize_bulk_grades_v1(text) from public, anon;
grant execute on function public.native_finalize_bulk_grades_v1(text) to authenticated;

create or replace function public.approve_auto_grades(p_exam text, p_mode text default 'auto_only')
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam record;
    v_keys jsonb;
    v_q jsonb;
    v_key jsonb;
    v_n integer;
    v_i integer;
    v_has_essay boolean := false;
    v_ans record;
    v_grades jsonb;
    v_old jsonb;
    v_g numeric;
    v_total numeric;
    v_count integer := 0;
    v_prefilled integer := 0;
    v_resp jsonb;
    v_objective integer[] := '{}';
    v_essay_q integer[] := '{}';
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;

    select * into v_exam from public.exams e where e.id = p_exam and e.teacher_id = v_uid limit 1;
    if v_exam.id is null then return jsonb_build_object('error', 'آزمون پیدا نشد یا متعلق به این حساب نیست'); end if;
    if jsonb_typeof(coalesce(v_exam.questions, 'null'::jsonb)) <> 'array' then
        return jsonb_build_object('error', 'ساختار سؤال‌های آزمون نامعتبر است');
    end if;
    v_n := jsonb_array_length(v_exam.questions);

    select coalesce(k.answers, '[]'::jsonb) into v_keys from public.exam_keys k where k.exam_id = v_exam.id limit 1;
    v_keys := coalesce(v_keys, '[]'::jsonb);
    if jsonb_typeof(v_keys) <> 'array' then v_keys := '[]'::jsonb; end if;

    for v_i in 0 .. v_n - 1 loop
        if lower(coalesce(v_exam.questions -> v_i ->> 'type', '')) in ('essay', 'long', 'descriptive') then
            v_has_essay := true; v_essay_q := v_essay_q || (v_i + 1);
        end if;
    end loop;

    for v_ans in
        select a.id, a.responses, a.grades, a.graded, a.auto_graded
        from public.answers a
        where a.exam_id = v_exam.id
          and (coalesce(a.graded, false) = false or coalesce(a.auto_graded, false) = true)
    loop
        v_grades := '[]'::jsonb;
        v_total := 0;
        v_objective := '{}';
        v_old := case when jsonb_typeof(coalesce(v_ans.grades, 'null'::jsonb)) = 'array' then v_ans.grades else '[]'::jsonb end;
        for v_i in 0 .. v_n - 1 loop
            v_q := coalesce(v_exam.questions -> v_i, '{}'::jsonb);
            select coalesce(item, '{}'::jsonb) into v_key
            from jsonb_array_elements(v_keys) with ordinality as key_row(item, ord)
            where coalesce((item ->> 'i')::integer, (ord - 1)::integer) = v_i
            order by ord limit 1;
            v_key := coalesce(v_key, '{}'::jsonb);
            v_resp := case when jsonb_typeof(coalesce(v_ans.responses, 'null'::jsonb)) = 'array' then v_ans.responses -> v_i else null end;
            v_g := public.native_auto_grade_question_v1(v_q ->> 'type', coalesce(nullif(v_q ->> 'score', '')::numeric, 0), v_key, v_resp);
            if v_g is null then
                begin v_g := coalesce(nullif(v_old ->> v_i, '')::numeric, 0); exception when others then v_g := 0; end;
            else
                v_objective := v_objective || v_i;
            end if;
            v_grades := v_grades || to_jsonb(v_g);
            v_total := v_total + v_g;
        end loop;

        if v_has_essay and coalesce(p_mode, 'auto_only') = 'auto_only' then
            update public.answers
            set grades = v_grades, total_grade = v_total, graded = false, auto_graded = true, graded_at = null
            where id = v_ans.id and exam_id = v_exam.id
              and (coalesce(graded, false) = false or coalesce(auto_graded, false) = true);
            if found then
                -- V270: نمرهٔ سؤال‌های عینی «ذخیره‌شده» ثبت می‌شود تا فقط تشریحی‌ها بمانند
                insert into public.native_grade_progress(answer_id, question_index, teacher_id, updated_at)
                select v_ans.id, q, v_uid, now() from unnest(v_objective) as q
                on conflict (answer_id, question_index) do update set updated_at = excluded.updated_at;
                v_prefilled := v_prefilled + 1;
            end if;
        else
            update public.answers
            set grades = v_grades, total_grade = v_total, graded = true, auto_graded = true, graded_at = now()::text
            where id = v_ans.id and exam_id = v_exam.id;
            v_count := v_count + 1;
        end if;
    end loop;

    return jsonb_build_object('ok', true, 'count', v_count, 'prefilled', v_prefilled, 'has_essay', v_has_essay,
        'essay_questions', to_jsonb(v_essay_q));
end;
$$;
revoke all on function public.approve_auto_grades(text, text) from public, anon;
grant execute on function public.approve_auto_grades(text, text) to authenticated;
commit;
