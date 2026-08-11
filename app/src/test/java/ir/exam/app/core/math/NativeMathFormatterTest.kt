package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeMathFormatterTest {
    @Test
    fun `fraction root scripts and symbols render without WebView`() {
        val rendered = NativeMathFormatter.renderText(
            "حل ${'$'}\\frac{x^2}{\\sqrt{a_1}} \\leq \\infty${'$'}"
        )
        assertTrue(rendered.contains("(x²)⁄(√(a₁))"))
        assertTrue(rendered.contains("≤"))
        assertTrue(rendered.contains("∞"))
    }

    @Test
    fun `quick syntax converts deterministically`() {
        assertEquals("\\sqrt{x} \\leq 4 \\times 2", NativeMathFormatter.quickToTex("sqrt(x) <= 4 * 2"))
    }

    @Test
    fun `unclosed dollar remains ordinary text and braces validate`() {
        assertEquals("مقدار ${'$'}x+1", NativeMathFormatter.renderText("مقدار ${'$'}x+1"))
        assertTrue(NativeMathFormatter.isBalanced("\\frac{1}{2}"))
        assertFalse(NativeMathFormatter.isBalanced("\\frac{1}{2"))
    }

    @Test
    fun `matrix is converted to accessible rows`() {
        val rendered = NativeMathFormatter.renderTex("\\begin{bmatrix}a&b\\\\c&d\\end{bmatrix}")
        assertEquals("[a  b; c  d]", rendered)
    }
}
