package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V22ClassStudentCardsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").isFile
    }

    @Test
    fun `quick student opens bulk and class plus exposes exactly two actions`() {
        val school = File(root(), "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        assertTrue("SchoolLaunchAction.CREATE_STUDENT ->" in school)
        val quick = school.substringAfter("SchoolLaunchAction.CREATE_STUDENT ->").substringBefore("SchoolLaunchAction.CREATE_CLASS")
        assertTrue("showBulk = true" in quick)
        val roster = school.substringAfter("private fun ClassRosterContent(").substringBefore("private fun StudentsContent(")
        assertTrue("addMenuOpen" in roster)
        assertTrue("افزودن موجود" in roster)
        assertTrue("افزودن جدید" in roster)
        assertFalse("old class new-account button returned", "حساب جدید" in roster)
        assertFalse("old class bulk label returned", "ساخت گروهی" in roster)
    }

    @Test
    fun `member picker filters and student accordion controls are complete`() {
        val school = File(root(), "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        val picker = school.substringAfter("private fun MemberPickerDialog(").substringBefore("private fun StudentEditDialog(")
        listOf("دختر", "پسر", "همه پایه‌ها", "selected").forEach {
            assertTrue("missing member filter $it", it in picker)
        }
        val card = school.substringAfter("private fun StudentCard(").substringBefore("private fun ClassEditorDialog(")
        assertTrue("expanded = !expanded" in card)
        assertTrue("Color(0xFFFF80AB)" in card)
        assertTrue("Color(0xFF64B5F6)" in card)
        assertTrue("Icons.Outlined.ToggleOn" in card)
        assertTrue("Icons.Outlined.Edit" in card)
        assertTrue("Icons.Outlined.Add" in card)
        assertTrue("Icons.Outlined.ContentCopy" in card)
        assertTrue("selectedClasses" in card)
    }

    @Test
    fun `student edit uses optional new password and never reads old password`() {
        val root = root()
        val school = File(root, "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        val repository = File(root, "app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt").readText()
        val allMain = File(root, "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertTrue("رمز جدید اختیاری" in school)
        assertTrue("خالی بماند تغییر نمی‌کند" in school)
        assertTrue("request.newPassword.orEmpty()" in repository)
        assertFalse(
            "plain password field returned",
            Regex("\\b(val|var)\\s+plain_password\\b").containsMatchIn(allMain)
        )
        assertFalse("old password retrieval must not exist", "getPassword" in repository)
    }

    @Test
    fun `multi-class membership rpc is owner scoped and atomic`() {
        val root = root()
        val sql = File(root, "supabase/migrations/20260814_native_student_class_membership_v22.sql").readText()
        val repo = File(root, "app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt").readText()
        listOf(
            "native_add_student_to_classes_v22",
            "teacher_id = auth.uid()",
            "on conflict do nothing",
            "revoke all on function",
            "grant execute"
        ).forEach { assertTrue("missing V22 SQL marker $it", it in sql) }
        assertTrue("native_add_student_to_classes_v22" in repo)
    }

    @Test
    fun `hamburger swaps students and calendar positions and bulk accepts one`() {
        val root = root()
        val app = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        val teacherMenu = app.substringAfter("val menuCards = if (user.role == UserRole.TEACHER)")
            .substringBefore("} else {")
        assertTrue(teacherMenu.indexOf("دانش‌آموزان") < teacherMenu.indexOf("\"تقویم\""))
        val repository = File(root, "app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt").readText()
        assertTrue("requests.size in 1..100" in repository)
    }
}
