package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V335.59 — بازخورد فوری تا بازشدن انتخاب‌گر حساب گوگل. */
class V335_59_GooglePickerFeedbackTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `native picker shows progress and an explicit opening message`() {
        val screen = src("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt")
        assertTrue("var openingGooglePicker by remember(role, registering)" in screen)
        assertTrue("در حال باز کردن فهرست حساب‌های گوگل…" in screen)
        assertTrue("enabled = !state.isLoading && !openingGooglePicker" in screen)
        assertTrue("در حال ورود با گوگل…" in screen)
    }

    @Test
    fun `website announces OAuth redirect before navigating and restores on timeout`() {
        val extras = src("site/src/extras.js")
        assertTrue("در حال انتقال به انتخاب حساب گوگل" in extras)
        assertTrue("در حال انتقال به گوگل…" in extras)
        assertTrue("requestAnimationFrame(function () { location.href = targetUrl; })" in extras)
        assertTrue("button.innerHTML = originalHtml" in extras)
        val css = src("site/src/site.css")
        assertTrue(".google-auth-spinner" in css && "@keyframes google-auth-spin" in css)
    }
}
