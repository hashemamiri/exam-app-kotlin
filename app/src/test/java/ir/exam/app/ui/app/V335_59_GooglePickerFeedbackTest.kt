package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V335.59/V335.61 — بازخورد فوری تا بازشدن انتخاب‌گر حساب گوگل (بدون متن، با انیمیشن). */
class V335_59_GooglePickerFeedbackTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `native picker shows the google animation instead of a waiting sentence`() {
        val screen = src("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt")
        assertTrue("var openingGooglePicker by remember(role, registering)" in screen)
        assertTrue("GooglePickerWaitingDialog(onCancel" in screen)
        assertTrue("private fun GoogleDotsAnimation()" in screen)
        assertFalse("در حال باز کردن فهرست حساب‌های گوگل" in screen)
        assertTrue("enabled = !state.isLoading && !openingGooglePicker" in screen)
    }

    @Test
    fun `website announces OAuth redirect before navigating and restores on timeout`() {
        val extras = src("site/src/extras.js")
        assertTrue("در حال انتقال به انتخاب حساب گوگل" in extras)
        assertTrue("google-auth-dots" in extras) // V335.62 — انیمیشن چهارنقطه‌ای جای متن انتظار
        assertTrue("requestAnimationFrame(function () { location.href = targetUrl; })" in extras)
        assertTrue("button.innerHTML = originalHtml" in extras)
        val css = src("site/src/site.css")
        assertTrue(".google-auth-dots" in css && "@keyframes google-auth-dot-wave" in css)
    }
}
