# V18 — ناوبری فشرده، مدیریت پنج‌کارت، حساب و سربرگ

**تاریخ:** ۲۰۲۶-۰۸-۱۳

**پیش‌نیاز:** V17

## تغییرات نوار پایین

- هاله انتخاب از سطح تقریباً کامل خانه به مربع `44×44dp` با radius `14dp` کاهش یافت.
- lift انتخاب از 5dp به 3dp کاهش یافت.
- دکمه + از 70dp به `58dp` کاهش یافت.
- + دقیقاً در مرکز ارتفاع dock قرار دارد و از بالای قاب بیرون نمی‌زند.
- هنگام انتقال + به مرکز صفحه، Composable دکمه پایین اصلاً ساخته نمی‌شود؛ در نتیجه سایه، هاله یا رد آن باقی نمی‌ماند.
- + متحرک از 58dp شروع و در مرکز تا حدود 70dp بزرگ می‌شود.

## پنج کارت مدیریتی

ترتیب قطعی:

```text
1. آمار
2. بانک سؤال
3. تصحیح
4. مانده
5. پاسخ
```

- stack و swipe چهارجهته حفظ شده است.
- پنج indicator زیر stack نمایش داده می‌شود.
- ردیف تکراری دکمه‌های کارت حذف شد.
- زیر stack فقط عنوان و توضیح کارت فعال نمایش داده می‌شود.
- لمس کارت فعال مقصد واقعی را باز می‌کند.

مسیرها:

```text
آمار       → ReportsScreen
بانک سؤال → QuestionBankScreen مستقل
تصحیح      → GradingScreen همه پاسخ‌ها
مانده      → GradingScreen فقط تصحیح‌نشده
پاسخ       → GradingScreen فقط تصحیح‌شده
```

## بانک سؤال مستقل

`QuestionBankScreen` و `QuestionBankViewModel` اضافه شدند:

- جست‌وجوی متن/درس؛
- فیلتر دسته؛
- ساخت و حذف دسته؛
- مشاهده سؤال و رندر Native فرمول؛
- ویرایش متن، درس، بارم، دسته و فیلدهای پاسخ متناسب با نوع؛
- حذف سؤال؛
- افزودن سؤال به یک آزمون جدید و انتقال مستقیم به Builder؛
- حفظ تصاویر/style/matching پیشین هنگام ویرایش فیلدهای عمومی.

RPC جدید `native_bank_update_question_v1` مالکیت `auth.uid()` را بررسی می‌کند و ویرایش و دسته‌ها را در یک تراکنش انجام می‌دهد.

## آزمون‌ها

- هر آزمون یک کارت خلاصه واقعی است؛
- عنوان، درس، کد، مدت، بارم و وضعیت همیشه دیده می‌شوند؛
- لمس کارت فقط همان کارت را باز می‌کند؛
- لمس دوباره کارت را می‌بندد؛
- عملیات ویرایش، باز/بستن، تکثیر، export، چاپ، چاپ کلید و حذف فقط در کارت باز نمایش داده می‌شوند؛
- Pull-to-Refresh حفظ شده است؛
- عنوان تکراری «داشبورد معلم» حذف شد.

## حذف عنوان‌های تکراری

برای معلم TopBar متنی حذف شد. عبارت‌های زیر دیگر فضای عمودی اشغال نمی‌کنند:

```text
داشبورد معلم
کارت‌های مدیریتی
افزودن سریع
کیف پول و پرداخت
منوی اصلی
```

دانش‌آموز فقط یک نوار فشرده 54dp با دکمه همبرگر/× دارد.

## منوی همبرگری

کارت پروفایل بزرگ در بالا باقی است و شش کارت معلم دقیقاً به ترتیب زیر هستند:

```text
تقویم و پیام‌ها
کلاس‌ها
دانش‌آموزان
سربرگ
تنظیمات
خروج
```

موارد زیر حذف شدند:

```text
داشبورد معلم
تصحیح و حضور
آمار و گزارش‌ها
درباره و بروزرسانی
آزمون جدید
```

- «کلاس‌ها» مستقیماً تب کلاس‌ها را باز می‌کند.
- «دانش‌آموزان» مستقیماً تب همه دانش‌آموزان را باز می‌کند.
- لمس کارت پروفایل مستقیماً بخش پروفایل را باز می‌کند.
- جمله «مشاهده و ویرایش حساب و تنظیمات» حذف شد.
- صفحه پیش‌فرض پس از ورود `CalendarScreen` است.

