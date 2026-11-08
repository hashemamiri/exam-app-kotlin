-- =====================================================================
-- V355 — بستهٔ ۲: کلیدواژه‌های تصحیح تشریحی + سابقهٔ بررسی شباهت
--
-- اجرا: در Supabase → SQL Editor، کل این فایل را یک‌جا اجرا کنید.
-- ایمنی: هیچ نمره یا پاسخ تاریخی بازنویسی نمی‌شود. فقط دو جدول تازه و
--        چهار تابع ساخته می‌شود. اجرای دوباره بی‌خطر است (idempotent).
--
-- نکتهٔ نوع (درسِ V354): در این پایگاه‌داده answers.id و exams.id از نوع
--   text هستند و exams.teacher_id از نوع uuid. پس همهٔ شناسه‌ها این‌جا
--   text نگه داشته می‌شوند و هر مقایسه با ::text در دو طرف انجام می‌شود.
--
-- نکتهٔ طراحی: الگوریتم شباهت و انطباق کلیدواژه در سمت معلم (سایت/اپ)
--   اجرا می‌شود و هیچ‌جا نمره را خودکار تغییر نمی‌دهد. این جدول‌ها فقط
--   «تعریف کلیدواژه» و «تصمیم معلم دربارهٔ هشدار شباهت» را نگه می‌دارند.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ۱) کلیدواژه‌های راهنمای تصحیح تشریحی
--    part_key خالی = کل سؤال؛ در غیر این صورت شناسهٔ زیربخش
-- ---------------------------------------------------------------------
create table if not exists public.essay_keywords (
  id             bigserial primary key,
  exam_id        text not null,
  question_index integer not null,
  part_key       text not null default '',
  keyword        text not null,
  weight         numeric not null default 1,
  created_at     timestamptz not null default now(),
  constraint essay_keywords_weight_ck check (weight > 0 and weight <= 100),
  constraint essay_keywords_len_ck check (char_length(btrim(keyword)) between 1 and 120),
  constraint essay_keywords_q_ck check (question_index >= 0)
);

-- تبدیل جدول بازمانده از اجرای ناقص با نوع uuid (بی‌زیان)
do $$
declare c text; ty text;
begin
  if to_regclass('public.essay_keywords') is null then return; end if;
  select data_type into ty from information_schema.columns
   where table_schema='public' and table_name='essay_keywords' and column_name='exam_id';
  if ty is not null and ty <> 'text' then
    execute 'alter table public.essay_keywords alter column exam_id type text using exam_id::text';
  end if;
end $$;

create unique index if not exists essay_keywords_uniq
  on public.essay_keywords(exam_id, question_index, part_key, keyword);
create index if not exists essay_keywords_exam_idx on public.essay_keywords(exam_id);

alter table public.essay_keywords enable row level security;

drop policy if exists "v355 teacher owns essay keywords" on public.essay_keywords;
create policy "v355 teacher owns essay keywords" on public.essay_keywords
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = essay_keywords.exam_id
               and e.teacher_id::text = auth.uid()::text)
  );

-- ---------------------------------------------------------------------
-- ۲) تصمیم معلم دربارهٔ هشدار شباهت
--    status: open (بررسی‌نشده) | checked (بررسی شد، مشکلی نبود)
--            | confirmed (تخلف تأیید شد)
--    pair_key: دو شناسهٔ پاسخ، مرتب‌شده و با «|» جدا شده
-- ---------------------------------------------------------------------
create table if not exists public.similarity_reviews (
  id             bigserial primary key,
  exam_id        text not null,
  pair_key       text not null,
  question_index integer not null,
  part_key       text not null default '',
  status         text not null default 'open',
  note           text,
  reviewed_by    text,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  constraint similarity_reviews_status_ck
    check (status in ('open', 'checked', 'confirmed')),
  constraint similarity_reviews_note_ck
    check (note is null or char_length(note) <= 2000),
  constraint similarity_reviews_q_ck check (question_index >= -1)
);

