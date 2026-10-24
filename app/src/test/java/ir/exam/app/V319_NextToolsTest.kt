package ir.exam.app

import ir.exam.app.core.figure.AXIS_FIGURES
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.NextMathSvgRenderer
import ir.exam.app.core.figure.PrimaryMathSvgRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V319.1 — بستهٔ سوم ابزار آموزشی (۳۰ ابزار): دبستان (تبدیل واحد، محیط/مساحت، نمودار تصویری،
 * گرد کردن، تقسیم با باقی‌مانده، دسته‌بندی اشکال، اجزای دایره، استوانه/مخروط، تناسب، دستگاه خطی)،
 * متوسطه و علوم (نمودار آماری، سهمی، زاویهٔ مرکزی/محاطی، آونگ، سطح شیب‌دار، مدار، عدسی، منحنی گرمایش،
 * DNA، سلول) و انسانی/دانشگاهی (خط زمان، نقشه، جمله، بردار، ماتریس، انتگرال، فلوچارت، پشته/صف،
 * مرتب‌سازی، رگرسیون).
 */
class V319_NextToolsTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private val ids = listOf(
        "axunitconv", "axperimeter", "axrounding",
        "axcircleparts", "axcylinder", "axproportion", "axlinear", "axbarchart", "axparabola",
        "axinthales", "axpendulum", "axincline", "axcircuit", "axoptics", "axheatcurve",
        "axdna", "axtimeline", "axmapgeo", "axsentence", "axvectors", "axmatrix",
        "axintegral", "axflowchart", "axstack", "axsort", "axregression"
    )
    private val legacyRemovedIds = listOf("axpictograph", "axdivision", "axshapeid", "axcell")

    @Test
    fun allNextToolsExistInGalleryAndRenderers() {
        val gallery = AXIS_FIGURES.map { it.id }.toSet()
        for (k in ids) {
            assertTrue("Missing $k in AXIS_FIGURES", k in gallery)
            assertTrue("Missing $k in PrimaryMathSvgRenderer.SUPPORTED", k in PrimaryMathSvgRenderer.SUPPORTED)
            assertTrue("Missing $k in NextMathSvgRenderer.SUPPORTED", k in NextMathSvgRenderer.SUPPORTED)
        }
        for (k in legacyRemovedIds) {
            assertTrue("Legacy $k must remain in PrimaryMathSvgRenderer.SUPPORTED", k in PrimaryMathSvgRenderer.SUPPORTED)
            assertTrue("Legacy $k must remain in NextMathSvgRenderer.SUPPORTED", k in NextMathSvgRenderer.SUPPORTED)
        }
    }

    @Test
    fun allNextToolsRenderNonEmptySvg() {
        for (k in ids) {
            val tpl = AXIS_FIGURES.first { it.id == k }
            val body = PrimaryMathSvgRenderer.body(tpl.toSpec())
            assertTrue("Empty SVG body for $k", body.isNotBlank())
            assertTrue("SVG without tags for $k", body.contains("<"))
            assertTrue("SVG contains NaN for $k", !body.contains("NaN"))
            assertTrue("SVG contains undefined for $k", !body.contains("undefined"))
        }
    }

    @Test
    fun galleryHoldsEveryFigureId() {
        assertEquals(AXIS_FIGURES.size, AXIS_FIGURES.map { it.id }.toSet().size)
        assertTrue("gallery too small", AXIS_FIGURES.size >= 124)
    }

    /** رگرسیون: چند ابزار دارای محاسبهٔ عددی باید مقدار درست را در متن خروجی داشته باشند. */
    @Test
    fun computedValuesAppearInSvg() {
        fun render(json: String) = PrimaryMathSvgRenderer.body(FigureSpec.parse(json)!!)
        // تقسیم: ۱۷ ÷ ۵ = ۳ و باقی‌ماندهٔ ۲ (ارقام لاتین در تست‌ها؛ کلید «اعداد فارسی» خاموش است)
        val dv = render("""{"t":"axdivision","X":{"a":"17","b":"5"}}""")
        assertTrue("division quotient", dv.contains("17 ÷ 5 = 3"))
        assertTrue("division remainder", dv.contains("باقی‌ماندهٔ 2"))
        // ماتریس: ۱۹ در درایهٔ (۱،۱)
        assertTrue("matrix result", render("""{"t":"axmatrix","X":{"a":"1,2;3,4","b":"5,6;7,8"}}""").contains("19"))
        // مدار سری: R = ۱۰ Ω
        assertTrue("circuit req", render("""{"t":"axcircuit","X":{"mode":"series","r1":"4","r2":"6","v":"12"}}""").contains("10"))
        // گرد کردن: ۱۴۷ ≈ ۱۵۰
        assertTrue("rounding", render("""{"t":"axrounding","X":{"n":"147","mode":"ten"}}""").contains("150"))
    }

    @Test
    fun pickerAndTouchHandlersCoverEveryNextTool() {
        val picker = File(root(), "app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").readText()
        val adj = File(root(), "app/src/main/java/ir/exam/app/ui/figure/AdjustableFigure.kt").readText()
        val gallery = File(root(), "app/src/main/java/ir/exam/app/core/figure/FigureGallery.kt").readText()
        for (k in ids) {
            assertTrue("Missing $k label in FigurePickerDialog", picker.contains("\"$k\" -> listOf("))
            assertTrue("Missing $k in AdjustableFigure", adj.contains("\"$k\""))
            assertTrue("Missing $k template in FigureGallery", gallery.contains("FigureTemplate(\"$k\""))
        }
    }
}
