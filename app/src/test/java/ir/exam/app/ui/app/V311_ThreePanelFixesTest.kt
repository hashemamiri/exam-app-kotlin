package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V311.0 — رفع ۱۰ اشکال تعامل بین سه پنل (مدیر، معلم و دانش‌آموز) در SQL، سایت، Edge Function و اندروید. */
class V311_ThreePanelFixesTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/app.js").isFile }
    private fun src(p: String): String = File(root(), p).readText()

    @Test
    fun `v311 sql migration covers multi school class student approval attendance and calendar fixes`() {
        val sql = src("supabase/migrations/20261016_native_three_panel_fixes_v311.sql")
        assertTrue("native_attach_created_student_v37" in sql)
        assertTrue("native_manager_set_class_student_v40c" in sql)
        assertTrue("native_add_student_to_classes_v22" in sql)
        assertTrue("native_manager_teacher_classes_v40c" in sql)
        assertTrue("order by m.joined_at asc limit 1" in sql)
        assertTrue("native_manager_save_teacher_class_v40c" in sql)
        assertTrue("p_school uuid default null" in sql)
        assertTrue("native_manager_remove_teacher_v40b" in sql)
        assertTrue("native_manager_delete_school_v1" in sql)
        assertTrue("native_manager_school_summary_v36" in sql)
        assertTrue("native_save_student_extra_v28" in sql)
        assertTrue("native_teacher_decide_manager_request_v41" in sql)
        assertTrue("exam_attendance(p_exam text)" in sql)
        assertTrue("exam_attend_summary(p_exam text)" in sql)
        assertTrue("cal_save_note(" in sql)
        assertTrue("cal_month(p_from date, p_to date)" in sql)
    }

    @Test
    fun `site admin and school js fix manager student creator requests analysis levels and calendar delete`() {
        val admin = src("site/src/admin.js")
        val school = src("site/src/school.js")
        assertTrue("very_easy: ['خیلی آسان', 'ok']" in admin)
        assertTrue("very_hard: ['خیلی سخت', 'warn']" in admin)
        assertTrue("weak_discrimination: ['تمیز ضعیف', 'danger']" in admin)
        assertTrue("buildManagerMyRequestsCard" in admin)
        assertTrue("full.teacher_id === myUid" in admin)
        assertTrue("S.user().role === 'teacher' || S.user().role === 'manager'" in admin)
        assertTrue("father_name: st.father.trim(), grade: st.grade.trim(), field_of_study: st.field.trim()" in school)
    }

    @Test
    fun `android calendar and school repository support manager calendar and student extra on approval`() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt")
        val calVm = src("app/src/main/java/ir/exam/app/ui/calendar/CalendarViewModel.kt")
        val calScreen = src("app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt")
        assertTrue("put(\"father_name\", request.fatherName.trim())" in repo)
        assertTrue("canEditCalendar" in calVm)
        assertTrue("role == UserRole.TEACHER || role == UserRole.MANAGER" in calScreen)
    }
}
