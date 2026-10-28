-- V335.53 — رفع ایراد ۴.۲ گزارش امنیتی: حذف remaining و email + سقف روزانه
-- اجرا در Supabase SQL Editor

create or replace function public.native_login_gate_v283(p_identifier text, p_password text)
returns jsonb
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $$
declare
  v_id text := lower(btrim(coalesce(p_identifier, '')));
  v_email text;
  v_user auth.users%rowtype;
  v_fail int := 0;
  v_oldest timestamptz;
  v_retry int;
  v_daily_fail int := 0;
  v_max constant int := 5;
  v_window constant interval := interval '15 minutes';
  v_daily_max constant int := 30;
begin
  if v_id = '' or coalesce(p_password, '') = '' then
    return jsonb_build_object('ok', false);
  end if;

  delete from public.native_login_failures where failed_at < now() - interval '2 hours';

  -- سقف ۱۵ دقیقهٔ اخیر (بدون remaining)
  select count(*), min(failed_at) into v_fail, v_oldest
  from public.native_login_failures
  where identifier = v_id and failed_at > now() - v_window;

  if v_fail >= v_max then
    v_retry := greatest(1, ceil(extract(epoch from (v_oldest + v_window - now())))::int);
    return jsonb_build_object('ok', false, 'locked', true, 'retry_after', v_retry);
  end if;

  -- سقف روزانه جدید (۳۰ شکست در ۲۴ ساعت)
  select count(*) into v_daily_fail
  from public.native_login_failures
  where identifier = v_id and failed_at > now() - interval '24 hours';

  if v_daily_fail >= v_daily_max then
    return jsonb_build_object('ok', false, 'locked', true, 'retry_after', 86400);
  end if;

  -- resolved identifier → email (فقط داخلی، پاسخ بیرون نمی‌رود)
  if position('@' in v_id) > 0 then
    v_email := v_id;
  elsif v_id ~ '^[a-z0-9_]{4,20}$' then
    select u.email into v_email
    from public.profiles p join auth.users u on u.id = p.id
    where lower(coalesce(p.username, '')) = v_id and p.role in ('teacher', 'manager')
    limit 1;
    if v_email is null then v_email := v_id || '@student.exam.local'; end if;
  else
    return jsonb_build_object('ok', false);
  end if;

  select * into v_user from auth.users where lower(email) = lower(v_email) limit 1;

  if v_user.id is not null and v_user.encrypted_password is not null
     and extensions.crypt(p_password, v_user.encrypted_password) = v_user.encrypted_password then
    delete from public.native_login_failures where identifier = v_id;
    return jsonb_build_object('ok', true);  -- ایمیل حذف شد
  end if;

  -- شکست: پیام یکسان، بدون 'remaining'
  insert into public.native_login_failures(identifier) values (v_id);
  return jsonb_build_object('ok', false);
end;
$$;

revoke all on function public.native_login_gate_v283(text, text) from public;
grant execute on function public.native_login_gate_v283(text, text) to anon, authenticated;
