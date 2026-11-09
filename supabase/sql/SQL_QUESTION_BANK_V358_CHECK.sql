-- =====================================================================
-- V358 — بررسی درستی نصب (فقط خواندنی؛ چیزی را تغییر نمی‌دهد)
-- انتظار: همهٔ ردیف‌ها ستون «درست» = true داشته باشند.
-- اگر نصب نشده باشد، ردیف‌ها false می‌شوند و پیام روشن می‌آید — نه خطا.
-- =====================================================================

with checks(label, ok) as (
  select '>>> نصب V358 انجام شده است <<<',
         exists (select 1 from information_schema.columns
                  where table_schema='public' and table_name='bank_categories'
                    and column_name='parent_id')
         and exists (select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
                      where n.nspname='public' and p.proname='native_bank_tree_v358')
  union all
  select 'ستون والد ساخته شد',
         exists (select 1 from information_schema.columns
                  where table_schema='public' and table_name='bank_categories'
                    and column_name='parent_id')
  union all
  select 'کلید خارجی والد ساخته شد',
         exists (select 1 from pg_constraint where conname='bank_categories_parent_fk')
  union all
  select 'حذف والد، زیردسته را می‌برد',
         exists (select 1 from pg_constraint c
                  where c.conname='bank_categories_parent_fk' and c.confdeltype='c')
  union all
  select 'ایندکس یکتایی هر سطح ساخته شد',
         to_regclass('public.uq_bank_categories_parent_name') is not null
  union all
  select 'ایندکس سراسری پیشین برداشته شد',
         to_regclass('public.uq_bank_categories_teacher_name') is null
  union all
  select 'ایندکس والد ساخته شد',
         to_regclass('public.ix_bank_categories_parent') is not null
  union all
  select 'تابع نگهبان درخت نصب شد',
         to_regprocedure('public.native_bank_tree_guard_v358()') is not null
  union all
  select 'تریگر نگهبان درخت نصب شد',
         exists (select 1 from pg_trigger where tgname='native_bank_tree_guard_v358' and not tgisinternal)
  union all
  select 'تابع افزودن دسته با والد نصب شد',
         to_regprocedure('public.native_bank_category_add_v358(text,bigint)') is not null
  union all
  select 'تابع جابه‌جایی دسته نصب شد',
         to_regprocedure('public.native_bank_category_move_v358(bigint,bigint)') is not null
  union all
  select 'تابع درخت با شمارش زیردرخت نصب شد',
         to_regprocedure('public.native_bank_tree_v358()') is not null
  union all
  select 'snapshot هنوز همان امضا را دارد',
         to_regprocedure('public.native_bank_snapshot_v1()') is not null
  union all
  select 'تابع افزودن قدیمی سر جایش ماند',
         to_regprocedure('public.native_bank_category_add_v1(text)') is not null
  union all
  select 'دسترسی اجرا به دستهٔ تازه داده شد',
         has_function_privilege('authenticated','public.native_bank_category_add_v358(text,bigint)','EXECUTE')
  union all
  select 'دسترسی اجرا به جابه‌جایی داده شد',
         has_function_privilege('authenticated','public.native_bank_category_move_v358(bigint,bigint)','EXECUTE')
  union all
  select 'دسترسی اجرا به درخت داده شد',
         has_function_privilege('authenticated','public.native_bank_tree_v358()','EXECUTE')
  union all
  select 'RLS دسته‌ها هنوز روشن است',
         exists (select 1 from pg_class where relname='bank_categories' and relrowsecurity)
  union all
  select 'هیچ سیاست نوشتنی برای دانش‌آموز ساخته نشد',
         not exists (select 1 from pg_policies
                      where schemaname='public' and tablename='bank_categories'
                        and cmd in ('INSERT','UPDATE','DELETE'))
  union all
  select 'جدول سؤال‌ها دست‌نخورده ماند',
         to_regclass('public.question_bank') is not null
         and to_regclass('public.bank_question_cats') is not null
)
select label as "بررسی", ok as "درست" from checks;

-- خلاصهٔ شمارش: اگر نصب نشده باشد، به‌جای خطای ۴۲P01 پیام روشن می‌دهد.
do $$
declare v_n bigint;
begin
  if not exists (select 1 from information_schema.columns
                  where table_schema='public' and table_name='bank_categories'
                    and column_name='parent_id') then
    raise notice 'هنوز نصب نشده است. ابتدا فایل نصب را اجرا کنید.';
    return;
  end if;
  execute 'select count(*) from public.bank_categories' into v_n;
  raise notice 'دستهٔ بانک: %', v_n;
  execute 'select count(*) from public.bank_categories where parent_id is not null' into v_n;
  raise notice 'دستهٔ زیرمجموعه: %', v_n;
  execute 'select count(*) from public.question_bank' into v_n;
  raise notice 'سؤال بانک (دست‌نخورده): %', v_n;
end $$;
