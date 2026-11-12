package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V369 — نمادهای «کمتر/بیشتر» و نشستن نماد روی پایه.
 *
 * شاردهای بانک مرجع بخشی از علائم را با فرمان می‌نویسند (`\lt`, `\gt`)؛ این‌ها
 * باید نماد شوند، نه □. همچنین `\overset{\triangle}{B}` باید نماد را وسطِ
 * پایه بنشاند (Accent)، نه در Script که حرف‌ها را جابه‌جا می‌کند.
 */
class V369_SymbolsTest {

    private fun glyphs(tex: String): List<String> {
        val out = mutableListOf<String>()
        fun walk(n: MathNode) {
            when (n) {
                is MathNode.Symbol -> out.add(n.value)
                is MathNode.Sequence -> n.children.forEach { walk(it) }
                is MathNode.Fraction -> { walk(n.top); walk(n.bottom) }
                is MathNode.Radical -> { n.index?.let { walk(it) }; walk(n.body) }
                is MathNode.Script -> { walk(n.base); n.upper?.let { walk(it) }; n.lower?.let { walk(it) } }
                is MathNode.Matrix -> n.rows.flatten().forEach { walk(it) }
                is MathNode.Accent -> walk(n.body)
                is MathNode.Delimited -> { out.add(n.open); walk(n.body); out.add(n.close) }
                MathNode.LineBreak -> out.add("\n")
            }
        }
        walk(NativeMathParser.parse(tex))
        return out
    }

    @Test
    fun `lt and gt commands become symbols`() {
        assertTrue(glyphs("x \\lt 5").contains("<"))
        assertTrue(glyphs("y \\gt 2").contains(">"))
        assertFalse(glyphs("x \\lt 5").contains("□"))
        assertTrue(NativeMathParser.unsupportedCommands("\\lt \\gt \\leqslant \\geqslant").isEmpty())
        assertTrue(glyphs("a \\leqslant b").contains("⩽"))
        assertTrue(glyphs("a \\geqslant b").contains("⩾"))
    }

    @Test
    fun `overset puts the mark on the base instead of a script`() {
        val node = NativeMathParser.parse("\\overset{\\triangle}{B}")
        assertTrue(node is MathNode.Accent)
        assertTrue((node as MathNode.Accent).mark == "over:△")
        assertEquals(listOf("B"), glyphs("\\overset{\\triangle}{B}").filter { it == "B" })
        assertTrue(NativeMathParser.parse("\\stackrel{def}{=}") is MathNode.Accent)
    }
}
