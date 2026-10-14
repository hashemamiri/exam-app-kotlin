-- V284 — مرکز اعلان (داخل اپ/سایت) + رویدادهای جدید.
-- (۱) push_outbox تاریخچهٔ اعلان هر کاربر می‌شود: ستون read_at؛ درج اعلان دیگر به داشتن توکن وابسته نیست
--     (کسی که Push ندارد هم اعلان را در مرکز اعلان می‌بیند؛ dispatcher چنین ردیفی را 'skipped' می‌کند).
-- (۲) RPC: native_my_notifications_v284(p_limit) / native_notifications_read_v284(p_ids)
-- (۳) رویدادهای جدید: یادآوری ۱ ساعت قبل از باز شدن آزمون، «۳۰ دقیقه تا پایان مهلت» برای کسانی که ارسال نکرده‌اند، افزوده‌شدن به کلاس.
-- پیش‌نیاز: SQL v232، v232.7، v232.9. اجرای دستی یک‌بار در SQL Editor.

begin;

alter table public.push_outbox add column if not exists read_at timestamptz;
create index if not exists push_outbox_user_idx on public.push_outbox(user_id, created_at desc);

-- (۱) push_enqueue بدون شرط توکن
create or replace function private.push_enqueue(p_user uuid, p_kind text, p_title text, p_body text, p_data jsonb, p_dedupe text)
returns void language sql security definer set search_path=public,pg_temp as $$
  insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
  select p_user,p_kind,left(p_title,120),left(p_body,400),coalesce(p_data,'{}'::jsonb),p_dedupe
  where p_user is not null
  on conflict(dedupe_key) do nothing;
$$;

-- مخاطبان آزمون (مشترک برای باز شدن / یادآوری / پایان مهلت)
create or replace function private.exam_audience_v284(p_exam text) returns table(student_id uuid)
language sql security definer set search_path=public,pg_temp stable as $$
  with e as (select id,teacher_id from public.exams where id=p_exam),
  explicit as (
    select student_id from public.exam_audience_students where exam_id=p_exam
    union select cm.student_id from public.exam_audience_classes ac join public.class_members cm on cm.class_id=ac.class_id where ac.exam_id=p_exam
    union select ss.student_id from public.exam_audience_schools sc join public.school_students ss on ss.school_id=sc.school_id where sc.exam_id=p_exam
  )
  select student_id from explicit
  union
  select l.student_id from (select student_id from public.teacher_student_links l, e where l.teacher_id=e.teacher_id
                            union select p.id from public.profiles p, e where p.teacher_id=e.teacher_id and p.role='student') l
  where not exists(select 1 from explicit);
$$;

-- باز شدن آزمون: همان V232.9 ولی بدون شرط توکن و با مخاطب مشترک
create or replace function private.push_enqueue_exam_opens() returns int language plpgsql security definer set search_path=public,pg_temp as $$
declare r record; n int:=0; k int;
begin
 for r in select e.id,e.title,e.code from public.exams e
          where e.opens_at is not null and e.opens_at<=now() and e.opens_at>now()-interval '15 minutes'
            and (e.closes_at is null or e.closes_at>now()) and coalesce(e.is_open,true)
 loop
   insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
   select s.student_id,'exam_open','آزمون باز شد','آزمون «'||coalesce(r.title,'')||'» از همین حالا قابل شرکت است.',
          jsonb_build_object('exam_id',r.id,'page','exam','code',coalesce(r.code,'')),'open:'||r.id||':'||s.student_id::text
   from private.exam_audience_v284(r.id) s
   on conflict(dedupe_key) do nothing;
   get diagnostics k=row_count; n:=n+k;
 end loop;
 return n;
end $$;

-- (۳الف) یادآوری ۱ ساعت قبل از باز شدن (پنجرهٔ ۵۰ تا ۶۰ دقیقه؛ dedupe یک‌بار)
create or replace function private.push_enqueue_exam_reminders() returns int language plpgsql security definer set search_path=public,pg_temp as $$
declare r record; n int:=0; k int;
begin
 for r in select e.id,e.title,e.code,e.opens_at from public.exams e
          where e.opens_at is not null and e.opens_at>now()+interval '50 minutes' and e.opens_at<=now()+interval '60 minutes'
            and coalesce(e.is_open,true)
 loop
   insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
   select s.student_id,'exam_soon','یک ساعت تا شروع آزمون','آزمون «'||coalesce(r.title,'')||'» حدود یک ساعت دیگر باز می‌شود.',
          jsonb_build_object('exam_id',r.id,'page','exam','code',coalesce(r.code,'')),'soon:'||r.id||':'||s.student_id::text
   from private.exam_audience_v284(r.id) s
   on conflict(dedupe_key) do nothing;
   get diagnostics k=row_count; n:=n+k;
 end loop;
 return n;
