package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V40BManagerTeacherCardsInvitesTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it,"app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun source(path:String)=File(root(),path).readText()

    @Test fun `batch invite sql creates one to five separate 24 hour codes`() {
        val sql=source("supabase/migrations/20260815_native_manager_teacher_cards_v40b.sql")
        assertEquals(sql,source("sql/manual/SQL_NATIVE_MANAGER_TEACHER_CARDS_V40B.sql"))
        listOf("p_count not between 1 and 5","for i in 1..p_count","display_code","interval '24 hours'","native_manager_invites_v40b","native_manager_revoke_invite_v40b").forEach{assertTrue(it in sql)}
    }

    @Test fun `teacher card has details and four hidden icon actions`() {
        val ui=source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        listOf("کد پرسنلی:","شماره تلفن:","expandedTeacher == teacher.id","Icons.Outlined.ToggleOn","Icons.Outlined.Login","Icons.Outlined.AccountBalanceWallet","Icons.Outlined.Delete").forEach{assertTrue(it in ui)}
        assertTrue("Modifier.fillMaxWidth().clickable" in ui)
    }

    @Test fun `invite mode hides teachers and has centered batch creator and status cards`() {
        val ui=source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("if (inviteMode)" in ui)
        assertTrue("Arrangement.Center" in ui)
        assertTrue("(1..5).forEach" in ui)
        assertTrue("زمان باقی‌مانده:" in ui)
        assertTrue("استفاده شده" in ui && "استفاده نشده" in ui)
        assertTrue("حذف کد دعوت" in ui)
    }

    @Test fun `active toggle and removal preserve auth`() {
        val sql=source("supabase/migrations/20260815_native_manager_teacher_cards_v40b.sql")
        assertTrue("status in('active','disabled','removed')" in sql)
        assertTrue("native_manager_set_teacher_active_v40b" in sql)
        assertTrue("native_manager_remove_teacher_v40b" in sql)
        assertFalse("deleteUser" in sql)
    }
}
