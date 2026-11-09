-- =====================================================================
-- V357 — بررسی درستی نصب (فقط خواندنی؛ چیزی را تغییر نمی‌دهد)
-- انتظار: همهٔ ردیف‌ها ستون «درست» = true داشته باشند.
-- اگر نصب نشده باشد، ردیف‌ها false می‌شوند و پیام روشن می‌آید — نه خطا.
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
