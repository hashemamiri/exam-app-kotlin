-- V260.8 — یافته‌های آزمون کامل جریان مدیر ↔ معلم ↔ دانش‌آموز (۱۴۰۵/۰۷/۲۰، حساب‌های amiri/teach)
-- ۱) فهرست «دانش‌آموزان مدرسه» برای مدیرِ چندمدرسه‌ای هر دانش‌آموز را دو بار نشان می‌داد (یک ردیف به ازای هر
--    مدرسه) و دانش‌آموزانِ کلاسی که معلم «با مدیر به اشتراک گذاشته» ولی قبل از پیوستن معلم به مدرسه ساخته
--    شده بودند، اصلاً دیده نمی‌شدند (ردیف school_students نداشتند).
-- ۲) اشتراک کلاس با مدیر (native_teacher_share_class_v137) اعضای کلاس را به school_students وصل نمی‌کرد →
--    مدیر نمی‌توانست برای آن‌ها درخواست ویرایش/رمز/حذف بفرستد.
-- ۳) پیام «درخواست برای تأیید معلم ارسال شد» در نسخهٔ اجراشده روی سرور با کدگذاری خراب (╪»╪▒…) ذخیره شده بود؛
--    توابع V41 عیناً دوباره ساخته می‌شوند + نام هدف و خلاصهٔ تغییر برای معلم + فهرست درخواست‌های خود مدیر.
-- ۴) exam_autograde_info قدیمی سؤال تشریحی (type=essay) را نمی‌شمرد (desc_q=0, fully_auto=true).
-- ۵) کارنامهٔ کلاس برای مدیر (native_manager_class_report_v1): نمرهٔ هر دانش‌آموزِ کلاس در هر آزمونِ کلاس.
begin;

-- ---------------------------------------------------------------- ۱) دانش‌آموزان مدرسه (بدون تکرار + اعضای کلاس‌های اشتراکی)
create or replace function public.native_manager_school_students_v40c()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
with mine as (
    select me.school_id from public.school_memberships me
    where me.user_id=auth.uid() and me.staff_role='manager' and me.status='active'
), vis as (
    select ss.student_id
    from public.school_students ss join mine on mine.school_id=ss.school_id
    where ss.shared_with_manager or ss.created_by=auth.uid()
       or exists(select 1 from public.school_memberships mm where mm.user_id=ss.created_by and mm.school_id=ss.school_id and mm.staff_role='manager' and mm.status<>'removed')
       or exists(select 1 from public.class_members cm2 join public.classes c2 on c2.id=cm2.class_id where cm2.student_id=ss.student_id and c2.school_id=ss.school_id and (c2.shared_with_manager or coalesce(c2.created_by,c2.teacher_id)<>c2.teacher_id))
    union
    select cm.student_id
    from public.class_members cm join public.classes c on c.id=cm.class_id join mine on mine.school_id=c.school_id
    where c.shared_with_manager or c.created_by=auth.uid()
)
select case when not exists(select 1 from mine) then jsonb_build_object('error','مدرسه یافت نشد')
       else jsonb_build_object('ok',true,'items',coalesce((
            select jsonb_agg(jsonb_build_object('id',p.id,'full_name',p.full_name,'username',p.username) order by p.full_name)
            from (select distinct student_id from vis) v join public.profiles p on p.id=v.student_id where p.role='student'),'[]'::jsonb)) end;
$$;

-- ---------------------------------------------------------------- ۲) اشتراک کلاس → اتصال اعضا به مدرسه
create or replace function public.native_teacher_share_class_v137(p_class uuid, p_share boolean)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_school uuid;
  v_shared boolean;
