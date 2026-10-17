package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V311.2 / V311.3 — نمایش محصولات، بسته‌های شارژ کیف پول، تعرفهٔ خدمات (الزام شاپرک)، دکمه بالای ۸ بلوک، نمایش تضمین‌شدهٔ اینماد و اسکرول گوشی. */
class V311_2_SiteLandingCatalogTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun landingPageDisplaysProductsPricesAndBuyButtonsOnDesktopAndMobile() {
        val app = src("site/src/app.js")
        val mob = src("site/src/mobile.js")
        val adm = src("site/src/admin.js")
        val css = src("site/src/site.css")
        val tpl = src("site/src/template.html")

        assertTrue("window.LP_PRODUCTS = [" in app && "window.LP_TARIFFS = [" in app)
        for (price in listOf("۱۰۰٬۰۰۰ تومان", "۲۰۰٬۰۰۰ تومان", "۵۰۰٬۰۰۰ تومان", "۱٬۰۰۰٬۰۰۰ تومان")) {
            assertTrue(price, price in app && price in tpl)
        }
        assertTrue("function buildLandingCatalog(" in app && "خرید و شارژ آنلاین" in app && "💳 محصولات و تعرفه‌ها" in app)
        assertTrue("var tilesCol = el('div', {class: 'lp-tiles-col'});" in app && "left.replaceChild(tilesCol, tilesEl);" in app && "tilesCol.appendChild(priceBtn);" in app && "tilesCol.appendChild(tilesEl);" in app)
        assertTrue("img.addEventListener('load', function () { if (img.naturalWidth && img.naturalWidth > 16) a.classList.add('enamad-ok');" in app)
        assertTrue("function buildMobileCatalog()" in mob && "function mobileShowcase()" in mob && "ice-price-jump" in mob)
        assertTrue("sessionStorage.getItem('lp-selected-pkg')" in adm && "sessionStorage.getItem('lp-selected-pkg')" in app)
        assertTrue(".lp-catalog{" in css && ".lp-tiles-col{" in css && ".enamad:not(.enamad-ok){" in css && ".ice{overflow-x:hidden;overflow-y:visible;" in css)
    }
}
