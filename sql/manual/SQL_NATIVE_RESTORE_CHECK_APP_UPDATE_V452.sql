-- تشخیص و بازسازی تابع check_app_update — V45.2
-- این فایل را فقط در SQL Editor پروژه اصلی اجرا کنید:
-- https://eazwuyrymsvdkwckdpco.supabase.co
-- (اجرای دوباره امن است؛ همگی idempotent هستند)

-- ۱) تشخیص: آیا تابع‌های کلیدی در این پروژه وجود دارند؟
--    اگر check_app_update در خروجی نبود، یعنی در همین پروژه گم شده است.
--    اگر هیچ ردیفی برنگشت، پروژه اشتباه باز شده است.
select p.proname as function_name,
       pg_get_function_identity_arguments(p.oid) as arguments
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname in ('check_app_update', 'publish_native_app_release_v1')
order by 1;

-- جدول نسخه برنامه هم باید موجود باشد
select to_regclass('public.app_version') as app_version_table;

-- ۲) بازسازی امن تابع (اگر نبود می‌سازد؛ اگر بود همان را به‌روز می‌کند)
create or replace function public.check_app_update(p_current_version_code integer)
returns table (
    version_code integer,
    version_name text,
    notes_fa jsonb,
    apk_url text,
    apk_sha256 text,
    apk_size_bytes bigint,
    is_required boolean
)
language sql
stable
security invoker
set search_path = public
as $$
    select
        version.version_code::integer,
        version.version_name::text,
        to_jsonb(version.notes_fa),
        version.apk_url::text,
        version.apk_sha256::text,
        version.apk_size_bytes::bigint,
        version.is_required::boolean
    from public.app_version as version
    where version.is_active = true
      and version.version_code > p_current_version_code
    order by version.version_code desc
    limit 1;
$$;

grant execute on function public.check_app_update(integer) to anon, authenticated;

-- ۳) تأیید نهایی
select p.proname as function_name,
       pg_get_function_identity_arguments(p.oid) as arguments
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname = 'check_app_update';
