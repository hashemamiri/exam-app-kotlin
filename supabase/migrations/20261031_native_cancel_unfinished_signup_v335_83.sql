-- V335.83 — پاک‌شدن حسابِ نیمه‌کارهٔ ثبت‌نام با گوگل.
--
-- مشکل: با «ورود/ثبت‌نام با گوگل»، سوپابیس در همان لحظهٔ بازگشت از گوگل
-- یک رکورد در auth.users می‌سازد؛ اگر کاربر در گام بعدی (انتخاب نام کاربری و
-- رمز) منصرف شود، آن رکورد تا ابد در فهرست کاربران می‌ماند.
--
-- راه‌حل: سه تابع.
--   ۱) native_signup_empty_v1(uid)          — آیا حساب «خالی و نیمه‌کاره» است؟
--   ۲) native_cancel_unfinished_signup_v1() — خودِ کاربر حساب نیمه‌کارهٔ خودش را پاک می‌کند.
--   ۳) native_purge_abandoned_signups_v1()  — پاک‌سازی دوره‌ای حساب‌های رهاشده (service_role/cron).
--
-- حساب «نیمه‌کاره» = نقش دانش‌آموز که هنوز به معلمی وصل نشده (همان requires_teacher_setup)،
-- ایمیل واقعی دارد (نه دانش‌آموز ساخته‌شده با نام کاربری) و هیچ آزمون یا آزمون چاپی ندارد.
-- بنابراین هیچ دادهٔ درسی در خطر نیست؛ هر آزمون/کلاس/برنامه‌ای که ساخته شود، حساب را از این وضعیت خارج می‌کند.

-- ۱) آیا این حساب هنوز خالی و نیمه‌کاره است؟
create or replace function public.native_signup_empty_v1(p_uid uuid)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select coalesce((
        select (p.role = 'student'
                and p.teacher_id is null
                and lower(coalesce(u.email, '')) not like '%@student.exam.local'
                and not exists (select 1 from public.exams e where e.teacher_id = p_uid)
                and not exists (select 1 from public.print_exams pe where pe.teacher_id = p_uid))
        from public.profiles p
        join auth.users u on u.id = p.id
        where p.id = p_uid
    ), false);
$$;
revoke all on function public.native_signup_empty_v1(uuid) from public, anon, authenticated;

-- ۲) انصراف خودِ کاربر: فقط حساب نیمه‌کارهٔ کمتر از ۴۸ ساعت پیش.
create or replace function public.native_cancel_unfinished_signup_v1()
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_created timestamptz;
    v_empty boolean;
begin
    if v_uid is null then
        return jsonb_build_object('ok', false, 'reason', 'no-session');
    end if;

    select u.created_at into v_created from auth.users u where u.id = v_uid;
    v_empty := public.native_signup_empty_v1(v_uid);

    if not coalesce(v_empty, false) then
        return jsonb_build_object('ok', false, 'reason', 'not-unfinished',
                                  'message', 'این حساب داده دارد؛ از مسیر «حذف حساب» استفاده کنید.');
    end if;
    if v_created is null or v_created < now() - interval '48 hours' then
        return jsonb_build_object('ok', false, 'reason', 'too-old',
                                  'message', 'این حساب قدیمی‌تر از ۴۸ ساعت است؛ از مسیر «حذف حساب» استفاده کنید.');
    end if;

    delete from public.profiles where id = v_uid;
    delete from auth.users where id = v_uid;
    return jsonb_build_object('ok', true, 'deleted', true);
exception when others then
    return jsonb_build_object('ok', false, 'reason', sqlerrm);
end;
$$;
revoke all on function public.native_cancel_unfinished_signup_v1() from public, anon;
grant execute on function public.native_cancel_unfinished_signup_v1() to authenticated;

-- ۳) پاک‌سازی دوره‌ای: حساب‌های نیمه‌کاره‌ای که از p_hours ساعت پیش ساخته شده‌اند و
--    هیچ‌وقت تکمیل نشده‌اند. فقط service_role (یا اجرای دستی در SQL Editor) اجازه دارد.
create or replace function public.native_purge_abandoned_signups_v1(
    p_hours int default 48,
    p_limit int default 500
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_role text;
    v_ids uuid[];
    v_deleted int := 0;
    v_id uuid;
begin
    v_role := coalesce(nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'role', '');
    if not (v_role = 'service_role' or current_user in ('postgres', 'supabase_admin')) then
        return jsonb_build_object('ok', false, 'reason', 'forbidden');
    end if;

    select array_agg(c.id) into v_ids from (
        select u.id
        from auth.users u
        where u.created_at < now() - make_interval(hours => greatest(coalesce(p_hours, 48), 1))
          and public.native_signup_empty_v1(u.id)
        order by u.created_at
        limit greatest(coalesce(p_limit, 500), 1)
    ) c;

    if v_ids is null then
        return jsonb_build_object('ok', true, 'deleted', 0);
    end if;

    foreach v_id in array v_ids loop
        begin
            delete from public.profiles where id = v_id;
            delete from auth.users where id = v_id;
            v_deleted := v_deleted + 1;
        exception when others then
            null; -- ردیف بعدی؛ یک خطا کل پاک‌سازی را متوقف نمی‌کند
        end;
    end loop;

    return jsonb_build_object('ok', true, 'deleted', v_deleted);
end;
$$;
revoke all on function public.native_purge_abandoned_signups_v1(int, int) from public, anon, authenticated;
grant execute on function public.native_purge_abandoned_signups_v1(int, int) to service_role;

-- (اختیاری) زمان‌بندی روزانه — اگر pg_cron روی پروژه فعال است، این خط را یک بار اجرا کنید:
--   select cron.schedule('purge-abandoned-signups', '30 3 * * *',
--                        $$ select public.native_purge_abandoned_signups_v1(48, 500); $$);
-- بدون pg_cron هم می‌توانید هر وقت خواستید همین تابع را در SQL Editor صدا بزنید:
--   select public.native_purge_abandoned_signups_v1(48, 500);
