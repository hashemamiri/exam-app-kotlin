package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V147 — سایت به‌صورت PWA نصب‌شدنی (مانیفست، Service Worker، پیشنهاد نصب). */
class V147_SitePwaTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `manifest service worker and install flow are wired`() {
        val manifest = source("site/pwa/manifest.webmanifest")
        assertTrue("\"display\": \"standalone\"" in manifest && "\"dir\": \"rtl\"" in manifest && "maskable-512.png" in manifest)
        val sw = source("site/pwa/sw.js")
        assertTrue("'__SW_VERSION__'" in sw && "url.origin !== self.location.origin) return" in sw && "req.mode === 'navigate'" in sw)
        val app = source("site/src/app.js")
        assertTrue("navigator.serviceWorker.register('/pwa/sw.js')" in app && "'beforeinstallprompt'" in app && "function pwaOffer()" in app)
        val tpl = source("site/src/template.html")
        assertTrue("<link rel=\"manifest\" href=\"/pwa/manifest.webmanifest\">" in tpl && "apple-touch-icon" in tpl)
        val ci = source(".github/workflows/site.yml")
        assertTrue("cp -r site/pwa site_out/pwa" in ci && "replace('__SW_VERSION__', h)" in ci)
    }
}
