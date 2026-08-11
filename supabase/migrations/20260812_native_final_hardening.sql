-- V11 final hardening for the Native Android application.
-- IMPORTANT ORDER: deploy the V11 manage-student Edge Function before running this file.
-- This migration intentionally retires WebView password-display paths.

begin;

-- ============================================================
-- 1) Security/audit tables and mutation RPCs
-- ============================================================
create table if not exists public.student_admin_audit (
    id bigserial primary key,
    teacher_id uuid not null references auth.users(id) on delete cascade,
    student_id uuid,
    action text not null check (action in ('create','update','reset_password','delete','bulk_create')),
    details jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);
create index if not exists idx_student_admin_audit_teacher
    on public.student_admin_audit(teacher_id, created_at desc);
alter table public.student_admin_audit enable row level security;

create table if not exists public.maintenance_audit (
    id bigserial primary key,
    requested_by uuid not null references auth.users(id) on delete cascade,
    dry_run boolean not null,
    orphan_candidates integer not null default 0,
    apk_candidates integer not null default 0,
    deleted_objects integer not null default 0,
    deleted_apks integer not null default 0,
    grace_days integer not null,
    created_at timestamptz not null default now()
);
create index if not exists idx_maintenance_audit_requester
    on public.maintenance_audit(requested_by, created_at desc);
alter table public.maintenance_audit enable row level security;

create or replace function public.native_ensure_profile_v1(p_fallback_name text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_name text := left(coalesce(nullif(btrim(p_fallback_name), ''), 'کاربر'), 200);
    v_result jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    insert into public.profiles(id, full_name, display_name, role)
    values (v_uid, v_name, v_name, 'student')
    on conflict (id) do nothing;

    select jsonb_build_object(
        'ok', true,
        'id', p.id,
        'full_name', p.full_name,
        'display_name', p.display_name,
        'username', p.username,
        'role', p.role,
        'avatar_url', p.avatar_url,
        'avatar_public', coalesce(p.avatar_public, true),
        'hdr_province', p.hdr_province,
        'hdr_city', p.hdr_city,
        'hdr_district', p.hdr_district,
        'hdr_school', p.hdr_school
    ) into v_result
    from public.profiles p
    where p.id = v_uid;
    return coalesce(v_result, jsonb_build_object('error', 'پروفایل ساخته نشد'));
end;
$$;

create or replace function public.native_set_exam_open_v1(p_exam text, p_open boolean)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare v_count integer;
begin
    if auth.uid() is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    update public.exams
    set is_open = coalesce(p_open, false)
    where id = p_exam and teacher_id = auth.uid();
    get diagnostics v_count = row_count;
    if v_count <> 1 then return jsonb_build_object('error', 'آزمون یافت نشد یا دسترسی ندارید'); end if;
    return jsonb_build_object('ok', true, 'is_open', coalesce(p_open, false));
end;
$$;

-- ============================================================
-- 2) Permanently remove legacy plaintext passwords
-- ============================================================
-- Erase values first so a failed DROP cannot leave readable data behind.
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema='public' and table_name='profiles' and column_name='plain_password'
    ) then
        execute 'update public.profiles set plain_password = null where plain_password is not null';
    end if;
end;
$$;

-- Drop every old public function that actually references the standalone column.
-- The word-boundary regex does not match safe keys such as contains_plain_password.
do $$
declare r record;
begin
    for r in
        select p.oid::regprocedure as signature
        from pg_proc p
        join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'public'
          and p.prokind = 'f'
          and p.proname <> 'native_security_status_v1'
          and pg_get_functiondef(p.oid) ~ '(^|[^A-Za-z0-9_])plain_password([^A-Za-z0-9_]|$)'
    loop
        execute 'drop function if exists ' || r.signature || ' cascade';
    end loop;
end;
$$;

alter table public.profiles drop column if exists plain_password;

-- Password-free replacements used by Native screens.
create or replace function public.my_students()
returns table(
    id uuid,
    full_name text,
    first_name text,
    last_name text,
    username text,
    gender text,
    is_active boolean,
    class_names text,
    created_at timestamptz,
    father_name text,
    grade text,
    avatar_url text
)
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select p.id, p.full_name, p.first_name, p.last_name, p.username, p.gender,
           coalesce(p.is_active, true),
           coalesce((
               select string_agg(distinct c.name, '، ' order by c.name)
               from public.class_members m
               join public.classes c on c.id = m.class_id
               where m.student_id = p.id and c.teacher_id = auth.uid()
           ), ''),
           p.created_at, p.father_name, p.grade, p.avatar_url
    from public.profiles p
    where p.teacher_id = auth.uid() and p.role = 'student'
    order by p.full_name, p.username;
