-- عملیات تراکنشی Native برای مدیریت آزمون
-- مطابق schema زنده پروژه eazwuyrymsvdkwckdpco

begin;

create or replace function public.native_delete_exam(p_exam text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_answers integer := 0;
begin
    if v_uid is null then
        return jsonb_build_object('error', 'ابتدا وارد شوید');
    end if;
    if not exists (
        select 1 from public.exams
        where id = p_exam and teacher_id = v_uid
    ) then
        return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید');
    end if;

    select count(*) into v_answers
    from public.answers
    where exam_id = p_exam;

    delete from public.answer_drafts where exam_id = p_exam;
    delete from public.answers_trash where exam_id = p_exam;
    delete from public.answers where exam_id = p_exam;
    delete from public.exam_attempts where exam_id = p_exam;
    delete from public.exam_overrides where exam_id = p_exam;
    delete from public.exam_audience_classes where exam_id = p_exam;
    delete from public.exam_audience_students where exam_id = p_exam;
    delete from public.exam_classes where exam_id = p_exam;
    delete from public.exam_students where exam_id = p_exam;
    delete from public.exam_sessions where exam_id = p_exam;
    delete from public.exam_keys where exam_id = p_exam;
    delete from public.exams where id = p_exam and teacher_id = v_uid;

    return jsonb_build_object('ok', true, 'deleted_answers', v_answers);
end;
$$;

revoke all on function public.native_delete_exam(text) from public, anon;
grant execute on function public.native_delete_exam(text) to authenticated;

create or replace function public.native_duplicate_exam(p_exam text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_source public.exams%rowtype;
    v_new_id text := gen_random_uuid()::text;
    v_code text;
    v_try integer := 0;
begin
    if v_uid is null then
        return jsonb_build_object('error', 'ابتدا وارد شوید');
    end if;

    select * into v_source
    from public.exams
    where id = p_exam and teacher_id = v_uid
    limit 1;
    if not found then
        return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید');
    end if;

    loop
        v_try := v_try + 1;
        v_code := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));
        exit when not exists (select 1 from public.exams where code = v_code);
        if v_try >= 20 then
            raise exception 'ساخت کد یکتا ناموفق بود';
        end if;
    end loop;

    insert into public.exams (
        id, title, subject, questions, code, teacher, total_score, is_open,
        duration, teacher_login, teacher_id, shuffle_q, shuffle_opt, neg_marking,
        opens_at, closes_at, class_id, audience, teacher_message,
        attempts_allowed, attempt_on_timeout, grade_policy, attempt_cooldown
    ) values (
        v_new_id,
        coalesce(v_source.title, '') || ' (کپی)',
        v_source.subject,
        v_source.questions,
        v_code,
        v_source.teacher,
        v_source.total_score,
        false,
        v_source.duration,
        v_source.teacher_login,
        v_uid,
        v_source.shuffle_q,
        v_source.shuffle_opt,
        v_source.neg_marking,
        null,
        null,
        null,
        'all',
        v_source.teacher_message,
        v_source.attempts_allowed,
        v_source.attempt_on_timeout,
        v_source.grade_policy,
        v_source.attempt_cooldown
    );

    insert into public.exam_keys (exam_id, answers)
    select v_new_id, answers
    from public.exam_keys
    where exam_id = p_exam;

    return jsonb_build_object('ok', true, 'id', v_new_id, 'code', v_code);
end;
$$;

revoke all on function public.native_duplicate_exam(text) from public, anon;
grant execute on function public.native_duplicate_exam(text) to authenticated;

commit;

notify pgrst, 'reload schema';
