-- تست یکپارچه V28: رشته تحصیلی دانش‌آموز، کلاس و سربرگ رسمی
-- این فایل روی PostgreSQL خام با schema شبیه‌سازی‌شده اجرا می‌شود و
-- مالکیت، محدودیت طول، پایداری مقدار و عدم افشای رمز را بررسی می‌کند.

\set ON_ERROR_STOP on

-- ============================================================
-- 0) auth شبیه‌سازی‌شده
-- ============================================================
create schema if not exists auth;
create table if not exists auth.users(id uuid primary key);

create or replace function auth.uid() returns uuid
language sql stable as $$ select nullif(current_setting('test.uid', true), '')::uuid $$;

-- ============================================================
-- 1) جدول‌های پایه
-- ============================================================
drop table if exists public.class_members cascade;
drop table if exists public.classes cascade;
drop table if exists public.profiles cascade;

create table public.profiles(
    id uuid primary key,
    teacher_id uuid,
    role text not null default 'student',
    full_name text,
    display_name text,
    first_name text,
    last_name text,
    username text,
    gender text,
    is_active boolean default true,
    father_name text,
    grade text,
    avatar_url text,
    avatar_public boolean default true,
    hdr_province text,
    hdr_city text,
    hdr_district text,
    hdr_school text,
    hdr_grade text,
    created_at timestamptz default now()
);

create table public.classes(
    id uuid primary key default gen_random_uuid(),
    teacher_id uuid not null,
    name text not null,
    grade text,
    created_at timestamptz default now()
);

create table public.class_members(
    class_id uuid not null references public.classes(id) on delete cascade,
    student_id uuid not null references public.profiles(id) on delete cascade,
    primary key (class_id, student_id)
);

-- توابع پیش‌نیاز که V28 آن‌ها را بازنویسی یا صدا می‌زند
create or replace function public.my_students()
returns table(id uuid, full_name text, first_name text, last_name text, username text,
              gender text, is_active boolean, class_names text, created_at timestamptz,
              father_name text, grade text, avatar_url text)
language sql stable security definer set search_path = public, pg_temp as $$
    select p.id, p.full_name, p.first_name, p.last_name, p.username, p.gender,
           coalesce(p.is_active, true), '', p.created_at, p.father_name, p.grade, p.avatar_url
    from public.profiles p where p.teacher_id = auth.uid() and p.role = 'student';
$$;

create or replace function public.class_roster(p_class uuid)
returns table(id uuid, full_name text, first_name text, last_name text, username text,
              gender text, is_active boolean, father_name text, grade text, avatar_url text)
language sql stable security definer set search_path = public, pg_temp as $$
    select p.id, p.full_name, p.first_name, p.last_name, p.username, p.gender,
           coalesce(p.is_active, true), p.father_name, p.grade, p.avatar_url
    from public.class_members m
    join public.classes c on c.id = m.class_id
    join public.profiles p on p.id = m.student_id
    where m.class_id = p_class and c.teacher_id = auth.uid();
$$;

create or replace function public.native_export_backup_v2()
returns jsonb language sql stable security definer set search_path = public, pg_temp as $$
    select jsonb_build_object(
        '_version', 3,
        'profile', jsonb_build_object(
            'header', jsonb_build_object('grade', coalesce((select hdr_grade from public.profiles where id = auth.uid()), ''))
        )
    );
$$;

create or replace function public.native_restore_backup_v2(p_operation uuid, p_bundle jsonb, p_options jsonb default '{}'::jsonb)
returns jsonb language plpgsql security definer set search_path = public, pg_temp as $$
begin
    update public.profiles
    set hdr_grade = nullif(left(btrim(coalesce(p_bundle->'profile'->'header'->>'grade','')),120), '')
    where id = auth.uid();
    return jsonb_build_object('ok', true);
end; $$;

do $$ begin
    if not exists (select 1 from pg_roles where rolname='authenticated') then create role authenticated; end if;
    if not exists (select 1 from pg_roles where rolname='anon') then create role anon; end if;
end $$;

-- ============================================================
-- 2) اجرای مهاجرت V28 (دو بار، برای اثبات idempotency)
-- ============================================================
\i supabase/migrations/20260814_native_field_of_study_v28.sql
\i supabase/migrations/20260814_native_field_of_study_v28.sql

-- ============================================================
-- 3) داده آزمایشی
-- ============================================================
insert into auth.users(id) values
    ('11111111-1111-4111-8111-111111111111'),
    ('22222222-2222-4222-8222-222222222222'),
    ('33333333-3333-4333-8333-333333333333'),
    ('44444444-4444-4444-8444-444444444444');

insert into public.profiles(id, teacher_id, role, full_name, username) values
    ('11111111-1111-4111-8111-111111111111', null, 'teacher', 'معلم یک', 'teacher1'),
    ('22222222-2222-4222-8222-222222222222', null, 'teacher', 'معلم دو', 'teacher2'),
    ('33333333-3333-4333-8333-333333333333', '11111111-1111-4111-8111-111111111111', 'student', 'دانش‌آموز الف', 'stu_a'),
    ('44444444-4444-4444-8444-444444444444', '22222222-2222-4222-8222-222222222222', 'student', 'دانش‌آموز ب', 'stu_b');

