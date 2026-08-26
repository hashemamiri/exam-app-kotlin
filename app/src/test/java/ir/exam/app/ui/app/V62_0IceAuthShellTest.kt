package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.0 — پوستهٔ «یخی قطبی» صفحهٔ ورود (طرح پیشنهادی کاربر azmoon-auth-compose):
 * فقط UI؛ منطق (گوگل Credential Manager، نام کاربری کادر، کد دعوت، تکمیل
 * ثبت‌نام، قواعد رمز ۸-۷۲) همان مسیر تست‌شدهٔ AuthViewModel/Supabase می‌ماند.
 * اجزا: IceBackdrop (گرادیان+هاله+موج)، Snowfall فقط در بازیابی رمز،
 * IceAuthCard شیشه‌ای، OtpBoxes با فیلد مخفی (کد ۶ تا ۸ رقمی سوپابیس)،
 * StepIndicator سه‌مرحله‌ای بازیابی، StaggeredEntrance ورود پلکانی فرم.
 */
class V62_0IceAuthShellTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val components by lazy { source("app/src/main/java/ir/exam/app/ui/auth/AuthIceComponents.kt") }
    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }

    @Test
    fun `ice components exist and otp accepts supabase code lengths`() {
        for (needle in listOf(
            "internal fun IceBackdrop(",
            "internal fun Snowfall(",
            "internal fun IceAuthCard(",
            "internal fun OtpBoxes(",
            "internal fun StepIndicator(",
            "internal fun StaggeredEntrance("
        )) assertTrue(needle, needle in components)
        // کد سوپابیس ۶ تا ۸ رقمی: باکس‌ها منعطف‌اند و فیلد مخفی Paste را می‌گیرد
        assertTrue("maxLength: Int = 8" in components)
        assertTrue("raw.filter(Char::isDigit).take(maxLength)" in components)
        assertTrue("val boxCount = maxOf(6, value.length.coerceAtMost(maxLength))" in components)
    }

    @Test
    fun `sign-in shell uses the ice skin without touching auth logic`() {
        assertTrue("IceBackdrop(Modifier.fillMaxSize())" in signIn)
        assertTrue("if (recoveryFlow) Snowfall(Modifier.fillMaxSize())" in signIn)
        assertTrue("IceAuthCard {" in signIn)
        // V62.1 — کلید ورود پلکانی گروهی شد تا جابه‌جایی تب کارت را محو نکند.
        assertTrue("StaggeredEntrance(key = entranceKey)" in signIn)
        // نوار مراحل فقط در جریان بازیابی (برچسب‌های ماژول)
        assertTrue("private val RecoverySteps = listOf(\"ایمیل\", \"کد بازیابی\", \"رمز جدید\")" in signIn)
        assertTrue("steps = RecoverySteps" in signIn)
        // OTP باکسی به ViewModel موجود وصل است و طول ۶..۸ حفظ شده
        assertTrue("OtpBoxes(\n        value = state.otp,\n        onValueChange = viewModel::setOtp," in signIn)
        assertTrue("state.otp.length in 6..8" in signIn)
        // منطق گوگل/دعوت دست‌نخورده (اسپات‌چک؛ needleهای کامل در تست‌های V60/V61)
        assertTrue("viewModel.signInWithGoogleIdToken(googleCredential.idToken, rawNonce, role)" in signIn)
        assertTrue("اگر مدیر مدرسه کد ۶ حرفی یا کد TCH داده است، آن را اینجا وارد کنید." in signIn)
    }
}
