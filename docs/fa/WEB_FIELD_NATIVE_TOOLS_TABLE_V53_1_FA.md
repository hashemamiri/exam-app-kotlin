# V53.1 — کادر متن سؤال WebView، نوار ۸ آیکن Native و ویرایشگر جدول Native

**پیش‌نیاز:** V52 (HEAD فعلی)
**SQL / Edge Function / Secret / Migration جدید:** ندارد
**Dependency جدید:** `com.caverock:androidsvg-aar:1.4` (همان کتابخانهٔ بسته‌بندی‌شده در coil-svg موجود؛ فقط ارجاع صریح برای PDF)

---

## ۱) درخواست کاربر

```text
متن سؤال جدید به‌جای متن سؤال قبلی قرار بگیرد.
آیکن‌های درج جدول، آناتومی، جدول تناوبی، فیزیک و شیمی کنار فرمول و شکل و نمودار.
تمام آیکن‌ها نیتیو شوند به‌جز فرمول و متن سؤال.
```

تصمیم‌های تأییدشده:

```text
ویرایشگرهای ۵ ابزار جدید            → کاملاً Native (سه مرحله V53.1..V53.3)
کادر متن سؤال                       → WebView (استثنای صریح کاربر)
رندر دانش‌آموز و چاپ/PDF            → در همین نقشه، هر ابزار در مرحلهٔ خودش
فایل ارسالی کاربر                   → جایگزین نشد؛ فقط اسکریپت tracking Cloudflare
                                       اضافه‌تر داشت و asset مخزن تمیزتر است
```

## ۲) مرجع ممیزی

```text
Asset: app/src/main/assets/question_editor/question_editor.html
قالب مشترک توکن:  %%FIG:{json}%%
کدهای k مرجع:      (خالی)=هندسه/نمودار ، t=جدول ، a=آناتومی ، p=تناوبی ، s=فیزیک/شیمی
جدول مرجع:         {k:'t', t:سبک, X:{title}, C:[[...]]} — ۱۸ سبک، ۱..۱۵×۱..۱۰
```

## ۳) تحویل V53.1

### کادر متن سؤال WebView

- `QuestionTextFieldWebView.kt` — سطح WebView محلی با همان قواعد امنیتی POC
  (فقط asset محلی، ناوبری خارجی مسدود، بدون Secret/token).
- `QuestionEditorFieldController` — فرمان Native به WebView:
  `openTool(name)`، `insertFigureJson(json)`، `setValue(text)`.
- پارامتر `?nativeTools=1` نوار ابزار داخلی HTML را مخفی می‌کند؛ دیالوگ فرمول
  گزینه‌ها همان صفحه را بدون پارامتر باز می‌کند و دست‌نخورده است.
- اسکریپت افزودهٔ `exam-editor-native-tools` (بدون تغییر هیچ کد مرجع):
  درج توکن در محل مکان‌نما، بازکردن ابزارهای مرجع با فرمان Native و گزارش
  باز/بسته‌شدن لایه‌های تمام‌صفحه (`onOverlayChanged`) برای بلندشدن ارتفاع WebView.
- متن همیشه از رویداد `onTextChanged` به `viewModel.updateText` برمی‌گردد؛
  ذخیره/draft/بانک سؤال هیچ تغییری نکردند.

### نوار ۸ آیکن Native

- `QuestionToolIcons.kt` — بازتولید SVGهای مرجع به `ImageVector` خالص:
  فرمول (∑)، شکل، نمودار، جدول، آناتومی بدن، جدول تناوبی، فیزیک (آذرخش)، شیمی (ارلن).
- `QuestionTextWebSection.kt` — کادر WebView + ردیف آیکن‌ها با ترتیب مرجع.
- فرمول → `openTool("formula")` (ویرایشگر WebView مصوب V45.4).
- شکل/نمودار → همان جریان دومرحله‌ای Native V45.3؛ خروجی به‌جای الحاق انتهای متن،
  در محل مکان‌نمای WebView درج می‌شود (fallback: مسیر قبلی ViewModel).
- جدول → ویرایشگر Native جدید همین مرحله.
- آناتومی/تناوبی/فیزیک/شیمی → تا تحویل V53.2/V53.3 ابزار مرجع داخل همان WebView
  باز می‌شود؛ آیکن‌ها از الان Native هستند.

