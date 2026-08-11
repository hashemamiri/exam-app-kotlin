-- Run only on an isolated PostgreSQL test database after V9, V10 and V11 migrations.
create or replace function pg_temp.assert_true(p_ok boolean, p_message text)
returns void language plpgsql as $$ begin
    if not coalesce(p_ok,false) then raise exception 'ASSERT: %', p_message; end if;
end $$;

-- Plaintext password is gone from both schema and RPC definitions.
select pg_temp.assert_true(not exists(
    select 1 from information_schema.columns
    where table_schema='public' and table_name='profiles' and column_name='plain_password'
), 'plain_password column removed');
select pg_temp.assert_true(not exists(
    select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public' and p.prokind='f'
      and p.proname <> 'native_security_status_v1'
      and pg_get_functiondef(p.oid) ~ '(^|[^A-Za-z0-9_])plain_password([^A-Za-z0-9_]|$)'
), 'no function reads plain_password');

-- Seed users after migration through owner privileges.
insert into auth.users(id) values
('71000000-0000-0000-0000-000000000001'),
('71000000-0000-0000-0000-000000000002'),
('72000000-0000-0000-0000-000000000001'),
('72000000-0000-0000-0000-000000000002'),
('73000000-0000-0000-0000-000000000001')
on conflict do nothing;
insert into public.profiles(id,full_name,username,role,teacher_id,created_at) values
('71000000-0000-0000-0000-000000000001','معلم امن ۱','secure_teacher_1','teacher',null,now()),
('71000000-0000-0000-0000-000000000002','معلم امن ۲','secure_teacher_2','teacher',null,now()),
('72000000-0000-0000-0000-000000000001','دانش‌آموز امن ۱','secure_student_1','student','71000000-0000-0000-0000-000000000001',now()),
('72000000-0000-0000-0000-000000000002','دانش‌آموز امن ۲','secure_student_2','student','71000000-0000-0000-0000-000000000002',now())
on conflict (id) do nothing;
insert into public.exams(id,title,questions,code,teacher_id,is_open,created_at) values
('secure-exam-1','آزمون معلم یک','[]','SEC001','71000000-0000-0000-0000-000000000001',false,now()),
('secure-exam-2','آزمون معلم دو','[]','SEC002','71000000-0000-0000-0000-000000000002',false,now())
on conflict do nothing;

-- Teacher sees only own direct-read rows and cannot mutate tables directly.
set role authenticated;
select set_config('request.jwt.claim.sub','71000000-0000-0000-0000-000000000001',false);
select pg_temp.assert_true((select count(*)=1 from public.exams),'teacher direct select isolated');
do $$ begin
    begin
        update public.exams set title='forbidden' where id='secure-exam-1';
        raise exception 'direct update unexpectedly succeeded';
    exception when insufficient_privilege then null;
    end;
end $$;
select pg_temp.assert_true((public.native_set_exam_open_v1('secure-exam-1',true)->>'ok')::boolean,'owner RPC mutation works');
select pg_temp.assert_true(public.native_set_exam_open_v1('secure-exam-2',true) ? 'error','cross-teacher RPC denied');
reset role;

-- Student receives no direct exam rows, but own profile remains readable.
set role authenticated;
select set_config('request.jwt.claim.sub','72000000-0000-0000-0000-000000000001',false);
select pg_temp.assert_true((select count(*)=0 from public.exams),'student direct exams hidden');
select pg_temp.assert_true((select count(*)=1 from public.profiles),'student own profile visible');
reset role;

-- New OTP user can create a profile only through the hardened RPC.
set role authenticated;
select set_config('request.jwt.claim.sub','73000000-0000-0000-0000-000000000001',false);
select pg_temp.assert_true((public.native_ensure_profile_v1('کاربر جدید')->>'ok')::boolean,'ensure profile RPC');
select pg_temp.assert_true((select role='student' from public.profiles where id=auth.uid()),'new role cannot escalate');
reset role;

-- Direct mutation grants and unsafe function execution are absent.
select pg_temp.assert_true(not has_table_privilege('authenticated','public.exams','INSERT,UPDATE,DELETE'),'exam DML revoked');
select pg_temp.assert_true(not has_table_privilege('authenticated','public.profiles','INSERT,UPDATE,DELETE'),'profile DML revoked');
select pg_temp.assert_true(not has_function_privilege('authenticated','public.submit_answer(text,jsonb,jsonb,jsonb)','EXECUTE'),'legacy submit revoked');
select pg_temp.assert_true(has_function_privilege('authenticated','public.native_submit_queued_answer_v1(uuid,text,jsonb,jsonb,jsonb)','EXECUTE'),'queued wrapper allowed');
select pg_temp.assert_true(has_function_privilege('anon','public.check_app_update(integer)','EXECUTE'),'public update check allowed');
select pg_temp.assert_true(not has_function_privilege('anon','public.native_save_exam_v1(jsonb)','EXECUTE'),'anon exam save denied');

-- Storage policies were replaced, not merely added alongside broad old rules.
select pg_temp.assert_true((
    select count(*)=3 from pg_policies where schemaname='storage' and tablename='objects'
),'exact storage policy count');
select pg_temp.assert_true(exists(
    select 1 from pg_policies where schemaname='storage' and tablename='objects'
      and policyname='v11_authenticated_upload_exam_images'
),'owner-prefix upload policy exists');

-- Consolidated status is all green.
select set_config('request.jwt.claim.sub','71000000-0000-0000-0000-000000000001',false);
do $$ declare status jsonb; begin
 status := public.native_security_status_v1();
 perform pg_temp.assert_true((status->>'plain_password_removed')::boolean,'status plain password');
 perform pg_temp.assert_true((status->>'public_tables_without_rls')::int=0,'all public tables RLS');
 perform pg_temp.assert_true((status->>'anon_mutating_table_grants')::int=0,'anon mutation grants');
 perform pg_temp.assert_true((status->>'authenticated_mutating_table_grants')::int=0,'auth mutation grants');
 perform pg_temp.assert_true((status->>'security_definer_public_execute')::int=0,'public definer execute');
end $$;

select 'V11_SECURITY_REGRESSION_PASS' as result;
