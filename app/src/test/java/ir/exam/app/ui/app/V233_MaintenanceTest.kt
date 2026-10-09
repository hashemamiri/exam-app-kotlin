package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V233 — کیفیت و نگه‌داری: کد مرده حذف شد، هشدارهای Lint شناخته‌شده رفع شد، CI گزارش می‌دهد. */
class V233_MaintenanceTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun deadCodeRemoved() {
        listOf(
            "app/src/main/java/ir/exam/app/core/navigation/AppRoute.kt",
            "app/src/main/java/ir/exam/app/data/dto/ExamWriteDto.kt",
            "app/src/main/java/ir/exam/app/data/dto/QuestionBankDto.kt",
            "app/src/main/java/ir/exam/app/domain/grading/GradeExamUseCase.kt",
            "app/src/main/java/ir/exam/app/ui/auth/AuthViewModelFactory.kt",
            "app/src/main/java/ir/exam/app/ui/image/FreeImageCanvas.kt",
            "app/src/main/java/ir/exam/app/ui/image/ImageEditorViewModel.kt"
        ).forEach { assertFalse(it, File(root(), it).exists()) }
        assertFalse("function demoFormula(" in src("site/src/app.js"))
        assertFalse("function maskedHtml(" in src("site/src/builder.js"))
        assertFalse("function homePanel(" in src("site/src/mobile.js"))
        assertTrue(File(root(), "scripts/dead_code_scan.py").isFile)
    }

    @Test
    fun lintFixesAndCiReports() {
        val main = src("app/src/main/java/ir/exam/app/MainActivity.kt")
        assertTrue("TRIM_MEMORY_UI_HIDDEN) FormulaEditorPool.release()" in main && "TRIM_MEMORY_RUNNING_LOW)" !in main)
        assertTrue("imagePosition=\"free\", // V233" in src("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt"))
        val ci = src(".github/workflows/android.yml")
        assertTrue("uses: actions/upload-artifact@v4" in ci && "name: reports-\${{ github.run_number }}" in ci && "Lint: تعداد به تفکیک نوع" in ci)
    }
}
