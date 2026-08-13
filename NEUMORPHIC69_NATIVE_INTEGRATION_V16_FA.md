# V16 — ادغام Native طرح نئومورفیک ۶۹ با سامانه آزمون

**تاریخ:** ۲۰۲۶-۰۸-۱۳

**پیش‌نیاز:** V14.1 و V15

**مرجع دریافتی:** `neumorphic69-compose_all.tar.gz`

## صحت مرجع

```text
Archive SHA-256:
0bb34550506669b4ec3a0f07fdb2e43b6176c66b22fd767133e4488bd35b7fa3

Archive bytes:       270642
Files:               20
Directories:         16
Unsafe paths:         0
Kotlin source lines: 1502
```

هش آرشیو دانلودشده دقیقاً با هش اعلام‌شده کاربر یکسان بود و همهٔ ۲۰ فایل پیش از استخراج از نظر path traversal بررسی شدند.

## روش ادغام

پروژه مستقل مرجع به‌صورت کورکورانه داخل برنامه کپی نشد، چون صفحه‌های کیف پول، آزمون و کارت آن دادهٔ نمایشی داشتند و package مستقل آن `com.example.neumorphic69` بود. در V16 فقط طراحی و رفتارهای قابل استفاده استخراج و به مسیرهای واقعی سامانه متصل شدند.

مواردی که عمداً وارد Runtime نشدند:

```text
MainActivity مستقل مرجع
Gradle و Manifest مستقل مرجع
package آزمایشی com.example.neumorphic69
موجودی و تراکنش ساختگی
آزمون و کارت بانکی ساختگی
Snackbarهای «آزمایشی»
صفحه Placeholder
```

هیچ صفحهٔ واقعی V1 تا V15 با نسخه demo جایگزین نشده است.

## پوسته نئومورفیک واقعی

فایل جدید:

```text
app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt
```

شامل:

- سایه روشن بالا-چپ و سایه تیره پایین-راست با Canvas Native؛
- حالت فرورفته هنگام لمس؛
- `NeumorphicPanel` و `NeumorphicPressable` قابل استفاده مجدد؛
- پشتیبانی خودکار light، dark و dynamic colors؛
- سربرگ نئومورفیک سازگار با status bar؛
- آیتم‌های Drawer با indicator و semantics انتخاب؛
- انیمیشن scale فشردن؛
- محدودسازی محتوای authenticated در نمایشگر بزرگ تا ۹۰۰dp بدون محدودکردن Builder.

جهت authenticated shell با `LayoutDirection.Rtl` صریح تثبیت شده است. Drawer برای فارسی از سمت راست باز می‌شود.

## نوار پایین V15 با ظاهر طرح ۶۹

تمام عملکردهای V15 حفظ شده‌اند:

```text
راست به چپ:
منو / کیف پول / افزودن / آزمون‌ها / کارت‌ها

افزودن:
دانش‌آموز جدید / آزمون جدید / کلاس جدید

کارت‌ها:
آمار و گزارش‌ها / تصحیح / مانده
```

تغییرات ظاهری:

- dock فقط آیکن مطابق مرجع و دارای contentDescription کامل؛
- indicator فعال گرادیانی؛
- دکمه مرکزی گرادیانی با چرخش؛
- سایه دوطرفه و حالت فرورفته؛
- کمان سه‌عملیاتی با fade/scale؛
- Sheet کارت‌های مدیریتی نئومورفیک؛
- navigation bar inset واقعی؛
- Back برای بستن کمان.

مسیرهای واقعی Drawer، `WalletScreen`، `ExamBuilderScreen`، `SchoolManagementScreen`، `TeacherDashboardScreen`، `ReportsScreen` و `GradingScreen` بدون تغییر قرارداد حفظ شده‌اند.

## کیف پول و داشبورد واقعی

### کیف پول

- کارت موجودی گرادیانی از balance واقعی ViewModel؛
- نمایش/مخفی‌کردن موجودی؛
- refresh واقعی؛
- شارژ و درگاه امن قبلی بدون تغییر؛
- تراکنش‌های واقعی داخل پنل‌های نئومورفیک؛
- هیچ مبلغ یا تراکنش نمونه وارد Runtime نشده است.

### داشبورد معلم

کارت آزمون‌های واقعی با پنل نئومورفیک نمایش داده می‌شوند. ویرایش، باز/بسته‌کردن، تکثیر، import/export، چاپ و حذف همان مسیرهای واقعی قبلی هستند.

