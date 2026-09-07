package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** جلوگیری از بازگشت مسیر چاپ HTML یا ویرایشگر سند بازنشسته. */
class PrintRetirementCleanupTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    @Test
    fun `retired print webview files and old editor are absent`() {
        listOf(
            "app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt",
            "app/src/main/java/ir/exam/app/core/printing/WordPageLayout.kt",
            "app/src/main/java/ir/exam/app/data/local/PrintLayoutStore.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamBuilder30Windows.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamPrintAssetRenderer.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlImageInliner.kt",
            "app/src/main/java/ir/exam/app/ui/builder/ExamPrintPreview.kt",
            "app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt",
            "app/src/main/assets/print/exam_print.html",
            "app/src/main/assets/print/exam_print_renderer.html",
            "app/src/main/assets/print/math_editor.html"
        ).forEach { path ->
            assertFalse("فایل بازنشسته برگشته است: $path", File(root(), path).exists())
        }
        listOf(
            "app/src/main/java/ir/exam/app/ui/printing/NativeExamPdfDocument.kt",
            "app/src/main/java/ir/exam/app/ui/printing/NativeExamPdfPreviewDialog.kt",
            "app/src/main/java/ir/exam/app/core/printing/PrintPreviewLayoutCodec.kt"
        ).forEach { path ->
            assertTrue("مسیر PDF بومی نیست: $path", File(root(), path).isFile)
        }
    }

    @Test
    fun `only formula host imports android webkit`() {
        val sources = File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { "import android.webkit" in it.readText() }
            .map { it.relativeTo(root()).path.replace(File.separatorChar, '/') }
            .toList()
        assertEquals(
            listOf("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt"),
            sources
        )
    }

    @Test
    fun `main source has no retired route or print layout compatibility layer`() {
        val mainSource = File(root(), "app/src/main/java")
        val forbidden = listOf(
            "ExamHtmlPrint",
            "ExamHtmlImageInliner",
            "HeadlessExamPrinter",
            "createExamPrintWebView",
            "createPrintDocumentAdapter",
            "exam_print_renderer.html",
            "PrintLayoutStore",
            "PrintLayoutMerger",
            "ExamDocumentEditorScreen",
            "WordPageLayout",
            "UnifiedDocumentEngine",
            "layoutExamForEditor",
            "editingDocumentExamId",
            "DOC_EDITOR",
            "questionsOverride",
            "overridePrintLayout"
        )
        val sources = mainSource.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        forbidden.forEach { term ->
            val offenders = sources.filter { file -> term in file.readText() }.map { it.relativeTo(root()).path }
            assertTrue("باقی‌ماندهٔ بازنشستهٔ $term در: $offenders", offenders.isEmpty())
        }
    }
}