do $$
declare ty text;
begin
  if to_regclass('public.similarity_reviews') is null then return; end if;
  select data_type into ty from information_schema.columns
   where table_schema='public' and table_name='similarity_reviews' and column_name='exam_id';
  if ty is not null and ty <> 'text' then
    execute 'alter table public.similarity_reviews alter column exam_id type text using exam_id::text';
  end if;
  select data_type into ty from information_schema.columns
   where table_schema='public' and table_name='similarity_reviews' and column_name='reviewed_by';
  if ty is not null and ty <> 'text' then
    execute 'alter table public.similarity_reviews alter column reviewed_by type text using reviewed_by::text';
  end if;
end $$;

create unique index if not exists similarity_reviews_uniq
  on public.similarity_reviews(exam_id, pair_key, question_index, part_key);
create index if not exists similarity_reviews_exam_idx on public.similarity_reviews(exam_id);

alter table public.similarity_reviews enable row level security;

drop policy if exists "v355 teacher owns similarity reviews" on public.similarity_reviews;
create policy "v355 teacher owns similarity reviews" on public.similarity_reviews
  for select using (
    exists (select 1 from public.exams e
             where e.id::text = similarity_reviews.exam_id
               and e.teacher_id::text = auth.uid()::text)
  );

-- ---------------------------------------------------------------------
-- ۳) خواندن کلیدواژه‌های یک آزمون (فقط معلم)
-- ---------------------------------------------------------------------
drop function if exists public.native_essay_keywords_v355(uuid);
create or replace function public.native_essay_keywords_v355(p_exam text)
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
             'id', k.id, 'question_index', k.question_index, 'part_key', k.part_key,
             'keyword', k.keyword, 'weight', k.weight
           ) order by k.question_index, k.part_key, k.id), '[]'::jsonb)
    from public.essay_keywords k
    where k.exam_id = p_exam);
end $$;

-- ---------------------------------------------------------------------
-- ۴) ذخیرهٔ کلیدواژه‌های یک سؤال/زیربخش
--    کل گروه جایگزین می‌شود؛ بقیهٔ سؤال‌ها دست‌نخورده می‌مانند.
--    آرایهٔ ورودی: [{"keyword":"...","weight":1}, ...]
-- ---------------------------------------------------------------------
drop function if exists public.native_essay_keywords_save_v355(uuid, integer, text, jsonb);
create or replace function public.native_essay_keywords_save_v355(
  p_exam text, p_question integer, p_part text, p_items jsonb
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid  text := auth.uid()::text;
  v_part text := coalesce(p_part, '');
  v_item jsonb;
  v_kw   text;
  v_w    numeric;
  v_n    integer := 0;
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if not exists (select 1 from public.exams where id::text = p_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;
  if p_question is null or p_question < 0 then raise exception 'شاخص سؤال نامعتبر است.'; end if;
  if jsonb_typeof(p_items) is distinct from 'array' then
    raise exception 'فهرست کلیدواژه‌ها باید آرایه باشد.';
  end if;
  if jsonb_array_length(p_items) > 60 then
    raise exception 'برای هر مورد بیش از ۶۰ کلیدواژه نمی‌توان تعریف کرد.';
  end if;

  -- اعتبارسنجی پیش از هر تغییر، تا ذخیرهٔ نیمه‌کاره رخ ندهد
  for v_item in select * from jsonb_array_elements(p_items) loop
    v_kw := btrim(coalesce(v_item ->> 'keyword', ''));
    if char_length(v_kw) < 1 or char_length(v_kw) > 120 then
      raise exception 'هر کلیدواژه باید بین ۱ تا ۲۰ نویسه باشد.';
    end if;
    v_w := coalesce((v_item ->> 'weight')::numeric, 1);
    if v_w <= 0 or v_w > 100 then raise exception 'وزن کلیدواژه نامعتبر است.'; end if;
  end loop;

  delete from public.essay_keywords
   where exam_id = p_exam and question_index = p_question and part_key = v_part;

  for v_item in select * from jsonb_array_elements(p_items) loop
    v_kw := btrim(v_item ->> 'keyword');
    v_w  := coalesce((v_item ->> 'weight')::numeric, 1);
    insert into public.essay_keywords(exam_id, question_index, part_key, keyword, weight)
    values (p_exam, p_question, v_part, v_kw, v_w)
    on conflict (exam_id, question_index, part_key, keyword) do update set weight = excluded.weight;
    v_n := v_n + 1;
  end loop;

  return jsonb_build_object('ok', true, 'count', v_n);
end $$;

-- ---------------------------------------------------------------------
-- ۵) خواندن تصمیم‌های شباهت (فقط معلم)
-- ---------------------------------------------------------------------
drop function if exists public.native_similarity_reviews_v355(uuid);
create or replace function public.native_similarity_reviews_v355(p_exam text)
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
             'id', r.id, 'pair_key', r.pair_key, 'question_index', r.question_index,
             'part_key', r.part_key, 'status', r.status, 'note', r.note,
             'updated_at', r.updated_at
           ) order by r.updated_at desc), '[]'::jsonb)
    from public.similarity_reviews r
    where r.exam_id = p_exam);
