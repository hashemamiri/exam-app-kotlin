-- =====================================================================
-- V356 — بستهٔ ۳: تصحیح کد، فاز صفر (بدون اجرای کد)
--
-- اجرا: در Supabase → SQL Editor، کل این فایل را یک‌جا اجرا کنید.
-- ایمنی: هیچ نمره یا پاسخ تاریخی بازنویسی نمی‌شود. فقط سه جدول تازه و
--        چهار تابع ساخته می‌شود. اجرای دوباره بی‌خطر است (idempotent).
--
-- فاز صفر یعنی چه: پاسخ مرجع و موارد آزمون نگه داشته می‌شوند و معلم
--   در یک چک‌لیست، نتیجهٔ هر مورد را خودش علامت می‌زند. نمره از وزن
--   موردهای قبول‌شده پیشنهاد می‌شود. **کد دانش‌آموز هرگز اجرا نمی‌شود.**
--
-- نکتهٔ نوع (درسِ V354): answers.id و exams.id در این پایگاه‌داده text
--   هستند و exams.teacher_id نوع uuid دارد. پس شناسه‌ها text نگه داشته
--   می‌شوند و هر مقایسه با ::text در دو طرف انجام می‌شود.
--
-- محرمانگی: پاسخ مرجع و موردهای آزمون فقط برای معلم آزمون خواندنی‌اند.
--   هیچ سیاستی برای دانش‌آموز ساخته نمی‌شود، پس مورد پنهان و پاسخ
--   مرجع هرگز به دانش‌آموز نمی‌رسد.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ۱) مشخصات تصحیح کد برای هر سؤال: زبان و پاسخ مرجع
-- ---------------------------------------------------------------------
create table if not exists public.code_specs (
  exam_id        text not null,
  question_index integer not null,
  language       text not null default 'python',
  reference      text not null default '',
  ignore_case    boolean not null default false,
  trim_lines     boolean not null default true,
  collapse_blank boolean not null default true,
  updated_at     timestamptz not null default now(),
  primary key (exam_id, question_index),
  constraint code_specs_q_ck check (question_index >= 0),
  constraint code_specs_lang_ck check (char_length(language) between 1 and 40),
  constraint code_specs_ref_ck check (char_length(reference) <= 20000)
);

-- ---------------------------------------------------------------------
-- ۲) موارد آزمون هر سؤال
--    hidden = فقط معلم می‌بیند (روی برگهٔ دانش‌آموز چاپ نمی‌شود)
-- ---------------------------------------------------------------------
create table if not exists public.code_tests (
  id             bigserial primary key,
  exam_id        text not null,
  question_index integer not null,
  ord            integer not null default 0,
  label          text not null default '',
  input          text not null default '',
  expected       text not null default '',
  weight         numeric not null default 1,
  hidden         boolean not null default false,
  created_at     timestamptz not null default now(),
  constraint code_tests_q_ck check (question_index >= 0),
  constraint code_tests_weight_ck check (weight > 0 and weight <= 100),
  constraint code_tests_len_ck check (
    char_length(label) <= 120 and char_length(input) <= 4000 and char_length(expected) <= 4000)
);

create index if not exists code_tests_exam_idx on public.code_tests(exam_id, question_index, ord);

-- ---------------------------------------------------------------------
-- ۳) چک‌لیست معلم برای هر پاسخ
--    passed = نتیجه‌ای که معلم خودش دیده؛ سامانه چیزی اجرا نمی‌کند.
--    actual = خروجی واقعی که معلم جای‌گذاری کرده (اختیاری)
-- ---------------------------------------------------------------------
create table if not exists public.code_checks (
  id             bigserial primary key,
  answer_id      text not null,
  exam_id        text not null,
  question_index integer not null,
  test_id        bigint not null,
  passed         boolean not null default false,
  actual         text not null default '',
  note           text,
  checked_by     text,
  updated_at     timestamptz not null default now(),
  constraint code_checks_q_ck check (question_index >= 0),
  constraint code_checks_actual_ck check (char_length(actual) <= 4000),
  constraint code_checks_note_ck check (note is null or char_length(note) <= 1000)
);

