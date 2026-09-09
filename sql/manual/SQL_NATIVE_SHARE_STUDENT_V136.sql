-- V136 — رفع «چشم اشتراک دانش‌آموز عمل نمی‌کند/پیام اشتباه».
-- ریشه: native_teacher_share_student_v62 فقط UPDATE روی school_students می‌زد؛ اگر
-- دانش‌آموز هنوز ردیفی در school_students (مدرسهٔ معلم) نداشت، هیچ سطری عوض
-- نمی‌شد، RPC ok برمی‌گرداند و my_students (bool_or روی همان جدول) همچنان false
-- می‌داد → چشم بسته می‌ماند ولی پیام «قابل مشاهده شد» نمایش داده می‌شد.
-- حالا: برای هر مدرسه‌ای که معلم عضو فعال آن است ردیف ساخته/به‌روز می‌شود؛ اگر
-- معلم عضو هیچ مدرسه‌ای نباشد خطای روشن برمی‌گردد و مقدار مؤثر «shared» به کلاینت
-- داده می‌شود تا پیام درست نشان دهد.
begin;
set local lock_timeout = '8s';

create or replace function public.native_teacher_share_student_v136(p_student uuid, p_share boolean)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_schools uuid[];
begin
  if not exists(select 1 from public.profiles p where p.id=p_student and p.teacher_id=auth.uid()) then
    return jsonb_build_object('error','دانش‌آموز شما نیست');
  end if;
  select coalesce(array_agg(school_id),'{}') into v_schools
    from public.school_memberships
   where user_id=auth.uid() and status='active';
  if coalesce(array_length(v_schools,1),0)=0 then
    return jsonb_build_object('error','شما عضو هیچ مدرسه‌ای نیستید؛ ابتدا با کد دعوت به مدرسه بپیوندید.');
  end if;
  insert into public.school_students(school_id,student_id,created_by,shared_with_manager)
  select s, p_student, auth.uid(), p_share from unnest(v_schools) as s
  on conflict (school_id,student_id) do update set shared_with_manager=excluded.shared_with_manager;
  return jsonb_build_object('ok',true,'shared',
    coalesce((select bool_or(ss.shared_with_manager) from public.school_students ss where ss.student_id=p_student),false));
end $$;

revoke all on function public.native_teacher_share_student_v136(uuid,boolean) from public,anon;
grant execute on function public.native_teacher_share_student_v136(uuid,boolean) to authenticated;
notify pgrst,'reload schema';
commit;
