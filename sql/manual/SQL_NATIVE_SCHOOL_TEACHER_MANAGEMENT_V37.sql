-- V37: دعوت امن معلم، فهرست/قطع عضویت و پایهٔ مالکیت دادهٔ مدرسه.
begin;
create extension if not exists pgcrypto;

create table if not exists public.school_teacher_invites (
 id uuid primary key default gen_random_uuid(), school_id uuid not null references public.schools(id) on delete cascade,
 email text not null, token_hash text not null unique, created_by uuid not null references auth.users(id),
 expires_at timestamptz not null default now()+interval '7 days', used_at timestamptz, revoked_at timestamptz,
 created_at timestamptz not null default now()
);
create index if not exists idx_school_invites_v37 on public.school_teacher_invites(school_id,created_at desc);
alter table public.school_teacher_invites enable row level security;

create table if not exists public.school_students (
 school_id uuid not null references public.schools(id) on delete cascade,
 student_id uuid not null references auth.users(id) on delete cascade,
 created_by uuid not null references auth.users(id) on delete restrict,
 joined_at timestamptz not null default now(), primary key(school_id,student_id)
);
alter table public.school_students enable row level security;

alter table public.classes add column if not exists school_id uuid references public.schools(id) on delete set null;
alter table public.classes add column if not exists created_by uuid references auth.users(id) on delete set null;
update public.classes set created_by=teacher_id where created_by is null;

create table if not exists public.school_admin_audit_v37 (
 id bigserial primary key, school_id uuid not null references public.schools(id) on delete cascade,
 actor_id uuid not null references auth.users(id), target_id uuid references auth.users(id),
 action text not null, details jsonb not null default '{}'::jsonb, created_at timestamptz not null default now()
);
alter table public.school_admin_audit_v37 enable row level security;

create or replace function public.native_manager_create_teacher_invite_v37(p_email text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid(); v_school uuid; v_email text:=lower(btrim(coalesce(p_email,''))); v_token text; v_id uuid;
begin
 select school_id into v_school from public.school_memberships where user_id=v_uid and staff_role='manager' and status='active';
 if v_school is null then return jsonb_build_object('error','مدرسه فعال پیدا نشد'); end if;
 if v_email !~ '^[^@[:space:]]+@[^@[:space:]]+[.][^@[:space:]]+$' then return jsonb_build_object('error','ایمیل معلم معتبر نیست'); end if;
 if exists(select 1 from public.school_memberships sm join auth.users u on u.id=sm.user_id where sm.school_id=v_school and sm.status='active' and lower(u.email)=v_email) then return jsonb_build_object('error','این معلم عضو فعال مدرسه است'); end if;
 update public.school_teacher_invites set revoked_at=now() where school_id=v_school and email=v_email and used_at is null and revoked_at is null;
 v_token:='TCH-'||replace(gen_random_uuid()::text,'-','')||replace(gen_random_uuid()::text,'-','');
 insert into public.school_teacher_invites(school_id,email,token_hash,created_by) values(v_school,v_email,encode(digest(v_token,'sha256'),'hex')) returning id into v_id;
 insert into public.school_admin_audit_v37(school_id,actor_id,action,details) values(v_school,v_uid,'teacher_invite_created',jsonb_build_object('email',v_email));
 return jsonb_build_object('ok',true,'invite_id',v_id,'invite_code',v_token,'email',v_email,'expires_in_days',7);
end $$;

create or replace function public.native_complete_teacher_registration_v37(p_full_name text,p_username text,p_invite_code text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid(); v_email text; v_inv public.school_teacher_invites%rowtype; v_name text:=left(btrim(coalesce(p_full_name,'')),200); v_user text:=lower(btrim(coalesce(p_username,'')));
begin
 if v_uid is null then return jsonb_build_object('error','ابتدا ایمیل را تأیید کنید'); end if;
 select lower(email) into v_email from auth.users where id=v_uid;
 select * into v_inv from public.school_teacher_invites where token_hash=encode(digest(btrim(coalesce(p_invite_code,'')),'sha256'),'hex') and used_at is null and revoked_at is null and expires_at>now() for update;
 if v_inv.id is null or v_inv.email<>v_email then return jsonb_build_object('error','کد دعوت نامعتبر، منقضی یا متعلق به ایمیل دیگری است'); end if;
 if v_name='' or v_user !~ '^[a-z0-9_]{4,20}$' then return jsonb_build_object('error','نام یا نام کاربری معتبر نیست'); end if;
 if exists(select 1 from public.profiles where lower(coalesce(username,''))=v_user and id<>v_uid) then return jsonb_build_object('error','نام کاربری تکراری است'); end if;
 insert into public.profiles(id,full_name,display_name,username,role) values(v_uid,v_name,v_name,v_user,'student') on conflict(id) do nothing;
 if exists(select 1 from public.profiles where id=v_uid and (teacher_id is not null or role not in ('student','teacher'))) then return jsonb_build_object('error','این حساب قابل عضویت به عنوان معلم نیست'); end if;
 update public.profiles set full_name=v_name,display_name=coalesce(nullif(display_name,''),v_name),username=v_user,role='teacher',teacher_id=null where id=v_uid;
 insert into public.school_memberships(school_id,user_id,staff_role,status,invited_by) values(v_inv.school_id,v_uid,'teacher','active',v_inv.created_by)
 on conflict(school_id,user_id) do update set status='active',staff_role='teacher',invited_by=excluded.invited_by;
 update public.school_teacher_invites set used_at=now() where id=v_inv.id;
 insert into public.school_admin_audit_v37(school_id,actor_id,target_id,action) values(v_inv.school_id,v_uid,v_uid,'teacher_invite_redeemed');
 return jsonb_build_object('ok',true,'id',v_uid,'full_name',v_name,'username',v_user,'role','teacher','school_id',v_inv.school_id);
end $$;

create or replace function public.native_manager_teachers_v37()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
 select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',p.id,'full_name',p.full_name,'username',p.username,'email',u.email,'joined_at',sm.joined_at) order by p.full_name),'[]'::jsonb)) from public.school_memberships me join public.school_memberships sm on sm.school_id=me.school_id and sm.staff_role='teacher' and sm.status='active' join public.profiles p on p.id=sm.user_id join auth.users u on u.id=sm.user_id where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'),jsonb_build_object('error','دسترسی مدیر یافت نشد'));
