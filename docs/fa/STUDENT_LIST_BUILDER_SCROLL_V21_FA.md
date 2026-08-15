# V21 — نوار دانش‌آموز، کنترل گروهی و اسکرول دقیق سؤال

**تاریخ:** ۲۰۲۶-۰۸-۱۳

**پیش‌نیاز:** V20

## فهرست دانش‌آموزان

دکمه «حساب جدید» حذف شد. نوار بالا دقیقاً به ترتیب زیر است:

```text
Excel | + | ذره‌بین
```

- `+` پنجره افزودن گروهی را باز می‌کند.
- ذره‌بین با fade/expand کادر جست‌وجو را زیر نوار باز می‌کند.
- هنگام بازبودن جست‌وجو، ذره‌بین پنهان می‌شود.
- × داخل گوشه کادر، query را پاک، کادر را جمع و ذره‌بین را برمی‌گرداند.
- محتوای همه Button/IconButtonها طبق Material3 و NeumorphicPressable در مرکز قرار دارد.

## افزودن گروهی

- کنترل `▦` و هر عنوان متنی بالای پنجره حذف شدند.
- سه کنترل به بالاترین بخش پنجره منتقل شدند:

```text
+ سبز | ایجاد | × قرمز
```

- «ساخت حساب‌ها» به «ایجاد» تغییر کرد.
- دکمه ایجاد دو برابر دکمه‌های کناری عرض دارد.
- انتخاب کلاس و کارت‌های دانش‌آموز زیر این نوار قرار می‌گیرند.

## اسکرول فرمول

Auto-scroll زودتر فعال می‌شود:

```text
look-ahead افقی: 14٪
هدف افقی: 62٪ viewport
look-ahead عمودی: 12٪
هدف عمودی: 68٪ viewport
```

## آزمون‌ساز

- + و ✓ در دو انتهای مخالف همان ناحیه شناور قرار دارند.
- بازکردن کارت سؤال ابتدا state را تغییر می‌دهد.
- دو frame صبر می‌شود: recomposition و اندازه‌گیری کارت بازشده.
- سپس `animateScrollToItem(questionPrefaceCount + index, 0)` اجرا می‌شود.
- offset صفر، بالای کارت را دقیقاً زیر TopAppBar قرار می‌دهد.
- سؤال جدید و سؤال بانک نیز از همین تابع مشترک استفاده می‌کنند.

## تست

```text
Kotlin compile                         PASS
JVM tests                              119/119 PASS
Student toolbar/search regression      PASS
Bulk top-control regression            PASS
Two-frame exact scroll regression      PASS
Earlier formula scroll regression      PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 22 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     3c563f64d530e6b79d0360d116a425f18879272a063bfb0e274bfd40022a1254
```

## عملیات

```text
SQL/Edge/Secret/Dependency جدید: ندارد
```
