-- =====================================================================
-- V354 — اعتراض به نمره، تاریخچهٔ تغییر نمره، و پشتیبانِ تصحیح ناشناس
-- بستهٔ ۱ از ۳ (تصحیح و نمره‌گذاری)
--
-- اجرا: در Supabase → SQL Editor، کل این فایل را یک‌جا اجرا کنید.
-- ایمنی: هیچ نمره یا پاسخ تاریخی بازنویسی نمی‌شود. فقط دو جدول تازه،
--        یک تریگرِ فقط-افزودنی، و چند تابع خواندنی/ثبتی ساخته می‌شود.
--        اجرای دوبارهٔ فایل بی‌خطر است (idempotent).
--
-- اصلاح مهم (نسخهٔ دوم این فایل):
--   در پایگاه‌دادهٔ واقعی، answers.id و answers.exam_id و exams.id از نوع
--   text هستند، ولی exams.teacher_id و answers.student_id از نوع uuid.
--   نسخهٔ نخست همه را uuid فرض کرده بود و در ساخت سیاست RLS خطای
--   «operator does not exist: text = uuid» می‌داد.
--   اکنون همهٔ شناسه‌ها در جدول‌های تازه text نگه داشته می‌شوند و هر
--   مقایسه با ::text در دو طرف انجام می‌شود؛ پس فایل به نوع ستون‌های
--   شما وابسته نیست و روی هر دو حالت کار می‌کند.
--
-- نکتهٔ طراحی: تریگر به‌جای ارجاع مستقیم به ستون، از to_jsonb(OLD)/
--   to_jsonb(NEW) می‌خواند؛ اگر ستونی نبود، null برمی‌گردد و خطا نمی‌دهد.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ۰) اگر اجرای ناقص قبلی جدول را با نوع uuid ساخته، به text تبدیل کن
--    (تبدیل uuid به text بی‌زیان است و داده‌ای از دست نمی‌رود)
-- ---------------------------------------------------------------------
do $$
declare
  t text; c text; ty text;
begin
  foreach t in array array['grade_history', 'grade_appeals'] loop
    if to_regclass('public.' || t) is null then continue; end if;
    foreach c in array array['answer_id', 'exam_id', 'student_id', 'changed_by', 'second_by', 'resolved_by'] loop
      select data_type into ty
        from information_schema.columns
       where table_schema = 'public' and table_name = t and column_name = c;
      if ty is not null and ty <> 'text' then
        -- کلید خارجی وابسته را موقتاً برمی‌داریم تا تبدیل نوع ممکن شود
        execute format('alter table public.%I drop constraint if exists grade_appeals_answer_fk', t);
        execute format('alter table public.%I alter column %I type text using %I::text', t, c, c);
      end if;
    end loop;
  end loop;
end $$;

-- ---------------------------------------------------------------------
-- ۱) تاریخچهٔ تغییر نمره — فقط افزودنی، هرگز پاک نمی‌شود
-- ---------------------------------------------------------------------
create table if not exists public.grade_history (
  id          bigserial primary key,
  answer_id   text not null,
  exam_id     text,
  student_id  text,
  changed_by  text,
  before      jsonb,
  after       jsonb,
  created_at  timestamptz not null default now()
);

create index if not exists grade_history_answer_idx on public.grade_history(answer_id, created_at desc);
create index if not exists grade_history_exam_idx   on public.grade_history(exam_id, created_at desc);

alter table public.grade_history enable row level security;

drop policy if exists "v354 teacher reads own exam history" on public.grade_history;
create policy "v354 teacher reads own exam history" on public.grade_history
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = grade_history.exam_id
               and e.teacher_id::text = auth.uid()::text)
  );

drop policy if exists "v354 student reads own history" on public.grade_history;
create policy "v354 student reads own history" on public.grade_history
  for select using (student_id = auth.uid()::text);