end $$;

-- (۳ب) ۳۰ دقیقه تا پایان مهلت، فقط برای کسانی که هنوز ارسال نکرده‌اند
create or replace function private.push_enqueue_exam_closing() returns int language plpgsql security definer set search_path=public,pg_temp as $$
declare r record; n int:=0; k int;
begin
 for r in select e.id,e.title,e.code from public.exams e
          where e.closes_at is not null and e.closes_at>now()+interval '20 minutes' and e.closes_at<=now()+interval '30 minutes'
            and coalesce(e.is_open,true)
 loop
   insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
   select s.student_id,'exam_closing','۳۰ دقیقه تا پایان مهلت','مهلت آزمون «'||coalesce(r.title,'')||'» تا نیم ساعت دیگر تمام می‌شود و هنوز پاسخی از شما ثبت نشده است.',
          jsonb_build_object('exam_id',r.id,'page','exam','code',coalesce(r.code,'')),'closing:'||r.id||':'||s.student_id::text
   from private.exam_audience_v284(r.id) s
   where not exists(select 1 from public.answers a where a.exam_id=r.id and a.student_id=s.student_id and a.submitted_at is not null)
   on conflict(dedupe_key) do nothing;
   get diagnostics k=row_count; n:=n+k;
 end loop;
 return n;
end $$;

-- (۳ج) افزوده‌شدن به کلاس
create or replace function private.push_on_class_member_added() returns trigger language plpgsql security definer set search_path=public,pg_temp as $$
declare v_name text;
begin
  select name into v_name from public.classes where id=new.class_id;
  perform private.push_enqueue(new.student_id,'class_added','به کلاس اضافه شدید',
    'شما به کلاس «'||coalesce(v_name,'')||'» اضافه شدید.',
    jsonb_build_object('class_id',new.class_id,'page','exam'),
    'cls:'||new.class_id::text||':'||new.student_id::text);
  return new;
end $$;
drop trigger if exists push_on_class_member_added on public.class_members;
create trigger push_on_class_member_added after insert on public.class_members
for each row execute function private.push_on_class_member_added();

-- tick با صف‌سازهای جدید
create or replace function private.push_tick() returns void language plpgsql security definer set search_path=public,pg_temp as $$
declare c record;
begin
 perform private.push_enqueue_exam_opens();
 perform private.push_enqueue_exam_reminders();
 perform private.push_enqueue_exam_closing();
 perform private.push_enqueue_calendar_notes();
 perform private.push_enqueue_calendar_reminders();
 if exists(select 1 from public.push_outbox where state='pending' and attempts<5) then
   select * into c from private.push_config where id=1;
   perform net.http_post(url:=c.function_url, headers:=jsonb_build_object('Content-Type','application/json','x-push-secret',c.secret), body:='{}'::jsonb, timeout_milliseconds:=20000);
 end if;
end $$;

-- (۲) مرکز اعلان
create or replace function public.native_my_notifications_v284(p_limit int default 50)
returns jsonb language sql security definer set search_path=public,pg_temp stable as $$
  select jsonb_build_object(
    'unread', (select count(*) from public.push_outbox o where o.user_id=auth.uid() and o.read_at is null),
    'items', coalesce((select jsonb_agg(jsonb_build_object('id',o.id,'kind',o.kind,'title',o.title,'body',o.body,'data',o.data,
                                                      'created_at',o.created_at,'read_at',o.read_at) order by o.created_at desc)
                       from (select * from public.push_outbox where user_id=auth.uid() order by created_at desc limit greatest(1,least(coalesce(p_limit,50),200))) o), '[]'::jsonb));
$$;
revoke all on function public.native_my_notifications_v284(int) from public;
grant execute on function public.native_my_notifications_v284(int) to authenticated;

create or replace function public.native_notifications_read_v284(p_ids bigint[] default null)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare n int;
begin
  update public.push_outbox set read_at=now()
  where user_id=auth.uid() and read_at is null and (p_ids is null or id = any(p_ids));
  get diagnostics n=row_count;
  return jsonb_build_object('ok',true,'marked',n);
end $$;
revoke all on function public.native_notifications_read_v284(bigint[]) from public;
grant execute on function public.native_notifications_read_v284(bigint[]) to authenticated;

-- نگهداری تاریخچه: ۶۰ روز (قبلاً ۱۴ روز)
select cron.unschedule(jobid) from cron.job where jobname='push_outbox_gc_v232';
select cron.schedule('push_outbox_gc_v232','17 3 * * *',$$delete from public.push_outbox where created_at<now()-interval '60 days'$$);

commit;
select 'V284 OK' as result;
