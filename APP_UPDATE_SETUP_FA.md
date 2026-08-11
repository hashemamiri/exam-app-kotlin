# راه‌اندازی بخش «درباره و بروزرسانی»

این قابلیت شامل منوی همبرگری، بررسی نسخه فعال در Supabase، دانلود با DownloadManager، کنترل امنیتی APK و بازکردن نصب‌کننده Android است.

## وضعیت فعلی

```text
SQL پایه app_version و bucket app-updates: اجراشده
نسخه‌گذاری GitHub Actions: خودکار
ساخت APK و metadata: خودکار
آپلود APK در Supabase Storage: خودکار و در اجرای واقعی HTTP 200
بررسی URL عمومی: در اجرای واقعی OK
فعال‌سازی اولیه app_version: HTTP 409
Hotfix V4.1 در دیتابیس تأیید شد: delete/insert=true، on conflict=false
نتیجه واقعی V4.1: HTTP 400 و SQLSTATE 21000
وضعیت جدول هنگام خطا: 1 ردیف، 0 ردیف فعال، 0 trigger کاربری
Hotfix V4.2 در دیتابیس فعال شد: jsonb، بدون RETURNING/SELECT MAX
فراخوانی مستقیم SQL تابع V4.2: موفق و rollback‌شده (P0001 تشخیصی)
مسیر REST نام قدیمی همچنان SQLSTATE 21000: metadata قدیمی PostgREST
Hotfix V4.3 با نام RPC کاملاً جدید: آماده اجرا
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

این SQL تابع مدیریتی `publish_app_update` را می‌سازد. اجرای آن برای کاربران عادی، anon و authenticated بسته است.

### Hotfix V4.2 پس از SQLSTATE 21000

Query تشخیصی تأیید کرد که V4.1 واقعاً فعال بوده، منطق قدیمی `ON CONFLICT` وجود نداشته، جدول فقط یک ردیف و هیچ trigger کاربری نداشته است. بنابراین مسیر composite شامل `RETURNING ... INTO` و خروجی `public.app_version` حذف شد.

فایل زیر را یک‌بار در SQL Editor اجرا کنید:

```text
supabase/migrations/20260811_app_update_publish_v42_hotfix.sql
```

V4.2 تابع قبلی را با همان ورودی‌ها حذف و با خروجی ساده `jsonb` می‌سازد. این تابع:

```text
هیچ SELECT ... INTO ندارد
هیچ RETURNING ... INTO ندارد
از advisory transaction lock استفاده می‌کند
ردیف نسخه جاری را تراکنشی جایگزین می‌کند
فقط JSON شامل ok، version_code و version_name برمی‌گرداند
```

اگر درج جدید شکست بخورد، تراکنش rollback می‌شود و ردیف قبلی باقی می‌ماند. پس از اجرای SQL، Push پچ جدید یک workflow تازه اجرا می‌کند.

workflow در صورت خطا، `code/message/details/hint` را پس از حذف URL، Token، کلید و Header و با محدودیت طول چاپ می‌کند؛ پاسخ خام هرگز نمایش داده نمی‌شود.

### Hotfix V4.3 برای metadata قدیمی PostgREST

تشخیص فشرده نتیجه قطعی زیر را داد:

```text
function_result_type=jsonb
publish_function_overload_count=1
safe_sqlstate=P0001
safe_message=DIAGNOSTIC_CALL_SUCCEEDED_AND_WAS_ROLLED_BACK
```

یعنی خود تابع در PostgreSQL کاملاً موفق است و خطای `21000` فقط در مسیر REST نام قدیمی رخ می‌دهد. برای حذف کامل metadata کش‌شده، V4.3 یک تابع مستقل با نام جدید می‌سازد:

```text
publish_native_app_release_v1
```

فایل زیر را یک‌بار در SQL Editor اجرا کنید:

```text
supabase/migrations/20260811_app_update_publish_v43_new_rpc.sql
```

workflow نیز از این پس فقط endpoint جدید زیر را صدا می‌زند:

```text
/rest/v1/rpc/publish_native_app_release_v1
```

V4.3 نوع کلید انتشار را نیز بدون چاپ مقدار آن تشخیص می‌دهد:

```text
sb_secret_*       → فقط header امن apikey
کلید JWT قدیمی    → apikey + Authorization Bearer
```

کلیدهای opaque جدید نباید به‌عنوان Bearer JWT ارسال شوند. تابع قدیمی حذف نمی‌شود، اما دیگر توسط workflow استفاده نخواهد شد.

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
→ فراخوانی امن publish_native_app_release_v1
→ جایگزینی تراکنشی ردیف نسخه جاری با نسخه جدید
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
Release RPC endpoint: publish_native_app_release_v1
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
