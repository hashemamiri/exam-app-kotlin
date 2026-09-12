package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V182 — کوچک‌سازی خروجی سایت (rjsmin/rcssmin در site/tools)؛ دارایی‌های اپ و src سایت دست‌نخورده. */
class V182_SiteMinifyTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `build script minifies site and engines with vendored tools`() {
        val b = source("site/build_site.py")
        assertTrue("import rjsmin, rcssmin" in b && "SITE_NO_MINIFY" in b)
        assertTrue("min_css(site_css)" in b && "min_js(site_js)" in b)
        assertTrue("parts.append(\"<style>%s</style>\" % min_css(css))" in b && "parts.append(\"<script>%s</script>\" % min_js(js))" in b)
        assertTrue(File(root(), "site/tools/rjsmin.py").isFile && File(root(), "site/tools/rcssmin.py").isFile && File(root(), "site/tools/LICENSE-rjsmin-rcssmin.txt").isFile)
        val index = File(root(), "site/index.html")
        if (index.isFile) {
            assertTrue(index.length() < 700_000)
            assertTrue("var SUPABASE_ANON_KEY = \"…\";" in index.readText().take(4000))
        }
    }
}
