# پچ جامع V10 — آفلاین پایدار، پشتیبان، چاپ رسمی، فرمول و تحلیل

## قابلیت‌های Native تکمیل‌شده

### WorkManager و صف آفلاین واقعی
- جدول محلی Room به نام `pending_actions` با dedupe، مالک حساب، state، attempts و خطای امن
- مهاجرت بدون‌تخریب Room از نسخه ۱ به ۲
- ذخیرهٔ پاسخ نهایی و تصاویر در فضای خصوصی برنامه پیش از صف‌شدن
- Constraints شبکه، unique work و backoff نمایی WorkManager
- ارسال خودکار پس از بازگشت اینترنت یا راه‌اندازی مجدد دستگاه
- توقف امن روی حساب اشتباه و ادامه پس از ورود به همان حساب
- نمایش تعداد صف، تلاش مجدد دستی و حذف عملیات ناموفق
- نگهداری پیش‌نویس تا رسیدن تأیید واقعی سرور
- RPC دارای operation id برای جلوگیری از ثبت دوباره پس از قطع پاسخ شبکه

### پیش‌نویس آزمون معلم
- ذخیره خودکار فرم آزمون، سؤال‌ها، تصاویر، مخاطبان و تنظیمات در Room
- تشخیص پیش‌نویس مرتبط با همان معلم و همان آزمون
- دیالوگ بازیابی یا حذف هنگام بازگشت
- پاک‌شدن فقط بعد از ذخیره موفق آزمون

### صادر/واردکردن آزمون
- پسوند اختصاصی `.azmoon`
- سازگاری با قالب قدیمی `EXAMPKG1` و JSON
- انتقال سؤال، answer key، تنظیمات تلاش، پیام و رسانه‌های HTTPS
- کنترل سقف ۸ مگابایت و حداکثر ۵۰۰ سؤال
- حذف URIهای محلی و ناامن از فایل ورودی
- واردکردن به Builder برای بازبینی؛ هزینه فقط هنگام ذخیره واقعی محاسبه می‌شود

### پشتیبان و بازیابی کامل
- خروجی JSON نسخه‌دار از آزمون‌ها، کلیدها، کلاس‌ها، عضویت‌ها و سربرگ
- عدم خروجی password، `plain_password`، JWT، API key یا token
- پیش‌نمایش تعداد آزمون، سؤال، کلاس، عضویت و هزینه
- بازیابی انتخابی آزمون، کلاس، عضویت و سربرگ
- کد و شناسه تازه برای آزمون‌های بازیابی‌شده
- تطبیق عضویت فقط با دانش‌آموز موجود و username یکسان
- کسر هزینه و ایجاد همه داده‌ها در یک تراکنش PostgreSQL
- operation id برای جلوگیری از بازیابی یا کسر دوباره

### چاپ و PDF رسمی Native
- حذف مسیر ساده WebView از گزارش رسمی
- `PrintDocumentAdapter` و `PdfDocument` کاملاً Native
- A4 واقعی، سربرگ استان/شهر/منطقه/مدرسه، تاریخ جلالی و شماره صفحه
- صفحه‌بندی چندصفحه‌ای و تکرار سربرگ/پاورقی
- چاپ برگه آزمون، تصاویر، گزینه‌ها، خطوط پاسخ و کلید
- چاپ لیست نمرات و ذخیره PDF از Android Print Service
- پشتیبانی RTL و متن فارسی با `StaticLayout`

### فرمول Native
- تشخیص بخش‌های `$TeX$` بدون اجرای HTML
- پشتیبانی کسر، رادیکال، توان، زیرنویس، ماتریس، یونانی، مجموع، انتگرال و عملگرها
- تبدیل تایپ سریع مثل `sqrt(x)`
- کتابخانه قالب‌های آماده و پیش‌نمایش Native
- نمایش در Builder، آزمون دانش‌آموز، تصحیح و PDF
- ورودی نامتوازن یا بیش‌ازحد بلند رد می‌شود

