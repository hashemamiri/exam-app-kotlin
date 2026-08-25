-- V60.2 — رفع ثبت‌نام گوگل مدیر که حساب معلم می‌ساخت.
-- ریشه: تابع قبلی روی auth.users UPDATE می‌زد؛ در Supabase مالک توابع
-- security definer اجازهٔ UPDATE روی auth.users را ندارد و خطا خورده و نقش
-- ثبت نمی‌شد (پیش‌فرض teacher می‌ماند). حالا نقش در جدول public خودمان
-- ذخیره و در state ثبت‌نام خوانده می‌شود.

create table if not exists public.native_registration_roles (
    user_id uuid primary key references auth.users(id) on delete cascade,
    role text not null check (role in ('teacher','manager')),
    created_at timestamptz not null default now()
);
alter table public.native_registration_roles enable row level security;
drop policy if exists native_registration_roles_own on public.native_registration_roles;
create policy native_registration_roles_own on public.native_registration_roles
    for all using (user_id = auth.uid()) with check (user_id = auth.uid());

create or replace function public.native_set_registration_role_v1(p_role text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if lower(coalesce(p_role,'')) not in ('teacher','manager') then
        return jsonb_build_object('error', 'نقش نامعتبر است');
    end if;
    insert into public.native_registration_roles(user_id, role)
    values (v_uid, lower(p_role))
    on conflict (user_id) do update set role = excluded.role, created_at = now();
    return jsonb_build_object('ok', true);
end;
$$;
revoke all on function public.native_set_registration_role_v1(text) from public, anon;
grant execute on function public.native_set_registration_role_v1(text) to authenticated;

-- state ثبت‌نام: pending_role اول از جدول ما، بعد metadata (سازگاری عقب‌رو).
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
                when lower(coalesce(
                        (select r.role from public.native_registration_roles r where r.user_id = p.id),
                        u.raw_user_meta_data->>'registration_role',
                        'teacher')) = 'manager'
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