$$;

create or replace function public.class_roster(p_class uuid)
returns table(
    id uuid,
    full_name text,
    first_name text,
    last_name text,
    username text,
    gender text,
    is_active boolean,
    father_name text,
    grade text,
    avatar_url text
)
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select p.id, p.full_name, p.first_name, p.last_name, p.username, p.gender,
           coalesce(p.is_active, true), p.father_name, p.grade, p.avatar_url
    from public.class_members m
    join public.classes c on c.id = m.class_id
    join public.profiles p on p.id = m.student_id
    where m.class_id = p_class and c.teacher_id = auth.uid()
    order by p.full_name, p.username;
$$;

-- ============================================================
-- 3) RLS and least-privilege table access
-- ============================================================
do $$
declare r record;
begin
    for r in select schemaname, tablename from pg_tables where schemaname = 'public'
    loop
        execute format('alter table %I.%I enable row level security', r.schemaname, r.tablename);
    end loop;
end;
$$;

-- Replace every policy on direct-read tables; old WebView policies must not broaden access.
do $$
declare r record;
begin
    for r in
        select tablename, policyname
        from pg_policies
        where schemaname='public'
          and tablename in ('profiles','exams','exam_keys','answers','classes','class_members',
                            'student_admin_audit','maintenance_audit')
    loop
        execute format('drop policy if exists %I on public.%I', r.policyname, r.tablename);
    end loop;
end;
$$;

drop policy if exists v11_profile_own_read on public.profiles;
create policy v11_profile_own_read on public.profiles
for select to authenticated using (id = auth.uid());

drop policy if exists v11_exam_teacher_read on public.exams;
create policy v11_exam_teacher_read on public.exams
for select to authenticated using (teacher_id = auth.uid());

drop policy if exists v11_exam_key_teacher_read on public.exam_keys;
create policy v11_exam_key_teacher_read on public.exam_keys
for select to authenticated using (
    exists (select 1 from public.exams e where e.id = exam_id and e.teacher_id = auth.uid())
);

drop policy if exists v11_answer_owner_read on public.answers;
create policy v11_answer_owner_read on public.answers
for select to authenticated using (
    student_id = auth.uid()
    or exists (select 1 from public.exams e where e.id = exam_id and e.teacher_id = auth.uid())
);

drop policy if exists v11_class_teacher_read on public.classes;
create policy v11_class_teacher_read on public.classes
for select to authenticated using (teacher_id = auth.uid());

drop policy if exists v11_class_member_read on public.class_members;
create policy v11_class_member_read on public.class_members
for select to authenticated using (
    student_id = auth.uid()
    or exists (select 1 from public.classes c where c.id = class_id and c.teacher_id = auth.uid())
);

drop policy if exists v11_student_audit_teacher_read on public.student_admin_audit;
create policy v11_student_audit_teacher_read on public.student_admin_audit
for select to authenticated using (teacher_id = auth.uid());

drop policy if exists v11_maintenance_audit_owner_read on public.maintenance_audit;
create policy v11_maintenance_audit_owner_read on public.maintenance_audit
for select to authenticated using (requested_by = auth.uid());

-- Remove every direct public-schema privilege, then add only required reads.
revoke all on all tables in schema public from anon, authenticated;
revoke all on all sequences in schema public from anon, authenticated;

grant select on public.app_version to anon, authenticated;
grant select on public.holidays, public.holiday_years to anon, authenticated;
grant select on public.profiles, public.exams, public.exam_keys, public.answers,
                public.classes, public.class_members,
                public.student_admin_audit, public.maintenance_audit
to authenticated;

alter default privileges in schema public revoke all on tables from anon, authenticated;
alter default privileges in schema public revoke all on sequences from anon, authenticated;
alter default privileges in schema public revoke execute on functions from public, anon, authenticated;

-- ============================================================
-- 4) Function allowlists and secure search_path
-- ============================================================
do $$
declare r record;
begin
    for r in
        select p.oid::regprocedure as signature
        from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public'
    loop
        execute 'revoke all on function ' || r.signature || ' from public, anon, authenticated';
        if r.signature::text not like 'publish_native_app_release_v1(%' then
            -- service_role keeps no implicit PUBLIC execute either; explicit grants follow.
            null;
        end if;
    end loop;
end;
$$;

