-- V36: نقش مدیر/معاون و مدرسهٔ تک‌مستأجره؛ پایهٔ V37/V38.
-- این migration هیچ دسترسی بین‌مدرسه‌ای، انتقال کیف پول یا مدیریت معلم ایجاد نمی‌کند.
begin;

-- محدودیت قدیمی role فقط student/teacher را می‌پذیرد؛ constraintهای CHECK مرتبط
-- به role به‌صورت نام‌مستقل حذف و یک constraint صریح سه‌نقشی جایگزین می‌شوند.
do $$
declare r record;
begin
    for r in
        select c.conname
        from pg_constraint c
        join pg_class t on t.oid = c.conrelid
        join pg_namespace n on n.oid = t.relnamespace
        where n.nspname = 'public' and t.relname = 'profiles' and c.contype = 'c'
          and pg_get_constraintdef(c.oid) ilike '%role%'
    loop
        execute format('alter table public.profiles drop constraint %I', r.conname);
    end loop;
end;
$$;

alter table public.profiles
    add constraint profiles_role_v36_check check (role in ('student','teacher','manager'));

create table if not exists public.schools (
    id uuid primary key default gen_random_uuid(),
    name text not null check (char_length(btrim(name)) between 2 and 160),
    province text not null default '' check (char_length(province) <= 100),
    city text not null default '' check (char_length(city) <= 100),
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.school_memberships (
    school_id uuid not null references public.schools(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    staff_role text not null check (staff_role in ('manager','teacher')),
    status text not null default 'active' check (status in ('active','disabled')),
    invited_by uuid references auth.users(id) on delete set null,
    joined_at timestamptz not null default now(),
    primary key (school_id, user_id)
);
create unique index if not exists ux_school_one_active_membership_v36
    on public.school_memberships(user_id) where status = 'active';
create index if not exists idx_school_members_v36
    on public.school_memberships(school_id, staff_role, status);

alter table public.schools enable row level security;
alter table public.school_memberships enable row level security;

-- در V36 فقط خود عضو دادهٔ مدرسهٔ خودش را می‌خواند؛ مدیریت اعضا در V37 از RPC
-- امنیت‌تعریف‌شده و auditشده انجام می‌شود.
drop policy if exists v36_school_member_read on public.schools;
create policy v36_school_member_read on public.schools
for select to authenticated using (
    exists (
        select 1 from public.school_memberships sm
        where sm.school_id = schools.id and sm.user_id = auth.uid() and sm.status = 'active'
    )
);
drop policy if exists v36_membership_self_read on public.school_memberships;
create policy v36_membership_self_read on public.school_memberships
for select to authenticated using (user_id = auth.uid());

-- تکمیل ثبت‌نام مدیر پس از OTP ایمیل. هر مدیر یک مدرسهٔ مستقل تازه می‌سازد.
create or replace function public.native_complete_manager_registration_v36(
    p_full_name text,
    p_username text,
    p_school_name text,
    p_province text,
    p_city text
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
    v_school_name text := left(btrim(coalesce(p_school_name, '')), 160);
    v_province text := left(btrim(coalesce(p_province, '')), 100);
    v_city text := left(btrim(coalesce(p_city, '')), 100);
    v_profile public.profiles%rowtype;
    v_school uuid;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا کد ایمیل را تأیید کنید'); end if;
    if char_length(v_name) < 2 then return jsonb_build_object('error', 'نام و نام خانوادگی را وارد کنید'); end if;
    if v_username !~ '^[a-z0-9_]{4,20}$' then return jsonb_build_object('error', 'نام کاربری معتبر نیست'); end if;
    if char_length(v_school_name) < 2 then return jsonb_build_object('error', 'نام مدرسه را وارد کنید'); end if;

    select lower(coalesce(email, '')) into v_email from auth.users where id = v_uid;
    if coalesce(v_email, '') = '' or v_email like '%@student.exam.local' then
        return jsonb_build_object('error', 'ایمیل مدیریتی معتبر و تأییدشده نیست');
    end if;

    perform pg_advisory_xact_lock(hashtext('native-staff-username:' || v_username));
    if exists (select 1 from public.profiles where lower(coalesce(username,'')) = v_username and id <> v_uid) then
        return jsonb_build_object('error', 'این نام کاربری قبلاً استفاده شده است');
    end if;

    insert into public.profiles(id, full_name, display_name, username, role)
    values (v_uid, v_name, v_name, v_username, 'student')
    on conflict (id) do nothing;
    select * into v_profile from public.profiles where id = v_uid for update;
    if v_profile.teacher_id is not null or v_profile.role not in ('student','manager') then
        return jsonb_build_object('error', 'این حساب قابل تبدیل به مدیر/معاون نیست');
    end if;
    if exists (select 1 from public.class_members where student_id = v_uid) then
        return jsonb_build_object('error', 'حساب عضو کلاس قابل تبدیل به مدیر نیست');
    end if;
    if exists (select 1 from public.school_memberships where user_id = v_uid and status = 'active') then
        return jsonb_build_object('error', 'این حساب قبلاً عضو یک مدرسه است');
    end if;

    update public.profiles
    set full_name = v_name, display_name = coalesce(nullif(display_name,''), v_name),
        username = v_username, role = 'manager', teacher_id = null
    where id = v_uid;

    insert into public.schools(name, province, city, created_by)
    values (v_school_name, v_province, v_city, v_uid)
    returning id into v_school;
    insert into public.school_memberships(school_id, user_id, staff_role, status, invited_by)
    values (v_school, v_uid, 'manager', 'active', v_uid);

    return jsonb_build_object(
        'ok', true, 'id', v_uid, 'full_name', v_name, 'username', v_username,
        'role', 'manager', 'school_id', v_school, 'school_name', v_school_name
    );
end;
$$;
revoke all on function public.native_complete_manager_registration_v36(text,text,text,text,text) from public, anon;
grant execute on function public.native_complete_manager_registration_v36(text,text,text,text,text) to authenticated;

-- حالت ثبت‌نام از metadata امن نشست OTP خوانده می‌شود تا پس از بسته‌شدن برنامه نیز
-- setup صحیح معلم یا مدیر ادامه پیدا کند.
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
            'pending_role', case
                when lower(coalesce(u.raw_user_meta_data->>'registration_role','teacher')) = 'manager'
                    then 'manager' else 'teacher' end,
            'requires_teacher_setup',
                p.role = 'student' and p.teacher_id is null
                and lower(coalesce(u.email, '')) not like '%@student.exam.local'
        )
        from public.profiles p join auth.users u on u.id = p.id
        where p.id = auth.uid()
    ), jsonb_build_object('error', 'پروفایل حساب پیدا نشد'));
