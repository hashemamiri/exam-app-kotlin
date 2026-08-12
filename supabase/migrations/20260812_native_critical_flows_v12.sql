-- V12 critical Native flows: teacher account completion, owner-only answer review.
-- Run only after 20260812_native_final_hardening.sql (V11).
-- No password, token, service key or plaintext credential is stored by this migration.

begin;

-- ============================================================
-- 1) Complete public teacher registration only after verified Auth OTP.
--    Managed student accounts use @student.exam.local and cannot self-promote.
-- ============================================================
create or replace function public.native_complete_teacher_registration_v1(
    p_full_name text,
    p_username text
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_email text;
    v_name text := left(btrim(coalesce(p_full_name, '')), 200);
    v_username text := lower(btrim(coalesce(p_username, '')));
    v_profile public.profiles%rowtype;
begin
    if v_uid is null then
        return jsonb_build_object('error', 'ابتدا کد ایمیل را تأیید کنید');
    end if;
    if v_name = '' then
        return jsonb_build_object('error', 'نام و نام خانوادگی را وارد کنید');
    end if;
    if v_username !~ '^[a-z0-9_]{4,20}$' then
        return jsonb_build_object('error', 'نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد');
    end if;

    select lower(coalesce(u.email, '')) into v_email
    from auth.users u
    where u.id = v_uid;
    if coalesce(v_email, '') = '' then
        return jsonb_build_object('error', 'ایمیل تأییدشده پیدا نشد');
    end if;
    if v_email like '%@student.exam.local' then
        return jsonb_build_object('error', 'حساب دانش‌آموز توسط معلم مدیریت می‌شود');
    end if;

    perform pg_advisory_xact_lock(hashtext('native-teacher-username:' || v_username));
    if exists (
        select 1 from public.profiles p
        where lower(coalesce(p.username, '')) = v_username
          and p.id <> v_uid
    ) then
        return jsonb_build_object('error', 'این نام کاربری قبلاً استفاده شده است');
    end if;

    insert into public.profiles(id, full_name, display_name, username, role)
    values (v_uid, v_name, v_name, v_username, 'student')
    on conflict (id) do nothing;

    select * into v_profile
    from public.profiles p
    where p.id = v_uid
    for update;

    if v_profile.id is null then
        return jsonb_build_object('error', 'پروفایل حساب پیدا نشد');
    end if;
    if v_profile.role = 'student' and v_profile.teacher_id is not null then
        return jsonb_build_object('error', 'حساب دانش‌آموز نمی‌تواند به حساب معلم تبدیل شود');
    end if;
    if v_profile.role not in ('student', 'teacher') then
        return jsonb_build_object('error', 'نقش حساب معتبر نیست');
    end if;
    if exists (
        select 1 from public.class_members m where m.student_id = v_uid
    ) then
        return jsonb_build_object('error', 'حساب عضو کلاس نمی‌تواند به حساب معلم تبدیل شود');
    end if;

    update public.profiles
    set full_name = v_name,
        display_name = coalesce(nullif(display_name, ''), v_name),
        username = v_username,
        role = 'teacher',
        teacher_id = null
    where id = v_uid;

    return jsonb_build_object(
        'ok', true,
        'id', v_uid,
        'full_name', v_name,
        'username', v_username,
        'role', 'teacher'
    );
end;
$$;

-- Teacher username is profile metadata; email remains the Auth login identity.
create or replace function public.native_update_my_username_v1(p_username text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_username text := lower(btrim(coalesce(p_username, '')));
    v_count integer;
begin
    if v_uid is null then
        return jsonb_build_object('error', 'ابتدا وارد شوید');
    end if;
    if v_username !~ '^[a-z0-9_]{4,20}$' then
        return jsonb_build_object('error', 'نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد');
    end if;

    perform pg_advisory_xact_lock(hashtext('native-teacher-username:' || v_username));
    if exists (
        select 1 from public.profiles p
        where lower(coalesce(p.username, '')) = v_username
          and p.id <> v_uid
    ) then
        return jsonb_build_object('error', 'این نام کاربری قبلاً استفاده شده است');
    end if;

    update public.profiles
    set username = v_username
    where id = v_uid
      and role = 'teacher'
      and teacher_id is null;
    get diagnostics v_count = row_count;
    if v_count <> 1 then
        return jsonb_build_object('error', 'تغییر نام کاربری فقط برای حساب معلم مجاز است');
    end if;
    return jsonb_build_object('ok', true, 'username', v_username);
end;
$$;

-- Detect an OTP-verified real-email account whose teacher setup was interrupted.
create or replace function public.native_my_registration_state_v1()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select coalesce((
        select jsonb_build_object(
            'ok', true,
            'role', p.role,
            'username', p.username,
            'full_name', p.full_name,
            'requires_teacher_setup',
                p.role = 'student'
                and p.teacher_id is null
                and lower(coalesce(u.email, '')) not like '%@student.exam.local'
        )
        from public.profiles p
        join auth.users u on u.id = p.id
        where p.id = auth.uid()
    ), jsonb_build_object('error', 'پروفایل حساب پیدا نشد'));
$$;

-- ============================================================
-- 2) Student-owned answer list and detail.
--    Answer keys are merged only after the selected answer is graded.
-- ============================================================
create or replace function public.native_my_answers_v1()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select jsonb_build_object(
        'ok', true,
        'items', coalesce(jsonb_agg(
            jsonb_build_object(
                'id', a.id::text,
                'exam_id', a.exam_id,
                'title', coalesce(e.title, 'آزمون'),
                'subject', coalesce(e.subject, ''),
                'submitted_at', a.submitted_at,
                'graded', coalesce(a.graded, false),
                'total_grade', coalesce(a.total_grade, 0),
                'total_score', coalesce(e.total_score, 0),
                'feedback', coalesce(a.feedback, '')
            ) order by a.submitted_at desc
        ), '[]'::jsonb)
    )
    from public.answers a
    join public.exams e on e.id = a.exam_id
    where auth.uid() is not null
      and a.student_id = auth.uid();
$$;

create or replace function public.native_my_answer_detail_v1(p_answer text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_answer public.answers%rowtype;
    v_exam public.exams%rowtype;
    v_keys jsonb := '[]'::jsonb;
    v_questions jsonb := '[]'::jsonb;
    v_question jsonb;
    v_key jsonb;
    v_index integer;
begin
    if v_uid is null then
        return jsonb_build_object('error', 'ابتدا وارد شوید');
    end if;

    select * into v_answer
    from public.answers a
    where a.id::text = btrim(coalesce(p_answer, ''))
      and a.student_id = v_uid;
    if v_answer.id is null then
        return jsonb_build_object('error', 'پاسخ پیدا نشد یا متعلق به این حساب نیست');
    end if;

    select * into v_exam from public.exams e where e.id = v_answer.exam_id;
    if v_exam.id is null then
        return jsonb_build_object('error', 'آزمون پاسخ پیدا نشد');
    end if;

    if coalesce(v_answer.graded, false) then
        select coalesce(k.answers, '[]'::jsonb) into v_keys
        from public.exam_keys k
        where k.exam_id = v_exam.id;
        v_keys := coalesce(v_keys, '[]'::jsonb);
    end if;

    if jsonb_typeof(coalesce(v_exam.questions, '[]'::jsonb)) = 'array' then
        for v_index in 0 .. jsonb_array_length(coalesce(v_exam.questions, '[]'::jsonb)) - 1 loop
            v_question := coalesce(v_exam.questions -> v_index, '{}'::jsonb)
                - 'correctOption' - 'correctAnswer' - 'accept' - 'answer'
                - 'tolerance' - 'matchAnswer' - 'explanation';
            v_key := '{}'::jsonb;
            if coalesce(v_answer.graded, false) then
                select coalesce(item, '{}'::jsonb) into v_key
                from jsonb_array_elements(v_keys) with ordinality as key_row(item, ord)
                where coalesce((item->>'i')::integer, (ord - 1)::integer) = v_index
                order by ord
                limit 1;
                v_key := coalesce(v_key, '{}'::jsonb) - 'i';
            end if;
            v_questions := v_questions || jsonb_build_array(v_question || v_key);
        end loop;
    end if;

    return jsonb_build_object(
        'ok', true,
        'id', v_answer.id::text,
        'exam_id', v_exam.id,
        'title', coalesce(v_exam.title, 'آزمون'),
        'subject', coalesce(v_exam.subject, ''),
        'graded', coalesce(v_answer.graded, false),
        'total_grade', coalesce(v_answer.total_grade, 0),
        'total_score', coalesce(v_exam.total_score, 0),
        'feedback', coalesce(v_answer.feedback, ''),
        'submitted_at', v_answer.submitted_at,
        'questions', v_questions,
        'responses', coalesce(v_answer.responses, '[]'::jsonb),
        'response_images', coalesce(v_answer.response_images, '{}'::jsonb),
        'grades', coalesce(v_answer.grades, '[]'::jsonb)
    );
end;
$$;

-- Explicit grants preserve the V11 function allowlist model.
revoke all on function public.native_complete_teacher_registration_v1(text,text) from public, anon, authenticated;
revoke all on function public.native_update_my_username_v1(text) from public, anon, authenticated;
revoke all on function public.native_my_registration_state_v1() from public, anon, authenticated;
revoke all on function public.native_my_answers_v1() from public, anon, authenticated;
revoke all on function public.native_my_answer_detail_v1(text) from public, anon, authenticated;
grant execute on function public.native_complete_teacher_registration_v1(text,text) to authenticated;
grant execute on function public.native_update_my_username_v1(text) to authenticated;
grant execute on function public.native_my_registration_state_v1() to authenticated;
grant execute on function public.native_my_answers_v1() to authenticated;
grant execute on function public.native_my_answer_detail_v1(text) to authenticated;

commit;
