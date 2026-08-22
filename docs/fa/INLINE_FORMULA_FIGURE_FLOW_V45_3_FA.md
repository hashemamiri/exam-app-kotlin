# اصلاح متن درون‌خطی فرمول و جریان درج شکل/نمودار — V45.3

**آخرین به‌روزرسانی:** ۲۰۲۶-۰۸-۲۲

## درخواست

در کادر متن سؤال باید بتوان متن را قبل و بعد از فرمول نوشت و فرمول بین دو
بخش جمله باقی بماند، نه این‌که به‌دلیل نبودن محل تایپ بعد از فرمول یا اندازهٔ
کادر، در سطر جداگانه قرار بگیرد. همچنین آیکن‌های «درج شکل» و «درج نمودار» نباید
پنجرهٔ مشترک شکل/نمودار را باز کنند.

## اصلاح متن و فرمول

- `RichTextSplitter` اکنون بخش‌های متنی خالی ابتدا و انتهای توکن‌ها را هم نگه
  می‌دارد.
- وقتی فرمول درج می‌شود، یک کادر متن خالی بعد از آن وجود دارد؛ کاربر می‌تواند
  ادامهٔ جمله را همان‌جا بنویسد.
- کادرهای متن دارای متن، اندازهٔ محتوای خود را در `FlowRow` می‌گیرند و دیگر با
  `fillMaxWidth` کل سطر را اشغال نمی‌کنند.
- در نتیجه ساختاری مانند زیر در متن باقی می‌ماند:

```text
متن اول  [فرمول]  ادامهٔ متن
```

فرمول همچنان در داده با قالب `$...$` ذخیره می‌شود و رندر نمایش نیز تغییری در
قالب ذخیره‌سازی ندارد.

## جریان جدید شکل و نمودار

### آیکن «درج شکل»

۱. فقط پنجرهٔ انتخاب **شکل‌های هندسی** باز می‌شود؛ فهرست نمودار در آن نیست.
۲. نوع شکل، مانند مثلث، دایره یا مربع، انتخاب می‌شود.
۳. پنجرهٔ **ویرایش شکل** همان نوع باز می‌شود.
۴. برچسب رأس‌ها، ضلع‌ها، زاویه‌ها و اندازه‌های مربوط و پیش‌نمایش قابل ویرایش
   است.
۵. با «درج شکل»، شکل در متن سؤال ثبت می‌شود.

### آیکن «درج نمودار»

۱. فقط پنجرهٔ انتخاب **نمودارها** باز می‌شود؛ فهرست شکل‌های هندسی در آن نیست.
۲. نوع نمودار، مانند خط، سهمی، سینوسی، نمایی یا ستونی، انتخاب می‌شود.
۳. پنجرهٔ **ویرایش نمودار** همان نوع باز می‌شود.
۴. پارامترهای نمودار، عنوان و پیش‌نمایش قابل ویرایش است.
۵. با «درج نمودار»، نمودار در متن سؤال ثبت می‌شود.

برای ویرایش شکل یا نمودار موجود، با لمس خود شکل، مستقیماً ویرایشگر همان نوع
باز می‌شود؛ انتخاب نوع دوباره لازم نیست.

## فایل‌های تغییرکرده

```text
app/src/main/java/ir/exam/app/core/text/RichText.kt
app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt
app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/test/java/ir/exam/app/core/text/RichTextTest.kt
app/src/test/java/ir/exam/app/ui/app/V45_3InlineFigureFlowTest.kt
scripts/verify_native_final.py
```

## بررسی

```text
FINAL_NATIVE_VERIFY  → PASS
بررسی source-based جریان درج → PASS
git diff --check       → PASS
./gradlew testDebugUnitTest lintDebug → پس از اعمال پچ در WSL/GitHub Actions اجرا شود
```

این تغییر SQL، Edge Function، Secret، Migration یا Dependency جدید ندارد.

## دستور اعمال در WSL

این پچ روی شاخه‌ای اعمال شود که V45.2.2 قبلاً روی آن اعمال شده است. فایل
`V45_3_inline_formula_figure_flow.patch` را در Downloads ویندوز قرار بده و
سپس اجرا کن:

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin

git apply /mnt/c/Users/Hashem/Downloads/V45_3_inline_formula_figure_flow.patch

git diff --check
python3 scripts/verify_native_final.py
./gradlew testDebugUnitTest lintDebug

git add -A
git diff --cached --stat
git commit -m "feat(v45.3): keep formulas inline and split figure flows"
git push origin HEAD
```

اگر پس از اعمال پچ خطای جدیدی آمد، متن کامل همان خطا را بفرست. برای خطای واقعی
دستگاه، فقط این دستور را اجرا کن و هیچ URL، Header، Token یا کلید محرمانه‌ای را
ارسال نکن:

```bash
adb logcat -d AndroidRuntime:E *:S
```
