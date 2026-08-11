-- ثبت امن نمره دستی Native بر اساس schema زنده

begin;

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
        graded_at = now()::text
    where id = p_answer
      and exam_id = v_exam_id;
    get diagnostics v_count = row_count;

    if v_count <> 1 then return jsonb_build_object('error', 'ثبت نمره انجام نشد'); end if;
    return jsonb_build_object('ok', true, 'total', v_total);
end;
$$;

revoke all on function public.native_save_grade(text, jsonb, text) from public, anon;
grant execute on function public.native_save_grade(text, jsonb, text) to authenticated;

commit;

notify pgrst, 'reload schema';
