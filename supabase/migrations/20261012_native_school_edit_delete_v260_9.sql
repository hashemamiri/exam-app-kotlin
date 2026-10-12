-- V260.9 — ویرایش/حذف مدرسه توسط مدیر، پرچم «کلاس مدیرساخته» برای معلم، شمارش درست «فعالیت معلم‌ها»
begin;

-- ۱) ویرایش مدرسه (نام/استان/شهر) — فقط سازنده یا مدیرِ عضو
create or replace function public.native_manager_update_school_v1(p_school uuid, p_name text, p_province text default '', p_city text default '')
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid := auth.uid(); v_name text := left(btrim(coalesce(p_name,'')),160);
begin
  if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
  if char_length(v_name) < 2 then return jsonb_build_object('error','نام مدرسه را وارد کنید'); end if;
  if not exists (select 1 from public.schools s where s.id=p_school and (s.created_by=v_uid
       or exists (select 1 from public.school_memberships m where m.school_id=s.id and m.user_id=v_uid and m.staff_role='manager' and m.status='active'))) then
    return jsonb_build_object('error','این مدرسه متعلق به شما نیست');
  end if;
  update public.schools set name=v_name, province=left(btrim(coalesce(p_province,'')),100), city=left(btrim(coalesce(p_city,'')),100) where id=p_school;
  return jsonb_build_object('ok',true);
end $$;
revoke all on function public.native_manager_update_school_v1(uuid,text,text,text) from public,anon;
grant execute on function public.native_manager_update_school_v1(uuid,text,text,text) to authenticated;

-- ۲) حذف مدرسه — فقط وقتی خالی است (بدون کلاس/آزمون/دانش‌آموز/معلم فعال)؛ آخرین مدرسهٔ مدیر حذف نمی‌شود
create or replace function public.native_manager_delete_school_v1(p_school uuid)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid := auth.uid(); v_classes int; v_exams int; v_students int; v_teachers int; v_mine int;
begin
  if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
  if not exists (select 1 from public.schools s where s.id=p_school and (s.created_by=v_uid
       or exists (select 1 from public.school_memberships m where m.school_id=s.id and m.user_id=v_uid and m.staff_role='manager' and m.status='active'))) then
    return jsonb_build_object('error','این مدرسه متعلق به شما نیست');
  end if;
  select count(*) into v_mine from public.schools s where s.created_by=v_uid
     or exists (select 1 from public.school_memberships m where m.school_id=s.id and m.user_id=v_uid and m.staff_role='manager' and m.status='active');
  if v_mine <= 1 then return jsonb_build_object('error','آخرین مدرسهٔ شما حذف نمی‌شود؛ می‌توانید نام آن را ویرایش کنید'); end if;
  select count(*) into v_classes from public.classes where school_id=p_school;
  select count(*) into v_exams from public.exams where school_id=p_school;
  select count(*) into v_students from public.school_students where school_id=p_school;
  select count(*) into v_teachers from public.school_memberships where school_id=p_school and staff_role='teacher' and status='active';
  if v_classes+v_exams+v_students+v_teachers > 0 then
    return jsonb_build_object('error', format('این مدرسه خالی نیست (%s کلاس، %s آزمون، %s دانش‌آموز، %s معلم)؛ اول آن‌ها را منتقل یا حذف کنید', v_classes, v_exams, v_students, v_teachers));
  end if;
  delete from public.schools where id=p_school;
  return jsonb_build_object('ok',true);
end $$;
revoke all on function public.native_manager_delete_school_v1(uuid) from public,anon;
grant execute on function public.native_manager_delete_school_v1(uuid) to authenticated;

