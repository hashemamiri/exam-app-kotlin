package ir.exam.app.ui.app

import ir.exam.app.core.math.MathNode
import ir.exam.app.core.math.NativeMathParser
import ir.exam.app.core.math.NativeMathSvgRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V312.2 — ۱) در ۸ بلوک صفحهٔ فرود (دسکتاپ و موبایل) «وایت برد» به‌جای «تختهٔ سفید» نمایش داده شود؛
 * ۲) جمع زیر هم (\vadd)، تفریق زیر هم (\vsub)، ضرب زیر هم (\vmul) و تقسیم کلنگی (\ldiv) در ویرایشگر فرمول،
 * پیش‌نمایش/چاپ، خروجی Word و رندرر بومی Kotlin پشتیبانی شوند.
 */
class V312_2_VerticalOpsAndWhiteboardTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun landingTilesDisplayWhiteboardOnDesktopAndMobile() {
        val app = src("site/src/app.js")
        val mob = src("site/src/mobile.js")
        assertTrue("t[0] === 'board' ? 'وایت برد' : t[1]" in app)
        assertTrue("['🎨', 'وایت برد', 'board']" in mob)
    }

    @Test
    fun formulaEditorHasVerticalOpsAndLongDivision() {
        val fh = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue(".math-vop,.mathx .math-vop{display:inline-table" in fh)
        assertTrue(".math-ldiv,.mathx .math-ldiv{display:inline-table" in fh)
        assertTrue("function mbVop(op)" in fh && "function mbLdiv()" in fh)
        assertTrue("['جمع زیر هم', '\\\\vadd{a}{b}{c}', ['vadd']]" in fh)
        assertTrue("['تفریق زیر هم', '\\\\vsub{a}{b}{c}', ['vsub']]" in fh)
        assertTrue("['ضرب زیر هم', '\\\\vmul{a}{b}{c}', ['vmul']]" in fh)
        assertTrue("['تقسیم کلنگی', '\\\\ldiv{a}{b}{q}{r}', ['ldiv']]" in fh)
        assertTrue("['تقسیم چکمه‌ای / تفنگی (کادر └─)', '\\\\sdiv{a}{b}{q}{c}{r}', ['sdiv']]" in fh)
        assertTrue("['تقسیم کلنگی با تفریق (خط ممتد)', '\\\\bdiv{a}{b}{q}{c}{r}', ['bdiv']]" in fh)
        assertTrue("['چندسطری با انتخاب علامت‌ها', '\\\\vops{}{a}{+}{b}{-;}{c}{}{d}', ['vops']]" in fh)
        assertTrue("function mbVops(rowsSpec)" in fh && "function mbBdiv(steps)" in fh && "function mbSdiv(steps)" in fh && "function mbVopAddRow(op, asAnswer)" in fh)
        assertTrue("<div id=\"mbVopBar\" class=\"mb-vop-bar\"" in fh)
        assertTrue("<button type=\"button\" class=\"mb-kt mb-kt-abc\" onclick=\"mbOpenAbcPad(event)\" title=\"حروف انگلیسی\">abc</button>" in fh)
        assertTrue("<button type=\"button\" class=\"mb-kt mb-kt-vop\" onclick=\"mbToolMenu(event,'vop')\" title=\"جمع، تفریق و ضرب زیر هم و تقسیم کلنگی\">زیرهم</button>" in fh)
        assertTrue("#mfModal.box-fullscreen .mb-kt-enter,#mfModal.box-fullscreen .mb-kt-abc,#mfModal.box-fullscreen .mb-chip-vop{display:none !important}" in fh)
        assertTrue("#mfModal.box-fullscreen .mb-kt-vop{display:inline-flex !important;order:13}" in fh)
        assertTrue("#mfModal.box-fullscreen .mb-fixed-keypad button[onclick=\"mbInsert('sym','=')\"],\n  #mfModal.box-fullscreen .mb-kt-abc,\n  #mfModal.box-fullscreen .mb-chip-vop,\n  #mfModal.box-fullscreen .mb-kt-enter{display:none !important}" in fh)
        val mh = src("app/src/main/assets/print/web/math_host.js")
        assertTrue("if (cmd === 'vadd' || cmd === 'vsub' || cmd === 'vmul')" in mh)
        assertTrue("if (cmd === 'ldiv')" in mh && "if (cmd === 'vops')" in mh && "if (cmd === 'bdiv')" in mh && "if (cmd === 'sdiv')" in mh)
        val we = src("app/src/main/assets/print/web/word_export.js")
        assertTrue("cl.contains('math-vop')" in we && "cl.contains('math-ldiv')" in we && "cl.contains('math-sdiv')" in we && "classList.contains('math-ldiv-sub')" in we)
    }

    @Test
    fun nativeKotlinParserAndSvgRenderVerticalOpsAndLongDivision() {
        val formulas = listOf(
            "\\vadd{12}{34}{46}",
            "\\vsub{50}{18}{32}",
            "\\vmul{12}{3}{36}",
            "\\vops{}{120}{+}{35}{-;}{15}{}{140}",
            "\\vops{}{12}{*;}{11}{}{12}{+;}{120}{}{132}",
            "\\ldiv{17}{5}{3}{2}",
            "\\bdiv{17}{5}{3}{15}{2}",
            "\\bdiv{125}{5}{25}{10}{25}{25}{0}",
            "\\sdiv{27}{6}{4}{24}{03}"
        )
        for (tex in formulas) {
            assertEquals(emptySet<String>(), NativeMathParser.unsupportedCommands(tex))
            val ast = NativeMathParser.parse(tex)
            assertTrue(ast is MathNode.Matrix)
            val svg = NativeMathSvgRenderer.render(tex, fontSizePx = 20f).xml
            assertFalse("□" in svg)
            assertTrue("<line " in svg)
        }
    }
}