-- ============================================================
-- تست ۱: مالک می‌تواند رشته دانش‌آموز خود را ذخیره کند
-- ============================================================
select set_config('test.uid', '11111111-1111-4111-8111-111111111111', false);
do $$
declare v jsonb;
begin
    v := public.native_save_student_extra_v28(
        '33333333-3333-4333-8333-333333333333', 'stu_a', 'پدر الف', 'دهم', 'ریاضی فیزیک');
    assert v->>'ok' = 'true', 'owner student extra save failed: ' || v::text;
    assert (select field_of_study from public.profiles where id='33333333-3333-4333-8333-333333333333') = 'ریاضی فیزیک',
        'field_of_study was not persisted';
    assert (select grade from public.profiles where id='33333333-3333-4333-8333-333333333333') = 'دهم',
        'grade was not persisted';
    raise notice 'TEST 1 owner student field save: PASS';
end $$;

-- ============================================================
-- تست ۲: معلم دیگر نمی‌تواند رشته این دانش‌آموز را تغییر دهد
-- ============================================================
select set_config('test.uid', '22222222-2222-4222-8222-222222222222', false);
do $$
declare v jsonb;
begin
    v := public.native_save_student_extra_v28(
        '33333333-3333-4333-8333-333333333333', 'stu_a', 'نفوذ', 'دوازدهم', 'تجربی');
    assert v ? 'error', 'cross-teacher student write was not denied';
    assert (select field_of_study from public.profiles where id='33333333-3333-4333-8333-333333333333') = 'ریاضی فیزیک',
        'cross-teacher write mutated the row';
    raise notice 'TEST 2 cross-teacher denial: PASS';
end $$;

-- ============================================================
-- تست ۳: محدودیت طول ۱۲۰ نویسه
-- ============================================================
select set_config('test.uid', '11111111-1111-4111-8111-111111111111', false);
do $$
declare v jsonb;
begin
    v := public.native_save_student_extra_v28(
        '33333333-3333-4333-8333-333333333333', 'stu_a', '', 'دهم', repeat('x', 121));
    assert v ? 'error', 'over-length field was accepted';
    raise notice 'TEST 3 length guard: PASS';
end $$;

-- ============================================================
-- تست ۴: کلاس با رشته ساخته و ویرایش می‌شود
-- ============================================================
do $$
declare v jsonb; v_id uuid;
begin
    v := public.native_save_class_v28(null, 'دهم ریاضی', 'دهم', 'ریاضی فیزیک');
    assert v->>'ok' = 'true', 'class create failed: ' || v::text;
    v_id := (v->>'id')::uuid;
    assert (select field_of_study from public.classes where id = v_id) = 'ریاضی فیزیک',
        'class field_of_study not stored';

    v := public.native_save_class_v28(v_id, 'دهم تجربی', 'دهم', 'علوم تجربی');
    assert v->>'ok' = 'true', 'class update failed: ' || v::text;
    assert (select field_of_study from public.classes where id = v_id) = 'علوم تجربی',
        'class field_of_study not updated';
    raise notice 'TEST 4 class field create/update: PASS';
end $$;

-- ============================================================
-- تست ۵: معلم دیگر نمی‌تواند کلاس را ویرایش کند
-- ============================================================
do $$
declare v jsonb; v_id uuid;
begin
    select id into v_id from public.classes where teacher_id='11111111-1111-4111-8111-111111111111' limit 1;
    perform set_config('test.uid', '22222222-2222-4222-8222-222222222222', false);
    v := public.native_save_class_v28(v_id, 'ربوده‌شده', 'دوازدهم', 'انسانی');
    assert v ? 'error', 'cross-teacher class update was not denied';
    assert (select field_of_study from public.classes where id = v_id) = 'علوم تجربی',
        'cross-teacher class update mutated the row';
    perform set_config('test.uid', '11111111-1111-4111-8111-111111111111', false);
    raise notice 'TEST 5 cross-teacher class denial: PASS';
end $$;

-- ============================================================
-- تست ۶: فهرست‌ها رشته را برمی‌گردانند و رمز افشا نمی‌شود
-- ============================================================
do $$
declare v_field text; v_cols integer;
begin
    select field_of_study into v_field from public.my_students()
    where id = '33333333-3333-4333-8333-333333333333';
    assert v_field = 'ریاضی فیزیک', 'my_students did not return field_of_study';

    select count(*) into v_cols
    from information_schema.columns
    where table_schema='public' and table_name='profiles' and column_name='plain_password';
    assert v_cols = 0, 'plain_password column reappeared';

    assert not exists(
        select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public' and p.proname in ('my_students','class_roster',
              'native_save_student_extra_v28','native_my_classes_v28')
          and pg_get_functiondef(p.oid) ilike '%plain_password%'
    ), 'a V28 function references plain_password';
    raise notice 'TEST 6 list exposure + no password leak: PASS';
