package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V164 — صفحهٔ ورود دسکتاپ به سبک اپ (نئومورفیک، فرم داخل کارت، بدون پنجرهٔ بازشو) + قلم وزیرمتن روی سایت. */
class V164_SiteLoginPageTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `vazirmatn font is shipped and cached`() {
        for (w in listOf(400, 700, 800)) assertTrue("font $w", File(root(), "site/fonts/Vazirmatn-$w.woff2").length() > 20_000)
        val css = source("site/src/site.css")
        assertTrue("src:url(/fonts/Vazirmatn-400.woff2) format('woff2')" in css && "font-weight:800 900" in css)
        val wf = source(".github/workflows/site.yml")
        assertTrue("cp -r site/fonts site_out/fonts" in wf && "/fonts/*\\n  Cache-Control: public, max-age=31536000, immutable" in wf)
        assertTrue("url.pathname.indexOf('/fonts/') === 0" in source("site/pwa/sw.js"))
    }

    @Test
    fun `landing is the app-style login page`() {
        val a = source("site/src/app.js")
        assertTrue("function renderLanding(mode)" in a && "class: 'lp'" in a && "drawAuthInto(card, mode || 'login')" in a)
        assertTrue("function drawAuthInto(m, mode)" in a && "function eyeButton(inp)" in a)
        assertFalse("modal-bg', onclick: function (e) { if (e.target === bg) closeAuth(); }" in a)
        assertFalse("class: 'hero'" in a || "class: 'roles'" in a)
        assertTrue("'ورود با کد ایمیل'" in a && "'فراموشی رمز'" in a && "googleButton('teacher')" in a)
        // V164.1 — بدون پانویس و دموها (درخواست کاربر)
        assertFalse("class: 'demos'" in a || "class: 'foot'" in a)
        assertTrue("'دانش‌آموزان نیازی به ثبت‌نام ندارند؛ معلم برایشان حساب می‌سازد.'" in a)
        val css = source("site/src/site.css")
        assertTrue(".lp{--bg:#E9EEF5" in css && ".m-dark .lp{--bg:#1F2530" in css && "--cta1:#5B52E8;--cta2:#0A8571" in css && ".lp .eye{" in css)
    }
}
