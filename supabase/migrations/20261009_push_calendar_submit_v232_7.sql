-- V232.7 — اعلان‌های بیشتر: (۴) یادداشت تقویم معلم (فوری + یادآوری ۷ صبح روز یادداشت)، (۵) ارسال پاسخ دانش‌آموز → معلم
-- پیش‌نیاز: SQL v232 اجرا شده باشد (push_outbox / push_tokens / push_tick).
begin;

-- مخاطبان یک یادداشت تقویم (مثل exam_audience): all → همهٔ دانش‌آموزان معلم؛ classes/students/schools → جدول‌های مربوطه
create or replace function private.push_calendar_audience(p_note uuid) returns table(student_id uuid)
language sql security definer set search_path=public,pg_temp stable as $$
  with n as (select id,teacher_id,audience from public.calendar_notes where id=p_note)
  select s.student_id from (
    select cs.student_id from public.calendar_note_students cs, n where cs.note_id=n.id and n.audience='students'
    union select cm.student_id from public.calendar_note_classes cc join public.class_members cm on cm.class_id=cc.class_id, n where cc.note_id=n.id and n.audience='classes'
    union select ss.student_id from public.calendar_note_schools sc join public.school_students ss on ss.school_id=sc.school_id, n where sc.note_id=n.id and n.audience='schools'
    union select l.student_id from public.teacher_student_links l, n where l.teacher_id=n.teacher_id and n.audience='all'
    union select p.id from public.profiles p, n where p.teacher_id=n.teacher_id and p.role='student' and n.audience='all'
  ) s
  where exists(select 1 from public.push_tokens t where t.user_id=s.student_id and t.enabled);
$$;

-- (۴الف) فوری: یادداشت‌هایی که ۱ تا ۳۰ دقیقه پیش ساخته شده‌اند (جدول‌های مخاطب بعد از خود یادداشت درج می‌شوند → یک دقیقه صبر)
create or replace function private.push_enqueue_calendar_notes() returns int language plpgsql security definer set search_path=public,pg_temp as $$
declare r record; n int:=0; k int; v_body text;
begin
 for r in select id,title,body,on_date,teacher_id from public.calendar_notes
          where created_at<=now()-interval '1 minute' and created_at>now()-interval '30 minutes'
 loop
   v_body := coalesce(nullif(left(r.body,160),''), 'برای تاریخ '||to_char(r.on_date,'YYYY-MM-DD'));
   insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
   select a.student_id,'calendar','پیام جدید معلم: '||coalesce(r.title,''),v_body,
          jsonb_build_object('note_id',r.id,'page','calendar'),'cal:'||r.id::text||':'||a.student_id::text
   from private.push_calendar_audience(r.id) a
   on conflict(dedupe_key) do nothing;
   get diagnostics k=row_count; n:=n+k;
 end loop;
 return n;
end $$;

-- (۴ب) یادآوری صبح: ساعت ۷:۰۰ تا ۷:۱۰ تهران، یادداشت‌هایی که تاریخشان امروز است (و دست‌کم ۳۰ دقیقه پیش ساخته شده‌اند)
create or replace function private.push_enqueue_calendar_reminders() returns int language plpgsql security definer set search_path=public,pg_temp as $$
declare r record; n int:=0; k int; v_now time; v_today date;
begin
 v_now := (now() at time zone 'Asia/Tehran')::time;
 v_today := (now() at time zone 'Asia/Tehran')::date;
 if v_now < time '07:00' or v_now > time '07:10' then return 0; end if;
 for r in select id,title,body,teacher_id from public.calendar_notes where on_date=v_today and created_at<=now()-interval '30 minutes'
 loop
   insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
   select a.student_id,'calendar_reminder','یادآوری امروز: '||coalesce(r.title,''),coalesce(nullif(left(r.body,160),''),'امروز در تقویم شما ثبت شده است.'),
          jsonb_build_object('note_id',r.id,'page','calendar'),'calrem:'||r.id::text||':'||a.student_id::text||':'||v_today::text
   from private.push_calendar_audience(r.id) a
   on conflict(dedupe_key) do nothing;
   get diagnostics k=row_count; n:=n+k;
 end loop;
 return n;
end $$;

-- (۵) ارسال پاسخ دانش‌آموز → معلم آزمون
create or replace function private.push_on_answer_submitted() returns trigger language plpgsql security definer set search_path=public,pg_temp as $$
declare v_title text; v_teacher uuid; v_student text;
begin
 if new.submitted_at is null then return new; end if;
 if tg_op='UPDATE' and old.submitted_at is not null then return new; end if;
 select e.title,e.teacher_id into v_title,v_teacher from public.exams e where e.id=new.exam_id;
 select coalesce(nullif(display_name,''),full_name) into v_student from public.profiles where id=new.student_id;
 perform private.push_enqueue(v_teacher,'submit','پاسخ جدید دریافت شد',
   coalesce(v_student,'یک دانش‌آموز')||' آزمون «'||coalesce(v_title,'')||'» را ارسال کرد.',
   jsonb_build_object('exam_id',new.exam_id,'page','grading'),'submit:'||new.id::text);
 return new;
end $$;
drop trigger if exists push_on_answer_submitted on public.answers;
create trigger push_on_answer_submitted after insert or update of submitted_at on public.answers
for each row execute function private.push_on_answer_submitted();

-- tick: صف‌سازهای جدید
create or replace function private.push_tick() returns void language plpgsql security definer set search_path=public,pg_temp as $$
declare c record;
begin
 perform private.push_enqueue_exam_opens();
 perform private.push_enqueue_calendar_notes();
 perform private.push_enqueue_calendar_reminders();
 if exists(select 1 from public.push_outbox where state='pending' and attempts<5) then
   select * into c from private.push_config where id=1;
   perform net.http_post(url:=c.function_url, headers:=jsonb_build_object('Content-Type','application/json','x-push-secret',c.secret), body:='{}'::jsonb, timeout_milliseconds:=20000);
 end if;
end $$;

commit;
select 'V232.7 OK' as result;
