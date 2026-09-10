package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V138 — سایت تک‌فایلی (فاز ۱): منابع سازنده، قراردادهای RPC و بدون کلید واقعی. */
class V138_SiteSkeletonTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `site sources exist and mirror app RPC contracts`() {
        val js = source("site/src/app.js")
        listOf(
            "native_ensure_profile_v1", "native_my_registration_state_v1", "native_staff_login_email_v1",
            "native_complete_teacher_registration_v1", "native_complete_teacher_registration_v37", "native_join_school_v39",
            "native_complete_manager_registration_v36", "native_update_my_username_v1",
            "native_set_exam_open_v1", "native_delete_exam", "native_duplicate_exam_v2", "native_charge_print_v1",
            "native_my_classes_v28", "native_save_class_v28", "delete_class", "my_students", "class_roster",
            "native_wallet_snapshot", "native_my_profile", "native_save_profile_v28", "native_my_teacher_details_v40",
            "native_save_teacher_details_v40", "my_grades", "native_my_answers_v1",
            "native_manager_school_summary_v36", "native_manager_teachers_v37"
        ).forEach { assertTrue(it, "'$it'" in js) }
        assertTrue("select=id,title,subject,duration,code,is_open,total_score,created_at" in js)
        assertTrue("'@' + STUDENT_DOMAIN" in js && "student.exam.local" in js)
        assertTrue("/^[a-z0-9_]{4,20}\$/" in js)
        assertTrue("window.__printBridge = {" in js && "window.__formulaBridge = {" in js)
        assertTrue("ExamFormulaHost.begin(text || '', s, e)" in js)
        assertTrue("PRINT_COST_PER_Q = 1000" in js)
        assertTrue("ExamPrintBridge = window.parent.__printBridge" in source("site/build_site.py"))
        assertTrue("ExamEditorNative = window.parent.__formulaBridge" in source("site/build_site.py"))
        assertTrue("SUPABASE_URL = \"https://eazwuyrymsvdkwckdpco.supabase.co\"" in source("site/build_site.py"))
    }

    @Test
    fun `site template keeps anon key as placeholder and no secrets`() {
        val tpl = source("site/src/template.html")
        assertTrue("var SUPABASE_ANON_KEY = \"…\";" in tpl)
        assertFalse("service_role" in source("site/src/app.js"))
        val index = File(root(), "site/index.html")
        if (index.isFile) {
            val head = index.readText().take(4000)
            assertTrue("var SUPABASE_ANON_KEY = \"…\";" in head)
            assertFalse("eyJhbGci" in head)
        }
    }
}
