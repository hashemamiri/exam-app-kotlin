# پچ جامع V9 — تقویم، پروفایل، تنظیمات، کیف پول و پرداخت

## آنچه واقعاً Native شده است

### تقویم جلالی و پیام هدف‌دار
- تبدیل آفلاین و تست‌شدهٔ میلادی ↔ جلالی با الگوریتم Borkowski
- بازهٔ سال ۱۴۰۰ تا ۱۵۰۰
- شبکهٔ ماه، امروز، جمعه، تعطیلات رسمی و تعداد پیام‌های هر روز
- دادهٔ رسمی تعطیلات ۱۴۰۳، ۱۴۰۴ و ۱۴۰۵ از جدول Supabase
- ثبت، ویرایش و حذف پیام روزانه توسط معلم
- مخاطب «همه»، «کلاس‌های انتخابی» یا «دانش‌آموزان انتخابی»
- دانش‌آموز فقط پیام مجاز خودش را از RPC مالک‌محور می‌گیرد
- کنترل سرور روی مالکیت تمام class/student idها، طول متن و بازه تاریخ

### پروفایل، آواتار و سربرگ
- بارگذاری و ذخیره پروفایل از RPC جدید، بدون خواندن `plain_password`
- انتخاب عکس با Photo Picker
- اصلاح EXIF، برش مرکزی مربع، resize و فشرده‌سازی واقعی
- مسیر استاندارد `avatars/{userId}/{uuid}` در bucket `exam-images`
- نمایش عکس/حرف اول در Drawer
- نام نمایشی معلم و اختیار نمایش آواتار به دانش‌آموز
- نمایش پروفایل عمومی معلم برای دانش‌آموز خودش
- ذخیره استان، شهر، منطقه و مدرسه برای سربرگ رسمی

### تنظیمات ظاهر
- حالت دستگاه، روشن یا تیره
- رنگ پویا در Android 12 به بالا
- اندازه متن ۸۵ تا ۱۳۰ درصد
- ذخیره محلی با DataStore؛ بدون token یا اطلاعات حساب
- اعمال فوری تنظیمات به کل Compose

### کیف پول و هزینه آزمون
- موجودی و ۵۰ گردش اخیر با `native_wallet_snapshot`
- واحد قطعی تمام مبالغ: تومان
- هزینه هر سؤال: ۱٬۰۰۰ تومان
- ذخیره آزمون و کسر موجودی در یک تراکنش PostgreSQL
- idempotency با `operation_id`: قطع پاسخ شبکه باعث کسر دوباره نمی‌شود
- ایجاد آزمون: همه سؤال‌ها مشمول هزینه
- ویرایش پیش از دریافت پاسخ: فقط سؤال افزوده
- ویرایش پس از دریافت پاسخ: سؤال افزوده یا تغییرکرده
- تکثیر: همه سؤال‌های کپی مشمول هزینه
- کسری موجودی، کل ذخیره را بدون تغییر و بدون کسر رد می‌کند

### پرداخت امن
- Edge Function واقعی `wallet-payment`
- پشتیبانی زرین‌پال و آیدی‌پی طبق API رسمی و حالت sandbox کنترل‌شده
- ساخت سفارش فقط برای JWT معتبر معلم
- تطبیق order، authority و amount هنگام callback
- تأیید بانکی در سرور و اعتباردهی service-role-only
- جلوگیری از پرداخت تکراری با قفل ردیف، ref یکتا و operation یکتا
- `wallet_topup` و `wallet_refund` مستقیم از نقش authenticated لغو می‌شوند
- هیچ service-role، merchant/API key، Header یا URL پرداخت در UI چاپ نمی‌شود

## فایل SQL الزامی

فقط فایل SQL زیر را در **Supabase SQL Editor** اجرا کنید؛ فایل Patch را هرگز آنجا paste نکنید:

```text
SQL_NATIVE_CALENDAR_PROFILE_WALLET_V9.sql
```

