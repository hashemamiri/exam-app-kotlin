package ir.exam.app.ui.app
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class V41B1ProfileInviteRequestsHotfixTest {
 private fun root()=listOf(File("."),File("..")).first{File(it,"scripts/verify_native_final.py").isFile}
 private fun source(p:String)=File(root(),p).readText()
 @Test fun `profile and invite rpc grants are restored`(){val m=source("supabase/migrations/20260816_native_profile_grant_invite_requests_v41b1.sql");assertEquals(m,source("sql/manual/SQL_NATIVE_PROFILE_GRANT_INVITE_REQUESTS_V41B1.sql"));assertTrue("grant execute on function public.native_my_profile() to authenticated" in m);assertTrue("native_manager_revoke_invite_v40b(uuid)" in m)}
 @Test fun `invite deletion is optimistic and countdown includes seconds`(){val u=source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt");val remove=u.indexOf("invites = invites.filterNot");val call=u.indexOf("repository.revokeInvite",remove);assertTrue(remove>=0&&call>remove);assertTrue("%02d:%02d:%02d" in u);assertTrue("delay(1_000)" in u)}
 @Test fun `requests exist only in cards destination`(){val dashboard=source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt");val cards=source("app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt");val requests=source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherManagerRequestsScreen.kt");assertFalse("درخواست‌های مدیر" in dashboard);assertTrue("\"درخواست‌ها\"" in cards);assertTrue("درخواست‌های مدیر" in requests);assertTrue("MainPage.REQUESTS" in source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt"))}
}