end $$;

-- ============================================================
-- تست ۷: class_roster و native_my_classes_v28 رشته را دارند
-- ============================================================
do $$
declare v_id uuid; v_field text;
begin
    select id into v_id from public.classes where teacher_id='11111111-1111-4111-8111-111111111111' limit 1;
    insert into public.class_members(class_id, student_id)
    values (v_id, '33333333-3333-4333-8333-333333333333') on conflict do nothing;

    select field_of_study into v_field from public.class_roster(v_id) limit 1;
    assert v_field = 'ریاضی فیزیک', 'class_roster did not return student field_of_study';

    select field_of_study into v_field from public.native_my_classes_v28() where id = v_id;
    assert v_field = 'علوم تجربی', 'native_my_classes_v28 did not return class field_of_study';
    raise notice 'TEST 7 roster/class list field: PASS';
end $$;

-- ============================================================
-- تست ۸: سربرگ رسمی رشته را ذخیره و برمی‌گرداند
-- ============================================================
do $$
declare v jsonb;
begin
    v := public.native_save_profile_v28('معلم یک', null, true,
        'تهران', 'تهران', 'منطقه ۵', 'دبیرستان نمونه', 'دهم', 'ریاضی فیزیک');
    assert v->>'ok' = 'true', 'profile save failed: ' || v::text;

    v := public.native_my_profile();
    assert v->>'hdr_field' = 'ریاضی فیزیک', 'native_my_profile did not return hdr_field';
    assert v->>'hdr_grade' = 'دهم', 'native_my_profile lost hdr_grade';
    raise notice 'TEST 8 official header field: PASS';
end $$;

-- ============================================================
-- تست ۹: دانش‌آموز نمی‌تواند سربرگ معلم را بنویسد
-- ============================================================
do $$
declare v jsonb;
begin
    perform set_config('test.uid', '33333333-3333-4333-8333-333333333333', false);
    v := public.native_save_profile_v28('دانش‌آموز الف', null, true,
        'x', 'x', 'x', 'x', 'x', 'نفوذ رشته');
    assert v->>'ok' = 'true', 'student profile save unexpectedly failed';
    assert (select hdr_field from public.profiles where id='33333333-3333-4333-8333-333333333333') is null,
        'student was able to write teacher header field';
    perform set_config('test.uid', '11111111-1111-4111-8111-111111111111', false);
    raise notice 'TEST 9 student header write denial: PASS';
end $$;

-- ============================================================
-- تست ۱۰: پشتیبان نسخه ۴ شامل رشته است و بازیابی می‌شود
-- ============================================================
do $$
declare v_backup jsonb; v jsonb;
begin
    v_backup := public.native_export_backup_v3();
    assert v_backup->>'_version' = '4', 'backup version is not 4';
    assert v_backup->'profile'->'header'->>'field' = 'ریاضی فیزیک',
        'backup did not carry header field';

    update public.profiles set hdr_field = 'پاک‌شده' where id = auth.uid();
    v := public.native_restore_backup_v3(gen_random_uuid(), v_backup, '{"header":true}'::jsonb);
    assert v->>'profile_header_v4' = 'true', 'restore did not report v4';
    assert (select hdr_field from public.profiles where id = auth.uid()) = 'ریاضی فیزیک',
        'restore did not bring back header field';
    raise notice 'TEST 10 backup v4 round-trip: PASS';
end $$;

-- ============================================================
-- تست ۱۱: نسخه‌های قدیمی پشتیبان همچنان پذیرفته می‌شوند
-- ============================================================
do $$
declare v jsonb;
begin
    v := public.native_restore_backup_v3(gen_random_uuid(),
        '{"_version":3,"profile":{"header":{"grade":"یازدهم"}}}'::jsonb, '{"header":true}'::jsonb);
    assert not (v ? 'error'), 'legacy v3 backup was rejected: ' || v::text;

    v := public.native_restore_backup_v3(gen_random_uuid(),
        '{"_version":9,"profile":{}}'::jsonb, '{}'::jsonb);
    assert v ? 'error', 'unsupported backup version was accepted';
    raise notice 'TEST 11 legacy/unsupported backup handling: PASS';
end $$;

-- ============================================================
-- تست ۱۲: دسترسی anon ممنوع است
-- ============================================================
do $$
begin
    assert not has_function_privilege('anon',
        'public.native_save_student_extra_v28(uuid,text,text,text,text)', 'execute'),
        'anon can execute student extra save';
    assert not has_function_privilege('anon',
        'public.native_save_class_v28(uuid,text,text,text)', 'execute'),
        'anon can execute class save';
    assert not has_function_privilege('anon',
        'public.native_save_profile_v28(text,text,boolean,text,text,text,text,text,text)', 'execute'),
        'anon can execute profile save';
    assert has_function_privilege('authenticated',
        'public.native_save_class_v28(uuid,text,text,text)', 'execute'),
        'authenticated cannot execute class save';
    raise notice 'TEST 12 function grants: PASS';
end $$;

select 'V28_INTEGRATION_ALL_PASS' as result;
