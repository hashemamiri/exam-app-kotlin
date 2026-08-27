-- V62.7: رفع خطای «more than one row returned by a subquery» در داشبورد/وضعیت/کارنامهٔ مدیر.
-- ریشه: native_manager_school_summary_v36 (نسخهٔ V38) با CTEی mine تک‌مدرسه‌ای نوشته شده بود؛
-- از V61.1 مدیر چندمدرسه‌ای شد و زیرپرس‌وجوی «from mine join schools» چند سطر برمی‌گرداند.
-- این نسخه آمار را روی «همهٔ» مدارس مدیر جمع می‌زند.
begin;

create or replace function public.native_manager_school_summary_v36()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
with mine as(select school_id from public.school_memberships where user_id=auth.uid() and staff_role='manager' and status='active'),
teachers as(select distinct sm.user_id from public.school_memberships sm where sm.school_id in(select school_id from mine) and sm.staff_role='teacher' and sm.status='active'),
examset as(select e.id,e.teacher_id,e.total_score from public.exams e where e.school_id in(select school_id from mine)),
answer_stats as(select count(*) answers,coalesce(avg(case when coalesce(e.total_score,0)>0 then coalesce(a.total_grade,0)*100.0/e.total_score end),0) average_percent from public.answers a join examset e on e.id=a.exam_id)
select case when not exists(select 1 from mine) then jsonb_build_object('error','مدرسه فعال پیدا نشد') else jsonb_build_object(
 'ok',true,
 'school_id',(select school_id from mine limit 1),
 'school_name',(select string_agg(s.name,'، ' order by s.name) from public.schools s where s.id in(select school_id from mine)),
 'province',(select min(coalesce(s.province,'')) from public.schools s where s.id in(select school_id from mine)),
 'city',(select min(coalesce(s.city,'')) from public.schools s where s.id in(select school_id from mine)),
 'teachers',(select count(*) from teachers),
 'students',(select count(distinct ss.student_id) from public.school_students ss where ss.school_id in(select school_id from mine)),
 'classes',(select count(*) from public.classes c where c.school_id in(select school_id from mine)),
 'exams',(select count(*) from examset),
 'answers',(select answers from answer_stats),
 'average_percent',(select round(average_percent::numeric,1) from answer_stats),
 'distributed_toman',(select coalesce(sum(amount_toman),0) from public.manager_wallet_transfers_v38 x where x.school_id in(select school_id from mine)),
 'teacher_activity',(select coalesce(jsonb_agg(jsonb_build_object(
   'teacher_id',p.id,'name',p.full_name,
   'exams',(select count(*) from examset e where e.teacher_id=p.id),
   'classes',(select count(*) from public.classes c where c.teacher_id=p.id and c.school_id in(select school_id from mine)),
   'students',(select count(distinct ss.student_id) from public.school_students ss where ss.created_by=p.id and ss.school_id in(select school_id from mine)),
   'wallet_balance',coalesce((select balance from public.wallets w where w.user_id=p.id),0)
 ) order by p.full_name),'[]'::jsonb) from public.profiles p where p.id in(select user_id from teachers))
) end;
$$;

revoke all on function public.native_manager_school_summary_v36() from public,anon;
grant execute on function public.native_manager_school_summary_v36() to authenticated;

commit;
