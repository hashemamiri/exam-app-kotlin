-- PostgreSQL integration test for V10. Run only on an isolated local database.
create or replace function pg_temp.assert_true(p_ok boolean, p_message text)
returns void language plpgsql as $$ begin
    if not coalesce(p_ok, false) then raise exception 'ASSERT: %', p_message; end if;
end $$;

insert into auth.users(id) values
('10000000-0000-0000-0000-000000000001'),
('10000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000001'),
('20000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000003'),
('20000000-0000-0000-0000-000000000004'),
('20000000-0000-0000-0000-000000000005'),
('20000000-0000-0000-0000-000000000006'),
('30000000-0000-0000-0000-000000000001')
on conflict do nothing;

insert into public.profiles(id,full_name,display_name,username,role,teacher_id,plain_password,hdr_province,hdr_city,hdr_district,hdr_school) values
('10000000-0000-0000-0000-000000000001','معلم اول','دبیر اول','teacher1','teacher',null,'teacher-secret','تهران','تهران','۱','مدرسه یک'),
('10000000-0000-0000-0000-000000000002','معلم دوم','دبیر دوم','teacher2','teacher',null,'other-secret','','','',''),
('20000000-0000-0000-0000-000000000001','دانش‌آموز ۱',null,'stu1','student','10000000-0000-0000-0000-000000000001','student-secret',null,null,null,null),
('20000000-0000-0000-0000-000000000002','دانش‌آموز ۲',null,'stu2','student','10000000-0000-0000-0000-000000000001','x',null,null,null,null),
('20000000-0000-0000-0000-000000000003','دانش‌آموز ۳',null,'stu3','student','10000000-0000-0000-0000-000000000001','x',null,null,null,null),
('20000000-0000-0000-0000-000000000004','دانش‌آموز ۴',null,'stu4','student','10000000-0000-0000-0000-000000000001','x',null,null,null,null),
('20000000-0000-0000-0000-000000000005','دانش‌آموز ۵',null,'stu5','student','10000000-0000-0000-0000-000000000001','x',null,null,null,null),
('20000000-0000-0000-0000-000000000006','دانش‌آموز ۶',null,'stu6','student','10000000-0000-0000-0000-000000000001','x',null,null,null,null),
('30000000-0000-0000-0000-000000000001','دانش‌آموز مقصد',null,'stu1','student','10000000-0000-0000-0000-000000000002','destination-secret',null,null,null,null)
on conflict (id) do nothing;

insert into public.wallets(user_id,balance) values
('10000000-0000-0000-0000-000000000001',100000),
('10000000-0000-0000-0000-000000000002',100000)
on conflict (user_id) do update set balance=excluded.balance;

insert into public.classes(id,teacher_id,name,grade) values
('40000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','کلاس الف','هفتم')
on conflict do nothing;
insert into public.class_members(class_id,student_id) values
('40000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001')
on conflict do nothing;

insert into public.exams(
 id,title,subject,questions,code,teacher_id,total_score,is_open,duration,audience,
 shuffle_q,shuffle_opt,neg_marking,attempts_allowed,attempt_on_timeout,grade_policy,attempt_cooldown
) values (
 'exam-v10','آزمون جامع','ریاضی',
 '[{"id":"q1","type":"essay","text":"سؤال ۱","score":3},
   {"id":"q2","type":"essay","text":"سؤال ۲","score":2},
   {"id":"q3","type":"essay","text":"سؤال ۳","score":5}]'::jsonb,
 'V10TST','10000000-0000-0000-0000-000000000001',10,false,30,'classes',false,false,0,1,false,'last',0
) on conflict do nothing;
insert into public.exam_keys(exam_id,answers) values
('exam-v10','[{"i":0},{"i":1},{"i":2}]'::jsonb) on conflict do nothing;
insert into public.exam_audience_classes(exam_id,class_id) values
('exam-v10','40000000-0000-0000-0000-000000000001') on conflict do nothing;

-- Idempotent queued submission: underlying submit_answer runs exactly once.
select set_config('request.jwt.claim.sub','20000000-0000-0000-0000-000000000001',false);
do $$
declare r jsonb; again jsonb; op uuid := '50000000-0000-0000-0000-000000000001';
begin
 r := public.native_submit_queued_answer_v1(op,'exam-v10','["a","b","c"]','{}','{}');
 perform pg_temp.assert_true((r->>'ok')::boolean, 'queued submit succeeds');
 again := public.native_submit_queued_answer_v1(op,'exam-v10','["a","b","c"]','{}','{}');
 perform pg_temp.assert_true((again->>'idempotent')::boolean, 'queued submit idempotent');
 perform pg_temp.assert_true((select count(*)=1 from public.answers where student_id=auth.uid() and exam_id='exam-v10'), 'one answer row');
end $$;

-- Add five more ungraded answers.
insert into public.answers(id,exam_id,student_id,student_name,responses,grades) values
('a2','exam-v10','20000000-0000-0000-0000-000000000002','دانش‌آموز ۲','["x","y","z"]','[]'),
('a3','exam-v10','20000000-0000-0000-0000-000000000003','دانش‌آموز ۳','["x","y","z"]','[]'),
('a4','exam-v10','20000000-0000-0000-0000-000000000004','دانش‌آموز ۴','["x","","z"]','[]'),
('a5','exam-v10','20000000-0000-0000-0000-000000000005','دانش‌آموز ۵','["","y","z"]','[]'),
('a6','exam-v10','20000000-0000-0000-0000-000000000006','دانش‌آموز ۶','["x","y",""]','[]')
on conflict do nothing;

select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',false);

