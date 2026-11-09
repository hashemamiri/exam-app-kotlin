-- =====================================================================
-- V357 — نصب و بررسی، در یک فایل
--
-- این تنها فایلی است که باید اجرا کنید. کل آن را در
-- Supabase -> SQL Editor بچسبانید و Run بزنید.
--
-- چرا یک فایل؟ چون اجرای فایل «بررسی» پیش از «نصب» خطای گمراه‌کنندهٔ
--   relation "public.exam_time_grants" does not exist
-- می‌داد. اینجا ترتیب اشتباه ممکن نیست.
--
-- ایمنی: هیچ پاسخ یا نمرهٔ تاریخی بازنویسی نمی‌شود. اجرای دوباره بی‌خطر
--   است. اگر پیش‌نیازی نبود، پیش از هر تغییر با پیام روشن می‌ایستد.
--
-- این فایل تولیدشده است؛ آن را با دست ویرایش نکنید.
--   منبع: SQL_EXAM_TIMING_V357.sql + SQL_EXAM_TIMING_V357_CHECK.sql
--   ساخت: python3 scripts/build_v357_sql.py
-- =====================================================================

-- ---------------------------------------------------------------------
-- نگهبان پیش‌نیاز — پیش از هر تغییر، با پیام روشن
-- ---------------------------------------------------------------------
do $$
declare
  v_missing text;
  v_cols    text;
begin
  select string_agg(t, ', ' order by t) into v_missing
    from (values ('public.exams'), ('public.exam_attempts'),
                 ('public.exam_sessions'), ('public.answers')) v(t)
   where to_regclass(t) is null;
  if v_missing is not null then
    raise exception 'جدول پایه پیدا نشد: %. آیا این فایل را در پروژهٔ درست Supabase اجرا می‌کنید؟', v_missing;
  end if;

  select string_agg(w.tbl || '.' || w.col, ', ' order by w.tbl, w.col) into v_cols
    from (values
      ('exam_attempts','id'), ('exam_attempts','exam_id'), ('exam_attempts','student_id'),
      ('exam_attempts','attempt_no'), ('exam_attempts','expires_at'),
      ('exam_sessions','exam_id'), ('exam_sessions','student_id'), ('exam_sessions','expires_at'),
      ('answers','id'), ('answers','exam_id'), ('answers','student_id'),
      ('answers','responses'), ('answers','meta'),
      ('exams','id'), ('exams','teacher_id')
    ) w(tbl, col)
   where not exists (select 1 from information_schema.columns c
                      where c.table_schema='public' and c.table_name=w.tbl and c.column_name=w.col);
  if v_cols is not null then
    raise exception 'ستون پایه پیدا نشد: %. ساختار پایگاه با آنچه انتظار می‌رفت فرق دارد؛ پیش از نصب گزارش بگیرید.', v_cols;
  end if;
end $$;

-- ---------------------------------------------------------------------
create table if not exists public.exam_time_grants (
  exam_id       text not null,
  student_id    text not null,
  extra_minutes integer not null default 0,
  reason        text not null,
  granted_by    text,
  updated_at    timestamptz not null default now(),
  primary key (exam_id, student_id),
  constraint exam_time_grants_min_ck check (extra_minutes between 0 and 240),
  constraint exam_time_grants_reason_ck check (char_length(btrim(reason)) between 5 and 500)
);

-- ---------------------------------------------------------------------
-- ۲) محدودیت زمان هر سؤال
--    question_index = -1 یعنی «همهٔ سؤال‌ها» (پیش‌فرض آزمون).
-- ---------------------------------------------------------------------
create table if not exists public.exam_question_limits (
  exam_id        text not null,
  question_index integer not null,
  seconds        integer not null,
  updated_at     timestamptz not null default now(),
  primary key (exam_id, question_index),
  constraint exam_question_limits_q_ck check (question_index >= -1),
  constraint exam_question_limits_sec_ck check (seconds between 10 and 7200)
);

