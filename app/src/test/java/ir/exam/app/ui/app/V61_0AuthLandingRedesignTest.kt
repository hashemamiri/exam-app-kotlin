package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.0 — بازطراحی صفحهٔ آغازین + V62.1 هم‌ترازی کامل با ماژول یخی کاربر:
 * - خوش‌آمد: لوگوی بزرگ، «آزمون آنلاین»، «ورود به حساب» بالای «ساخت حساب جدید».
 * - ورود: هر سه نقش در یک کارت با تب‌های سگمنتی لغزان (مدیر/معاون، معلم،
 *   دانش‌آموز)؛ هر تب همان پنجرهٔ اختصاصی نقش V61.0 را نشان می‌دهد.
 * - پنجرهٔ ورود معلم/مدیر دکمهٔ «ورود با گوگل» با لوگوی گوگل دارد (حساب
 *   جیمیلی موجود مستقیم وارد می‌شود؛ جیمیل تازه به تکمیل ثبت‌نام می‌رود).
 * - ثبت‌نام: تب معلم اول و مدیر/معاون دوم (ترتیب ماژول).
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
    fun `landing is the module welcome with login above register`() {
        assertTrue("AuthScreen.SIGN_IN -> LandingPane(state, viewModel)" in signIn)
        val landing = signIn.substringAfter("private fun LandingPane(")
            .substringBefore("private fun LoginPane(")
        assertTrue("BrandHero()" in landing)
        assertTrue("به سامانهٔ آزمون و ارزشیابی خوش آمدید" in landing)
        assertTrue(
            landing.indexOf("\"ورود به حساب\"") in 0 until landing.indexOf("\"ساخت حساب جدید\"")
        )
        assertTrue("حساب دانش‌آموز را معلم می‌سازد" in landing)
    }

    @Test
    fun `login roles are segmented tabs with dedicated panes`() {
        // V62.1 — تب سگمنتی ماژول به‌جای دکمه‌های عمودی؛ ترتیب مدیر/معلم/دانش‌آموز.
        val loginPane = signIn.substringAfter("private fun LoginPane(")
            .substringBefore("private fun StaffLoginPane(")
        assertTrue("labels = listOf(\"مدیر/معاون\", \"معلم\", \"دانش‌آموز\")" in loginPane)
        assertTrue("viewModel.showManagerLogin()" in loginPane)
        assertTrue("viewModel.showTeacherLogin()" in loginPane)
        assertTrue("viewModel.showStudentLogin()" in loginPane)
        // چهار صفحهٔ ورود ViewModel همگی به همان کارت تب‌دار می‌روند.
        assertTrue("AuthScreen.LOGIN_STUDENT -> LoginPane(state, viewModel)" in signIn)
        assertTrue("fun showLoginRole() = switchTo(AuthScreen.LOGIN_ROLE)" in authVm)
    }

    @Test
    fun `staff login has google button and registration tabs put teacher first`() {
        val staff = signIn.substringAfter("private fun StaffLoginPane(")
            .substringBefore("private fun StudentLoginPane(")
        assertTrue("Text(\"ورود با گوگل\")" in staff)
        assertTrue("if (managerRole) \"manager\" else \"teacher\"" in staff)
        // ثبت‌نام: تب معلم اول (ترتیب SignupScreen ماژول)
        val register = signIn.substringAfter("private fun RegisterPane(")
            .substringBefore("private fun TeacherRegistrationPane(")
        assertTrue("labels = listOf(\"معلم\", \"مدیر/معاون\")" in register)
        assertTrue("viewModel.showTeacherRegistration()" in register)
        assertTrue("viewModel.showManagerRegistration()" in register)
    }

    @Test
    fun `every auth pane ends with a centered back button`() {
        // دکمهٔ بازگشت مشترک: وسط‌چین
        val back = signIn.substringAfter("private fun BackButtonRow(")
            .substringBefore("private fun RegisterPane(")
        assertTrue("horizontalArrangement = Arrangement.Center" in back)
        assertTrue("LinkTextButton(\"بازگشت\"" in back)
        // در پنجره‌های ورود/ثبت‌نام/بازیابی/کد استفاده شده است
        assertTrue(signIn.split("BackButtonRow(").size - 1 >= 6)
    }
}
