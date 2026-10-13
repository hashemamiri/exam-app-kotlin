package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V261 — پاریتهٔ اپ با سایت V260.8:
 * ۱) کارنامهٔ کلاس برای مدیر (native_manager_class_report_v1) داخل کلاس در پنل مدیر.
 * ۲) «درخواست‌های من» مدیر (native_manager_my_requests_v1) و پیام اطلاع‌رسانی به‌جای خطا
 *    وقتی ویرایش/حذف کلاس معلم نیاز به تأیید او دارد.
 * ۳) کارت درخواست‌های معلم: نام کلاس/دانش‌آموز هدف و «تغییر به: …».
 * ۴) «تأیید خودکار» پیام را از نتیجهٔ واقعی سرور (count/prefilled) می‌سازد.
 */
class V261_ManagerReportRequestsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `manager repository exposes class report, my requests and non-error change result`() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt")
        assertTrue("native_manager_class_report_v1" in repo)
        assertTrue("native_manager_my_requests_v1" in repo)
        assertTrue("ManagerChangeResult(true, \"درخواست برای تأیید معلم ارسال شد" in repo)
        assertTrue("error(raw.text(\"message\"))" !in repo)
    }

    @Test
    fun `manager class screen shows report and my requests`() {
        val screen = src("app/src/main/java/ir/exam/app/ui/manager/ManagerTeacherClassScreen.kt")
        assertTrue("Text(\"کارنامهٔ کلاس\"" in screen)
        assertTrue("repo.classReport(item.id)" in screen)
        assertTrue("\"درخواست‌های من\"" in screen)
        assertTrue("info=if(r.requested)r.message else null" in screen)
    }

    @Test
    fun `teacher requests show target name and change summary`() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseTeacherDashboardRepository.kt")
        assertTrue("text(\"target_name\")" in repo)
        assertTrue("\"تغییر به: \$it\"" in repo)
        val screen = src("app/src/main/java/ir/exam/app/ui/dashboard/TeacherManagerRequestsScreen.kt")
        assertTrue("request.targetName" in screen && "request.changeSummary" in screen)
        assertTrue("\"executed\" -> \"انجام شد\"" in screen)
    }

    @Test
    fun `auto approve message reflects server result`() {
        val vm = src("app/src/main/java/ir/exam/app/ui/grading/GradingViewModel.kt")
        assertTrue("r.approved > 0 ->" in vm && "r.prefilled > 0 ->" in vm)
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt")
        assertTrue("data class AutoApproveResult(val approved: Int, val prefilled: Int, val hasEssay: Boolean, val essayQuestions: List<Int>" in repo) // V270
    }
}
