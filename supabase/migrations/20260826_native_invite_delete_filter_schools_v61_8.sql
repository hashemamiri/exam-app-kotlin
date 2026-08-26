-- V61.8 — سه اصلاح سروری:
-- ۱) حذف «پایدار» کارت کد دعوت: تابع قبلی revoke فقط سطر را باطل می‌کرد و
--    لیست سرور همچنان آن را برمی‌گرداند؛ با هر بازشدن صفحه کارت «برمی‌گشت».
--    تابع جدید سطر را حذف می‌کند (حذف = ابطال فوری کد استفاده‌نشده) و
--    چندمدرسه‌ای است (revoke قدیمی فقط اولین عضویت مدیر را می‌دید).
-- ۲) زمان‌سنج منجمد: used_at با فرمت ISO (به‌جای ::text با فاصله) تا کلاینت
--    بتواند parse کند.
-- ۳) فیلتر «مدرسه»: متادادهٔ فیلتر حالا آرایهٔ school_ids هر دانش‌آموز را
--    می‌دهد تا انتخاب مدرسهٔ خاص از لیست ممکن شود.

begin;

-- ------------------------------------------------------------
-- ۱) حذف پایدار کد دعوت (چندمدرسه‌ای)
-- ------------------------------------------------------------
create or replace function public.native_manager_delete_invite_v61(p_invite uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_count int;
begin
    delete from public.school_teacher_invites i
    where i.id = p_invite
      and exists (
          select 1 from public.school_memberships m
          where m.school_id = i.school_id and m.user_id = auth.uid()
            and m.staff_role = 'manager' and m.status = 'active'
      );
    get diagnostics v_count = row_count;
    if v_count <> 1 then return jsonb_build_object('error','کد دعوت پیدا نشد یا دسترسی ندارید'); end if;
    return jsonb_build_object('ok', true);
end $$;
revoke all on function public.native_manager_delete_invite_v61(uuid) from public, anon;
grant execute on function public.native_manager_delete_invite_v61(uuid) to authenticated;

-- ------------------------------------------------------------
-- ۲) used_at با فرمت ISO قابل parse (زمان‌سنج منجمد)
-- ------------------------------------------------------------
create or replace function public.native_manager_invites_v40b()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
 select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',i.id,'code',coalesce(i.display_code,'------'),'expires_at',i.expires_at,'used',i.used_at is not null,'used_at',coalesce(to_char(i.used_at at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS"Z"'),''),'revoked',i.revoked_at is not null) order by i.created_at desc),'[]'::jsonb)) from public.school_memberships me join public.school_teacher_invites i on i.school_id=me.school_id where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active' and i.created_at>now()-interval '7 days'),jsonb_build_object('error','دسترسی مدیر یافت نشد'));
$$;

-- ------------------------------------------------------------
-- ۳) متادادهٔ فیلتر با آرایهٔ مدرسه‌های هر دانش‌آموز
-- ------------------------------------------------------------
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
        ),
        -- V61.8: مدرسه‌های دانش‌آموز برای فیلتر «مدرسهٔ خاص»
        'schools', coalesce((
            select jsonb_agg(distinct ss.school_id::text)
            from public.school_students ss
            join my_schools ms on ms.school_id = ss.school_id
            where ss.student_id = p.id
        ), '[]'::jsonb)
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

commit;

-- سلامت‌سنجی پس از اجرا (هر سه باید true بدهند):
-- select to_regprocedure('public.native_manager_delete_invite_v61(uuid)') is not null;
-- select position('YYYY-MM-DD' in pg_get_functiondef('public.native_manager_invites_v40b()'::regprocedure)) > 0;
-- select position('''schools''' in pg_get_functiondef('public.native_student_filter_meta_v61()'::regprocedure)) > 0;
