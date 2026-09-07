package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V100 — حذفِ کاملِ «آزمون‌ساز چاپی»:
 * پنجرهٔ چاپ (WebView) حالا فقط دو حالت دارد: پیش‌نمایشِ بومی (آیکن چشم)
 * و چاپِ مستقیم (دانش‌آموز/پاسخ‌نامه). همهٔ امکاناتِ حالتِ ویرایش — کارت‌های
 * بومی، منوی رادیال، ذخیره/بازکردن JSON، بازیابی، مدیریت سؤال، ویرایشگر
 * فرمول، استودیوی تصویر، هدرِ «سربرگ» و تشخیص‌های فنی — حذف شدند. ویرایش
 * آزمون در آزمون‌سازِ بومی انجام می‌شود.
 *
 * چیزهایی که باید **بمانند** (مصرف‌کنندهٔ واقعی: پیش‌نمایش + چاپ):
 *  - موتورِ رندرِ صفحه (exam_print.html) و تزریقِ setExamData
 *  - پیش‌نمایش + درگِ شکل + اسنپ‌شاتِ چیدمان (V99.2 round-trip)
 *  - چاپِ مستقیم (qmf-print-mode + printStudent/printTeacher + پنجرهٔ چاپ)
 *  - ویرایشِ شکل با لمسِ دوباره (editFigureTool → ExamFigureToolHost)
 *  - نوارِ خطا (jsError + فیلترِ V99.2d) و پیام‌های toast (barStatus)
 *  - PrintHeaderStore (سربرگ در هر سه مسیر)
 *  - آزمون‌های محلی: مدادِ آن‌ها به آزمون‌سازِ بومی می‌رود (دست‌نخورده)
 */
