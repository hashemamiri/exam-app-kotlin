# V45.4 — ویرایشگر فرمول WebView (استخراج کد به کد از 66.html)

**تاریخ:** ۲۰۲۶-۰۸-۲۲
**پیش‌نیاز:** V45.3

## چه چیزی تغییر کرد؟

ویرایشگر فرمول بومی (دیالوگ Compose با پد دکمه‌ای) حذف و با **همان صفحهٔ
ویرایشگر فرمول نسخهٔ وب 66.html** جایگزین شد. صفحهٔ وب **بدون هیچ تغییری**
استفاده می‌شود: رشتهٔ `MATH_EDITOR_HTML` از 66.html استخراج و
بایت‌به‌بایت در `app/src/main/assets/math_editor_standalone.html` ذخیره شد.

نکتهٔ مهم: **نمایش فرمول‌ها** (در متن سؤال، گزینه‌ها، تصحیح، گزارش، آزمون
دانش‌آموز و چاپ PDF) همچنان **Native** است و تغییری نکرده. فقط ورودی/ویرایش
فرمول از طریق WebView انجام می‌شود.

## فایل استخراج‌شده

| مشخصه | مقدار |
|---|---|
| مسیر asset | `app/src/main/assets/math_editor_standalone.html` |
| حجم | ۶۲۴,۲۰۹ بایت |
| SHA-256 | `aae5777f9fb8705ccb2ed4a7c52e426e44ab45c7280055f936ed0aff4e917ceb` |
| روش استخراج | دو مفسر مستقل (Python و Node.js) — خروجی یکسان |
| منابع خارجی | هیچ (کاملاً آفلاین؛ فقط localStorage برای فرمول‌های اخیر) |

تأیید سریع یکپارچگی:

```bash
sha256sum app/src/main/assets/math_editor_standalone.html
```

## فایل‌های حذف‌شده

```text
ui/math/FormulaEditorDialog.kt / FormulaSmartHubDialog.kt /
FormulaLibraryDialog.kt / FormulaLibraryNavigator.kt /
FormulaReferenceLibrary.kt / FormulaReferenceStore.kt / FormulaSmartReference.kt
core/math/FormulaBoxEditor.kt / FormulaMatrixFactory.kt
assets/formula_library_v13.json
tests: FormulaBoxEditorTest / FormulaMatrixFactoryTest /
FormulaReferenceAssetTest / FormulaLibraryNavigatorTest / FormulaSmartReferenceTest
```

همهٔ این‌ها فقط توسط ویرایشگر بومی استفاده می‌شدند؛ هیچ ارجاع دیگری در
`app/src` به آن‌ها باقی نمانده (با grep سراسری تأیید شد).

## فایل جدید: MathEditorWebViewDialog.kt

`app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt`

- امضا دقیقاً همان `FormulaEditorDialog` قدیمی است:
  `MathEditorWebViewDialog(initialTex: String, onDismiss: () -> Unit, onInsert: (String) -> Unit)`
- نقطهٔ اتصال در `ExamBuilderScreen.kt` فقط import و نام دیالوگ تغییر کرده؛
  `FormulaTarget` و `viewModel.insertFormula(...)` دست‌نخورده‌اند.

### جریان هدایت صفحهٔ وب

```text
۱) qTxt_1.value = "$formula" + "$"   ← فرمول فعلی؛ برای درج جدید "$$"
   کل متن انتخاب می‌شود (selection تمام $...$)
۲) openMath('qTxt_1')   ← ویرایشگر با همان فرمول قبلی باز می‌شود
۳) دکمهٔ ثبت (mfApply): کل انتخاب با "$فرمول جدید$" جایگزین و سپس closeMath
   → Bridge مقدار نهایی را می‌گیرد → $...$ باز می‌شود → onInsert(tex)
۴) بستن بدون ثبت (closeMath) → onDismiss
```

- همان الگوی `66.html` (bridge میزبان): فقط `mfApply` و `closeMath` در لحظهٔ
  انتها wrap می‌شوند؛ در خود asset هیچ تغییری نیست.
- رویدادهای ثبت/بستن با هم تداخل نمی‌کنند (فلاگ `__mbApplyInFlight` +
  `AtomicBoolean` سمت اندروید).
