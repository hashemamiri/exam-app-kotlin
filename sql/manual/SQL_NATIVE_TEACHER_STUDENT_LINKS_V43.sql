-- V43: explicit teacher student-list links without ownership transfer.
begin;
create table if not exists public.teacher_student_links(
 teacher_id uuid not null references public.profiles(id) on delete cascade,
 student_id uuid not null references public.profiles(id) on delete cascade,
 source_class_id uuid references public.classes(id) on delete set null,
 created_at timestamptz not null default now(),
 primary key(teacher_id,student_id)
);
alter table public.teacher_student_links enable row level security;
drop policy if exists "v43 teacher reads own links" on public.teacher_student_links;
create policy "v43 teacher reads own links" on public.teacher_student_links for select using(teacher_id=auth.uid());

drop function if exists public.my_students();
create function public.my_students()
returns table(id uuid,full_name text,first_name text,last_name text,username text,gender text,is_active boolean,class_names text,created_at timestamptz,father_name text,grade text,field_of_study text,avatar_url text,can_manage boolean,in_my_list boolean)
language sql stable security definer set search_path=public,pg_temp as $$
with me as(select school_id,staff_role from school_memberships where user_id=auth.uid() and status='active' limit 1)
select p.id,p.full_name,p.first_name,p.last_name,p.username,p.gender,coalesce(p.is_active,true),coalesce((select string_agg(distinct c.name,'، ' order by c.name) from class_members cm join classes c on c.id=cm.class_id where cm.student_id=p.id and c.teacher_id=auth.uid()),''),p.created_at,p.father_name,p.grade,p.field_of_study,p.avatar_url,
case when (select staff_role from me)='manager' then true else p.teacher_id=auth.uid() end,true
from profiles p where p.role='student' and (
 ((select staff_role from me)='manager' and exists(select 1 from school_students ss where ss.school_id=(select school_id from me) and ss.student_id=p.id))
 or p.teacher_id=auth.uid() or exists(select 1 from teacher_student_links l where l.teacher_id=auth.uid() and l.student_id=p.id)
) order by p.full_name,p.username;
$$;

drop function if exists public.class_roster(uuid);
create function public.class_roster(p_class uuid)
returns table(id uuid,full_name text,first_name text,last_name text,username text,gender text,is_active boolean,father_name text,grade text,field_of_study text,avatar_url text,can_manage boolean,in_my_list boolean)
language sql stable security definer set search_path=public,pg_temp as $$
select p.id,p.full_name,p.first_name,p.last_name,p.username,p.gender,coalesce(p.is_active,true),p.father_name,p.grade,p.field_of_study,p.avatar_url,
(p.teacher_id=auth.uid() or exists(select 1 from school_memberships sm where sm.user_id=auth.uid() and sm.school_id=c.school_id and sm.staff_role='manager' and sm.status='active')),
(p.teacher_id=auth.uid() or exists(select 1 from teacher_student_links l where l.teacher_id=auth.uid() and l.student_id=p.id) or exists(select 1 from school_memberships sm where sm.user_id=auth.uid() and sm.school_id=c.school_id and sm.staff_role='manager' and sm.status='active'))
from class_members cm join classes c on c.id=cm.class_id join profiles p on p.id=cm.student_id
where cm.class_id=p_class and (c.teacher_id=auth.uid() or exists(select 1 from school_memberships sm where sm.user_id=auth.uid() and sm.school_id=c.school_id and sm.staff_role='manager' and sm.status='active')) order by p.full_name;
$$;

create or replace function public.add_students_to_class(p_class uuid,p_students uuid[])
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_added int;begin
 if not exists(select 1 from classes where id=p_class and teacher_id=auth.uid()) then return jsonb_build_object('error','کلاس در دسترس نیست');end if;
 insert into class_members(class_id,student_id)
 select p_class,p.id from profiles p where p.id=any(p_students) and p.role='student' and (p.teacher_id=auth.uid() or exists(select 1 from teacher_student_links l where l.teacher_id=auth.uid() and l.student_id=p.id))
 on conflict do nothing;
 get diagnostics v_added=row_count;return jsonb_build_object('ok',true,'added',v_added);
end $$;

create or replace function public.native_teacher_add_class_student_to_list_v43(p_class uuid,p_student uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
begin
 if not exists(select 1 from classes c join class_members cm on cm.class_id=c.id and cm.student_id=p_student where c.id=p_class and c.teacher_id=auth.uid()) then return jsonb_build_object('error','دانش‌آموز عضو کلاس شما نیست');end if;
 insert into teacher_student_links(teacher_id,student_id,source_class_id) values(auth.uid(),p_student,p_class) on conflict(teacher_id,student_id) do nothing;
 return jsonb_build_object('ok',true);
end $$;
revoke all on table public.teacher_student_links from anon;
revoke all on function public.my_students(),public.class_roster(uuid),public.native_teacher_add_class_student_to_list_v43(uuid,uuid),public.add_students_to_class(uuid,uuid[]) from public,anon;
grant execute on function public.my_students(),public.class_roster(uuid),public.native_teacher_add_class_student_to_list_v43(uuid,uuid),public.add_students_to_class(uuid,uuid[]) to authenticated;
commit;