### داشبورد دانش‌آموز

فرم کد آزمون و وضعیت صف WorkManager داخل پنل‌های نئومورفیک قرار گرفتند. بازیابی آزمون، draft، timer، پاسخ و ارسال تغییری نکرده‌اند.

## تنظیمات ماندگار ظاهر

`AppearancePreferences` دو مقدار جدید و محلی دارد:

```text
neumorphicPalette
neumorphicDepth
```

چهار پالت مرجع:

```text
نیلی + سبز
آبی + فیروزه‌ای
صورتی + نارنجی
بنفش + صورتی
```

عمق سایه بین ۸ تا ۲۲ است و مقدار پیش‌فرض ۱۴ است. هر دو مقدار در DataStore دستگاه ذخیره می‌شوند. اگر dynamic colors فعال باشد، رنگ دستگاه اولویت دارد؛ این وضعیت در UI توضیح داده شده است.

مسیر دسترسی:

```text
منو → پروفایل و تنظیمات → ظاهر → ظاهر نئومورفیک ۶۹
```

## فونت

وزن‌های واقعی مرجع اضافه شدند:

```text
vazirmatn_medium.ttf
SHA-256: b986623e4ddef10755e04be39f8ea7bcb1dc08bfe8dd0aa6af395736f256ad4a

vazirmatn_bold.ttf
SHA-256: f635fdbea28f265de395ba83b4b1570dcf2f58d13c65469e61903b1c2d2ae723
```

وزن‌های `Normal`، `Medium`، `SemiBold` و `Bold` اکنون واقعاً به فایل مناسب نگاشت می‌شوند. مجوز OFL وزیرمتن از قبل در assets پروژه وجود داشت.

## اصلاح عملیاتی Gradle

mode فایل `gradlew` از `100644` به `100755` تغییر کرد تا اجرای مستقیم `./gradlew` در Ubuntu/GitHub Actions قطعی باشد.

## امنیت و سازگاری

```text
WebView/JavaScript جدید:       ندارد
SQL جدید:                      ندارد
Edge Function جدید:            ندارد
Secret جدید:                   ندارد
Migration جدید:                ندارد
Dependency جدید:               ندارد
داده demo در Runtime:          ندارد
تغییر Auth/Supabase/RLS:        ندارد
```

هیچ URL، Header، Token، کلید پرداخت یا اطلاعات امضا در UI/کد جدید نمایش داده نمی‌شود.

## تست انجام‌شده

```text
Reference archive checksum                  PASS
Archive path safety                         PASS
Kotlin compile                              PASS
JVM tests                                   97/97 PASS
Dock contract order                         PASS
Quick-create contract                       PASS
Management-card contract                    PASS
No standalone demo package                  PASS
No fake wallet balance                      PASS
Reference medium/bold font hashes           PASS
FINAL_NATIVE_VERIFY                         PASS
lintDebug                                   PASS — 0 error, 24 warning
assembleDebug                               PASS
Debug package                               ir.exam.app.native
APK Signature Scheme v2                     Verified
Debug APK SHA-256                           975d79e127a749209e1999be03d84e6f28dd92e7dfc6d21eba7c106dbd6a37df
```

هش APK بالا فقط مربوط به Build داخلی Debug با Secretهای خالی است. Build نهایی Release باید در GitHub Actions انجام شود.

## تست دستگاه پس از Build

1. بازشدن Drawer از سمت راست؛
2. سربرگ و متن RTL در معلم و دانش‌آموز؛
3. ترتیب پنج آیکن پایین؛
4. بازشدن و بسته‌شدن کمان با لمس و Back؛
5. هر سه quick-create واقعی؛
6. Wallet و نمایش/مخفی‌کردن موجودی واقعی؛
7. مدیریت آزمون‌ها؛
8. Sheet کارت‌ها و هر سه مسیر آمار/تصحیح/مانده؛
9. چهار پالت با خاموش‌کردن dynamic colors؛
10. عمق سایه ۸ و ۲۲؛
11. ماندگاری پالت و عمق بعد از بستن/بازکردن؛
12. تم روشن و تیره؛
13. فونت Medium/Bold؛
14. عدم هم‌پوشانی dock با navigation bar؛
15. تبلت یا چرخش افقی و محدودشدن محتوا در مرکز.