-- ---------------------------------------------------------------------
-- ۳) ستون‌های وضعیت روی تلاش
--    time_grant_applied: چند دقیقه از مجوز اعمال شده — برای آنکه تریگر
--      هنگام ادامهٔ آزمون زمان را دوباره اضافه نکند.
--    question_deadlines / question_locked: همان الگوی بخش‌ها در V332.
--    offline_token: توکن یک‌بارمصرف بستهٔ آفلاین.
-- ---------------------------------------------------------------------
alter table public.exam_attempts
  add column if not exists time_grant_applied integer not null default 0,
  add column if not exists question_deadlines jsonb not null default '{}'::jsonb,
  add column if not exists question_locked jsonb not null default '{}'::jsonb,
  add column if not exists offline_token text,
  add column if not exists offline_issued_at timestamptz,
  add column if not exists offline_used_at timestamptz;

alter table public.exam_sessions
  add column if not exists time_grant_applied integer not null default 0;

-- ---------------------------------------------------------------------
-- ۴) تریگر زمان اضافه — بی‌تکرار (idempotent)
--    فقط «تفاوت» مجوز فعلی با آنچه اعمال شده را جابه‌جا می‌کند، پس
--    ادامهٔ آزمون یا تغییر مجوز زمان را دوباره اضافه نمی‌کند.
-- ---------------------------------------------------------------------
create or replace function public.native_time_grant_apply_v357()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_want  integer := 0;
  v_delta integer;
begin
  if new.expires_at is null then
    return new;
  end if;

  select g.extra_minutes into v_want
    from public.exam_time_grants g
   where g.exam_id = new.exam_id::text
     and g.student_id = new.student_id::text;
  v_want := coalesce(v_want, 0);

  v_delta := v_want - coalesce(new.time_grant_applied, 0);
  if v_delta <> 0 then
    new.expires_at := new.expires_at + make_interval(mins => v_delta);
  end if;
  new.time_grant_applied := v_want;
  return new;
end $$;

drop trigger if exists native_time_grant_attempt_v357 on public.exam_attempts;
create trigger native_time_grant_attempt_v357
  before insert or update of expires_at on public.exam_attempts
  for each row execute function public.native_time_grant_apply_v357();

drop trigger if exists native_time_grant_session_v357 on public.exam_sessions;
create trigger native_time_grant_session_v357
  before insert or update of expires_at on public.exam_sessions
  for each row execute function public.native_time_grant_apply_v357();

alter table public.exam_time_grants     enable row level security;
alter table public.exam_question_limits enable row level security;

-- معلم مالک آزمون می‌بیند؛ دانش‌آموز فقط مجوز خودش را (تا از زمان خود آگاه شود)
drop policy if exists "v357 teacher reads grants" on public.exam_time_grants;
create policy "v357 teacher reads grants" on public.exam_time_grants
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = exam_time_grants.exam_id
               and e.teacher_id::text = auth.uid()::text));

drop policy if exists "v357 student reads own grant" on public.exam_time_grants;
create policy "v357 student reads own grant" on public.exam_time_grants
  for select using (student_id = auth.uid()::text);

-- محدودیت سؤال برای دانش‌آموز هم خواندنی است (باید شمارش معکوس ببیند)
drop policy if exists "v357 limits readable" on public.exam_question_limits;
create policy "v357 limits readable" on public.exam_question_limits
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = exam_question_limits.exam_id
               and (e.teacher_id::text = auth.uid()::text
                    or exists (select 1 from public.exam_attempts a
                                where a.exam_id::text = e.id::text
                                  and a.student_id::text = auth.uid()::text))));

