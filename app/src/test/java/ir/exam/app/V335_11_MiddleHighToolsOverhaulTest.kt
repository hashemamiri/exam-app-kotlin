package ir.exam.app

import ir.exam.app.core.figure.AXIS_FIGURES
import ir.exam.app.core.figure.AdvancedMathSvgRenderer
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer
import ir.exam.app.core.figure.MATH_CATEGORIES
import ir.exam.app.core.figure.MATH_TOOL_IDS
import ir.exam.app.core.figure.NextMathSvgRenderer
import ir.exam.app.core.figure.PrimaryInteractiveFigureHelper
import ir.exam.app.core.figure.PrimaryMathSvgRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V335.11 — بازطراحی، رفع باگ و پیشرفته‌سازی ۲۲ ابزار تعاملی متوسطهٔ اول و دوم + پالایش کاتالوگ (۱۲۴ قالب / ۹۷ ابزار).
 */
class V335_11_MiddleHighToolsOverhaulTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun read(path: String): String = File(root(), path).readText()

    @Test
    fun items5_and_12_removedFourToolsFromCatalogsWhilePreservingLegacyRenderers() {
        val removedMiddle = listOf("axpictograph", "axdivision", "axshapeid")
        val removedHigh = listOf("axcell")
        val allRemoved = removedMiddle + removedHigh
        val galleryIds = AXIS_FIGURES.map { it.id }.toSet()

        for (id in allRemoved) {
            assertFalse("$id should be removed from AXIS_FIGURES", id in galleryIds)
            assertFalse("$id should be removed from MATH_TOOL_IDS", id in MATH_TOOL_IDS)
            assertTrue("$id legacy SVG branch must stay in NextMathSvgRenderer", id in NextMathSvgRenderer.SUPPORTED)
            assertTrue("$id legacy SVG branch must stay in PrimaryMathSvgRenderer", id in PrimaryMathSvgRenderer.SUPPORTED)
        }
        assertEquals(12, MATH_CATEGORIES.first { it.id == "middle" }.ids.size)
        assertEquals(21, MATH_CATEGORIES.first { it.id == "high" }.ids.size)
        assertEquals(97, MATH_TOOL_IDS.size)
        assertEquals(124, AXIS_FIGURES.size)

        val graphFig = read("app/src/main/assets/print/web/graph_fig.js")
        val axisTypesBlock = graphFig.substringAfter("var AXIS_TYPES = [").substringBefore("];")
        for (id in allRemoved) {
            assertFalse("$id should be removed from web AXIS_TYPES", "'$id'" in axisTypesBlock)
            assertTrue("$id legacy web renderer must remain", "if (t === '$id')" in graphFig)
        }
    }

    @Test
    fun item1_algebraTilesCapacityAndTopModelBottomExpression() {
        val spec = FigureSpec.parse("""{"t":"axtiles","X":{"x2":"9","x":"16","k":"24","neg":"0","label":"1"}}""")!!
        val svg = FigureSvgRenderer.render(spec).xml
        assertFalse(svg.contains("NaN"))
        assertTrue("Top tile model legend missing", svg.contains("مدل کاشی"))
        assertTrue("Bottom algebraic expression missing", svg.contains("عبارت جبری"))
        assertTrue("9x² missing", svg.contains("9x²") || svg.contains("۹x²"))
        assertTrue("16x missing", svg.contains("16x") || svg.contains("۱۶x"))
        assertTrue("24 unit tiles missing", svg.contains("24") || svg.contains("۲۴"))
        // مدل کاشی باید بالاتر از عبارت جبری در بوم قرار گیرد
        val modelIdx = svg.indexOf("مدل کاشی")
        val exprIdx = svg.indexOf("عبارت جبری")
        assertTrue("Tile model header must appear before bottom expression", modelIdx in 0 until exprIdx)
    }

    @Test
    fun items3_8_9_10_interactiveCellsWithoutQuestionMarksAndStudentOverlay() {
        val cases = listOf(
            Triple("axdicefold", """{"t":"axdicefold","X":{"net":"cross","mode":"num","top":"1"}}""", "f4"),
            Triple("axunitconv", """{"t":"axunitconv","X":{"kind":"len","val":"2"}}""", "u1"),
            Triple("axprobtree", """{"t":"axprobtree","X":{"stages":"4","first":"A,B","second":"A,B","third":"A,B","fourth":"A,B"}}""", "n_1_0"),
            Triple("axorbital", """{"t":"axorbital","X":{"z":"54","mode":"box"}}""", "5p_2"),
            Triple("axpunnett", """{"t":"axpunnett","X":{"kind":"pedigree","gens":"4"}}""", "g3_1"),
            Triple("axkenken", """{"t":"axkenken","X":{"kind":"kenken","size":"4"}}""", "0_1")
        )
        for ((id, rawJson, emptyKey) in cases) {
            val base = FigureSpec.parse(rawJson)!!
            val regions = PrimaryInteractiveFigureHelper.regionsFor(base)
            assertTrue("Expected interactive regions for $id", regions.any { it.key == emptyKey })

            val emptied = PrimaryInteractiveFigureHelper.applyTeacherCellEdit(base, emptyKey, null, makeEmpty = true)
            val emptyKeys = PrimaryInteractiveFigureHelper.listEmptyCellKeys(emptied)
            assertTrue("Expected $emptyKey in empty keys of $id ($emptyKeys)", emptyKey in emptyKeys)

            val emptySvg = FigureSvgRenderer.render(emptied).xml
            assertFalse("$id empty cell must not render question mark", emptySvg.contains(">؟<"))

            val sampleAns = if (id == "axorbital") "↑↓" else "7"
            val enc = PrimaryInteractiveFigureHelper.encodeStudentCellAnswer(emptied, mapOf(emptyKey to sampleAns))
            val overlaid = PrimaryInteractiveFigureHelper.injectStudentOverlay(emptied, enc)
            val ansSvg = FigureSvgRenderer.render(overlaid).xml
            assertFalse("$id student overlay produced NaN", ansSvg.contains("NaN"))
            if (id != "axorbital") {
                assertTrue("$id student answer should appear in SVG", ansSvg.contains("7") || ansSvg.contains("۷"))
            }
        }

        // ابعاد پویا برای درخت احتمال ۳ و ۴ مرحله‌ای + پوشش ۵۴ الکترون زنون
        val tree3 = FigureSpec.parse("""{"t":"axprobtree","X":{"stages":"3"}}""")!!
        val tree4 = FigureSpec.parse("""{"t":"axprobtree","X":{"stages":"4"}}""")!!
        assertEquals(440f, PrimaryMathSvgRenderer.canvasWidth(tree3), 0.01f)
        assertEquals(520f, PrimaryMathSvgRenderer.canvasWidth(tree4), 0.01f)
        assertEquals(440f, PrimaryMathSvgRenderer.canvasHeight(tree4), 0.01f)

        val xe54 = FigureSpec.parse("""{"t":"axorbital","X":{"z":"54","mode":"box"}}""")!!
        val xeSvg = FigureSvgRenderer.render(xe54).xml
        assertTrue("Xenon Z=54 must include 5p orbital", xeSvg.contains("5p"))
        assertTrue("Xenon symbol missing", xeSvg.contains("Xe"))
    }

    @Test
    fun items4_6_7_11_and_item2_advancedModesRenderCleanly() {
        // ۴) محیط و مساحت: کنترل نمایش هر دو، فقط محیط، فقط مساحت، یا هیچ‌کدام
        val pBoth = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axperimeter","X":{"shape":"rect","w":"6","h":"4","show":"both"}}""")!!).xml
        val pOnlyP = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axperimeter","X":{"shape":"rect","w":"6","h":"4","show":"perim"}}""")!!).xml
        val pOnlyS = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axperimeter","X":{"shape":"rect","w":"6","h":"4","show":"area"}}""")!!).xml
        val pNone = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axperimeter","X":{"shape":"rect","w":"6","h":"4","show":"none"}}""")!!).xml
        assertTrue(pBoth.contains("P =") && pBoth.contains("S ="))
        assertTrue(pOnlyP.contains("P =") && !pOnlyP.contains("S ="))
        assertTrue(!pOnlyS.contains("P =") && pOnlyS.contains("S ="))
        assertTrue(!pNone.contains("P =") && !pNone.contains("S ="))

        // ۶) دستگاه دو معادلهٔ خطی: معادله‌ها بالای شبکه و قابلیت مخفی‌سازی
        val linEq = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axlinear","X":{"m1":"1","b1":"2","m2":"-1","b2":"4","show":"all"}}""")!!).xml
        val linNone = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axlinear","X":{"m1":"1","b1":"2","m2":"-1","b2":"4","show":"none"}}""")!!).xml
        assertTrue(linEq.contains("y ="))
        assertFalse(linNone.contains("y ="))

        // ۷) دایرهٔ مثلثاتی در همهٔ ربع‌ها و زوایای بازتابی/منفی بدون خطای کمان
        for (deg in listOf("0", "45", "120", "225", "315", "-60")) {
            val trig = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axtrigcircle","X":{"deg":"$deg","show":"all"}}""")!!).xml
            assertFalse("NaN in axtrigcircle deg=$deg", trig.contains("NaN"))
            assertTrue(trig.contains("sin"))
        }

        // ۲ و ۱۱) حالت‌های پیشرفتهٔ ۱۰ ابزار ارتقایافته + عدسی، سطح شیب‌دار و آونگ
        val advancedSpecs = listOf(
            """{"t":"axdna","X":{"mode":"mrna","seq":"ATGCGATC"}}""",
            """{"t":"axheatcurve","X":{"t0":"-20","t1":"120","m":"100","mode":"full"}}""",
            """{"t":"axcircuit","X":{"mode":"mixed","r1":"4","r2":"6","r3":"12","v":"18"}}""",
            """{"t":"axbarchart","X":{"data":"4,7,7,3,9","show":"all"}}""",
            """{"t":"axstaff","X":{"kind":"color","harmony":"comp"}}""",
            """{"t":"axstaff","X":{"kind":"staff","clef":"bass","notes":"48,50#,52b,53"}}""",
            """{"t":"axprosody","X":{"verse":"بنام خداوند جان و خرد","line":"فعولن فعولن فعولن فعل","mode":"arkan"}}""",
            """{"t":"axmapping","X":{"from":"1,2,3","to":"a,b,c","arrows":"1-a;2-b;3-c"}}""",
            """{"t":"axkenken","X":{"kind":"kakuro","size":"4"}}""",
            """{"t":"axcaliper","X":{"kind":"micrometer","val":"7.38","max":"25"}}""",
            """{"t":"axrounding","X":{"n":"14.37","mode":"tenth","label":"1"}}""",
            """{"t":"axoptics","X":{"kind":"concave","f":"10","d":"25","show":"all"}}""",
            """{"t":"axincline","X":{"ang":"30","mass":"5","mu":"0.2","show":"all"}}""",
            """{"t":"axpendulum","X":{"len":"1.2","amp":"25","mode":"forces"}}"""
        )
        for (raw in advancedSpecs) {
            val svg = FigureSvgRenderer.render(FigureSpec.parse(raw)!!).xml
            assertTrue("Empty SVG for $raw", svg.contains("<svg"))
            assertFalse("NaN in $raw", svg.contains("NaN"))
            assertFalse("Infinity in $raw", svg.contains("Infinity"))
        }

        // بررسی اتصال دانش‌آموز در وب و اندروید
        val studentJs = read("site/src/student.js")
        assertTrue(studentJs.contains("t === 'axprobtree'"))
        assertTrue(studentJs.contains("t === 'axorbital'"))
        assertTrue(studentJs.contains("t === 'axpunnett'"))
        val nativeMathKt = read("app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt")
        assertTrue(nativeMathKt.contains("PrimaryInteractiveFigureHelper.KEYED_CELL_TYPES"))
    }
}