$$;
revoke all on function public.native_my_registration_state_v1() from public, anon;
grant execute on function public.native_my_registration_state_v1() to authenticated;

-- نام کاربری برای هر دو نقش کادر مدرسه قابل ویرایش است؛ ورود همچنان با ایمیل است.
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
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if v_username !~ '^[a-z0-9_]{4,20}$' then return jsonb_build_object('error', 'نام کاربری معتبر نیست'); end if;
    perform pg_advisory_xact_lock(hashtext('native-staff-username:' || v_username));
    if exists (select 1 from public.profiles where lower(coalesce(username,''))=v_username and id<>v_uid) then
        return jsonb_build_object('error', 'این نام کاربری قبلاً استفاده شده است');
    end if;
    update public.profiles set username=v_username
    where id=v_uid and role in ('teacher','manager') and teacher_id is null;
    get diagnostics v_count = row_count;
    if v_count <> 1 then return jsonb_build_object('error', 'تغییر نام کاربری مجاز نیست'); end if;
    return jsonb_build_object('ok', true, 'username', v_username);
end;
$$;
revoke all on function public.native_update_my_username_v1(text) from public, anon;
grant execute on function public.native_update_my_username_v1(text) to authenticated;

-- نمای پایهٔ آمار؛ جزئیات آموزشی کامل در V38 افزوده می‌شود.
create or replace function public.native_manager_school_summary_v36()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select coalesce((
        select jsonb_build_object(
            'ok', true, 'school_id', s.id, 'school_name', s.name,
            'province', s.province, 'city', s.city,
            'teachers', (select count(*) from public.school_memberships x where x.school_id=s.id and x.staff_role='teacher' and x.status='active'),
            'students', 0, 'classes', 0, 'exams', 0
        )
        from public.school_memberships me join public.schools s on s.id=me.school_id
        where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'
    ), jsonb_build_object('error', 'مدرسهٔ فعال پیدا نشد'));
$$;
revoke all on function public.native_manager_school_summary_v36() from public, anon;
grant execute on function public.native_manager_school_summary_v36() to authenticated;

commit;
