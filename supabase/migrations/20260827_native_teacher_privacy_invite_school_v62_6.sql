-- V62.6: حریم خصوصی معلم (اشتراک کلاس/دانش‌آموز با مدیر)، کد دعوت با انتخاب
-- مدرسه و لیست کلاس‌های قابل‌مشاهدهٔ مدیر برای فیلتر.
begin;

-- ستون اشتراک: پیش‌فرض false یعنی ساخته‌های معلم از دید مدیر پنهان‌اند.
alter table public.classes add column if not exists shared_with_manager boolean not null default false;
alter table public.school_students add column if not exists shared_with_manager boolean not null default false;

-- معلم اشتراک کلاس خودش را روشن/خاموش می‌کند (قابل تغییر).
create or replace function public.native_teacher_share_class_v62(p_class uuid, p_share boolean)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
begin
 update public.classes set shared_with_manager=p_share where id=p_class and teacher_id=auth.uid();
 if not found then return jsonb_build_object('error','کلاس شما نیست'); end if;
 return jsonb_build_object('ok',true,'shared',p_share);
end $$;

-- معلم اشتراک دانش‌آموز ساختهٔ خودش را روشن/خاموش می‌کند.
create or replace function public.native_teacher_share_student_v62(p_student uuid, p_share boolean)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
begin
 if not exists(select 1 from public.profiles p where p.id=p_student and p.teacher_id=auth.uid()) then
   return jsonb_build_object('error','دانش‌آموز شما نیست');
 end if;
 update public.school_students set shared_with_manager=p_share where student_id=p_student;
 return jsonb_build_object('ok',true,'shared',p_share);
end $$;

-- my_students: ستون جدید shared_with_manager + دید مدیر فقط روی دانش‌آموزان
-- مجاز (اشتراکی معلم، ساختهٔ مدیر یا عضو کلاس اشتراکی/مدیرساخته) + class_names
-- مدیر شامل کلاس‌های قابل‌مشاهدهٔ مدرسه.
drop function if exists public.my_students();
create function public.my_students()
returns table(id uuid,full_name text,first_name text,last_name text,username text,gender text,is_active boolean,class_names text,created_at timestamptz,father_name text,grade text,field_of_study text,avatar_url text,can_manage boolean,shared_with_manager boolean)
language sql stable security definer set search_path=public,pg_temp as $$
with me as(select school_id,staff_role from public.school_memberships where user_id=auth.uid() and status='active' limit 1)
select p.id,p.full_name,p.first_name,p.last_name,p.username,p.gender,coalesce(p.is_active,true),
coalesce((select string_agg(distinct c.name,'، ' order by c.name) from public.class_members cm join public.classes c on c.id=cm.class_id where cm.student_id=p.id and (c.teacher_id=auth.uid() or ((select staff_role from me)='manager' and c.school_id=(select school_id from me) and (c.shared_with_manager or coalesce(c.created_by,c.teacher_id)<>c.teacher_id)))),''),
p.created_at,p.father_name,p.grade,p.field_of_study,p.avatar_url,
case when (select staff_role from me)='manager' then true else p.teacher_id=auth.uid() end,
coalesce((select bool_or(ss2.shared_with_manager) from public.school_students ss2 where ss2.student_id=p.id),false)
from public.profiles p where p.role='student' and (
 p.teacher_id=auth.uid()
 or exists(select 1 from me join public.school_students ss on ss.school_id=me.school_id where ss.student_id=p.id
    and (me.staff_role<>'manager'
      or ss.shared_with_manager
      or ss.created_by=auth.uid()
      or exists(select 1 from public.school_memberships mm where mm.user_id=ss.created_by and mm.school_id=ss.school_id and mm.staff_role='manager' and mm.status<>'removed')
      or exists(select 1 from public.class_members cm2 join public.classes c2 on c2.id=cm2.class_id where cm2.student_id=p.id and c2.school_id=ss.school_id and (c2.shared_with_manager or coalesce(c2.created_by,c2.teacher_id)<>c2.teacher_id))))
) order by p.full_name,p.username;
$$;

-- کلاس‌های خود معلم + وضعیت اشتراک برای سوییچ UI.
drop function if exists public.native_my_classes_v28();
create function public.native_my_classes_v28()
returns table(id uuid,name text,grade text,field_of_study text,boys integer,girls integer,total integer,created_at timestamptz,shared_with_manager boolean)
language sql stable security definer set search_path=public,pg_temp as $$
 select c.id,c.name,c.grade,c.field_of_study,
  coalesce(count(*) filter (where p.gender='male'),0)::integer,
  coalesce(count(*) filter (where p.gender='female'),0)::integer,
  coalesce(count(p.id),0)::integer,
  c.created_at,coalesce(c.shared_with_manager,false)
 from public.classes c
 left join public.class_members m on m.class_id=c.id
 left join public.profiles p on p.id=m.student_id
 where c.teacher_id=auth.uid()
 group by c.id,c.name,c.grade,c.field_of_study,c.created_at,c.shared_with_manager
 order by c.created_at desc nulls last,c.name;
$$;

