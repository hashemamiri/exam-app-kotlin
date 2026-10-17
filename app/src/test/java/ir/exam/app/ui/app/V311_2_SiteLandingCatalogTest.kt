package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V311.2..V311.6 — پنجرهٔ میانی محصولات و تعرفه‌ها با لمس دکمهٔ «💳 محصولات و تعرفه‌ها»، تفکیک ۳ دکمهٔ صوت سؤال و اسکرول گوشی. */
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
        assertTrue("['صوت سؤال تا ۱ مگابایت', '۲٬۰۰۰ تومان']" in app && "['صوت سؤال تا ۲ مگابایت', '۴٬۰۰۰ تومان']" in app && "['صوت سؤال تا ۳ مگابایت', '۶٬۰۰۰ تومان']" in app)
        assertTrue("['بازیابی نسخهٔ پشتیبان (هر عکس)', '۱٬۰۰۰ تومان']" in app && ".lp-tariff-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}" in css)
        assertTrue("function openLandingCatalogModal(" in app && "خرید و شارژ آنلاین" in app && "💳 محصولات و تعرفه‌ها" in app)
        assertTrue("var tilesCol = el('div', {class: 'lp-tiles-col'});" in app && "left.replaceChild(tilesCol, tilesEl);" in app && "tilesCol.appendChild(priceBtn);" in app && "tilesCol.appendChild(tilesEl);" in app)
        assertTrue("img.setAttribute('alt', '');" in app && "img.style.cursor = 'pointer';" in app)
        assertTrue("function openMobileCatalog()" in mob && "function mobileShowcase()" in mob && "ice-price-jump" in mob)
        assertTrue("sessionStorage.getItem('lp-selected-pkg')" in adm && "sessionStorage.getItem('lp-selected-pkg')" in app)
        assertTrue(".lp-cat-modal{" in css && ".lp-tiles-col{" in css && ".ice{overflow-x:hidden;overflow-y:visible;" in css)
        assertTrue(".lp-tariff-head{display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;" in css && "stag(6, S.user() ? null : link('ساخت حساب جدید (معلم / مدیر)'" !in mob)
        val sw = src("site/pwa/sw.js")
        val hdr = src("site/_headers")
        assertTrue("function fetchTimed(" in sw && "Promise.race([" in sw && "function fetchWithTimeout(" in app && "Alt-Svc: clear" in hdr && "rel=\"preconnect\" href=\"https://eazwuyrymsvdkwckdpco.supabase.co\"" in tpl)
    }
}
