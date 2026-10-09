package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V248 — سئوی سایت: description/canonical/OG/JSON-LD، متن معرفی قابل‌ایندکس، robots.txt و sitemap.xml. */
class V248_SiteSeoTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, path).isFile) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `template has seo head tags and indexable intro`() {
        val t = source("site/src/template.html")
        assertTrue("<meta name=\"description\"" in t && "<link rel=\"canonical\" href=\"https://onlineexam.ir/\">" in t)
        assertTrue("property=\"og:title\"" in t && "application/ld+json" in t)
        assertTrue("class=\"seo-intro\"" in t && "<h1>" in t)
    }

    @Test
    fun `robots and sitemap exist and are deployed`() {
        assertTrue("Sitemap: https://onlineexam.ir/sitemap.xml" in source("site/robots.txt"))
        assertTrue("<loc>https://onlineexam.ir/</loc>" in source("site/sitemap.xml"))
        assertTrue("cp site/robots.txt site/sitemap.xml site_out/" in source(".github/workflows/site.yml"))
    }
}
