package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** قرارداد فعال: یک PDF A4 برای preview و Print Framework، با overlay Native. */
class NativeExamPdfContractTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `preview renders the shared pdf and print adapter copies that exact file`() {
        val document = source("app/src/main/java/ir/exam/app/ui/printing/NativeExamPdfDocument.kt")
        val preview = source("app/src/main/java/ir/exam/app/ui/printing/NativeExamPdfPreviewDialog.kt")
        assertTrue("PDF A4 ساخته نمی‌شود", "PdfDocument" in document)
        assertTrue("adapter همان فایل را نمی‌خواند", "FileInputStream(document.pdfFile)" in document)
        assertTrue("پیش‌نمایش با PdfRenderer نیست", "PdfRenderer" in preview)
        assertTrue("چاپ preview PDF را دوباره تولید می‌کند", "NativeExamPdfPrintAdapter(documentToPrint" in preview)
        assertFalse("WebView در preview مانده است", "WebView" in preview)
        assertFalse("پل JavaScript در preview مانده است", "evaluateJavascript" in preview)
    }

    @Test
    fun `native gestures preserve figure edit move resize and separator persistence`() {
        val preview = source("app/src/main/java/ir/exam/app/ui/printing/NativeExamPdfPreviewDialog.kt")
        val document = source("app/src/main/java/ir/exam/app/ui/printing/NativeExamPdfDocument.kt")
        val engine = source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt")
        val state = source("app/src/main/java/ir/exam/app/core/printing/PrintPreviewLayoutCodec.kt")
        listOf(
            "detectTapGestures",
            "detectDragGestures",
            "PreviewDragMode.MOVE",
            "PreviewDragMode.RESIZE",
            "PreviewDragMode.SEPARATOR",
            "ExamFigureToolHost",
            "PrintPreviewLayoutCodec.updateFigure"
        ).forEach { marker -> assertTrue("gesture بومی ناقص است: $marker", marker in preview) }
        assertTrue("شکل‌های inline metadata ندارند", "InlineFigureMark" in engine)
        assertTrue("resize ارتفاع شکل در PDF مصرف نمی‌شود", "imageHeightMm" in engine)
        assertTrue("metadata شکلِ کامل پیش از page crop نگه‌داری نمی‌شود", "flowBounds" in document)
        assertTrue("gesture هنوز از اندازهٔ crop صفحه استفاده می‌کند", "target.flowBounds" in preview)
        assertTrue("جابجایی شکل در مرز page crop گیر می‌کند", "boundedHorizontally" in preview)
        assertTrue("resize در crop میانیِ شکل split‌شده محدود نشده است", "figure.canResize" in preview)
        assertTrue("فاصلهٔ سؤال در PDF مصرف نمی‌شود", "separatorQuestionIndex" in engine)
        assertTrue("وضعیت layout بومی نیست", "nativePdf" in state)
    }

    @Test
    fun `retired dashboard exam route cannot select the old adapter`() {
        val dashboard = source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt")
        val state = source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardViewModel.kt")
        val controller = source("app/src/main/java/ir/exam/app/core/printing/OfficialPrintController.kt")
        val printCenter = source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt")
        assertFalse("dashboard هنوز controller قدیمی چاپ آزمون را نگه داشته است", "OfficialPrintController" in dashboard)
        assertFalse("state قدیمی preparePrint هنوز یک مسیر موازی می‌سازد", "preparePrint(" in state)
        assertFalse("controller کارنامه هنوز چاپ آزمونِ موازی دارد", "fun printExam" in controller)
        assertTrue("تنها مسیر چاپ مستقیم آزمون launcher مشترک نیست", "NativeExamPrintLauncher.print" in printCenter)
    }
}
