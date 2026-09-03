package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.4 — انتخاب پاسخنامه هنگام صدور فایل آزمون (بند ۳.۳ گزارش امنیتی):
 * فایل .azmoon همیشه کلید پاسخ را هم بسته‌بندی می‌کرد؛ حالا معلم پیش از صدور
 * بین «بدون پاسخنامه» (برای ارسال به دیگران) و «همراه پاسخنامه» (آرشیو خودش)
 * یکی را انتخاب می‌کند و در حالت اول کلید پاسخ اصلاً خوانده/نوشته نمی‌شود.
 */
class V75_4ExamExportAnswerKeyTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(relative: String): String = File(root(), relative).readText()

    private val codec by lazy { source("app/src/main/java/ir/exam/app/data/repository/ExamPackageCodec.kt") }
    private val repository by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt") }
    private val viewModel by lazy { source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardViewModel.kt") }
    private val screen by lazy { source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt") }

    @Test
    fun `codec can omit the answer key`() {
        assertTrue("fun encode(source: ExportedExam, includeAnswerKey: Boolean = true)" in codec)
        assertTrue("JsonArray(encoded.publicQuestions)" in codec)
        assertTrue("put(\"answer_key\", includeAnswerKey)" in codec)
        assertTrue("بدون-پاسخنامه" in codec)
    }

    @Test
    fun `repository does not even read the answer key when it is excluded`() {
        assertTrue("fun exportExam(examId: String, includeAnswerKey: Boolean = true)" in repository)
        assertTrue("if (includeAnswerKey) {" in repository)
    }

    @Test
    fun `view model forwards the choice`() {
        assertTrue("fun exportExam(examId: String, includeAnswerKey: Boolean = true)" in viewModel)
        assertTrue("portability.exportExam(examId, includeAnswerKey)" in viewModel)
    }

    @Test
    fun `teacher chooses explicitly before exporting`() {
        assertTrue("exportCandidate = exam" in screen)
        assertTrue("viewModel.exportExam(exam.id, false)" in screen)
        assertTrue("viewModel.exportExam(exam.id, true)" in screen)
        assertTrue("بدون پاسخنامه" in screen)
        assertTrue("همراه پاسخنامه" in screen)
    }
}
