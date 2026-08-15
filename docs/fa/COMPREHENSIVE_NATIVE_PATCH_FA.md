# پچ جامع V6 مدیریت Native

این پچ بر اساس سه منبع واقعی نوشته شده است:

```text
سورس Kotlin فعلی
سورس مرجع WebView commit d82b2feedee1
schema_snapshot زنده Supabase در 2026-08-11
```

## قابلیت‌های افزوده‌شده

### مدیریت آزمون

- فهرست آزمون‌های معلم
- ویرایش آزمون ذخیره‌شده
- حفظ تنظیمات واقعی shuffle، نمره منفی، پیام معلم و تلاش‌ها
- باز و بسته‌کردن آزمون
- تکثیر تراکنشی با کد جدید، وضعیت بسته و تاریخ خالی
- حذف تراکنشی آزمون و داده‌های وابسته
- جداسازی answer key در جدول `exam_keys`
- جلوگیری از قرارگرفتن پاسخ صحیح داخل `exams.questions`
- سازگاری decode/encode با JSON نسخه WebView
- حفظ فیلدهای ناشناخته و سؤال جورکردنی هنگام ویرایش
- مخاطب همه، کلاس‌های خاص یا دانش‌آموزان خاص

### تصویر سؤال

- انتخاب چند تصویر واقعی
- فشرده‌سازی و محدودکردن ابعاد در Android
- خروجی WebP روی Android جدید و JPEG روی نسخه‌های قدیمی
- آپلود واقعی در bucket `exam-images`
- مسیر استاندارد `questions/{teacherId}/{examId}/{uuid}`
- نمایش پیشرفت آپلود
- عدم آپلود دوباره URLهای قبلی
- حذف تصویر از فرم و حرکت مستقل روی بوم

### کلاس و دانش‌آموز

- فهرست واقعی کلاس‌ها از `my_classes`
- ساخت، ویرایش و حذف کلاس
- حذف کلاس بدون حذف ناخواسته حساب دانش‌آموز
- نمایش roster واقعی
- افزودن چند دانش‌آموز موجود به کلاس
- خروج دانش‌آموز از کلاس با حفظ حساب
- فهرست و جست‌وجوی دانش‌آموزان
- فعال و غیرفعال‌کردن دانش‌آموز
- ساخت حساب جدید از Edge Function موجود `manage-student`
- نمایش یک‌باره credential ساخته‌شده
- عدم دریافت یا نمایش `plain_password` در DTO Native

### حساب کاربری

- گزینه «خروج و تعویض حساب» در منوی همبرگری
- تأیید پیش از خروج
- خروج فقط از session همین دستگاه با `SignOutScope.LOCAL`
- پاک‌سازی cache نمای پروفایل
- حفظ رفتار ماندگاری نشست در حالت عادی

## SQL موردنیاز

فایل زیر را یک‌بار در SQL Editor پروژه اصلی اجرا کنید:

```text
supabase/migrations/20260811_native_comprehensive_management.sql
```

این SQL فقط دو RPC جدید می‌سازد:

```text
native_delete_exam(text)
native_duplicate_exam(text)
```

هر دو تابع مالکیت معلم را با `auth.uid()` کنترل می‌کنند. همه DELETEها دارای WHERE هستند و با `safeupdate` سازگارند.

## پیش‌نیاز موجود

برای ساخت حساب دانش‌آموز، Edge Function زیر باید همانند نسخه WebView مستقر باشد:

```text
manage-student
```

هیچ کلید مدیریتی داخل APK قرار نمی‌گیرد.

## محدودیت شفاف این مرحله

- داده‌های جورکردنی قدیمی حفظ می‌شوند، اما UI تخصصی ویرایش ستون‌های جورکردنی هنوز کامل نیست.
- تصویر سؤال واقعی است؛ UI تازه برای انتخاب تصویر گزینه و پاسخ دانش‌آموز در مرحله رسانه بعدی تکمیل می‌شود. URLهای قبلی آن‌ها هنگام ویرایش از بین نمی‌روند.
- حذف فایل‌های Storage هنگام حذف آزمون عمداً خودکار نشده، چون آزمون تکثیرشده ممکن است همان URL را استفاده کند.
- حذف کامل حساب دانش‌آموز در UI Native ارائه نشده؛ خروج از کلاس و حذف کلاس حساب را حفظ می‌کند.
- schema زنده ستون legacy به نام `plain_password` دارد. Native آن را decode یا نمایش نمی‌دهد؛ حذف کامل این بدهی امنیتی نیازمند تغییر هم‌زمان Edge Function و نسخه WebView است.

## تست‌های انجام‌شده

```text
Kotlin compile                         PASS
JVM tests                              13/13 PASS
ExamQuestionCodec key separation       PASS
Legacy question decode                 PASS
ClassesViewModel                       PASS
Auth explicit sign-out                 PASS
PostgreSQL 17 SQL integration          PASS
Duplicate exam transaction             PASS
Delete exam dependents                 PASS
safeupdate audit                       PASS
assembleDebug                          BUILD SUCCESSFUL
lintDebug                              BUILD SUCCESSFUL
APK Signature Scheme v2                Verified
```
