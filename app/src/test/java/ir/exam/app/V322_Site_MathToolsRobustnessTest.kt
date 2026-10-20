package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V322.0 (سایت) — همان مهارهای پایداری ابزارهای ریاضی در موتور وب (graph_fig.js) تا نسخهٔ سایت و
 * اپ دقیقاً یک رفتار داشته باشند (چرخندهٔ تک‌بخشی، d = f در عدسی، دادهٔ منفی، شمارهٔ نت نامعتبر،
 * سقف قیمت بزرگ، مؤلفهٔ ۹۹۹ بردار، جابه‌جایی بزرگ، مختصات بیرون از بازهٔ محور، ۱۰ میلهٔ کوئیزنر و …).
 */
class V322_Site_MathToolsRobustnessTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun js(): String = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()

    /** موتور وب هم همان مهارها را دارد (هم‌ترازی اپ و سایت). */
    @Test
    fun siteEngineCarriesTheSameGuards() {
        val js = js()
        assertTrue("web label fitting missing", js.contains("function fitTxt(x, s, an, sz)"))
        assertTrue("web barchart clamp missing", js.contains("bcData = bcData.map(function (vbc) { return clamp(vbc, 0, 999); });"))
        assertTrue("web sort cap missing", js.contains("var soh = 26 + Math.abs(soVals[soI]) / soMaxAbs * 110,"))
        assertTrue("web staff clamp missing", js.contains("stfNotes = stfNotes.map(function (nst) { return clamp(nst, 53, 74); });"))
        assertTrue("web supply clamp missing", js.contains("var cy4 = clamp(spY0 - 40 - spCeil * 12, spY1 + 40, spY0 - 30);"))
        assertTrue("web caliper clamp missing", js.contains("cpVal = Math.min(cpMax, Math.max(0, num(X.val, 23.4)))"))
        assertTrue("web vectors clamp missing", js.contains("var vAx = clamp(vox + v1x * vsc, 56, 318)"))
        assertTrue("web transform clamp missing", js.contains("var tfDx = clamp(num(X.dx, 3), -4, 4)"))
        assertTrue("web linear clip missing", js.contains("var lpcx = function (v3) { return clamp(lpx(v3), 46, 326); }"))
        assertTrue("web axis auto-range missing", js.contains("[num(X.x1, 0), num(X.x2, 3)].forEach(function (vxv)"))
        assertTrue("web 3d clamp missing", js.contains("var axMax3 = n3 * st3;"))
        assertTrue("web rods cap removed", js.contains(".filter(function (qrd) { return !isNaN(qrd); }).slice(0, 10)"))
        assertTrue("web optics infinity guard missing", js.contains("var opAtFocus = opKind !== 'concave' && Math.abs(opD - opF) < 0.05;"))
        assertTrue("web integral clamp missing", js.contains("inA = clamp(num(X.lo, 0), 0, 20), inB = clamp(num(X.hi, 2), 0, 20)"))
        assertTrue("web normal clamp missing", js.contains("nrLo = clamp(nrLo, nrMin, nrMax); nrHi = clamp(nrHi, nrMin, nrMax);"))
    }
}
