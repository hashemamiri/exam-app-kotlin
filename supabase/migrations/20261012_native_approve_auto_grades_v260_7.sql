-- V260.7 — تصحیح خودکار واقعی روی سرور
-- مشکل: تابع قدیمی approve_auto_grades کلید پاسخ (exam_keys) را نمی‌خواند و فقط آرایهٔ صفرها را
-- «تأیید» می‌کرد؛ نتیجه: نمرهٔ همهٔ دانش‌آموزان ۰ (آزمایش ۱۴۰۵/۰۷/۲۰ با ۷۲ پاسخ).
-- این مهاجرت تابع را با همان امضا و همان قواعد سایت/اپ (autoScore) بازنویسی می‌کند:
--   چندگزینه‌ای = correctOption، صحیح/غلط = correctAnswer، جای‌خالی = accept (+caseSensitive)،
--   عددی = answer ± tolerance، جورکردنی = matchAnswer با نمرهٔ جزئی، تشریحی = دست‌نخورده.
-- حالت auto_only: فقط آزمون‌هایی که همهٔ سؤال‌هایشان عینی است «تأیید» می‌شوند؛ در آزمون‌های
-- دارای سؤال تشریحی فقط نمرهٔ سؤال‌های عینی پیش‌پر می‌شود و graded=false می‌ماند.
-- پاسخ‌هایی که قبلاً با تابع قدیمی به‌اشتباه auto_graded=true و صفر شده‌اند دوباره نمره می‌گیرند؛
-- نمره‌هایی که معلم دستی ذخیره کرده (auto_graded=false و graded=true) دست نمی‌خورند.
begin;

do $drop$
declare r record;
begin
    for r in
        select p.oid::regprocedure as sig
        from pg_proc p join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'public' and p.proname = 'approve_auto_grades'
    loop
        execute format('drop function %s', r.sig);
    end loop;
end
$drop$;

create or replace function public.native_normalize_text_v1(p text)
returns text
language sql
immutable
as $$
    select lower(trim(translate(coalesce(p, ''),
        '۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩يكۀ٫،',
        '01234567890123456789یکه.,')));
$$;

create or replace function public.native_auto_grade_question_v1(p_type text, p_score numeric, p_key jsonb, p_resp jsonb)
returns numeric
language plpgsql
immutable
as $$
declare
    v_type text := lower(coalesce(p_type, ''));
    v_resp_text text;
    v_num numeric;
    v_ans numeric;
    v_tol numeric;
    v_ok integer := 0;
    v_n integer := 0;
    v_k text;
    v_acc jsonb;
    v_item jsonb;
    v_cs boolean;
