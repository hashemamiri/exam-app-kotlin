-- V40C: مدیریت کلاس‌های معلم، دانش‌آموزان مشترک مدرسه و حذف فقط عضویت کلاس.
begin;

drop function if exists public.my_students();
create function public.my_students()
returns table(id uuid,full_name text,first_name text,last_name text,username text,gender text,is_active boolean,class_names text,created_at timestamptz,father_name text,grade text,field_of_study text,avatar_url text,can_manage boolean)
language sql stable security definer set search_path=public,pg_temp as $$
with me as(select school_id,staff_role from public.school_memberships where user_id=auth.uid() and status='active' limit 1)
select p.id,p.full_name,p.first_name,p.last_name,p.username,p.gender,coalesce(p.is_active,true),coalesce((select string_agg(distinct c.name,'، ' order by c.name) from public.class_members cm join public.classes c on c.id=cm.class_id where cm.student_id=p.id and c.teacher_id=auth.uid()),''),p.created_at,p.father_name,p.grade,p.field_of_study,p.avatar_url,
case when (select staff_role from me)='manager' then true else p.teacher_id=auth.uid() end
from public.profiles p where p.role='student' and (p.teacher_id=auth.uid() or exists(select 1 from me join public.school_students ss on ss.school_id=me.school_id where ss.student_id=p.id)) order by p.full_name,p.username;
$$;

drop function if exists public.class_roster(uuid);
create function public.class_roster(p_class uuid)
returns table(id uuid,full_name text,first_name text,last_name text,username text,gender text,is_active boolean,father_name text,grade text,field_of_study text,avatar_url text,can_manage boolean)
language sql stable security definer set search_path=public,pg_temp as $$
select p.id,p.full_name,p.first_name,p.last_name,p.username,p.gender,coalesce(p.is_active,true),p.father_name,p.grade,p.field_of_study,p.avatar_url,(p.teacher_id=auth.uid() or exists(select 1 from public.school_memberships sm where sm.user_id=auth.uid() and sm.school_id=c.school_id and sm.staff_role='manager' and sm.status='active'))
from public.class_members cm join public.classes c on c.id=cm.class_id join public.profiles p on p.id=cm.student_id
where cm.class_id=p_class and (c.teacher_id=auth.uid() or exists(select 1 from public.school_memberships sm where sm.user_id=auth.uid() and sm.school_id=c.school_id and sm.staff_role='manager' and sm.status='active')) order by p.full_name;
$$;

