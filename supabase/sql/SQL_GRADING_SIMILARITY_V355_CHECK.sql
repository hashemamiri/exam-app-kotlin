-- =====================================================================
-- V355 — بررسی درستی نصب (فقط خواندنی؛ چیزی را تغییر نمی‌دهد)
-- پس از اجرای SQL_GRADING_SIMILARITY_V355.sql این را اجرا کنید.
-- انتظار: همهٔ ردیف‌ها ستون ok = true داشته باشند.
-- =====================================================================

with checks(label, ok) as (
  select 'جدول کلیدواژه‌ها ساخته شد',
         to_regclass('public.essay_keywords') is not null
  union all
  select 'جدول بررسی شباهت ساخته شد',
         to_regclass('public.similarity_reviews') is not null
  union all
  select 'RLS کلیدواژه‌ها روشن است',
         exists (select 1 from pg_class
                 where relname = 'essay_keywords' and relrowsecurity)
  union all
  select 'RLS بررسی شباهت روشن است',
         exists (select 1 from pg_class
                 where relname = 'similarity_reviews' and relrowsecurity)
  union all
  select 'تابع خواندن کلیدواژه‌ها نصب شد',
         to_regprocedure('public.native_essay_keywords_v355(text)') is not null
  union all
  select 'تابع ذخیرهٔ کلیدواژه‌ها نصب شد',
         to_regprocedure('public.native_essay_keywords_save_v355(text,integer,text,jsonb)') is not null
  union all
  select 'تابع خواندن بررسی شباهت نصب شد',
         to_regprocedure('public.native_similarity_reviews_v355(text)') is not null
  union all
  select 'تابع ثبت تصمیم شباهت نصب شد',
         to_regprocedure('public.native_similarity_review_save_v355(text,text,integer,text,text,text)') is not null
  union all
  select 'نوع ستون‌های شناسه با پایگاه‌داده سازگار است',
         (select count(*) from information_schema.columns
           where table_schema='public' and table_name in ('essay_keywords','similarity_reviews')
             and column_name in ('exam_id','reviewed_by','part_key','pair_key')
             and data_type <> 'text') = 0
  union all
  select 'کلید یکتای کلیدواژه ساخته شد',
         exists (select 1 from pg_indexes
                 where tablename = 'essay_keywords' and indexname = 'essay_keywords_uniq')
  union all
  select 'کلید یکتای بررسی شباهت ساخته شد',
         exists (select 1 from pg_indexes
                 where tablename = 'similarity_reviews' and indexname = 'similarity_reviews_uniq')
  union all
  select 'محدودیت وضعیت بررسی درست است',
         exists (select 1 from pg_constraint
                 where conname = 'similarity_reviews_status_ck')
  union all
  select 'محدودیت وزن کلیدواژه درست است',
         exists (select 1 from pg_constraint
                 where conname = 'essay_keywords_weight_ck')
  union all
  select 'هیچ سیاست نوشتنی برای دانش‌آموز ساخته نشد',
         not exists (select 1 from pg_policies
                     where tablename in ('essay_keywords','similarity_reviews')
                       and cmd <> 'SELECT')
)
select label as "بررسی", ok as "درست" from checks;

-- شمار ردیف‌های فعلی (برای اطلاع؛ صفر بودن طبیعی است)
select
  (select count(*) from public.essay_keywords) as "ردیف کلیدواژه",
  (select count(*) from public.similarity_reviews) as "ردیف بررسی شباهت";