begin
  if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
  if not exists(select 1 from public.classes c where c.id=p_class and c.teacher_id=auth.uid()) then
    return jsonb_build_object('error','کلاس شما نیست');
  end if;
  select school_id into v_school
    from public.school_memberships
   where user_id=auth.uid() and status='active'
   order by joined_at desc limit 1;
  if v_school is null then
    return jsonb_build_object('error','شما عضو هیچ مدرسه‌ای نیستید؛ ابتدا با کد دعوت به مدرسه بپیوندید.');
  end if;
  update public.classes
     set shared_with_manager=p_share,
         school_id=coalesce(school_id, v_school)
   where id=p_class and teacher_id=auth.uid();
  if p_share then
    -- V260.8: اعضای کلاسِ اشتراکی عضو مدرسه می‌شوند تا مدیر بتواند آن‌ها را ببیند و درخواست تغییر بفرستد
    insert into public.school_students(school_id,student_id,created_by)
    select c.school_id, cm.student_id, auth.uid()
      from public.classes c join public.class_members cm on cm.class_id=c.id
     where c.id=p_class and c.school_id is not null
    on conflict do nothing;
  end if;
  select coalesce(c.shared_with_manager,false) and c.school_id is not null into v_shared
    from public.classes c where c.id=p_class;
  return jsonb_build_object('ok',true,'shared',coalesce(v_shared,false));
end $$;
revoke all on function public.native_teacher_share_class_v137(uuid,boolean) from public,anon;
grant execute on function public.native_teacher_share_class_v137(uuid,boolean) to authenticated;

-- ---------------------------------------------------------------- ۳) درخواست‌های مدیر ↔ معلم (V41 بازسازی + نام هدف)
create or replace function public.native_teacher_manager_requests_v41() returns jsonb language plpgsql security definer set search_path=public as $$
begin
 update manager_approval_requests set status='expired' where teacher_id=auth.uid() and status='pending' and expires_at<=now();
 return jsonb_build_object('ok',true,'items',coalesce((select jsonb_agg(jsonb_build_object(
   'id',r.id,'target_type',r.target_type,'action',r.action,'status',r.status,'expires_at',r.expires_at,'created_at',r.created_at,
   'manager_name',coalesce(p.full_name,''),
   'target_name',case when r.target_type='class' then (select c.name from classes c where c.id=r.target_id)
                      else (select s.full_name from profiles s where s.id=r.target_id) end,
   'payload',coalesce(r.payload,'{}'::jsonb)) order by r.created_at desc)
   from manager_approval_requests r left join profiles p on p.id=r.manager_id where r.teacher_id=auth.uid() and r.created_at>now()-interval '30 days'),'[]'::jsonb));
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

-- فهرست درخواست‌های خودِ مدیر (در انتظار/تأیید/رد) — تا مدیر بداند سرنوشت درخواستش چه شد
create or replace function public.native_manager_my_requests_v1() returns jsonb language plpgsql security definer set search_path=public as $$
begin
 update manager_approval_requests set status='expired' where manager_id=auth.uid() and status='pending' and expires_at<=now();
 return jsonb_build_object('ok',true,'items',coalesce((select jsonb_agg(jsonb_build_object(
   'id',r.id,'target_type',r.target_type,'action',r.action,'status',r.status,'expires_at',r.expires_at,'created_at',r.created_at,'decided_at',r.decided_at,
   'teacher_name',coalesce(t.full_name,''),
   'target_name',case when r.target_type='class' then coalesce((select c.name from classes c where c.id=r.target_id), nullif(r.payload->>'name',''), 'کلاس حذف‌شده')
                      else coalesce((select s.full_name from profiles s where s.id=r.target_id),'حساب حذف‌شده') end,
   'payload',coalesce(r.payload,'{}'::jsonb)) order by r.created_at desc)
   from manager_approval_requests r left join profiles t on t.id=r.teacher_id where r.manager_id=auth.uid() and r.created_at>now()-interval '30 days'),'[]'::jsonb));
end $$;
revoke all on function public.native_manager_my_requests_v1() from public,anon;
grant execute on function public.native_teacher_manager_requests_v41() to authenticated;
grant execute on function public.native_teacher_decide_manager_request_v41(uuid,boolean,text) to authenticated;
grant execute on function public.native_manager_change_teacher_class_v41(uuid,text,jsonb) to authenticated;
grant execute on function public.native_manager_my_requests_v1() to authenticated;

-- ---------------------------------------------------------------- ۴) اطلاعات تصحیح خودکار (تشریحی‌ها درست شمرده شوند)
do $drop$
declare r record;
begin
    for r in
        select p.oid::regprocedure as sig
        from pg_proc p join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'public' and p.proname = 'exam_autograde_info'
    loop
        execute format('drop function %s', r.sig);
    end loop;
end
$drop$;

