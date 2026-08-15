package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V39ShortInviteQuickAddExamTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `school invite is six character no email one use and 24 hours`() {
        val sql = source("supabase/migrations/20260815_native_short_school_invite_v39.sql")
        assertEquals(sql, source("SQL_NATIVE_SHORT_SCHOOL_INVITE_V39.sql"))
        listOf(
            "alter column email drop not null",
            "upper(substr(replace(gen_random_uuid()::text,'-',''),1,6))",
            "now()+interval '24 hours'",
            "used_at is null",
            "revoked_at is null",
            "native_school_invite_preview_v39",
            "native_join_school_v39",
            "school_invite_attempts_v39",
            ">=10"
        ).forEach { assertTrue("missing $it", it in sql) }
        assertTrue("email,null" in sql)
    }

    @Test
    fun `account has preview then confirm join school card`() {
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        val repo = source("app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolJoinRepository.kt")
        assertTrue("title = \"پیوستن به مدرسه\"" in profile)
        assertTrue("import androidx.compose.material3.IconButton" in profile)
        assertTrue("Icons.Outlined.Search" in profile)
        assertTrue("schoolJoinRepository.preview" in profile)
        assertTrue("تأیید و پیوستن" in profile)
        assertTrue("native_school_invite_preview_v39" in repo)
        assertTrue("native_join_school_v39" in repo)
    }

    @Test
    fun `quick add actions differ for manager and teacher`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        val add = source("app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt")
        assertTrue("primaryTitle: String = \"آزمون جدید\"" in add)
        assertTrue("primaryTitle = if (user.role == UserRole.MANAGER) \"دعوت معلم\" else \"آزمون جدید\"" in app)
        assertTrue("onCreateStudent = onCreateStudent" in app)
        assertTrue("onCreateClass = onCreateClass" in app)
        assertTrue("quickAddOpen && user.role != UserRole.STUDENT" in app)
        assertTrue("teacher?.role !== 'teacher' && teacher?.role !== 'manager'" in source("supabase/functions/manage-student/index.ts"))
    }

    @Test
    fun `student hamburger has centered exam card and search dialog`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        val menu = source("app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt")
        val home = source("app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt")
        assertTrue("featuredCard = if (user.role == UserRole.STUDENT)" in app)
        assertTrue("\"آزمون\", \"ورود با کد آزمون\"" in app)
        assertTrue("Icons.Outlined.Search" in app)
        assertTrue("studentJoinRequestKey += 1" in app)
        assertTrue("fillMaxWidth(.52f)" in menu)
        assertTrue("initialJoinCode" in home)
        assertTrue("viewModel.join()" in home)
    }

    @Test
    fun `manager-created school data is scoped but old data is untouched`() {
        val sql = source("supabase/migrations/20260815_native_short_school_invite_v39.sql")
        assertTrue("staff_role in('teacher','manager')" in sql)
        assertTrue("native_attach_created_student_v37" in sql)
        assertTrue("native_scope_new_school_row_v38" in sql)
        assertFalse("update public.classes set school_id" in sql)
        assertFalse("update public.exams set school_id" in sql)
    }
}