بررسی پس از اجرا:

```sql
select
  to_regprocedure('public.native_save_exam_v1(jsonb)') is not null as exam_wallet_ready,
  to_regprocedure('public.native_wallet_snapshot()') is not null as wallet_ready,
  to_regprocedure('public.native_save_profile(text,text,boolean,text,text,text,text)') is not null as profile_ready,
  to_regprocedure('public.cal_month(date,date)') is not null as calendar_ready,
  to_regprocedure('public.native_credit_wallet_payment(bigint,text)') is not null as payment_credit_ready;
```

هر پنج مقدار باید `true` باشند.

## استقرار Edge Function

بعد از اعمال Patch و از داخل پوشه پروژه:

```bash
npx supabase@latest login
npx supabase@latest link --project-ref eazwuyrymsvdkwckdpco
npx supabase@latest functions deploy wallet-payment --no-verify-jwt
```

`verify_jwt=false` فقط برای callback بانک است. درخواست ساخت سفارش در خود تابع دوباره با `auth.getUser` احراز هویت می‌شود.

### تست sandbox بدون پول واقعی

```bash
npx supabase@latest secrets set PAY_PROVIDER=sandbox PAY_ALLOW_SANDBOX=true
```

### درگاه واقعی

در Dashboard Supabase، Secretهای Edge Function را تنظیم کنید:

- زرین‌پال: `PAY_PROVIDER=zarinpal` و `PAY_ZARINPAL_MERCHANT`
- آیدی‌پی: `PAY_PROVIDER=idpay` و `PAY_IDPAY_API_KEY`
- برای محیط واقعی، `PAY_ALLOW_SANDBOX=false`

مقدار مرچنت/API key را در Chat، Git، APK، screenshot عمومی یا فایل پروژه نگذارید. فعال‌سازی درگاه واقعی نیازمند حساب تأییدشدهٔ پذیرنده و مدارک قانونی همان درگاه است. callback به‌طور خودکار از نشانی Edge Function ساخته می‌شود.

## نتیجه بررسی انجام‌شده

```text
Kotlin compile                         PASS
JVM tests                              26/26 PASS
Jalali reference + 101-year roundtrip  PASS
Wallet validation tests               PASS
PostgreSQL 17 migration               PASS
Migration second-run idempotency      PASS
Profile/avatar ownership              PASS
Calendar audience isolation           PASS
Atomic exam billing scenarios         PASS
Insufficient balance rollback         PASS
Exam/payment idempotency               PASS
Sensitive grants audit                PASS
safeupdate WHERE audit                 PASS
Deno TypeScript check                  PASS
assembleDebug                          BUILD SUCCESSFUL
lintDebug                              BUILD SUCCESSFUL (0 error)
APK Signature Scheme v2                Verified
GitHub workflow: unit test + Deno + lint + release build enabled
```

## محدودیت‌های شفاف

- درگاه واقعی بدون مرچنت/API key متعلق به مالک سامانه قابل فعال‌شدن نیست؛ کد کامل است ولی Secret باید فقط در Supabase تنظیم شود.
- قالب نهایی چندصفحه‌ای چاپ سربرگ در V10 تکمیل می‌شود؛ داده سربرگ اکنون واقعی و ماندگار است.
- حذف فایل آواتار قدیمی از Storage به پاک‌سازی orphan/reference-counting در V11 واگذار شده است.
- مسیرهای مستقیم قدیمی جدول‌ها تا hardening نهایی V11 کاملاً revoke نمی‌شوند؛ مسیر Native ساخت/ویرایش/تکثیر اکنون اتمیک و هزینه‌دار است.
- تاریخ رسمی سال‌های بعد از ۱۴۰۵ باید پس از انتشار تقویم رسمی در `holidays`/`holiday_years` افزوده شود؛ برنامه تاریخ قمری را حدس نمی‌زند.
