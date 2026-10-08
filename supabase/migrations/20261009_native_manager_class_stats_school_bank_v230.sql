-- V230 — بستهٔ ۳: (الف) داشبورد مقایسه‌ای کلاس‌ها برای مدیر؛ (ب) بانک سؤال مشترک مدرسه.
-- اجرا: SQL Editor سوپابیس، یک بار. همهٔ توابع security definer و فقط برای authenticated.
begin;

-- (الف) آمار کلاس‌ها و روند ماهانهٔ مدارس مدیر
create or replace function public.native_manager_class_stats_v1()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
with mine as(select school_id from public.school_memberships where user_id=auth.uid() and staff_role='manager' and status='active'),
examset as(select e.id,e.teacher_id,e.total_score,e.title from public.exams e where e.school_id in(select school_id from mine)),
ans as(select a.exam_id,a.student_id,a.total_grade,a.graded,a.submitted_at,
        case when coalesce(e.total_score,0)>0 then coalesce(a.total_grade,0)*100.0/e.total_score end pct
       from public.answers a join examset e on e.id=a.exam_id),
cls as(select c.id,c.name,c.grade,c.field_of_study,c.teacher_id,c.school_id from public.classes c where c.school_id in(select school_id from mine)),
cstat as(
  select cl.id class_id,
    (select count(*) from public.class_members cm where cm.class_id=cl.id) students,
    (select count(distinct x.exam_id) from ans x join public.class_members cm on cm.student_id=x.student_id and cm.class_id=cl.id) exams,
    (select count(*) from ans x join public.class_members cm on cm.student_id=x.student_id and cm.class_id=cl.id) answers,
    (select count(*) from ans x join public.class_members cm on cm.student_id=x.student_id and cm.class_id=cl.id where x.graded) graded,
    (select round(avg(x.pct)::numeric,1) from ans x join public.class_members cm on cm.student_id=x.student_id and cm.class_id=cl.id where x.graded) average_percent,
    (select round(stddev_pop(x.pct)::numeric,1) from ans x join public.class_members cm on cm.student_id=x.student_id and cm.class_id=cl.id where x.graded) spread_percent,
    (select count(*) from ans x join public.class_members cm on cm.student_id=x.student_id and cm.class_id=cl.id where x.graded and x.pct<50) below_half
  from cls cl)
select case when not exists(select 1 from mine) then jsonb_build_object('error','مدرسه فعال پیدا نشد') else jsonb_build_object(
 'ok',true,
 'classes',coalesce((select jsonb_agg(jsonb_build_object(
    'id',cl.id,'name',cl.name,'grade',cl.grade,'field',cl.field_of_study,
    'school',(select s.name from public.schools s where s.id=cl.school_id),
    'teacher',(select p.full_name from public.profiles p where p.id=cl.teacher_id),
    'students',st.students,'exams',st.exams,'answers',st.answers,'graded',st.graded,
    'average_percent',st.average_percent,'spread_percent',st.spread_percent,'below_half',st.below_half
  ) order by st.average_percent desc nulls last, cl.name) from cls cl join cstat st on st.class_id=cl.id),'[]'::jsonb),
 'months',coalesce((select jsonb_agg(jsonb_build_object('month',m.ym,'answers',m.n,'average_percent',m.avg_pct) order by m.ym)
   from (select to_char(date_trunc('month',coalesce(x.submitted_at,now())),'YYYY-MM') ym,count(*) n,round(avg(x.pct)::numeric,1) avg_pct
         from ans x where x.graded and coalesce(x.submitted_at,now())>=date_trunc('month',now())-interval '5 months' group by 1) m),'[]'::jsonb),
 'top_exams',coalesce((select jsonb_agg(jsonb_build_object('title',e.title,'teacher',(select p.full_name from public.profiles p where p.id=e.teacher_id),
      'answers',(select count(*) from ans x where x.exam_id=e.id),
      'average_percent',(select round(avg(x.pct)::numeric,1) from ans x where x.exam_id=e.id and x.graded)) order by (select count(*) from ans x where x.exam_id=e.id) desc)
   from (select * from examset order by (select count(*) from ans x where x.exam_id=examset.id) desc limit 8) e),'[]'::jsonb)
) end;
$$;
revoke all on function public.native_manager_class_stats_v1() from public,anon;
grant execute on function public.native_manager_class_stats_v1() to authenticated;

