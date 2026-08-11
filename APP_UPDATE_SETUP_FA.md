# راه‌اندازی بخش «درباره و بروزرسانی»

این قابلیت شامل منوی همبرگری، بررسی نسخه فعال در Supabase، دانلود با DownloadManager، کنترل امنیتی APK و بازکردن نصب‌کننده Android است.

## وضعیت فعلی

```text
SQL پایه app_version و bucket app-updates: اجراشده
نسخه‌گذاری GitHub Actions: خودکار
ساخت APK و metadata: خودکار
آپلود APK در Supabase Storage: خودکار در پچ V4
فعال‌سازی ردیف app_version: خودکار و تراکنشی در پچ V4
```

## ۱) SQL پایه اجراشده

فایل پایه زیر قبلاً در پروژه اصلی اجرا شده است:

```text
supabase/migrations/20260811_app_updates.sql
```

پروژه صحیح:

```text
https://eazwuyrymsvdkwckdpco.supabase.co
```

## ۲) اجرای SQL انتشار خودکار فقط یک‌بار

پیش از اولین Build پچ V4، فایل زیر را در SQL Editor همان پروژه اجرا کنید:

```text
supabase/migrations/20260811_app_update_auto_publish.sql
```

این SQL تابع مدیریتی `publish_app_update` را می‌سازد. تابع در یک تراکنش نسخه جدید را ثبت، نسخه قبلی را غیرفعال و نسخه جدید را فعال می‌کند. اجرای آن برای کاربران عادی، anon و authenticated بسته است.

## ۳) افزودن یک Secret برای انتشار — فقط یک‌بار

در Supabase پروژه اصلی، کلید سروری انتشار را از بخش API Keys بردارید. آن را هرگز در گفتگو، فایل پروژه، APK یا commit قرار ندهید.

در GitHub repository وارد مسیر زیر شوید:

```text
Settings → Secrets and variables → Actions → Secrets
```

روی `New repository secret` بزنید و وارد کنید:

```text
Name:  SUPABASE_RELEASE_KEY
Value: کلید سروری پروژه اصلی Supabase
```

این کار فقط یک‌بار انجام می‌شود. مقدار Secret را در هیچ پیام یا تصویر قابل مشاهده ارسال نکنید.

Variableهای قدیمی زیر دیگر استفاده نمی‌شوند و می‌توانند باقی بمانند یا حذف شوند:

```text
APP_VERSION_CODE
APP_VERSION_NAME
```

## ۴) نسخه‌گذاری خودکار

در هر اجرای GitHub Actions مقادیر به‌صورت خودکار ساخته می‌شوند:

```text
versionCode = تعداد ثانیه‌های گذشته از 2020-01-01 UTC
versionName = تاریخ و ساعت UTC ساخت + native
```

نمونه:

```text
APP_VERSION_CODE=208585452
APP_VERSION_NAME=2026.08.11.042412-native
```

برای Build محلی خارج از CI، fallback فعلی `3` و `1.1.1-native` است.

## ۵) جریان کامل انتشار خودکار V4

پس از هر push موفق به شاخه main:

```text
تولید نسخه یکتا
→ اجرای تست‌ها
→ ساخت APK Release با keystore اصلی
→ محاسبه SHA-256 و اندازه
→ ذخیره Artifact و update-metadata.txt
→ آپلود APK در bucket عمومی app-updates
→ بررسی دانلود عمومی APK
→ فراخوانی امن publish_app_update
→ غیرفعال‌کردن نسخه قبلی و فعال‌کردن نسخه جدید
```

Workflowها به‌صورت صف اجرا می‌شوند تا دو انتشار هم‌زمان ترتیب نسخه‌ها را خراب نکنند.

## ۶) Artifact قابل نگهداری

Artifact خروجی نامی شبیه مورد زیر دارد:

```text
exam-app-release-run-25
```

داخل آن فایل‌های زیر قرار دارند:

```text
exam-app-<versionName>-<versionCode>.apk
update-metadata.txt
```

`update-metadata.txt` شامل موارد زیر است:

```text
APP_VERSION_CODE
APP_VERSION_NAME
APK_FILE
APK_SHA256
APK_SIZE_BYTES
```

حتی اگر مرحله انتشار Supabase خطا بدهد، Artifact پیش از آن ساخته و بارگذاری می‌شود تا فایل Release از دست نرود.

## ۷) کنترل نتیجه انتشار

در GitHub Actions این پیام‌ها باید دیده شوند:

```text
Supabase Storage upload status: 200 یا 201
Public APK verification: OK
Supabase release activation status: 200 یا 201
Automatic app update publication: SUCCESS
```

در Supabase نیز باید دقیقاً یک ردیف فعال وجود داشته باشد:

```sql
select
    version_code,
    version_name,
    apk_url,
    apk_sha256,
    apk_size_bytes,
    is_active,
    published_at
from public.app_version
order by version_code desc;
```

## ۸) رفتار خطا و امنیت

- اگر Upload ناموفق باشد، نسخه فعال قبلی تغییر نمی‌کند.
- اگر فعال‌سازی دیتابیس ناموفق باشد، تراکنش rollback می‌شود و نسخه قبلی فعال می‌ماند.
- فایل آپلودشده اضافی ممکن است باقی بماند، اما تا زمان ثبت موفق در جدول به کاربران پیشنهاد نمی‌شود.
- Secret انتشار فقط در GitHub Actions مصرف می‌شود و وارد APK یا Artifact نمی‌شود.
- workflow فقط status امن HTTP را چاپ می‌کند و پاسخ حاوی اطلاعات حساس را نمایش نمی‌دهد.
- URL فقط باید متعلق به bucket `app-updates` پروژه اصلی باشد.
- SHA-256 باید دقیقاً ۶۴ کاراکتر hex و اندازه APK مثبت باشد.

## نکات قطعی Android

- Android اجازه نصب کاملاً بی‌صدا را به برنامه عادی نمی‌دهد؛ کاربر باید صفحه نصب سیستم را تأیید کند.
- بار اول، Android اجازه «Install unknown apps» را برای سامانه آزمون درخواست می‌کند.
- APK جایگزین باید package یکسان، امضای یکسان و `versionCode` بالاتر داشته باشد.
- نسخه Debug با شناسه `ir.exam.app.native` کنار نسخه اصلی نصب می‌شود؛ نسخه Release شناسه `ir.exam.app` دارد.
- برنامه URL غیر HTTPS، APK با package اشتباه، versionCode ناسازگار، امضای متفاوت، اندازه متفاوت یا SHA-256 نامعتبر را نصب نمی‌کند.
