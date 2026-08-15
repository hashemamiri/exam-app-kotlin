# V31 — رفع غیب‌شدن گزینه، مخاطبان در مشخصات آزمون، پیغام آپدیت، رفع کرش آپلود و پنجره گروهی بدون کلاس

**تاریخ:** ۲۰۲۶-۰۸-۱۵
**پیش‌نیاز:** V30
**SQL جدید:** ندارد
**Edge deploy:** ندارد
**Secret جدید:** ندارد

---

## ۱) رفع غیب‌شدن گزینه هنگام جابه‌جایی

### علت

انیمیشن جابه‌جایی سفارشی V30 (`AnimatedReorderColumn`) با فنر، موقعیت کارت را
با `graphicsLayer` جابه‌جا می‌کرد. هنگام درگ سریع، آفست‌های قبلی/جدید با هم
تداخل می‌کردند و کارت گزینه از دید خارج می‌شد (غیب می‌شد).

### اصلاح

```text
AnimatedReorderColumn حذف شد.
گزینه‌ها و موارد جورکردنی دوباره در ستون کلیدخوردهٔ پایدار (key(optionId) / key(itemId)) هستند.
رنگ فعال کارت هنگام درگ (primaryContainer) و قرارداد درگ کارت سؤال حفظ شد.
```

## ۲) مخاطبان آزمون داخل مشخصات آزمون

- کارت «مخاطبان آزمون» به داخل بخش «مشخصات آزمون» منتقل شد و با آن باز/بسته می‌شود.

## ۳) پیغام آپدیت جدید هنگام ورود

- با ورود به برنامه (پس از ورود به حساب)، بررسی بروزرسانی خودکار انجام می‌شود.
- اگر نسخهٔ جدید موجود باشد، یک پنجرهٔ پیام روی صفحه ظاهر می‌شود:
  عنوان «بروزرسانی جدید»، سه مورد اول لیست تغییرات، دکمهٔ «دریافت نسخه» و «بعداً».
- «دریافت نسخه» همان مسیر دانلود امن و نصب Android را اجرا می‌کند.

## ۴) رفع کرش هنگام آپلود تصویر

### علت

`SupabaseQuestionImageUploader` برخلاف `LocalImageRepository` هیچ محافظ حافظه
نداشت: تصویر بزرگ با `Bitmap.createBitmap/createScaledBitmap` می‌توانست
`OutOfMemoryError` بدهد که یک Error است، توسط runCatching گرفته نمی‌شود و
پروسه را می‌کشد.

### اصلاح

```text
حلقهٔ تلاش مجدد (MAX_ATTEMPTS=4) با گرفتن صریح OutOfMemoryError
بودجهٔ پیکسل بر اساس حافظهٔ آزاد واقعی JVM (سقف ۷ مگاپیکسل)
در تلاش‌های بعدی بودجه نصف و پیکسل‌ها RGB_565 می‌شوند
نتیجهٔ نهایی به پیام فارسی «حافظه دستگاه کافی نیست» تبدیل می‌شود نه کرش
```

## ۵) پنجره گروهی بدون کلاس

```text
کلاس‌ها به‌کلی از پنجره حذف شدند؛ دانش‌آموزها بدون نیاز به انتخاب کلاس ثبت می‌شوند
(Edge Function موجود کلاس خالی را پشتیبانی می‌کند).
زیر دکمه‌ها فقط لیست شمارهٔ کارت‌ها نمایش داده می‌شود (بدون اسکرول؛ در ردیف‌های شش‌تایی).
چیدمان کارت دو ستونه:
  نام + نام خانوادگی
  نام پدر + نام کاربری
  پایه + رشته
  رمز + رمز فعلی
کادر «رمز فعلی» رمز تعیین‌شده را در خود ثبت می‌کند و با تغییر رمز خودکار به‌روز می‌شود.
دکمهٔ کپی روی کارت دانش‌آموز، رمز را از همین کادر برمی‌دارد و به‌صورت حساس
(IS_SENSITIVE) کپی می‌کند؛ اگر رمزی در دسترس نباشد، پیام «قابل بازیابی نیست» می‌ماند.
```

## امنیت رمز دانش‌آموز

رمز قبلی Supabase Auth غیرقابل بازیابی است و `plain_password` بازنمی‌گردد.
رمزهای تازه‌تنظیم‌شده فقط در حافظهٔ نشست نگه داشته می‌شوند و کپی آن‌ها
با پرچم حساس Clipboard انجام می‌شود.

## فایل‌های کلیدی

```text
docs/fa/STABLE_REORDER_UPDATE_PROMPT_V31_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt
app/src/main/java/ir/exam/app/domain/repository/SchoolRepository.kt
app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/test/java/ir/exam/app/ui/app/V31StableReorderUpdatePromptBulkTest.kt
scripts/verify_native_final.py
```

## تست

```text
Kotlin compile             PASS
JVM tests                  226/226 PASS
V31 regression tests        10/10 PASS
FINAL_NATIVE_VERIFY        PASS
lintDebug                  PASS — 0 error
assembleDebug              PASS
Debug package              ir.exam.app.native
APK Signature Scheme v2    Verified
```

SQL/Edge/Secret/Dependency جدید ندارد.
