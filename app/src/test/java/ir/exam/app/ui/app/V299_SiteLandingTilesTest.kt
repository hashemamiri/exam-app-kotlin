package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V299 — صفحهٔ ورود سایت: ۸ بلوک معرفی دو ستونِ وسط‌چین با توضیح؛ اینماد زیر بلوک‌ها با پیوند متنی جایگزین در صورت نیامدن تصویر. */
class V299_SiteLandingTilesTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun eightTilesTwoColumnsAndEnamadFallback() {
        val app = src("site/src/app.js")
        for (k in listOf("bank", "live", "report", "print", "formula", "figure", "board", "school")) assertTrue(k, "      $k: '<" in app)
        // V299.1 — کاشی‌ها مثل قبل (آیکن + عنوان)؛ توضیح کامل در متن معرفی؛ چیدمان: معرفی | کاشی‌ها | کارت ورود
        for (t in listOf("آزمون‌ساز و بانک سؤال", "برگزاری آنلاین", "کارنامهٔ خودکار", "چاپ رسمی و Word", "فرمول و جدول تناوبی", "شکل، نمودار و محور", "تختهٔ سفید", "مدیریت مدرسه")) assertTrue(t, "'$t']" in app)
        assertTrue("var tilesEl = el('div', {class: 'tiles tiles8'}, tiles.map(" in app && "var left = el('div', {class: 'lp-l lp3'}, [intro, tilesEl]);" in app)
        assertTrue("el('p', {class: 'lp-more', text: 'سؤال تستی، تشریحی، جای‌خالی، جورکردنی و عددی" in app)
        assertTrue("el('div', {class: 'lp-badges'}, [enamadBadge()]) /* V292.1 — زیر توضیحات */\n    ]);" in app)
        assertTrue("img.addEventListener('error', function () { a.classList.add('enamad-txt'); img.remove(); a.textContent = 'نماد اعتماد الکترونیکی'; });" in app)
        assertTrue("a.style.display = 'none'" !in app)
        val css = src("site/src/site.css")
        assertTrue(".lp-l.lp3{flex-direction:row;align-items:center;justify-content:center;" in css && ".lp .tiles.tiles8{grid-template-columns:repeat(2,150px);" in css)
        assertTrue(".lp-badges{margin-top:22px;display:flex;gap:12px;align-items:center}" in css && ".enamad.enamad-txt{" in css)
        val mob = src("site/src/mobile.js")
        assertTrue("['🗂️', 'آزمون‌ساز و بانک سؤال'], ['⏱️', 'برگزاری آنلاین']" in mob && "['🏫', 'مدیریت مدرسه']]; /* V299" in mob)
        assertTrue(".ice-tiles{display:grid;grid-template-columns:repeat(2,1fr);" in css)
    }
}
