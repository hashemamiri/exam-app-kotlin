package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قرارداد جاری موتور پیش‌نمایش و چاپ آزمون.
 * V126 — رندرر مستقلِ قبلی حذف شد؛ قرارداد اکنون روی میزبانِ نازک + webhost.js (موتور وب آزمون‌ساز v20) است.
 */
class ExamPrintRendererContractTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()
    private val hostFile by lazy { File(root(), "app/src/main/assets/print/exam_print_renderer.html") }
    private val webhost by lazy { source("app/src/main/assets/print/web/webhost.js") }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val nativeRenderer by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintAssetRenderer.kt") }

    @Test
    fun `host asset and webhost expose the active print contract`() {
        assertTrue("asset میزبان وجود ندارد", hostFile.isFile)
        assertTrue("میزبان باید نازک بماند", hostFile.length() < 8_000L)
        assertFalse("رندرر قبلی باید حذف شده باشد", File(root(), "app/src/main/assets/print/exam_print_renderer_legacy.html").exists())
        listOf(
            "window.setExamData = setExamData;",
            "window.printStudent = function",
            "window.printTeacher = function",
            "window.ExamPrintRenderer = {",
            "restorePreview: restorePreview, setPageSetup: setPageSetup, getPageSetup: getPageSetup",
            "function requestPrint(mode, opts)"
        ).forEach { required ->
            assertTrue("قرارداد فعال رندرر پیدا نشد: $required", required in webhost)
        }
    }

    @Test
    fun `webhost contains no retired authoring or storage surface`() {
        listOf("<iframe", "<textarea", "contenteditable", "localStorage", "math_editor.html", "innerHTML", "applyBoxStyle", "applyFormat")
            .forEach { forbidden ->
                assertFalse("باقی‌ماندهٔ ممنوع در webhost.js است: $forbidden", forbidden.lowercase() in webhost.lowercase())
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
        listOf("layoutSnapshot: snapshot", "function figureAt(questionId, figureIndex)", "function replaceFigure(", "callBridge('editFigureTool', String(qid), index)")
            .forEach { required -> assertTrue("پیش‌نمایش قابلیت فعال را ندارد: $required", required in webhost) }
        assertTrue("snapshot پیش‌نمایش به میزبان برنمی‌گردد", "onFigLayouts?.invoke(json)" in dialog)
        assertTrue("ویرایش شکل از پیش‌نمایش به ابزار بومی نمی‌رسد", "ExamFigureToolHost(" in dialog)
    }

    @Test
    fun `native preview chrome is gone and page setup comes from the web panel`() {
        listOf("PrintPreviewHeader", "FormatChip", "PrintPageSetupDialog", "SetupSwitch", "PrintBoxSettingsDialog", "PrintBoxStyleStore", "WEB_ENGINE_PREVIEW")
            .forEach { retired -> assertFalse("رابطِ بومیِ حذف‌شده هنوز هست: $retired", retired in dialog) }
        assertTrue("تنظیمات صفحهٔ وب روی دستگاه ذخیره نمی‌شود", "fun pageSetupChanged(json: String?)" in dialog)
        assertTrue("PrintPageSetup.fromJson(json)" in dialog)
        assertTrue("callBridge('pageSetupChanged', JSON.stringify(readPageSetup()))" in webhost)
        assertFalse(File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintBoxSettingsDialog.kt").exists())
        assertFalse(File(root(), "app/src/main/java/ir/exam/app/data/local/PrintBoxStyleStore.kt").exists())
    }

    @Test
    fun `headless printing uses the configured shared webview`() {
        assertTrue("کارخانهٔ مشترک WebView نیست", "internal fun createExamPrintWebView(" in dialog)
        assertTrue("چاپگر بدون‌صفحه نیست", "internal class HeadlessExamPrinter" in dialog)
        assertTrue("WebView پیکربندی‌شده در چاپگر نگه‌داری نمی‌شود", "webView = configuredWebView" in dialog)
        assertTrue("adapter چاپ اندروید فراخوانی نمی‌شود", "createPrintDocumentAdapter(jobName)" in dialog)
        assertFalse("یک WebView پیکربندی‌نشده برای چاپ ساخته می‌شود", "val web = WebView(appContext)" in dialog)
        // V106 — PrintManager از Context فعالیت
        assertTrue("printContext.getSystemService(Context.PRINT_SERVICE)" in dialog)
        assertTrue("window.ExamPrintRenderer.restorePreview()" in dialog)
    }
}
