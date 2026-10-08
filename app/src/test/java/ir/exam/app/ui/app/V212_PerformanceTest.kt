package ir.exam.app.ui.app

import ir.exam.app.data.dto.ExamDashboardDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V212 — سرعت فاز ۱: ستون‌های محدود، حافظهٔ موقت صفحه‌ها، شروع سریع، رمزگشایی خارج از نخ اصلی، Baseline Profile. */
class V212_PerformanceTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun examListQueriesSelectOnlyCardColumns() {
        assertEquals("id,title,subject,duration,code,is_open,total_score,created_at", ExamDashboardDto.COLUMNS)
        listOf(
            "app/src/main/java/ir/exam/app/data/repository/SupabaseTeacherDashboardRepository.kt",
            "app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt"
        ).forEach { rel ->
            val s = src(rel)
            assertTrue(rel, "from(\"exams\").select(Columns.raw(ExamDashboardDto.COLUMNS))" in s)
        }
        val grading = src("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt")
        assertTrue("select(Columns.raw(\"id,student_id,graded\"))" in grading)
        assertTrue("withContext(Dispatchers.Default) { ExamQuestionCodec.decode(" in grading)
    }

    @Test
    fun screensShowCachedDataFirst() {
        listOf(
            "ui/dashboard/TeacherDashboardViewModel.kt", "ui/grading/GradingViewModel.kt", "ui/reports/ReportsViewModel.kt",
            "ui/billing/BillingViewModel.kt", "ui/reports/StudentResultsViewModel.kt", "ui/classes/ClassesViewModel.kt"
        ).forEach { rel ->
            val s = src("app/src/main/java/ir/exam/app/$rel")
            assertTrue("no SessionCache.get in $rel", "SessionCache.get<" in s)
            assertTrue("no SessionCache.put in $rel", "SessionCache.put(" in s)
        }
        val grading = src("app/src/main/java/ir/exam/app/ui/grading/GradingViewModel.kt")
        assertTrue("Semaphore(4)" in grading)
        assertTrue("repository.getAnswerStats(id)" in grading)
        assertFalse("repository.getAnswers(id).getOrDefault" in grading)
        val auth = src("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt")
        assertTrue("SessionCache.clear()" in auth)
    }

    @Test
    fun startupUsesCachedProfileImmediately() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt")
        assertTrue("return@runCatching persistUser(cachedUser)" in repo)
        assertTrue("backgroundScope.launch" in repo)
        val gradle = src("app/build.gradle.kts")
        assertTrue("androidx.profileinstaller:profileinstaller" in gradle)
        val prof = src("app/src/main/baseline-prof.txt")
        assertTrue("HSPLir/exam/app/**->**(**)**" in prof)
    }
}