-- (ب) بانک مشترک مدرسه: پرچم اشتراک روی هر سؤال بانک؛ همکارانِ همان مدرسه فقط می‌خوانند و کپی می‌کنند
alter table public.question_bank add column if not exists shared_school boolean not null default false;
create index if not exists question_bank_shared_idx on public.question_bank(teacher_id) where shared_school;

create or replace function public.native_bank_set_shared_v1(p_id bigint, p_shared boolean)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
begin
 if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 update public.question_bank set shared_school=coalesce(p_shared,false) where id=p_id and teacher_id=auth.uid();
 if not found then return jsonb_build_object('error','سؤال پیدا نشد'); end if;
 return jsonb_build_object('ok',true,'shared',coalesce(p_shared,false));
end $$;

create or replace function public.native_school_bank_v1()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
with mine as(select school_id from public.school_memberships where user_id=auth.uid() and status='active'),
mates as(select distinct sm.user_id from public.school_memberships sm where sm.school_id in(select school_id from mine) and sm.status='active' and sm.user_id<>auth.uid())
select jsonb_build_object('ok',true,'items',coalesce((
  select jsonb_agg(jsonb_build_object('id',q.id,'subject',q.subject,'question',q.question,'created_at',q.created_at,
    'teacher',(select coalesce(nullif(p.display_name,''),p.full_name) from public.profiles p where p.id=q.teacher_id),
    'already',exists(select 1 from public.question_bank m where m.teacher_id=auth.uid() and m.question=q.question)) order by q.created_at desc,q.id desc)
  from public.question_bank q where q.shared_school and q.teacher_id in(select user_id from mates)),'[]'::jsonb));
$$;

create or replace function public.native_bank_copy_from_school_v1(p_id bigint)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_src public.question_bank%rowtype; v_id bigint;
begin
 if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 select * into v_src from public.question_bank q where q.id=p_id and q.shared_school
   and exists(select 1 from public.school_memberships a join public.school_memberships b on a.school_id=b.school_id
              where a.user_id=auth.uid() and a.status='active' and b.user_id=q.teacher_id and b.status='active');
 if not found then return jsonb_build_object('error','این سؤال در بانک مدرسهٔ شما نیست'); end if;
 if exists(select 1 from public.question_bank m where m.teacher_id=auth.uid() and m.question=v_src.question) then
   return jsonb_build_object('error','این سؤال از قبل در بانک شما هست','duplicate',true);
 end if;
 insert into public.question_bank(teacher_id,subject,question) values(auth.uid(),v_src.subject,v_src.question) returning id into v_id;
 return jsonb_build_object('ok',true,'id',v_id);
end $$;

-- اسنپ‌شات بانک معلم: فیلد جدید shared (برای کلید اشتراک در سایت/اپ)؛ بقیه عیناً نسخهٔ قبلی
create or replace function public.native_bank_snapshot_v1()
returns jsonb language sql stable security definer set search_path = public, pg_temp as $$
select jsonb_build_object(
    'ok',true,
    'categories',coalesce((
        select jsonb_agg(jsonb_build_object(
            'id',c.id,'name',c.name,
            'count',(select count(*) from public.bank_question_cats l where l.category_id=c.id)
        ) order by lower(c.name),c.id)
        from public.bank_categories c where c.teacher_id=auth.uid()
    ),'[]'::jsonb),
    'items',coalesce((
        select jsonb_agg(jsonb_build_object(
            'id',q.id,'subject',q.subject,'question',q.question,'created_at',q.created_at,'shared',q.shared_school,
            'cat_ids',coalesce((select jsonb_agg(l.category_id order by l.category_id)
                from public.bank_question_cats l where l.question_id=q.id),'[]'::jsonb),
            'cat_names',coalesce((select jsonb_agg(c.name order by lower(c.name))
                from public.bank_question_cats l join public.bank_categories c on c.id=l.category_id
                where l.question_id=q.id),'[]'::jsonb)
        ) order by q.created_at desc,q.id desc)
        from public.question_bank q where q.teacher_id=auth.uid()
    ),'[]'::jsonb)
);
$$;

revoke all on function public.native_bank_set_shared_v1(bigint,boolean) from public,anon;
revoke all on function public.native_school_bank_v1() from public,anon;
revoke all on function public.native_bank_copy_from_school_v1(bigint) from public,anon;
grant execute on function public.native_bank_set_shared_v1(bigint,boolean) to authenticated;
grant execute on function public.native_school_bank_v1() to authenticated;
grant execute on function public.native_bank_copy_from_school_v1(bigint) to authenticated;

commit;
