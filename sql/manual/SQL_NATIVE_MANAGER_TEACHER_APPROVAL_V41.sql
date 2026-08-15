-- V41: manager changes to teacher-owned classes/student accounts require teacher approval.
create table if not exists public.manager_approval_requests(
 id uuid primary key default gen_random_uuid(), school_id uuid not null references public.schools(id) on delete cascade,
 manager_id uuid not null references public.profiles(id), teacher_id uuid not null references public.profiles(id),
 target_type text not null check(target_type in('class','student')), target_id uuid not null,
 action text not null check(action in('edit','delete')), payload jsonb not null default '{}'::jsonb,
 status text not null default 'pending' check(status in('pending','approved','rejected','expired','executed')),
 created_at timestamptz not null default now(), expires_at timestamptz not null default(now()+interval '24 hours'),
 decided_at timestamptz, executed_at timestamptz, decision_note text
);
create index if not exists manager_approval_teacher_idx on public.manager_approval_requests(teacher_id,status,expires_at desc);
alter table public.manager_approval_requests enable row level security;
drop policy if exists "v41 request parties read" on public.manager_approval_requests;
create policy "v41 request parties read" on public.manager_approval_requests for select using(auth.uid() in(manager_id,teacher_id));

create or replace function public.native_teacher_manager_requests_v41() returns jsonb language plpgsql security definer set search_path=public as $$
begin
 update manager_approval_requests set status='expired' where teacher_id=auth.uid() and status='pending' and expires_at<=now();
 return jsonb_build_object('ok',true,'items',coalesce((select jsonb_agg(jsonb_build_object('id',r.id,'target_type',r.target_type,'action',r.action,'status',r.status,'expires_at',r.expires_at,'manager_name',coalesce(p.full_name,'')) order by r.created_at desc) from manager_approval_requests r left join profiles p on p.id=r.manager_id where r.teacher_id=auth.uid() and r.created_at>now()-interval '30 days'),'[]'::jsonb));
end $$;

create or replace function public.native_teacher_decide_manager_request_v41(p_request uuid,p_approve boolean,p_note text default null) returns jsonb language plpgsql security definer set search_path=public as $$
declare r manager_approval_requests;begin
 select * into r from manager_approval_requests where id=p_request and teacher_id=auth.uid() for update;
 if r.id is null then return jsonb_build_object('error','درخواست یافت نشد'); end if;
 if r.status<>'pending' then return jsonb_build_object('error','این درخواست قبلاً تعیین تکلیف شده است'); end if;
 if r.expires_at<=now() then update manager_approval_requests set status='expired' where id=r.id;return jsonb_build_object('error','درخواست منقضی شده است');end if;
 update manager_approval_requests set status=case when p_approve then 'approved' else 'rejected' end,decided_at=now(),decision_note=nullif(left(coalesce(p_note,''),500),'') where id=r.id;
 if p_approve and r.target_type='class' then
  if r.action='edit' then update classes set name=coalesce(nullif(btrim(r.payload->>'name'),''),name),grade=nullif(btrim(coalesce(r.payload->>'grade','')),''),field_of_study=nullif(btrim(coalesce(r.payload->>'field','')),'') where id=r.target_id and teacher_id=auth.uid();
  else delete from classes where id=r.target_id and teacher_id=auth.uid(); end if;
  update manager_approval_requests set status='executed',executed_at=now() where id=r.id;
 end if;
 return jsonb_build_object('ok',true,'status',case when p_approve and r.target_type='class' then 'executed' when p_approve then 'approved' else 'rejected' end);
end $$;

create or replace function public.native_manager_change_teacher_class_v41(p_class uuid,p_action text,p_payload jsonb default '{}'::jsonb) returns jsonb language plpgsql security definer set search_path=public as $$
declare c classes;v_school uuid;v_id uuid;begin
 select * into c from classes where id=p_class; if c.id is null then return jsonb_build_object('error','کلاس یافت نشد');end if;
 select school_id into v_school from school_memberships where user_id=auth.uid() and staff_role='manager' and status='active' and school_id=c.school_id;
 if v_school is null then return jsonb_build_object('error','دسترسی مدیر معتبر نیست');end if;
 if c.created_by=auth.uid() then if p_action='delete' then delete from classes where id=c.id;else update classes set name=coalesce(nullif(btrim(p_payload->>'name'),''),name),grade=nullif(btrim(coalesce(p_payload->>'grade','')),''),field_of_study=nullif(btrim(coalesce(p_payload->>'field','')),'') where id=c.id;end if;return jsonb_build_object('ok',true,'executed',true);end if;
 select id into v_id from manager_approval_requests where manager_id=auth.uid() and teacher_id=c.teacher_id and target_type='class' and target_id=c.id and action=p_action and status='pending' and expires_at>now() limit 1;
 if v_id is null then insert into manager_approval_requests(school_id,manager_id,teacher_id,target_type,target_id,action,payload) values(v_school,auth.uid(),c.teacher_id,'class',c.id,p_action,p_payload) returning id into v_id;end if;
 return jsonb_build_object('ok',true,'approval_required',true,'request_id',v_id,'message','درخواست برای تأیید معلم ارسال شد');
end $$;
grant execute on function public.native_teacher_manager_requests_v41() to authenticated;
grant execute on function public.native_teacher_decide_manager_request_v41(uuid,boolean,text) to authenticated;
grant execute on function public.native_manager_change_teacher_class_v41(uuid,text,jsonb) to authenticated;
revoke all on table public.manager_approval_requests from anon;
