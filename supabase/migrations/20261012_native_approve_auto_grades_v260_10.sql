-- V260.10 — approve_auto_grades: در آزمون‌های دارای سؤال تشریحی، پاسخ‌هایی که نسخهٔ قدیمی (قبل از V260.7)
-- به‌اشتباه «تصحیح‌شده با نمرهٔ صفر» کرده بود، با زدن «تأیید نمره‌های خودکار» به «در انتظار تصحیح» با نمرهٔ
-- پیش‌پرشدهٔ سؤال‌های عینی برمی‌گردند. (قبلاً فیلتر graded=false آن‌ها را رد می‌کرد و میانگین گزارش‌ها ۴.۶٪ می‌شد.)
begin;
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
            -- V260.10: پاسخ‌هایی که تابع قدیمی به‌اشتباه graded=true/auto_graded=true و صفر کرده بود هم
            -- به حالت «در انتظار تصحیح» با نمرهٔ پیش‌پرشدهٔ سؤال‌های عینی برمی‌گردند (نمرهٔ دستی معلم auto_graded=false است و دست نمی‌خورد)
            update public.answers
            set grades = v_grades, total_grade = v_total, graded = false, auto_graded = true, graded_at = null
            where id = v_ans.id and exam_id = v_exam.id
              and (coalesce(graded, false) = false or coalesce(auto_graded, false) = true);
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
revoke all on function public.approve_auto_grades(text, text) from public, anon;
grant execute on function public.approve_auto_grades(text, text) to authenticated;
commit;