-- هیچ سیاست INSERT/UPDATE/DELETE برای کاربر عادی تعریف نمی‌شود:
-- تنها تریگرِ security definer می‌نویسد، پس سابقه دست‌کاری‌شدنی نیست.
-- علاوه بر RLS، دسترسی نوشتن صریحاً گرفته می‌شود تا حتی اگر روزی
-- سیاستی اشتباهاً اضافه شد، کاربر نتواند سابقه را پاک یا ویرایش کند.
do $$
declare r text;
begin
  foreach r in array array['public', 'anon', 'authenticated'] loop
    if r = 'public' or exists (select 1 from pg_roles where rolname = r) then
      execute format('revoke insert, update, delete, truncate on public.grade_history from %s',
                     case when r = 'public' then 'PUBLIC' else quote_ident(r) end);
    end if;
  end loop;
end $$;

create or replace function public.native_grade_history_trg_v354()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  b jsonb;
  a jsonb;
begin
  b := jsonb_build_object(
        'score',     to_jsonb(OLD) -> 'score',
        'graded',    to_jsonb(OLD) -> 'graded',
        'responses', to_jsonb(OLD) -> 'responses',
        'meta',      to_jsonb(OLD) -> 'meta');
  a := jsonb_build_object(
        'score',     to_jsonb(NEW) -> 'score',
        'graded',    to_jsonb(NEW) -> 'graded',
        'responses', to_jsonb(NEW) -> 'responses',
        'meta',      to_jsonb(NEW) -> 'meta');

  if b is distinct from a then
    insert into public.grade_history(answer_id, exam_id, student_id, changed_by, before, after)
    values (
      to_jsonb(NEW) ->> 'id',
      nullif(to_jsonb(NEW) ->> 'exam_id', ''),
      nullif(to_jsonb(NEW) ->> 'student_id', ''),
      auth.uid()::text,
      b, a);
  end if;
  return NEW;
end $$;

drop trigger if exists grade_history_audit_v354 on public.answers;
create trigger grade_history_audit_v354
  after update on public.answers
  for each row execute function public.native_grade_history_trg_v354();

-- ---------------------------------------------------------------------
-- ۲) اعتراض به نمره
--    status: open (در انتظار) | second (ارجاع به مصحح دوم)
--            | accepted (پذیرفته) | rejected (رد شده)
-- ---------------------------------------------------------------------
create table if not exists public.grade_appeals (
  id              uuid primary key default gen_random_uuid(),
  answer_id       text not null,
  exam_id         text not null,
  student_id      text not null,
  question_index  integer not null default -1,   -- ‎-۱ = کل برگه
  reason          text not null,
  status          text not null default 'open',
  second_score    numeric,
  second_by       text,
  second_note     text,
  resolution_note text,
  resolved_by     text,
  created_at      timestamptz not null default now(),
  resolved_at     timestamptz,
  constraint grade_appeals_status_ck
    check (status in ('open', 'second', 'accepted', 'rejected')),
  constraint grade_appeals_reason_len
    check (char_length(reason) between 5 and 2000)
);

-- کلید خارجی تنها اگر نوع answers.id واقعاً text باشد اضافه می‌شود؛
-- در غیر این صورت از آن می‌گذریم تا مهاجرت نشکند.
do $$
begin
  if exists (select 1 from information_schema.columns
              where table_schema = 'public' and table_name = 'answers'
                and column_name = 'id' and data_type = 'text') then
    if not exists (select 1 from pg_constraint where conname = 'grade_appeals_answer_fk') then
      execute 'alter table public.grade_appeals
                 add constraint grade_appeals_answer_fk
                 foreign key (answer_id) references public.answers(id) on delete cascade';
    end if;
  end if;
end $$;

create index if not exists grade_appeals_exam_idx    on public.grade_appeals(exam_id, created_at desc);
create index if not exists grade_appeals_student_idx on public.grade_appeals(student_id, created_at desc);

-- یک اعتراض باز برای هر سؤال از هر دانش‌آموز
create unique index if not exists grade_appeals_one_open_idx
  on public.grade_appeals(answer_id, question_index)
  where status in ('open', 'second');

alter table public.grade_appeals enable row level security;