-- مدیر فقط کلاس‌های اشتراکی معلم یا کلاس‌های مدیرساخته را می‌بیند.
create or replace function public.native_manager_teacher_classes_v40c(p_teacher uuid)
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select coalesce((select jsonb_build_object('ok',true,'teacher_name',p.full_name,'items',coalesce((select jsonb_agg(jsonb_build_object('id',c.id,'name',c.name,'grade',c.grade,'field_of_study',c.field_of_study,'total',(select count(*) from public.class_members cm where cm.class_id=c.id)) order by c.name) from public.classes c where c.teacher_id=p_teacher and c.school_id=me.school_id and (c.shared_with_manager or coalesce(c.created_by,c.teacher_id)<>c.teacher_id)),'[]'::jsonb)) from public.school_memberships me join public.school_memberships t on t.school_id=me.school_id and t.user_id=p_teacher and t.staff_role='teacher' and t.status<>'removed' join public.profiles p on p.id=p_teacher where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'),jsonb_build_object('error','معلم در مدرسه یافت نشد'));
$$;

create or replace function public.native_manager_class_roster_v40c(p_class uuid)
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select coalesce((select jsonb_build_object('ok',true,'class_name',c.name,'items',coalesce(jsonb_agg(jsonb_build_object('id',p.id,'full_name',p.full_name,'username',p.username) order by p.full_name) filter(where p.id is not null),'[]'::jsonb)) from public.classes c join public.school_memberships me on me.school_id=c.school_id and me.user_id=auth.uid() and me.staff_role='manager' and me.status='active' left join public.class_members cm on cm.class_id=c.id left join public.profiles p on p.id=cm.student_id where c.id=p_class and (c.shared_with_manager or coalesce(c.created_by,c.teacher_id)<>c.teacher_id) group by c.id),jsonb_build_object('error','کلاس یافت نشد'));
$$;

create or replace function public.native_manager_school_students_v40c()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',p.id,'full_name',p.full_name,'username',p.username) order by p.full_name),'[]'::jsonb)) from public.school_memberships me join public.school_students ss on ss.school_id=me.school_id join public.profiles p on p.id=ss.student_id where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'
 and (ss.shared_with_manager or ss.created_by=auth.uid()
      or exists(select 1 from public.school_memberships mm where mm.user_id=ss.created_by and mm.school_id=ss.school_id and mm.staff_role='manager' and mm.status<>'removed')
      or exists(select 1 from public.class_members cm2 join public.classes c2 on c2.id=cm2.class_id where cm2.student_id=p.id and c2.school_id=ss.school_id and (c2.shared_with_manager or coalesce(c2.created_by,c2.teacher_id)<>c2.teacher_id)))),jsonb_build_object('error','مدرسه یافت نشد'));
$$;

-- لیست کلاس‌های قابل‌مشاهدهٔ مدیر برای بخش «کلاس» پنجرهٔ فیلتر.
create or replace function public.native_manager_school_classes_v62()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',c.id,'name',c.name) order by c.name),'[]'::jsonb)) from public.classes c where exists(select 1 from public.school_memberships me where me.user_id=auth.uid() and me.school_id=c.school_id and me.staff_role='manager' and me.status='active') and (c.shared_with_manager or coalesce(c.created_by,c.teacher_id)<>c.teacher_id)),jsonb_build_object('error','مدرسه یافت نشد'));
$$;

-- کد دعوت با انتخاب مدرسهٔ مقصد (برای مدیر چندمدرسه‌ای).
create or replace function public.native_manager_create_teacher_invites_v62(p_count integer, p_school uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp,extensions as $$
declare v_uid uuid:=auth.uid(); v_code text; v_hash text; v_items jsonb:='[]'::jsonb; i int;
begin
 if p_count not between 1 and 5 then return jsonb_build_object('error','تعداد کد باید بین ۱ تا ۵ باشد'); end if;
 if not exists(select 1 from public.school_memberships where user_id=v_uid and school_id=p_school and staff_role='manager' and status='active') then
   return jsonb_build_object('error','مدرسه انتخابی معتبر نیست');
 end if;
 for i in 1..p_count loop
   loop
     v_code:=upper(substr(replace(gen_random_uuid()::text,'-',''),1,6));
     v_hash:=encode(extensions.digest(convert_to(v_code,'UTF8'),'sha256'),'hex');
     exit when not exists(select 1 from public.school_teacher_invites where token_hash=v_hash and used_at is null and revoked_at is null);
   end loop;
   insert into public.school_teacher_invites(school_id,email,token_hash,display_code,created_by,expires_at)
   values(p_school,null,v_hash,v_code,v_uid,now()+interval '24 hours');
   v_items:=v_items||jsonb_build_array(jsonb_build_object('code',v_code,'expires_at',now()+interval '24 hours','used',false));
 end loop;
 insert into public.school_admin_audit_v37(school_id,actor_id,action,details) values(p_school,v_uid,'short_teacher_invite_batch_created',jsonb_build_object('count',p_count));
 return jsonb_build_object('ok',true,'items',v_items);
end $$;

revoke all on function public.native_teacher_share_class_v62(uuid,boolean),public.native_teacher_share_student_v62(uuid,boolean),public.my_students(),public.native_my_classes_v28(),public.native_manager_teacher_classes_v40c(uuid),public.native_manager_class_roster_v40c(uuid),public.native_manager_school_students_v40c(),public.native_manager_school_classes_v62(),public.native_manager_create_teacher_invites_v62(integer,uuid) from public,anon;
grant execute on function public.native_teacher_share_class_v62(uuid,boolean),public.native_teacher_share_student_v62(uuid,boolean),public.my_students(),public.native_my_classes_v28(),public.native_manager_teacher_classes_v40c(uuid),public.native_manager_class_roster_v40c(uuid),public.native_manager_school_students_v40c(),public.native_manager_school_classes_v62(),public.native_manager_create_teacher_invites_v62(integer,uuid) to authenticated;

commit;
