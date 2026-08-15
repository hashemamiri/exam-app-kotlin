package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V40ATeacherProfileStudentMenuTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `teacher details sql is owner only and validates optional fields`() {
        val sql = source("supabase/migrations/20260815_native_teacher_profile_v40a.sql")
        assertEquals(sql, source("sql/manual/SQL_NATIVE_TEACHER_PROFILE_V40A.sql"))
        listOf("employee_code", "phone", "native_my_teacher_details_v40", "native_save_teacher_details_v40", "id=auth.uid() and role='teacher'", "^09[0-9]{9}$").forEach {
            assertTrue("missing $it", it in sql)
        }
    }

    @Test
    fun `teacher profile has display card and teacher details card`() {
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        val vm = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt")
        val repo = source("app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt")
        listOf("نام نمایشی", "مشخصات معلم", "کد پرسنلی", "شماره تلفن").forEach { assertTrue(it in profile) }
        listOf("setFirstName", "setLastName", "setEmployeeCode", "setPhone").forEach { assertTrue(it in vm) }
        assertTrue("native_my_teacher_details_v40" in repo)
        assertTrue("native_save_teacher_details_v40" in repo)
    }

    @Test
    fun `student account hides credential mutation and email`() {
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        val account = profile.substringAfter("private fun AccountSection(").substringBefore("private fun AccountAccordionCard(")
        assertTrue("if (role != UserRole.STUDENT)" in account)
        assertTrue("if (role != UserRole.STUDENT) {\n                    LabeledValue(\"ایمیل\"" in account)
        assertFalse("حساب مدیریت‌شده توسط معلم" in account)
    }

    @Test
    fun `student menu exact two-column order has no data card`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        val student = app.substringAfter("// ترتیب دقیق دانش‌آموز:").substringBefore("Neumorphic69Provider")
        val labels = listOf("\"آزمون\"", "\"نتایج من\"", "\"تقویم\"", "\"حساب\"", "\"تنظیمات\"", "\"خروج\"")
        labels.zipWithNext().forEach { (a, b) -> assertTrue(student.indexOf(a) < student.indexOf(b)) }
        assertFalse("\"داده‌ها\"" in student)
    }

    @Test
    fun `appearance and about controls are centered`() {
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        assertTrue("Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)" in profile)
        assertFalse("horizontalScroll(rememberScrollState())" in profile)
    }
}
