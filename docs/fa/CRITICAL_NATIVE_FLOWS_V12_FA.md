# پچ جامع V12 — رفع مسیرهای حیاتی Native

## هدف

این Patch پنج شکاف حیاتیِ تأییدشده در ممیزی سورس Native را یکجا رفع می‌کند:

1. ورود دانش‌آموز با همان نام کاربری تحویلی معلم
2. ثبت‌نام، بازیابی رمز و مدیریت حساب معلم
3. اجرای تصادفی‌سازی، پیام معلم و مهلت واقعی سرور در آزمون
4. ادامه آزمون و پیش‌نویس پس از بسته‌شدن یا process death
5. مشاهده امن جزئیات پاسخ‌های قبلی دانش‌آموز

این Patch شامل Kotlin، Room migration، SQL امن، تست JVM، تست PostgreSQL و به‌روزرسانی هندآف است؛ Edge Function یا Secret تازه ندارد.

---

## ۱) ورود و حساب

### ورود دانش‌آموز

نام کاربری مثل `student_25` فقط داخل لایه Auth به ایمیل داخلی مدیریت‌شده تبدیل می‌شود. دامنه داخلی در UI نمایش داده نمی‌شود و معلم همان نام کاربری عادی را به دانش‌آموز می‌دهد.

- معلم: ایمیل + رمز یا OTP
- دانش‌آموز: نام کاربری + رمز
- OTP ورود موجود با `createUser=false` اجرا می‌شود تا حساب ناخواسته ساخته نشود.

### ثبت‌نام معلم

```text
نام و ایمیل
→ OTP ایمیل
→ نام کاربری و رمز ۸ تا ۷۲ کاراکتر
→ native_complete_teacher_registration_v1
→ نقش teacher
```

- حساب‌های مدیریت‌شده دانش‌آموز با دامنه داخلی امکان self-promotion ندارند.
- حساب عضو کلاس یا دارای `teacher_id` به معلم تبدیل نمی‌شود.
- نام کاربری با advisory lock و کنترل تکراری بودن ذخیره می‌شود.
- اگر برنامه بعد از تأیید OTP و قبل از تکمیل بسته شود، `native_my_registration_state_v1` مرحله تکمیل را بازیابی می‌کند و کاربر اشتباهاً وارد داشبورد دانش‌آموز نمی‌شود.

### بازیابی و تغییر رمز

- بازیابی با OTP ایمیل حساب موجود و `createUser=false`
- تعیین رمز تازه فقط بعد از نشست Auth تأییدشده
- تغییر رمز از «پروفایل و تنظیمات → حساب»
- تغییر نام کاربری نمایشی معلم با RPC مالک‌محور
- رمز قبلی هرگز نمایش، خوانده یا در دیتابیس ذخیره نمی‌شود.

---

## ۲) اجرای صحیح آزمون دانش‌آموز

`StudentExamPayloadCodec` اکنون این فیلدهای واقعی سرور را مصرف می‌کند:

```text
shuffle_q
shuffle_opt
teacher_message
server_now
expires_at
attempts_allowed
attempt_no / attempts_remaining (در صورت وجود در پاسخ سرور)
```

### تصادفی‌سازی امن

- ترتیب سؤال و گزینه برای هر دانش‌آموز و آزمون پایدار است.
- گزینه نمایشی به اندیس اصلی سرور نگاشت می‌شود.
- پاسخ جورکردنی نیز نگاشت ستون راست را حفظ می‌کند.
- پیش از ارسال، سؤال‌ها بر اساس اندیس اصلی مرتب می‌شوند؛ در نتیجه تصادفی‌سازی کلید تصحیح را خراب نمی‌کند.

### تایمر سرور

- زمان باقی‌مانده از اختلاف `expires_at - server_now` محاسبه می‌شود، نه ساعت خام گوشی.
- deadline محلی متناظر در Room نگهداری می‌شود و پس از بازشدن دوباره از ابتدا شروع نمی‌شود.
- هنگام رسیدن به صفر، یک بار زمان سرور تازه می‌شود تا تمدید معلم اعمال شود.
- آزمون بدون مدت با برچسب «بدون محدودیت» نمایش داده می‌شود و خودکار ارسال نمی‌شود.

### صفحه پیش از شروع

پیش از پاسخ‌گویی نمایش داده می‌شود:

- عنوان و درس
- تعداد سؤال
- زمان باقی‌مانده
- پیام معلم
- اطلاعات تلاش در صورت ارسال سرور
- وضعیت «آزمون نیمه‌تمام بازیابی شد»

---

## ۳) Room و ادامه آزمون

Room از نسخه ۲ به ۳ مهاجرت می‌کند و جدول زیر را می‌سازد:

```text
active_exam_sessions
```

ذخیره می‌شود:

