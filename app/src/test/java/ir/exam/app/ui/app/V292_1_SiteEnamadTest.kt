package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V292.1 — نماد اعتماد الکترونیکی (اینماد) در صفحهٔ ورود دسکتاپ، صفحهٔ اول موبایل و «درباره». */
class V292_1_SiteEnamadTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun enamadBadgeIsOfficialAndPlaced() {
        val app = src("site/src/app.js")
        assertTrue("var ENAMAD_ID = '7284073', ENAMAD_CODE = 'jMyKBK6rprs9sP21KAmyzz1gB7ZuPySb';" in app)
        assertTrue("a.setAttribute('referrerpolicy', 'origin');" in app && "img.setAttribute('referrerpolicy', 'origin');" in app)
        assertTrue("img.setAttribute('src', 'https://trustseal.enamad.ir/logo.aspx?id=' + ENAMAD_ID + '&Code=' + ENAMAD_CODE);" in app)
        assertTrue("img.setAttribute('code', ENAMAD_CODE);" in app)
        assertTrue("el('div', {class: 'lp-badges'}, [enamadBadge()])" in app)
        assertTrue("enamadBadge: enamadBadge," in app)
        val mob = src("site/src/mobile.js")
        assertTrue("stag(7, el('div', {class: 'ice-badges'}, [S.enamadBadge()]))" in mob)
        assertTrue("el('span', {text: 'نماد اعتماد'}), S.enamadBadge()" in mob)
        val css = src("site/src/site.css")
        assertTrue(".enamad img{height:72px;width:auto;display:block}" in css && ".lp-badges{" in css)
    }
}