- فوکوس و انتخاب در دست خود صفحهٔ وب است (پروتکل `qTxt_1`).

### تنظیمات WebView

```text
javascriptEnabled  : true      (لازم)
domStorageEnabled  : true      (فرمول‌های اخیر)
allowFileAccess    : true      (فقط asset محلی)
allowContentAccess : false
navigations        : مسدود    (shouldOverrideUrlLoading → true)
poll آماده‌شدن      : 80ms × تا 100 بار (8 ثانیه) — مثل میزبان 66.html
دکمهٔ بازگشت       : closeMath → بستن بدون ثبت
خطا                : پیام + «تلاش مجدد»
```

## رندر/چاپ (بدون تغییر)

```text
NativeFormulaView.kt / NativeMathText.kt        نمایش
NativeMathAst / NativeMathSvgRenderer / NativeMathCanvasRenderer
NativeMathFormatter / NativeNaturalMathConverter / FormulaTextCodec
OfficialPdfPrintAdapter.kt / PdfExamRenderer.kt چاپ PDF
ExistingFormulaEditor (FormulaInlineEditor.kt)  چیپ فرمول‌های موجود
InlineMathTextEditor.kt                         کادر متن سؤال
```

## تست

`V19InteractionTest.kt`:

```kotlin
@Test fun `formula editor dialog hosts the standalone web editor untouched`()
- assert math_editor_standalone.html و AndroidMathBridge و javaScriptEnabled
  و openMath('qTxt_1') در MathEditorWebViewDialog.kt
- assert وجود function openMath(targetId) و function mfApply() در asset
```

## آزمایش دستی پیشنهادی (روی دستگاه/شبیه‌ساز)

```text
۱) ساخت آزمون جدید → سؤال چندگزینه‌ای
۲) دکمهٔ «∑ فرمول» در کادر متن سؤال → ویرایشگر وب باز شود
۳) درج «فرمول جدید» → ثبت → $...$ در متن سؤال (NativeMathText) دیده شود
۴) لمس/ویرایش همان فرمول → همان محتوا دوباره در ویرایشگر باز شود
۵) فرمول‌های «اخیر» صفحهٔ ویرایشگر بین جلسات حفظ شوند (localStorage)
۶) دکمهٔ بازگشت Android → بستن بدون ثبت
۷) گزینه‌ها و جورکردنی: ویرایش فرمول موجود در گزینه
۸) پیش‌نمایش/چاپ: نمایش فرمول چاپی همچنان Native باشد
```

## دستورهای WSL (پس از دریافت پچ)

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin && \
git diff --check && \
git add app/src/main/assets/math_editor_standalone.html \
        app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt && \
git add -A && \
git commit -m "feat(v45.4): replace native formula editor with WebView hosting the untouched standalone editor from 66.html" && \
git push origin HEAD
```

## نکتهٔ نگهداری asset

```text
.gitattributes:
  app/src/main/assets/math_editor_standalone.html -diff -text
```

asset به‌عنوان باینری «تغییرناپذیر» ثبت شده تا:
- `git diff --check` هرگز روی فاصله‌های انتهاییِ اصیل صفحهٔ وب (که بخشی از
  بایت‌های 66.html هستند) اعتراض نکند؛
- هر تغییر ناخواسته در صفحهٔ استخراج‌شده در diff به‌صورت «باینری» دیده شود.

در صورت نیاز به بروزرسانی ویرایشگر از نسخهٔ جدید وب، کل فرایند استخراج
کد به کد باید دوباره انجام و SHA-256 جدید در همین سند و بخش ۹۰ هندآف ثبت شود.

## هات‌فیکس V45.4.1

CI روی `scripts/verify_native_final.py` شکست (ارجاع به `FormulaEditorDialog.kt` حذف‌شده).
اسکریپت هماهنگ شد: بررسی‌های ویرایشگر بومی حذف، `android.webkit` فقط برای
`MathEditorWebViewDialog.kt` مجاز و بررسی‌های جدید asset/bridge افزوده شد.
نتیجه: `FINAL_NATIVE_VERIFY=PASS` — بخش ۹۱ هندآف را ببینید.
