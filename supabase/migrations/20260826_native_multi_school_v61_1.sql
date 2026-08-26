-- V61.1 — سه اصلاح:
-- ۱) رفع خطای «Could not choose the best candidate function»: از V40C دو
--    overload از native_add_student_to_classes_v22 وجود دارد (jsonb قدیمی V22
--    و uuid[] جدید V40C)؛ PostgREST بین آن‌ها انتخاب نمی‌کند. نسخهٔ jsonb حذف.
-- ۲) معلم می‌تواند عضو نامحدود مدرسه باشد: حذف unique index تک‌عضویتی V36 و
--    گارد «قبلاً عضو یک مدرسه است» از native_join_school_v39.
-- ۳) مدیر می‌تواند چند مدرسه بسازد: native_manager_create_school_v61 + لیست
--    مدارس مدیر (created_by یا عضویت مدیری) در native_teacher_schools_v61 و
--    کلاس‌های کل مدرسه برای مدیر در native_teacher_school_classes_v61.

begin;

-- ------------------------------------------------------------
-- ۱) حذف overload قدیمی V22 (کلاینت آرایهٔ JSON می‌فرستد که با تک‌نسخهٔ
--    uuid[] بدون ابهام cast می‌شود؛ منطق V40C جدیدتر و کامل‌تر است).
-- ------------------------------------------------------------
drop function if exists public.native_add_student_to_classes_v22(uuid, jsonb);

-- ------------------------------------------------------------
-- ۲) عضویت چندمدرسه‌ای
-- ------------------------------------------------------------
drop index if exists public.ux_school_one_active_membership_v36;

create or replace function public.native_join_school_v39(p_code text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp,extensions as $$
declare v_uid uuid:=auth.uid(); v_code text:=upper(btrim(coalesce(p_code,''))); v_inv public.school_teacher_invites%rowtype; v_school public.schools%rowtype;
begin
 if v_uid is null or not exists(select 1 from public.profiles where id=v_uid and role='teacher') then return jsonb_build_object('error','فقط حساب معلم مجاز است'); end if;
 -- V61.1: گارد تک‌مدرسه‌ای حذف شد؛ معلم می‌تواند عضو چند مدرسه باشد.
 if (select count(*) from public.school_invite_attempts_v39 where user_id=v_uid and attempted_at>now()-interval '10 minutes')>=10 then return jsonb_build_object('error','تلاش بیش از حد؛ ده دقیقه بعد دوباره امتحان کنید'); end if;
 insert into public.school_invite_attempts_v39(user_id) values(v_uid);
 select * into v_inv from public.school_teacher_invites where token_hash=encode(extensions.digest(convert_to(v_code,'UTF8'),'sha256'),'hex') and used_at is null and revoked_at is null and expires_at>now() for update;
 if v_inv.id is null then return jsonb_build_object('error','کد دعوت نامعتبر، مصرف‌شده یا منقضی است'); end if;
 if exists(select 1 from public.school_memberships where school_id=v_inv.school_id and user_id=v_uid and status='active') then
   return jsonb_build_object('error','قبلاً عضو همین مدرسه هستید');
 end if;
 insert into public.school_memberships(school_id,user_id,staff_role,status,invited_by) values(v_inv.school_id,v_uid,'teacher','active',v_inv.created_by)
 on conflict(school_id,user_id) do update set status='active',staff_role='teacher',invited_by=excluded.invited_by;
 update public.school_teacher_invites set used_at=now() where id=v_inv.id;
 select * into v_school from public.schools where id=v_inv.school_id;
 insert into public.school_admin_audit_v37(school_id,actor_id,target_id,action,details) values(v_inv.school_id,v_uid,v_uid,'short_teacher_invite_redeemed',jsonb_build_object('invite_id',v_inv.id));
 return jsonb_build_object('ok',true,'school_id',v_school.id,'school_name',v_school.name);
end $$;
revoke all on function public.native_join_school_v39(text) from public,anon;
grant execute on function public.native_join_school_v39(text) to authenticated;

-- ------------------------------------------------------------
-- ۳) چند مدرسه برای مدیر
-- ------------------------------------------------------------
create or replace function public.native_manager_create_school_v61(
    p_name text,
    p_province text default '',
    p_city text default ''
)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare
    v_uid uuid := auth.uid();
    v_name text := left(btrim(coalesce(p_name,'')),160);
    v_school uuid;
