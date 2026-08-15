# V13.2 — مرحله دوم برابری کامل Native: تجربه دانش‌آموز

این Patch مستقل پس از V13.1 اعمال می‌شود.

## امکانات
- شبکه ناوبری همه سؤال‌ها با وضعیت پاسخ‌داده‌شده
- علامت‌گذاری سؤال برای مرور
- ذخیره علامت‌ها و آخرین سؤال در Room draft و بازیابی پس از process death
- صفحه مرور نهایی با تعداد پاسخ‌ها، سؤال‌های بی‌پاسخ و علامت‌دار
- تأیید صریح پیش از ارسال نهایی
- محافظ Back/Exit با هشدار اینکه زمان سرور ادامه دارد
- نمایش کامل style سؤال و فونت/تراز منتقل‌شده از Builder
- ویرایش crop/rotate واقعی تصاویر پاسخ قبل از ذخیره و صف آفلاین
- حفظ رفتار timer/server deadline و WorkManager V12

## تست
```text
Kotlin compile            PASS
JVM tests                 52/52 PASS
lintDebug                 PASS (0 error)
assembleDebug             PASS
APK Signature v2          Verified
```

SQL یا Edge Function جدید ندارد؛ SQL مرحله اول کافی است.