-- ---------------------------------------------------------------------
-- ۵) ثبت یا تغییر زمان اضافه (فقط معلم)
--    اگر آزمون در جریان است، مهلت زنده تمدید می‌شود.
--    minutes = 0 یعنی لغو مجوز.
-- ---------------------------------------------------------------------
drop function if exists public.native_time_grant_save_v357(text, text, integer, text);
create or replace function public.native_time_grant_save_v357(
  p_exam text, p_student text, p_minutes integer, p_reason text
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid    text := auth.uid()::text;
  v_reason text := btrim(coalesce(p_reason, ''));
  v_min    integer := coalesce(p_minutes, 0);
  v_sid    text := btrim(coalesce(p_student, ''));
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if not exists (select 1 from public.exams where id::text = p_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;
  if char_length(v_sid) < 1 then raise exception 'شناسهٔ دانش‌آموز نامعتبر است.'; end if;
  if v_min < 0 or v_min > 240 then raise exception 'زمان اضافه باید بین ۰ تا ۲۴۰ دقیقه باشد.'; end if;
  if char_length(v_reason) < 5 or char_length(v_reason) > 500 then
    raise exception 'دلیل زمان اضافه باید بین ۵ تا ۵۰۰ نویسه باشد.';
  end if;

  insert into public.exam_time_grants(exam_id, student_id, extra_minutes, reason, granted_by, updated_at)
  values (p_exam, v_sid, v_min, v_reason, v_uid, now())
  on conflict (exam_id, student_id) do update
    set extra_minutes = excluded.extra_minutes, reason = excluded.reason,
        granted_by = excluded.granted_by, updated_at = now();

  -- تمدید زنده؛ تریگر خودش تفاوت را اعمال می‌کند
  update public.exam_attempts set expires_at = expires_at
   where exam_id::text = p_exam and student_id::text = v_sid and expires_at is not null;
  update public.exam_sessions set expires_at = expires_at
   where exam_id::text = p_exam and student_id::text = v_sid and expires_at is not null;

  return jsonb_build_object('ok', true, 'extra_minutes', v_min);
end $$;

drop function if exists public.native_time_grants_v357(text);
create or replace function public.native_time_grants_v357(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare v_uid text := auth.uid()::text;
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if not exists (select 1 from public.exams where id::text = p_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;
  return (
    select coalesce(jsonb_agg(jsonb_build_object(
             'student_id', g.student_id, 'extra_minutes', g.extra_minutes,
             'reason', g.reason, 'updated_at', g.updated_at
           ) order by g.student_id), '[]'::jsonb)
    from public.exam_time_grants g where g.exam_id = p_exam);
end $$;

-- ---------------------------------------------------------------------
-- ۶) محدودیت زمان سؤال‌ها (فقط معلم)
--    کل گروه آزمون جایگزین می‌شود.
-- ---------------------------------------------------------------------
drop function if exists public.native_question_limits_save_v357(text, jsonb);
create or replace function public.native_question_limits_save_v357(p_exam text, p_limits jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid   text := auth.uid()::text;
  v_key   text;
  v_q     integer;
  v_sec   integer;
  v_n     integer := 0;
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if not exists (select 1 from public.exams where id::text = p_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;
  if jsonb_typeof(p_limits) is distinct from 'object' then
    raise exception 'فهرست محدودیت‌ها باید شیء باشد.';
  end if;

  -- اعتبارسنجی کامل پیش از هر تغییر
  for v_key, v_sec in select k, (v.value #>> '{}')::integer from jsonb_each(p_limits) v(k, value) loop
    v_q := v_key::integer;
    if v_q < -1 then raise exception 'شاخص سؤال نامعتبر است.'; end if;
    if v_sec is null or v_sec < 10 or v_sec > 7200 then
      raise exception 'زمان هر سؤال باید بین ۱۰ تا ۷۲۰۰ ثانیه باشد.';
    end if;
    v_n := v_n + 1;
  end loop;
  if v_n > 500 then raise exception 'تعداد محدودیت‌ها بیش از حد است.'; end if;

  delete from public.exam_question_limits where exam_id = p_exam;
  insert into public.exam_question_limits(exam_id, question_index, seconds)
  select p_exam, k::integer, (v.value #>> '{}')::integer
    from jsonb_each(p_limits) v(k, value);

  return jsonb_build_object('ok', true, 'count', v_n);
end $$;

drop function if exists public.native_question_limits_v357(text);
create or replace function public.native_question_limits_v357(p_exam text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
  if auth.uid() is null then raise exception 'وارد حساب شوید.'; end if;
  return (
    select coalesce(jsonb_object_agg(l.question_index::text, l.seconds), '{}'::jsonb)
    from public.exam_question_limits l where l.exam_id = p_exam);
end $$;

-- ---------------------------------------------------------------------
-- ۷) همگام‌سازی زمان یک سؤال (دانش‌آموز)
--    نخستین بازدید، ساعت را روشن می‌کند. پس از انقضا، پاسخ همان سؤال
--    قفل می‌شود. بازگشت به سؤال، زمان را از نو شروع نمی‌کند.
-- ---------------------------------------------------------------------
drop function if exists public.native_sync_exam_question_v357(text, integer);
create or replace function public.native_sync_exam_question_v357(p_exam text, p_question integer)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid  text := auth.uid()::text;
  v_att  public.exam_attempts%rowtype;
  v_dl   jsonb;
  v_lock jsonb;
  v_key  text := p_question::text;
  v_sec  integer;
  v_exp  timestamptz;
  v_now  timestamptz := now();
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if p_question is null or p_question < 0 then raise exception 'شاخص سؤال نامعتبر است.'; end if;

  select * into v_att from public.exam_attempts
   where exam_id::text = p_exam and student_id::text = v_uid
   order by attempt_no desc limit 1;
  if not found then raise exception 'تلاشی برای این آزمون یافت نشد.'; end if;

  v_dl := case when jsonb_typeof(v_att.question_deadlines) = 'object'
               then v_att.question_deadlines else '{}'::jsonb end;
  v_lock := case when jsonb_typeof(v_att.question_locked) = 'object'
                then v_att.question_locked else '{}'::jsonb end;

  select l.seconds into v_sec from public.exam_question_limits l
   where l.exam_id = p_exam and l.question_index = p_question;
  if v_sec is null then
    select l.seconds into v_sec from public.exam_question_limits l
     where l.exam_id = p_exam and l.question_index = -1;
  end if;

  if v_sec is null then
    return jsonb_build_object('limited', false, 'locked', false, 'server_now', v_now);
  end if;

  if v_dl ? v_key then
    v_exp := (v_dl ->> v_key)::timestamptz;
  else
    -- مهلت سؤال هرگز از مهلت کل آزمون جلوتر نمی‌رود
    v_exp := v_now + make_interval(secs => v_sec);
    if v_att.expires_at is not null and v_exp > v_att.expires_at then
      v_exp := v_att.expires_at;
    end if;
    v_dl := v_dl || jsonb_build_object(v_key, v_exp);
  end if;

  if v_exp <= v_now then
    v_lock := v_lock || jsonb_build_object(v_key, true);
  end if;

  update public.exam_attempts
     set question_deadlines = v_dl, question_locked = v_lock
   where id = v_att.id;

  return jsonb_build_object(
    'limited', true,
    'locked', coalesce((v_lock ->> v_key)::boolean, false),
    'expires_at', v_exp,
    'seconds', v_sec,
    'server_now', v_now);
end $$;

-- ---------------------------------------------------------------------
-- ۸) بستهٔ آفلاین
--    توکن یک‌بارمصرف ساخته می‌شود. کلید پاسخ در بسته نیست.
-- ---------------------------------------------------------------------
drop function if exists public.native_offline_pack_v357(text);
create or replace function public.native_offline_pack_v357(p_exam text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid   text := auth.uid()::text;
  v_exam  public.exams%rowtype;
  v_att   public.exam_attempts%rowtype;
  v_token text;
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;

  select * into v_exam from public.exams where id::text = p_exam;
  if not found then raise exception 'آزمون پیدا نشد.'; end if;
  if not exists (select 1 from public.exam_attempts
                  where exam_id::text = p_exam and student_id::text = v_uid) then
    raise exception 'اول آزمون را شروع کنید، بعد بستهٔ آفلاین بگیرید.';
  end if;

  select * into v_att from public.exam_attempts
   where exam_id::text = p_exam and student_id::text = v_uid
   order by attempt_no desc limit 1;
  if v_att.expires_at is null then
    raise exception 'این آزمون مهلت زمانی ندارد و به بستهٔ آفلاین نیازی نیست.';
  end if;
  if v_att.offline_used_at is not null then
    raise exception 'بستهٔ آفلاین این تلاش یک بار مصرف شده است.';
  end if;

  v_token := v_att.offline_token;
  if v_token is null then
    -- بدون وابستگی به pgcrypto؛ gen_random_uuid در هستهٔ PostgreSQL است
    v_token := replace(gen_random_uuid()::text, '-', '') ||
               replace(gen_random_uuid()::text, '-', '');
    update public.exam_attempts
       set offline_token = v_token, offline_issued_at = now()
     where id = v_att.id;
  end if;

  return jsonb_build_object(
    'token', v_token,
    'expires_at', v_att.expires_at,
    'server_now', now(),
    'attempt_no', v_att.attempt_no,
    'question_deadlines', coalesce(v_att.question_deadlines, '{}'::jsonb),
    'limits', (select coalesce(jsonb_object_agg(l.question_index::text, l.seconds), '{}'::jsonb)
                 from public.exam_question_limits l where l.exam_id = p_exam));
end $$;

-- ---------------------------------------------------------------------
-- ۹) ارسال آفلاین
--    توکن باید بخورد، یک بار مصرف شود، و زمان پایان از مهلت جلوتر
--    نباشد. برگه «آفلاین» علامت می‌خورد تا معلم بداند نظارت زنده نداشته.
-- ---------------------------------------------------------------------
drop function if exists public.native_offline_submit_v357(text, text, jsonb, jsonb, timestamptz);
create or replace function public.native_offline_submit_v357(
  p_exam text, p_token text, p_responses jsonb, p_events jsonb, p_finished_at timestamptz
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid  text := auth.uid()::text;
  v_att  public.exam_attempts%rowtype;
  v_ans  text;
  v_grace constant integer := 120;  -- ثانیه: رواداری ساعت دستگاه
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if btrim(coalesce(p_token, '')) = '' then raise exception 'توکن آفلاین نامعتبر است.'; end if;
  if jsonb_typeof(p_responses) is distinct from 'array' then
    raise exception 'پاسخ‌ها باید آرایه باشند.';
  end if;
  if p_events is not null and jsonb_typeof(p_events) is distinct from 'array' then
    raise exception 'رویدادها باید آرایه باشند.';
  end if;

  select * into v_att from public.exam_attempts
   where exam_id::text = p_exam and student_id::text = v_uid
   order by attempt_no desc limit 1;
  if not found then raise exception 'تلاشی برای این آزمون یافت نشد.'; end if;
  if v_att.offline_token is null or v_att.offline_token <> p_token then
    raise exception 'توکن آفلاین با این تلاش نمی‌خواند.';
  end if;
  if v_att.offline_used_at is not null then
    raise exception 'این بستهٔ آفلاین پیش‌تر مصرف شده است.';
  end if;
  if p_finished_at is null then raise exception 'زمان پایان آفلاین لازم است.'; end if;
  if v_att.expires_at is not null
     and p_finished_at > v_att.expires_at + make_interval(secs => v_grace) then
    raise exception 'زمان پایان آفلاین پس از مهلت آزمون است.';
  end if;

  select a.id::text into v_ans from public.answers a
   where a.exam_id::text = p_exam and a.student_id::text = v_uid
   order by a.attempt_no desc nulls last limit 1;

  update public.exam_attempts
     set offline_used_at = now()
   where id = v_att.id;

  if v_ans is not null then
    update public.answers
       set responses = p_responses,
           meta = coalesce(meta, '{}'::jsonb) || jsonb_build_object(
                    'offline', true,
                    'offline_finished_at', p_finished_at,
                    'offline_events', coalesce(p_events, '[]'::jsonb))
     where id::text = v_ans;
  end if;

  return jsonb_build_object('ok', true, 'answer_id', v_ans, 'offline', true);
end $$;

-- ---------------------------------------------------------------------
-- ۱۰) دسترسی اجرا
-- ---------------------------------------------------------------------
grant execute on function public.native_time_grant_save_v357(text, text, integer, text) to authenticated;
grant execute on function public.native_time_grants_v357(text) to authenticated;
grant execute on function public.native_question_limits_save_v357(text, jsonb) to authenticated;
grant execute on function public.native_question_limits_v357(text) to authenticated;
grant execute on function public.native_sync_exam_question_v357(text, integer) to authenticated;
grant execute on function public.native_offline_pack_v357(text) to authenticated;
grant execute on function public.native_offline_submit_v357(text, text, jsonb, jsonb, timestamptz) to authenticated;

-- =====================================================================
-- بررسی درستی — همهٔ ردیف‌های زیر باید true باشند
-- =====================================================================

with checks(label, ok) as (
  select '>>> نصب V357 انجام شده است <<<',
         to_regclass('public.exam_time_grants') is not null
         and to_regclass('public.exam_question_limits') is not null
         and exists (select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
                      where n.nspname='public' and p.proname='native_offline_submit_v357')
  union all
  select 'جدول زمان اضافه ساخته شد', to_regclass('public.exam_time_grants') is not null
  union all
  select 'جدول محدودیت سؤال ساخته شد', to_regclass('public.exam_question_limits') is not null
  union all
  select 'ستون زمان اعمال‌شده روی تلاش ساخته شد',
         exists (select 1 from information_schema.columns
                 where table_schema='public' and table_name='exam_attempts'
                   and column_name='time_grant_applied')
  union all
  select 'ستون زمان اعمال‌شده روی نشست ساخته شد',
         exists (select 1 from information_schema.columns
                 where table_schema='public' and table_name='exam_sessions'
                   and column_name='time_grant_applied')
  union all
  select 'ستون مهلت سؤال‌ها ساخته شد',
         exists (select 1 from information_schema.columns
                 where table_schema='public' and table_name='exam_attempts'
                   and column_name='question_deadlines')
  union all
  select 'ستون قفل سؤال‌ها ساخته شد',
         exists (select 1 from information_schema.columns
                 where table_schema='public' and table_name='exam_attempts'
                   and column_name='question_locked')
  union all
  select 'ستون توکن آفلاین ساخته شد',
         exists (select 1 from information_schema.columns
                 where table_schema='public' and table_name='exam_attempts'
                   and column_name='offline_token')
  union all
  select 'تریگر زمان اضافه روی تلاش نصب شد',
         exists (select 1 from pg_trigger
                 where tgname='native_time_grant_attempt_v357' and not tgisinternal)
  union all
  select 'تریگر زمان اضافه روی نشست نصب شد',
         exists (select 1 from pg_trigger
                 where tgname='native_time_grant_session_v357' and not tgisinternal)
  union all
  select 'تابع ثبت زمان اضافه نصب شد',
         to_regprocedure('public.native_time_grant_save_v357(text,text,integer,text)') is not null
  union all
  select 'تابع خواندن زمان‌های اضافه نصب شد',
         to_regprocedure('public.native_time_grants_v357(text)') is not null
  union all
  select 'تابع ذخیرهٔ محدودیت سؤال نصب شد',
         to_regprocedure('public.native_question_limits_save_v357(text,jsonb)') is not null
  union all
  select 'تابع خواندن محدودیت سؤال نصب شد',
         to_regprocedure('public.native_question_limits_v357(text)') is not null
  union all
  select 'تابع همگام‌سازی زمان سؤال نصب شد',
         to_regprocedure('public.native_sync_exam_question_v357(text,integer)') is not null
  union all
  select 'تابع بستهٔ آفلاین نصب شد',
         to_regprocedure('public.native_offline_pack_v357(text)') is not null
  union all
  select 'تابع ارسال آفلاین نصب شد',
         to_regprocedure('public.native_offline_submit_v357(text,text,jsonb,jsonb,timestamptz)') is not null
  union all
  select 'RLS زمان اضافه روشن است',
         exists (select 1 from pg_class where relname='exam_time_grants' and relrowsecurity)
  union all
  select 'RLS محدودیت سؤال روشن است',
         exists (select 1 from pg_class where relname='exam_question_limits' and relrowsecurity)
  union all
  select 'دانش‌آموز فقط مجوز خودش را می‌بیند',
         exists (select 1 from pg_policies
                 where tablename='exam_time_grants'
                   and policyname='v357 student reads own grant')
  union all
  select 'هیچ سیاست نوشتنی برای دانش‌آموز ساخته نشد',
         not exists (select 1 from pg_policies
                     where tablename in ('exam_time_grants','exam_question_limits')
                       and cmd <> 'SELECT')
  union all
  select 'محدودیت دلیل زمان اضافه درست است',
         exists (select 1 from pg_constraint where conname='exam_time_grants_reason_ck')
  union all
  select 'سقف ثانیهٔ سؤال درست است',
         exists (select 1 from pg_constraint where conname='exam_question_limits_sec_ck')
)
select label as "بررسی", ok as "درست" from checks;

-- خلاصهٔ شمارش: اگر نصب نشده باشد، به‌جای خطای ۴۲P01 پیام روشن می‌دهد.
do $$
declare v_n bigint;
begin
  if to_regclass('public.exam_time_grants') is null
     or to_regclass('public.exam_question_limits') is null then
    raise notice 'هنوز نصب نشده است. ابتدا فایل نصب را اجرا کنید.';
    return;
  end if;
  execute 'select count(*) from public.exam_time_grants' into v_n;
  raise notice 'مجوز زمان ثبت‌شده: %', v_n;
  execute 'select count(*) from public.exam_question_limits' into v_n;
  raise notice 'محدودیت سؤال ثبت‌شده: %', v_n;
  execute 'select count(*) from public.exam_attempts where offline_token is not null' into v_n;
  raise notice 'بستهٔ آفلاین صادرشده: %', v_n;
end $$;