-- Invalid atomic batch changes nothing.
do $$
declare items jsonb; r jsonb;
begin
 select jsonb_agg(jsonb_build_object('answer_id',id,'score',case when id='a2' then 99 else 1 end))
 into items from public.answers where exam_id='exam-v10';
 r := public.native_bulk_save_question_grades_v1('exam-v10',0,items);
 perform pg_temp.assert_true(r ? 'error', 'invalid batch rejected');
 perform pg_temp.assert_true(not exists(select 1 from native_grade_progress), 'invalid batch is atomic');
end $$;

-- Grade all three questions question-centrically.
do $$
declare ids text[]; vals numeric[]; items jsonb; r jsonb;
begin
 select array_agg(id order by id) into ids from public.answers where exam_id='exam-v10';
 vals := array[3,2.5,2,1.5,1,0];
 select jsonb_agg(jsonb_build_object('answer_id',ids[i],'score',vals[i])) into items
 from generate_subscripts(ids,1) i;
 r := public.native_bulk_save_question_grades_v1('exam-v10',0,items);
 perform pg_temp.assert_true((r->>'updated')::int=6, 'question 1 bulk');
 r := public.native_finalize_bulk_grades_v1('exam-v10');
 perform pg_temp.assert_true(r ? 'error' and (r->>'incomplete_answers')::int=6, 'early finalize blocked');

 vals := array[2,2,1.5,1,0.5,0];
 select jsonb_agg(jsonb_build_object('answer_id',ids[i],'score',vals[i])) into items
 from generate_subscripts(ids,1) i;
 perform pg_temp.assert_true((public.native_bulk_save_question_grades_v1('exam-v10',1,items)->>'updated')::int=6, 'question 2 bulk');

 vals := array[5,4,3,2,1,0];
 select jsonb_agg(jsonb_build_object('answer_id',ids[i],'score',vals[i])) into items
 from generate_subscripts(ids,1) i;
 perform pg_temp.assert_true((public.native_bulk_save_question_grades_v1('exam-v10',2,items)->>'updated')::int=6, 'question 3 bulk');
 r := public.native_finalize_bulk_grades_v1('exam-v10');
 perform pg_temp.assert_true((r->>'finalized')::int=6, 'finalize after all questions');
 perform pg_temp.assert_true((select count(*)=6 from answers where exam_id='exam-v10' and graded), 'all answers finalized');
end $$;

-- Item analysis includes difficulty, omission, discrimination and reliability.
do $$
declare r jsonb;
begin
 r := public.native_question_analysis_v1('exam-v10');
 perform pg_temp.assert_true((r->>'answer_count')::int=6, 'analysis answer count');
 perform pg_temp.assert_true(jsonb_array_length(r->'questions')=3, 'analysis question count');
 perform pg_temp.assert_true(r ? 'cronbach_alpha', 'cronbach field');
 perform pg_temp.assert_true((r->'questions'->0) ? 'discrimination', 'discrimination field');
 perform pg_temp.assert_true((r->'questions'->0) ? 'point_biserial', 'point biserial field');
end $$;

-- Export contains no password/token and has all safe sections.
create temporary table exported_bundle(value jsonb);
insert into exported_bundle select public.native_export_backup_v1();
select pg_temp.assert_true((select value->>'_app'='exam-native' from exported_bundle), 'backup app marker');
select pg_temp.assert_true((select jsonb_array_length(value->'exams')=1 from exported_bundle), 'backup exam count');
select pg_temp.assert_true((select position('student-secret' in value::text)=0 from exported_bundle), 'student password excluded');
select pg_temp.assert_true((select position('teacher-secret' in value::text)=0 from exported_bundle), 'teacher password excluded');
select pg_temp.assert_true((select (value->'security'->>'contains_tokens')::boolean=false from exported_bundle), 'token security marker');

-- Restore under another teacher is atomic, charged once and idempotent.
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000002',false);
do $$
declare b jsonb; r jsonb; again jsonb; op uuid := '60000000-0000-0000-0000-000000000001';
begin
 select value into b from exported_bundle;
 r := public.native_restore_backup_v1(op,b,'{"exams":true,"classes":true,"memberships":true,"header":true}');
 perform pg_temp.assert_true((r->>'exams_created')::int=1, 'one exam restored');
 perform pg_temp.assert_true((r->>'classes_created')::int=1, 'one class restored');
 perform pg_temp.assert_true((r->>'memberships_restored')::int=1, 'membership matched by username');
 perform pg_temp.assert_true((r->>'cost')::bigint=3000 and (r->>'balance')::bigint=97000, 'restore wallet charge');
 again := public.native_restore_backup_v1(op,b,'{"exams":true,"classes":true,"memberships":true,"header":true}');
 perform pg_temp.assert_true((again->>'idempotent')::boolean, 'restore idempotent');
 perform pg_temp.assert_true((select count(*)=1 from exams where teacher_id=auth.uid()), 'no duplicate restore exam');
 perform pg_temp.assert_true((select hdr_school='مدرسه یک' from profiles where id=auth.uid()), 'header restored');
end $$;

-- Grants: only authenticated application paths are exposed.
select pg_temp.assert_true(has_function_privilege('authenticated','public.native_submit_queued_answer_v1(uuid,text,jsonb,jsonb,jsonb)','execute'),'queued submit grant');
select pg_temp.assert_true(has_function_privilege('authenticated','public.native_bulk_save_question_grades_v1(text,integer,jsonb)','execute'),'bulk grade grant');
select pg_temp.assert_true(not has_function_privilege('anon','public.native_restore_backup_v1(uuid,jsonb,jsonb)','execute'),'anon restore denied');

select 'V10_SQL_INTEGRATION_PASS' as result;
