package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V260.5 — سایت: لوگوی جدید در فرود گوشی، خطای ورود داخل کارت، تبلت ۸۶۱–۱۰۲۴ دوستونه، کش نسخهٔ APK، فایل _headers. */
class V260_5_SiteQaFixesTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `qa fixes are present`() {
        val m = source("site/src/mobile.js")
        assertTrue("el('div', {class: 'ice-hero'}, [el('img', {src: '/pwa/v3/mark-256.png'" in m)
        assertTrue("if (card) card.appendChild(msg); else wrap.appendChild(msg);" in m)
        val c = source("site/src/site.css")
        assertTrue("@media(min-width:861px) and (max-width:1024px){\n  .lp{grid-template-columns:1fr 400px}" in c)
        assertTrue(".ice-card .ice-err{background:rgba(185,28,28,.08)" in c)
        val a = source("site/src/app.js")
        assertTrue("localStorage.getItem('examsite.apk.v1')" in a)
        val h = source("site/_headers")
        assertTrue("X-Content-Type-Options: nosniff" in h && "Strict-Transport-Security: max-age=31536000" in h)
    }
}