create or replace function public.native_manager_teacher_classes_v40c(p_teacher uuid)
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select coalesce((select jsonb_build_object('ok',true,'teacher_name',p.full_name,'items',coalesce((select jsonb_agg(jsonb_build_object('id',c.id,'name',c.name,'grade',c.grade,'field_of_study',c.field_of_study,'total',(select count(*) from public.class_members cm where cm.class_id=c.id)) order by c.name) from public.classes c where c.teacher_id=p_teacher and c.school_id=me.school_id),'[]'::jsonb)) from public.school_memberships me join public.school_memberships t on t.school_id=me.school_id and t.user_id=p_teacher and t.staff_role='teacher' and t.status<>'removed' join public.profiles p on p.id=p_teacher where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'),jsonb_build_object('error','معلم در مدرسه یافت نشد'));
$$;
create or replace function public.native_manager_save_teacher_class_v40c(p_teacher uuid,p_name text,p_grade text,p_field text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$ declare v_uid uuid:=auth.uid();v_school uuid;v_id uuid;begin
select me.school_id into v_school from public.school_memberships me join public.school_memberships t on t.school_id=me.school_id and t.user_id=p_teacher and t.staff_role='teacher' and t.status<>'removed' where me.user_id=v_uid and me.staff_role='manager' and me.status='active';
if v_school is null or btrim(coalesce(p_name,''))='' then return jsonb_build_object('error','معلم یا نام کلاس معتبر نیست');end if;
insert into public.classes(teacher_id,name,grade,field_of_study,school_id,created_by) values(p_teacher,btrim(p_name),nullif(btrim(coalesce(p_grade,'')),''),nullif(btrim(coalesce(p_field,'')),''),v_school,v_uid) returning id into v_id;return jsonb_build_object('ok',true,'id',v_id);end $$;
create or replace function public.native_manager_delete_teacher_class_v40c(p_class uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$ declare v_count int;begin
delete from public.classes c where c.id=p_class and exists(select 1 from public.school_memberships me where me.user_id=auth.uid() and me.school_id=c.school_id and me.staff_role='manager' and me.status='active');get diagnostics v_count=row_count;if v_count<>1 then return jsonb_build_object('error','کلاس یافت نشد');end if;return jsonb_build_object('ok',true);end $$;
create or replace function public.native_manager_class_roster_v40c(p_class uuid)
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select coalesce((select jsonb_build_object('ok',true,'class_name',c.name,'items',coalesce(jsonb_agg(jsonb_build_object('id',p.id,'full_name',p.full_name,'username',p.username) order by p.full_name) filter(where p.id is not null),'[]'::jsonb)) from public.classes c join public.school_memberships me on me.school_id=c.school_id and me.user_id=auth.uid() and me.staff_role='manager' and me.status='active' left join public.class_members cm on cm.class_id=c.id left join public.profiles p on p.id=cm.student_id where c.id=p_class group by c.id),jsonb_build_object('error','کلاس یافت نشد'));
$$;
create or replace function public.native_manager_school_students_v40c()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',p.id,'full_name',p.full_name,'username',p.username) order by p.full_name),'[]'::jsonb)) from public.school_memberships me join public.school_students ss on ss.school_id=me.school_id join public.profiles p on p.id=ss.student_id where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'),jsonb_build_object('error','مدرسه یافت نشد'));
$$;
create or replace function public.native_manager_set_class_student_v40c(p_class uuid,p_student uuid,p_add boolean)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$ declare v_school uuid;begin
select c.school_id into v_school from public.classes c join public.school_memberships me on me.school_id=c.school_id and me.user_id=auth.uid() and me.staff_role='manager' and me.status='active' where c.id=p_class;
if v_school is null or not exists(select 1 from public.school_students where school_id=v_school and student_id=p_student) then return jsonb_build_object('error','کلاس یا دانش‌آموز معتبر نیست');end if;
if p_add then insert into public.class_members(class_id,student_id) values(p_class,p_student) on conflict do nothing;else delete from public.class_members where class_id=p_class and student_id=p_student;end if;return jsonb_build_object('ok',true);end $$;

-- معلم فقط دانش‌آموز مدرسه را به کلاس خودش اضافه/حذف می‌کند؛ حساب حذف نمی‌شود.
create or replace function public.native_add_student_to_classes_v22(p_student uuid,p_classes uuid[])
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$ declare v_uid uuid:=auth.uid();v_added int;begin
if not exists(select 1 from public.school_memberships me join public.school_students ss on ss.school_id=me.school_id and ss.student_id=p_student where me.user_id=v_uid and me.status='active') and not exists(select 1 from public.profiles where id=p_student and teacher_id=v_uid) then return jsonb_build_object('error','دانش‌آموز در دسترس نیست');end if;
insert into public.class_members(class_id,student_id) select distinct c.id,p_student from public.classes c where c.id=any(p_classes) and c.teacher_id=v_uid on conflict do nothing;get diagnostics v_added=row_count;return jsonb_build_object('ok',true,'added',v_added);end $$;

revoke all on function public.my_students(),public.class_roster(uuid),public.native_manager_teacher_classes_v40c(uuid),public.native_manager_save_teacher_class_v40c(uuid,text,text,text),public.native_manager_delete_teacher_class_v40c(uuid),public.native_manager_class_roster_v40c(uuid),public.native_manager_school_students_v40c(),public.native_manager_set_class_student_v40c(uuid,uuid,boolean),public.native_add_student_to_classes_v22(uuid,uuid[]) from public,anon;
grant execute on function public.my_students(),public.class_roster(uuid),public.native_manager_teacher_classes_v40c(uuid),public.native_manager_save_teacher_class_v40c(uuid,text,text,text),public.native_manager_delete_teacher_class_v40c(uuid),public.native_manager_class_roster_v40c(uuid),public.native_manager_school_students_v40c(),public.native_manager_set_class_student_v40c(uuid,uuid,boolean),public.native_add_student_to_classes_v22(uuid,uuid[]) to authenticated;
commit;
