-- V232.9 — اعلان «آزمون باز شد» کد آزمون را هم می‌فرستد تا ضربه روی اعلان مستقیم همان آزمون را باز کند (اپ و سایت)
begin;
create or replace function private.push_enqueue_exam_opens() returns int language plpgsql security definer set search_path=public,pg_temp as $$
declare r record; n int:=0; v_data jsonb;
begin
 for r in select e.id,e.title,e.teacher_id,e.code from public.exams e
          where e.opens_at is not null and e.opens_at<=now() and e.opens_at>now()-interval '15 minutes'
            and (e.closes_at is null or e.closes_at>now()) and coalesce(e.is_open,true)
 loop
   v_data := jsonb_build_object('exam_id',r.id,'page','exam','code',coalesce(r.code,''));
   insert into public.push_outbox(user_id,kind,title,body,data,dedupe_key)
   select s.student_id,'exam_open','آزمون باز شد','آزمون «'||coalesce(r.title,'')||'» از همین حالا قابل شرکت است.',
          v_data,'open:'||r.id||':'||s.student_id::text
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
            v_data,'open:'||r.id||':'||l.student_id::text
     from (select student_id from public.teacher_student_links where teacher_id=r.teacher_id
           union select id from public.profiles where teacher_id=r.teacher_id and role='student') l
     where exists(select 1 from public.push_tokens t where t.user_id=l.student_id and t.enabled)
     on conflict(dedupe_key) do nothing;
   end if;
 end loop;
 return n;
end $$;
commit;
select 'V232.9 OK' as result;
