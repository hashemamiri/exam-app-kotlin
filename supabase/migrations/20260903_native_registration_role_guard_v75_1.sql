-- V75.1 — گارد سمت سرور برای ارتقای نقش (بند ۲.۲ گزارش امنیتی).
-- ریشه: native_complete_manager_registration_v36 و
-- native_complete_teacher_registration_v1 فقط وضعیت پروفایل را چک می‌کردند و
-- نه انتخاب نقش در ثبت‌نام را و نه تأیید ایمیل را؛ بنابراین هر حساب تازه‌ای
-- می‌توانست با فراخوانی مستقیم RPC خود را «مدیر مدرسه» یا «معلم» کند.
-- این فایل فقط دو گارد اضافه می‌کند و منطق مالکیت/هم‌نامی را دست‌نخورده نگه می‌دارد.

begin;

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
    v_confirmed timestamptz;
    v_pending text := 'teacher';
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا کد ایمیل را تأیید کنید'); end if;
    if char_length(v_name) < 2 then return jsonb_build_object('error', 'نام و نام خانوادگی را وارد کنید'); end if;
    if v_username !~ '^[a-z0-9_]{4,20}$' then return jsonb_build_object('error', 'نام کاربری معتبر نیست'); end if;
    if char_length(v_school_name) < 2 then return jsonb_build_object('error', 'نام مدرسه را وارد کنید'); end if;

    select lower(coalesce(email, '')) into v_email from auth.users where id = v_uid;
    if coalesce(v_email, '') = '' or v_email like '%@student.exam.local' then
        return jsonb_build_object('error', 'ایمیل مدیریتی معتبر و تأییدشده نیست');
    end if;

    -- V75.1 — گارد سمت سرور ۱: ایمیل باید واقعاً تأیید شده باشد.
    select u.email_confirmed_at into v_confirmed from auth.users u where u.id = v_uid;
    if v_confirmed is null then
        return jsonb_build_object('error', 'ایمیل شما هنوز تأیید نشده است؛ ابتدا کد ایمیل را تأیید کنید');
    end if;

    -- V75.1 — گارد سمت سرور ۲: نقش انتخابی ثبت‌نام باید واقعاً «مدیر» باشد
    -- (قبلاً فقط رابط کاربر تصمیم می‌گرفت و هر حساب تازه می‌توانست مدیر شود).
    if to_regclass('public.native_registration_roles') is not null then
        select r.role into v_pending
        from public.native_registration_roles r
        where r.user_id = v_uid;
    end if;
    if coalesce(v_pending, 'teacher') <> 'manager' then
        select lower(coalesce(u.raw_user_meta_data->>'registration_role', 'teacher')) into v_pending
        from auth.users u
        where u.id = v_uid;
    end if;
    if coalesce(v_pending, 'teacher') <> 'manager' then
        return jsonb_build_object('error', 'این حساب برای ثبت‌نام مدیر انتخاب نشده است');
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

create or replace function public.native_complete_teacher_registration_v1(
    p_full_name text,
    p_username text
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
    v_profile public.profiles%rowtype;
    v_confirmed timestamptz;
begin
    if v_uid is null then
        return jsonb_build_object('error', 'ابتدا کد ایمیل را تأیید کنید');
    end if;
    if v_name = '' then
        return jsonb_build_object('error', 'نام و نام خانوادگی را وارد کنید');
    end if;
    if v_username !~ '^[a-z0-9_]{4,20}$' then
        return jsonb_build_object('error', 'نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد');
    end if;

    select lower(coalesce(u.email, '')) into v_email
    from auth.users u
    where u.id = v_uid;
    if coalesce(v_email, '') = '' then
        return jsonb_build_object('error', 'ایمیل تأییدشده پیدا نشد');
    end if;

    -- V75.1 — ارتقای نقش به معلم فقط با ایمیلِ واقعاً تأییدشده.
    select u.email_confirmed_at into v_confirmed from auth.users u where u.id = v_uid;
    if v_confirmed is null then
        return jsonb_build_object('error', 'ایمیل شما هنوز تأیید نشده است؛ ابتدا کد ایمیل را تأیید کنید');
    end if;
    if v_email like '%@student.exam.local' then
        return jsonb_build_object('error', 'حساب دانش‌آموز توسط معلم مدیریت می‌شود');
    end if;

    perform pg_advisory_xact_lock(hashtext('native-teacher-username:' || v_username));
    if exists (
        select 1 from public.profiles p
        where lower(coalesce(p.username, '')) = v_username
          and p.id <> v_uid
    ) then
        return jsonb_build_object('error', 'این نام کاربری قبلاً استفاده شده است');
    end if;

    insert into public.profiles(id, full_name, display_name, username, role)
    values (v_uid, v_name, v_name, v_username, 'student')
    on conflict (id) do nothing;

    select * into v_profile
    from public.profiles p
    where p.id = v_uid
    for update;

    if v_profile.id is null then
        return jsonb_build_object('error', 'پروفایل حساب پیدا نشد');
    end if;
    if v_profile.role = 'student' and v_profile.teacher_id is not null then
        return jsonb_build_object('error', 'حساب دانش‌آموز نمی‌تواند به حساب معلم تبدیل شود');
    end if;
    if v_profile.role not in ('student', 'teacher') then
        return jsonb_build_object('error', 'نقش حساب معتبر نیست');
    end if;
    if exists (
        select 1 from public.class_members m where m.student_id = v_uid
    ) then
        return jsonb_build_object('error', 'حساب عضو کلاس نمی‌تواند به حساب معلم تبدیل شود');
    end if;

    update public.profiles
    set full_name = v_name,
        display_name = coalesce(nullif(display_name, ''), v_name),
        username = v_username,
        role = 'teacher',
        teacher_id = null
    where id = v_uid;

    return jsonb_build_object(
        'ok', true,
        'id', v_uid,
        'full_name', v_name,
        'username', v_username,
        'role', 'teacher'
    );
end;
$$;

revoke all on function public.native_complete_teacher_registration_v1(text,text) from public, anon;
grant execute on function public.native_complete_teacher_registration_v1(text,text) to authenticated;

commit;
