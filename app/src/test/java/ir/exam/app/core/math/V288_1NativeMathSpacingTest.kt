package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V288.1 — رندر بومی کادر سؤال: معادلهٔ ارگون و فرمول‌های کتابخانهٔ دانشگاه بدون شکاف/□ */
class V288_1NativeMathSpacingTest {
    private fun flat(n: MathNode): String = when (n) {
        is MathNode.Symbol -> n.value
        is MathNode.Sequence -> n.children.joinToString("") { flat(it) }
        is MathNode.Fraction -> flat(n.top) + "/" + flat(n.bottom)
        is MathNode.Script -> flat(n.base) + (n.upper?.let { "^" + flat(it) } ?: "") + (n.lower?.let { "_" + flat(it) } ?: "")
        is MathNode.Radical -> "√" + flat(n.body)
        is MathNode.Delimited -> n.open + flat(n.body) + n.close
        is MathNode.Accent -> flat(n.body)
        is MathNode.Matrix -> n.rows.joinToString(";") { r -> r.joinToString(",") { flat(it) } }
        else -> ""
    }

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    @Test
    fun `raw spaces are ignored in math but kept beside Persian text`() {
        val ergun = "\\frac{\\Delta P}{L}=\\frac{150\\mu v(1-\\epsilon)^{2}}{d^{2}_{p}\\epsilon^{3}}+\\frac{1.75\\rho v^{2}(1-\\epsilon)}{d_{p}\\epsilon^{3}}"
        val flatErgun = flat(NativeMathParser.parse(ergun))
        assertFalse(flatErgun, flatErgun.contains(" "))
        assertEquals("ΔP/L=150μv(1−ε)^2/d^2_pε^3+1.75ρv^2(1−ε)/d_pε^3", flatErgun)
        assertEquals("x=ab+2 ، عدد y", flat(NativeMathParser.parse("x = ab + 2 ، عدد y")))
    }

    @Test
    fun `commands glued to letters or non-ascii and size prefixes do not produce boxes`() {
        assertEquals("‖f‖_p", flat(NativeMathParser.parse("\\Vertf\\Vert_{p}")))
        assertEquals("λw(x)", flat(NativeMathParser.parse("\\lambdaw(x)")))
        assertEquals("π_1(S^1)≅ℤ", flat(NativeMathParser.parse("\\pi_{1}(S^{1})\\congℤ")))
        assertEquals("K⊂ℝ^n", flat(NativeMathParser.parse("K\\subsetℝ^{n}")))
        assertEquals("N⊴G", flat(NativeMathParser.parse("N \\trianglelefteq G")))
        assertEquals("∂u/∂n|_∂Ω=h", flat(NativeMathParser.parse("\\frac{\\partial u}{\\partial n}\\Big|_{\\partial\\Omega}=h")))
        assertEquals("P(|X−μ|≥t)", flat(NativeMathParser.parse("P\\left( |\\overline{X}-\\mu|\\geqt \\right)")))
    }

    @Test
    fun `display layout does not pad narrow glyphs but edit layout keeps touch targets`() {
        val shown = NativeMathSvgRenderer.render("1.75", 40f)
        val edited = NativeMathSvgRenderer.render("1.75", 40f, showEditBoxes = true)
        assertTrue(shown.widthPx < edited.widthPx)
        assertTrue(shown.widthPx < 40f * 2.4f)
    }

    @Test
    fun `editor serializer separates letter commands from following letters and site has no native confirm`() {
        val html = File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
        assertTrue("/\\\\[A-Za-z]+\$/.test(acc) && /^[A-Za-z]/.test(piece)" in html)
        val app = File(root(), "site/src/app.js").readText()
        assertFalse(Regex("[^.a-zA-Z_]confirm\\('").containsMatchIn(app))
        assertTrue("confirmDlg('به‌روزرسانی سایت'" in app)
    }
}
