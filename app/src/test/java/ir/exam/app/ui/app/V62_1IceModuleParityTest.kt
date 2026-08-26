package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.1 — «همه چیز مثل ماژول آپلودشده» (azmoon-auth-compose):
 * - پالت دقیق ماژول (IceAccent 0284C7، متن 0C3D5C، هالهٔ 7DD3FC و ...).
 * - خوش‌آمد WelcomeScreen: لوگوی گرادیانی ۸۴dp + «ورود به حساب»/«ساخت حساب جدید».
 * - ورود/ثبت‌نام تک‌کارتی با RoleTabs سگمنتی لغزان سازگار با RTL.
 * - موج سه‌لایهٔ پایین (quadraticBezierTo)، برف هاله‌دار ۱۶ دانه‌ای،
 *   StaggeredItem با تأخیر ۵۵ms، ScreenHeader آیکون‌دار و StepIndicator
 *   با برچسب‌های «ایمیل/کد بازیابی/رمز جدید» و ارقام فارسی.
 * منطق دست‌نخورده: فقط پوسته (بک‌اند شبیه‌سازی ماژول عمداً وارد نشد).
 */
class V62_1IceModuleParityTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val components by lazy { source("app/src/main/java/ir/exam/app/ui/auth/AuthIceComponents.kt") }
    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }

    @Test
    fun `module palette and building blocks are imported verbatim`() {
        for (needle in listOf(
            "internal val IceAccent = Color(0xFF0284C7)",
            "internal val IceInk = Color(0xFF0C3D5C)",
            "internal val IceAccentLight = Color(0xFF38BDF8)",
            "internal val IceDisc = Color(0xFF7DD3FC)",
            "internal val IceFieldBg = Color(0xC0FFFFFF)",
            "internal fun RoleTabs(",
            "internal fun Brand()",
            "internal fun BrandHero()",
            "internal fun ScreenHeader(",
            "internal fun IceField(",
            "internal fun IceButton(",
            "internal fun IceOutlinedButton(",
            "internal fun StaggeredItem("
        )) assertTrue(needle, needle in components)
        // موج سه‌لایه و برف هاله‌دار ماژول
        assertTrue("quadraticBezierTo" in components)
        assertTrue("radius = r * 2.2f" in components)
        // stagger ماژول: تأخیر ۵۵ میلی‌ثانیه به‌ازای هر آیتم
        assertTrue("delay(index * 55L)" in components)
    }

    @Test
    fun `role tabs slide correctly in rtl and welcome matches the module`() {
        // V62.1.2 — گزارش دستگاه: نشانگر روی تب قرینه می‌نشست. ریشه:
        // Modifier.offset(x) خودش RTL-آگاه است؛ آینه‌سازی دستی ماژول جبران
        // دوباره می‌شد. offset منطقی مستقیم (Dp*Int به‌خاطر V62.1.1).
        assertTrue("targetValue = itemWidth * selected" in components)
        assertTrue("maxWidth - itemWidth - logicalOffset" !in components)
        // خوش‌آمد: ترتیب دکمه‌ها و یادآوری دانش‌آموز
        val landing = signIn.substringAfter("private fun LandingPane(")
            .substringBefore("private fun LoginPane(")
        assertTrue("BrandHero()" in landing)
        assertTrue(landing.indexOf("\"ورود به حساب\"") in 0 until landing.indexOf("\"ساخت حساب جدید\""))
        assertTrue("حساب دانش‌آموز را معلم می‌سازد" in landing)
    }

    @Test
    fun `login and signup are single cards with sliding tabs over untouched logic`() {
        // ورود: سه نقش در یک کارت؛ تب فقط صفحهٔ ViewModel را عوض می‌کند
        assertTrue("labels = listOf(\"مدیر/معاون\", \"معلم\", \"دانش‌آموز\")" in signIn)
        assertTrue("AuthScreen.LOGIN_STUDENT -> LoginPane(state, viewModel)" in signIn)
        // ثبت‌نام: معلم اول (ترتیب ماژول)
        assertTrue("labels = listOf(\"معلم\", \"مدیر/معاون\")" in signIn)
        // V62.1.2 — ورود پلکانی آیتم‌به‌آیتم ماژول (StaggeredItem) در فرم‌ها
        assertTrue("StaggeredItem(0) { Brand() }" in signIn)
        assertTrue("StaggeredItem(1) {" in signIn)
        // V62.1.3 — تعویض تب بدون remount انیمیشن نمی‌داد؛ key هویت را عوض می‌کند
        assertTrue("androidx.compose.runtime.key(selectedTab) {" in signIn)
        assertTrue("androidx.compose.runtime.key(managerTab) {" in signIn)
        // V62.1.3 — کارت مات بدون سایه: هالهٔ سایه مثل کادر دوم دیده می‌شد
        assertTrue(".background(Color.White)" in components)
        assertTrue("border(1.dp, IceStroke, RoundedCornerShape(24.dp))" in components)
        // مغز همان است: گوگل Credential Manager و قواعد سرور
        assertTrue("viewModel.signInWithGoogleIdToken(googleCredential.idToken, rawNonce, role)" in signIn)
        assertTrue("state.newPassword.length >= 8" in signIn)
        assertTrue("state.otp.length in 6..8" in signIn)
    }

    @Test
    fun `recovery keeps module step labels with persian digits`() {
        assertTrue("private val RecoverySteps = listOf(\"ایمیل\", \"کد بازیابی\", \"رمز جدید\")" in signIn)
        // مراحل: ۰=ایمیل، ۱=کد، ۲=رمز جدید
        assertTrue("StepIndicator(steps = RecoverySteps, current = 0)" in signIn)
        assertTrue("StepIndicator(steps = RecoverySteps, current = 1)" in signIn)
        assertTrue("StepIndicator(steps = RecoverySteps, current = 2)" in signIn)
        // ارقام فارسی مراحل (مثل ماژول)
        assertTrue("internal fun faNum(" in components)
        assertTrue("faNum(index + 1)" in components)
    }
}