### ویرایشگر Native جدول

- `TableEditorDialog.kt` — انتخاب ۱۸ سبک، عنوان، stepper سطر/ستون (۱..۱۵ × ۱..۱۰)،
  ویرایش تک‌تک خانه‌ها، پیش‌نمایش زنده و «پرکردن با نمونهٔ این سبک».
- `TableSvgRenderer.kt` — رندر SVG امن با همان قواعد `isHead`/`sample`/`def` مرجع؛
  فقط elementهای `svg/g/rect/line/path/text`؛ بدون style/script/URL/foreignObject.
- `FigureSpec.buildTable/tableCells/kind/isTable` — قرارداد دادهٔ مرجع حفظ شد؛
  خروجی WebView قدیمی و Native هم‌ارز و قابل‌تبادل‌اند.

### رندر دانش‌آموز و چاپ/PDF

- `FigureSvgRenderer.render` توکن `k='t'` را به `TableSvgRenderer` می‌سپارد؛
  بنابراین `NativeMathText` (دانش‌آموز/پیش‌نمایش) بدون هیچ تغییری جدول را نشان می‌دهد.
- انواع `a/p/s` تا رندر کامل V53.2/V53.3 «پلاک عنوان‌دار» امن می‌گیرند؛ JSON خام
  هرگز نمایش داده نمی‌شود.
- `OfficialPdfPrintAdapter` متن سؤال را با `RichTextSplitter` می‌شکند و هر
  `%%FIG%%` را با AndroidSVG به تصویر برداری تبدیل می‌کند (قبلاً JSON خام چاپ می‌شد).

## ۴) امنیت

```text
WebView مجاز فقط: FormulaEditorDialog / QuestionEditorWebView /
                  QuestionEditorWebViewDialog / QuestionTextFieldWebView
ناوبری خارجی WebView                  → مسدود (shouldOverrideUrlLoading=true)
دسترسی فایل WebView                   → allowFileAccess=false و مشتقات false
Secret/token در WebView               → صفر
SVG جدول                              → XML-escaped، بدون script/href/foreignObject
کد مرجع HTML                          → دست‌نخورده؛ فقط دو بلوک افزوده و پرچم مخفی‌سازی
```

## ۵) فایل‌های تغییرکرده

```text
app/src/main/assets/question_editor/question_editor.html      (بلوک پل + پرچم nativeTools)
app/src/main/assets/question_editor/version.txt
app/src/main/java/ir/exam/app/core/figure/FigureSpec.kt
app/src/main/java/ir/exam/app/core/figure/FigureSvgRenderer.kt
app/src/main/java/ir/exam/app/core/figure/TableSvgRenderer.kt              (جدید)
app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt         (جدید)
app/src/main/java/ir/exam/app/ui/figure/TableEditorDialog.kt               (جدید)
app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt          (جدید)
app/src/main/java/ir/exam/app/ui/math/QuestionToolIcons.kt                 (جدید)
app/build.gradle.kts
app/src/test/java/ir/exam/app/ui/app/V53WebFieldNativeToolsTableTest.kt    (جدید)
app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt
scripts/verify_native_final.py
text/CHANGELOG_FA.txt
docs/fa/WEB_FIELD_NATIVE_TOOLS_TABLE_V53_1_FA.md                           (جدید)
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
```

## ۶) محدودیت ثبت‌شده، نه پنهان

- ویرایشگر و رندر Native آناتومی/تناوبی/فیزیک/شیمی در V53.2 و V53.3 تحویل می‌شوند؛
  تا آن زمان ابزار مرجع داخل WebView کادر متن سؤال باز می‌شود و در نمای دانش‌آموز/PDF
  پلاک عنوان‌دار نمایش داده می‌شود.
- `InlineMathTextEditor.kt` عمداً حذف نشده (گزینه‌ها و matching هنوز از
  `NativeMathText`/`ExistingFormulaEditor` استفاده می‌کنند)؛ فقط از کارت سؤال خارج شد.

## ۷) مرحله‌های بعدی

```text
V53.2 → جدول تناوبی Native (داده ۱۱۸ عنصر فارسی + ویرایشگر + رندر)
V53.3 → آناتومی + فیزیک/شیمی Native (اطلس تصاویر asset + ویرایشگر + رندر) + رگرسیون کل
```
