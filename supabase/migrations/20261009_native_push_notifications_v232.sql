-- V232 — اعلان‌ها (Push): توکن دستگاه‌ها، صندوق خروجی اعلان، تریگرهای رویداد، زمان‌بند ارسال.
-- شبکه فقط در Edge Function «push-dispatch» است؛ این فایل فقط جدول/تابع/تریگر می‌سازد.
-- اجرا: یک بار در SQL Editor. در پایان، خروجی «select secret from private.push_config» را کپی کنید
-- و به‌عنوان Secret با نام PUSH_WEBHOOK_SECRET در Edge Functions بگذارید (راهنمای چت).
begin;

create extension if not exists pg_cron;
create extension if not exists pg_net;
create schema if not exists private;

-- رمز مشترک بین زمان‌بند (pg_cron → pg_net) و Edge Function
create table if not exists private.push_config(id int primary key default 1 check(id=1), secret text not null, function_url text not null);
insert into private.push_config(id,secret,function_url)
values(1, encode(gen_random_bytes(24),'hex'), 'https://eazwuyrymsvdkwckdpco.supabase.co/functions/v1/push-dispatch')
on conflict(id) do nothing;

-- توکن‌های FCM / Web Push
create table if not exists public.push_tokens(
  token text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  platform text not null check(platform in('android','web')),
  enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  last_error text
);
create index if not exists push_tokens_user_idx on public.push_tokens(user_id) where enabled;
alter table public.push_tokens enable row level security;
-- هیچ policy مستقیم: فقط از طریق RPCها (security definer) و service role

