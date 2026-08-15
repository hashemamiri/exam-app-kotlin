# V16.1 — شبکه کارت‌های منو، دکمه + دایره و Pull-to-Refresh

**تاریخ:** ۲۰۲۶-۰۸-۱۳

**پیش‌نیاز:** V16

## درخواست

```text
کارت پروفایل، اولین و بزرگ‌ترین کارت منوی همبرگری باشد.
سایر کارت‌ها در شبکه دو ستونه و هر ردیف دقیقاً دو کارت باشند.
دکمه + دایره کامل باشد.
دکمه بروزرسانی داشبورد معلم حذف و کشیدن رو به پایین جایگزین شود.
```

## Drawer

### کارت پروفایل

- اولین عنصر Drawer است؛
- عرض کامل یک سطر را می‌گیرد؛
- ارتفاع آن `148dp` است؛
- کارت‌های دیگر `116dp` هستند؛
- آواتار واقعی، نام، نقش و توضیح تنظیمات را نشان می‌دهد؛
- لمس کارت، صفحه واقعی «پروفایل و تنظیمات» را باز می‌کند.

### شبکه دو ستونه

قرارداد ثابت:

```text
Columns:              2
Teacher cards:       10 = 5 × 2
Student cards:        6 = 3 × 2
Incomplete rows:      0
```

کارت‌های معلم:

```text
داشبورد معلم      | تقویم و پیام‌ها
کلاس و دانش‌آموز  | تصحیح و حضور
آمار و گزارش‌ها   | کیف پول
تنظیمات           | درباره و بروزرسانی
آزمون جدید        | خروج
```

کارت‌های دانش‌آموز:

```text
داشبورد دانش‌آموز | تقویم و پیام‌ها
نتایج من          | تنظیمات
درباره و بروزرسانی| خروج
```

همه مقصدها واقعی‌اند. «آزمون جدید» مستقیماً Builder را باز می‌کند و هیچ صفحه demo یا placeholder اضافه نشده است.

## دکمه مرکزی +

- اندازه بیرونی دقیق `70 × 70dp`؛
- شعاع سایه `35dp`؛
- لایه داخلی با `CircleShape` و `clip=true`؛
- گرادیان و چرخش فقط داخل دایره؛
- عملکرد کمان سه‌گانه V15/V16 بدون تغییر حفظ شده است.

## داشبورد معلم

دکمه دستی «به‌روزرسانی» حذف شد. کل محتوای داشبورد داخل `PullToRefreshBox` و یک `LazyColumn` واحد قرار گرفت.

رفتار:

```text
کشیدن صفحه از بالاترین نقطه به پایین
→ viewModel.load()
→ نمایش indicator استاندارد Material 3
→ دریافت مجدد آزمون‌های واقعی
→ حفظ فهرست قبلی هنگام refresh تا رسیدن پاسخ جدید
```

دکمه‌های ساخت آزمون، import، ویرایش، باز/بستن، تکثیر، export، چاپ و حذف حفظ شده‌اند.

## تست

```text
Large profile height                         PASS
Two-column contract                          PASS
Teacher 10 cards / 5 complete rows           PASS
Student 6 cards / 3 complete rows            PASS
All menu actions real                        PASS
Perfect circular + markers                   PASS
PullToRefreshBox → viewModel.load             PASS
Manual refresh button absent                 PASS
Kotlin compile                               PASS
JVM tests                                    99/99 PASS
FINAL_NATIVE_VERIFY                          PASS
lintDebug                         PASS — 0 error, 24 warning
assembleDebug                                PASS
APK Signature Scheme v2                      Verified
Debug APK SHA-256                            9e5ea5d0276bb833f36cf45c1ce3e9c2ea47e1aca1fbe4b2ad4ea1f9a84690f9
```

## عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
```
