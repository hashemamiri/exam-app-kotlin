package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V368 — هیچ مربعِ «□» کنار فرمول‌های بانک مرجع نباشد.
 *
 * شاردهای بانک مرجع تقریباً هر فرمول را با `\\displaystyle` آغاز می‌کنند؛
 * ویرایشگر وب این دستورها را بی‌صدا رد می‌کند ولی پارسر بومی برایشان شاخه
 * نداشت و به‌جای آن □ می‌گذاشت. این تست روی همان بردارهای واقعی شارد
 * (`v361_formula_vectors.json`) و محیط‌های رایج کتاب درسی می‌ایستد تا □
 * برنگردد، و مطمئن می‌شود دستور ناشناخته هنوز □ نشان می‌دهد.
 */
class V368_FormulaBoxRegressionTest {

    private fun glyphsOf(node: MathNode): List<String> = when (node) {
        is MathNode.Symbol -> listOf(node.value)
        is MathNode.Sequence -> node.children.flatMap { glyphsOf(it) }
        is MathNode.Fraction -> glyphsOf(node.top) + glyphsOf(node.bottom)
        is MathNode.Radical ->
            (node.index?.let { glyphsOf(it) } ?: emptyList()) + glyphsOf(node.body)
        is MathNode.Script ->
            glyphsOf(node.base) +
                (node.upper?.let { glyphsOf(it) } ?: emptyList()) +
                (node.lower?.let { glyphsOf(it) } ?: emptyList())
        is MathNode.Matrix -> node.rows.flatten().flatMap { glyphsOf(it) }
        is MathNode.Accent -> glyphsOf(node.body)
        is MathNode.Delimited -> listOf(node.open) + glyphsOf(node.body) + listOf(node.close)
        MathNode.LineBreak -> listOf("\n")
    }

    private fun glyphsOf(tex: String): List<String> = glyphsOf(NativeMathParser.parse(tex))

    private fun visible(tex: String): List<String> = glyphsOf(tex).filter { it.isNotBlank() }

    @Test
    fun `reference bank displaystyle formula shows no box`() {
        val vectors = listOf(
            "\\displaystyle \\text{۸}-\\text{۳}=\\text{۵}",
            "\\displaystyle \\left(\\dfrac{\\text{۳}}{\\text{۴}}\\right)",
            "\\displaystyle\\text{۱۰}-\\text{۲}",
            "\\textstyle \\frac{۱}{۲}",
            "\\scriptstyle x^{۲}"
        )
        vectors.forEach { tex ->
            assertFalse(tex, glyphsOf(tex).contains("□"))
        }
        assertEquals(
            visible("\\text{۸}-\\text{۳}=\\text{۵}"),
            visible("\\displaystyle \\text{۸}-\\text{۳}=\\text{۵}")
        )
        assertTrue(visible("\\displaystyle \\text{۸}").contains("۸"))
    }

    @Test
    fun `math environments render instead of box`() {
        val cases = listOf(
            "\\begin{aligned} a &= b \\\\ c &= d \\end{aligned}",
            "\\begin{align*} a &= b \\end{align*}",
            "\\begin{array}{cc} 1 & 2 \\\\ 3 & 4 \\end{array}",
            "\\begin{gathered} a \\\\ b \\end{gathered}",
            "\\begin{bmatrix} 1 & 2 \\\\ 3 & 4 \\end{bmatrix}",
            "\\begin{weird} x+y \\end{weird}",
            "\\begin{matrix} a"
        )
        cases.forEach { tex -> assertFalse(tex, glyphsOf(tex).contains("□")) }
        val array = NativeMathParser.parse("\\begin{array}{cc} 1 & 2 \\\\ 3 & 4 \\end{array}")
        assertTrue(array is MathNode.Matrix)
        assertEquals(2, (array as MathNode.Matrix).rows.size)
        assertEquals(2, array.rows[0].size)
    }

    @Test
    fun `style wrapper and spacing commands are silent`() {
        val cases = listOf(
            "\\phantom{x} y",
            "\\hspace{1cm} x",
            "\\hspace*{2mm} x",
            "\\color{red}{x}",
            "\\textcolor{blue}{y}",
            "\\bf{x}",
            "\\rm x",
            "\\cal L",
            "\\cancel{x} + \\boxed{y}",
            "\\mathop{x}",
            "\\operatorname*{argmax}_{x} f(x)",
            "\\lbrace x \\rbrace",
            "\\lbrack 1,2 \\rbrack",
            "\\substack{a \\\\ b}",
            "\\genfrac{(}{)}{0}{}{a}{b}",
            "\\not\\in A",
            "\\left\\{ x \\middle| x>0 \\right\\}",
            "\\stackrel{def}{=}",
            "\\widetilde{AB}",
            "\\tag{1} x"
        )
        cases.forEach { tex -> assertFalse(tex, glyphsOf(tex).contains("□")) }
        assertTrue(visible("\\operatorname*{argmax}").contains("argmax"))
        assertTrue(visible("\\not\\in").contains("∉"))
        assertTrue(visible("\\lbrace").contains("{"))
    }

    @Test
    fun `unknown command still shows the box`() {
        assertTrue(glyphsOf("\\definitelyUnknownCommand").contains("□"))
    }

    @Test
    fun `style commands are reported as supported`() {
        val tex = "\\displaystyle \\textstyle \\scriptstyle \\limits \\phantom{x} " +
            "\\color{red}{x} \\hspace{1cm} \\cancel{x} \\lbrace \\rbrace"
        assertTrue(NativeMathParser.unsupportedCommands(tex).toString(),
            NativeMathParser.unsupportedCommands(tex).isEmpty())
    }
}
