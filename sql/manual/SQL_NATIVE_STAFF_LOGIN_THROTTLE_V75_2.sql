-- V75.2 — محدودسازی افشای ایمیل کارکنان (بند ۳.۱ گزارش امنیتی).
-- ریشه: native_staff_login_email_v1 به anon مجاز است و از auth.users ایمیل کامل
-- معلم/مدیر را برمی‌گرداند؛ بدون محدودیت نرخ، مهاجم می‌توانست نام‌های کاربری را
-- جست‌وجو و ایمیل ورود را برای فیشینگ/حملهٔ رمز برداشت کند.
-- نکته: تابع برای ورود با نام کاربری لازم است (کلاینت باید ایمیل را بشناسد)،
-- بنابراین به‌جای حذف آن، نرخ فراخوانی محدود و تلاش‌ها ثبت می‌شود.

begin;

create table if not exists public.native_staff_login_attempts (
    id bigserial primary key,
    username text not null,
    attempted_at timestamptz not null default now()
);

create index if not exists idx_native_staff_login_attempts_username
    on public.native_staff_login_attempts(username, attempted_at desc);
create index if not exists idx_native_staff_login_attempts_time
    on public.native_staff_login_attempts(attempted_at desc);

alter table public.native_staff_login_attempts enable row level security;

create or replace function public.native_staff_login_email_v1(p_username text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_username text := lower(btrim(coalesce(p_username, '')));
    v_email text;
    v_recent integer := 0;
    v_global integer := 0;
begin
    if v_username !~ '^[a-z0-9_]{4,20}$' then
        return jsonb_build_object('error', 'نام کاربری نامعتبر است');
    end if;

    -- V75.2 — محدودسازی نرخ: شمارش نام‌های کاربری (Username Enumeration) کند و
    -- پرهزینه می‌شود. جدول تلاش‌ها فقط توسط همین تابعsecurity definer نوشته می‌شود.
    delete from public.native_staff_login_attempts
    where attempted_at < now() - interval '2 hours';

    select count(*) into v_recent
    from public.native_staff_login_attempts
    where username = v_username and attempted_at > now() - interval '10 minutes';

    select count(*) into v_global
    from public.native_staff_login_attempts
    where attempted_at > now() - interval '1 minute';

    if v_recent >= 5 or v_global >= 20 then
        return jsonb_build_object('error', 'تلاش‌های ورود بیش از حد است؛ چند دقیقه دیگر دوباره تلاش کنید');
    end if;

    insert into public.native_staff_login_attempts(username) values (v_username);
    select u.email into v_email
    from public.profiles p
    join auth.users u on u.id = p.id
    where lower(coalesce(p.username, '')) = v_username
      and p.role in ('teacher', 'manager')
    limit 1;
    if v_email is null then
        -- برای جلوگیری از شمارش نام‌های کاربری، همان پیام ورود ناموفق برمی‌گردد.
        return jsonb_build_object('error', 'ایمیل/نام کاربری یا رمز عبور نادرست است.');
    end if;
    return jsonb_build_object('ok', true, 'email', v_email);
end;
$$;

revoke all on function public.native_staff_login_email_v1(text) from public;
grant execute on function public.native_staff_login_email_v1(text) to anon, authenticated;

commit;
