package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V214 — سرعت فاز ۳: پیش‌نمایش چاپ موازی، حافظهٔ موقت صفحه‌های باقی‌مانده، تراشه‌های سبک آزمون دانش‌آموز. */
class V214_PerformancePhase3Test {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun printPreviewLoadsEngineAndImagesInParallel() {
        val dialog = src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt")
        assertTrue("pendingPrintable = { inlinedRef.value }" in dialog)
        assertTrue("pendingReady = { printable == null || inlinedRef.value != null }" in dialog)
        assertTrue("if (!pendingReady()) {" in dialog)
        assertTrue("pendingPrintable?.invoke() ?: printable" in dialog)
        assertFalse("if (readyPrintable != null || printable == null) AndroidView(" in dialog)
        val inliner = src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlImageInliner.kt")
        assertTrue("withContext(Dispatchers.Default)" in inliner)
        assertTrue("Semaphore(PARALLEL_DOWNLOADS)" in inliner)
        val main = src("app/src/main/java/ir/exam/app/MainActivity.kt")
        assertTrue("FormulaEditorPool.prepare(this)" in main)
        assertFalse("android.webkit" in main)
    }

    @Test
    fun remainingScreensShowCachedDataFirst() {
        listOf(
            "ui/bank/QuestionBankViewModel.kt", "ui/calendar/CalendarViewModel.kt",
            "ui/manager/ManagerFoundationScreens.kt", "ui/dashboard/TeacherManagerRequestsScreen.kt"
        ).forEach { rel ->
            val s = src("app/src/main/java/ir/exam/app/$rel")
            assertTrue("no SessionCache.get in $rel", "SessionCache.get<" in s)
            assertTrue("no SessionCache.put in $rel", "SessionCache.put(" in s)
        }
    }

    @Test
    fun studentStripChipsAreLightweight() {
        val student = src("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt")
        assertTrue("items(exam.questions.size, key = { i -> exam.questions[i].id })" in student)
        val chip = student.substringAfter("private fun StripChipCell(").substringBefore(") {")
        assertFalse("state: StudentExamUiState" in chip)
        assertTrue("answered: Boolean" in chip && "flagged: Boolean" in chip && "selectedChip: Boolean" in chip)
    }
}
