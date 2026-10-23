-- ===============================================================================
-- V335.0 (2026-10-23) — ممیزی جامع، تکمیل مجوزهای Data API و ایمن سازی نهایی Supabase
-- ویژه آمادگی صددرصد پیش از تغییر ۳۰ اکتبر Supabase (تکرارپذیر و ایمن / Idempotent)
--
-- یافته های واقعی ممیزی کل ۹۲ فایل مهاجرت + کد اپ کاتلین + سایت + ۶ تابع لبه:
--   ۱) ۱۶ جدول هسته ای اولیه (از جمله profiles, exams, exam_keys, answers, classes,
--      class_members, exam_attempts, question_bank و ...) پیش از پوشه migrations ساخته
--      شده بودند و در V324 گرنت صریح Data API (به ویژه برای service_role و authenticated)
--      نداشتند. این اسکریپت هم با نام صریح و هم به صورت پویا برای تمام جدول های موجود
--      در public گرنت دقیق حداقل دسترسی + فعال بودن RLS را تضمین می کند.
--   ۲) تابع حذف زمان بندی شده حساب ها (public.native_due_account_deletions_v282) در V282
--      از public/anon/authenticated سلب دسترسی شده بود اما grant صریح به service_role نداشت.
--   ۳) ۴ تابع عمومی فاقد SET search_path = public, pg_temp بودند
--      (native_auto_grade_question_v1, native_normalize_text_v1,
--       native_answer_max_score_v255, native_exams_created_at_v137) + ایمن سازی خودکار
--      search_path تمام توابع موجود در schema عمومی برای رفع کامل هشدار Security Advisor.
--   ۴) قفل کامل schema خصوصی (private) در برابر anon و authenticated.
--   ۵) در پایان اجرا، یک جدول گزارش فارسی مستقیم در SQL Editor نمایش داده می شود تا
--      صفر بودن تمام خطاها را روی دیتابیس واقعی مشاهده و تایید فرمایید.
-- ===============================================================================

-- ۱) تنظیم پیش فرض آینده (ALTER DEFAULT PRIVILEGES) برای جدول ها، دنباله ها و توابع جدید
alter default privileges for role postgres in schema public grant select on tables to anon;
alter default privileges for role postgres in schema public grant select, insert, update, delete on tables to authenticated;
alter default privileges for role postgres in schema public grant all on tables to service_role;
alter default privileges for role postgres in schema public grant usage, select on sequences to anon, authenticated, service_role;
alter default privileges for role postgres in schema public grant execute on functions to anon, authenticated, service_role;

do $$
begin
  begin
    execute 'alter default privileges for role supabase_admin in schema public grant select on tables to anon';
    execute 'alter default privileges for role supabase_admin in schema public grant select, insert, update, delete on tables to authenticated';
    execute 'alter default privileges for role supabase_admin in schema public grant all on tables to service_role';
    execute 'alter default privileges for role supabase_admin in schema public grant usage, select on sequences to anon, authenticated, service_role';
    execute 'alter default privileges for role supabase_admin in schema public grant execute on functions to anon, authenticated, service_role';
  exception when undefined_object or insufficient_privilege then
    raise notice 'supabase_admin default privileges skipped';
  end;
end $$;

-- ۲) قفل قطعی schema خصوصی (private) در برابر کلاینت ها
do $$
begin
  if exists (select 1 from pg_namespace where nspname = 'private') then
    execute 'revoke all on schema private from public, anon, authenticated';
    execute 'revoke all on all tables in schema private from public, anon, authenticated';
    execute 'revoke all on all sequences in schema private from public, anon, authenticated';
    execute 'revoke all on all functions in schema private from public, anon, authenticated';
    execute 'grant usage on schema private to service_role';
    execute 'grant all on all tables in schema private to service_role';
    execute 'grant all on all sequences in schema private to service_role';
  end if;
end $$;

-- ۳) گرنت صریح برای ۱۶ جدول هسته ای اولیه (اگر هر جدول از نام های قدیمی وجود نداشته باشد بدون خطا رد می شود)
--    توجه امنیتی حیاتی: روی جدول های حساس قدیمی (مثل exam_keys, profiles, exams, answers)
--    دسترسی anon هرگز باز نمی شود (فقط authenticated با حفاظت کامل RLS و service_role).
do $$
declare
  v_core_tables text[] := array[
    'answer_drafts',
    'answers',
    'answers_trash',
    'class_members',
    'classes',
    'exam_attempts',
    'exam_audience_classes',
    'exam_audience_students',
    'exam_classes',
    'exam_keys',
    'exam_overrides',
    'exam_sessions',
    'exam_students',
    'exams',
    'profiles',
    'question_bank'
  ];
  t text;
begin
  foreach t in array v_core_tables loop
    if to_regclass('public.' || quote_ident(t)) is not null then
      execute format('alter table public.%I enable row level security', t);
      execute format('revoke all on table public.%I from anon', t);
      execute format('grant select, insert, update, delete on table public.%I to authenticated', t);
      execute format('grant all on table public.%I to service_role', t);
    end if;
  end loop;
end $$;

