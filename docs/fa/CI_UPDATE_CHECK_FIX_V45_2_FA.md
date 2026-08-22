# رفع خطای CI «Supabase public update RPC status: 404» — V45.2

**آخرین بهروزرسانی:** ۲۰۲۶-۰۸-۲۲

## گزارش واقعی

```text
Run STATUS=$(curl -sS -o /dev/null -w "%{http_code}" ...
Supabase public update RPC status: 404
Error: Process completed with exit code 1.
```

مرحله «بررسی امن اتصال عمومی Supabase» در GitHub Actions با 404 متوقف شد و
بنابراین APK ساخته و منتشر نمیشود.

## علت

- این مرحله از پچ V11 به بعد، بهجای بررسی جدول، RPC بهنام `check_app_update`
  را صدا میزند (`POST /rest/v1/rpc/check_app_update`).
- پاسخ 404 یعنی تابع در پروژهای که `SUPABASE_URL` به آن اشاره میکند وجود
  ندارد (یا پروژه اشتباه است).
- نکته کلیدی: **برنامه Kotlin از این RPC استفاده نمیکند**. برنامه مستقیماً
  جدول `app_version` را با کلید anon و RLS میخواند
  (`SupabaseAppUpdateRepository` / `PublicUpdateSupabaseProvider`). تابع
  `check_app_update` فقط «سازگار برای کلاینتهای دیگر» است (طبق کامنت خود
  migration) و نباید CI را بلاک کند.

## تغییرات

### ۱) workflow — `.github/workflows/android.yml`

- بررسی اتصال عمومی حالا دقیقاً مسیر واقعی برنامه را تست میکند:
  `GET /rest/v1/app_version?select=version_code&is_active=eq.true&limit=1`
  با کلید anon. اگر 200 بدهد، بررسی نسخه داخل برنامه هم کار میکند.
- RPC جانبی `check_app_update` همچنان صدا زده میشود ولی فقط بهصورت
  اطلاعاتی چاپ میشود (`check_app_update RPC status (informational)`) و
  دیگر build را متوقف نمیکند.

### ۲) SQL دستی — `sql/manual/SQL_NATIVE_RESTORE_CHECK_APP_UPDATE_V452.sql`

- تشخیص: نمایش پروژه فعلی، وجود `check_app_update` و `publish_native_app_release_v1`
  و جدول `app_version`.
- بازسازی امن و idempotent تابع `check_app_update(integer)` (همان تعریف
  migration اصلی) با grant به anon/authenticated — برای کلاینتهای دیگر.

### ۳) اسکریپت verify و تست

- `scripts/verify_native_final.py`: دو require جدید — بررسی CI باید مسیر
  `app_version` را تست کند و نباید با RPC جانبی بلاک شود.
- تست رگرسیون `V45_2CiUpdateCheckFixTest` اضافه شد.

## عملیات برای کاربر

۱) فایل `SQL_NATIVE_RESTORE_CHECK_APP_UPDATE_V452.sql` را در **SQL Editor
پروژه اصلی** (`https://eazwuyrymsvdkwckdpco.supabase.co`) اجرا کن و خروجی
تشخیصی را ببین:

- اگر در خروجی بخش ۱ هیچ ردیفی برای `check_app_update` نبود ولی
  `app_version` موجود بود → تابع در پروژه اصلی گم شده بود؛ همین فایل آن را
  بازسازی میکند و CI بعدی میگذرد.
- اگر `app_version_table` هم `null` برگرداند → URL یا پروژه درست نیست؛
  `SUPABASE_URL` در GitHub Secrets را با پروژه اصلی مقایسه کن.

۲) پچ V45.2 را اعمال، commit و push کن (دستورها پایین).

۳) دوباره GitHub Actions را اجرا کن؛ باید از مرحله بررسی عبور کند و APK جدید
ساخته و در `app-updates` منتشر شود.

## تست

```text
FINAL_NATIVE_VERIFY                      → PASS
V45_2CiUpdateCheckFixTest                → اضافه شد
git diff --check                         → PASS
testDebugUnitTest / lintDebug            → باید در WSL/GitHub Actions اجرا شود
```

## عملیات

```text
SQL جدید: sql/manual/SQL_NATIVE_RESTORE_CHECK_APP_UPDATE_V452.sql (اجرای دستی)
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیشنیاز: V45 (یا V45.1)
```

راهنمای V45.1: `docs/fa/UPDATE_DOWNLOAD_FIX_V45_1_FA.md`.
