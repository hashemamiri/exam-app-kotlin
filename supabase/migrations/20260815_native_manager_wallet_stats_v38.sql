-- V38: شارژ مدیر، انتقال اتمیک تومان به معلم و آمار آموزشی مدرسه.
begin;

-- دادهٔ جدید کادر مدرسه خودکار scope می‌شود؛ داده‌های قدیمی طبق تصمیم کاربر
-- بدون انتخاب مدیر منتقل نمی‌شوند و school_id=null باقی می‌مانند.
alter table public.exams add column if not exists school_id uuid references public.schools(id) on delete set null;
create index if not exists idx_exams_school_v38 on public.exams(school_id);
create index if not exists idx_classes_school_v38 on public.classes(school_id);
create or replace function public.native_scope_new_school_row_v38()
returns trigger language plpgsql security definer set search_path=public,pg_temp as $$
begin
 if new.school_id is null then
   select school_id into new.school_id from public.school_memberships
   where user_id=new.teacher_id and staff_role='teacher' and status='active';
 end if;
 return new;
end $$;
drop trigger if exists trg_scope_new_class_v38 on public.classes;
create trigger trg_scope_new_class_v38 before insert on public.classes for each row execute function public.native_scope_new_school_row_v38();
drop trigger if exists trg_scope_new_exam_v38 on public.exams;
create trigger trg_scope_new_exam_v38 before insert on public.exams for each row execute function public.native_scope_new_school_row_v38();

create table if not exists public.manager_wallet_transfers_v38 (
 operation_id uuid primary key, school_id uuid not null references public.schools(id) on delete cascade,
 manager_id uuid not null references auth.users(id), teacher_id uuid not null references auth.users(id),
 amount_toman bigint not null check(amount_toman>0 and amount_toman%1000=0), created_at timestamptz not null default now()
);
create index if not exists idx_manager_transfers_school_v38 on public.manager_wallet_transfers_v38(school_id,created_at desc);
alter table public.manager_wallet_transfers_v38 enable row level security;
drop policy if exists v38_manager_transfer_read on public.manager_wallet_transfers_v38;
create policy v38_manager_transfer_read on public.manager_wallet_transfers_v38 for select to authenticated using(manager_id=auth.uid());

