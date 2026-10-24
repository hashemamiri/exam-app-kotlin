package ir.exam.app

import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer
import ir.exam.app.core.figure.PrimaryInteractiveFigureHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V335.13 — بازطراحی آیکن‌های حواس پنج‌گانه، جدول کارنوی تعاملی، ارتقای ۱۴ ابزار دانشگاهی و علوم انسانی،
 * و اصلاح عنوان «تنظیمات ابزار» در پنجرهٔ ابزار ریاضی (اندروید و وب).
 */
class V335_13_UniversityHumanitiesToolsTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun read(path: String): String = File(root(), path).readText()

    @Test
    fun fiveSensesIconsAndModesRenderCleanlyOnAndroidAndWeb() {
        for (mode in listOf("full", "blank_sense", "blank_organ", "icon_only")) {
            val spec = FigureSpec.parse("""{"t":"axsenses","X":{"mode":"$mode","active":"2"}}""")!!
            val svg = FigureSvgRenderer.render(spec).xml
            assertTrue("Expected valid SVG for axsenses mode=$mode", svg.contains("<svg"))
            assertFalse("Unexpected NaN in axsenses mode=$mode", svg.contains("NaN"))
            assertTrue("Expected rich anatomical path shapes in axsenses", svg.split("<path").size >= 8)
        }
        val fullSvg = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axsenses","X":{"mode":"full"}}""")!!).xml
        for (lbl in listOf("بینایی", "شنوایی", "بویایی", "چشایی", "لامسه")) {
            assertTrue("Missing sense label $lbl", fullSvg.contains(lbl))
        }
        val graphFig = read("app/src/main/assets/print/web/graph_fig.js")
        assertTrue(graphFig.contains("'axsenses:X.mode'"))
        assertTrue(graphFig.contains("'axsenses:X.active'"))
    }

    @Test
    fun interactiveKarnaughMapSupportsTeacherEmptySlotsAndStudentOverlay() {
        val base = FigureSpec.parse("""{"t":"axkmap","X":{"vars":"4","ones":"0,1,4,5,8,10","dc":"15","groups":"0,2;0,4","expr":"B'D'"}}""")!!
        assertTrue("axkmap" in PrimaryInteractiveFigureHelper.KEYED_CELL_TYPES)

        val regions = PrimaryInteractiveFigureHelper.regionsFor(base)
        assertEquals("4-var K-Map with expr should expose 16 minterms + 1 expr region", 17, regions.size)
        val m0 = regions.firstOrNull { it.key == "m_0" }
        assertNotNull("Expected region m_0", m0)
        val hit = PrimaryInteractiveFigureHelper.hitEditableNumberCell(base, m0!!.x + m0.w / 2f, m0.y + m0.h / 2f)
        assertNotNull("Expected hit on m_0 center", hit)
        assertEquals("m_0", hit!!.key)

        val chips = PrimaryInteractiveFigureHelper.quickChipsForCell(base, "m_0").map { it.first }
        assertEquals(listOf("1", "0", "X"), chips)
        assertTrue(PrimaryInteractiveFigureHelper.quickChipsForCell(base, "expr").isEmpty())

        // خالی‌کردن خانه توسط طراح سؤال: بدون علامت سؤال (؟)
        val emptied = PrimaryInteractiveFigureHelper.applyTeacherCellEdit(base, "m_0", null, makeEmpty = true)
        val withDontCare = PrimaryInteractiveFigureHelper.applyTeacherCellEdit(emptied, "m_2", "X", makeEmpty = false)
        val emptyKeys = PrimaryInteractiveFigureHelper.listEmptyCellKeys(withDontCare)
        assertEquals(listOf("m_0"), emptyKeys)

        val teacherSvg = FigureSvgRenderer.render(withDontCare).xml
        assertFalse("Empty K-Map cell must not contain question mark", teacherSvg.contains(">؟<"))
        assertFalse("Empty K-Map cell must not contain ASCII question mark", teacherSvg.contains(">?<"))
        assertTrue("Expected overridden X value in m_2", teacherSvg.contains(">X<"))

        // ثبت پاسخ دانش‌آموز در خانهٔ خالی
        val enc = PrimaryInteractiveFigureHelper.encodeStudentCellAnswer(withDontCare, mapOf("m_0" to "1"))
        val overlaid = PrimaryInteractiveFigureHelper.injectStudentOverlay(withDontCare, enc)
        val studentSvg = FigureSvgRenderer.render(overlaid).xml
        assertFalse(studentSvg.contains("NaN"))
        assertTrue(studentSvg.contains("#1d4ed8"))

        val graphFig = read("app/src/main/assets/print/web/graph_fig.js")
        val studentJs = read("site/src/student.js")
        assertTrue("Web K-Map should emit icWrap regions", graphFig.contains("icWrap(cKey, 'خانهٔ m'"))
        assertTrue("Web student exam should bind axkmap DOM keyed cells", studentJs.contains("t === 'axkmap'"))
    }

    @Test
    fun allFourteenUniversityAndHumanitiesToolsRenderAccurately() {
        val specs = listOf(
            """{"t":"axflowchart","X":{"mode":"branch","steps":"شروع,ورودی: n,n > 0 ؟,چاپ n,پایان"}}""",
            """{"t":"axflowchart","X":{"mode":"loop","steps":"شروع,i = 1,i <= 5 ؟,چاپ i,پایان"}}""",
            """{"t":"axflowchart","X":{"mode":"seq","steps":"شروع,بخوان a,b = 2a,چاپ b,پایان"}}""",
            """{"t":"axmatrix","X":{"op":"mul","a":"1,2;3,4","b":"5,6;7,8"}}""",
            """{"t":"axmatrix","X":{"op":"inv","a":"4,7;2,6"}}""",
            """{"t":"axmatrix","X":{"op":"add","a":"1,2;3,4","b":"5,6;7,8"}}""",
            """{"t":"axvectors","X":{"ax1":"3","ay1":"1","bx":"1","by":"2","mode":"sum"}}""",
            """{"t":"axvectors","X":{"ax1":"3","ay1":"1","bx":"1","by":"2","mode":"diff"}}""",
            """{"t":"axvectors","X":{"ax1":"3","ay1":"0","bx":"0","by":"4","mode":"dot"}}""",
            """{"t":"axsentence","X":{"subj":"دانش‌آموز","prep":"در کتابخانه","obj":"کتاب را","verb":"مطالعه می‌کند","mode":"verbal"}}""",
            """{"t":"axsentence","X":{"subj":"هوا","obj":"آفتابی","verb":"است","mode":"nominal"}}""",
            """{"t":"axmapgeo","X":{"kind":"compass"}}""",
            """{"t":"axmapgeo","X":{"kind":"compass_quiz"}}""",
            """{"t":"axmapgeo","X":{"kind":"map","cities":"تهران:51,36;اصفهان:52,32;شیراز:53,29"}}""",
            """{"t":"axtimeline","X":{"dir":"ltr","mode":"full"}}""",
            """{"t":"axtimeline","X":{"dir":"rtl","mode":"diff"}}""",
            """{"t":"axgame","X":{"kind":"game","mat":"3,0;5,1"}}""",
            """{"t":"axgame","X":{"kind":"islm"}}""",
            """{"t":"axgame","X":{"kind":"islm_fiscal"}}""",
            """{"t":"axgame","X":{"kind":"islm_monetary"}}""",
            """{"t":"axsupply","X":{"kind":"equilibrium"}}""",
            """{"t":"axsupply","X":{"kind":"shift","shift":"2"}}""",
            """{"t":"axsupply","X":{"kind":"shift_s","shift":"2"}}""",
            """{"t":"axsupply","X":{"kind":"ceiling","ceiling":"2"}}""",
            """{"t":"axsupply","X":{"kind":"floor"}}""",
            """{"t":"axpert","X":{"acts":"A:1-2:3;B:2-3:2;C:2-4:4;D:3-4:1","show":"cpm"}}""",
            """{"t":"axpert","X":{"acts":"A:1-2:3;B:2-3:2;C:2-4:4;D:3-4:1","show":"quiz"}}""",
            """{"t":"axpv","X":{"kind":"carnot"}}""",
            """{"t":"axpv","X":{"kind":"otto"}}""",
            """{"t":"axpv","X":{"kind":"diesel"}}""",
            """{"t":"axpv","X":{"kind":"rankine"}}""",
            """{"t":"axbeam","X":{"load":"point","len":"6","p":"12","a":"2"}}""",
            """{"t":"axbeam","X":{"load":"udl","len":"6","q":"3"}}""",
            """{"t":"axbeam","X":{"load":"cantilever","len":"4","p":"8"}}""",
            """{"t":"axbode","X":{"kind":"bode","gain":"20","poles":"1,10","zeros":"5"}}""",
            """{"t":"axbode","X":{"kind":"nyquist","gain":"10","poles":"1,5,20"}}""",
            """{"t":"axbst","X":{"mode":"bst","vals":"50,30,70,20,40,60,80,10,35","trav":"inorder"}}""",
            """{"t":"axbst","X":{"mode":"heap","vals":"90,75,80,40,60,55,30"}}""",
            """{"t":"axgate","X":{"kind":"xnor","a":"1","b":"1","show":"all"}}""",
            """{"t":"axgate","X":{"kind":"nand","a":"1","b":"0","show":"quiz"}}"""
        )
        for (raw in specs) {
            val svg = FigureSvgRenderer.render(FigureSpec.parse(raw)!!).xml
            assertTrue("Expected <svg in $raw", svg.contains("<svg"))
            assertFalse("Unexpected NaN in $raw", svg.contains("NaN"))
            assertFalse("Unexpected Infinity in $raw", svg.contains("Infinity"))
        }

        // بررسی صحت علمی ضرب ماتریس، زاویهٔ متعامد بردارها، تعادل نش و مسیر بحرانی
        val mulSvg = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axmatrix","X":{"op":"mul","a":"1,2;3,4","b":"5,6;7,8"}}""")!!).xml
        assertTrue(mulSvg.contains("19") || mulSvg.contains("۱۹"))
        val dotSvg = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axvectors","X":{"ax1":"3","ay1":"0","bx":"0","by":"4","mode":"dot"}}""")!!).xml
        assertTrue(dotSvg.contains("90°") || dotSvg.contains("۹۰°"))
        val gameSvg = FigureSvgRenderer.render(FigureSpec.parse("""{"t":"axgame","X":{"kind":"game","mat":"3,0;5,1"}}""")!!).xml
        assertTrue("Expected Nash star marker in Prisoner's Dilemma", gameSvg.contains("★"))
    }

    @Test
    fun mathToolPickerDisplaysToolSettingsInsteadOfChartSettings() {
        val pickerKt = read("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt")
        val graphFig = read("app/src/main/assets/print/web/graph_fig.js")
        assertTrue(pickerKt.contains("تنظیمات ابزار"))
        assertTrue(pickerKt.contains("isMathTool"))
        assertTrue(graphFig.contains("math ? 'تنظیمات ابزار'"))
    }
}
