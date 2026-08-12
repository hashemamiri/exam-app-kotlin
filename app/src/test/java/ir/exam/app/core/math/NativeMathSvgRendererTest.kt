package ir.exam.app.core.math

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.InputSource

class NativeMathSvgRendererTest {
    @Test
    fun `fraction radical scripts matrix and delimiter become valid svg`() {
        val document = NativeMathSvgRenderer.render(
            "\\left(\\frac{x^2}{\\sqrt[3]{y_1}}\\right)+\\begin{bmatrix}a&b\\\\c&d\\end{bmatrix}"
        )
        val xml = document.xml
        val parsed = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))

        assertEquals("svg", parsed.documentElement.localName ?: parsed.documentElement.nodeName)
        assertTrue(xml.startsWith("<svg"))
        assertTrue(xml.contains("<line"))
        assertTrue(xml.contains("<path"))
        assertTrue(xml.contains("<text"))
        assertTrue(document.widthPx > 0f)
        assertTrue(document.heightPx > 0f)
        assertFalse(xml.contains("\\frac"))
        assertFalse(xml.contains("\\sqrt"))
    }

    @Test
    fun `svg is self contained escaped and contains no executable element`() {
        val xml = NativeMathSvgRenderer.render("x < y & \\alpha <script>alert(1)</script>").xml
        assertTrue(xml.contains("&lt;"))
        assertTrue(xml.contains("&amp;"))
        assertFalse(xml.contains("<script", ignoreCase = true))
        assertFalse(xml.contains("foreignObject", ignoreCase = true))
        assertFalse(xml.contains("javascript:", ignoreCase = true))
        assertFalse(Regex("(?:href|src)\\s*=", RegexOption.IGNORE_CASE).containsMatchIn(xml))
    }

    @Test
    fun `unknown tex command never leaks as visual code`() {
        val unknown = "definitelyUnknownCommand"
        val parsed = NativeMathParser.parse("\\$unknown") as MathNode.Symbol
        assertEquals("□", parsed.value)
        val xml = NativeMathSvgRenderer.render("\\$unknown").xml
        assertFalse(xml.contains(unknown))
        assertTrue(xml.contains("□"))
    }

    @Test
    fun `reference operators and functions map to visible glyphs`() {
        val node = NativeMathParser.parse(
            "\\varepsilon \\fallingdotseq \\Pi \\prime \\det \\sinh \\rightleftharpoons"
        ) as MathNode.Sequence
        val values = node.children.filterIsInstance<MathNode.Symbol>().map { it.value }
        assertTrue(values.containsAll(listOf("ϵ", "≒", "Π", "′", "det", "sinh", "⇌")))
    }

    @Test
    fun `persian text inside formula keeps rtl svg direction`() {
        val xml = NativeMathSvgRenderer.render("\\text{از مقدار}").xml
        assertTrue(xml.contains("direction=\"rtl\""))
        assertTrue(xml.contains("text-anchor=\"end\""))
        assertFalse(xml.contains("\\text"))
    }

    @Test
    fun `editor svg exposes touch boxes and changes active box color`() {
        val tex = "\\frac{12}{x}"
        val numberStart = tex.indexOf("12")
        val document = NativeMathSvgRenderer.render(
            tex = tex,
            showEditBoxes = true,
            activeStart = numberStart,
            activeEnd = numberStart + 2,
            boxColor = "#445566",
            activeBoxColor = "#FF8800"
        )
        assertTrue(document.editBoxes.size >= 2)
        val active = document.editBoxes.first { it.sourceStart == numberStart && it.sourceEnd == numberStart + 2 }
        assertTrue(active.contains(active.xPx + active.widthPx / 2f, active.yPx + active.heightPx / 2f))
        assertTrue(document.xml.contains("<rect"))
        assertTrue(document.xml.contains("fill=\"#FF8800\""))
        assertTrue(document.xml.contains("fill=\"#445566\""))
    }

    @Test
    fun `radical overbar stretches with every added value`() {
        val short = NativeMathSvgRenderer.render("\\sqrt{1}", showEditBoxes = true)
        val long = NativeMathSvgRenderer.render("\\sqrt{123456789}", showEditBoxes = true)
        val shortBar = short.radicalBars.single()
        val longBar = long.radicalBars.single()
        assertTrue(longBar.endXPx - longBar.startXPx > shortBar.endXPx - shortBar.startXPx)
        assertTrue(long.widthPx > short.widthPx)
        val longValueBox = long.editBoxes.first { it.sourceEnd - it.sourceStart == 9 }
        assertTrue(longBar.endXPx >= longValueBox.xPx + longValueBox.widthPx)
    }

    @Test
    fun `svg cache key is stable and style sensitive`() {
        val first = NativeMathSvgRenderer.render("x^2", 30f, "#112233")
        val same = NativeMathSvgRenderer.render("x^2", 30f, "#112233")
        val changed = NativeMathSvgRenderer.render("x^2", 31f, "#112233")
        assertEquals(first.cacheKey, same.cacheKey)
        assertEquals(first.xml, same.xml)
        assertTrue(first.cacheKey != changed.cacheKey)
    }
}
