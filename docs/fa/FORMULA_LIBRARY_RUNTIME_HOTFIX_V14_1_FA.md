# V14.1 — Hotfix اجرای واقعی کتابخانه‌های فرمول

**تاریخ:** ۲۰۲۶-۰۸-۱۳

## علت قطعی

دادهٔ کتابخانه سالم بود، اما مسیر UI دسته‌ها فقط `categoryId` را عوض می‌کرد و Dialog گروه را می‌بست. Grid نمادها پایین‌تر از keypad و search داخل LazyColumn قرار داشت؛ بنابراین بعد از لمس گروه/دسته هیچ کتابخانه‌ای جلوی کاربر باز نمی‌شد و در دستگاه چنین به‌نظر می‌رسید که دکمه‌ها کار نمی‌کنند.

تست‌های قبلی فقط JSON و درج ۲۱۱۸ TeX را بررسی می‌کردند و «قابل مشاهده بودن نتیجهٔ کلیک» را پوشش نمی‌دادند.

## اصلاح

- `FormulaLibraryNavigator` مسیر واحد common/all/unicode/group/favorites/recent/letters/search است.
- `FormulaLibraryDialog` تمام‌صفحه اضافه شد.
- هر انتخاب گروه و زیرگروه، بلافاصله کتابخانهٔ همان دسته را باز می‌کند.
- موارد پرکاربرد، همه نمادها، Unicode 1200، نمادهای اخیر، علاقه‌مندی و abc مستقیم باز می‌شوند.
- کتابخانه‌های Smart Hub و بسته‌ها نیز از همان Dialog استفاده می‌کنند.
- تعداد کل و نتیجهٔ فیلتر در سربرگ دیده می‌شود.
- جست‌وجوی داخلی هر کتابخانه اضافه شد.
- label نماد همیشه زیر SVG دیده می‌شود.
- سه مسیر قطعی انتخاب وجود دارد:
  1. لمس کامل کارت؛
  2. دکمهٔ صریح «درج»؛
  3. long-press برای علاقه‌مندی.
- پس از درج، Dialog بسته و پیام موفقیت در ویرایشگر نمایش داده می‌شود.
- favorites داخل Dialog بدون بستن قابل تغییر است.
- حالت empty و «نتیجه‌ای پیدا نشد» دیگر silent نیست.
- decoder خالص `FormulaReferenceLibrary.decode()` اضافه شد تا تست دقیقاً همان loader Runtime را اجرا کند.

## دامنهٔ تست

```text
main groups                       8/8
category links                   75/75
categories                       77/77
common route                     24
Unicode route                  1200/1200
letters lower                    26
letters upper                    26
broken routes                     0
empty linked routes               0
reference SVG generation      2118/2118
reference active-box insert   2118/2118
```

## تست نهایی

```text
Kotlin compile                    PASS
JVM tests                         91/91 PASS
FormulaLibrary decode             PASS
All route navigation              PASS
Full-screen UI regression         PASS
Explicit insert action            PASS
FINAL_NATIVE_VERIFY               PASS
lintDebug                PASS — 0 error, 24 warning
assembleDebug                     PASS
APK Signature Scheme v2        Verified
Patch apply/tree comparison       PASS
```

## عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V14
```
