package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V141 — سایت فاز ۴: تصحیح، مدیر، شارژ کیف پول، تقویم؛ قراردادها مطابق Supabase{Grading,Billing,Calendar,Manager}Repository. */
class V141_SitePhase4Test {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `admin module mirrors app contracts`() {
        val js = source("site/src/admin.js")
        assertTrue("native_save_grade" in js && "p_grades" in js && "p_feedback" in js)
        assertTrue("native_monitor_list_v1" in js && "exam_live_status" in js && "exam_attendance" in js)
        assertTrue("extend_student_time" in js && "reset_student_attempt" in js)
        assertTrue("native_question_analysis_v1" in js && "native_bulk_save_question_grades_v1" in js && "native_finalize_bulk_grades_v1" in js)
        assertTrue("approve_auto_grades" in js && "exam_autograde_info" in js)
        assertTrue("native_my_answer_detail_v1" in js)
        assertTrue("wallet-payment" in js && "amount_toman" in js)
        assertTrue("cal_month" in js && "holidays_for" in js && "cal_save_note" in js && "cal_delete_note" in js && "cal_mark_seen_v59" in js)
        assertTrue("native_manager_teachers_v37" in js && "native_manager_transfer_wallet_v38" in js && "p_operation" in js)
        assertTrue("native_manager_teacher_classes_v40c" in js && "native_manager_class_roster_v40c" in js && "native_manager_set_class_student_v40c" in js)
        assertTrue("native_manager_change_teacher_class_v41" in js && "approval_required" in js)
        // الگوریتم جلالی (jalaali-js)
        assertTrue("[-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178]" in js)
    }

    @Test
    fun `app wires phase 4 pages and build includes admin module`() {
        val app = source("site/src/app.js")
        assertTrue("SiteAdmin.gradingPage" in app && "SiteAdmin.calendarPage" in app && "SiteAdmin.managerSchoolPage" in app)
        assertTrue("SiteAdmin.managerTeachersPage" in app && "SiteAdmin.topUpCard" in app && "SiteAdmin.answerDetail" in app)
        assertTrue("['calendar', '📅', 'تقویم و پیام‌ها']" in app)
        assertTrue("admin.js" in source("site/build_site.py"))
        val html = source("site/index.html")
        assertTrue("window.SiteAdmin" in html && "cal_month" in html)
    }
}
