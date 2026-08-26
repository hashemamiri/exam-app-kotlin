-- V61.5 — متادادهٔ فیلتر لیست دانش‌آموزان:
-- برای فیلترهای «مدرسه» (دانش‌آموزِ ثبت‌شده در مدرسه‌های کاربر) و «معلم»
-- (فقط پنل مدیر) به ازای هر دانش‌آموز، معلم مالک و وضعیت عضویت مدرسه لازم
-- است که در my_students نبود. تابع فقط دانش‌آموزانِ در دسترس کاربر را می‌دهد.

begin;

create or replace function public.native_student_filter_meta_v61()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
with my_schools as (
    select m.school_id from public.school_memberships m
    where m.user_id = auth.uid() and m.status = 'active'
    union
    select s.id from public.schools s where s.created_by = auth.uid()
)
select jsonb_build_object('ok', true, 'items', coalesce((
    select jsonb_agg(jsonb_build_object(
        'id', p.id,
        'teacher_id', coalesce(p.teacher_id::text, ''),
        'teacher_name', coalesce((select t.full_name from public.profiles t where t.id = p.teacher_id), ''),
        'in_school', exists (
            select 1 from public.school_students ss
            join my_schools ms on ms.school_id = ss.school_id
            where ss.student_id = p.id
        )
    ))
    from public.profiles p
    where p.role = 'student'
      and (
        p.teacher_id = auth.uid()
        or exists (select 1 from public.teacher_student_links l
                   where l.teacher_id = auth.uid() and l.student_id = p.id)
        or exists (select 1 from public.school_students ss
                   join my_schools ms on ms.school_id = ss.school_id
                   where ss.student_id = p.id)
      )
), '[]'::jsonb));
$$;
revoke all on function public.native_student_filter_meta_v61() from public, anon;
grant execute on function public.native_student_filter_meta_v61() to authenticated;

commit;

-- سلامت‌سنجی پس از اجرا (باید true بدهد):
-- select to_regprocedure('public.native_student_filter_meta_v61()') is not null;
