# V32 — رفع کرش آپلود، اسکرول شماره کارت‌ها، ویرایش همانند گروهی و کپی رمز

## وضعیت ورودی

- V31 build/device → SUCCESS (اعلام کاربر)
- گزارش دستگاه → پس از انتخاب تصویر جهت آپلود، برنامه کرش می‌کند

## ۱) رفع کرش آپلود تصویر

فایل: `app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt`

- بودجهٔ لبهٔ decode از `maxDimension * 2 shr attempt` به `maxDimension shr attempt`
  اصلاح شد؛ کف `MIN_DECODE_EDGE = 640` اضافه شد تا تلاش اول دقیقاً روی هدف ۲۲۰۰
  بماند و هر تلاش نصف شود (نه اینکه تلاش اول دو برابر هدف decode کند).
- `uploadOnce` بدنهٔ خود را در `try/finally` گذاشت؛ `bitmap.recycle()` روی هر مسیر
  (حتی خطا) اجرا می‌شود.
- `decodeSampledBitmap` با `var current: Bitmap` و `catch (t: Throwable)` هر bitmap
  میانی را هنگام OutOfMemoryError بازیافت می‌کند تا حلقهٔ `while (attempt < MAX_ATTEMPTS)`
  در `uploadAt` بدون نشتی حافظه تلاش بعدی را انجام دهد.

نتیجه: آپلود تصویر بزرگ دیگر فرایند را نمی‌کشد؛ در بدترین حالت پیام فارسی
«حافظه دستگاه برای این تصویر کافی نیست…» نمایش داده می‌شود.

## ۲) اسکرول شمارهٔ کارت‌ها در پنجرهٔ گروهی

فایل: `app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt`

- لیست شمارهٔ کارت‌ها از `rows.indices.chunked(6)` (چند سطر ثابت) به یک `LazyRow`
  افقی تبدیل شد.
- `rememberLazyListState()` + `LaunchedEffect(activeIndex, rows.size)` +
  `animateScrollToItem(activeIndex)` شمارهٔ کارت فعال را خودکار به دید می‌آورد.
- بدون کلاس و بدون اسکرول عمودی اضافی.

## ۳) پنجرهٔ ویرایش دانش‌آموز همانند پنجرهٔ گروهی

فایل: `app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt`

- همان wrapper پنجرهٔ گروهی: `Dialog` + `BoxWithConstraints` + `Surface`
  هم‌عرض ۶۲۰dp + `heightIn(max = availableHeight)` + `SOFT_INPUT_ADJUST_RESIZE`.
- به‌جای `+`/`ایجاد`/`×`: دکمهٔ قرمز «انصراف» و دکمهٔ «ذخیره» در بالای پنجره.
- فیلدها پیش‌پر از اطلاعات دانش‌آموز و با همان چیدمان دوستونی گروهی:
  نام/نام‌خانوادگی، نام پدر/نام کاربری، پایه/رشته، رمز جدید اختیاری/رمز فعلی،
  دختر/پسر/🎲.
- عنوان «ویرایش دانش‌آموز» حذف شد.

## ۴) دکمهٔ کپی روی کارت دانش‌آموز

فایل: `app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt`

- رمز از کادر رمز فعلی (حافظهٔ نشست `knownPasswords`) برداشته و به‌صورت حساس
  (IS_SENSITIVE) کپی می‌شود.
- اخطار «رمز قبلی در سامانه ذخیره نمی‌شود» از Toast حذف شد؛ در نبود رمز فقط
  «اطلاعات دانش‌آموز کپی شد.» نمایش داده می‌شود.

## امنیت رمز (بدون تغییر)

رمز قبلی Supabase Auth یک hash یک‌طرفه است و قابل بازیابی نیست؛ `plain_password`
بازنمی‌گردد. فقط رمز جدید ثبت‌شده یک‌بار با Clipboard حساس قابل کپی است.

## عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V31
```

## تست

```text
FINAL_NATIVE_VERIFY (اسکریپت محلی)   → PASS
V32 source tests                      → 7/7
رگرسیون V19/V21/V31                   → به‌روزرسانی شد
lintDebug / assembleDebug             → اجرا در WSL (gradlew)
```