- شناسه مالک حساب
- شناسه و کد آزمون
- payload امن بدون answer key
- deadline محلی متناظر سرور
- زمان ذخیره

قواعد:

- داده هر حساب جداست.
- answer key پیش از cache صریحاً حذف می‌شود.
- اولین ورود به آزمون ناشناخته همچنان اینترنت می‌خواهد.
- آزمونی که قبلاً با سرور باز شده، همراه پیش‌نویس در قطع اینترنت قابل ادامه است.
- پس از ارسال مستقیم یا صف‌شدن نهایی، نشست فعال پاک می‌شود.
- پیش‌نویس صف‌شده تا رسید واقعی WorkManager نگهداری می‌شود.

---

## ۴) جزئیات پاسخ دانش‌آموز

دو RPC تازه:

```text
native_my_answers_v1()
native_my_answer_detail_v1(text)
```

امنیت:

- فقط `student_id = auth.uid()` قابل مشاهده است.
- پاسخ کاربر دیگر خطا می‌دهد.
- تا قبل از `graded=true`، answer key و explanation از پاسخ حذف می‌شوند.
- بعد از تصحیح، پاسخ صحیح، نمره هر سؤال، بازخورد و تصاویر پاسخ قابل مشاهده‌اند.
- همه توابع از PUBLIC و anon revoke و فقط به authenticated grant شده‌اند.

UI اکنون برای هر ارسال این موارد را نشان می‌دهد:

- سؤال و پاسخ دانش‌آموز
- پاسخ چهارگزینه‌ای به‌صورت متن گزینه
- پاسخ صحیح/غلط
- پاسخ جورکردنی قدیمی یا Native
- تصاویر پاسخ
- نمره هر سؤال بعد از تصحیح
- پاسخ صحیح و توضیح فقط بعد از تصحیح

---

## SQL الزامی

فقط فایل زیر را یک بار در SQL Editor پروژه اصلی اجرا کنید:

```text
SQL_NATIVE_CRITICAL_FLOWS_V12.sql
```

بررسی readiness:

```sql
select
  to_regprocedure('public.native_complete_teacher_registration_v1(text,text)') is not null as teacher_signup_ready,
  to_regprocedure('public.native_update_my_username_v1(text)') is not null as username_update_ready,
  to_regprocedure('public.native_my_registration_state_v1()') is not null as registration_resume_ready,
  to_regprocedure('public.native_my_answers_v1()') is not null as answer_list_ready,
  to_regprocedure('public.native_my_answer_detail_v1(text)') is not null as answer_detail_ready;
```

هر پنج مقدار باید `true` باشند.

این SQL:

- هیچ Secret یا رمز ندارد.
- Edge Function جدید نمی‌خواهد.
- V9/V10/V11 را دوباره اجرا نمی‌کند.
- قبل از نصب APK جدید اجرا شود تا مسیر ثبت‌نام و پاسخ‌ها آماده باشد.

---

## نتیجه تست مستقل

```text
Kotlin compile                                  PASS
JVM tests                                       50/50 PASS
Student username mapping regression             PASS
Teacher registration/recovery state regression  PASS
Stable question/option shuffle regression       PASS
Canonical answer order regression               PASS
Answer-key cache stripping regression           PASS
Process-death exam/draft restore regression      PASS
PostgreSQL 17 migration first run               PASS
PostgreSQL 17 migration second run              PASS
Teacher self-promotion guards                   PASS
Ungraded answer-key non-disclosure               PASS
Cross-student answer denial                      PASS
Function revoke/grant audit                      PASS
FINAL_NATIVE_VERIFY                             PASS
lintDebug                                       BUILD SUCCESSFUL (0 error)
assembleDebug                                   BUILD SUCCESSFUL
Debug package                                   ir.exam.app.native
APK Signature Scheme v2                         Verified
```

---

## تست دستگاه پس از انتشار

1. دانش‌آموز فقط با نام کاربری و رمز وارد شود.
2. معلم موجود با ایمیل و رمز و همچنین OTP وارد شود.
3. مسیر «رمز را فراموش کرده‌ام» تا تعیین رمز تازه کامل شود.
4. ثبت‌نام آزمایشی معلم با ایمیل مجاز، OTP و تعیین رمز کامل شود.
5. آزمون دارای `shuffle_q` و `shuffle_opt` روی دو بار ورود همان ترتیب پایدار را داشته باشد.
6. پیام معلم پیش از شروع نمایش داده شود.
7. برنامه وسط آزمون بسته و دوباره باز شود؛ پاسخ‌ها و زمان قبلی بازیابی شوند.
8. در «نتایج و پاسخ‌های من»، پاسخ تصحیح‌نشده کلید را نشان ندهد.
9. همان پاسخ بعد از تصحیح، نمره و پاسخ صحیح را نشان دهد.
10. آزمون آنلاین و صف آفلاین هر دو رسید قبلی خود را حفظ کنند.