drop policy if exists "v354 student reads own appeals" on public.grade_appeals;
create policy "v354 student reads own appeals" on public.grade_appeals
  for select using (student_id = auth.uid()::text);

drop policy if exists "v354 teacher reads exam appeals" on public.grade_appeals;
create policy "v354 teacher reads exam appeals" on public.grade_appeals
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = grade_appeals.exam_id
               and e.teacher_id::text = auth.uid()::text)
  );

-- نوشتن فقط از راه تابع‌های زیر (security definer) انجام می‌شود.

-- ---------------------------------------------------------------------
-- ۳) دانش‌آموز اعتراض ثبت می‌کند
-- ---------------------------------------------------------------------
drop function if exists public.native_appeal_create_v354(uuid, integer, text);
create or replace function public.native_appeal_create_v354(
  p_answer text, p_question integer, p_reason text
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid text := auth.uid()::text;
  v_ans public.answers%rowtype;
  v_id  uuid;
  v_txt text := btrim(coalesce(p_reason, ''));
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if char_length(v_txt) < 5 then raise exception 'دلیل اعتراض را کامل‌تر بنویسید (دست‌کم ۵ نویسه).'; end if;
  if char_length(v_txt) > 2000 then raise exception 'متن اعتراض بیش از حد بلند است.'; end if;

  select * into v_ans from public.answers where id::text = p_answer;
  if not found then raise exception 'پاسخ پیدا نشد.'; end if;
  if v_ans.student_id::text <> v_uid then raise exception 'این پاسخ متعلق به شما نیست.'; end if;

  -- تنها پس از نهایی‌شدن تصحیح می‌توان اعتراض کرد
  if coalesce((to_jsonb(v_ans) ->> 'graded')::boolean, false) is not true then
    raise exception 'تصحیح این برگه هنوز نهایی نشده است.';
  end if;

  if exists (select 1 from public.grade_appeals
             where answer_id = p_answer and question_index = coalesce(p_question, -1)
               and status in ('open', 'second')) then
    raise exception 'برای این مورد یک اعتراض باز دارید.';
  end if;

  insert into public.grade_appeals(answer_id, exam_id, student_id, question_index, reason)
  values (p_answer, v_ans.exam_id::text, v_uid, coalesce(p_question, -1), v_txt)
  returning id into v_id;

  return jsonb_build_object('ok', true, 'id', v_id);
end $$;

-- ---------------------------------------------------------------------
-- ۴) دانش‌آموز اعتراض‌های خودش را می‌بیند
-- ---------------------------------------------------------------------
create or replace function public.native_my_appeals_v354()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select coalesce(jsonb_agg(jsonb_build_object(
           'id', a.id, 'exam_id', a.exam_id, 'exam_title', e.title,
           'answer_id', a.answer_id, 'question_index', a.question_index,
           'reason', a.reason, 'status', a.status,
           'second_score', a.second_score, 'second_note', a.second_note,
           'resolution_note', a.resolution_note,
           'created_at', a.created_at, 'resolved_at', a.resolved_at
         ) order by a.created_at desc), '[]'::jsonb)
  from public.grade_appeals a
  left join public.exams e on e.id::text = a.exam_id
  where a.student_id = auth.uid()::text;
$$;

