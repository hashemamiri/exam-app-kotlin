package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V82.0 — دو خواستهٔ کاربر در آزمون‌ساز چاپی:
 *
 *  ۱) «دکمهٔ فرمول هم مانند ۷ ابزار دیگر پل بزن» — استثنای فرمول در
 *     `openQuestionTool` برداشته شد؛ هر هشت ابزار به
 *     `ExamPrintNative.openFigureTool` می‌روند. پنجرهٔ فرمول همان
 *     `FormulaHostDialog` است که آزمون‌سازِ بومی استفاده می‌کند
 *     (استفادهٔ مجدد، نه بازنویسی).
 *
 *  ۲) «با دابل‌کلیک ویرایشگر باز نمی‌شود» — دابل‌کلیک روی یک ابزارِ درج‌شده
 *     حالا `ExamPrintNative.editFigureTool(qid, index)` را صدا می‌زند و همان
 *     پنجرهٔ بومی با `initialSpec` باز می‌شود؛ نتیجه **جایگزینِ همان توکن**
 *     می‌شود، نه درجِ یک ابزارِ تازه.
 */
class V82_0FormulaBridgeAndEditTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun src(p: String) = File(root(), p).readText()
    private val assetText by lazy { src("app/src/main/assets/print/exam_print.html") }
    private val host by lazy { src("app/src/main/java/ir/exam/app/ui/printing/ExamFigureToolHost.kt") }
    private val dialog by lazy { src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }

    // ---------- ۱) فرمول هم پل دارد ----------

    @Test
    fun `the formula exception is gone from openQuestionTool`() {
        assertFalse("استثنای فرمول باید برداشته شود", "activeExactTool !== 'formula'" in assetText)
        assertTrue("ExamPrintNative.openFigureTool" in assetText)
    }

    @Test
    fun `formula is a first class native tool`() {
        assertTrue("const val FORMULA = \"formula\"" in host)
        assertTrue("val ALL_TOOLS = NATIVE_TOOLS + FORMULA" in host)
        // هفت ابزار قبلی دست‌نخورده‌اند
        listOf("figure", "graph", "table", "anatomy", "periodic", "physics", "chemistry")
            .forEach { assertTrue("ابزار $it گم شد", "\"$it\"" in host) }
    }

    @Test
    fun `the formula window reuses the existing native editor`() {
        assertTrue("FormulaHostDialog(" in dialog)
        assertTrue("import ir.exam.app.ui.math.FormulaHostDialog" in dialog)
        // قرارداد آن متن‌محور است، نه FigureSpec
        assertTrue("__qmfQuestionText" in dialog)
        assertTrue("__qmfSetQuestionText" in dialog)
    }

    @Test
    fun `the text bridges exist on the page`() {
        assertTrue("window.__qmfQuestionText = function" in assetText)
        assertTrue("window.__qmfSetQuestionText = function" in assetText)
        // برگشت‌ها
        assertTrue("'no-question'" in assetText)
    }

    @Test
    fun `the html formula editor stays as a fallback`() {
        // اگر پل بومی نبود، همان مسیر قبلی باید کار کند
        assertTrue("buttonMap[activeExactTool]" in assetText)
        assertTrue("f.src = MATH_EDITOR_URL" in assetText)
        assertTrue(File(root(), "app/src/main/assets/print/math_editor.html").isFile)
    }

    // ---------- ۲) دابل‌کلیک = ویرایش بومی ----------

    @Test
    fun `double click routes to the native edit bridge`() {
        assertTrue("ExamPrintNative.editFigureTool" in assetText)
        assertTrue("function qmfFigIndexOf" in assetText)
        assertTrue("function qmfQidOfFig" in assetText)
    }

    @Test
    fun `the geometry capture listener tries native first`() {
        // این شنونده زودتر از همه اجرا می‌شود و قبلاً مسیر بومی را می‌بلعید
        assertTrue("if (typeof openQmfFigEditor === 'function' && openQmfFigEditor(fig)) return;" in assetText)
    }

    @Test
    fun `the table only dblclick path also goes native first`() {
        assertTrue("if(!openQmfFigEditor(fig)){openTable(fig)}" in assetText)
    }

    @Test
    fun `edit bridges locate and replace one token`() {
        assertTrue("window.__qmfEditFigAt = function" in assetText)
        assertTrue("window.__qmfReplaceFigToken = function" in assetText)
        assertTrue("'bad-range'" in assetText)
    }

    @Test
    fun `the kotlin bridge exposes editFigureTool`() {
        assertTrue("fun editFigureTool(questionId: String?, index: Int)" in dialog)
        assertTrue("onEditFigureTool" in dialog)
        assertTrue("figureEditRequest" in dialog)
    }

    @Test
    fun `editing prefills the dialog and replaces instead of inserting`() {
        assertTrue("val editIndex: Int? = null" in host)
        assertTrue("val initialSpecJson: String? = null" in host)
        assertTrue("val isEdit: Boolean" in host)
        assertTrue("initialSpec = initial" in host)
        assertTrue("__qmfReplaceFigToken" in dialog)
        assertTrue("req.isEdit" in dialog)
    }

    @Test
    fun `spec kind maps back to the right tool`() {
        assertTrue("internal fun toolOfSpec" in dialog)
        listOf("\"t\" -> \"table\"", "\"p\" -> \"periodic\"", "\"a\" -> \"anatomy\"", "\"g\" -> \"graph\"")
            .forEach { assertTrue("نگاشت $it نیست", it in dialog) }
        assertTrue("chemistry" in dialog && "physics" in dialog)
    }

    @Test
    fun `editing skips the type picker`() {
        // موقع ویرایش، نوع از قبل معلوم است
        assertTrue("mutableStateOf(initialSpec)" in host)
        assertTrue("mutableStateOf(initialSpec?.type)" in host)
    }

    // ---------- محافظت از کارهای قبلی ----------

    @Test
    fun `out of scope areas are untouched`() {
        listOf("function printStudent", "function printTeacher", "function renderPreview",
               "function renderFigToken", "function buildHeader")
            .forEach { assertTrue("بخش خارج از دامنه تغییر کرد: $it", it in assetText) }
    }

    @Test
    fun `earlier fixes still hold`() {
        assertTrue("if (url != MAIN_PAGE_URL) return" in dialog)      // V80.0
        assertTrue("data.reset && !data.force" in assetText)          // V80.0
        assertTrue("getComputedStyle(f).display !== 'none'" in assetText) // V81.0
        assertFalse("doc.write(MATH_EDITOR_HTML)" in assetText)       // V79.0
        assertTrue("__qmfInsertFigToken" in assetText)                // V78.0
    }
}
