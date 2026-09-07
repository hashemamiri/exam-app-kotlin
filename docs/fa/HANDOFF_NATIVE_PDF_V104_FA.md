# تحویل V104 — PDF بومیِ واحد برای پیش‌نمایش و چاپ آزمون

## نتیجهٔ این انتقال

مسیر چاپ آزمون اکنون فقط Native است. به‌جز `FormulaHostDialog` و asset محلی
`formula_editor/formula.html`، هیچ `WebView` یا HTML در production باقی نمانده
است.

این موارد حذف شده‌اند:

- `exam_print.html` و `exam_print_renderer.html`
- `ExamHtmlPrintDialog`، `ExamHtmlPrintPayload` و `ExamHtmlImageInliner`
- `HeadlessExamPrinter`، bridge جاوااسکریپت و
  `WebView.createPrintDocumentAdapter`
- پیش‌نمایش A4 جداگانهٔ Compose/HTML و ویرایشگر سند Word-مانندِ بازنشسته
- route قدیمی چاپ آزمون از dashboard و `OfficialPrintController.printExam`

`OfficialPrintController` فقط چاپ کارنامه را نگه می‌دارد. چاپ آزمون از سازنده و
Print Center هر دو به `NativeExamPrintLauncher` می‌رسد.

## معماری فعال

```text
OfficialExamPrintable + header fields
          │
          ├─ OfficialExamImageLoader
          │  (تصویر private / content / file → Bitmap)
          ▼
NativeExamPdfDocumentFactory
          │
          │  یک PDF A4 immutable
          ├───────────────┬────────────────────────┐
          ▼               ▼                        ▼
PdfRenderer          overlay Native       NativeExamPdfPrintAdapter
پیش‌نمایش            move/resize/edit    کپی همان pdfFile به Print Framework
```

- `NativeExamPdfPreviewDialog` فقط صفحه‌های همان فایل PDF را با
  `android.graphics.pdf.PdfRenderer` نشان می‌دهد. Canvas روی آن صرفاً hit-target
  و gesture است؛ متن، فرمول، شکل یا page-break را دوباره layout نمی‌کند.
- دکمهٔ «چاپ» داخل preview و چاپ مستقیم از Print Center، همان PDF آماده را به
  adapter می‌دهند؛ adapter فقط byteهای فایل را کپی می‌کند و renderer دوم ندارد.
- سربرگ‌های ذخیره‌شدهٔ هفت‌قالبی از `PrintHeaderStore` و
  `header_settings_schema.json` در `NativeExamHeader` رسم می‌شوند.

## تعامل‌های حفظ‌شده، از جمله page break

- **Move شکل:** کشیدن شکل، آن را به حالت آزاد تبدیل می‌کند و x/y/width/height را
  با میلی‌متر PDF، نسبت به ابتدای همان سؤال، در `figLayoutsJson` نگه می‌دارد.
- **Resize شکل:** کشیدن گوشهٔ پایینِ بخش آخر شکل اندازه را تغییر می‌دهد. شکل
  inline تا زمان جابه‌جایی در جریان متن باقی می‌ماند.
- **شکل split‌شده بین صفحه‌ها:** metadata هم `bounds` قابل‌دیدنِ crop صفحه را
  دارد و هم `flowBounds` کاملِ سند پیوسته را. بنابراین drag از هر بخشِ شکل، کل
  شکل را جابه‌جا می‌کند و resize از ارتفاع crop‌شده حساب نمی‌شود. روی cropهای
  میانی/اول handle تغییر اندازه دیده نمی‌شود؛ فقط صفحه‌ای که انتهای واقعی شکل
  را نمایش می‌دهد handle دارد. حرکت عمودی از مرز صفحه عبور می‌کند و به جریان
  پیوستهٔ PDF اعمال می‌شود.
- **ویرایش شکل:** دو ضربه، `ExamFigureToolHost` و ابزارهای Native هندسه، نمودار،
  جدول، اطلس و جدول تناوبی را باز می‌کند؛ token تازه به ViewModel برمی‌گردد.
- **فاصلهٔ سؤال:** کشیدن خط آبی بین سؤال‌ها، `sepExtraPx` را پایدار می‌کند.
- **دادهٔ قبلی:** `PrintPreviewLayoutCodec` چیدمان HTML قدیمیِ
  `{x,y,w,h,free}` را فقط هنگام خواندن به میلی‌متر تبدیل می‌کند؛ تمام ذخیره‌های
  جدید `nativePdf=true` دارند.

## قابلیت‌های PDF باقی‌مانده

