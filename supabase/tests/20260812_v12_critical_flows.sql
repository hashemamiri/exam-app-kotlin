-- Run after 20260812_native_critical_flows_v12.sql in an isolated database.
create or replace function pg_temp.assert_true(p_ok boolean, p_message text)
returns void language plpgsql as $$ begin
    if not coalesce(p_ok, false) then raise exception 'ASSERT: %', p_message; end if;
end $$;

insert into auth.users(id,email) values
('81000000-0000-0000-0000-000000000001','new.teacher@example.test'),
('81000000-0000-0000-0000-000000000002','student_one@student.exam.local'),
('81000000-0000-0000-0000-000000000003','other.student@student.exam.local'),
('81000000-0000-0000-0000-000000000004','interrupted.teacher@example.test')
on conflict (id) do update set email=excluded.email;
insert into public.profiles(id,full_name,display_name,username,role,teacher_id) values
('81000000-0000-0000-0000-000000000002','دانش‌آموز یک','دانش‌آموز یک','student_one','student','81000000-0000-0000-0000-000000000001'),
('81000000-0000-0000-0000-000000000003','دانش‌آموز دیگر','دانش‌آموز دیگر','other_student','student','81000000-0000-0000-0000-000000000001'),
('81000000-0000-0000-0000-000000000004','معلم نیمه‌تمام','معلم نیمه‌تمام','interrupted_teacher','student',null)
on conflict (id) do update set username=excluded.username,role=excluded.role,teacher_id=excluded.teacher_id;

set role authenticated;
select set_config('request.jwt.claim.sub','81000000-0000-0000-0000-000000000001',false);
select pg_temp.assert_true(
    (public.native_complete_teacher_registration_v1('معلم تازه','new_teacher')->>'ok')::boolean,
    'verified non-student account completes teacher registration'
);
select pg_temp.assert_true(
    (select role='teacher' and username='new_teacher' from public.profiles where id=auth.uid()),
    'teacher profile updated'
);
select pg_temp.assert_true(
    (public.native_update_my_username_v1('new_teacher_2')->>'ok')::boolean,
    'teacher changes own username'
);
reset role;

set role authenticated;
select set_config('request.jwt.claim.sub','81000000-0000-0000-0000-000000000004',false);
select pg_temp.assert_true(
    (public.native_my_registration_state_v1()->>'requires_teacher_setup')::boolean,
    'interrupted real-email teacher setup is recoverable after process restart'
);
reset role;

set role authenticated;
select set_config('request.jwt.claim.sub','81000000-0000-0000-0000-000000000002',false);
select pg_temp.assert_true(
    public.native_complete_teacher_registration_v1('دانش‌آموز یک','cannot_promote') ? 'error',
    'managed synthetic student cannot self-promote'
);
reset role;

insert into public.exams(id,title,subject,questions,total_score) values (
    'critical-exam','آزمون بحرانی','ریاضی',
    '[{"id":"q1","type":"multiple","text":"دو بعلاوه دو؟","score":1,"options":["۳","۴"]},
      {"id":"q2","type":"essay","text":"توضیح دهید","score":2}]'::jsonb,
    3
) on conflict (id) do update set questions=excluded.questions,total_score=excluded.total_score;
insert into public.exam_keys(exam_id,answers) values (
    'critical-exam',
    '[{"i":0,"correctOption":1,"explanation":"چهار"},{"i":1}]'::jsonb
) on conflict (exam_id) do update set answers=excluded.answers;
insert into public.answers(
    id,exam_id,student_id,responses,response_images,grades,total_grade,feedback,graded,submitted_at
) values
('critical-answer-pending','critical-exam','81000000-0000-0000-0000-000000000002','[1,"متن"]','{}','[]',0,'',false,now()),
('critical-answer-graded','critical-exam','81000000-0000-0000-0000-000000000002','[1,"متن"]','{}','[1,1.5]',2.5,'خوب',true,now()),
('critical-answer-other','critical-exam','81000000-0000-0000-0000-000000000003','[0,""]','{}','[]',0,'',false,now())
on conflict (id) do update set graded=excluded.graded,responses=excluded.responses,grades=excluded.grades;

set role authenticated;
select set_config('request.jwt.claim.sub','81000000-0000-0000-0000-000000000002',false);
select pg_temp.assert_true(
    jsonb_array_length(public.native_my_answers_v1()->'items')=2,
    'student list contains only own attempts'
);
select pg_temp.assert_true(
    not (public.native_my_answer_detail_v1('critical-answer-pending')::text ~ 'correctOption|explanation|چهار'),
    'ungraded detail never exposes answer key'
);
select pg_temp.assert_true(
    public.native_my_answer_detail_v1('critical-answer-graded')::text ~ 'correctOption',
    'graded detail contains answer key'
);
select pg_temp.assert_true(
    public.native_my_answer_detail_v1('critical-answer-other') ? 'error',
    'cross-student answer detail denied'
);
reset role;

select pg_temp.assert_true(
    has_function_privilege('authenticated','public.native_my_answer_detail_v1(text)','EXECUTE'),
    'authenticated detail execute granted'
);
select pg_temp.assert_true(
    not has_function_privilege('anon','public.native_my_answer_detail_v1(text)','EXECUTE'),
    'anon detail execute denied'
);
select pg_temp.assert_true(not exists(
    select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public'
      and p.proname in (
        'native_complete_teacher_registration_v1','native_update_my_username_v1',
        'native_my_registration_state_v1','native_my_answers_v1','native_my_answer_detail_v1'
      )
      and has_function_privilege('public',p.oid,'EXECUTE')
), 'no PUBLIC execute on V12 security-definer functions');

select 'V12_CRITICAL_FLOWS_PASS' as result;
