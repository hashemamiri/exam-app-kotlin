package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V41InviteTeacherCardPolishTest {
    private fun root() = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `teacher list removes duplicate titles and aligns details`() {
        val ui = source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertFalse("Text(\"معلم‌ها\", style" in ui)
        assertFalse("summaryState.summary?.let { Text(it.schoolName) }" in ui)
        assertTrue("کد پرسنلی:" in ui)
        assertTrue("تلفن:" in ui)
        assertTrue("Arrangement.spacedBy(12.dp)" in ui)
    }

    @Test
    fun `teacher icons are large and active state is colored`() {
        val ui = source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("Color(0xFF25A86B)" in ui)
        assertTrue("Color(0xFFE5484D)" in ui)
        assertTrue("Modifier.size(34.dp)" in ui)
        assertTrue(ui.split("Modifier.size(32.dp)").size - 1 >= 3)
        assertTrue("Icons.Outlined.Login" in ui)
        assertTrue("Icons.Outlined.AccountBalanceWallet" in ui)
        assertTrue("Icons.Outlined.Delete" in ui)
    }

    @Test
    fun `invite timer chips and immediate removal exist`() {
        val ui = source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("delay(1_000)" in ui)
        assertTrue("clockNow" in ui)
        assertTrue("FilterChipDefaults.filterChipColors" in ui)
        assertTrue("استفاده شده" in ui)
        assertTrue("منقضی شده" in ui)
        assertTrue("invites = invites.filterNot" in ui)
    }

    @Test
    fun `invite title belongs to header and teacher dock restores list`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("managerInviteHeader = managerInviteHeader" in app)
        assertTrue("managerInviteHeader: Boolean" in app)
        assertTrue("\"کدهای دعوت معلم\"" in app)
        assertTrue("managerInviteHeader = false" in app)
        assertTrue("managerTeacherListKey += 1" in app)
    }
    @Test
    fun `manager teacher dock always closes invite and management contexts`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        val ui = source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("managerTeacherListKey += 1" in app)
        assertTrue("managerTeacherId = null" in app)
        assertTrue("teacherListRequested = managerTeacherListKey" in app)
        assertTrue("LaunchedEffect(teacherListRequested)" in ui)
        assertTrue("inviteMode = false" in ui)
        assertTrue("reloadTeachers()" in ui)
    }

}