create unique index if not exists code_checks_uniq
  on public.code_checks(answer_id, question_index, test_id);
create index if not exists code_checks_exam_idx on public.code_checks(exam_id);

-- تبدیل جدول بازمانده از اجرای ناقص با نوع uuid (بی‌زیان)
do $$
declare t text; c text; ty text;
begin
  foreach t in array array['code_specs', 'code_tests', 'code_checks'] loop
    if to_regclass('public.' || t) is null then continue; end if;
    foreach c in array array['exam_id', 'answer_id', 'checked_by'] loop
      select data_type into ty from information_schema.columns
       where table_schema = 'public' and table_name = t and column_name = c;
      if ty is not null and ty <> 'text' then
        execute format('alter table public.%I alter column %I type text using %I::text', t, c, c);
      end if;
    end loop;
  end loop;
end $$;

alter table public.code_specs  enable row level security;
alter table public.code_tests  enable row level security;
alter table public.code_checks enable row level security;

drop policy if exists "v356 teacher owns code specs" on public.code_specs;
create policy "v356 teacher owns code specs" on public.code_specs
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = code_specs.exam_id and e.teacher_id::text = auth.uid()::text));

drop policy if exists "v356 teacher owns code tests" on public.code_tests;
create policy "v356 teacher owns code tests" on public.code_tests
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = code_tests.exam_id and e.teacher_id::text = auth.uid()::text));

drop policy if exists "v356 teacher owns code checks" on public.code_checks;
create policy "v356 teacher owns code checks" on public.code_checks
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = code_checks.exam_id and e.teacher_id::text = auth.uid()::text));

-- ---------------------------------------------------------------------
-- ۴) خواندن مشخصات و موارد آزمون یک آزمون (فقط معلم)
-- ---------------------------------------------------------------------
drop function if exists public.native_code_spec_get_v356(uuid);
create or replace function public.native_code_spec_get_v356(p_exam text)
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

  return jsonb_build_object(
    'specs', (
      select coalesce(jsonb_agg(jsonb_build_object(
               'question_index', s.question_index, 'language', s.language,
               'reference', s.reference, 'ignore_case', s.ignore_case,
               'trim_lines', s.trim_lines, 'collapse_blank', s.collapse_blank
             ) order by s.question_index), '[]'::jsonb)
      from public.code_specs s where s.exam_id = p_exam),
    'tests', (
      select coalesce(jsonb_agg(jsonb_build_object(
               'id', t.id, 'question_index', t.question_index, 'ord', t.ord,
               'label', t.label, 'input', t.input, 'expected', t.expected,
               'weight', t.weight, 'hidden', t.hidden
             ) order by t.question_index, t.ord, t.id), '[]'::jsonb)
      from public.code_tests t where t.exam_id = p_exam));
end $$;

