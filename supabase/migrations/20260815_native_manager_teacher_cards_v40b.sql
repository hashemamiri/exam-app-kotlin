-- V40B: کارت معلم، وضعیت عضویت و دعوت دسته‌ای ۱..۵.
begin;
alter table public.school_teacher_invites add column if not exists display_code text;
-- status قبلی فقط active/disabled بود؛ removed عضویت حذف‌شده بدون حذف Auth است.
do $$ declare r record; begin
 for r in select conname from pg_constraint c join pg_class t on t.oid=c.conrelid join pg_namespace n on n.oid=t.relnamespace where n.nspname='public' and t.relname='school_memberships' and c.contype='c' and pg_get_constraintdef(c.oid) ilike '%status%' loop execute format('alter table public.school_memberships drop constraint %I',r.conname); end loop;
end $$;
alter table public.school_memberships add constraint school_membership_status_v40b check(status in('active','disabled','removed'));

create or replace function public.native_manager_create_teacher_invites_v40b(p_count integer)
returns jsonb language plpgsql security definer set search_path=public,pg_temp,extensions as $$
declare v_uid uuid:=auth.uid(); v_school uuid; v_code text; v_hash text; v_items jsonb:='[]'::jsonb; i int;
begin
 if p_count not between 1 and 5 then return jsonb_build_object('error','تعداد کد باید بین ۱ تا ۵ باشد'); end if;
 select school_id into v_school from public.school_memberships where user_id=v_uid and staff_role='manager' and status='active';
 if v_school is null then return jsonb_build_object('error','مدرسه فعال پیدا نشد'); end if;
 for i in 1..p_count loop
   loop
     v_code:=upper(substr(replace(gen_random_uuid()::text,'-',''),1,6));
     v_hash:=encode(extensions.digest(convert_to(v_code,'UTF8'),'sha256'),'hex');
     exit when not exists(select 1 from public.school_teacher_invites where token_hash=v_hash and used_at is null and revoked_at is null);
   end loop;
   insert into public.school_teacher_invites(school_id,email,token_hash,display_code,created_by,expires_at)
   values(v_school,null,v_hash,v_code,v_uid,now()+interval '24 hours');
   v_items:=v_items||jsonb_build_array(jsonb_build_object('code',v_code,'expires_at',now()+interval '24 hours','used',false));
 end loop;
 insert into public.school_admin_audit_v37(school_id,actor_id,action,details) values(v_school,v_uid,'short_teacher_invite_batch_created',jsonb_build_object('count',p_count));
 return jsonb_build_object('ok',true,'items',v_items);
end $$;

create or replace function public.native_manager_invites_v40b()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
 select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',i.id,'code',coalesce(i.display_code,'------'),'expires_at',i.expires_at,'used',i.used_at is not null,'revoked',i.revoked_at is not null) order by i.created_at desc),'[]'::jsonb)) from public.school_memberships me join public.school_teacher_invites i on i.school_id=me.school_id where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active' and i.created_at>now()-interval '7 days'),jsonb_build_object('error','دسترسی مدیر یافت نشد'));
$$;

create or replace function public.native_manager_revoke_invite_v40b(p_invite uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$ declare v_uid uuid:=auth.uid(); v_school uuid; v_count int; begin
 select school_id into v_school from public.school_memberships where user_id=v_uid and staff_role='manager' and status='active';
 update public.school_teacher_invites set revoked_at=coalesce(revoked_at,now()) where id=p_invite and school_id=v_school and used_at is null; get diagnostics v_count=row_count;
 if v_count<>1 then return jsonb_build_object('error','کد دعوت قابل حذف نیست'); end if;
 return jsonb_build_object('ok',true);
end $$;

create or replace function public.native_manager_teachers_v37()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
 select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',p.id,'full_name',p.full_name,'employee_code',coalesce(p.employee_code,''),'phone',coalesce(p.phone,''),'status',sm.status,'wallet_balance',coalesce(w.balance,0)) order by p.full_name),'[]'::jsonb)) from public.school_memberships me join public.school_memberships sm on sm.school_id=me.school_id and sm.staff_role='teacher' and sm.status<>'removed' join public.profiles p on p.id=sm.user_id left join public.wallets w on w.user_id=sm.user_id where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'),jsonb_build_object('error','دسترسی مدیر یافت نشد'));
$$;

create or replace function public.native_manager_set_teacher_active_v40b(p_teacher uuid,p_active boolean)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$ declare v_uid uuid:=auth.uid(); v_school uuid; v_count int; begin
 select school_id into v_school from public.school_memberships where user_id=v_uid and staff_role='manager' and status='active';
 update public.school_memberships set status=case when p_active then 'active' else 'disabled' end where school_id=v_school and user_id=p_teacher and staff_role='teacher' and status<>'removed'; get diagnostics v_count=row_count;
 if v_count<>1 then return jsonb_build_object('error','معلم پیدا نشد'); end if;
 return jsonb_build_object('ok',true,'active',p_active);
end $$;

create or replace function public.native_manager_remove_teacher_v40b(p_teacher uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$ declare v_uid uuid:=auth.uid(); v_school uuid; v_count int; begin
 select school_id into v_school from public.school_memberships where user_id=v_uid and staff_role='manager' and status='active';
 update public.school_memberships set status='removed' where school_id=v_school and user_id=p_teacher and staff_role='teacher' and status<>'removed'; get diagnostics v_count=row_count;
 if v_count<>1 then return jsonb_build_object('error','معلم پیدا نشد'); end if;
 insert into public.school_admin_audit_v37(school_id,actor_id,target_id,action) values(v_school,v_uid,p_teacher,'teacher_membership_removed');
 return jsonb_build_object('ok',true);
end $$;

revoke all on function public.native_manager_create_teacher_invites_v40b(integer),public.native_manager_invites_v40b(),public.native_manager_revoke_invite_v40b(uuid),public.native_manager_set_teacher_active_v40b(uuid,boolean),public.native_manager_remove_teacher_v40b(uuid) from public,anon;
grant execute on function public.native_manager_create_teacher_invites_v40b(integer),public.native_manager_invites_v40b(),public.native_manager_revoke_invite_v40b(uuid),public.native_manager_set_teacher_active_v40b(uuid,boolean),public.native_manager_remove_teacher_v40b(uuid) to authenticated;
commit;
