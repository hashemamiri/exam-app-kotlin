-- V60.3 — رفع «گوگل مدیر مستقیم وارد پنل معلم می‌شود».
-- ریشه: trigger قدیمی وب‌اپ، profile هر حساب ایمیلی تازه را با role='teacher'
-- می‌سازد؛ پس requires_teacher_setup که فقط role='student' را چک می‌کرد false
-- می‌شد و کاربر گوگلی تازه بدون صفحهٔ تکمیل، مستقیم «معلم» می‌ماند (مسیر OTP
-- در V38.1 دور زده شده بود، مسیر گوگل نه).
-- راه‌حل: «معلم خالی» (هیچ کلاس/آزمون/دانش‌آموز/عضویت/نام کاربری) که نقش
-- انتخابی ثبت‌نام (native_registration_roles) دارد نیز نیازمند setup است.

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
                lower(coalesce(u.email, '')) not like '%@student.exam.local'
                and (
                    -- حالت قدیمی: هنوز student است و مالکی ندارد
                    (p.role = 'student' and p.teacher_id is null)
                    -- V60.3: معلمِ خالیِ ساختهٔ trigger که ثبت‌نامش را (با گوگل)
                    -- شروع کرده ولی تکمیل نکرده است
                    or (
                        p.role = 'teacher'
                        and coalesce(p.username, '') = ''
                        and exists (select 1 from public.native_registration_roles r
                                    where r.user_id = p.id)
                        and not exists (select 1 from public.classes c where c.teacher_id = p.id)
                        and not exists (select 1 from public.exams e where e.teacher_id = p.id)
                        and not exists (select 1 from public.profiles s
                                        where s.teacher_id = p.id and s.role = 'student')
                        and not exists (select 1 from public.school_memberships sm
                                        where sm.user_id = p.id and sm.status = 'active')
                    )
                )
        )
        from public.profiles p join auth.users u on u.id = p.id
        where p.id = auth.uid()
    ), jsonb_build_object('error', 'پروفایل حساب پیدا نشد'));
$$;
revoke all on function public.native_my_registration_state_v1() from public, anon;
grant execute on function public.native_my_registration_state_v1() to authenticated;

-- سلامت‌سنجی پس از اجرا (باید true بدهد):
-- select position('native_registration_roles' in pg_get_functiondef('public.native_my_registration_state_v1()'::regprocedure)) > 0;
