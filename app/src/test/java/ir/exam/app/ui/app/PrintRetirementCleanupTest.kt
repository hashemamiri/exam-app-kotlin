package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** جلوگیری از بازگشت مسیرها و لایه‌های بازنشستهٔ چاپ. */
class PrintRetirementCleanupTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    @Test
    fun `retired source files and page assets are absent`() {
        listOf(
            "app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt",
            "app/src/main/java/ir/exam/app/core/printing/WordPageLayout.kt",
            "app/src/main/java/ir/exam/app/data/local/PrintLayoutStore.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamBuilder30Windows.kt",
            "app/src/main/assets/print/exam_print.html",
            "app/src/main/assets/print/math_editor.html"
        ).forEach { path ->
            assertFalse("فایل بازنشسته برگشته است: $path", File(root(), path).exists())
        }
        assertTrue(File(root(), "app/src/main/assets/print/exam_print_renderer.html").isFile)
    }

    @Test
    fun `main source has no retired route or print layout compatibility layer`() {
        val mainSource = File(root(), "app/src/main/java")
        val forbidden = listOf(
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