create or replace function public.native_push_register_v1(p_token text, p_platform text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
begin
 if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 if coalesce(p_token,'')='' or length(p_token)>4096 then return jsonb_build_object('error','توکن نامعتبر است'); end if;
 if p_platform not in('android','web') then return jsonb_build_object('error','سکوی نامعتبر'); end if;
 insert into public.push_tokens(token,user_id,platform,enabled,updated_at,last_error)
 values(p_token,auth.uid(),p_platform,true,now(),null)
 on conflict(token) do update set user_id=excluded.user_id,platform=excluded.platform,enabled=true,updated_at=now(),last_error=null;
 return jsonb_build_object('ok',true);
end $$;

create or replace function public.native_push_unregister_v1(p_token text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
begin
 if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 delete from public.push_tokens where token=p_token and user_id=auth.uid();
 return jsonb_build_object('ok',true);
end $$;

-- صندوق خروجی: هر ردیف = یک اعلان برای یک کاربر (Edge Function می‌خواند و می‌فرستد)
create table if not exists public.push_outbox(
  id bigserial primary key,
  user_id uuid not null,
  kind text not null,           -- grade | exam_open | manager_request | request_decided
  title text not null,
  body text not null,
  data jsonb not null default '{}'::jsonb,
  dedupe_key text unique,
  state text not null default 'pending' check(state in('pending','sent','failed','skipped')),
  attempts int not null default 0,
  created_at timestamptz not null default now(),
  sent_at timestamptz,
  last_error text
);
create index if not exists push_outbox_pending_idx on public.push_outbox(created_at) where state='pending';
alter table public.push_outbox enable row level security;

create or replace function private.push_enqueue(p_user uuid, p_kind text, p_title text, p_body text, p_data jsonb, p_dedupe text)
returns void language sql security definer set search_path=public,pg_temp as $$
  insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
  select p_user,p_kind,left(p_title,120),left(p_body,400),coalesce(p_data,'{}'::jsonb),p_dedupe
  where p_user is not null and exists(select 1 from public.push_tokens t where t.user_id=p_user and t.enabled)
  on conflict(dedupe_key) do nothing;
$$;

-- (۱) نمرهٔ جدید: وقتی پاسخ تصحیح‌شده می‌شود
create or replace function private.push_on_answer_graded() returns trigger language plpgsql security definer set search_path=public,pg_temp as $$
declare v_title text;
begin
 if coalesce(new.graded,false) and not coalesce(old.graded,false) then
   select e.title into v_title from public.exams e where e.id=new.exam_id;
   perform private.push_enqueue(new.student_id,'grade','نمرهٔ آزمون ثبت شد',
     'نمرهٔ آزمون «'||coalesce(v_title,'')||'» آمادهٔ مشاهده است.',
     jsonb_build_object('exam_id',new.exam_id,'page','grades'),
     'grade:'||new.id::text);
 end if;
 return new;
end $$;
drop trigger if exists push_on_answer_graded on public.answers;
create trigger push_on_answer_graded after update of graded on public.answers
for each row execute function private.push_on_answer_graded();

-- (۲) درخواست مدیر → معلم؛ تصمیم معلم → مدیر
create or replace function private.push_on_manager_request() returns trigger language plpgsql security definer set search_path=public,pg_temp as $$
declare v_mgr text; v_tch text;
begin
 if tg_op='INSERT' then
   select coalesce(nullif(display_name,''),full_name) into v_mgr from public.profiles where id=new.manager_id;
   perform private.push_enqueue(new.teacher_id,'manager_request','درخواست جدید مدیر',
     coalesce(v_mgr,'مدیر مدرسه')||' درخواست '||case when new.action='delete' then 'حذف' else 'ویرایش' end||' '||case when new.target_type='class' then 'کلاس' else 'دانش‌آموز' end||' داده است.',
     jsonb_build_object('request_id',new.id,'page','requests'),'mreq:'||new.id::text);
 elsif tg_op='UPDATE' and new.status in('approved','rejected') and old.status='pending' then
   select coalesce(nullif(display_name,''),full_name) into v_tch from public.profiles where id=new.teacher_id;
   perform private.push_enqueue(new.manager_id,'request_decided','پاسخ معلم به درخواست',
     coalesce(v_tch,'معلم')||' درخواست شما را '||case when new.status='approved' then 'تأیید' else 'رد' end||' کرد.',
     jsonb_build_object('request_id',new.id,'page','requests'),'mdec:'||new.id::text||':'||new.status);
 end if;
 return new;
end $$;
drop trigger if exists push_on_manager_request on public.manager_approval_requests;
create trigger push_on_manager_request after insert or update of status on public.manager_approval_requests
for each row execute function private.push_on_manager_request();

-- (۳) باز شدن آزمون: هر دقیقه آزمون‌هایی که opens_at در ۱۵ دقیقهٔ اخیر است؛ مخاطبان = audience یا همهٔ دانش‌آموزان معلم
create or replace function private.push_enqueue_exam_opens() returns int language plpgsql security definer set search_path=public,pg_temp as $$
declare r record; n int:=0;
begin
 for r in select e.id,e.title,e.teacher_id from public.exams e
          where e.opens_at is not null and e.opens_at<=now() and e.opens_at>now()-interval '15 minutes'
            and (e.closes_at is null or e.closes_at>now()) and coalesce(e.is_open,true)
 loop
   insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
   select s.student_id,'exam_open','آزمون باز شد','آزمون «'||coalesce(r.title,'')||'» از همین حالا قابل شرکت است.',
          jsonb_build_object('exam_id',r.id,'page','exam'),'open:'||r.id||':'||s.student_id::text
   from (
     select student_id from public.exam_audience_students where exam_id=r.id
     union select cm.student_id from public.exam_audience_classes ac join public.class_members cm on cm.class_id=ac.class_id where ac.exam_id=r.id
     union select ss.student_id from public.exam_audience_schools sc join public.school_students ss on ss.school_id=sc.school_id where sc.exam_id=r.id
   ) s
   where exists(select 1 from public.push_tokens t where t.user_id=s.student_id and t.enabled)
   on conflict(dedupe_key) do nothing;
   get diagnostics n=row_count;
   if not exists(select 1 from public.exam_audience_students where exam_id=r.id)
      and not exists(select 1 from public.exam_audience_classes where exam_id=r.id)
      and not exists(select 1 from public.exam_audience_schools where exam_id=r.id) then
     insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
     select l.student_id,'exam_open','آزمون باز شد','آزمون «'||coalesce(r.title,'')||'» از همین حالا قابل شرکت است.',
            jsonb_build_object('exam_id',r.id,'page','exam'),'open:'||r.id||':'||l.student_id::text
     from (select student_id from public.teacher_student_links where teacher_id=r.teacher_id
           union select id from public.profiles where teacher_id=r.teacher_id and role='student') l
     where exists(select 1 from public.push_tokens t where t.user_id=l.student_id and t.enabled)
     on conflict(dedupe_key) do nothing;
   end if;
 end loop;
 return n;
end $$;

-- زمان‌بند: هر دقیقه (الف) صف باز شدن آزمون‌ها (ب) فراخوانی Edge Function اگر چیزی در صف باشد
create or replace function private.push_tick() returns void language plpgsql security definer set search_path=public,pg_temp as $$
declare c record;
begin
 perform private.push_enqueue_exam_opens();
 if exists(select 1 from public.push_outbox where state='pending' and attempts<5) then
   select * into c from private.push_config where id=1;
   perform net.http_post(url:=c.function_url, headers:=jsonb_build_object('Content-Type','application/json','x-push-secret',c.secret), body:='{}'::jsonb, timeout_milliseconds:=20000);
 end if;
end $$;
select cron.unschedule(jobid) from cron.job where jobname='push_tick_v232';
select cron.schedule('push_tick_v232','* * * * *',$$select private.push_tick()$$);

-- ۱۴ روز نگه‌داری صندوق
select cron.unschedule(jobid) from cron.job where jobname='push_outbox_gc_v232';
select cron.schedule('push_outbox_gc_v232','17 3 * * *',$$delete from public.push_outbox where created_at<now()-interval '14 days'$$);

revoke all on function public.native_push_register_v1(text,text) from public,anon;
revoke all on function public.native_push_unregister_v1(text) from public,anon;
grant execute on function public.native_push_register_v1(text,text) to authenticated;
grant execute on function public.native_push_unregister_v1(text) to authenticated;

commit;

-- این مقدار را کپی کنید و به‌عنوان Secret با نام PUSH_WEBHOOK_SECRET در Edge Functions بگذارید:
select secret from private.push_config where id=1;
