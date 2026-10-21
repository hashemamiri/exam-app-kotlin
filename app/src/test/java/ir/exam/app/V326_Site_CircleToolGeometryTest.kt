package ir.exam.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V326 — هندسهٔ درستِ ابزارهای دایره (وتر، مماس، کمان، قطاع…) و برچسب‌ها.
 *
 * گزارش کاربر: «اجزای دایره وتر به درستی روی دایره قرار نمی‌گیرد و کمان روی دایره نوشته می‌شود».
 * ریشه: در `graph_fig.js` (و آینهٔ بومی‌اش `NextMathSvgRenderer.kt`) وتر/مماس/کمان با اعداد
 * دستی کشیده می‌شدند و سرشان ۵ تا ۷ پیکسل از محیط فاصله داشت؛ برچسب‌ها هم روی محیط می‌نشستند.
 *
 * این تست سه چیز را تضمین می‌کند:
 *   ۱) اجزای دایره از روی همان شعاع محاسبه می‌شوند (توابع circPt / circLabel / arcOnCircle)،
 *   ۲) اعداد جادوییِ قبلی (کمان و وترِ دستی) دیگر برنگردند،
 *   ۳) گزارش بازرسی چشمی با مرورگر واقعی (`site/figure_audit.json`) صفر ایراد داشته باشد.
 */
class V326_Site_CircleToolGeometryTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun circleLabelsAndPartsUseOneRadius() {
        val g = src("app/src/main/assets/print/web/graph_fig.js")
        assertTrue("تابع نقطه روی محیط نیست", "function circPt(cx, cy, r, deg)" in g)
        assertTrue("برچسب‌گذاری بیرونِ دایره نیست", "function circLabel(cx, cy, r, deg, t2, c, gap)" in g)
        assertTrue("کمانِ روی محیط نیست", "function arcOnCircle(cx, cy, r, d0, d1, c, w)" in g)
        assertTrue("اجزای دایره بازنویسی نشده", "if (t === 'axcircleparts') {" in g && "circPt(cpx, cpy, cpr, 32)" in g)
        // وتر باید دو سرش روی محیط باشد (نه عدد دستی)
        assertFalse("وتر هنوز مختصات دستی دارد", "lnr(cpc.x - 66, cpc.y + 46, cpc.x + 30, cpc.y + 74" in g)
        assertFalse("مماس هنوز با عدد دستی کشیده می‌شود", "lnr(cpc.x - 96, cpc.y + cpc.r, cpc.x + 96" in g)
        // برچسب‌ها باید با هالهٔ سفید خوانا بمانند (روی خط/محیط)
        assertTrue("هالهٔ سفید پشت برچسب‌ها نیست", "paint-order:stroke;stroke:#fff" in g)
        assertTrue("هالهٔ سفید در شکل‌های هندسی نیست", "paint-order:stroke;stroke:#fff" in
            src("app/src/main/assets/print/web/geo_fig.js"))
    }

    @Test
    fun nativeRendererMatchesTheWebTool() {
        val k = src("app/src/main/java/ir/exam/app/core/figure/NextMathSvgRenderer.kt")
        val block = k.substring(k.indexOf("private fun circleParts"), k.indexOf("private fun cylinderCone"))
        assertTrue("نقطه روی محیط در نسخهٔ بومی نیست", "fun pt(deg: Double)" in block)
        assertTrue("برچسب بیرونِ دایره در نسخهٔ بومی نیست", "fun outLabel(deg: Double" in block)
        assertTrue("وتر بومی از شعاع محاسبه نمی‌شود", "val c1 = pt(32.0); val c2b = pt(148.0)" in block)
        assertTrue("مماس بومی روی محیط نمی‌نشیند", "ln(cx - 96f, cy + r, cx + 96f, cy + r" in block)
        assertFalse("مختصات دستی وتر در نسخهٔ بومی مانده", "ln(cx - 66f, cy + 46f" in block)
        assertFalse("کمان بومی با عدد ثابت نوشته می‌شود", block.contains("A1200,1200") )
    }

    @Test
    fun browserAuditReportIsClean() {
        val json = src("site/figure_audit.json")
        assertTrue("گزارش بازرسی نیست — node scripts/audit_figures.js را اجرا کنید", "\"tools\"" in json)
        assertTrue("بازرسی ابزارها کمتر از حد انتظار است", Regex("\"tools\":\\s*(\\d+)").find(json)!!
            .groupValues[1].toInt() >= 95)
        val issues = Regex("\"issues\":\\s*(\\d+)").find(json)!!.groupValues[1].toInt()
        assertEquals("ابزارهایی هستند که خط/برچسبشان روی دایره درست نمی‌نشیند", 0, issues)
    }

    @Test
    fun auditScriptCoversEveryTool() {
        val a = src("scripts/audit_figures.js")
        assertTrue("بازرس شکل‌ها به فهرست ابزارهای هندسی وصل نیست", "'chord', 'tang'" in a)
        assertTrue("بازرس ابزارهای نموداری را نمی‌بیند", "'axcircleparts'" in a && "'axinthales'" in a)
        assertTrue("قاعدهٔ «سرِ خط باید روی محیط باشد» نیست", "فاصله از مرکز" in a)
    }
}
