package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.62 — انیمیشن چهارنقطه‌ای گوگل (آبی/قرمز/زرد/سبز) داخل خودِ دکمهٔ «ورود/ثبت‌نام با گوگل» سایت
 * (هر دو پوستهٔ موبایل و دسکتاپ از `SiteExtras.googleButton` استفاده می‌کنند) جای متن «در حال انتقال به گوگل…».
 */
class V335_62_SiteGoogleDotsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `site google button shows four animated dots instead of waiting text`() {
        val extras = src("site/src/extras.js")
        assertTrue("google-auth-dots" in extras)
        assertTrue("<i></i><i></i><i></i><i></i>" in extras)
        assertFalse(("در حال انتقال به " + "گوگل…") in extras) // متن انتظار حذف شده
        // بازگردانی دکمه پس از ۱۲ ثانیه و غیرفعال‌سازی در طول انتظار حفظ شده است.
        assertTrue("button.innerHTML = originalHtml" in extras)
        assertTrue("button.disabled = true" in extras)
        assertTrue("aria-busy" in extras)
    }

    @Test
    fun `dots use the four google colors and a wave animation`() {
        val css = src("site/src/site.css")
        listOf("#4285F4", "#EA4335", "#FBBC05", "#34A853").forEach { assertTrue(it, it in css) }
        assertTrue("@keyframes google-auth-dot-wave" in css)
        assertTrue("prefers-reduced-motion" in css)
    }

    @Test
    fun `both site shells reuse the same google button`() {
        assertTrue("window.SiteExtras.googleButton(role)" in src("site/src/mobile.js"))
        assertTrue("window.SiteExtras.googleButton('teacher')" in src("site/src/app.js"))
    }
}
