# V17 — رفتار کامل Native مرجع `design-69.html`

**تاریخ:** ۲۰۲۶-۰۸-۱۳

**پیش‌نیاز:** V16 (`NEUMORPHIC69_NATIVE_INTEGRATION_V16_FA.md`)

> V17 تغییرات V16.1 را نیز در خود دارد؛ اگر V16.1 هنوز Apply نشده است، فقط V17 اعمال می‌شود.

## مرجع ممیزی‌شده

```text
File: design-69.html
SHA-256: 8b1970f5317a6736ba20b1c239d6457e8f6739e7222d937a6d384329a95d4ecf
Size: 61,677 bytes
Lines: 467
Buttons: 35
Inline SVG: 21
JavaScript functions: 17
External resources: 0
```

ممیزی کامل خط‌به‌خط در `DESIGN_69_BEHAVIOR_AUDIT_FA.md` ثبت شده است.

## تصمیم‌های صریح کاربر

```text
منو                  → صفحه کامل؛ بازگشت به آخرین صفحه
+                     → حرکت به مرکز؛ عملیات واقعی دانش‌آموز/آزمون/کلاس
کارت‌ها               → stack واقعی آمار/تصحیح/مانده با swipe چهارجهته
کارت‌های منو          → همه مسیرهای واقعی؛ ۱۰ معلم و ۶ دانش‌آموز
بروزرسانی و خروج      → AboutScreen واقعی و AlertDialog امن
آیکن‌ها و حرکت‌ها     → دقیق‌ترین نسخه Native خطی و متحرک
لمس دوباره مقصد فعال → refresh داده واقعی یا cycle کارت مدیریتی
```

## ۱) منوی تمام‌صفحه

`ModalNavigationDrawer` حذف شد. منو اکنون روی ناحیه محتوای جاری به‌صورت صفحه کامل باز می‌شود، در حالی که صفحه زیرین و state آن حفظ می‌شود.

رفتار:

1. لمس همبرگر، `menuOpen=true` می‌کند؛
2. صفحه فعلی تغییر نمی‌کند و نقش `lastNonMenu` را دارد؛
3. منو تمام محتوا را می‌پوشاند؛
4. آیکن سه‌خط طی 420ms به × تبدیل می‌شود؛
5. لمس دوباره، Back یا انتخاب کارت منو را می‌بندد؛
6. کاربر دقیقاً به همان صفحه و state قبلی برمی‌گردد.

برای معلم morph در نوار پایین و برای دانش‌آموز در سربرگ بالا اجرا می‌شود.

### انیمیشن ورود

- پروفایل از پایین با fade/scale وارد می‌شود؛
- ستون راست از چپ و ستون چپ از راست؛
- تأخیر stagger از 120ms شروع و برای هر کارت 40ms اضافه می‌شود؛
- تنظیم Animator Duration Scale/Remove Animations اندروید توسط Compose رعایت می‌شود.

## ۲) کارت پروفایل و شبکه منو

```text
Profile height: 148dp
Menu card height: 116dp
Columns: 2
Teacher cards: 10 = 5 × 2
Student cards: 6 = 3 × 2
```

کارت پروفایل آواتار، نام و نقش واقعی را نشان می‌دهد و دانش‌آموز هرگز دامنه داخلی Auth را نمی‌بیند. لمس کارت، `ProfileSettingsScreen` واقعی را باز می‌کند.

### معلم

```text
داشبورد / تقویم
کلاس و دانش‌آموز / تصحیح و حضور
گزارش‌ها / کیف پول
تنظیمات / درباره و بروزرسانی
آزمون جدید / خروج
```

### دانش‌آموز

```text
داشبورد / تقویم
نتایج / تنظیمات
درباره و بروزرسانی / خروج
```

بروزرسانی وارد `AboutScreen` واقعی و خروج وارد `AlertDialog` امن می‌شود؛ check و خروج آزمایشی HTML وارد Runtime نشده‌اند.

## ۳) آیکن‌های خطی Native و میکروانیمیشن

فایل `Design69Icons.kt` pathهای خطی را با `ImageVector` و `Canvas` می‌سازد؛ SVG/JavaScript/WebView اجرا نمی‌شود.

آیکن‌های Native:

```text
Wallet / Add / Exams / Cards
Calendar / Classes / Students
InfoUpdate / Settings / Logout
Reports / Grading / Dashboard
PersonAdd / ClassAdd / ExamAdd / Chevron
```

حرکت‌ها:

```text
menu   → morph سه خط به ×
wallet → flip 180° در میانه و بازگشت
exams  → bounce عمودی
cards  → wiggle و rotation
all    → ripple 520ms و press scale
active → lift 5dp + inner shadow + dot
```

## ۴) دکمه + مشترک و متحرک

- دکمه dock دایره کامل 70×70dp است؛
- پس از لمس، نسخه dock پنهان و همان دکمه متحرک از پایین به مرکز می‌آید؛
- طی 620ms به مرکز می‌رسد، 135 درجه می‌چرخد و به × تبدیل می‌شود؛
- صفحه افزودن با پنل فرورفته و خطوط dashed نمایش داده می‌شود؛
- سه گزینه با delayهای 40/110/180ms باز می‌شوند:

