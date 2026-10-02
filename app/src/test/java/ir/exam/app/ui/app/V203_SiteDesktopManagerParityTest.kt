package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V203 — پنل مدیر دسکتاپ هم‌تراز اپ: کلاس‌های مدرسه + کلاس جدید، دانش‌آموزان مدرسه + ساخت تکی/گروهی، تقویم، مدرسهٔ جدید، پشتیبان. */
class V203_SiteDesktopManagerParityTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun src(name: String) = File(root(), "site/src/$name").readText()

    @Test fun admin_has_manager_classes_and_students_pages() {
        val a = src("admin.js")
        assertTrue("async function managerClassesPage(c)" in a)
        assertTrue("async function managerStudentsPage(c, arg)" in a)
        assertTrue("native_manager_save_teacher_class_v40c" in a && "native_manager_school_students_v40c" in a)
        assertTrue("native_manager_set_class_student_v40c', {p_class: sel.value, p_student: s.id, p_add: true}" in a)
        assertTrue("native_manager_create_school_v61" in a && "native_manager_export_backup_v61" in a)
        assertTrue("managerClassesPage: managerClassesPage, managerStudentsPage: managerStudentsPage" in a)
        assertTrue("window.SiteSchool.studentForm(null, [{id: arg.classId" in a)
    }

    @Test fun school_exports_forms_and_app_routes_manager() {
        assertTrue("studentForm: studentForm, bulkForm: bulkForm, credentialDlg: credentialDlg, manageStudent: manageStudent" in src("school.js"))
        val app = src("app.js")
        assertTrue("user.role === 'manager' && window.SiteAdmin && window.SiteAdmin.managerClassesPage" in app)
        assertTrue("user.role === 'manager' && window.SiteAdmin && window.SiteAdmin.managerStudentsPage" in app)
        assertTrue("['classes', '🏫', 'کلاس‌ها'], ['students', '🎓', 'دانش‌آموزان'], ['calendar', '📅', 'تقویم'], ['wallet', '👛', 'کیف پول']]" in app)
    }
}
