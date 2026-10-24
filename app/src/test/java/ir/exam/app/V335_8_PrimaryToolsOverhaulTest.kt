package ir.exam.app

import ir.exam.app.core.figure.AXIS_FIGURES
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer
import ir.exam.app.core.figure.MATH_CATEGORIES
import ir.exam.app.core.figure.MATH_TOOL_IDS
import ir.exam.app.core.figure.PrimaryInteractiveFigureHelper
import ir.exam.app.core.figure.PrimaryMathSvgRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V335.8 — بازطراحی و ارتقای ۱۶گانهٔ ابزارهای تعاملی ریاضی، علوم و فارسی دبستان در اندروید و وب.
 */
class V335_8_PrimaryToolsOverhaulTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun read(path: String): String = File(root(), path).readText()

    @Test
    fun item8_removedThreeToolsFromCatalogsWhilePreservingLegacySvgRenderers() {
        val removed = listOf("axspin", "axseedgrow", "axbodyparts")
        val galleryIds = AXIS_FIGURES.map { it.id }.toSet()
        for (id in removed) {
            assertFalse("$id should be removed from AXIS_FIGURES", id in galleryIds)
            assertFalse("$id should be removed from MATH_TOOL_IDS", id in MATH_TOOL_IDS)
        }
        assertEquals(41, MATH_CATEGORIES.first { it.id == "elementary" }.ids.size)
        assertEquals(101, MATH_TOOL_IDS.size)

        val graphFig = read("app/src/main/assets/print/web/graph_fig.js")
        val axisTypesBlock = graphFig.substringAfter("var AXIS_TYPES = [").substringBefore("];")
        for (id in removed) {
            assertFalse("$id should be removed from web AXIS_TYPES", "'$id'" in axisTypesBlock)
            assertTrue("$id legacy SVG branch must remain in PrimaryMathSvgRenderer", id in PrimaryMathSvgRenderer.SUPPORTED)
        }
    }

    @Test
    fun items1_2_3_4_base10FractionAbacusAndJumpUpgrades() {
        // ۱) دسته‌های صدتایی تا ۹۹۹٬۹۹۹٬۹۹۹
        val base10 = FigureSpec.parse("""{"t":"axbase10","X":{"n":"999999999"}}""")!!
        assertEquals(999_999_999L, PrimaryMathSvgRenderer.parseBase10Long(base10))
        val base10Svg = FigureSvgRenderer.render(base10).xml
        assertTrue(base10Svg.contains("صدگان میلیون"))
        assertTrue(read("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").contains("عدد (۰ تا ۹۹۹٬۹۹۹٬۹۹۹)"))
        assertTrue(read("app/src/main/assets/print/web/graph_fig.js").contains("عدد (۰ تا ۹۹۹٬۹۹۹٬۹۹۹)"))

        // ۲) کسر شکلی مربعی تا ۲۵ قسمت بدون تداخل متن پایین
        val frac25 = FigureSpec.parse("""{"t":"axfrac","X":{"num":"9","den":"25","shape":"square"}}""")!!
        val fracSvg = FigureSvgRenderer.render(frac25).xml
        assertTrue(fracSvg.contains(">9</text>") || fracSvg.contains(">۹</text>"))
        assertTrue(fracSvg.contains(">25</text>") || fracSvg.contains(">۲۵</text>"))
        assertTrue(fracSvg.contains("y=\"226\""))

        // ۳) چرتکهٔ ۶ستونی (یکان تا صدگان هزار) با کادر بزرگ ۴۶۰×۳۱۰
        val abacus = FigureSpec.parse("""{"t":"axabacus","X":{"n":"142536","cols":"6"}}""")!!
        assertEquals(460f, PrimaryMathSvgRenderer.canvasWidth(abacus), 0.01f)
        assertEquals(310f, PrimaryMathSvgRenderer.canvasHeight(abacus), 0.01f)
        val abacusSvg = FigureSvgRenderer.render(abacus).xml
        for (col in listOf("یکان", "دهگان", "صدگان", "یکان هزار", "دهگان هزار", "صدگان هزار")) {
            assertTrue("Missing column $col in abacus SVG", abacusSvg.contains(col))
        }
        assertTrue(read("app/src/main/assets/print/web/tools_styles.css").contains("width:min(100%,640px) !important"))

        // ۴) پرش روی محور با چهار عمل اصلی (+، -، ×، ÷)
        for ((op, sym) in listOf("+" to "+", "-" to "−", "*" to "×", "/" to "÷")) {
            val jmp = FigureSpec.parse("""{"t":"axjump","X":{"op":"$op","start":"12","step":"3","jumps":"4","max":"25"}}""")!!
            val jmpSvg = FigureSvgRenderer.render(jmp).xml
            assertTrue("Missing operator $sym for op=$op", jmpSvg.contains(sym))
        }
    }

    @Test
    fun items5_6_7_9_10_sawPopupPatternSymmetryBeakerAndFnMachine() {
        // ۵) جدول شگفت‌انگیز و سودوکو: ویرایش سلولی با انتخاب از پنجره
        val saw = FigureSpec.parse("""{"t":"axsaw","X":{"size":"4","mode":"num"}}""")!!
        val sawEdited = PrimaryInteractiveFigureHelper.setSawCell(saw, 0, 1, "_")
        assertNotNull(sawEdited)
        assertFalse(FigureSvgRenderer.render(sawEdited).xml.contains("NaN"))

        // ۶) الگویابی ۶ حالته (شامل عددی، افزایشی و ترکیبی) با فهرست کشویی الگوهای آماده
        for (mode in listOf("shape", "color", "bead", "number", "growing", "mixed")) {
            val pat = FigureSpec.parse("""{"t":"axpattern","X":{"mode":"$mode","seq":"2,4,6,8,?"}}""")!!
            val svg = FigureSvgRenderer.render(pat).xml
            assertTrue("Empty pattern SVG for $mode", svg.contains("<svg"))
        }
        val graphFig = read("app/src/main/assets/print/web/graph_fig.js")
        assertTrue(graphFig.contains("'axpattern:X.seq'"))
        assertTrue(graphFig.contains("'axpattern:X.mode'"))

        // ۷) تقارن شطرنجی: در حالت عدم نمایش کامل شکل قرینه هیچ علامت سؤالی روی شکل نیست
        val symHidden = FigureSpec.parse("""{"t":"axsym","X":{"axis":"v","mirror":"0","cells":"2,1;3,2"}}""")!!
        val symSvg = FigureSvgRenderer.render(symHidden).xml
        assertFalse("Symmetry grid must not render question marks when mirror=0", symSvg.contains("؟"))
        val symStudent = PrimaryInteractiveFigureHelper.injectStudentOverlay(symHidden, "2,5")
        val symStudentSvg = FigureSvgRenderer.render(symStudent).xml
        assertFalse(symStudentSvg.contains("؟"))
        assertTrue(symStudentSvg.contains("#27c4a8"))

        // ۹) استوانهٔ مدرج تا ۴ استوانه
        val beaker4 = FigureSpec.parse("""{"t":"axbeaker","X":{"count":"4","max":"100","val":"20","val2":"45","val3":"70","val4":"90"}}""")!!
        val beakerSvg = FigureSvgRenderer.render(beaker4).xml
        assertTrue(beakerSvg.contains("90") || beakerSvg.contains("۹۰"))
        assertTrue(graphFig.contains("'axbeaker:X.count'"))

        // ۱۰) ماشین ورودی و خروجی تا ۸ عدد
        val fn8 = FigureSpec.parse("""{"t":"axfnmachine","X":{"op1":"+ 3","in":"1,2,3,4,5,6,7,8","blank":"0"}}""")!!
        val fn8Svg = FigureSvgRenderer.render(fn8).xml
        assertTrue(fn8Svg.contains("11") || fn8Svg.contains("۱۱"))
    }

    @Test
    fun items11_12_13_14_15_16_interactiveEmptyCellsCubesFixesPaintSyllableAndMagnet() {
        // ۱۱) خانه‌های خالی بدون علامت سؤال + پاسخ مستقیم دانش‌آموز در تقویم، جدول ضرب، چرخ ضرب و هرم اعداد
        for ((t, emptyKey) in listOf(
            "axcalendar" to "15",
            "axmultgrid" to "1_1",
            "axmulwheel" to "s_0",
            "axtower" to "1_0"
        )) {
            val base = AXIS_FIGURES.first { it.id == t }.toSpec()
            val emptied = PrimaryInteractiveFigureHelper.applyTeacherCellEdit(base, emptyKey, null, makeEmpty = true)
            val emptySvg = FigureSvgRenderer.render(emptied).xml
            assertFalse("$t empty cell must not show question mark", emptySvg.contains(">؟<"))
            val withAns = PrimaryInteractiveFigureHelper.injectStudentOverlay(emptied, "42")
            val ansSvg = FigureSvgRenderer.render(withAns).xml
            assertTrue("$t student answer should render on figure", ansSvg.contains("42") || ansSvg.contains("۴۲"))
        }

        // ۱۲) شمارش مکعب‌ها: نمای بالا و روبه‌رو + ویرایشگر شبکه‌ای
        for (view in listOf("iso", "top", "front", "side")) {
            val cubes = FigureSpec.parse("""{"t":"axcubes","X":{"map":"2,1,3;1,0,2;3,2,1","view":"$view"}}""")!!
            val svg = FigureSvgRenderer.render(cubes).xml
            assertFalse("NaN in axcubes view=$view", svg.contains("NaN"))
            assertTrue(svg.contains("<rect") || svg.contains("<polygon"))
        }
        assertTrue(read("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").contains("CubesHeightMatrixEditor("))
        assertTrue(read("app/src/main/assets/print/web/graph_fig.js").contains("gf-cubes-ed"))

        // ۱۳) حواس پنج‌گانه، اهرم/قرقره/چرخ‌دنده، و قطار کلمه‌سازی بدون خطای هندسی
        for (specJson in listOf(
            """{"t":"axsenses","X":{"active":"0"}}""",
            """{"t":"axlever","X":{"kind":"pulley","w1":"4","d1":"3","w2":"6","d2":"2"}}""",
            """{"t":"axlever","X":{"kind":"gear","w1":"12","w2":"6"}}""",
            """{"t":"axwordtrain","X":{"parts":"مد,ر,سه","word":"مدرسه"}}"""
        )) {
            val svg = FigureSvgRenderer.render(FigureSpec.parse(specJson)!!).xml
            assertFalse(svg.contains("NaN"))
            assertFalse(svg.contains("undefined"))
        }

        // ۱۴) شبکهٔ رنگ‌آمیزی با تاگل مستقیم تک‌خانه
        val paint = FigureSpec.parse("""{"t":"axpaintgrid","X":{"rows":"4","cols":"4","cells":"1,1"}}""")!!
        val paint2 = PrimaryInteractiveFigureHelper.togglePaintCell(paint, 2, 3)
        assertTrue(paint2.xStr("cells").contains("2,3"))

        // ۱۵) هجاهای کلمه با افزودن پویا (+) در صفحهٔ دانش‌آموز اندروید و وب + نگهبان EssayQuestion
        val studentKt = read("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt")
        assertTrue(studentKt.contains("+ افزودن کادر"))
        assertTrue(studentKt.contains("question is EssayQuestion"))
        assertFalse(studentKt.contains("DescriptiveQuestion"))
        assertTrue(read("site/src/student.js").contains("+ افزودن کادر"))
        assertTrue(read("site/src/student.js").contains("bindInteractiveQuestionFigs("))

        // ۱۶) آهنربا و قطب‌ها با منوی کشویی فارسی (جذب / دفع)
        assertTrue(read("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").contains("\"axmagnet\" to \"mode\""))
        assertTrue(read("app/src/main/assets/print/web/graph_fig.js").contains("'axmagnet:X.mode'"))
    }
}
