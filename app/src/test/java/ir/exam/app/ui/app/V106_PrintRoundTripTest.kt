package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V106 — چاپ از کارت‌های بخش «چاپ آزمون» واقعاً پنل چاپ را باز می‌کند
 * (Context فعالیت) و رفت‌وبرگشت چاپ ↔ پیش‌نمایش حالتِ برگه را خراب نمی‌کند.
 */
class V106_PrintRoundTripTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()
    private val renderer by lazy { source("app/src/main/assets/print/exam_print_renderer.html") }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val centre by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }

    @Test
    fun `print manager is obtained from the activity context, not the application context`() {
        assertTrue("یاب Context فعالیت نیست", "internal tailrec fun Context.findActivityContext(): android.app.Activity?" in dialog)
        assertTrue("چاپگر بدون‌صفحه از Context فعالیت استفاده نمی‌کند", "private val printContext: Context = context.findActivityContext() ?: context" in dialog)
        assertTrue("PrintManager چاپگر بدون‌صفحه از printContext گرفته نمی‌شود", "printContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager" in dialog)
        assertFalse("PrintManager از Context خودِ WebView (application) گرفته می‌شود", "web.context.getSystemService(Context.PRINT_SERVICE)" in dialog)
        // V107 — آیکن پرینتر و چاپِ مستقیم از کارت‌های مرکز چاپ حذف شد.
        assertFalse("مرکز چاپ هنوز چاپگر بدون‌صفحه دارد", "HeadlessExamPrinter(" in centre)
        assertFalse("آیکن پرینتر هنوز روی کارت‌ها هست", "Icons.Outlined.Print" in centre)
    }

    @Test
    fun `renderer leaves print mode cleanly and restores the preview`() {
        assertTrue("restorePreview در قرارداد نیست", "restorePreview:restorePreview" in renderer)
        assertTrue("requestPrint پیش‌نمایش را نمی‌بندد", "previewWasOpen = document.body.classList.contains('preview-open');" in renderer)
        assertTrue("restorePreview کلاس چاپ را پاک نمی‌کند", "document.body.classList.remove('exam-print-mode');\n    state.mode = 'student';" in renderer)
        assertTrue("afterprint به restorePreview وصل نیست", "window.addEventListener('afterprint', restorePreview);" in renderer)
        assertTrue("showPreview حالت استاد را ریست نمی‌کند", "endDrag(); state.mode = 'student';" in renderer)
    }

    @Test
    fun `dialog restores preview after the print panel and closes direct print windows`() {
        assertTrue("پس از پنل چاپ restorePreview صدا زده نمی‌شود", "window.ExamPrintRenderer.restorePreview()" in dialog)
        assertTrue("adapter یک‌بارمصرف در پنجرهٔ پیش‌نمایش استفاده نمی‌شود", "OneShotPrintAdapter(printAdapter) { restore() }" in dialog)
        assertTrue("چاپ مستقیم از بیلدر پس از پنل چاپ بسته نمی‌شود", "if (initialPrintMode != null) view.post { requestDismiss() }" in dialog)
    }
}