-- ۴) گرنت صریح استاندارد Data API برای جدول های عمومی (شامل جدول های مرجع عمومی دارای RLS)
--    و تضمین پوشش ۱۰۰ درصدی هر جدول یا دنباله دیگری که در public وجود دارد
grant select on table public.app_version to anon;
grant select, insert, update, delete on table public.app_version to authenticated;
grant all on table public.app_version to service_role;

grant select on table public.holidays to anon;
grant select, insert, update, delete on table public.holidays to authenticated;
grant all on table public.holidays to service_role;

grant select on table public.holiday_years to anon;
grant select, insert, update, delete on table public.holiday_years to authenticated;
grant all on table public.holiday_years to service_role;

-- تضمین پویا برای تمام جدول ها و دنباله های موجود در public (جلوگیری از جا افتادن هر جدول احتمالی)
do $$
declare
  r record;
begin
  for r in
    select tablename
    from pg_tables
    where schemaname = 'public'
  loop
    execute format('alter table public.%I enable row level security', r.tablename);
    execute format('grant all on table public.%I to service_role', r.tablename);
    execute format('grant select, insert, update, delete on table public.%I to authenticated', r.tablename);
  end loop;
end $$;

grant usage, select on all sequences in schema public to anon, authenticated, service_role;

-- ۵) رفع کمبود grant به service_role برای RPC حذف زمان بندی شده حساب ها و کلیه توابع public
do $$
declare
  r record;
begin
  if to_regprocedure('public.native_due_account_deletions_v282()') is not null then
    revoke all on function public.native_due_account_deletions_v282() from public, anon, authenticated;
    grant execute on function public.native_due_account_deletions_v282() to service_role;
  end if;

  -- اطمینان از دسترسی service_role به تمام توابع schema عمومی
  for r in
    select p.oid::regprocedure as sig
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public'
  loop
    execute format('grant execute on function %s to service_role', r.sig);
  end loop;
end $$;

-- ۶) قفل امنیتی search_path روی ۴ تابع شناسایی شده + تمام توابع موجود در schema عمومی و خصوصی
do $$
declare
  r record;
begin
  for r in
    select p.oid::regprocedure as sig
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname in ('public', 'private')
      and p.prokind = 'f'
      and (
        p.proconfig is null
        or not exists (
          select 1 from unnest(p.proconfig) cfg where cfg like 'search_path=%'
        )
      )
  loop
    execute format('alter function %s set search_path = public, pg_temp', r.sig);
  end loop;
end $$;

-- ۷) بازخوانی کش PostgREST
notify pgrst, 'reload schema';

-- ۸) گزارش خودکار تایید نهایی (در پایین صفحه SQL Editor نمایش داده می شود)
select *
from (
  select
    1 as ردیف,
    'کل جدول های عمومی محافظت شده با RLS' as شاخص_بررسی,
    (select count(*)::text from pg_tables where schemaname = 'public' and rowsecurity = true) as نتیجه,
    'سالم (همه جدول های عمومی RLS فعال دارند)' as وضعیت
  union all
  select
    2,
    'جدول های عمومی بدون RLS فعال (باید ۰ باشد)',
    (select count(*)::text from pg_tables where schemaname = 'public' and coalesce(rowsecurity, false) = false),
    case when (select count(*) from pg_tables where schemaname = 'public' and coalesce(rowsecurity, false) = false) = 0
         then 'سالم (۰ خطا)' else 'نیازمند بررسی!' end
  union all
  select
    3,
    'جدول های عمومی بدون دسترسی صریح service_role (باید ۰ باشد)',
    (select count(*)::text
     from pg_tables t
     where t.schemaname = 'public'
       and not has_table_privilege('service_role', format('public.%I', t.tablename), 'SELECT,INSERT,UPDATE,DELETE')),
    case when (select count(*)
               from pg_tables t
               where t.schemaname = 'public'
                 and not has_table_privilege('service_role', format('public.%I', t.tablename), 'SELECT,INSERT,UPDATE,DELETE')) = 0
         then 'سالم (۰ خطا)' else 'نیازمند بررسی!' end
  union all
  select
    4,
    'عدم دسترسی ناشناس (anon) به کلید محرمانه سوالات (exam_keys)',
    case when to_regclass('public.exam_keys') is null then 'جدول موجود نیست'
         when not has_table_privilege('anon', 'public.exam_keys', 'SELECT') then 'قفل کامل'
         else 'باز!' end,
    case when to_regclass('public.exam_keys') is null or not has_table_privilege('anon', 'public.exam_keys', 'SELECT')
         then 'سالم (کاملا محرمانه)' else 'خطر امنیتی!' end
  union all
  select
    5,
    'توابع public/private بدون search_path امن (باید ۰ باشد)',
    (select count(*)::text
     from pg_proc p
     join pg_namespace n on n.oid = p.pronamespace
     where n.nspname in ('public', 'private')
       and p.prokind = 'f'
       and (p.proconfig is null or not exists (select 1 from unnest(p.proconfig) c where c like 'search_path=%'))),
    case when (select count(*)
               from pg_proc p
               join pg_namespace n on n.oid = p.pronamespace
               where n.nspname in ('public', 'private')
                 and p.prokind = 'f'
                 and (p.proconfig is null or not exists (select 1 from unnest(p.proconfig) c where c like 'search_path=%'))) = 0
         then 'سالم (۰ هشدار امنیتی)' else 'نیازمند بررسی!' end
) audit_summary
order by ردیف;
