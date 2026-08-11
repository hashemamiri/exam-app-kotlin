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

این SQL جدول `app_version`، RLS فقط‌خواندنی، تابع `check_app_update` و bucket عمومی `app-updates` را آماده می‌کند. آپلود در bucket عمومی نیست و فقط از Dashboard یا محیط مدیریتی انجام می‌شود.

## ۲) تعیین نسخه Build در GitHub

در repository به مسیر زیر بروید:

```text
Settings → Secrets and variables → Actions → Variables
```

دو Variable بسازید:

```text
APP_VERSION_CODE
APP_VERSION_NAME
```

نمونه:

```text
APP_VERSION_CODE=20
APP_VERSION_NAME=2.0.0
```

`APP_VERSION_CODE` باید از نسخه نصب‌شده بیشتر باشد. اگر Variable ساخته نشود، مقدارهای پیش‌فرض پچ V2 برابر `3` و `1.1.1-native` هستند.

اگر جدول `app_version` هنوز ردیف فعال نداشته باشد، برنامه پیام «برنامه شما به‌روز است» نشان می‌دهد؛ خالی‌بودن جدول خطا محسوب نمی‌شود.

برای دیدن versionCode برنامه نصب‌شده، در صورت فعال بودن adb:

```bash
adb shell dumpsys package ir.exam.app | grep -m1 versionCode
```

## ۳) ساخت Release

پس از push، workflow زیر APK امضاشده را می‌سازد:

```text
.github/workflows/android.yml
```

فقط Artifact مربوط به اجرای کاملاً موفق را دانلود کنید. Release باید با همان `release.keystore` اصلی امضا شده باشد.

## ۴) آپلود APK

در Supabase Dashboard وارد Storage و bucket زیر شوید:

```text
app-updates
```

APK را با نام انگلیسی و بدون فاصله آپلود کنید، برای مثال:

```text
exam-app-2.0.0.apk
```

نشانی مستقیم آن به شکل زیر است:

```text
https://eazwuyrymsvdkwckdpco.supabase.co/storage/v1/object/public/app-updates/exam-app-2.0.0.apk
```

## ۵) محاسبه SHA-256 و اندازه

اگر APK در Downloads ویندوز قرار دارد:

```bash
APK=/mnt/c/Users/Hashem/Downloads/app-release.apk && \
sha256sum "$APK" && \
stat -c 'SIZE_BYTES=%s' "$APK"
```

## ۶) فعال‌کردن نسخه جدید

در SQL زیر همه مقدارهای نمونه را با اطلاعات واقعی APK عوض کنید:

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
    20,
    '2.0.0',
    '["بهبود رابط برنامه", "افزودن بروزرسانی مستقیم"]'::jsonb,
    'https://eazwuyrymsvdkwckdpco.supabase.co/storage/v1/object/public/app-updates/exam-app-2.0.0.apk',
    'SHA256_REAL_64_HEX',
    12345678,
    false,
    true
);

commit;
```

این اطلاعات باید دقیقاً با خود APK یکسان باشند؛ در غیر این صورت برنامه برای امنیت نصب را متوقف می‌کند.

## نکات قطعی Android

- Android اجازه نصب کاملاً بی‌صدا را به برنامه عادی نمی‌دهد؛ کاربر باید صفحه نصب سیستم را تأیید کند.
- بار اول، Android اجازه «Install unknown apps» را برای سامانه آزمون درخواست می‌کند.
- APK جایگزین باید package یکسان، امضای یکسان و `versionCode` بالاتر داشته باشد.
- نسخه Debug با شناسه `ir.exam.app.native` کنار نسخه اصلی نصب می‌شود؛ نسخه Release شناسه `ir.exam.app` دارد.
- برنامه URL غیر HTTPS، APK با package اشتباه، versionCode ناسازگار، امضای متفاوت، اندازه متفاوت یا SHA-256 نامعتبر را نصب نمی‌کند.
