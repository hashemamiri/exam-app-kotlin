package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V142 — سایت فاز ۵: مدیریت دانش‌آموزان، پیوستن به مدرسه، درخواست‌های مدیر، بانک سؤال؛ مطابق SupabaseSchoolRepository و manage-student. */
class V142_SitePhase5Test {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `school module mirrors app contracts`() {
        val js = source("site/src/school.js")
        assertTrue("/functions/v1/manage-student" in js)
        listOf("'create'", "'update'", "'reset_password'", "'delete'", "'bulk'").forEach { assertTrue(it, "action: $it" in js) }
        assertTrue("/^[a-z0-9_]{4,20}$/" in js)
        assertTrue("native_save_student_extra_v28" in js && "p_father_name" in js)
        assertTrue("add_students_to_class" in js && "native_add_student_to_classes_v22" in js && "remove_student_from_class" in js && "set_student_active" in js)
        assertTrue("native_teacher_add_class_student_to_list_v43" in js)
        assertTrue("native_teacher_share_class_v137" in js && "native_teacher_share_student_v136" in js)
        assertTrue("native_school_invite_preview_v39" in js && "native_join_school_v39" in js && "/^[A-Z0-9]{6}$/" in js)
        assertTrue("native_teacher_manager_requests_v41" in js && "native_teacher_decide_manager_request_v41" in js)
        assertTrue("native_bank_delete_question_v1" in js && "native_bank_set_categories_v1" in js && "native_bank_category_add_v1" in js && "native_bank_category_delete_v1" in js && "p_delete_questions" in js)
    }

    @Test
    fun `builder supports bank edit and app wires phase 5`() {
        val b = source("site/src/builder.js")
        assertTrue("native_bank_update_question_v1" in b && "bankEdit" in b)
        val app = source("site/src/app.js")
        assertTrue("SiteSchool.studentsPage" in app && "SiteSchool.rosterDlg" in app && "SiteSchool.classShareChip" in app)
        assertTrue("SiteSchool.joinSchoolCard" in app && "SiteSchool.managerRequestsCard" in app && "SiteSchool.bankPage" in app)
        assertTrue("['bank', '🏦', 'بانک سؤال']" in app)
        assertTrue("school.js" in source("site/build_site.py"))
        assertTrue("window.SiteSchool" in source("site/index.html"))
    }
}