$$;

create or replace function public.native_manager_disable_teacher_v37(p_teacher uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid(); v_school uuid; v_count int;
begin
 select school_id into v_school from public.school_memberships where user_id=v_uid and staff_role='manager' and status='active';
 update public.school_memberships set status='disabled' where school_id=v_school and user_id=p_teacher and staff_role='teacher' and status='active'; get diagnostics v_count=row_count;
 if v_count<>1 then return jsonb_build_object('error','معلم فعال پیدا نشد'); end if;
 insert into public.school_admin_audit_v37(school_id,actor_id,target_id,action) values(v_school,v_uid,p_teacher,'teacher_membership_disabled');
 return jsonb_build_object('ok',true);
end $$;

-- helper امن برای Edge manage-student: فقط service backend با grant صریح migration نهایی استفاده می‌کند.
create or replace function public.native_attach_created_student_v37(p_actor uuid,p_student uuid)
returns void language sql security definer set search_path=public,pg_temp as $$
 insert into public.school_students(school_id,student_id,created_by)
 select school_id,p_student,p_actor from public.school_memberships where user_id=p_actor and staff_role='teacher' and status='active'
 on conflict do nothing;
$$;

-- grants
revoke all on function public.native_manager_create_teacher_invite_v37(text) from public,anon;
revoke all on function public.native_complete_teacher_registration_v37(text,text,text) from public,anon;
revoke all on function public.native_manager_teachers_v37() from public,anon;
revoke all on function public.native_manager_disable_teacher_v37(uuid) from public,anon;
grant execute on function public.native_manager_create_teacher_invite_v37(text),public.native_complete_teacher_registration_v37(text,text,text),public.native_manager_teachers_v37(),public.native_manager_disable_teacher_v37(uuid) to authenticated;
revoke all on function public.native_attach_created_student_v37(uuid,uuid) from public,anon,authenticated;
grant execute on function public.native_attach_created_student_v37(uuid,uuid) to service_role;
commit;