### تصحیح سؤال‌محور و تحلیل پیشرفته
- جابه‌جایی سؤال‌به‌سؤال و مشاهده پاسخ همه دانش‌آموزان
- اعتبارسنجی زنده نمره و پشتیبانی ارقام فارسی
- ثبت یکجای اتمیک؛ یک نمره نامعتبر کل batch را رد می‌کند
- جدول progress و جلوگیری از نهایی‌سازی پیش از تصحیح همه سؤال‌ها
- دشواری/میانگین، درصد حذف، تمایز گروه بالا و پایین
- point-biserial اصلاح‌شده با نمره کل منهای همان سؤال
- آلفای کرونباخ برای پایایی آزمون
- سطح‌بندی آسان، متعادل، دشوار و تمایز ضعیف

## SQL الزامی

فقط فایل زیر را در SQL Editor پروژه اصلی اجرا کنید:

```text
SQL_NATIVE_OFFLINE_PORTABILITY_ANALYSIS_V10.sql
```

بررسی پس از اجرا:

```sql
select
  to_regprocedure('public.native_submit_queued_answer_v1(uuid,text,jsonb,jsonb,jsonb)') is not null as offline_submit_ready,
  to_regprocedure('public.native_bulk_save_question_grades_v1(text,integer,jsonb)') is not null as bulk_grading_ready,
  to_regprocedure('public.native_finalize_bulk_grades_v1(text)') is not null as finalize_ready,
  to_regprocedure('public.native_question_analysis_v1(text)') is not null as analysis_ready,
  to_regprocedure('public.native_export_backup_v1()') is not null as backup_ready,
  to_regprocedure('public.native_restore_backup_v1(uuid,jsonb,jsonb)') is not null as restore_ready;
```

هر شش مقدار باید `true` باشند.

## امنیت
- payload صف شامل token یا رمز نیست.
- فایل‌های تصویر صف در sandbox خصوصی Android نگهداری می‌شوند.
- هر عملیات صف و restore به user id مالک متصل است.
- تمام RPCهای معلم مالکیت آزمون را با `auth.uid()` کنترل می‌کنند.
- backup سرورمحور عمداً ستون legacy `plain_password` را انتخاب نمی‌کند.
- Restore هیچ id، teacher id یا کد آزمون ورودی را اعتماد نمی‌کند.
- تمام UPDATE/DELETEهای migration دارای WHERE هستند.
- ثبت پاسخ و restore در retry از operation id استفاده می‌کنند.

## تست‌های انجام‌شده

```text
Kotlin compile                         PASS
JVM tests                              38/38 PASS
Math parser/renderer                   PASS
Exam package compatibility            PASS
Pending submission codec              PASS
Builder draft serialization           PASS
Network failure classification        PASS
PostgreSQL 17 migration               PASS
Migration second run                  PASS
Queued submit idempotency              PASS
Atomic bulk grading                    PASS
Finalize progress guard                PASS
Advanced question analysis             PASS
Password/token exclusion               PASS
Atomic paid restore                    PASS
Restore idempotency                     PASS
safeupdate audit: 0 without WHERE      PASS
assembleDebug                           BUILD SUCCESSFUL
lintDebug                               BUILD SUCCESSFUL (0 error)
APK signature v2                        Verified
```

## محدودیت شفاف
- تصویرهای چاپ فقط از HTTPS بارگیری می‌شوند؛ شکست یک تصویر مانع چاپ متن آزمون نمی‌شود.
- عضویت دانش‌آموزی که در حساب مقصد وجود ندارد ایجاد نمی‌شود و در گزارش restore به‌عنوان missing می‌آید.
- WorkManager فقط پاسخ نهایی آزمون را صف می‌کند؛ عملیات مدیریتی معلم در حالت قطع اینترنت به‌جای حدس یا تعارض، خطای روشن می‌دهد.
- پاک‌سازی orphanهای قدیمی Storage و hardening نهایی grant/RLS در V11 انجام می‌شود.
