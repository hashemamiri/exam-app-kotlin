package ir.exam.app.ui.app

import ir.exam.app.core.figure.FigureDigits
import ir.exam.app.core.math.MathNode
import ir.exam.app.core.math.NativeMathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V287 — ویرایشگر فرمول: ردیف اضافهٔ دسکتاپ، اعداد فارسی همه‌جا، تتراسیون، پیش‌فرض کروشهٔ ماتریس. */
class V287_FormulaEditorDesktopTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun desktopExtraRowIsHiddenOnPhones() {
        val f = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue("<div class=\"mb-dk-extra\"" in f)
        assertTrue(".mb-dk-extra{display:none}" in f)
        assertTrue("@media (min-width:861px) and (pointer:fine){\n  #mfModal.box-fullscreen .mb-dk-extra{" in f)
        assertTrue("#mfModal.box-fullscreen .mb-digitkey{order:17}" in f)
        assertTrue("#mfModal.box-fullscreen .mb-spacekey{order:18}" in f)
    }

    @Test
    fun tetrationTemplateEverywhere() {
        val f = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue("['تتراسیون','ⁿa',['tpl','{}^{n}a']]" in f)
        assertTrue("{ label:'تتراسیون', preview:'ⁿa', act:['tpl','{}^{n}a'] }" in f)
        assertTrue("onclick=\"mbPadDo(['tpl','{}^{n}a'])\"" in f)
    }

    @Test
    fun nativeParserAcceptsLeftSuperscript() {
        val node = NativeMathParser.parse("^{n}a")
        val first = (node as MathNode.Sequence).children.first()
        assertTrue(first is MathNode.Script)
        assertEquals("", ((first as MathNode.Script).base as MathNode.Symbol).value)
        assertEquals("n", (first.upper as MathNode.Symbol).value)
    }

    @Test
    fun persianDigitsReachKeypadRenderersAndSite() {
        val f = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue("function mbSetDigitsFa(v) { MB_DIGIT_FA = !!v; mbSyncDigitBtn(); }" in f)
        assertTrue("window.__mfPersianDigits !== undefined && window.mbSetDigitsFa" in f)
        assertTrue("window.__mfPersianDigits=\" + ir.exam.app.core.figure.FigureDigits.persian" in src("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt"))
        assertTrue("FigureDigits.apply(node.value)" in src("app/src/main/java/ir/exam/app/core/math/NativeMathCanvasRenderer.kt"))
        assertTrue("val shownValue = ir.exam.app.core.figure.FigureDigits.apply(node.value)" in src("app/src/main/java/ir/exam/app/core/math/NativeMathSvgRenderer.kt"))
        assertTrue("function faMathDigits(html) {" in src("app/src/main/assets/print/web/math_host.js"))
        val app = src("site/src/app.js")
        assertTrue("window.__mfPersianDigits=' + fa + ';window.__figPersianDigits=' + fa" in app)
        assertTrue("persianDigits: persianDigitsOn()" in app)
        assertTrue("اعداد فارسی در ابزارها و فرمول‌ها" in src("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt"))
        assertTrue("اعداد فارسی در ابزارها و فرمول‌ها" in src("site/src/mobile.js"))
    }

    @Test
    fun figureDigitsKeepsLength() {
        val old = FigureDigits.persian
        try {
            FigureDigits.persian = true
            assertEquals("x۱۲", FigureDigits.apply("x12"))
            assertEquals(3, FigureDigits.apply("x12").length)
        } finally { FigureDigits.persian = old }
    }

    @Test
    fun matrixDefaultsToBrackets() {
        val f = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue("let MB_MATRIX_DIM = { r: 2, c: 2, env: 'bmatrix' };" in f)
        assertTrue("mbInsert('matrix', { rows: d.r, cols: d.c, env: d.env || 'bmatrix' });" in f)
        assertTrue("[['bmatrix','[ ]'],['pmatrix','( )'],['vmatrix','| |'],['Bmatrix','{ }'],['matrix','بدون']]" in f)
        assertTrue("W.__mbBracketDefault = bracketDefault;" in f)
        assertTrue("if (MB_MATRIX_DIM) MB_MATRIX_DIM.env = 'bmatrix';" in f)
    }

    @Test
    fun formulaBackAndToolNoDuplicateBar() {
        val f = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue("id=\"mbBackWinBtn\"" in f && "window.mbBackOrClose = function ()" in f)
        assertTrue("window.__mbActivePrevMenu = window.__mbFromCatMenu" in f)
        val host = src("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt")
        assertTrue("window.mbBackOrClose ? window.mbBackOrClose() : 'closed'" in host)
        val app = src("site/src/app.js")
        assertTrue("overlay.__onBack = function ()" in app)
        assertTrue("iframe.srcdoc = h;\n      iframe.addEventListener('load'" in app)
        val mob = src("site/src/mobile.js")
        assertTrue("if (typeof e.__onBack === 'function') { e.__onBack(); return true; }" in mob)
        val builder = src("site/src/builder.js")
        val fn = builder.substringAfter("function insertFigure(").substringBefore("function dictate(")
        assertTrue("overlay.__close = close;" in fn && "engine-bar" !in fn)
    }
}
