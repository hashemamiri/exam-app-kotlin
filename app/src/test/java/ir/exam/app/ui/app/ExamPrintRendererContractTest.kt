package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** قرارداد جاری موتور مستقلِ پیش‌نمایش و چاپ آزمون. */
class ExamPrintRendererContractTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()
    private val rendererFile by lazy { File(root(), "app/src/main/assets/print/exam_print_renderer.html") }
    private val renderer by lazy { rendererFile.readText() }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val nativeRenderer by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintAssetRenderer.kt") }

    @Test
    fun `renderer-only asset exposes the active print contract`() {
        assertTrue("asset رندرر وجود ندارد", rendererFile.isFile)
        // V115 — سقف با scripts/verify_native_final.py هم‌تراز شد (نوار قالب‌بندی V114 + صفحه‌بندی V115).
        assertTrue("رندرر باید سبک بماند", rendererFile.length() < 140_000L)
        listOf(
            "window.setExamData = setExamData;",
            "window.printStudent = function",
            "window.printTeacher = function",
            "window.ExamPrintRenderer = {showPreview:showPreview,layoutSnapshot:snapshot,figureAt:figureAt,replaceFigure:replaceFigure,restorePreview:restorePreview};",
            "@page{size:A4",
            "function requestPrint(mode)",
            "function buildHeader()"
        ).forEach { required ->
            assertTrue("قرارداد فعال رندرر پیدا نشد: $required", required in renderer)
        }
    }

    @Test
    fun `renderer contains no retired document authoring surface`() {
        listOf(
            "<iframe",
            "<textarea",
            "contenteditable",
            "localStorage",
            "math_editor.html",
            "renderEditor",
            "addQuestion",
            "__qmf"
        ).forEach { forbidden ->
            assertFalse("باقی‌ماندهٔ ویرایشگر در رندرر است: $forbidden", forbidden.lowercase() in renderer.lowercase())
        }
    }

    @Test
    fun `native bridge renders formulas and figures instead of bundling another renderer`() {
        listOf("fun renderFormula", "fun renderFigure", "ExamPrintAssetRenderer(context)", "\"ExamPrintBridge\"")
            .forEach { required -> assertTrue("پل چاپ ناقص است: $required", required in dialog) }
        listOf("NativeMathSvgRenderer", "FigureSvgRenderer", "AtlasBitmapRenderer", "data:image/svg+xml;base64")
            .forEach { required -> assertTrue("رندر بومی مورد انتظار نیست: $required", required in nativeRenderer) }
    }

    @Test
    fun `preview keeps layout persistence and native figure replacement`() {
        listOf("layoutSnapshot", "figureAt", "replaceFigure", "pointerdown", "question-sep-drag")
            .forEach { required -> assertTrue("پیش‌نمایش قابلیت فعال را ندارد: $required", required in renderer) }
        assertTrue("snapshot پیش‌نمایش به میزبان برنمی‌گردد", "onFigLayouts?.invoke(json)" in dialog)
        assertTrue("ویرایش شکل از پیش‌نمایش به ابزار بومی نمی‌رسد", "ExamFigureToolHost(" in dialog)
    }

    @Test
    fun `headless printing uses the configured shared webview`() {
        assertTrue("کارخانهٔ مشترک WebView نیست", "internal fun createExamPrintWebView(" in dialog)
        assertTrue("چاپگر بدون‌صفحه نیست", "internal class HeadlessExamPrinter" in dialog)
        assertTrue("WebView پیکربندی‌شده در چاپگر نگه‌داری نمی‌شود", "webView = configuredWebView" in dialog)
        assertTrue("adapter چاپ اندروید فراخوانی نمی‌شود", "createPrintDocumentAdapter(jobName)" in dialog)
        assertFalse("یک WebView پیکربندی‌نشده برای چاپ ساخته می‌شود", "val web = WebView(appContext)" in dialog)
    }
}