create or replace function public.native_manager_transfer_wallet_v38(p_teacher uuid,p_amount_toman bigint,p_operation uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_manager uuid:=auth.uid(); v_school uuid; v_manager_balance bigint; v_teacher_balance bigint;
begin
 if v_manager is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 if p_operation is null then return jsonb_build_object('error','شناسه عملیات لازم است'); end if;
 if p_amount_toman is null or p_amount_toman<=0 or p_amount_toman%1000<>0 then return jsonb_build_object('error','مبلغ باید مثبت و مضرب ۱٬۰۰۰ تومان باشد'); end if;
 select school_id into v_school from public.school_memberships where user_id=v_manager and staff_role='manager' and status='active';
 if v_school is null then return jsonb_build_object('error','مدرسه فعال پیدا نشد'); end if;
 if not exists(select 1 from public.school_memberships where school_id=v_school and user_id=p_teacher and staff_role='teacher' and status='active') then return jsonb_build_object('error','معلم فعال این مدرسه نیست'); end if;
 if exists(select 1 from public.manager_wallet_transfers_v38 where operation_id=p_operation) then
   select balance into v_manager_balance from public.wallets where user_id=v_manager;
   select balance into v_teacher_balance from public.wallets where user_id=p_teacher;
   return jsonb_build_object('ok',true,'already_applied',true,'manager_balance',coalesce(v_manager_balance,0),'teacher_balance',coalesce(v_teacher_balance,0));
 end if;
 insert into public.wallets(user_id,balance) values(v_manager,0) on conflict do nothing;
 insert into public.wallets(user_id,balance) values(p_teacher,0) on conflict do nothing;
 select balance into v_manager_balance from public.wallets where user_id=v_manager for update;
 select balance into v_teacher_balance from public.wallets where user_id=p_teacher for update;
 if v_manager_balance<p_amount_toman then return jsonb_build_object('error','موجودی کیف پول مدیر کافی نیست','balance',v_manager_balance); end if;
 if v_teacher_balance+p_amount_toman>10000000 then return jsonb_build_object('error','موجودی معلم از سقف مجاز بیشتر می‌شود'); end if;
 update public.wallets set balance=balance-p_amount_toman,updated_at=now() where user_id=v_manager returning balance into v_manager_balance;
 update public.wallets set balance=balance+p_amount_toman,updated_at=now() where user_id=p_teacher returning balance into v_teacher_balance;
 insert into public.manager_wallet_transfers_v38(operation_id,school_id,manager_id,teacher_id,amount_toman) values(p_operation,v_school,v_manager,p_teacher,p_amount_toman);
 insert into public.wallet_tx(user_id,amount,reason,balance_after,operation_key) values(v_manager,-p_amount_toman,'school_transfer_to_teacher:'||p_teacher,v_manager_balance,p_operation);
 insert into public.wallet_tx(user_id,amount,reason,balance_after,operation_key) values(p_teacher,p_amount_toman,'school_transfer_from_manager:'||v_manager,v_teacher_balance,gen_random_uuid());
 insert into public.school_admin_audit_v37(school_id,actor_id,target_id,action,details) values(v_school,v_manager,p_teacher,'wallet_transfer',jsonb_build_object('amount_toman',p_amount_toman,'operation_id',p_operation));
 return jsonb_build_object('ok',true,'manager_balance',v_manager_balance,'teacher_balance',v_teacher_balance,'amount',p_amount_toman);
end $$;
revoke all on function public.native_manager_transfer_wallet_v38(uuid,bigint,uuid) from public,anon;
grant execute on function public.native_manager_transfer_wallet_v38(uuid,bigint,uuid) to authenticated;

create or replace function public.native_manager_teachers_v37()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
 select coalesce((select jsonb_build_object('ok',true,'items',coalesce(jsonb_agg(jsonb_build_object('id',p.id,'full_name',p.full_name,'username',p.username,'email',u.email,'joined_at',sm.joined_at,'wallet_balance',coalesce(w.balance,0)) order by p.full_name),'[]'::jsonb)) from public.school_memberships me join public.school_memberships sm on sm.school_id=me.school_id and sm.staff_role='teacher' and sm.status='active' join public.profiles p on p.id=sm.user_id join auth.users u on u.id=sm.user_id left join public.wallets w on w.user_id=sm.user_id where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'),jsonb_build_object('error','دسترسی مدیر یافت نشد'));
$$;
revoke all on function public.native_manager_teachers_v37() from public,anon;
grant execute on function public.native_manager_teachers_v37() to authenticated;

-- پرداخت بانکی برای manager نیز مجاز است؛ Edge همچنان کاربر جاری را ارسال می‌کند.
create or replace function public.native_create_wallet_payment_order(p_user uuid,p_amount_toman bigint,p_provider text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_balance bigint; v_id bigint; v_operation uuid;
begin
 if p_user is null or not exists(select 1 from public.profiles where id=p_user and role in('teacher','manager')) then return jsonb_build_object('error','حساب کادر مدرسه معتبر نیست'); end if;
 if p_amount_toman<100000 or p_amount_toman>10000000 or p_amount_toman%10000<>0 then return jsonb_build_object('error','مبلغ شارژ نامعتبر است'); end if;
 if p_provider not in('zarinpal','idpay','sandbox') then return jsonb_build_object('error','درگاه نامعتبر است'); end if;
 insert into public.wallets(user_id,balance) values(p_user,0) on conflict do nothing;
 select balance into v_balance from public.wallets where user_id=p_user for update;
 if v_balance+p_amount_toman>10000000 then return jsonb_build_object('error','موجودی پس از شارژ از سقف مجاز بیشتر می‌شود'); end if;
 update public.wallet_payment_orders set status='failed',error_code='expired_open_order' where user_id=p_user and status in('pending','requested','verifying') and created_at<now()-interval '30 minutes';
 if exists(select 1 from public.wallet_payment_orders where user_id=p_user and status in('pending','requested','verifying')) then return jsonb_build_object('error','یک سفارش پرداخت باز دارید'); end if;
 insert into public.wallet_payment_orders(user_id,amount_toman,amount_rial,provider) values(p_user,p_amount_toman,p_amount_toman*10,p_provider) returning id,operation_id into v_id,v_operation;
 return jsonb_build_object('ok',true,'id',v_id,'operation_id',v_operation);
end $$;
revoke all on function public.native_create_wallet_payment_order(uuid,bigint,text) from public,anon,authenticated;
grant execute on function public.native_create_wallet_payment_order(uuid,bigint,text) to service_role;

create or replace function public.native_manager_school_summary_v36()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
with mine as(select school_id from public.school_memberships where user_id=auth.uid() and staff_role='manager' and status='active'),
teachers as(select sm.user_id from public.school_memberships sm join mine on mine.school_id=sm.school_id where sm.staff_role='teacher' and sm.status='active'),
examset as(select e.id,e.teacher_id,e.total_score from public.exams e join mine on mine.school_id=e.school_id),
answer_stats as(select count(*) answers,coalesce(avg(case when coalesce(e.total_score,0)>0 then coalesce(a.total_grade,0)*100.0/e.total_score end),0) average_percent from public.answers a join examset e on e.id=a.exam_id)
select coalesce((select jsonb_build_object('ok',true,'school_id',s.id,'school_name',s.name,'province',s.province,'city',s.city,
'teachers',(select count(*) from teachers),'students',(select count(*) from public.school_students ss where ss.school_id=s.id),'classes',(select count(*) from public.classes c where c.school_id=s.id),'exams',(select count(*) from examset),'answers',(select answers from answer_stats),'average_percent',(select round(average_percent::numeric,1) from answer_stats),'distributed_toman',(select coalesce(sum(amount_toman),0) from public.manager_wallet_transfers_v38 x where x.school_id=s.id),'teacher_activity',(select coalesce(jsonb_agg(jsonb_build_object('teacher_id',p.id,'name',p.full_name,'exams',(select count(*) from public.exams e where e.teacher_id=p.id and e.school_id=s.id),'classes',(select count(*) from public.classes c where c.teacher_id=p.id and c.school_id=s.id),'students',(select count(*) from public.school_students ss where ss.created_by=p.id and ss.school_id=s.id),'wallet_balance',coalesce((select balance from public.wallets w where w.user_id=p.id),0)) order by p.full_name),'[]'::jsonb) from public.profiles p where p.id in(select user_id from teachers))) from mine join public.schools s on s.id=mine.school_id),jsonb_build_object('error','مدرسه فعال پیدا نشد'));
$$;
revoke all on function public.native_manager_school_summary_v36() from public,anon;
grant execute on function public.native_manager_school_summary_v36() to authenticated;
commit;
