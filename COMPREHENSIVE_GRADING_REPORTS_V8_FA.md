# پچ جامع V8 — ارزیابی، حضور، آمار و گزارش

## امکانات معلم

### تصحیح
- انتخاب آزمون
- فهرست پاسخ‌ها
- مشاهده پاسخ هر سؤال و تصاویر ضمیمه
- نمره بین صفر و بارم
- بازخورد
- بانک بازخورد
- تأیید نمره خودکار
- لغو تأیید

### امنیت نمره
ثبت نمره با RPC جدید انجام می‌شود:

```text
native_save_grade(answerId, grades, feedback)
```

سرور مالکیت معلم، تعداد نمره‌ها و بازه هر نمره را کنترل و total را خودش محاسبه می‌کند.

### حضور و نظارت
- وضعیت حضور/شروع/ارسال/غیبت
- تعداد تلاش
- تمدید ۱۰ دقیقه‌ای
- اجازه تلاش مجدد با حفظ نسخه قبلی
- استفاده از RPCهای زنده `exam_attendance`, `exam_live_status`, `extend_student_time`, `reset_student_attempt`

### آمار و کارنامه
- تعداد آزمون، پاسخ، تصحیح و مانده
- میانگین درصد
- انتخاب کلاس و آزمون‌ها
- نمرات roster
- CSV سازگار با Excel
- چاپ Android و ذخیره PDF از PrintManager

## امکانات دانش‌آموز

- صفحه نتایج و پاسخ‌های من
- نمره، درصد و بازخورد
- میانگین کل
- خروجی CSV کارنامه
- استفاده از RPCهای `my_grades` و `my_answers`

## SQL

فایل زیر یک‌بار اجرا شود:

```text
supabase/migrations/20260811_native_grading_reports.sql
```

## تست

```text
Kotlin compile                    PASS
JVM tests                         19/19 PASS
AutoGrader tests                  2/2 PASS
PostgreSQL 17 integration         PASS
Invalid grade rejection           PASS
safeupdate audit                  PASS
assembleDebug                     BUILD SUCCESSFUL
lintDebug                         BUILD SUCCESSFUL
```

## محدودیت شفاف

- حالت سؤال‌محور گروهی و میان‌برهای صفحه‌کلید WebView هنوز عیناً منتقل نشده است.
- نمودار گرافیکی Chart.js با کارت آماری Native جایگزین شده؛ تحلیل دشواری پیشرفته در V10 تکمیل می‌شود.
- چاپ گزارش از HTML داخلی و PrintManager استفاده می‌کند؛ قالب رسمی چندصفحه‌ای در V10 نهایی می‌شود.
