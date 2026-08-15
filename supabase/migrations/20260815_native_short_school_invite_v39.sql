-- V39: کد دعوت کوتاه شش‌کاراکتری، بدون ایمیل، یک‌بارمصرف و ۲۴ ساعته.
begin;
alter table public.school_teacher_invites alter column email drop not null;
create table if not exists public.school_invite_attempts_v39(
 id bigserial primary key,user_id uuid not null references auth.users(id) on delete cascade,
 attempted_at timestamptz not null default now()
);
create index if not exists idx_invite_attempt_user_v39 on public.school_invite_attempts_v39(user_id,attempted_at desc);
alter table public.school_invite_attempts_v39 enable row level security;

create or replace function public.native_manager_create_teacher_invite_v39()
returns jsonb language plpgsql security definer set search_path=public,pg_temp,extensions as $$
declare v_uid uuid:=auth.uid(); v_school uuid; v_code text; v_hash text; v_id uuid; i int;
begin
 select school_id into v_school from public.school_memberships where user_id=v_uid and staff_role='manager' and status='active';
 if v_school is null then return jsonb_build_object('error','مدرسه فعال پیدا نشد'); end if;
 update public.school_teacher_invites set revoked_at=now() where school_id=v_school and used_at is null and revoked_at is null;
 for i in 1..20 loop
   v_code:=upper(substr(replace(gen_random_uuid()::text,'-',''),1,6));
   v_hash:=encode(extensions.digest(convert_to(v_code,'UTF8'),'sha256'),'hex');
   exit when not exists(select 1 from public.school_teacher_invites where token_hash=v_hash and used_at is null and revoked_at is null);
 end loop;
 insert into public.school_teacher_invites(school_id,email,token_hash,created_by,expires_at)
 values(v_school,null,v_hash,v_uid,now()+interval '24 hours') returning id into v_id;
 insert into public.school_admin_audit_v37(school_id,actor_id,action,details) values(v_school,v_uid,'short_teacher_invite_created',jsonb_build_object('invite_id',v_id,'hours',24));
 return jsonb_build_object('ok',true,'invite_id',v_id,'invite_code',v_code,'expires_in_hours',24);
end $$;

create or replace function public.native_school_invite_preview_v39(p_code text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp,extensions as $$
declare v_uid uuid:=auth.uid(); v_code text:=upper(btrim(coalesce(p_code,''))); v_inv public.school_teacher_invites%rowtype; v_school public.schools%rowtype;
begin
 if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 if not exists(select 1 from public.profiles where id=v_uid and role='teacher') then return jsonb_build_object('error','فقط حساب معلم می‌تواند به مدرسه بپیوندد'); end if;
 if (select count(*) from public.school_invite_attempts_v39 where user_id=v_uid and attempted_at>now()-interval '10 minutes')>=10 then return jsonb_build_object('error','تلاش بیش از حد؛ ده دقیقه بعد دوباره امتحان کنید'); end if;
 insert into public.school_invite_attempts_v39(user_id) values(v_uid);
 if v_code !~ '^[A-Z0-9]{6}$' then return jsonb_build_object('error','کد دعوت باید ۶ حرف یا عدد باشد'); end if;
 select * into v_inv from public.school_teacher_invites where token_hash=encode(extensions.digest(convert_to(v_code,'UTF8'),'sha256'),'hex') and used_at is null and revoked_at is null and expires_at>now();
 if v_inv.id is null then return jsonb_build_object('error','کد دعوت نامعتبر یا منقضی است'); end if;
 select * into v_school from public.schools where id=v_inv.school_id;
 return jsonb_build_object('ok',true,'invite_id',v_inv.id,'school_id',v_school.id,'school_name',v_school.name,'province',v_school.province,'city',v_school.city,'expires_at',v_inv.expires_at);
end $$;

create or replace function public.native_join_school_v39(p_code text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp,extensions as $$
declare v_uid uuid:=auth.uid(); v_code text:=upper(btrim(coalesce(p_code,''))); v_inv public.school_teacher_invites%rowtype; v_school public.schools%rowtype;
begin
 if v_uid is null or not exists(select 1 from public.profiles where id=v_uid and role='teacher') then return jsonb_build_object('error','فقط حساب معلم مجاز است'); end if;
 if exists(select 1 from public.school_memberships where user_id=v_uid and status='active') then return jsonb_build_object('error','این معلم قبلاً عضو یک مدرسه است'); end if;
 if (select count(*) from public.school_invite_attempts_v39 where user_id=v_uid and attempted_at>now()-interval '10 minutes')>=10 then return jsonb_build_object('error','تلاش بیش از حد؛ ده دقیقه بعد دوباره امتحان کنید'); end if;
 insert into public.school_invite_attempts_v39(user_id) values(v_uid);
 select * into v_inv from public.school_teacher_invites where token_hash=encode(extensions.digest(convert_to(v_code,'UTF8'),'sha256'),'hex') and used_at is null and revoked_at is null and expires_at>now() for update;
 if v_inv.id is null then return jsonb_build_object('error','کد دعوت نامعتبر، مصرف‌شده یا منقضی است'); end if;
 insert into public.school_memberships(school_id,user_id,staff_role,status,invited_by) values(v_inv.school_id,v_uid,'teacher','active',v_inv.created_by)
 on conflict(school_id,user_id) do update set status='active',staff_role='teacher',invited_by=excluded.invited_by;
 update public.school_teacher_invites set used_at=now() where id=v_inv.id;
 select * into v_school from public.schools where id=v_inv.school_id;
 insert into public.school_admin_audit_v37(school_id,actor_id,target_id,action,details) values(v_inv.school_id,v_uid,v_uid,'short_teacher_invite_redeemed',jsonb_build_object('invite_id',v_inv.id));
 return jsonb_build_object('ok',true,'school_id',v_school.id,'school_name',v_school.name);
end $$;

-- مدیر نیز هنگام ساخت مستقیم دانش‌آموز/کلاس در مدرسه scope می‌شود.
create or replace function public.native_attach_created_student_v37(p_actor uuid,p_student uuid)
returns void language sql security definer set search_path=public,pg_temp as $$
 insert into public.school_students(school_id,student_id,created_by)
 select school_id,p_student,p_actor from public.school_memberships
 where user_id=p_actor and staff_role in('teacher','manager') and status='active'
 on conflict do nothing;
$$;
revoke all on function public.native_attach_created_student_v37(uuid,uuid) from public,anon,authenticated;
grant execute on function public.native_attach_created_student_v37(uuid,uuid) to service_role;

create or replace function public.native_scope_new_school_row_v38()
returns trigger language plpgsql security definer set search_path=public,pg_temp as $$
begin
 if new.school_id is null then
   select school_id into new.school_id from public.school_memberships
   where user_id=new.teacher_id and staff_role in('teacher','manager') and status='active';
 end if;
 return new;
end $$;

revoke all on function public.native_manager_create_teacher_invite_v39() from public,anon;
revoke all on function public.native_school_invite_preview_v39(text) from public,anon;
revoke all on function public.native_join_school_v39(text) from public,anon;
grant execute on function public.native_manager_create_teacher_invite_v39(),public.native_school_invite_preview_v39(text),public.native_join_school_v39(text) to authenticated;
commit;
