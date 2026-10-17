package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V311.2 — نمایش مستقیم محصولات، بسته‌های شارژ کیف پول و تعرفهٔ خدمات به تومان در صفحهٔ اصلی سایت (الزام شاپرک). */
class V311_2_SiteLandingCatalogTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/app.js").isFile }
    private fun src(p: String): String = File(root(), p).readText()

    @Test
    fun `landing page displays products packages prices and tariffs on desktop mobile and html`() {
        val app = src("site/src/app.js")
        val mob = src("site/src/mobile.js")
        val tpl = src("site/src/template.html")
        val css = src("site/src/site.css")
        assertTrue("window.LP_PRODUCTS = [" in app)
        assertTrue("window.LP_TARIFFS = [" in app)
        assertTrue("function buildLandingCatalog(card, mode)" in app)
        assertTrue("خرید و شارژ آنلاین" in app)
        assertTrue("function buildMobileCatalog()" in mob)
        assertTrue("بستهٔ اعتبار پایه (۱۰۰ سؤالی): ۱۰۰٬۰۰۰ تومان" in tpl)
        assertTrue(".lp-catalog{" in css && ".lp-prod-grid{" in css && ".ice-catalog{" in css)
    }
}
