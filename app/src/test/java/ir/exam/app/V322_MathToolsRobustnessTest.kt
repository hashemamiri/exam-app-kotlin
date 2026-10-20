package ir.exam.app

import ir.exam.app.core.figure.AXIS_FIGURES
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V322.0 — پایداری ابزارهای ریاضی در برابر ورودی‌های مرزی.
 *
 * ممیزی همهٔ ۱۲۴ قالب ریاضی نشان داد چند ابزار با مقادیر مرزی (تک‌بخشی‌شدن چرخندهٔ احتمال،
 * d = f در عدسی، دادهٔ منفی در نمودار میله‌ای، شمارهٔ نت نامعتبر، سقف قیمت بزرگ، مؤلفهٔ ۹۹۹ در
 * بردارها، مختصات بیرون از بازهٔ محور و …) یا کرش می‌کردند یا شکلی با مختصات ده‌ها هزار بیرون از
 * بوم می‌ساختند. این تست همهٔ آن حالت‌ها را می‌رندر می‌کند و مطمئن می‌شود خروجی سالم و در بوم بماند.
 */
class V322_MathToolsRobustnessTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun source(path: String): String = File(root(), path).readText()

    /** بزرگ‌ترین مقدار عددیِ مختصات در بدنهٔ SVG (برای تشخیص شکلِ بیرون از بوم). */
    private fun maxCoord(body: String): Float {
        val nums = Regex("\\s(?:x|y|cx|cy|r|rx|ry|width|height|x1|y1|x2|y2)=\"(-?[0-9.]+)\"")
            .findAll(body).mapNotNull { it.groupValues[1].toFloatOrNull() }
        return nums.maxOrNull() ?: 0f
    }

    private fun assertClean(tool: String, json: String) {
        val spec = FigureSpec.parse(json)
        assertTrue("spec parse failed for $tool", spec != null)
        val body = FigureSvgRenderer.render(spec!!).xml
        assertTrue("Empty SVG body for $tool ($json)", body.isNotBlank())
        assertTrue("NaN in $tool", !body.contains("NaN"))
        assertTrue("Infinity in $tool", !body.contains("Infinity"))
        assertTrue("undefined in $tool", !body.contains("undefined"))
        assertTrue("null in $tool", !body.contains("null"))
        assertTrue("negative size in $tool", !Regex("(?:width|height|r)=\"-[0-9]").containsMatchIn(body))
        assertTrue("coordinate outside canvas in $tool (max=${maxCoord(body)})", maxCoord(body) < 1500f)
    }

    /** هر ۱۲۴ قالب با ورودی خالی/پیش‌فرض بدون استثنا و بدون NaN رندر می‌شود. */
    @Test
    fun everyMathToolRendersCleanlyWithDefaultsAndEmptyInput() {
        assertTrue("tool gallery is too small", AXIS_FIGURES.size >= 124)
        for (tpl in AXIS_FIGURES) {
            assertClean(tpl.id, tpl.specJson)
            assertClean(tpl.id, "{\"t\":\"${tpl.id}\"}")
        }
    }

    /** رگرسیون باگ‌های V322 — هر مورد قبلاً کرش یا مختصات بیرون از بوم می‌ساخت. */
    @Test
    fun boundaryInputsRenderInsideTheCanvas() {
        val cases = mapOf(
            "axspin" to "{\"t\":\"axspin\",\"X\":{\"parts\":\"قرمز\"}}",
            "axspin-single-num" to "{\"t\":\"axspin\",\"X\":{\"parts\":\"3\"}}",
            "axoptics-focus" to "{\"t\":\"axoptics\",\"X\":{\"f\":\"10\",\"d\":\"10\"}}",
            "axoptics-far" to "{\"t\":\"axoptics\",\"X\":{\"f\":\"999\",\"d\":\"999\"}}",
            "axbarchart-negative" to "{\"t\":\"axbarchart\",\"X\":{\"data\":\"-5\"}}",
            "axsort-huge" to "{\"t\":\"axsort\",\"X\":{\"vals\":\"999\"}}",
            "axstaff-bad-note" to "{\"t\":\"axstaff\",\"X\":{\"notes\":\"999\"}}",
            "axsupply-ceiling" to "{\"t\":\"axsupply\",\"X\":{\"ceiling\":\"999\"}}",
            "axcaliper-over" to "{\"t\":\"axcaliper\",\"X\":{\"val\":\"999\",\"max\":\"50\"}}",
            "axvectors-huge" to "{\"t\":\"axvectors\",\"X\":{\"ax1\":\"999\",\"by\":\"-999\"}}",
            "axtransform-huge" to "{\"t\":\"axtransform\",\"X\":{\"dx\":\"999\",\"dy\":\"-999\"}}",
            "axlinear-huge" to "{\"t\":\"axlinear\",\"X\":{\"m1\":\"999\",\"b1\":\"999\"}}",
            "axxyvec-out" to "{\"t\":\"axxyvec\",\"X\":{\"x1\":\"999\",\"y2\":\"-999\"}}",
            "axxycirc-out" to "{\"t\":\"axxycirc\",\"X\":{\"cx\":\"999\",\"r\":\"999\"}}",
            "ax3dpt-out" to "{\"t\":\"ax3dpt\",\"X\":{\"x1\":\"999\",\"z1\":\"-999\"}}",
            "axintegral-negative" to "{\"t\":\"axintegral\",\"X\":{\"lo\":\"-5\",\"hi\":\"4\"}}",
            "axnormal-sd0" to "{\"t\":\"axnormal\",\"X\":{\"sd\":\"0\",\"lo\":\"-9\",\"hi\":\"9\"}}",
            "axparabola-huge-b" to "{\"t\":\"axparabola\",\"X\":{\"b\":\"999\"}}"
        )
        for ((name, json) in cases) assertClean(name, json)
    }

    /** چرخندهٔ احتمال با یک بخش نباید استثنا بدهد (باگ اصلی V322). */
    @Test
    fun spinnerWithSingleSegmentDoesNotCrash() {
        val spec = FigureSpec.parse("{\"t\":\"axspin\",\"X\":{\"parts\":\"قرمز\"}}")
        assertTrue(spec != null)
        val body = FigureSvgRenderer.render(spec!!).xml
        assertTrue("spinner drew nothing", body.contains("<path"))
        assertTrue("spinner lost its default sectors", body.split("<path").size - 1 >= 4)
    }

    /** نگهبان: تیترهای بالاترین بلوک لیست تغییرات نباید شمارهٔ نسخهٔ داخلی داشته باشند (پنجرهٔ بروزرسانی). */
    @Test
    fun changelogTopBlockHasNoInternalVersionNumbers() {
        val lines = source("text/CHANGELOG_FA.txt").lines()
        assertTrue("changelog is empty", lines.isNotEmpty())
        assertTrue("changelog must start with a version header", lines.first().trim().startsWith("V"))
        val top = ir.exam.app.core.update.ReleaseNotes.parse(lines.take(60)).first()
        assertTrue("top block is not app-affecting", top.appAffecting)
        assertTrue("top block has no bullets", top.bullets.isNotEmpty())
        val offenders = top.bullets.filter { Regex("""\bV\d""").containsMatchIn(it) }
        assertTrue("bullets contain internal version numbers: $offenders", offenders.isEmpty())
        assertTrue("bullet too long", top.bullets.all { it.length <= 200 })
    }

    /** همان مهارها در موتور بومی (Kotlin) — نگهبان رگرسیون. */
    @Test
    fun nativeEngineCarriesTheSameGuards() {
        val primary = source("app/src/main/java/ir/exam/app/core/figure/PrimaryMathSvgRenderer.kt")
        assertTrue("spinner guard missing", primary.contains("if (parts.size < 2) parts = listOf(\"قرمز\", \"آبی\", \"سبز\", \"زرد\")"))
        assertTrue("spinner n guard missing", primary.contains("val n = parts.size"))

        val next = source("app/src/main/java/ir/exam/app/core/figure/NextMathSvgRenderer.kt")
        assertTrue("barchart clamp missing", next.contains("data = data.map { it.coerceIn(0, 999) }"))
        assertTrue("sort cap missing", next.contains("val h = 26f + abs(v) / maxAbs.toFloat() * 110f"))
        assertTrue("linear clip missing", next.contains("fun pyc(v: Float) = py(v).coerceIn(44f, 254f)"))
        assertTrue("vectors clamp missing", next.contains("val axx = (ox + ax1 * sc).coerceIn(56f, 318f)"))
        assertTrue("optics guard missing", next.contains("val atFocus = !concave && abs(d - f) < 0.05f"))
        assertTrue("optics infinity caption missing", next.contains("val diText = if (atFocus) \"بی‌نهایت\""))
        assertTrue("integral clamp missing", next.contains("var lo = spec.xNum(\"lo\", 0f).coerceIn(0f, 20f)"))
        assertTrue("parabola axis guard missing", next.contains("if (abs(vx) <= 6.2f) sb.append(ln(ox + vx * sc, 258f"))
        assertTrue("label fitting helper missing", next.contains("private fun fitLabel(x: Float, s: String, anchor: String, size: Int): Triple<Float, String, Int>"))

        val adv = source("app/src/main/java/ir/exam/app/core/figure/AdvancedMathSvgRenderer.kt")
        assertTrue("caliper clamp missing", adv.contains("val v = max(0f, spec.xNum(\"val\", 23.4f)).coerceAtMost(mx)"))
        assertTrue("staff clamp missing", adv.contains(".map { it.coerceIn(53, 74) }"))
        assertTrue("supply clamp missing", adv.contains("val cy = (y0 - 40f - ceiling * 12f).coerceIn(y1 + 40f, y0 - 30f)"))
        assertTrue("transform clamp missing", adv.contains("val dx = spec.xNum(\"dx\", 3f).coerceIn(-4f, 4f)"))
        assertTrue("benzene carbon labels missing", adv.contains("if (mode != \"circle\") pts.forEach { p ->"))
        assertTrue("normal clamp missing", adv.contains("val loC = lo.coerceIn(vmin, vmax)"))
        assertTrue("advanced label fitting missing", adv.contains("private fun fitLabel(x: Float, s: String, anchor: String, size: Int): Triple<Float, String, Int>"))

        val axis = source("app/src/main/java/ir/exam/app/core/figure/AxisSvgRenderer.kt")
        // پین V137.5: تبدیل ارقام باید در همان تابع text محورها بماند (جاگیری برچسب نباید آن را جابه‌جا کند).
        assertTrue("axis digit conversion lost", "FigureDigits.apply(s)" in axis)
        assertTrue("axis auto-range missing", axis.contains("if (type == \"axxyvec\") {"))
        assertTrue("axis 3d clamp missing", axis.contains("val axisMax = n * step"))
        assertTrue("axis label fitting missing", axis.contains("val xx = if (lo <= hi) x.coerceIn(lo, hi) else x.coerceIn(3f, 357f)"))
    }
}
