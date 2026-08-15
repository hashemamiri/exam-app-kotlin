# V13.3 — مرحله سوم برابری کامل Native: مدیریت و گزارش

این Patch پس از V13.1 و V13.2 اعمال می‌شود و مجموعه سه‌مرحله‌ای را کامل می‌کند.

## امکانات
- Room نسخه ۴ و یادداشت خصوصی مالک‌محور برای هر دانش‌آموز
- ساخت گروهی ۱ تا ۱۰۰ حساب با Edge Function امن موجود، نتیجه موفق/ناموفق و رمز فقط همان بار
- خروجی واقعی `.xlsx` با OOXML و چند Sheet؛ CSV جعلی نیست
- Excel فهرست دانش‌آموزان، حساب‌های تازه، لیست نمرات و کارنامه
- نمودار خطی روند دانش‌آموز، نمودار میله‌ای وضعیت پاسخ‌ها و تحلیل سؤال
- وضعیت زنده آزمون با تازه‌سازی خودکار هر ۲۰ ثانیه
- نمایش summary در حال آزمون/تحویل/شروع‌نشده
- ویرایش و حذف امن بانک بازخورد با RPCهای V13.1
- `FINAL_NATIVE_VERIFY` برای تمام سه مرحله

## نیاز عملیاتی
- SQL جدید ندارد؛ توابع feedback در SQL مرحله V13.1 نصب شده‌اند.
- Edge deploy جدید ندارد؛ action ساخت گروهی در `manage-student` امن V11 از قبل موجود است.
- Secret جدید ندارد.

## تست نهایی مجموعه V13
```text
Kotlin compile                     PASS
JVM tests                          53/53 PASS
PostgreSQL 17 migration x2         PASS
V13_FULL_PARITY_PASS               PASS
FINAL_NATIVE_VERIFY                PASS
lintDebug                          PASS (0 error)
assembleDebug                      PASS
APK Signature Scheme v2            Verified
```

پس از موفقیت Build و تست دستگاه هر سه Patch، موارد فهرست «قابلیت‌های ناقص نسبت به WebView» پوشش داده شده‌اند. موارد ممنوع امنیتی مانند plain_password و نصب silent عمداً بازنمی‌گردند.
