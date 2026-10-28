-- V335.53 — رفع ایراد ۴.۱ گزارش امنیتی: حذف ایمیل از پاسخ + REVOKE از anon
-- اجرا در Supabase SQL Editor

create or replace function public.native_staff_login_email_v1(p_username text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_username text := lower(btrim(coalesce(p_username, '')));
    v_exists boolean := false;
begin
    if v_username !~ '^[a-z0-9_]{4,20}$' then
        return jsonb_build_object('ok', false);
    end if;

    delete from public.native_staff_login_attempts
    where attempted_at < now() - interval '2 hours';

    -- ثبت تلاش (برای سقف نرخ/mapدنام کاربری)
    insert into public.native_staff_login_attempts(username) values (v_username);

    -- فقط وجود/عدم‌وجود — ایمیل برنمی‌گردد
    select exists (
        select 1 from public.profiles p
        join auth.users u on u.id = p.id
        where lower(coalesce(p.username, '')) = v_username
          and p.role in ('teacher', 'manager')
        limit 1
    ) into v_exists;

    if v_exists then
        return jsonb_build_object('ok', true);
    else
        return jsonb_build_object('ok', false);
    end if;
end;
$$;

revoke all on function public.native_staff_login_email_v1(text) from public;
grant execute on function public.native_staff_login_email_v1(text) to authenticated;
