# راه‌اندازی بخش «درباره و بروزرسانی»

این قابلیت شامل منوی همبرگری، نمایش نسخه، بررسی نسخه فعال در Supabase، دانلود با DownloadManager، نمایش پیشرفت و بازکردن نصب‌کننده Android است.

## ۱) اجرای SQL فقط یک‌بار

فایل زیر را در SQL Editor پروژه اصلی Supabase اجرا کنید:

```text
supabase/migrations/20260811_app_updates.sql
```

پروژه صحیح:

```text
https://eazwuyrymsvdkwckdpco.supabase.co
```

این SQL جدول `app_version`، RLS فقط‌خواندنی، تابع `check_app_update` و bucket عمومی `app-updates` را آماده می‌کند. این SQL طبق اعلام کاربر اجرا شده است.

## ۲) نسخه‌گذاری کاملاً خودکار GitHub Actions

از پچ V3 به بعد دیگر ساخت یا ویرایش این Variableها لازم نیست:

```text
APP_VERSION_CODE
APP_VERSION_NAME
```

اگر قبلاً آن‌ها را ساخته‌اید، می‌توانید نگه دارید یا حذف کنید؛ workflow دیگر آن‌ها را نمی‌خواند.

در هر اجرای جدید GitHub Actions مقادیر به‌صورت خودکار ساخته می‌شوند:

```text
versionCode = تعداد ثانیه‌های گذشته از 2020-01-01 UTC
versionName = تاریخ و ساعت UTC ساخت + native
```

نمونه:

```text
APP_VERSION_CODE=208843215
APP_VERSION_NAME=2026.08.11.041335-native
```

بنابراین هر Build جدید، بدون ورود دستی مقدار، کد نسخه بالاتری می‌گیرد. اجرای دوباره workflow نیز نسخه جدید خودکار می‌سازد.

برای Build محلی که داخل GitHub Actions نیست، fallback فعلی `3` و `1.1.1-native` باقی می‌ماند.

اگر جدول `app_version` هنوز ردیف فعال نداشته باشد، برنامه پیام «برنامه شما به‌روز است» نشان می‌دهد؛ خالی‌بودن جدول خطا محسوب نمی‌شود.

## ۳) ساخت Release

پس از هر push، workflow زیر APK امضاشده را می‌سازد:

```text
.github/workflows/android.yml
```

Artifact خروجی نامی شبیه مورد زیر دارد:

```text
exam-app-release-run-25
```

داخل Artifact دو فایل قرار می‌گیرد:

```text
exam-app-<versionName>-<versionCode>.apk
update-metadata.txt
```

فایل `update-metadata.txt` به‌صورت خودکار شامل این اطلاعات واقعی است:

```text
APP_VERSION_CODE
APP_VERSION_NAME
APK_FILE
APK_SHA256
APK_SIZE_BYTES
```

فقط Artifact مربوط به اجرای کاملاً موفق را استفاده کنید. Release باید با همان `release.keystore` اصلی امضا شده باشد.

## ۴) آپلود APK

در Supabase Dashboard وارد Storage و bucket زیر شوید:

```text
app-updates
```

همان APK داخل Artifact را بدون تغییر نام آپلود کنید. نشانی مستقیم به شکل زیر خواهد بود:

```text
https://eazwuyrymsvdkwckdpco.supabase.co/storage/v1/object/public/app-updates/<APK_FILE>
```

## ۵) دریافت SHA-256 و اندازه

دیگر محاسبه دستی لازم نیست. مقادیر دقیق را از فایل زیر بردارید:

```text
update-metadata.txt
```

برای کنترل اختیاری فایل دانلودشده در WSL نیز می‌توانید اجرا کنید:

```bash
APK=/mnt/c/Users/Hashem/Downloads/نام-واقعی-فایل.apk && \
sha256sum "$APK" && \
stat -c 'APK_SIZE_BYTES=%s' "$APK"
```

## ۶) فعال‌کردن نسخه جدید

در SQL زیر مقادیر را دقیقاً از `update-metadata.txt` و نام فایل آپلودشده جایگزین کنید:

```sql
begin;

update public.app_version
set is_active = false
where is_active;

insert into public.app_version (
    version_code,
    version_name,
    notes_fa,
    apk_url,
    apk_sha256,
    apk_size_bytes,
    is_required,
    is_active
) values (
    APP_VERSION_CODE_REAL,
    'APP_VERSION_NAME_REAL',
    '["بهبود رابط برنامه", "نسخه جدید سامانه آزمون"]'::jsonb,
    'https://eazwuyrymsvdkwckdpco.supabase.co/storage/v1/object/public/app-updates/APK_FILE_REAL',
    'APK_SHA256_REAL',
    APK_SIZE_BYTES_REAL,
    false,
    true
);

commit;
```

اطلاعات جدول باید دقیقاً با خود APK یکسان باشند؛ در غیر این صورت برنامه برای امنیت نصب را متوقف می‌کند.

آپلود APK و فعال‌کردن ردیف انتشار فعلاً عمداً مدیریتی است. کلید مدیریتی نباید داخل APK، Git یا متن گفتگو قرار بگیرد.

## نکات قطعی Android

- Android اجازه نصب کاملاً بی‌صدا را به برنامه عادی نمی‌دهد؛ کاربر باید صفحه نصب سیستم را تأیید کند.
- بار اول، Android اجازه «Install unknown apps» را برای سامانه آزمون درخواست می‌کند.
- APK جایگزین باید package یکسان، امضای یکسان و `versionCode` بالاتر داشته باشد.
- نسخه Debug با شناسه `ir.exam.app.native` کنار نسخه اصلی نصب می‌شود؛ نسخه Release شناسه `ir.exam.app` دارد.
- برنامه URL غیر HTTPS، APK با package اشتباه، versionCode ناسازگار، امضای متفاوت، اندازه متفاوت یا SHA-256 نامعتبر را نصب نمی‌کند.