do $$
declare r record;
declare allowed text[] := array[
    'native_ensure_profile_v1','native_my_profile','native_save_profile','teacher_public_profile',
    'native_delete_exam','native_duplicate_exam_v2','native_set_exam_open_v1','native_save_exam_v1',
    'get_exam_audience','set_exam_audience','get_exam_for_student','native_submit_queued_answer_v1',
    'my_classes','create_class','update_class','delete_class','class_roster','class_roster_pick',
    'add_students_to_class','remove_student_from_class','my_students','my_students_for_pick',
    'save_student_extra','set_student_active','bank_add','bank_list','bank_del','bank_move',
    'fb_add','fb_list','exam_attendance','exam_attend_summary','exam_live_status',
    'exam_autograde_info','approve_auto_grades','unapprove_grade','reset_student_attempt',
    'extend_student_time','native_save_grade','native_bulk_save_question_grades_v1',
    'native_finalize_bulk_grades_v1','native_question_analysis_v1','my_answers','my_grades',
    'cal_month','cal_day','cal_save_note','cal_delete_note','holidays_for',
    'native_wallet_snapshot','native_export_backup_v1','native_restore_backup_v1',
    'check_app_update','native_security_status_v1'
];
begin
    for r in
        select p.oid::regprocedure as signature
        from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public' and p.proname = any(allowed)
    loop
        execute 'grant execute on function ' || r.signature || ' to authenticated';
    end loop;
end;
$$;

do $$
declare r record;
begin
    for r in
        select p.oid::regprocedure as signature
        from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public' and p.proname in ('check_app_update','holidays_for')
    loop
        execute 'grant execute on function ' || r.signature || ' to anon';
    end loop;
    for r in
        select p.oid::regprocedure as signature
        from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public' and p.proname in (
            'publish_native_app_release_v1','native_create_wallet_payment_order',
            'native_set_wallet_payment_authority','native_fail_wallet_payment_order',
            'native_credit_wallet_payment','wallet_topup','wallet_refund'
        )
    loop
        execute 'grant execute on function ' || r.signature || ' to service_role';
    end loop;
end;
$$;

-- Lock search_path for every SECURITY DEFINER function in public.
do $$
declare r record;
begin
    for r in
        select p.oid::regprocedure as signature
        from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public' and p.prosecdef
    loop
        execute 'alter function ' || r.signature || ' set search_path = public, pg_temp';
    end loop;
end;
$$;

-- ============================================================
-- 5) Storage policy reset: public reads, owner-prefixed writes only
-- ============================================================
do $$
declare r record;
begin
    for r in
        select policyname
        from pg_policies
        where schemaname='storage' and tablename='objects'
    loop
        execute format('drop policy if exists %I on storage.objects', r.policyname);
    end loop;
end;
$$;

create policy v11_public_read_app_updates
on storage.objects for select to public
using (bucket_id = 'app-updates');

create policy v11_public_read_exam_images
on storage.objects for select to public
using (bucket_id = 'exam-images');

create policy v11_authenticated_upload_exam_images
on storage.objects for insert to authenticated
with check (
    bucket_id = 'exam-images'
    and (storage.foldername(name))[1] in ('avatars','questions','option_images','matching','answers')
    and (storage.foldername(name))[2] = auth.uid()::text
);

-- ============================================================
-- 6) Safe status RPC for post-migration verification
-- ============================================================
create or replace function public.native_security_status_v1()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select jsonb_build_object(
        'plain_password_removed', not exists (
            select 1 from information_schema.columns
            where table_schema='public' and table_name='profiles' and column_name='plain_password'
        ),
        'public_tables_without_rls', (
            select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace
            where n.nspname='public' and c.relkind='r' and not c.relrowsecurity
        ),
        'anon_mutating_table_grants', (
            select count(*) from information_schema.role_table_grants
            where grantee='anon' and table_schema='public'
              and privilege_type in ('INSERT','UPDATE','DELETE','TRUNCATE','REFERENCES','TRIGGER')
        ),
        'authenticated_mutating_table_grants', (
            select count(*) from information_schema.role_table_grants
            where grantee='authenticated' and table_schema='public'
              and privilege_type in ('INSERT','UPDATE','DELETE','TRUNCATE','REFERENCES','TRIGGER')
        ),
        'security_definer_public_execute', (
            select count(distinct p.oid)
            from pg_proc p
            join pg_namespace n on n.oid=p.pronamespace
            cross join lateral aclexplode(coalesce(p.proacl, acldefault('f', p.proowner))) privilege
            where n.nspname='public' and p.prosecdef
              and privilege.grantee=0 and privilege.privilege_type='EXECUTE'
        ),
        'student_admin_audit_ready', to_regclass('public.student_admin_audit') is not null,
        'maintenance_audit_ready', to_regclass('public.maintenance_audit') is not null
    );
$$;
revoke all on function public.native_security_status_v1() from public, anon;
grant execute on function public.native_security_status_v1() to authenticated;

commit;
notify pgrst, 'reload schema';
