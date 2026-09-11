package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V162 — سرعت سایت: موتورهای چاپ/فرمول در engines.<hash>.js جداگانه با بارگذاری تنبل و کش دائمی؛ صفحهٔ «تنظیمات» گوشی مثل اپ (ظاهر/داده‌ها/درباره). */
class V162_SiteSpeedSettingsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `engines are split out of index and lazy loaded`() {
        val b = source("site/build_site.py")
        assertTrue("engines_name = \"engines.%s.js\"" in b && "window.__ENGINES_URL" in b)
        val a = source("site/src/app.js")
        assertTrue("function loadEngines()" in a && "function prefetchEngines()" in a && "sc.src = url" in a)
        assertTrue("iframe.srcdoc = engineHtml(" !in a && "f.srcdoc = S.engineHtml(" !in source("site/src/builder.js") && "f.srcdoc = S.engineHtml(" !in source("site/src/student.js"))
        assertTrue("cp site/engines.*.js site_out/" in source(".github/workflows/site.yml") && "max-age=31536000, immutable" in source(".github/workflows/site.yml"))
        assertTrue("engines\\.[0-9a-f]+\\.js" in source("site/pwa/sw.js"))
        val index = File(root(), "site/index.html")
        if (index.isFile) {
            assertTrue("index.html باید سبک باشد", index.length() < 1_500_000)
            assertTrue("window.__ENGINES = {" !in index.readText())
            assertTrue(root().resolve("site").listFiles()!!.any { it.name.startsWith("engines.") && it.name.endsWith(".js") })
        }
    }

    @Test
    fun `mobile settings screen mirrors AppearanceSection`() {
        val m = source("site/src/mobile.js")
        assertTrue("function settingsScreen(" in m && "page === 'tools') settingsScreen(content)" in m)
        for (t in listOf("حالت نمایش", "چیدمان دستگاه", "ظاهر نئومورفیک — پالت رنگ", "ظاهر نئومورفیک — عمق سایه", "ظاهر نئومورفیک — پیش‌نمایش", "قلم فارسی", "اندازه متن", "بازگردانی تنظیمات ظاهری", "native_manager_export_backup_v61")) assertTrue(t, t in m)
        assertTrue("'#6C63F5', '#27C4A8'" in m && "'#1877D2', '#32B7C6'" in m && "'#E96D8A', '#FFA14E'" in m && "'#8C5AD7', '#EC6DA7'" in m)
        val css = source("site/src/site.css")
        assertTrue(".m-dark .m-mode{" in css && "var(--m-depth,14px)" in css && ".m-switch{" in css && ".m-pal{" in css)
    }
}
