package ir.exam.app.ui.app

import ir.exam.app.core.network.UserFacingError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.UnknownHostException

/** V209 — هیچ متن فنی به کاربر نشان داده نمی‌شود؛ خطای شبکه → پیام اینترنت؛ پیام فارسی معنادار می‌ماند. */
class V209_UserFacingErrorTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    @Test
    fun networkAndTechnicalMessagesBecomePersian() {
        val ktor = IllegalStateException("HTTP request to  (POST) failed with message: Unable to resolve host \"eazwuyrymsvdkwckdpco.supabase.co\": No address associated with hostname")
        assertEquals(UserFacingError.NETWORK, UserFacingError.of(ktor, "x"))
        assertEquals(UserFacingError.NETWORK, UserFacingError.of(RuntimeException("wrap", UnknownHostException("supabase.co")), "x"))
        assertEquals(UserFacingError.SERVER, UserFacingError.of(IllegalStateException("Could not find the function public.native_x (PGRST202)"), "x"))
        assertEquals(UserFacingError.SERVER, UserFacingError.of(IllegalStateException("java.lang.NullPointerException"), "x"))
        assertEquals("x", UserFacingError.of(IllegalStateException(""), "x"))
        assertEquals("x", UserFacingError.of(null, "x"))
    }

    @Test
    fun meaningfulPersianMessagesSurvive() {
        assertEquals("موجودی کافی نیست.", UserFacingError.of(IllegalStateException("موجودی کافی نیست."), "x"))
        assertEquals("آپلود به فضای ابری ناموفق بود", UserFacingError.of(IllegalStateException("آپلود به فضای ابری ناموفق بود: HTTP 500 Internal Server Error"), "x"))
        assertEquals("کد نادرست است.", UserFacingError.fromText("کد نادرست است. URL: https://x.supabase.co/rest Headers: apikey=abc", "x"))
        assertFalse(UserFacingError.of(IllegalStateException("خطا: Bearer abc.def apikey: zzz https://x.supabase.co"), "x").contains("supabase"))
    }

    @Test
    fun allSafeErrorHelpersUseUserFacingError() {
        val files = listOf(
            "ui/billing/BillingViewModel.kt", "ui/manager/ManagerFoundationScreens.kt", "ui/grading/GradingViewModel.kt",
            "ui/bank/QuestionBankViewModel.kt", "ui/calendar/CalendarViewModel.kt", "ui/reports/StudentResultsViewModel.kt",
            "ui/student/StudentExamViewModel.kt", "ui/dashboard/TeacherDashboardViewModel.kt", "ui/builder/ExamBuilderViewModel.kt",
            "ui/printing/ExamPrintCenterScreen.kt", "ui/profile/ProfileSettingsViewModel.kt", "ui/classes/ClassesViewModel.kt",
            "ui/portability/DataPortabilityViewModel.kt", "ui/update/UpdateViewModel.kt", "ui/auth/AuthViewModel.kt", "data/work/PendingActionWorker.kt"
        )
        files.forEach { rel ->
            val src = File(root(), "app/src/main/java/ir/exam/app/$rel").readText()
            assertTrue("missing UserFacingError in $rel", "UserFacingError." in src)
            // Auth/Update برای تشخیص کلیدواژه‌ها متن را خودشان تمیز می‌کنند ولی خروجی نهایی از UserFacingError.fromText می‌گذرد
            if (rel !in setOf("ui/update/UpdateViewModel.kt", "ui/auth/AuthViewModel.kt")) {
                assertFalse("raw sanitizer chain remains in $rel", ".substringBefore(\"Headers:\")" in src)
            } else assertTrue("UserFacingError.fromText(" in src)
        }
        val site = File(root(), "site/src/app.js").readText()
        assertTrue("var ERR_NETWORK = 'خطا در ارتباط با سرور؛" in site && "function errText(raw)" in site)
    }
}
