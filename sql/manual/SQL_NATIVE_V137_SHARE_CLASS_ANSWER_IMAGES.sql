-- V137 — دو رفع سروری:
-- ۱) چشم اشتراک کلاس: native_teacher_share_class_v62 فقط UPDATE می‌زد و اگر معلم عضو
--    هیچ مدرسه‌ای نبود (یا کلاس school_id نداشت) باز هم ok برمی‌گشت و پیام «برای مدیر
--    قابل مشاهده شد» دروغ بود (مدیر فقط کلاس‌های c.school_id=مدرسهٔ خودش را می‌بیند).
--    حالا: بدون عضویت فعال → خطای روشن؛ school_id خالی کلاس با مدرسهٔ معلم پر می‌شود؛
--    مقدار مؤثر «shared» برمی‌گردد تا پیام کلاینت واقعی باشد.
-- ۲) تصویر تختهٔ وایت‌برد برای معلم دیده نمی‌شد: تابع قدیمی submit_answer تصاویرِ سؤالِ
--    بدون سهمیهٔ تصویر (allowImages=no) را نگه نمی‌دارد. native_submit_queued_answer_v1
--    پس از ثبت موفق، p_images را روی آخرین ردیف answers همین دانش‌آموز/آزمون ادغام می‌کند
--    (فقط کلیدهایی که آرایهٔ ناتهی دارند) تا مسیر تصحیح (answers.response_images) کامل باشد.
begin;
set local lock_timeout = '8s';

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
  select coalesce(c.shared_with_manager,false) and c.school_id is not null into v_shared
    from public.classes c where c.id=p_class;
  return jsonb_build_object('ok',true,'shared',coalesce(v_shared,false));
end $$;

revoke all on function public.native_teacher_share_class_v137(uuid,boolean) from public,anon;
grant execute on function public.native_teacher_share_class_v137(uuid,boolean) to authenticated;

create or replace function public.native_submit_queued_answer_v1(
    p_operation uuid,
    p_exam text,
    p_responses jsonb,
    p_images jsonb,
    p_meta jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_result jsonb;
    v_images jsonb;
    v_answer public.answers.id%type;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    if coalesce(btrim(p_exam), '') = '' then return jsonb_build_object('error', 'شناسه آزمون لازم است'); end if;
    if jsonb_typeof(p_responses) <> 'array' then return jsonb_build_object('error', 'پاسخ‌ها نامعتبرند'); end if;
    if p_images is not null and jsonb_typeof(p_images) <> 'object' then
        return jsonb_build_object('error', 'تصاویر پاسخ نامعتبرند');
    end if;

    perform pg_advisory_xact_lock(hashtextextended(p_operation::text, 104729));
    select result into v_result
    from public.native_submission_operations
    where operation_id = p_operation and user_id = v_uid;
    if found then return v_result || jsonb_build_object('idempotent', true); end if;
    if exists (
        select 1 from public.native_submission_operations
        where operation_id = p_operation and user_id <> v_uid
    ) then return jsonb_build_object('error', 'شناسه عملیات متعلق به حساب دیگری است'); end if;

    if to_regprocedure('public.submit_answer(text,jsonb,jsonb,jsonb)') is not null then
        execute 'select public.submit_answer($1,$2,$3,$4)'
        into v_result
        using p_exam, p_responses, coalesce(p_images, '{}'::jsonb),
              coalesce(p_meta, '{}'::jsonb) || jsonb_build_object('operation_id', p_operation, 'native_queue', true);
    elsif to_regprocedure('public.submit_answer(text,jsonb,jsonb)') is not null then
        execute 'select public.submit_answer($1,$2,$3)'
        into v_result
        using p_exam, p_responses, coalesce(p_images, '{}'::jsonb);
    else
        return jsonb_build_object('error', 'تابع ثبت پاسخ در سرور آماده نیست');
    end if;

    v_result := coalesce(v_result, jsonb_build_object('ok', true));
    if v_result ? 'error' then return v_result; end if;

    -- V137 — تور ایمنی تصاویر پاسخ (تختهٔ وایت‌برد روی سؤال بدون سهمیهٔ تصویر):
    -- فقط کلیدهایی که آرایهٔ ناتهی از آدرس https دارند روی آخرین ردیف answers ادغام می‌شوند.
    select coalesce(jsonb_object_agg(k, v), '{}'::jsonb) into v_images
      from jsonb_each(coalesce(p_images, '{}'::jsonb)) as e(k, v)
     where jsonb_typeof(v) = 'array' and jsonb_array_length(v) > 0
       and not exists (
             select 1 from jsonb_array_elements_text(v) as u(url)
              where u.url not like 'https://%'
           );
    if v_images <> '{}'::jsonb then
        select a.id into v_answer
          from public.answers a
         where a.exam_id = p_exam and a.student_id = v_uid
         order by a.submitted_at desc nulls last
         limit 1;
        if v_answer is not null then
            update public.answers
               set response_images = coalesce(response_images, '{}'::jsonb) || v_images
             where id = v_answer and exam_id = p_exam and student_id = v_uid;
        end if;
    end if;

    insert into public.native_submission_operations(operation_id, user_id, exam_id, result)
    values (p_operation, v_uid, p_exam, v_result);
    return v_result;
end;
$$;
revoke all on function public.native_submit_queued_answer_v1(uuid,text,jsonb,jsonb,jsonb) from public, anon;
grant execute on function public.native_submit_queued_answer_v1(uuid,text,jsonb,jsonb,jsonb) to authenticated;

-- ۳) آزمون تازه‌ساخته باید بالای فهرست «آزمون‌ها» باشد: کلاینت روی created_at مرتب می‌کند
--    ولی native_save_exam_v1 این ستون را پر نمی‌کند؛ اگر پیش‌فرض نداشته باشد NULL می‌ماند و
--    آزمون جدید به تهِ فهرست می‌رود. تریگر پیش از درج، created_at خالی را now() می‌کند و
--    ردیف‌های قدیمیِ بدون تاریخ یک‌بار پر می‌شوند.
alter table public.exams add column if not exists created_at timestamptz;
alter table public.exams alter column created_at set default now();
update public.exams set created_at = now() where created_at is null;

create or replace function public.native_exams_created_at_v137()
returns trigger language plpgsql as $$
begin
  if new.created_at is null then new.created_at := now(); end if;
  return new;
end $$;
drop trigger if exists trg_native_exams_created_at_v137 on public.exams;
create trigger trg_native_exams_created_at_v137
  before insert on public.exams
  for each row execute function public.native_exams_created_at_v137();

notify pgrst,'reload schema';
commit;