-- ---------------------------------------------------------------------
-- ۵) ذخیرهٔ مشخصات و موارد آزمون یک سؤال
--    کل گروه همان سؤال جایگزین می‌شود؛ سؤال‌های دیگر دست‌نخورده.
--    هشدار: چک‌لیست ثبت‌شدهٔ همان سؤال پاک می‌شود، چون موردها شناسهٔ
--    تازه می‌گیرند. تعداد پاک‌شده در پاسخ برمی‌گردد تا معلم آگاه شود.
-- ---------------------------------------------------------------------
drop function if exists public.native_code_spec_save_v356(uuid, integer, text, text, jsonb);
create or replace function public.native_code_spec_save_v356(
  p_exam text, p_question integer, p_language text, p_reference text,
  p_tests jsonb, p_ignore_case boolean default false,
  p_trim_lines boolean default true, p_collapse_blank boolean default true
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid  text := auth.uid()::text;
  v_lang text := btrim(coalesce(p_language, 'python'));
  v_ref  text := coalesce(p_reference, '');
  v_item jsonb;
  v_w    numeric;
  v_ord  integer := 0;
  v_n    integer := 0;
  v_cleared integer := 0;
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if not exists (select 1 from public.exams where id::text = p_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;
  if p_question is null or p_question < 0 then raise exception 'شاخص سؤال نامعتبر است.'; end if;
  if char_length(v_lang) < 1 or char_length(v_lang) > 40 then raise exception 'نام زبان نامعتبر است.'; end if;
  if char_length(v_ref) > 20000 then raise exception 'پاسخ مرجع بیش از حد بلند است.'; end if;
  if jsonb_typeof(p_tests) is distinct from 'array' then
    raise exception 'فهرست موارد آزمون باید آرایه باشد.';
  end if;
  if jsonb_array_length(p_tests) > 40 then
    raise exception 'برای هر سؤال بیش از ۴۰ مورد آزمون نمی‌توان تعریف کرد.';
  end if;

  -- اعتبارسنجی کامل پیش از هر تغییر، تا ذخیرهٔ نیمه‌کاره رخ ندهد
  for v_item in select * from jsonb_array_elements(p_tests) loop
    if char_length(coalesce(v_item ->> 'label', '')) > 120 then
      raise exception 'برچسب مورد آزمون بلندتر از ۱۲۰ نویسه است.';
    end if;
    if char_length(coalesce(v_item ->> 'input', '')) > 4000
       or char_length(coalesce(v_item ->> 'expected', '')) > 4000 then
      raise exception 'ورودی یا خروجی مورد آزمون بیش از حد بلند است.';
    end if;
    v_w := coalesce((v_item ->> 'weight')::numeric, 1);
    if v_w <= 0 or v_w > 100 then raise exception 'وزن مورد آزمون نامعتبر است.'; end if;
  end loop;

  insert into public.code_specs(exam_id, question_index, language, reference,
                                ignore_case, trim_lines, collapse_blank, updated_at)
  values (p_exam, p_question, v_lang, v_ref,
          coalesce(p_ignore_case, false), coalesce(p_trim_lines, true),
          coalesce(p_collapse_blank, true), now())
  on conflict (exam_id, question_index) do update
    set language = excluded.language, reference = excluded.reference,
        ignore_case = excluded.ignore_case, trim_lines = excluded.trim_lines,
        collapse_blank = excluded.collapse_blank, updated_at = now();

  -- چون موردها با شناسهٔ تازه ساخته می‌شوند، چک‌لیست پیشین این سؤال
  -- دیگر به مورد معتبری اشاره نمی‌کند. پس پاک می‌شود و تعدادش به معلم
  -- برگردانده می‌شود تا در رابط هشدار داده شود.
  -- (نسخهٔ نخست این پاک‌سازی را پیش از حذف موردها انجام می‌داد و
  --  به همین دلیل هرگز ردیفی پاک نمی‌کرد.)
  with gone as (
    delete from public.code_checks
     where exam_id = p_exam and question_index = p_question
    returning 1)
  select count(*) into v_cleared from gone;
  delete from public.code_tests where exam_id = p_exam and question_index = p_question;

  for v_item in select * from jsonb_array_elements(p_tests) loop
    v_w := coalesce((v_item ->> 'weight')::numeric, 1);
    insert into public.code_tests(exam_id, question_index, ord, label, input, expected, weight, hidden)
    values (p_exam, p_question, v_ord,
            coalesce(v_item ->> 'label', ''),
            coalesce(v_item ->> 'input', ''),
            coalesce(v_item ->> 'expected', ''),
            v_w, coalesce((v_item ->> 'hidden')::boolean, false));
    v_ord := v_ord + 1;
    v_n := v_n + 1;
  end loop;

  return jsonb_build_object('ok', true, 'count', v_n, 'cleared_checks', v_cleared);
end $$;

-- ---------------------------------------------------------------------
-- ۶) خواندن چک‌لیست‌های یک آزمون (فقط معلم)
-- ---------------------------------------------------------------------
drop function if exists public.native_code_checks_v356(uuid);
create or replace function public.native_code_checks_v356(p_exam text)
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
             'answer_id', c.answer_id, 'question_index', c.question_index,
             'test_id', c.test_id, 'passed', c.passed, 'actual', c.actual,
             'note', c.note, 'updated_at', c.updated_at
           ) order by c.answer_id, c.question_index, c.test_id), '[]'::jsonb)
    from public.code_checks c where c.exam_id = p_exam);