class V100_PrintBuilderRemovedTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val center by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val asset by lazy { source("app/src/main/assets/print/exam_print.html") }

    // ---------- فایل‌های حذف‌شده ----------

    @Test
    fun `the builder-mode files are gone from the repo`() {
        val deleted = listOf(
            "app/src/main/java/ir/exam/app/ui/printing/PrintQuestionCards.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamQuestionManagerSheet.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamDraftMirror.kt"
        )
        deleted.forEach { assertFalse("فایل حذف‌نشده: $it", File(root(), it).exists()) }
    }

    // ---------- پنجرهٔ چاپ: حالتِ ویرایش رفته، دو حالتِ واقعی مانده ----------

    @Test
    fun `the dialog no longer hosts any builder-mode state`() {
        listOf(
            "cardsLoaded", "cardDetails", "openCardId", "cardsRefresh",
            "formulaTarget", "formulaCaret", "formulaEnd",
            "questionRows", "questionTotal", "showQuestionManager",
            "mathAssetProbe", "formulaDiag",
            "headerSchema", "showHeaderSettings", "showSaveDialog",
            "radialMenuOpen", "showPrintMenu", "showRestore", "restoreAsked",
            "studioQuestionId", "studioImagesJson", "pageSnapshotJson",
            "pendingOpenText", "saveFileLauncher", "openExamPicker",
            "fileChooserCallback", "imagePicker", "mirrorDraft",
            "flushPendingEdits", "scheduleTextWrite", "scheduleCardsRefresh"
        ).forEach { assertFalse("حالتِ بیلدر مانده: $it", it in dialog) }
        // پنجره‌های بومیِ بیلدر هم از پنجرهٔ چاپ رفته‌اند
        assertFalse("PrintQuestionCard(" in dialog)
        assertFalse("BuilderRadialMenuOverlay(" in dialog)
        assertFalse("ExamQuestionManagerSheet(" in dialog)
        assertFalse("FormulaHostDialog(" in dialog)
        assertFalse("ExamImageStudioDialog(" in dialog)
        assertFalse("HeaderSettingsDialog(" in dialog)
        assertFalse("SaveExamDialog" in dialog)
        assertFalse("OpenExamSummaryDialog" in dialog)
        // هدرِ قدیمی (بازگشت/عنوان/«سربرگ») دیگر نیست
        assertFalse("Icons.AutoMirrored.Outlined.ArrowBack" in dialog)
        assertFalse("onShowFileChooser" in dialog)
        assertFalse("onLongClick" in dialog)
    }

    @Test
    fun `the dialog keeps exactly the preview and print surface`() {
        // دو حالتِ واقعی
        assertTrue("initialPreview: Boolean = false" in dialog)
        assertTrue("initialPrintMode: String? = null" in dialog)
        assertTrue("onFigLayouts: ((String) -> Unit)? = null" in dialog)
        // لود + خطا + پیام
        assertTrue("var loading by remember { mutableStateOf(true) }" in dialog)
        assertTrue("var jsError by remember { mutableStateOf<String?>(null) }" in dialog)
        assertTrue("var barStatus by remember { mutableStateOf<String?>(null) }" in dialog)
        assertTrue("if (text.contains(\"Ignored attempt to cancel\")) return true" in dialog)
        // تزریق + حالت‌های واقعی
        assertTrue("window.setExamData(\$payload)" in dialog)
        assertTrue("document.body.classList.add('qmf-print-mode')" in dialog)
        assertTrue("printStudent()" in dialog)
        assertTrue("printTeacher()" in dialog)
        assertTrue("__qmfShowPreview" in dialog)
        // ویرایشِ دابل‌کلیک + اسنپ‌شاتِ چیدمان
        assertTrue("fun editFigureTool(" in dialog)
        assertTrue("fun fetchFigLayoutsSnapshot()" in dialog)
        assertTrue("ExamFigureToolHost(" in dialog)
        // گاردِ فریمِ اصلی (V80.0) دست‌نخورده
        assertTrue("if (url != MAIN_PAGE_URL) return" in dialog)
    }

    @Test
    fun `the bridge keeps only the five live methods`() {
        listOf("fun toast(", "fun previewClosed()", "fun print(", "fun editFigureTool(", "fun onError(")
            .forEach { assertTrue("پل گم شد: $it", it in dialog) }
        listOf("fun openImageStudio(", "fun openFigureTool(", "fun close(")
            .forEach { assertFalse("پلِ بیلدر مانده: $it", it in dialog) }
    }

    // ---------- مرکز چاپ ----------

    @Test
    fun `server exam cards lost their pencil`() {
        assertFalse("openBuilder30" in center)
        assertFalse("contentDescription = \"ویرایش آزمون\"" in center)
        // پرینتر و منوی دانش‌آموز/پاسخ‌نامه می‌مانند
        assertTrue("contentDescription = \"چاپ آزمون\"" in center)
        assertTrue("startPrint(target, \"student\")" in center)
        assertTrue("startPrint(target, \"teacher\")" in center)
    }

    @Test
    fun `local exam cards keep the native builder pencil`() {
        assertTrue("contentDescription = \"ویرایش آزمون چاپی\"" in center)
        assertTrue("onOpenLocalPrintExam(rec.id)" in center)
        assertTrue("contentDescription = \"حذف آزمون چاپی\"" in center)
    }

    // ---------- صفحه ----------

    @Test
    fun `the page lost its toolbar and dead builder functions`() {
        assertFalse("qmfLegacyToolbar\" style" in asset)
        assertFalse("window.togglePreviewWindow = function" in asset)
        assertFalse("window.__qmfOpenCard = function" in asset)
        assertFalse("window.__qmfCardOpener = function" in asset)
        assertFalse("window.__qmfRichPreview = function" in asset)
        assertFalse("window.__qmfPreviewCss = function" in asset)
        assertFalse("window.__qmfNeedFigTools = function" in asset)
        assertFalse("function saveExam()" in asset)
        assertFalse("function loadExam()" in asset)
        assertFalse("isPreviewOpen" in asset)
    }

    @Test
    fun `the page keeps the render engine and the preview machinery`() {
        // موتور رندر + تزریق
        assertTrue("window.setExamData = function" in asset)
        assertTrue("function renderAll()" in asset)
        assertTrue("function printStudent" in asset)
        assertTrue("function printTeacher" in asset)
        // پنلِ سربرگ می‌ماند: فیلدهای f_* همان حافظهٔ مقادیرِ سربرگ‌اند
        assertTrue("id=\"settingsPanel\"" in asset)
        assertTrue("id=\"f_course\"" in asset)
        assertTrue("if (!el) return;" in asset)
        // پنجرهٔ پیش‌نمایش + حالتِ چاپ
        assertTrue("window.__qmfShowPreview = function" in asset)
        assertTrue("function openPreviewWindow" in asset)
        assertTrue("body.qmf-print-mode{background:#cbd5e1 !important}" in asset)
        assertTrue("width:min(210mm, 92vw) !important" in asset)
        // اسنپ‌شاتِ چیدمان (V99.2)
        assertTrue("window.__qmfFigLayoutsSnapshot = function ()" in asset)
    }

    // ---------- اجزای مشترک: می‌مانند، فقط در بیلدر مصرف می‌شوند ----------

    @Test
    fun `shared components live only in the native builder now`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val windows = source("app/src/main/java/ir/exam/app/ui/printing/ExamBuilder30Windows.kt")
        // استودیوی تصویر + تنظیمات سربرگ در بیلدر هستند
        assertTrue("ExamImageStudioDialog(" in builder)
        assertTrue("ir.exam.app.ui.printing.HeaderSettingsDialog(" in builder)
        // شِما + بارگذاری‌کننده در ExamBuilder30Windows می‌مانند
        assertTrue("fun loadHeaderSchema" in windows)
        assertTrue("fun HeaderSettingsDialog(" in windows)
        // اما پنجره‌های JSONِ بیلدر حذف شده‌اند
        assertFalse("fun SaveExamDialog(" in windows)
        assertFalse("fun OpenExamSummaryDialog(" in windows)
        assertFalse("fun safeExamFileName(" in windows)
        // فراخوان‌های گاردشدهٔ صفحه (دوربین/ابزار) بی‌اثر اما امن‌اند
        assertTrue("typeof window.ExamPrintNative.openFigureTool === 'function'" in asset)
        assertTrue("typeof window.ExamPrintNative.openImageStudio === 'function'" in asset)
    }
}
