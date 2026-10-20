package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V321 — سرعت (پیش‌بارگذاری موتورها)، آفلاین (Service Worker) و دارک‌مود در سایت. */
class V321_Site_SpeedOfflineDarkTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun enginesPrefetchStartsOnFirstInteraction() {
        val js = File(root(), "site/src/app.js").readText()
        assertTrue("pointerdown" in js && "keydown" in js && "scroll" in js)
        assertTrue("setTimeout(start, 6000)" in js)
        assertTrue("saveData" in js)
    }

    @Test
    fun serviceWorkerKeepsEngineAndShellOffline() {
        val sw = File(root(), "site/pwa/sw.js").readText()
        assertTrue("azmoon-shell-" in sw)
        assertTrue("engines" in sw && "panels" in sw)
        assertTrue("/fonts/" in sw)
    }

    @Test
    fun darkModeClassStillAppliedBySite() {
        val css = File(root(), "site/src/site.css").readText()
        assertTrue(".m-dark" in css)
        val html = File(root(), "site/index.html").readText()
        assertTrue("m-dark" in html)
    }
}
