-- V60.0 — ۱) ورود معلم/مدیر با نام کاربری: نگاشت username → ایمیل واقعی Auth.
--          ۲) پشتیبانی ثبت‌نام با گوگل (نقش موقت در metadata خوانده می‌شود —
--             جریان تکمیل ثبت‌نام موجود v12 همان را ادامه می‌دهد).

create or replace function public.native_staff_login_email_v1(p_username text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_username text := lower(btrim(coalesce(p_username, '')));
    v_email text;
begin
    if v_username !~ '^[a-z0-9_]{4,20}$' then
        return jsonb_build_object('error', 'نام کاربری نامعتبر است');
    end if;
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

-- ثبت نقش انتخابی ثبت‌نام گوگل روی metadata کاربر لاگین‌شده (برای pending_role در v12).
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
    update auth.users
    set raw_user_meta_data = coalesce(raw_user_meta_data, '{}'::jsonb)
        || jsonb_build_object('registration_role', lower(p_role))
    where id = v_uid;
    return jsonb_build_object('ok', true);
end;
$$;
revoke all on function public.native_set_registration_role_v1(text) from public, anon;
grant execute on function public.native_set_registration_role_v1(text) to authenticated;