`OfficialPrintLayoutEngine` هنوز منبع رسم متن RTL، فرمول، شکل، نمودار، اطلس،
تصویر خصوصی، گزینه‌ها، جورکردنی، پاسخ‌نامه، خطوط پاسخ و برش A4 است. هنگام page
slicing، extent واقعی تصویر آزاد هم به `total` و هم به draw window داده می‌شود؛
پس شکلی که پایین‌تر از slot اولیه کشیده شده در صفحهٔ مقصد واقعاً رسم می‌شود.
ارتفاع صریح `imageHeightMm` هم برای شکل آزاد و هم inline مصرف می‌شود؛ در نبود آن
نسبت طبیعی bitmap و cap قبلی حفظ می‌شود.

## فایل‌های اصلی

- `app/src/main/java/ir/exam/app/ui/printing/NativeExamPdfDocument.kt`
- `app/src/main/java/ir/exam/app/ui/printing/NativeExamPdfPreviewDialog.kt`
- `app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt`
- `app/src/main/java/ir/exam/app/core/printing/OfficialExamImageLoader.kt`
- `app/src/main/java/ir/exam/app/core/printing/PrintPreviewLayoutCodec.kt`
- `app/src/test/java/ir/exam/app/core/printing/PrintPreviewLayoutCodecTest.kt`
- `app/src/test/java/ir/exam/app/ui/app/NativeExamPdfContractTest.kt`
- `app/src/test/java/ir/exam/app/ui/app/PrintRetirementCleanupTest.kt`

## اعتبارسنجی انجام‌شده

- `git diff --check`: موفق
- `python3 scripts/verify_native_final.py`: موفق
- تست JUnit واقعیِ `PrintPreviewLayoutCodecTest`: موفق
- تست‌های JUnit واقعیِ `NativeExamPdfContractTest` و
  `PrintRetirementCleanupTest`: موفق
- فایل واقعی preview با Android + Compose API در یک ماژول ایزوله compile شد:
  موفق
- فایل واقعی `NativeExamPdfDocument.kt` با Android API در ماژول ایزوله compile
  شد: موفق

تلاش کامل `:app:compileDebugKotlin` نیز با JDK 17 و Android SDK 35 تا پایان
`kspDebugKotlin` رفت، اما در sandbox دارای حدود ۱٫۹GB RAM، هنگام IR lowering فایل
نامرتبط `QuestionBankScreen.kt` با `OutOfMemoryError: Java heap space` متوقف شد.
هیچ خطای Kotlin از فایل‌های انتقال PDF گزارش نشد و پس از توقف نیز daemon یا Java
process باقی نماند. این محدودیت حافظهٔ محیط است؛ برای build کامل در WSL/CI از
تنظیم پروژه (`org.gradle.jvmargs=-Xmx4096m`) و حداقل چند گیگابایت RAM آزاد استفاده
شود.

## بررسی ساده روی WSL

در پوشهٔ پروژه این سه دستور را به‌ترتیب اجرا کنید:

```bash
cd ~/exam-app-kotlin
git diff --check
python3 scripts/verify_native_final.py
./gradlew :app:compileDebugKotlin --no-daemon --max-workers=1
```

اگر WSL حافظهٔ کافی ندارد، دو دستور اول همچنان بدون نیاز به Android emulator قابل
اجرا هستند. هیچ secret، key یا keystore برای این بررسی لازم نیست.

## چک دستی روی دستگاه واقعی

1. آزمونی با فرمول، شکل، جدول/نمودار، اطلس، تصویر private، گزینه، جورکردنی و
   خطوط پاسخ باز کنید؛ preview PDF را ببینید.
2. یک شکل inline را جابه‌جا کنید، سپس آن را resize کنید؛ dialog را ببندید و
   دوباره باز کنید تا ماندگاری چیدمان دیده شود.
3. یک شکل بلند آزاد را طوری قرار دهید که از page break عبور کند. از بخش دوم آن
   drag کنید و مطمئن شوید کل شکل حرکت می‌کند. handle resize باید فقط در بخشی که
   لبهٔ پایین واقعی شکل دیده می‌شود ظاهر شود.
4. خط آبی بین دو سؤال را جابه‌جا کنید، preview را ببندید و باز کنید.
5. همان preview را چاپ کنید و از یکی‌بودن صفحه‌ها با PDF در پنجرهٔ چاپ مطمئن
   شوید. سپس از Print Center نسخهٔ دانش‌آموز و پاسخ‌نامهٔ آزمون دارای تصویر
   private را چاپ کنید.
6. ویرایشگر فرمول را در متن سؤال، گزینه و جورکردنی باز کنید. این تنها WebView
   مجاز است و باید مانند قبل کار کند.

## SQL، Edge، secret و dependency

هیچ SQL migration، Edge deployment، secret، token، service key یا release keystore
به این انتقال اضافه یا در آن افشا نشده است.