## تفکیک پروفایل، سربرگ و تنظیمات

### پروفایل

- عکس پروفایل و کنترل انتشار عکس؛
- فقط فیلد نام نمایشی؛
- مشخصات حساب از این بخش حذف شد.

### سربرگ

```text
استان
شهر / شهرستان
منطقه / ناحیه
نام مدرسه
پایه
```

پایه با ستون `profiles.hdr_grade` در سرور ذخیره، در backup v3 حفظ و در PDF/چاپ رسمی درج می‌شود.

### تنظیمات

فقط چهار تب:

```text
ظاهر
حساب
داده‌ها
درباره
```

AboutScreen واقعی داخل تب «درباره» قرار دارد.

### حساب

```text
مشخصات حساب
تغییر نام کاربری
تغییر ایمیل
تغییر رمز عبور
قفل برنامه
```

- تغییر ایمیل با `Supabase Auth updateUser` انجام می‌شود.
- پیام تأیید به ایمیل جدید ارسال می‌شود و تا تأیید، ایمیل فعلی معتبر می‌ماند.
- ایمیل داخلی حساب مدیریت‌شده دانش‌آموز نمایش داده نمی‌شود.

## قفل برنامه

طبق انتخاب کاربر، PIN اختصاصی برنامه حذف شد. فقط `BiometricPrompt` رسمی Android استفاده می‌شود:

```text
اثر انگشت
چهره
الگو
PIN دستگاه
رمز دستگاه
```

Android بر اساس روش فعال دستگاه تصمیم می‌گیرد. برنامه هیچ PIN، الگو یا داده زیستی را ذخیره نمی‌کند. hash/salt قدیمی PIN نیز هنگام خواندن تنظیمات پاک می‌شود.

Dependency جدید:

```text
androidx.biometric:biometric:1.1.0
```

`MainActivity` اکنون `FragmentActivity` است تا BiometricPrompt رسمی را پشتیبانی کند.

## SQL V18

فایل:

```text
supabase/migrations/20260813_native_navigation_account_v18.sql
SQL_NATIVE_NAVIGATION_ACCOUNT_V18.sql
```

Readiness مورد انتظار:

```text
header_grade_ready       true
profile_save_v18_ready   true
bank_update_ready        true
backup_v2_ready          true
restore_v2_ready         true
```

تغییرات SQL:

- `profiles.hdr_grade`؛
- امضای هشت‌پارامتری `native_save_profile`؛
- بازنشانی `native_my_profile` و `native_ensure_profile_v1`؛
- `native_bank_update_question_v1`؛
- `native_export_backup_v2` با format version 3؛
- `native_restore_backup_v2` سازگار با backup نسخه 1 تا 3؛
- revoke از public/anon و grant فقط authenticated؛
- همه UPDATE/DELETEها دارای WHERE.

## فایل‌های اصلی

```text
NAVIGATION_ACCOUNT_MANAGEMENT_V18_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
SQL_NATIVE_NAVIGATION_ACCOUNT_V18.sql
app/build.gradle.kts
app/src/main/java/ir/exam/app/MainActivity.kt
app/src/main/java/ir/exam/app/core/security/AppLockManager.kt
app/src/main/java/ir/exam/app/ui/security/AppLockUi.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt
app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt
app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt
app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt
app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt
app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt
app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt
app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt
supabase/migrations/20260813_native_navigation_account_v18.sql
supabase/tests/20260813_v18_integration.sql
```

## تست

```text
Kotlin compile                         PASS
JVM tests                              105/105 PASS
V18 migration PostgreSQL parser       PASS — 23 statements
V18 test SQL parser                    PASS — 4 statements
Unsafe UPDATE/DELETE                   0
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 22 warning
assembleDebug                         PASS
Debug package                         ir.exam.app.native
Biometric dependency packaged         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     8812bd60fae6489ece6485f3421a62416444db053f8cbfe20c963780c7374803
```

## عملیات

```text
SQL جدید: بله؛ یک‌بار روی پروژه اصلی اجرا شود.
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration Room: ندارد
Dependency جدید: Biometric 1.1.0
```
