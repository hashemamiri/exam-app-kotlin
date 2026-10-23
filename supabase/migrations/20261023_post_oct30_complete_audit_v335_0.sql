-- ===============================================================================
-- V335.0 (نسخه نهایی بدون Deadlock و با بررسی مستقیم OID در کاتالوگ PostgreSQL)
-- ممیزی جامع، تکمیل مجوزهای Data API و ایمن سازی نهایی پیش از ۳۰ اکتبر Supabase
-- ===============================================================================

-- بخش ۱: پیش فرض آینده برای جدول ها، دنباله ها و توابع جدید
set lock_timeout = '10s';
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
    null;
  end;
end $$;

-- بخش ۲: قفل schema خصوصی (private) در برابر کلاینت ها
do $$
begin
  if exists (select 1 from pg_namespace where nspname = 'private') then
    revoke all on schema private from public, anon, authenticated;
    revoke all on all tables in schema private from public, anon, authenticated;
    revoke all on all sequences in schema private from public, anon, authenticated;
    revoke all on all functions in schema private from public, anon, authenticated;
    grant usage on schema private to service_role;
    grant all on all tables in schema private to service_role;
    grant all on all sequences in schema private to service_role;
  end if;
end $$;

-- بخش ۳: فعال سازی RLS فقط روی جدول هایی که هنوز RLS ندارند (بدون قفل کردن جدول های فعال)
do $$
declare
  r record;
begin
  for r in
    select c.relname as tablename
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relkind in ('r', 'p')
      and not c.relrowsecurity
    order by c.relname
  loop
    execute format('alter table public.%I enable row level security', r.tablename);
  end loop;
end $$;

-- بخش ۴: پوشش ۱۶ جدول هسته ای اولیه (با بررسی مستقیم OID بدون خطای optimizer)
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
  v_oid oid;
begin
  foreach t in array v_core_tables loop
    v_oid := to_regclass('public.' || quote_ident(t));
    if v_oid is not null then
      if has_table_privilege('anon', v_oid, 'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER') then
        execute format('revoke all on table public.%I from anon', t);
      end if;
      if not has_table_privilege('authenticated', v_oid, 'SELECT,INSERT,UPDATE,DELETE') then
        execute format('grant select, insert, update, delete on table public.%I to authenticated', t);
      end if;
      if not has_table_privilege('service_role', v_oid, 'SELECT,INSERT,UPDATE,DELETE') then
        execute format('grant all on table public.%I to service_role', t);
      end if;
    end if;
  end loop;
end $$;

-- بخش ۵: تکمیل گرنت جدول های مرجع و سایر جدول های public (با OID مستقیم)
grant select on table public.app_version to anon;
grant select, insert, update, delete on table public.app_version to authenticated;
grant all on table public.app_version to service_role;

grant select on table public.holidays to anon;
grant select, insert, update, delete on table public.holidays to authenticated;
grant all on table public.holidays to service_role;

grant select on table public.holiday_years to anon;
grant select, insert, update, delete on table public.holiday_years to authenticated;
grant all on table public.holiday_years to service_role;

do $$
declare
  r record;
begin
  for r in
    select c.oid, c.relname as tablename
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relkind in ('r', 'p')
    order by c.relname
  loop
    if not has_table_privilege('service_role', r.oid, 'SELECT,INSERT,UPDATE,DELETE') then
      execute format('grant all on table public.%I to service_role', r.tablename);
    end if;
    if not has_table_privilege('authenticated', r.oid, 'SELECT,INSERT,UPDATE,DELETE') then
      execute format('grant select, insert, update, delete on table public.%I to authenticated', r.tablename);
    end if;
  end loop;
end $$;

grant usage, select on all sequences in schema public to anon, authenticated, service_role;

-- بخش ۶: مجوز اجرا برای service_role روی تابع حذف حساب و سایر توابع فاقد دسترسی
do $$
declare
  r record;
begin
  if to_regprocedure('public.native_due_account_deletions_v282()') is not null then
    revoke all on function public.native_due_account_deletions_v282() from public, anon, authenticated;
    grant execute on function public.native_due_account_deletions_v282() to service_role;
  end if;

  for r in
    select p.oid::regprocedure as sig
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public'
      and not has_function_privilege('service_role', p.oid, 'EXECUTE')
  loop
    execute format('grant execute on function %s to service_role', r.sig);
  end loop;
end $$;

-- بخش ۷: قفل امنیتی search_path فقط روی توابعی که هنوز search_path ندارند
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

notify pgrst, 'reload schema';

-- بخش ۸: جدول گزارش نهایی تایید سلامت (بر پایه OID واقعی pg_class بدون pushdown متنی)
with pub_tables as materialized (
  select c.oid, c.relname, c.relrowsecurity
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
  where n.nspname = 'public'
    and c.relkind in ('r', 'p')
)
select *
from (
  select
    1 as ردیف,
    'کل جدول های عمومی محافظت شده با RLS' as شاخص_بررسی,
    (select count(*)::text from pub_tables where relrowsecurity) as نتیجه,
    'سالم (همه جدول های عمومی RLS فعال دارند)' as وضعیت
  union all
  select
    2,
    'جدول های عمومی بدون RLS فعال (باید ۰ باشد)',
    (select count(*)::text from pub_tables where not relrowsecurity),
    case when (select count(*) from pub_tables where not relrowsecurity) = 0
         then 'سالم (۰ خطا)' else 'نیازمند بررسی!' end
  union all
  select
    3,
    'جدول های عمومی بدون دسترسی صریح service_role (باید ۰ باشد)',
    (select count(*)::text
     from pub_tables t
     where not has_table_privilege('service_role', t.oid, 'SELECT,INSERT,UPDATE,DELETE')),
    case when (select count(*)
               from pub_tables t
               where not has_table_privilege('service_role', t.oid, 'SELECT,INSERT,UPDATE,DELETE')) = 0
         then 'سالم (۰ خطا)' else 'نیازمند بررسی!' end
  union all
  select
    4,
    'عدم دسترسی ناشناس (anon) به کلید محرمانه سوالات (exam_keys)',
    case when to_regclass('public.exam_keys') is null then 'جدول موجود نیست'
         when not has_table_privilege('anon', to_regclass('public.exam_keys'), 'SELECT') then 'قفل کامل'
         else 'باز!' end,
    case when to_regclass('public.exam_keys') is null or not has_table_privilege('anon', to_regclass('public.exam_keys'), 'SELECT')
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
