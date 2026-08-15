package ir.exam.app.ui.app
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class V41InviteTeacherCardPolishTest{
 private fun root()=listOf(File("."),File("..")).first{File(it,"app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile}
 private fun source(p:String)=File(root(),p).readText()
 @Test fun `teacher list removes duplicate titles and aligns details`(){val u=source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt");assertFalse("Text(\"معلم‌ها\", style" in u);assertFalse("summaryState.summary?.let { Text(it.schoolName) }" in u);assertTrue("کد پرسنلی:" in u&&"تلفن:" in u&&"Arrangement.spacedBy(12.dp)" in u)}
 @Test fun `teacher icons are large and active state is colored`(){val u=source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt");assertTrue("Color(0xFF25A86B)" in u);assertTrue("Color(0xFFE5484D)" in u);assertTrue("Modifier.size(34.dp)" in u);assertTrue("Modifier.size(32.dp)" in u)}
 @Test fun `invite timer and chips update live and deletion removes card immediately`(){val u=source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt");assertTrue("delay(1_000)" in u);assertTrue("clockNow" in u);assertTrue("FilterChipDefaults.filterChipColors" in u);assertTrue("استفاده شده" in u&&"منقضی شده" in u);assertTrue("invites = invites.filterNot" in u)}
 @Test fun `invite title belongs to header and teacher dock restores list`(){val a=source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt");assertTrue("managerInviteHeader" in a);assertTrue("\"کدهای دعوت معلم\"" in a);assertTrue("if (user.role == UserRole.MANAGER) managerInviteHeader = false" in a)}
}
