package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V339.2 — پوستهٔ اپ برای پیش‌نمایش کارنامه.
 *
 * قراردادهای نگهبان:
 *  • هدر ثابت است و اسکرول افقی نمی‌گیرد: سه دکمه در یک ردیف، زیرش نام قالب، زیرش ساختار جدول؛
 *  • برگهٔ A4 هم‌عرض صفحهٔ گوشی می‌شود (کوچک‌نمایی چیدمان، نه زوم WebView)؛
 *  • زوم دو‌انگشتی فقط روی برگه اثر دارد و هدر را بزرگ/جابه‌جا نمی‌کند؛
 *  • چاپ بدون زوم و روی A4 واقعی است؛
 *  • این پوسته فقط برای میزبان اندروید فعال می‌شود؛ رفتار سایت دست‌نخورده می‌ماند.
 */
class V339_2_ReportCardAppChromeTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "text/APP_VERSION.txt").isFile }
    private fun src(path: String) = File(root(), path).readText()

    private val js = "app/src/main/assets/reportcard/reportcard.js"
    private val css = "app/src/main/assets/reportcard/reportcard.css"
    private val dialog = "app/src/main/java/ir/exam/app/ui/reports/ReportCardPrintDialog.kt"
    private val siteHost = "site/src/reportcard.js"

    @Test
    fun `app skin is opt-in and the site keeps the old chrome`() {
        val engine = src(js)
        assertTrue("پوسته باید پیش‌فرض وب باشد", "skin: 'web'" in engine)
        assertTrue("پوستهٔ اپ فقط با درخواست میزبان", "opts.skin === 'app'" in engine)
        assertTrue("کلاس پوسته روی body", "document.body.classList.toggle('rc-app', isApp())" in engine)
        val host = src(siteHost)
        assertTrue("میزبان سایت همان فراخوانی قبلی را نگه می‌دارد", "w.ReportCard.open(payload);" in host)
        assertFalse("میزبان سایت نباید پوستهٔ اپ را درخواست کند", "skin" in host)
    }

    @Test
    fun `header stacks buttons then template name then table structure`() {
        val engine = src(js)
        val bar = engine.substringAfter("if (isApp()) {\n      bar.innerHTML = '';").substringBefore("barPad();")
        assertTrue("ردیف دکمه‌ها ساخته شود", "row.className = 'rc-bar-row'" in bar)
        assertTrue("نام قالب زیر دکمه‌ها", "t.className = 'rc-bar-title'" in bar)
        assertTrue("جایگاه ساختار جدول زیر نام", "slot.className = 'rc-bar-struct'" in bar)
        assertTrue(
            "ترتیب الحاق باید دکمه‌ها → نام → ساختار باشد",
            bar.indexOf("bar.appendChild(row)") < bar.indexOf("bar.appendChild(t)") &&
                bar.indexOf("bar.appendChild(t)") < bar.indexOf("bar.appendChild(slot)")
        )
        // V340 — دفترچهٔ مدرسه هم جدول دارد، پس انتخاب ساختار برای «کلاس» و «مدرسه»
        // نشان داده می‌شود و برای کارنامهٔ انفرادی همچنان پنهان می‌ماند.
        assertTrue(
            "ساختار جدول برای کلاس و مدرسه، نه انفرادی",
            "(state.payload.scope === 'class' || state.payload.scope === 'school') ? '<span class=\"rc-lbl\">" in engine
        )
        assertFalse("برای کارنامهٔ انفرادی ساختار نشان داده نشود", "state.payload.scope === 'student' ? '<span class=\"rc-lbl\">" in engine)
        assertTrue("برچسب ساختار یک‌بار نوشته شود", "var structInner = '' + [" in engine)
    }

    @Test
    fun `three preview buttons stay on one row inside the screen`() {
        val engine = src(js)
        assertTrue("دکمهٔ تغییر قالب", "{ label: 'تغییر قالب', kind: 'ghost', act: showPicker }" in engine)
        assertTrue("دکمهٔ چاپ در اپ کوتاه شود", "label: isApp() ? '🖨 چاپ' : '🖨 چاپ / PDF'" in engine)
        assertTrue("دکمهٔ بستن", "{ label: 'بستن', kind: 'ghost', act: closeHost }" in engine)
        val style = src(css)
        assertTrue("ردیف دکمه‌ها فلکس و بدون شکستن خط", "body.rc-app .rc-bar-row{display:flex;gap:6px;align-items:stretch;flex-wrap:nowrap}" in style)
        assertTrue("هر دکمه سهم مساوی و قابل کوچک‌شدن", "body.rc-app .rc-bar-row .rc-btn{flex:1 1 0;min-width:0" in style)
        assertTrue("هدر هم‌عرض صفحه و بدون سرریز", "body.rc-app .rc-bar{position:fixed;top:0;left:0;right:0;width:100%;display:block" in style)
        assertTrue("خود هدر هرگز اسکرول افقی نگیرد", "padding:8px 8px 9px;overflow:hidden;flex-wrap:nowrap}" in style)
    }

    @Test
    fun `sheet is fitted to the phone width and only the sheet area scrolls`() {
        val style = src(css)
        val engine = src(js)
        assertTrue("بدنهٔ اپ اسکرول افقی نداشته باشد", "body.rc-app{overflow:hidden}" in style)
        assertTrue("ناحیهٔ برگه زیر هدر و قابل اسکرول", "body.rc-app #rc-root{position:fixed;top:var(--rc-bar-h,104px);left:0;right:0;bottom:0;overflow:auto" in style)
        assertTrue("برگه با کوچک‌نمایی چیدمان هم‌عرض شود", "body.rc-app .rc-scale{position:absolute;top:0;left:0;width:794px;transform-origin:top left}" in style)
        assertTrue("اندازهٔ واقعی برگهٔ زوم‌شده برای اسکرول", "sizer.style.width = Math.ceil(PAGE_W * k) + 'px'" in engine)
        assertTrue("ضریب تناسب از عرض صفحه", "(root.clientWidth - 16) / PAGE_W" in engine)
        assertTrue("ارتفاع هدر به CSS داده شود", "style.setProperty('--rc-bar-h'" in engine)
        assertTrue("سرریز A4 با ارتفاع چیدمان سنجیده شود، نه با زوم", "max = Math.max(max, p.offsetHeight)" in engine)
        assertFalse("سنجهٔ سرریز نباید زیر اثر زوم باشد", "p.getBoundingClientRect().height); });\n      if (max > 1124" in engine)
    }

    @Test
    fun `pinch zoom changes only the sheet and never the header`() {
        val engine = src(js)
        assertTrue("زوم دو‌انگشتی روی ناحیهٔ برگه", "root.addEventListener('touchmove'" in engine)
        assertTrue("فقط با دو انگشت", "e.touches.length !== 2" in engine)
        assertTrue("زوم از نسبت فاصلهٔ دو انگشت", "state.zoom = z0 * (dist(e.touches) / d0)" in engine)
        assertTrue("در پیش‌نمایش فعال شود", "state.step !== 'preview'" in engine)
        assertTrue("کف زوم، تناسب با عرض صفحه", "state.zoom = Math.min(3, Math.max(fit, state.zoom))" in engine)
        val style = src(css)
        val barRule = style.substringAfter("body.rc-app .rc-bar{").substringBefore("}")
        assertFalse("هدر نباید transform بگیرد", "transform" in barRule)
        assertFalse("هدر نباید زوم شود", "scale(" in barRule)
        assertTrue("چرخش/تغییر اندازه، تناسب را نو کند", "window.addEventListener('resize', function () { barPad(); fitStage(true); })" in engine)
    }

    @Test
    fun `print drops the zoom and keeps real A4`() {
        val style = src(css)
        val print = style.substringAfter("/* چاپ باید بدون زوم و روی A4 واقعی باشد */")
        assertTrue("برگه در چاپ به اندازهٔ واقعی برگردد", "body.rc-app .rc-scale{position:static!important;transform:none!important;width:auto!important}" in print)
        assertTrue("ناحیهٔ برگه در چاپ ثابت نباشد", "body.rc-app #rc-root{position:static!important" in print)
        assertTrue("جعبهٔ اندازه در چاپ آزاد شود", "body.rc-app .rc-sizer{position:static!important;width:auto!important;height:auto!important" in print)
        assertTrue("نگهبان قدیمی چاپ دست‌نخورده بماند", ".rc-bar,.rc-picker,.rc-map" in style)
        assertTrue("اندازهٔ کاغذ A4 بماند", "@page{size:A4;margin:0}" in style)
    }

    @Test
    fun `android host requests the app skin and blocks system text zoom`() {
        val kt = src(dialog)
        assertTrue("میزبان اندروید پوستهٔ اپ را درخواست کند", "window.ReportCard.open(\$json,{skin:'app'})" in kt)
        assertTrue("بزرگ‌نمایی متنی سیستم خنثی شود", "settings.textZoom = 100" in kt)
        assertTrue("زوم خود WebView خاموش بماند تا هدر بزرگ نشود", "settings.setSupportZoom(false)" in kt)
        assertTrue("کنترل‌های زوم WebView خاموش", "settings.builtInZoomControls = false" in kt)
        assertTrue("چاپ روی A4 رنگی بدون حاشیه", "PrintAttributes.MediaSize.ISO_A4" in kt)
    }

    @Test
    fun `app version is bumped for this release`() {
        // پین «کف نسخه» است، نه شمارهٔ دقیق؛ انتشار بعدی این تست را نمی‌شکند.
        val version = src("text/APP_VERSION.txt").trim()
        assertTrue("قالب شماره نسخه", version.matches(Regex("""\d+\.\d{2}\.\d{2}""")))
        val a = version.split(".").map { it.toInt() }
        val b = listOf(1, 3, 58)
        var ok = true
        for (i in b.indices) {
            if (a[i] > b[i]) break
            if (a[i] < b[i]) { ok = false; break }
        }
        assertTrue("نسخهٔ اپ نباید از 1.03.58 عقب‌تر باشد", ok)
        assertTrue(
            "بلوک V339.2 باید در CHANGELOG بماند",
            src("text/CHANGELOG_FA.txt").lineSequence().any { it.startsWith("V339.2") && "1.03.58" in it }
        )
    }
}
