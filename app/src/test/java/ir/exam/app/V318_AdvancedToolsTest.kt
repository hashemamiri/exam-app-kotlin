package ir.exam.app

import ir.exam.app.core.figure.AdvancedMathSvgRenderer
import ir.exam.app.core.figure.AXIS_FIGURES
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.PrimaryMathSvgRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V318.0 — بستهٔ جامع ۴۰ ابزار آموزشی تعاملی (دبستان تا دانشگاه) در اندروید:
 * کوئیزنر، هرم اعداد، چرخ ضرب، جدول ضرب پازل، گستردهٔ احجام، شمارش مکعب، تقویم، اهرم/قرقره،
 * چرخه‌های علوم، قطار کلمه‌سازی، کاشی‌های جبر، کن‌کن، گستردهٔ تاس، تبدیلات هندسی، کولیس،
 * جدول تعیین علامت، دایرهٔ مثلثاتی، تناظر تابع، درخت احتمال، مقاطع مخروطی، آرایش الکترونی، مربع پانت،
 * نوار قلب، تقطیع عروضی، جدول ارزش، سند T، خطوط حامل، کارنو، گیت منطقی، DFA، BST، بود، تیر،
 * دایرهٔ مور، چرخهٔ ترمودینامیکی، CPM/PERT، بنزن، منحنی نرمال، عرضه و تقاضا، ماتریس بازی.
 */
class V318_AdvancedToolsTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private val advancedIds = listOf(
            "axrod", "axtower", "axmulwheel", "axmultgrid", "axnet",
            "axcubes", "axcalendar", "axlever", "axcycle", "axwordtrain",
            "axtiles", "axkenken", "axdicefold", "axtransform", "axcaliper",
            "axsigntable", "axtrigcircle", "axmapping", "axprobtree", "axconic",
            "axorbital", "axpunnett", "axecg", "axprosody", "axtruthtable",
            "axledger", "axstaff", "axkmap", "axgate", "axdfa",
            "axbst", "axbode", "axbeam", "axmohr", "axpv",
            "axpert", "axbenzene", "axnormal", "axsupply", "axgame"
    )

    @Test
    fun allAdvancedToolsExistInGalleryAndRenderers() {
        val gallery = AXIS_FIGURES.map { it.id }.toSet()
        for (k in advancedIds) {
            assertTrue("Missing $k in AXIS_FIGURES", k in gallery)
            assertTrue("Missing $k in PrimaryMathSvgRenderer.SUPPORTED", k in PrimaryMathSvgRenderer.SUPPORTED)
            assertTrue("Missing $k in AdvancedMathSvgRenderer.SUPPORTED", k in AdvancedMathSvgRenderer.SUPPORTED)
        }
    }

    @Test
    fun allAdvancedToolsRenderNonEmptySvg() {
        for (k in advancedIds) {
            val tpl = AXIS_FIGURES.first { it.id == k }
            val body = PrimaryMathSvgRenderer.body(tpl.toSpec())
            assertTrue("Empty SVG body for $k", body.isNotBlank())
            assertTrue("SVG without tags for $k", body.contains("<"))
            assertTrue("SVG contains NaN for $k", !body.contains("NaN"))
        }
    }

    /** V318.1 — رگرسیون: جدول کارنو در هر سه حالت ۲/۳/۴ متغیره بدون خطا و با تعداد خانهٔ درست رندر شود. */
    @Test
    fun karnaughMapRendersForTwoThreeAndFourVariables() {
        val expected = mapOf(2 to 4, 3 to 8, 4 to 16)
        for ((vars, cells) in expected) {
            val spec = FigureSpec.parse("""{"t":"axkmap","X":{"vars":"$vars","ones":"0,1,3,5"}}""")
            assertTrue("spec parse failed for vars=$vars", spec != null)
            val body = PrimaryMathSvgRenderer.body(spec!!)
            assertTrue("Empty K-map body for vars=$vars", body.isNotBlank())
            assertTrue("NaN in K-map vars=$vars", !body.contains("NaN"))
            val drawn = body.split(">۱</text>").size - 1 + body.split(">۰</text>").size - 1
            assertEquals("K-map cell count for vars=$vars", cells, drawn)
        }
    }

    @Test
    fun pickerAndTouchHandlersCoverEveryAdvancedTool() {
        val picker = File(root(), "app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").readText()
        val adj = File(root(), "app/src/main/java/ir/exam/app/ui/figure/AdjustableFigure.kt").readText()
        for (k in advancedIds) {
            assertTrue("Missing $k label in FigurePickerDialog", picker.contains("\"$k\" -> listOf("))
            assertTrue("Missing $k in AdjustableFigure", adj.contains("\"$k\""))
        }
        assertTrue(adj.contains("ADV_CYCLE_PARAMS"))
        assertTrue(adj.contains("ADV_NUM_PARAMS"))
    }
}