end $$;

-- ---------------------------------------------------------------------
-- ۷) ثبت نتیجهٔ یک مورد آزمون برای یک پاسخ
--    این تابع نمره را تغییر نمی‌دهد؛ فقط چک‌لیست را نگه می‌دارد.
--    نمره را معلم در تب تصحیح اعمال می‌کند تا در تاریخچهٔ V354 ثبت شود.
-- ---------------------------------------------------------------------
drop function if exists public.native_code_check_save_v356(uuid, integer, bigint, boolean, text, text);
create or replace function public.native_code_check_save_v356(
  p_answer text, p_question integer, p_test bigint,
  p_passed boolean, p_actual text default '', p_note text default ''
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid    text := auth.uid()::text;
  v_ans    text := btrim(coalesce(p_answer, ''));
  v_exam   text;
  v_actual text := coalesce(p_actual, '');
  v_note   text := nullif(btrim(coalesce(p_note, '')), '');
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if char_length(v_ans) < 1 then raise exception 'شناسهٔ پاسخ نامعتبر است.'; end if;
  if p_question is null or p_question < 0 then raise exception 'شاخص سؤال نامعتبر است.'; end if;
  if char_length(v_actual) > 4000 then raise exception 'خروجی واقعی بیش از حد بلند است.'; end if;
  if v_note is not null and char_length(v_note) > 1000 then raise exception 'یادداشت بیش از حد بلند است.'; end if;

  select a.exam_id::text into v_exam from public.answers a where a.id::text = v_ans;
  if v_exam is null then raise exception 'پاسخ پیدا نشد.'; end if;
  if not exists (select 1 from public.exams where id::text = v_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;
  if not exists (select 1 from public.code_tests
                  where id = p_test and exam_id = v_exam and question_index = p_question) then
    raise exception 'مورد آزمون پیدا نشد یا از این سؤال نیست.';
  end if;

  insert into public.code_checks(answer_id, exam_id, question_index, test_id, passed, actual, note, checked_by, updated_at)
  values (v_ans, v_exam, p_question, p_test, coalesce(p_passed, false), v_actual, v_note, v_uid, now())
  on conflict (answer_id, question_index, test_id) do update
    set passed = excluded.passed, actual = excluded.actual, note = excluded.note,
        checked_by = excluded.checked_by, updated_at = now();

  return jsonb_build_object('ok', true, 'passed', coalesce(p_passed, false));
end $$;

-- ---------------------------------------------------------------------
-- ۸) دسترسی اجرا
-- ---------------------------------------------------------------------
grant execute on function public.native_code_spec_get_v356(text) to authenticated;
grant execute on function public.native_code_spec_save_v356(text, integer, text, text, jsonb, boolean, boolean, boolean) to authenticated;
grant execute on function public.native_code_checks_v356(text) to authenticated;
grant execute on function public.native_code_check_save_v356(text, integer, bigint, boolean, text, text) to authenticated;

-- =====================================================================
-- پایان. برای بررسی درستی، فایل SQL_CODE_GRADING_V356_CHECK.sql را اجرا کنید.
-- =====================================================================
