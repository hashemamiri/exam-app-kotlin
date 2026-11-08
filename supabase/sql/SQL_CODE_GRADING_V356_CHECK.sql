-- =====================================================================
-- V356 — بررسی درستی نصب (فقط خواندنی؛ چیزی را تغییر نمی‌دهد)
-- پس از اجرای SQL_CODE_GRADING_V356.sql این را اجرا کنید.
-- انتظار: همهٔ ردیف‌ها ستون ok = true داشته باشند.
-- =====================================================================

with checks(label, ok) as (
  select 'جدول مشخصات کد ساخته شد',
         to_regclass('public.code_specs') is not null
  union all
  select 'جدول موارد آزمون ساخته شد',
         to_regclass('public.code_tests') is not null
  union all
  select 'جدول چک‌لیست معلم ساخته شد',
         to_regclass('public.code_checks') is not null
  union all
  select 'RLS مشخصات روشن است',
         exists (select 1 from pg_class where relname = 'code_specs' and relrowsecurity)
  union all
  select 'RLS موارد آزمون روشن است',
         exists (select 1 from pg_class where relname = 'code_tests' and relrowsecurity)
  union all
  select 'RLS چک‌لیست روشن است',
         exists (select 1 from pg_class where relname = 'code_checks' and relrowsecurity)
  union all
  select 'تابع خواندن مشخصات نصب شد',
         to_regprocedure('public.native_code_spec_get_v356(text)') is not null
  union all
  select 'تابع ذخیرهٔ مشخصات نصب شد',
         to_regprocedure('public.native_code_spec_save_v356(text,integer,text,text,jsonb,boolean,boolean,boolean)') is not null
  union all
  select 'تابع خواندن چک‌لیست نصب شد',
         to_regprocedure('public.native_code_checks_v356(text)') is not null
  union all
  select 'تابع ثبت نتیجهٔ مورد آزمون نصب شد',
         to_regprocedure('public.native_code_check_save_v356(text,integer,bigint,boolean,text,text)') is not null
  union all
  select 'نوع ستون‌های شناسه با پایگاه‌داده سازگار است',
         (select count(*) from information_schema.columns
           where table_schema = 'public'
             and table_name in ('code_specs','code_tests','code_checks')
             and column_name in ('exam_id','answer_id','checked_by')
             and data_type <> 'text') = 0
  union all
  select 'کلید یکتای چک‌لیست ساخته شد',
         exists (select 1 from pg_indexes
                 where tablename = 'code_checks' and indexname = 'code_checks_uniq')
  union all
  select 'محدودیت وزن مورد آزمون درست است',
         exists (select 1 from pg_constraint where conname = 'code_tests_weight_ck')
  union all
  select 'سقف تعداد و طول پاسخ مرجع درست است',
         exists (select 1 from pg_constraint where conname = 'code_specs_ref_ck')
  union all
  select 'ستون مورد پنهان ساخته شد',
         exists (select 1 from information_schema.columns
                 where table_schema = 'public' and table_name = 'code_tests'
                   and column_name = 'hidden' and data_type = 'boolean')
  union all
  select 'هیچ سیاست نوشتنی برای دانش‌آموز ساخته نشد',
         not exists (select 1 from pg_policies
                     where tablename in ('code_specs','code_tests','code_checks')
                       and cmd <> 'SELECT')
)
select label as "بررسی", ok as "درست" from checks;

-- شمار ردیف‌های فعلی (برای اطلاع؛ صفر بودن طبیعی است)
select
  (select count(*) from public.code_specs)  as "سؤال کد",
  (select count(*) from public.code_tests)  as "مورد آزمون",
  (select count(*) from public.code_checks) as "ردیف چک‌لیست";
