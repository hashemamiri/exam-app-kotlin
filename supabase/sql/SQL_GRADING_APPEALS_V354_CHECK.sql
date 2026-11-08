-- =====================================================================
-- V354 — بررسی درستی نصب (فقط خواندنی؛ چیزی را تغییر نمی‌دهد)
-- پس از اجرای SQL_GRADING_APPEALS_V354.sql این را اجرا کنید.
-- انتظار: همهٔ ردیف‌ها ستون ok = true داشته باشند.
-- =====================================================================

with checks(label, ok) as (
  select 'جدول تاریخچهٔ نمره ساخته شد',
         to_regclass('public.grade_history') is not null
  union all
  select 'جدول اعتراض‌ها ساخته شد',
         to_regclass('public.grade_appeals') is not null
  union all
  select 'تریگر تاریخچه روی answers نصب شد',
         exists (select 1 from pg_trigger
                 where tgname = 'grade_history_audit_v354' and not tgisinternal)
  union all
  select 'RLS تاریخچه روشن است',
         exists (select 1 from pg_class
                 where relname = 'grade_history' and relrowsecurity)
  union all
  select 'RLS اعتراض‌ها روشن است',
         exists (select 1 from pg_class
                 where relname = 'grade_appeals' and relrowsecurity)
  union all
  select 'تابع ثبت اعتراض ساخته شد',
         to_regprocedure('public.native_appeal_create_v354(text,integer,text)') is not null
  union all
  select 'تابع اعتراض‌های من ساخته شد',
         to_regprocedure('public.native_my_appeals_v354()') is not null
  union all
  select 'تابع اعتراض‌های آزمون ساخته شد',
         to_regprocedure('public.native_exam_appeals_v354(text)') is not null
  union all
  select 'تابع تعیین‌تکلیف اعتراض ساخته شد',
         to_regprocedure('public.native_appeal_resolve_v354(uuid,text,text,numeric)') is not null
  union all
  select 'تابع تاریخچهٔ نمره ساخته شد',
         to_regprocedure('public.native_grade_history_v354(text)') is not null
  union all
  select 'نوع ستون‌های شناسه با پایگاه‌داده سازگار است',
         (select count(*) from information_schema.columns
           where table_schema='public' and table_name in ('grade_history','grade_appeals')
             and column_name in ('answer_id','exam_id','student_id','changed_by','second_by','resolved_by')
             and data_type <> 'text') = 0
  union all
  select 'محدودیت وضعیت اعتراض درست است',
         exists (select 1 from pg_constraint
                 where conname = 'grade_appeals_status_ck')
  union all
  select 'ایندکس «یک اعتراض باز» ساخته شد',
         exists (select 1 from pg_indexes
                 where indexname = 'grade_appeals_one_open_idx')
  union all
  select 'هیچ سیاست نوشتنِ عمومی روی تاریخچه نیست',
         not exists (select 1 from pg_policies
                     where tablename = 'grade_history' and cmd <> 'SELECT')
)
select label as "بررسی", ok as "درست" from checks;

-- شمار ردیف‌های فعلی (برای اطلاع؛ صفر بودن طبیعی است)
select
  (select count(*) from public.grade_history) as "ردیف تاریخچه",
  (select count(*) from public.grade_appeals) as "ردیف اعتراض";