```text
بالا: دانش‌آموز جدید → SchoolManagement CREATE_STUDENT
راست: آزمون جدید     → ExamBuilderScreen
چپ: کلاس جدید        → SchoolManagement CREATE_CLASS
```

لمس × یا Back دکمه را به dock بازمی‌گرداند. عملیات نمونه تراکنش/کارت بانکی HTML عمداً جایگزین مسیرهای واقعی نشده‌اند.

## ۵) کارت‌های مدیریتی چهارجهته

دکمه کارت‌ها دیگر Bottom Sheet باز نمی‌کند؛ صفحه کامل stack مدیریتی باز می‌شود:

```text
آمار و گزارش‌ها → ReportsScreen
تصحیح           → GradingScreen
مانده            → GradingScreen(initialPendingOnly=true)
```

رفتار:

- آستانه drag دقیق 52dp؛
- چپ/بالا → کارت بعدی؛ راست/پایین → کارت قبلی؛
- drag لغوشده طی 280ms برمی‌گردد؛
- stack با scale، alpha، rotation و offset متحرک می‌شود؛
- Arrow keys و Enter پشتیبانی می‌شوند؛
- لمس کارت فعال مقصد واقعی را باز می‌کند؛
- لمس مجدد آیکن کارت‌ها، کارت بعدی را فعال می‌کند.

هیچ شماره کارت، رمز پویا، مسدودسازی بانکی یا مبلغ ساختگی از HTML وارد برنامه نشده است.

## ۶) رفتار لمس مجدد نوار پایین

```text
Wallet فعال → BillingViewModel.load + tilt کارت موجودی + flip آیکن
Exams فعال  → TeacherDashboardViewModel.load + bounce آیکن
Cards فعال  → cycle کارت مدیریتی + wiggle آیکن
Menu باز     → بسته‌شدن و بازگشت به صفحه قبلی
```

## ۷) داشبورد معلم

- دکمه دستی «به‌روزرسانی» وجود ندارد؛
- `PullToRefreshBox` به `viewModel.load` متصل است؛
- کل صفحه یک `LazyColumn` است؛
- ساخت، import، ویرایش، باز/بستن، تکثیر، export، چاپ و حذف حفظ شده‌اند.

## ۸) امنیت و سازگاری

```text
WebView/JavaScript جدید: 0
داده demo در Runtime: 0
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
```

- موجودی، تراکنش، آزمون، پروفایل، بروزرسانی و خروج همگی از مسیرهای واقعی قبلی‌اند.
- internal student Auth domain در کارت پروفایل نمایش داده نمی‌شود.
- light/dark/dynamic colors و چهار پالت ماندگار V16 حفظ شده‌اند.
- Builder همچنان dock ندارد تا ویرایش آزمون مزاحمت نداشته باشد.

## فایل‌های اصلی V17

```text
DESIGN_69_BEHAVIOR_AUDIT_FA.md
DESIGN_69_NATIVE_BEHAVIOR_V17_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt
app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt
app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt
app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt
app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt
app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt
app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt
app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt
app/src/test/java/ir/exam/app/ui/app/TeacherBottomDockTest.kt
scripts/verify_native_final.py
```

## تست

```text
Reference audit/hash                         PASS
Kotlin compile                              PASS
JVM tests                                   101/101 PASS
Full-page menu contract                     PASS
10/6 complete two-column grids              PASS
Native vector/morph markers                 PASS
Real quick-add route markers                PASS
52dp four-direction card regression         PASS
Active destination refresh regression       PASS
No demo/WebView regression                  PASS
FINAL_NATIVE_VERIFY                         PASS
lintDebug                        PASS — 0 error, 24 warning
assembleDebug                               PASS
Debug package                               ir.exam.app.native
APK Signature Scheme v2                     Verified
Debug APK SHA-256                           bab45f4cfdadba570765886ceefb88758585e283eb37cede96717545f8523c92
```

## تست دستگاه

1. همبرگر → صفحه کامل منو و morph به ×؛
2. بستن با × و Back → بازگشت به همان صفحه قبلی؛
3. ورود stagger کارت پروفایل و دو ستون؛
4. همه ۱۰ کارت معلم و ۶ کارت دانش‌آموز؛
5. About و Dialog خروج واقعی؛
6. حرکت + از dock تا مرکز و بازگشت؛
7. سه عملیات واقعی quick-add؛
8. swipe کارت‌های مدیریتی در هر چهار جهت؛
9. لمس و کلید جهت/Enter روی کارت‌ها؛
10. flip/bounce/wiggle/ripple آیکن‌ها؛
11. لمس دوباره Wallet/Exams/Cards؛
12. Pull-to-refresh داشبورد؛
13. light/dark/dynamic و Remove Animations؛
14. عدم هم‌پوشانی با status/navigation bar.