-- ۳) پرچم کلاس‌های من: کدام کلاس را مدیر ساخته (اشتراک با مدیر برای آن بی‌معناست) + نام مدرسه
create or replace function public.native_my_classes_flags_v1()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select jsonb_build_object('ok',true,'items',coalesce((select jsonb_agg(jsonb_build_object(
  'id',c.id,
  'manager_created',(c.created_by is not null and c.created_by<>c.teacher_id),
  'school_name',coalesce((select s.name from public.schools s where s.id=c.school_id),'')))
  from public.classes c where c.teacher_id=auth.uid()),'[]'::jsonb));
$$;
revoke all on function public.native_my_classes_flags_v1() from public,anon;
grant execute on function public.native_my_classes_flags_v1() to authenticated;

-- ۴) خلاصهٔ مدرسه: «فعالیت معلم‌ها» — دانش‌آموز = افراد یکتای کلاس‌های معلم در مدرسه (+ ساخته‌های خودش)، کلاس = کلاس‌های معلم در مدرسه (ساختهٔ مدیر هم)، آزمون = آزمون‌های معلم در مدرسه
create or replace function public.native_manager_school_summary_v36()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
with mine as(select school_id from public.school_memberships where user_id=auth.uid() and staff_role='manager' and status='active'
             union select id from public.schools where created_by=auth.uid()),
teachers as(select distinct sm.user_id from public.school_memberships sm where sm.school_id in(select school_id from mine) and sm.staff_role='teacher' and sm.status='active'),
examset as(select e.id,e.teacher_id,e.total_score from public.exams e where e.school_id in(select school_id from mine)),
answer_stats as(select count(*) answers,coalesce(avg(case when coalesce(e.total_score,0)>0 then coalesce(a.total_grade,0)*100.0/e.total_score end),0) average_percent from public.answers a join examset e on e.id=a.exam_id)
select case when not exists(select 1 from mine) then jsonb_build_object('error','مدرسه فعال پیدا نشد') else jsonb_build_object(
 'ok',true,
 'school_id',(select school_id from mine limit 1),
 'school_name',(select string_agg(s.name,'، ' order by s.name) from public.schools s where s.id in(select school_id from mine)),
 'school_count',(select count(*) from mine),
 'province',(select min(coalesce(s.province,'')) from public.schools s where s.id in(select school_id from mine)),
 'city',(select min(coalesce(s.city,'')) from public.schools s where s.id in(select school_id from mine)),
 'teachers',(select count(*) from teachers),
 'students',(select count(distinct x) from (select ss.student_id x from public.school_students ss where ss.school_id in(select school_id from mine)
             union select cm.student_id from public.class_members cm join public.classes c on c.id=cm.class_id where c.school_id in(select school_id from mine)) u),
 'classes',(select count(*) from public.classes c where c.school_id in(select school_id from mine)),
 'exams',(select count(*) from examset),
 'answers',(select answers from answer_stats),
 'average_percent',(select round(average_percent::numeric,1) from answer_stats),
 'distributed_toman',(select coalesce(sum(amount_toman),0) from public.manager_wallet_transfers_v38 x where x.school_id in(select school_id from mine)),
 'teacher_activity',(select coalesce(jsonb_agg(jsonb_build_object(
   'teacher_id',p.id,'name',p.full_name,
   'exams',(select count(*) from examset e where e.teacher_id=p.id),
   'classes',(select count(*) from public.classes c where c.teacher_id=p.id and c.school_id in(select school_id from mine)),
   'students',(select count(distinct x) from (select ss.student_id x from public.school_students ss where ss.created_by=p.id and ss.school_id in(select school_id from mine)
               union select cm.student_id from public.class_members cm join public.classes c on c.id=cm.class_id where c.teacher_id=p.id and c.school_id in(select school_id from mine)) u),
   'wallet_balance',coalesce((select balance from public.wallets w where w.user_id=p.id),0)
 ) order by p.full_name),'[]'::jsonb) from public.profiles p where p.id in(select user_id from teachers))
) end;
$$;
revoke all on function public.native_manager_school_summary_v36() from public,anon;
grant execute on function public.native_manager_school_summary_v36() to authenticated;

commit;