-- ---------------------------------------------------------------------
-- ۵) معلم اعتراض‌های آزمون خودش را می‌بیند
-- ---------------------------------------------------------------------
drop function if exists public.native_exam_appeals_v354(uuid);
create or replace function public.native_exam_appeals_v354(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare v_uid text := auth.uid()::text;
begin
  if not exists (select 1 from public.exams where id::text = p_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;

  return (
    select coalesce(jsonb_agg(jsonb_build_object(
             'id', a.id, 'answer_id', a.answer_id, 'student_id', a.student_id,
             'student_name', coalesce(p.full_name, p.username, 'دانش‌آموز'),
             'question_index', a.question_index, 'reason', a.reason,
             'status', a.status, 'second_score', a.second_score,
             'second_note', a.second_note, 'resolution_note', a.resolution_note,
             'created_at', a.created_at, 'resolved_at', a.resolved_at
           ) order by a.created_at desc), '[]'::jsonb)
    from public.grade_appeals a
    left join public.profiles p on p.id::text = a.student_id
    where a.exam_id = p_exam);
end $$;

-- ---------------------------------------------------------------------
-- ۶) معلم: ارجاع به مصحح دوم، یا پذیرش/رد اعتراض
--    پذیرش، نمره را عوض نمی‌کند؛ فقط وضعیت را ثبت می‌کند تا معلم
--    آگاهانه در تب تصحیح نمره را اصلاح کند (آن اصلاح در تاریخچه می‌آید).
-- ---------------------------------------------------------------------
create or replace function public.native_appeal_resolve_v354(
  p_appeal uuid, p_status text, p_note text, p_second_score numeric default null
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid text := auth.uid()::text;
  v_ap  public.grade_appeals%rowtype;
  v_st  text := coalesce(p_status, '');
begin
  if v_st not in ('second', 'accepted', 'rejected') then
    raise exception 'وضعیت نامعتبر است.';
  end if;

  select * into v_ap from public.grade_appeals where id = p_appeal;
  if not found then raise exception 'اعتراض پیدا نشد.'; end if;
  if not exists (select 1 from public.exams where id::text = v_ap.exam_id and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;
  if v_ap.status in ('accepted', 'rejected') then
    raise exception 'این اعتراض پیش‌تر تعیین‌تکلیف شده است.';
  end if;

  update public.grade_appeals
     set status          = v_st,
         second_score    = case when v_st = 'second' then p_second_score else second_score end,
         second_by       = case when v_st = 'second' then v_uid else second_by end,
         second_note     = case when v_st = 'second' then nullif(btrim(coalesce(p_note, '')), '') else second_note end,
         resolution_note = case when v_st in ('accepted', 'rejected') then nullif(btrim(coalesce(p_note, '')), '') else resolution_note end,
         resolved_by     = case when v_st in ('accepted', 'rejected') then v_uid else resolved_by end,
         resolved_at     = case when v_st in ('accepted', 'rejected') then now() else resolved_at end
   where id = p_appeal;

  return jsonb_build_object('ok', true, 'status', v_st);
end $$;

-- ---------------------------------------------------------------------
-- ۷) تاریخچهٔ نمره برای معلم (با نام دانش‌آموز)
-- ---------------------------------------------------------------------
drop function if exists public.native_grade_history_v354(uuid);
create or replace function public.native_grade_history_v354(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare v_uid text := auth.uid()::text;
begin
  if not exists (select 1 from public.exams where id::text = p_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;

  return (
    select coalesce(jsonb_agg(jsonb_build_object(
             'id', h.id, 'answer_id', h.answer_id,
             'student_name', coalesce(p.full_name, p.username, 'دانش‌آموز'),
             'changed_by_name', coalesce(q.full_name, q.username, 'سامانه'),
             'before_score', (h.before ->> 'score'),
             'after_score',  (h.after  ->> 'score'),
             'before_graded', (h.before ->> 'graded'),
             'after_graded',  (h.after  ->> 'graded'),
             'created_at', h.created_at
           ) order by h.created_at desc), '[]'::jsonb)
    from public.grade_history h
    left join public.profiles p on p.id::text = h.student_id
    left join public.profiles q on q.id::text = h.changed_by
    where h.exam_id = p_exam);
end $$;

-- ---------------------------------------------------------------------
-- ۸) دسترسی اجرا
-- ---------------------------------------------------------------------
grant execute on function public.native_appeal_create_v354(text, integer, text) to authenticated;
grant execute on function public.native_my_appeals_v354()                      to authenticated;
grant execute on function public.native_exam_appeals_v354(text)                to authenticated;
grant execute on function public.native_appeal_resolve_v354(uuid, text, text, numeric) to authenticated;
grant execute on function public.native_grade_history_v354(text)               to authenticated;

-- =====================================================================
-- پایان. برای بررسی درستی، فایل SQL_GRADING_APPEALS_V354_CHECK.sql را اجرا کنید.
-- =====================================================================
