# هماهنگ‌سازی دو تست رگرسیون قدیمی CI — V45.2.2

**آخرین به‌روزرسانی:** ۲۰۲۶-۰۸-۲۲

## گزارش واقعی GitHub Actions

در اجرای زیر:

```text
Run ./gradlew testDebugUnitTest lintDebug
...
> Task :app:compileDebugKotlin
> Task :app:testDebugUnitTest

V29ReorderViewerEditBulkTest > formula icon sits in the same row as the question camera FAILED
    java.lang.AssertionError at V29ReorderViewerEditBulkTest.kt:76

V31StableReorderUpdatePromptBulkTest > app entry checks for updates and shows a prompt when one exists FAILED
    java.lang.AssertionError at V31StableReorderUpdatePromptBulkTest.kt:91

313 tests completed, 2 failed
> Task :app:testDebugUnitTest FAILED
```

کامپایل Kotlin، پردازش KSP و lint تا مرحلهٔ اجرای تست‌ها پیش رفتند. پیام
`daemon has terminated unexpectedly ... code: 0` در شروع daemon علت شکست نهایی
نبود؛ daemon دوباره آماده شد و تست‌ها اجرا شدند. هشدارهای deprecated نیز خطا
نیستند.

## علت قطعی هر دو شکست

### ۱) تست V29

تست قدیمی هنوز قرارداد V29 را می‌خواست: وجود `Icons.Outlined.Functions` داخل
`QuestionMediaEditor.kt` و پارامتر `onFormula`. اما در V45 این جابه‌جایی عمداً
انجام شده است:

- `QuestionMediaEditor` فقط ردیف دوربین و رسانهٔ سؤال را نگه می‌دارد.
- آیکن فرمول در `InlineMathTextEditor` و زیر کادر متن سؤال قرار دارد.
- اتصال آن به سؤال از `ExamBuilderScreen` با `onInsertFormula` انجام می‌شود.

بنابراین خط ۷۶ یک assertion منسوخ‌شده بود، نه خرابی قابلیت درج فرمول.

### ۲) تست V31

تست قدیمی به‌دنبال فراخوانی مستقیم
`updateViewModel.downloadAndInstall()` داخل `onClick` می‌گشت. در V45.1 این
طراحی عمداً به callback دیالوگ منتقل شده است:

```text
onDownload = updateViewModel::downloadAndInstall
```

این کار لازم است تا دیالوگ هنگام دانلود باز بماند، پیشرفت و خطا را نشان دهد و
نصب خودکار پس از دریافت انجام شود. بنابراین خط ۹۱ هم فقط به‌دلیل انتظار ساختار
قدیمی شکست خورده بود.

## اصلاح V45.2.2

- `V29ReorderViewerEditBulkTest.kt` اکنون دوربین را در
  `QuestionMediaEditor` و آیکن فرمول را در `InlineMathTextEditor` بررسی می‌کند.
- `V31StableReorderUpdatePromptBulkTest.kt` اکنون اتصال callback واقعی
  `onDownload = updateViewModel::downloadAndInstall` را بررسی می‌کند.
- کد اجرایی برنامه تغییر نکرده است؛ این پچ فقط تست‌های منبع‌محور قدیمی و
  مستندات را با طراحی قطعی V45/V45.1 هماهنگ می‌کند.

## بررسی انجام‌شده

```text
FINAL_NATIVE_VERIFY  → باید PASS بماند
assertionهای به‌روزشدهٔ V29 و V31 → با سورس فعلی منطبق شدند
لینت متنی پچ (git diff --check) → باید PASS باشد
testDebugUnitTest / lintDebug → پس از اعمال پچ در WSL یا GitHub Actions اجرا شود
```

این پچ هیچ SQL، Edge Function، Secret، Migration یا Dependency جدیدی ندارد.

## دستور اعمال در WSL

این پچ برای مخزنی است که V45.2.1 روی آن اعمال شده است؛ گزارش فعلی CI نشان
می‌دهد اصلاح ثابت `DownloadManager` قبلاً وارد شده است.

فایل `V45_2_2_regression_test_alignment.patch` را در پوشهٔ Downloads ویندوز
قرار بده، سپس در WSL دقیقاً این دستورها را اجرا کن:

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin

git apply /mnt/c/Users/Hashem/Downloads/V45_2_2_regression_test_alignment.patch

git diff --check
python3 scripts/verify_native_final.py

git add -A
git diff --cached --stat
git commit -m "test(v45.2.2): align legacy regression tests"
git push origin HEAD
```

پس از اجرای workflow جدید، خروجی کامل را بفرست. اگر تست‌ها سبز شدند، مرحلهٔ
بعدی بررسی `assembleRelease`، انتشار APK و آزمایش واقعی دانلود روی دستگاه است.
برای هر خطای جدید دستگاه، فقط خروجی واقعی زیر را بفرست و هیچ URL، Header، Token
یا کلید محرمانه‌ای را ارسال نکن:

```bash
adb logcat -d AndroidRuntime:E *:S
```