begin
    if v_type in ('essay', 'long', 'descriptive') then return null; end if;
    if p_resp is null or jsonb_typeof(p_resp) = 'null' or (jsonb_typeof(p_resp) = 'string' and trim(p_resp #>> '{}') = '') then
        return 0;
    end if;
    v_resp_text := p_resp #>> '{}';

    if v_type = 'multiple' then
        if p_key ? 'correctOption' then
            begin
                return case when (v_resp_text)::numeric = (p_key ->> 'correctOption')::numeric then p_score else 0 end;
            exception when others then return 0; end;
        end if;
        return null;
    elsif v_type = 'truefalse' then
        if p_key ? 'correctAnswer' then
            return case when (lower(v_resp_text) in ('true', 't', '1')) = (lower(coalesce(p_key ->> 'correctAnswer', '')) in ('true', 't', '1')) then p_score else 0 end;
        end if;
        return null;
    elsif v_type = 'fill' then
        v_acc := coalesce(p_key -> 'accept', '[]'::jsonb);
        if jsonb_typeof(v_acc) <> 'array' or jsonb_array_length(v_acc) = 0 then return null; end if;
        v_cs := coalesce((p_key ->> 'caseSensitive')::boolean, false);
        for v_item in select value from jsonb_array_elements(v_acc) loop
            if v_cs then
                if trim(v_item #>> '{}') = trim(v_resp_text) then return p_score; end if;
            else
                if public.native_normalize_text_v1(v_item #>> '{}') = public.native_normalize_text_v1(v_resp_text) then return p_score; end if;
            end if;
        end loop;
        return 0;
    elsif v_type = 'numeric' then
        if not (p_key ? 'answer') or jsonb_typeof(p_key -> 'answer') = 'null' then return null; end if;
        begin
            v_ans := (p_key ->> 'answer')::numeric;
            v_tol := coalesce(nullif(p_key ->> 'tolerance', '')::numeric, 0);
            v_num := nullif(regexp_replace(public.native_normalize_text_v1(v_resp_text), '[^0-9.\-]', '', 'g'), '')::numeric;
        exception when others then
            return 0;
        end;
        if v_num is null then return 0; end if;
        return case when abs(v_num - v_ans) <= abs(v_tol) then p_score else 0 end;
    elsif v_type = 'matching' then
        if jsonb_typeof(coalesce(p_key -> 'matchAnswer', 'null'::jsonb)) <> 'object' then return null; end if;
        if jsonb_typeof(p_resp) <> 'object' then return 0; end if;
        for v_k in select key from jsonb_object_keys(p_key -> 'matchAnswer') as t(key) loop
            v_n := v_n + 1;
            begin
                if (p_resp ->> v_k) is not null and (p_resp ->> v_k)::numeric = (p_key -> 'matchAnswer' ->> v_k)::numeric then v_ok := v_ok + 1; end if;
            exception when others then null; end;
        end loop;
        if v_n = 0 then return null; end if;
        return round(p_score * v_ok / v_n, 2);
    end if;
    return null;
end;
$$;

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
        if lower(coalesce(v_exam.questions -> v_i ->> 'type', '')) in ('essay', 'long', 'descriptive') then v_has_essay := true; end if;
    end loop;

    for v_ans in
        select a.id, a.responses, a.grades, a.graded, a.auto_graded
        from public.answers a
        where a.exam_id = v_exam.id
          and (coalesce(a.graded, false) = false or coalesce(a.auto_graded, false) = true)
    loop
        v_grades := '[]'::jsonb;
        v_total := 0;
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
                -- تشریحی یا بدون کلید: نمرهٔ قبلی (اگر بود) حفظ می‌شود
                begin v_g := coalesce(nullif(v_old ->> v_i, '')::numeric, 0); exception when others then v_g := 0; end;
            end if;
            v_grades := v_grades || to_jsonb(v_g);
            v_total := v_total + v_g;
        end loop;

        if v_has_essay and coalesce(p_mode, 'auto_only') = 'auto_only' then
            -- فقط پیش‌پر کردن نمرهٔ سؤال‌های عینی؛ تأیید نهایی با معلم
            update public.answers
            set grades = v_grades, total_grade = v_total
            where id = v_ans.id and exam_id = v_exam.id and coalesce(graded, false) = false;
            v_prefilled := v_prefilled + 1;
        else
            update public.answers
            set grades = v_grades, total_grade = v_total, graded = true, auto_graded = true, graded_at = now()::text
            where id = v_ans.id and exam_id = v_exam.id;
            v_count := v_count + 1;
        end if;
    end loop;

    return jsonb_build_object('ok', true, 'count', v_count, 'prefilled', v_prefilled, 'has_essay', v_has_essay);
end;
$$;

-- نمرهٔ دستی معلم را از تأیید خودکارِ بعدی مصون می‌کند
create or replace function public.native_save_grade(
    p_answer text,
    p_grades jsonb,
    p_feedback text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam_id text;
    v_questions jsonb;
    v_total numeric := 0;
    v_value numeric;
    v_max numeric;
    v_index integer;
    v_count integer;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if jsonb_typeof(p_grades) <> 'array' then return jsonb_build_object('error', 'ساختار نمره نامعتبر است'); end if;

    select a.exam_id, e.questions
    into v_exam_id, v_questions
    from public.answers a
    join public.exams e on e.id = a.exam_id
    where a.id = p_answer
      and e.teacher_id = v_uid
    limit 1;

    if v_exam_id is null then return jsonb_build_object('error', 'پاسخ یافت نشد یا دسترسی ندارید'); end if;
    if jsonb_array_length(p_grades) <> jsonb_array_length(coalesce(v_questions, '[]'::jsonb)) then
        return jsonb_build_object('error', 'تعداد نمره‌ها با سؤال‌ها برابر نیست');
    end if;

    for v_index in 0..jsonb_array_length(p_grades) - 1 loop
        begin
            v_value := (p_grades ->> v_index)::numeric;
            v_max := coalesce((v_questions -> v_index ->> 'score')::numeric, 0);
        exception when others then
            return jsonb_build_object('error', 'مقدار نمره نامعتبر است');
        end;
        if v_value < 0 or v_value > v_max then
            return jsonb_build_object('error', format('نمره سؤال %s خارج از بازه است', v_index + 1));
        end if;
        v_total := v_total + v_value;
    end loop;

    update public.answers
    set grades = p_grades,
        total_grade = v_total,
        feedback = left(coalesce(p_feedback, ''), 2000),
        graded = true,
        auto_graded = false, -- V260.7: نمرهٔ دستی معلم با تأیید خودکار بعدی بازنویسی نمی‌شود
        graded_at = now()::text
    where id = p_answer
      and exam_id = v_exam_id;
    get diagnostics v_count = row_count;

    if v_count <> 1 then return jsonb_build_object('error', 'ثبت نمره انجام نشد'); end if;
    return jsonb_build_object('ok', true, 'total', v_total);
end;
$$;

revoke all on function public.approve_auto_grades(text, text) from public, anon;
grant execute on function public.approve_auto_grades(text, text) to authenticated;
revoke all on function public.native_auto_grade_question_v1(text, numeric, jsonb, jsonb) from public, anon;
grant execute on function public.native_auto_grade_question_v1(text, numeric, jsonb, jsonb) to authenticated;
revoke all on function public.native_normalize_text_v1(text) from public, anon;
grant execute on function public.native_normalize_text_v1(text) to authenticated;

commit;

notify pgrst, 'reload schema';