create or replace function public.exam_autograde_info(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam record;
    v_total integer := 0;
    v_desc integer := 0;
    v_pending integer := 0;
    v_auto integer := 0;
    v_q jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select * into v_exam from public.exams e where e.id = p_exam and e.teacher_id = v_uid limit 1;
    if v_exam.id is null then return jsonb_build_object('error', 'آزمون پیدا نشد یا متعلق به این حساب نیست'); end if;
    if jsonb_typeof(coalesce(v_exam.questions, 'null'::jsonb)) = 'array' then
        for v_q in select value from jsonb_array_elements(v_exam.questions) loop
            v_total := v_total + 1;
            if lower(coalesce(v_q ->> 'type', '')) in ('essay', 'long', 'descriptive') then v_desc := v_desc + 1; end if;
        end loop;
    end if;
    select count(*) filter (where coalesce(a.graded, false) = false),
           count(*) filter (where coalesce(a.auto_graded, false))
      into v_pending, v_auto
      from public.answers a where a.exam_id = v_exam.id;
    return jsonb_build_object('ok', true, 'total_q', v_total, 'desc_q', v_desc, 'partial', 0,
        'fully_auto', v_desc = 0, 'auto_only', v_pending, 'pending', v_pending, 'auto_graded', v_auto, 'ready_score', 0);
end;
$$;
revoke all on function public.exam_autograde_info(text) from public, anon;
grant execute on function public.exam_autograde_info(text) to authenticated;

-- ---------------------------------------------------------------- ۵) کارنامهٔ کلاس برای مدیر
create or replace function public.native_manager_class_report_v1(p_class uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_class record;
    v_exams jsonb;
    v_students jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select c.* into v_class
      from public.classes c
      join public.school_memberships me on me.school_id = c.school_id and me.user_id = v_uid and me.staff_role = 'manager' and me.status = 'active'
     where c.id = p_class
       and (coalesce(c.shared_with_manager, false) or c.created_by = v_uid)
     limit 1;
    if v_class.id is null then return jsonb_build_object('error', 'کلاس یافت نشد یا با مدیر به اشتراک گذاشته نشده است'); end if;

    -- آزمون‌های کلاس: مخاطبِ کلاس یا آزمون‌های همین معلم که عضوی از کلاس به آن پاسخ داده
    with ex as (
        select e.id, e.title, e.subject, e.total_score, e.created_at
          from public.exams e
         where e.teacher_id = v_class.teacher_id
           and (exists (select 1 from public.exam_audience_classes ac where ac.exam_id = e.id and ac.class_id = v_class.id)
             or exists (select 1 from public.answers a join public.class_members cm on cm.student_id = a.student_id and cm.class_id = v_class.id where a.exam_id = e.id))
    )
    select coalesce(jsonb_agg(jsonb_build_object('id', ex.id, 'title', ex.title, 'subject', ex.subject, 'total_score', ex.total_score, 'created_at', ex.created_at) order by ex.created_at), '[]'::jsonb)
      into v_exams from ex;

    select coalesce(jsonb_agg(jsonb_build_object(
            'id', p.id, 'full_name', p.full_name, 'username', p.username,
            'grades', coalesce((
                select jsonb_object_agg(a.exam_id, jsonb_build_object(
                    'grade', a.total_grade, 'graded', coalesce(a.graded, false),
                    'max', coalesce(nullif((a.meta ->> 'max_score'), '')::numeric, e.total_score),
                    'submitted_at', a.submitted_at))
                  from public.answers a join public.exams e on e.id = a.exam_id
                 where a.student_id = p.id and e.teacher_id = v_class.teacher_id
                   and a.exam_id in (select (x ->> 'id') from jsonb_array_elements(v_exams) x)
            ), '{}'::jsonb)) order by p.full_name), '[]'::jsonb)
      into v_students
      from public.class_members cm join public.profiles p on p.id = cm.student_id
     where cm.class_id = v_class.id;

    return jsonb_build_object('ok', true, 'class_id', v_class.id, 'class_name', v_class.name, 'teacher_id', v_class.teacher_id, 'exams', v_exams, 'students', v_students);
end;
$$;
revoke all on function public.native_manager_class_report_v1(uuid) from public, anon;
grant execute on function public.native_manager_class_report_v1(uuid) to authenticated;

commit;

notify pgrst, 'reload schema';
