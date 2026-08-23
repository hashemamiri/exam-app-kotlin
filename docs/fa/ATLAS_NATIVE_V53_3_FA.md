# V53.3 — آناتومی + فیزیک/شیمی کاملاً Native و ویرایش دوبار-کلیک

**پیش‌نیاز:** V53.2 (اعمال و build شده)
**SQL / Edge Function / Secret / Migration / Dependency جدید:** ندارد
**افزایش حجم asset:** ~2.3MB (۱۳۷ تصویر اطلس؛ قبلاً به‌صورت base64 داخل HTML بود و از آنجا حذف نشده — HTML مرجع دست‌نخورده است)

---

## ۱) دامنه

با این مرحله، نقشهٔ سه‌مرحله‌ای V53 کامل می‌شود: هر ۸ آیکن نوار متن سؤال اکنون
ویرایشگر Native باز می‌کنند (فرمول طبق استثنای کاربر WebView است) و همهٔ انواع
`%%FIG%%` در نمای دانش‌آموز و چاپ/PDF رندر Native دارند.

## ۲) استخراج برنامه‌ای از مرجع

```text
ATLAS (آناتومی)          → ۶۷ تصویر JPEG از base64 → assets/figure_atlas/anatomy/
SCIENCE_ATLAS            → ۷۰ تصویر JPEG از base64 → assets/figure_atlas/science/
TYPES آناتومی            → ۶۷ نوع + کپشن آموزشی + ۱۵ دسته (CATS)
FILE map                 → ۷۷ نگاشت id→فایل (aliasهای مرجع مثل organs/musF حفظ شدند)
TYPES علوم               → ۷۰ نوع + ۸ دستهٔ فیزیک + ۶ دستهٔ شیمی
inferDomain              → scienceDomain (دستهٔ شیمی→chem، فیزیک→phys)
```

## ۳) قرارداد داده — بدون تغییر نسبت به مرجع

```text
{k:'a'|'s', t:نوع, X:{title, lab, blank, mkName, marks:[{x1,y1,x2,y2,n,lbl}]}}
مختصات marks: درصدی ۰..۱۰۰ نسبت به قاب تصویر (همان قرارداد مرجع)
حداکثر نشانه: ۱۲ (همان سقف مرجع) — شمارهٔ آزاد بعدی مثل nextMarkN مرجع
```

توکن‌های ساخته‌شده با WebView قدیمی در Native رندر/ویرایش می‌شوند و برعکس.

## ۴) اجزای جدید

- `AtlasCatalog.kt` — کاتالوگ کامل انواع/دسته‌ها/نگاشت فایل + `scienceDomain` + `assetPath`.
- `AtlasMarkPainter.kt` — هندسهٔ خالص فلش و ارقام فارسی (تست‌پذیر JVM).
- `AtlasFigureView.kt` — نمایش Compose: تصویر اطلس + Canvas نشانه‌ها + عنوان +
  سطرهای جای پاسخ با قواعد `lab/blank/mkName` مرجع.
- `AtlasEditorDialog.kt` — ویرایشگر Native: دسته‌بندی، انتخاب نوع با thumbnail،
  کپشن آموزشی آناتومی، نشانه‌گذاری با کشیدن انگشت (پیش‌نمایش زندهٔ draft)،
  برچسب هر نشانه، حذف تکی/همه، سوییچ‌های عنوان/جای پاسخ/نمایش نام‌ها.
- `AtlasBitmapRenderer.kt` — رندر Bitmap برای چاپ/PDF (تصویر + فلش‌ها + جای پاسخ).

## ۵) ویرایش دوبار-کلیک (رفع محدودیت ثبت‌شدهٔ V53.1/V53.2)

```text
dblclick توکن k∈{t,p,a,s} داخل WebView (فقط حالت nativeTools=1)
→ ExamEditorNative.onEditFigure(rawJson)
→ Builder ویرایشگر Native همان نوع را با spec موجود باز می‌کند
→ تأیید: ExamEditorTools.applyEditedToken → جایگزینی همان توکن در متن
→ انصراف: cancelEditToken → بدون تغییر
هندسه/نمودار (k خالی) مثل قبل به ابزار مرجع GeoFig می‌روند؛ کد مرجع دست‌نخورده است.
```

## ۶) فایل‌های تغییرکرده

```text
app/src/main/assets/figure_atlas/anatomy/*.jpg   (۶۷ فایل جدید)
app/src/main/assets/figure_atlas/science/*.jpg   (۷۰ فایل جدید)
app/src/main/assets/question_editor/question_editor.html  (پل dblclick/applyEditedToken)
app/src/main/assets/question_editor/version.txt
app/src/main/java/ir/exam/app/core/figure/AtlasCatalog.kt          (جدید)
app/src/main/java/ir/exam/app/core/figure/AtlasMarkPainter.kt      (جدید)
app/src/main/java/ir/exam/app/core/figure/AtlasBitmapRenderer.kt   (جدید)
app/src/main/java/ir/exam/app/core/figure/FigureSpec.kt            (marks/buildAtlas/AtlasMark)
app/src/main/java/ir/exam/app/ui/figure/AtlasFigureView.kt         (جدید)
app/src/main/java/ir/exam/app/ui/figure/AtlasEditorDialog.kt       (جدید)
app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt            (مسیر k∈{a,s})
app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt  (onEditFigure/applyEdited)
app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/test/java/ir/exam/app/ui/app/V53_3AtlasNativeTest.kt       (جدید)
scripts/verify_native_final.py
text/CHANGELOG_FA.txt
docs/fa/ATLAS_NATIVE_V53_3_FA.md                                   (جدید)
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
```

## ۷) محدودیت ثبت‌شده، نه پنهان

- پلاک موقت `renderKindPlate` در `FigureSvgRenderer` برای k∈{a,s} باقی است اما
  دیگر در مسیر نمایش دانش‌آموز/PDF استفاده نمی‌شود (این دو مسیر مستقیم به
  `AtlasFigureView`/`AtlasBitmapRenderer` می‌روند)؛ فقط fallback داخلی مسیر SVG است.
- تصاویر base64 داخل HTML مرجع عمداً حذف نشدند تا کد مرجع byte-identical بماند؛
  در V54 (در صورت تأیید کاربر) می‌توان HTML را سبک کرد.

## ۸) وضعیت نقشه V53

```text
V53.1 → کادر WebView + ۸ آیکن Native + جدول Native           ✔ build شد
V53.2 → جدول تناوبی Native                                    ✔ build شد
V53.3 → آناتومی + فیزیک/شیمی Native + ویرایش دوبار-کلیک       ← این پچ
باقی‌مانده: هیچ (نقشهٔ V53 کامل است)
```
