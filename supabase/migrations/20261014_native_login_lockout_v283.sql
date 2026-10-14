-- V283 — قفل ورود ناموفق: ۵ رمز اشتباه در ۱۵ دقیقه → همان حساب ۱۵ دقیقه قفل (معلم، مدیر، دانش‌آموز).
-- چرا این‌طور: هوک «Password Verification Attempt» سوپابیس فقط در پلن Teams/Enterprise است؛ این تابع
-- جایگزین رایگان آن است: پیش از ورود واقعی، رمز با bcrypt همان auth.users مقایسه و شکست‌ها شمرده می‌شود.
-- پس از ۵ شکست، علاوه بر شمارنده، auth.users.banned_until هم ۱۵ دقیقه جلو می‌رود تا ورود مستقیم هم بسته باشد.
-- اجرای دستی یک‌بار در SQL Editor. نیاز: pgcrypto (در سوپابیس پیش‌فرض در schema extensions فعال است).

begin;

create table if not exists public.native_login_failures(
  id bigserial primary key,
  identifier text not null,
  failed_at timestamptz not null default now()
);
create index if not exists native_login_failures_idx on public.native_login_failures(identifier, failed_at);
alter table public.native_login_failures enable row level security; -- هیچ policy: فقط تابع security definer

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
  v_max constant int := 5;
  v_window constant interval := interval '15 minutes';
begin
  if v_id = '' or coalesce(p_password, '') = '' then
    return jsonb_build_object('error', 'نام کاربری و رمز عبور را وارد کنید.');
  end if;

  delete from public.native_login_failures where failed_at < now() - interval '2 hours';

  -- شمارش شکست‌های ۱۵ دقیقهٔ اخیر برای همین شناسه
  select count(*), min(failed_at) into v_fail, v_oldest
  from public.native_login_failures
  where identifier = v_id and failed_at > now() - v_window;

  if v_fail >= v_max then
    v_retry := greatest(1, ceil(extract(epoch from (v_oldest + v_window - now())))::int);
    return jsonb_build_object('error', 'به‌خاطر چند بار رمز اشتباه، ورود این حساب تا ' || ceil(v_retry / 60.0)::int || ' دقیقهٔ دیگر بسته است.',
                              'locked', true, 'retry_after', v_retry);
  end if;

  -- شناسه → ایمیل: ایمیل مستقیم / نام کاربری معلم‌ومدیر / نام کاربری دانش‌آموز
  if position('@' in v_id) > 0 then
    v_email := v_id;
  elsif v_id ~ '^[a-z0-9_]{4,20}$' then
    select u.email into v_email
    from public.profiles p join auth.users u on u.id = p.id
    where lower(coalesce(p.username, '')) = v_id and p.role in ('teacher', 'manager')
    limit 1;
    if v_email is null then v_email := v_id || '@student.exam.local'; end if;
  else
    return jsonb_build_object('error', 'نام کاربری نامعتبر است.');
  end if;

  select * into v_user from auth.users where lower(email) = lower(v_email) limit 1;

  if v_user.id is not null and v_user.encrypted_password is not null and v_user.encrypted_password <> ''
     and extensions.crypt(p_password, v_user.encrypted_password) = v_user.encrypted_password then
    delete from public.native_login_failures where identifier = v_id;
    return jsonb_build_object('ok', true, 'email', v_user.email);
  end if;

  -- شکست (حساب ناموجود هم شمرده می‌شود تا شمارش نام‌ها ممکن نباشد)
  insert into public.native_login_failures(identifier) values (v_id);
  v_fail := v_fail + 1;
  if v_fail >= v_max then
    if v_user.id is not null then
      begin
        update auth.users set banned_until = now() + v_window where id = v_user.id;
      exception when others then null; -- اگر دسترسی نبود، شمارندهٔ خودمان کافی است
      end;
    end if;
    return jsonb_build_object('error', 'به‌خاطر ۵ بار رمز اشتباه، ورود این حساب ۱۵ دقیقه بسته شد.', 'locked', true, 'retry_after', 900);
  end if;
  return jsonb_build_object('error', 'ایمیل/نام کاربری یا رمز عبور نادرست است.', 'remaining', v_max - v_fail);
end;
$$;

revoke all on function public.native_login_gate_v283(text, text) from public;
grant execute on function public.native_login_gate_v283(text, text) to anon, authenticated;

commit;
