# V15 — نوار ثابت و پویای پایین برنامه برای معلم

**تاریخ:** ۲۰۲۶-۰۸-۱۳

## ترتیب پنج دکمه از راست به چپ

```text
1. منو
2. کیف پول
3. افزودن (+)
4. آزمون‌ها
5. کارت‌ها
```

جهت ردیف با `LayoutDirection.Rtl` صریح تثبیت شده و به زبان/تنظیم دستگاه وابسته نیست.

## مسیرهای واقعی

### منو

- Drawer موجود برنامه را باز می‌کند.
- تمام مسیرهای تقویم، کلاس، تصحیح، گزارش، پروفایل، درباره و خروج حفظ شده‌اند.
- hamburger بالای صفحه برای معلم حذف شد تا دکمه تکراری نباشد؛ دانش‌آموز همچنان top hamburger دارد.

### کیف پول

- مستقیم `WalletScreen` واقعی را باز می‌کند.
- موجودی، تراکنش‌ها، مبلغ شارژ، preset شارژ و درگاه امن موجود حفظ شده‌اند.

### دکمه مرکزی +

- دکمه صورتی برجسته با سایه و چرخش ۴۵ درجه هنگام بازشدن؛
- ارتفاع dock با انیمیشن افزایش می‌یابد؛
- سه action با fade/scale در یک کمان باز می‌شوند:
  - دانش‌آموز جدید؛
  - آزمون جدید؛
  - کلاس جدید.
- Back دکمه کمانی را می‌بندد.
- «دانش‌آموز جدید» صفحه مدیریت مدرسه را باز و Dialog ساخت حساب را خودکار اجرا می‌کند.
- «آزمون جدید» مستقیماً Builder جدید را باز می‌کند.
- «کلاس جدید» صفحه مدرسه را باز و Dialog ساخت کلاس را خودکار اجرا می‌کند.

### آزمون‌ها

- داشبورد واقعی معلم و فهرست مدیریت آزمون‌ها را باز می‌کند.
- ویرایش، باز/بستن، تکثیر، export، چاپ و حذف موجود حفظ می‌شوند.

### کارت‌ها

یک `ModalBottomSheet` با سه کارت باز می‌شود:

```text
آمار و گزارش‌ها → ReportsScreen
تصحیح           → GradingScreen
مانده            → GradingScreen با فیلتر فقط تصحیح‌نشده
```

در صفحه تصحیح، FilterChip جدید «فقط مانده» اضافه شده و نتیجه خالی پیام مشخص دارد.

## پویایی و ظاهر

- dock ثابت روی همه صفحات اصلی معلم؛
- Surface سه‌لایه با gradient، گوشه ۲۸dp و shadow؛
- رنگ accent صورتی الهام‌گرفته از تصویر مرجع؛
- indicator دایره‌ای برای گزینه فعال؛
- scale و تغییر رنگ icon/label؛
- FAB مرکزی elevated؛
- انیمیشن ارتفاع، rotation، fade و scale؛
- haptic وابسته به سیستم و semantics دکمه‌ها؛
- پشتیبانی dark/light theme؛
- bottom content padding خودکار توسط Scaffold؛
- در Builder برای جلوگیری از مزاحمت مخفی می‌شود.

## تست

```text
RTL five-button order             PASS
Menu drawer route                 PASS
Wallet route                      PASS
Create student route              PASS
Create exam route                 PASS
Create class route                PASS
Exam management route             PASS
Stats card route                  PASS
Grading card route                PASS
Pending-only route                PASS
Arc animation markers             PASS
Kotlin compile                    PASS
JVM tests                         93/93 PASS
FINAL_NATIVE_VERIFY              PASS
lintDebug               PASS — 0 error, 24 warning
assembleDebug                    PASS
APK Signature Scheme v2       Verified
```

## عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V14.1
```
