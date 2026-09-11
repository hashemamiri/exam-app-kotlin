package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V146 — سایت برای گوشی و تبلت: منوی پایین، سایدبار کشویی با پس‌زمینه، جدول‌های اسکرول‌شونده. */
class V146_SiteMobileTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `site has responsive layers for tablet phone and touch`() {
        val css = source("site/src/site.css")
        assertTrue("@media (max-width:1024px)" in css && "@media (pointer:coarse)" in css)
        assertTrue(".bottom-nav{position:fixed;bottom:0" in css && ".tbl-wrap{width:100%;overflow-x:auto" in css)
        assertTrue(".field input,.field select,.field textarea{font-size:16px}" in css)
        val app = source("site/src/app.js")
        assertTrue("function toggleSidebar()" in app && "function closeSidebar()" in app && "function wrapTables(rootEl)" in app)
        assertTrue("el('nav', {class: 'bottom-nav'" in app && "[side, sbBg, main, bottom]" in app)
        assertTrue("viewport-fit=cover" in source("site/src/template.html"))
    }
}