end $$;

-- ---------------------------------------------------------------------
-- ۶) ثبت تصمیم معلم دربارهٔ یک جفت پاسخ
--    این تابع هرگز نمره را تغییر نمی‌دهد؛ فقط تصمیم را ثبت می‌کند.
-- ---------------------------------------------------------------------
drop function if exists public.native_similarity_review_save_v355(uuid, text, integer, text, text, text);
create or replace function public.native_similarity_review_save_v355(
  p_exam text, p_pair text, p_question integer, p_part text, p_status text, p_note text
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid  text := auth.uid()::text;
  v_st   text := coalesce(p_status, '');
  v_part text := coalesce(p_part, '');
  v_pair text := btrim(coalesce(p_pair, ''));
  v_note text := nullif(btrim(coalesce(p_note, '')), '');
begin
  if v_uid is null then raise exception 'وارد حساب شوید.'; end if;
  if not exists (select 1 from public.exams where id::text = p_exam and teacher_id::text = v_uid) then
    raise exception 'این آزمون متعلق به شما نیست.';
  end if;
  if v_st not in ('open', 'checked', 'confirmed') then raise exception 'وضعیت نامعتبر است.'; end if;
  if char_length(v_pair) < 3 or char_length(v_pair) > 300 then
    raise exception 'شناسهٔ جفت پاسخ نامعتبر است.';
  end if;
  if v_st = 'confirmed' and (v_note is null or char_length(v_note) < 3) then
    raise exception 'برای تأیید تخلف، توضیح دست‌کم ۳ نویسه لازم است.';
  end if;
  if v_note is not null and char_length(v_note) > 2000 then
    raise exception 'توضیح بیش از حد بلند است.';
  end if;

  insert into public.similarity_reviews(exam_id, pair_key, question_index, part_key, status, note, reviewed_by)
  values (p_exam, v_pair, coalesce(p_question, -1), v_part, v_st, v_note, v_uid)
  on conflict (exam_id, pair_key, question_index, part_key)
  do update set status = excluded.status, note = excluded.note,
                reviewed_by = excluded.reviewed_by, updated_at = now();

  return jsonb_build_object('ok', true, 'status', v_st);
end $$;

-- ---------------------------------------------------------------------
-- ۷) دسترسی اجرا
-- ---------------------------------------------------------------------
grant execute on function public.native_essay_keywords_v355(text) to authenticated;
grant execute on function public.native_essay_keywords_save_v355(text, integer, text, jsonb) to authenticated;
grant execute on function public.native_similarity_reviews_v355(text) to authenticated;
grant execute on function public.native_similarity_review_save_v355(text, text, integer, text, text, text) to authenticated;

-- =====================================================================
-- پایان. برای بررسی درستی، فایل SQL_GRADING_SIMILARITY_V355_CHECK.sql را اجرا کنید.
-- =====================================================================
