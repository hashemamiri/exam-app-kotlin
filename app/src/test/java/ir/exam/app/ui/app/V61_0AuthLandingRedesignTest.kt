package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.0 — بازطراحی صفحهٔ آغازین طبق درخواست کاربر:
 * - «آزمون آنلاین» بالای صفحه وسط‌چین؛ وسط صفحه «ورود» و پایین آن «ثبت‌نام».
 * - لمس ورود → دکمه‌های مدیر/معاون، معلم و دانش‌آموز؛ هر کدام پنجرهٔ اختصاصی.
 * - پنجرهٔ ورود معلم/مدیر دکمهٔ «ورود با گوگل» با لوگوی گوگل دارد (حساب
 *   جیمیلی موجود مستقیم وارد می‌شود؛ جیمیل تازه به تکمیل ثبت‌نام می‌رود).
 * - لمس ثبت‌نام → مدیر/معاون بالا و معلم پایین.
 * - همهٔ پنجره‌های ورود/ثبت‌نام دکمهٔ بازگشت وسط‌چین پایین دارند.
 */
class V61_0AuthLandingRedesignTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }
    private val authVm by lazy { source("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt") }

    @Test
    fun `landing shows centered title with login above register`() {
        assertTrue("textAlign = TextAlign.Center" in signIn)
        assertTrue("AuthScreen.SIGN_IN -> LandingPane(state, viewModel)" in signIn)
        val landing = signIn.substringAfter("private fun LandingPane(")
            .substringBefore("private fun LoginRolePane(")
        assertTrue(landing.indexOf("Text(\"ورود\")") < landing.indexOf("Text(\"ثبت‌نام\")"))
    }

    @Test
    fun `login role order is manager teacher student with dedicated panes`() {
        val rolePane = signIn.substringAfter("private fun LoginRolePane(")
            .substringBefore("private fun StaffLoginPane(")
        val manager = rolePane.indexOf("Text(\"مدیر/معاون\")")
        val teacher = rolePane.indexOf("Text(\"معلم\")")
        val student = rolePane.indexOf("Text(\"دانش‌آموز\")")
        assertTrue(manager in 0 until teacher && teacher < student)
        assertTrue("AuthScreen.LOGIN_MANAGER -> StaffLoginPane(state, viewModel, managerRole = true)" in signIn)
        assertTrue("AuthScreen.LOGIN_TEACHER -> StaffLoginPane(state, viewModel, managerRole = false)" in signIn)
        assertTrue("AuthScreen.LOGIN_STUDENT -> StudentLoginPane(state, viewModel)" in signIn)
        assertTrue("fun showLoginRole() = switchTo(AuthScreen.LOGIN_ROLE)" in authVm)
    }

    @Test
    fun `staff login has google button and registration roles are vertical`() {
        val staff = signIn.substringAfter("private fun StaffLoginPane(")
            .substringBefore("private fun StudentLoginPane(")
        assertTrue("Text(\"ورود با گوگل\")" in staff)
        assertTrue("if (managerRole) \"manager\" else \"teacher\"" in staff)
        // ثبت‌نام: مدیر/معاون بالای معلم
        val register = signIn.substringAfter("private fun RegistrationRolePane(")
            .substringBefore("private fun TeacherRegistrationPane(")
        assertTrue(register.indexOf("Text(\"مدیر/معاون\")") < register.indexOf("Text(\"معلم\")"))
    }

    @Test
    fun `every auth pane ends with a centered back button`() {
        // دکمهٔ بازگشت مشترک: وسط‌چین
        val back = signIn.substringAfter("private fun BackButtonRow(")
            .substringBefore("private fun RegistrationRolePane(")
        assertTrue("horizontalArrangement = Arrangement.Center" in back)
        assertTrue("Text(\"بازگشت\")" in back)
        // در پنجره‌های نقش/ورود/ثبت‌نام/بازیابی استفاده شده است
        assertTrue(signIn.split("BackButtonRow(").size - 1 >= 6)
    }
}
