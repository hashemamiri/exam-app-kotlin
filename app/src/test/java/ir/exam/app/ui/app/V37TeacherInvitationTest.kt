package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** رگرسیون V37: دعوت ایمیل‌محور، عضویت مدرسه و قطع عضویت بدون حذف Auth. */
class V37TeacherInvitationTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `sql copy is exact and invitation is hashed expiring and email bound`() {
        val migration = source("supabase/migrations/20260815_native_school_teacher_management_v37.sql")
        assertEquals(migration, source("SQL_NATIVE_SCHOOL_TEACHER_MANAGEMENT_V37.sql"))
        listOf(
            "school_teacher_invites",
            "encode(digest(v_token,'sha256'),'hex')",
            "expires_at>now()",
            "v_inv.email<>v_email",
            "native_complete_teacher_registration_v37",
            "native_manager_remove_teacher_v40b",
            "teacher_membership_disabled"
        ).forEach { assertTrue("missing $it", it in migration) }
        assertFalse("admin.deleteUser" in migration)
    }

    @Test
    fun `teacher setup accepts optional TCH invitation`() {
        val screen = source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt")
        val vm = source("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt")
        val repo = source("app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt")
        assertTrue("کد دعوت مدرسه (اختیاری)" in screen)
        assertTrue("teacherInviteCode" in vm)
        assertTrue("completeInvitedTeacherRegistration" in vm)
        assertTrue("native_complete_teacher_registration_v37" in repo)
        assertTrue("startsWith(\"TCH-\")" in repo)
    }

    @Test
    fun `manager creates invitation and only disables membership`() {
        val manager = source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        val repo = source("app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt")
        assertTrue("ساخت کد دعوت" in manager)
        assertTrue("ساخت کد دعوت" in manager)
        assertTrue("زمان باقی‌مانده:" in manager)
        assertTrue("حذف معلم از مدرسه" in manager)
        assertTrue("native_manager_teachers_v37" in repo)
        assertTrue("native_manager_create_teacher_invites_v40b" in repo)
        assertTrue("native_manager_remove_teacher_v40b" in repo)
        assertFalse("deleteUser" in repo)
    }

    @Test
    fun `new school teacher students attach through the service side helper`() {
        val edge = source("supabase/functions/manage-student/index.ts")
        assertEquals(2, edge.split("native_attach_created_student_v37").size - 1)
        assertTrue("p_actor: teacherId" in edge)
        assertTrue("p_student: studentId" in edge)
    }
}
