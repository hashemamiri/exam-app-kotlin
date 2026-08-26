-- V61.7 — دو اصلاح سروری:
-- ۱) پشتیبان‌گیری مدیر/معاون: خروجی JSON از مدرسه‌ها، معلم‌ها، کلاس‌ها و
--    دانش‌آموزان مدرسه (بدون رمز/توکن) برای کارت «داده‌ها»ی مدیر.
-- ۲) زمان‌سنج متوقف کد دعوت استفاده‌شده: used_at به خروجی لیست کدها اضافه
--    می‌شود تا کلاینت زمان باقی‌مانده در «لحظهٔ استفاده» را منجمد نشان دهد.

begin;

-- ------------------------------------------------------------
-- ۱) پشتیبان مدیر
-- ------------------------------------------------------------
create or replace function public.native_manager_export_backup_v61()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select case
  when not exists (select 1 from public.profiles where id=auth.uid() and role='manager')
    then jsonb_build_object('error','فقط مدیر/معاون دسترسی دارد')
  else jsonb_build_object(
    '_version', 1,
    '_kind', 'manager_school_backup',
    'exported_at', now(),
    'schools', coalesce((
        select jsonb_agg(jsonb_build_object(
            'name', s.name,
            'province', coalesce(s.province,''),
            'city', coalesce(s.city,''),
            'teachers', coalesce((
                select jsonb_agg(jsonb_build_object(
                    'full_name', p.full_name,
                    'username', coalesce(p.username,''),
                    'status', m.status
                ) order by p.full_name)
                from public.school_memberships m
                join public.profiles p on p.id = m.user_id
                where m.school_id = s.id and m.staff_role='teacher' and m.status <> 'removed'
            ), '[]'::jsonb),
            'classes', coalesce((
                select jsonb_agg(jsonb_build_object(
                    'name', c.name,
                    'grade', coalesce(c.grade,''),
                    'field_of_study', coalesce(c.field_of_study,''),
                    'teacher', coalesce((select t.full_name from public.profiles t where t.id=c.teacher_id),''),
                    'members', coalesce((
                        select jsonb_agg(jsonb_build_object(
                            'full_name', sp.full_name,
                            'username', coalesce(sp.username,'')
                        ) order by sp.full_name)
                        from public.class_members cm
                        join public.profiles sp on sp.id = cm.student_id
                        where cm.class_id = c.id
                    ), '[]'::jsonb)
                ) order by c.name)
                from public.classes c where c.school_id = s.id
            ), '[]'::jsonb),
            'students', coalesce((
                select jsonb_agg(jsonb_build_object(
                    'full_name', sp.full_name,
                    'username', coalesce(sp.username,''),
                    'grade', coalesce(sp.grade,''),
                    'field_of_study', coalesce(sp.field_of_study,''),
                    'gender', coalesce(sp.gender,'')
                ) order by sp.full_name)
                from public.school_students ss
                join public.profiles sp on sp.id = ss.student_id
                where ss.school_id = s.id
            ), '[]'::jsonb)
        ) order by s.name)
        from public.schools s
        where s.created_by = auth.uid()
           or exists (select 1 from public.school_memberships mm
                      where mm.school_id = s.id and mm.user_id = auth.uid()
                        and mm.staff_role = 'manager' and mm.status = 'active')
    ), '[]'::jsonb)
  )
end;
$$;
revoke all on function public.native_manager_export_backup_v61() from public, anon;
grant execute on function public.native_manager_export_backup_v61() to authenticated;

-- ------------------------------------------------------------
-- ۲) used_at در لیست کدهای دعوت
-- ------------------------------------------------------------
create or replace function public.native_manager_invites_v40b()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
 select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',i.id,'code',coalesce(i.display_code,'------'),'expires_at',i.expires_at,'used',i.used_at is not null,'used_at',coalesce(i.used_at::text,''),'revoked',i.revoked_at is not null) order by i.created_at desc),'[]'::jsonb)) from public.school_memberships me join public.school_teacher_invites i on i.school_id=me.school_id where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active' and i.created_at>now()-interval '7 days'),jsonb_build_object('error','دسترسی مدیر یافت نشد'));
$$;

commit;

-- سلامت‌سنجی پس از اجرا (هر دو باید true بدهند):
-- select to_regprocedure('public.native_manager_export_backup_v61()') is not null;
-- select position('used_at' in pg_get_functiondef('public.native_manager_invites_v40b()'::regprocedure)) > 0;