begin
    if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
    if not exists (select 1 from public.profiles where id=v_uid and role='manager') then
        return jsonb_build_object('error','فقط مدیر/معاون می‌تواند مدرسه بسازد');
    end if;
    if char_length(v_name) < 2 then return jsonb_build_object('error','نام مدرسه را وارد کنید'); end if;
    insert into public.schools(name,province,city,created_by)
    values (v_name,left(btrim(coalesce(p_province,'')),100),left(btrim(coalesce(p_city,'')),100),v_uid)
    returning id into v_school;
    insert into public.school_memberships(school_id,user_id,staff_role,status)
    values (v_school,v_uid,'manager','active')
    on conflict(school_id,user_id) do update set status='active',staff_role='manager';
    return jsonb_build_object('ok',true,'id',v_school);
end $$;
revoke all on function public.native_manager_create_school_v61(text,text,text) from public,anon;
grant execute on function public.native_manager_create_school_v61(text,text,text) to authenticated;

-- لیست مدارس: معلم = عضویت‌های فعال؛ مدیر = مدارس ساخته‌شده یا عضویت مدیری.
create or replace function public.native_teacher_schools_v61()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select jsonb_build_object('ok',true,'items',coalesce((
    select jsonb_agg(jsonb_build_object(
        'id',s.id,'name',s.name,
        'province',coalesce(s.province,''),'city',coalesce(s.city,''),
        'classes',(select count(*) from public.classes c
                   where c.school_id=s.id
                     and (c.teacher_id=auth.uid() or s.created_by=auth.uid()
                          or exists (select 1 from public.school_memberships mm
                                     where mm.school_id=s.id and mm.user_id=auth.uid()
                                       and mm.staff_role='manager' and mm.status='active')))
    ) order by s.name)
    from public.schools s
    where s.created_by=auth.uid()
       or exists (select 1 from public.school_memberships m
                  where m.school_id=s.id and m.user_id=auth.uid() and m.status='active')
),'[]'::jsonb));
$$;

-- کلاس‌های مدرسه: مدیرِ مدرسه همهٔ کلاس‌ها را می‌بیند؛ معلم فقط کلاس‌های خودش.
create or replace function public.native_teacher_school_classes_v61(p_school uuid)
returns table(id uuid,name text,grade text,field_of_study text,boys integer,girls integer,total integer,created_at timestamptz)
language sql stable security definer set search_path=public,pg_temp as $$
    select c.id,c.name,c.grade,c.field_of_study,
           coalesce(count(*) filter (where p.gender='male'),0)::integer,
           coalesce(count(*) filter (where p.gender='female'),0)::integer,
           coalesce(count(p.id),0)::integer,c.created_at
    from public.classes c
    left join public.class_members m on m.class_id=c.id
    left join public.profiles p on p.id=m.student_id
    where c.school_id=p_school
      and (
        c.teacher_id=auth.uid()
        or exists (select 1 from public.schools s where s.id=p_school and s.created_by=auth.uid())
        or exists (select 1 from public.school_memberships mm
                   where mm.school_id=p_school and mm.user_id=auth.uid()
                     and mm.staff_role='manager' and mm.status='active')
      )
    group by c.id,c.name,c.grade,c.field_of_study,c.created_at
    order by c.created_at desc nulls last,c.name;
$$;

revoke all on function public.native_teacher_schools_v61() from public,anon;
grant execute on function public.native_teacher_schools_v61() to authenticated;
revoke all on function public.native_teacher_school_classes_v61(uuid) from public,anon;
grant execute on function public.native_teacher_school_classes_v61(uuid) to authenticated;

commit;

-- سلامت‌سنجی پس از اجرا (هر دو باید true بدهند):
-- select to_regprocedure('public.native_add_student_to_classes_v22(uuid,jsonb)') is null;
-- select to_regprocedure('public.native_manager_create_school_v61(text,text,text)') is not null;
