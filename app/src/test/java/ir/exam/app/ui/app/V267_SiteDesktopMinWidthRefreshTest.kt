package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V267 — سایت: کوچک کردن پنجرهٔ دسکتاپ چیدمان گوشی نمی‌شود + رفرش به همان صفحه برمی‌گردد (پین‌های سایت). */
class V267_SiteDesktopMinWidthRefreshTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `mobile shell only on coarse pointer devices`() {
        val mobile = src("site/src/mobile.js")
        assertTrue("var MQ = window.matchMedia('(max-width: 860px) and (pointer: coarse)');" in mobile)
        val css = src("site/src/site.css")
        assertTrue("@media (pointer:fine){html{min-width:1024px}body{min-width:1024px}}" in css)
        assertTrue("@media (min-width:861px),(pointer:fine){" in css)
        assertFalse(Regex("@media ?\\(max-width:(860|600|380|700|760|900|1024)px\\)\\{").containsMatchIn(css))
    }

    @Test
    fun `refresh restores panel for same user`() {
        val app = src("site/src/app.js")
        assertTrue("var SS_VIEW = 'site-view';" in app)
        assertTrue("sessionStorage.setItem(SS_VIEW, JSON.stringify({uid: user.id, panel: view.panel, arg: view.arg === undefined ? null : view.arg}))" in app)
        assertTrue("if (!v || v.uid !== user.id || !v.panel) return;" in app)
        assertTrue(app.indexOf("restoreView();\n    render();\n    if (user) prefetchEngines();") > 0)
        assertTrue("sessionStorage.removeItem(SS_VIEW)" in app)
    }
}
