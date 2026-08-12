package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaBoxEditorTest {
    @Test
    fun `library replaces the active merged value box instead of appending code`() {
        val current = "\\frac{123}{b}"
        val cursorAfterNumber = current.indexOf("123") + 3
        val result = FormulaBoxEditor.insert(
            current = current,
            selectionStart = cursorAfterNumber,
            selectionEnd = cursorAfterNumber,
            insertion = "\\alpha",
            activateFirstInsertedBox = true,
            replaceActiveBoxWhenCollapsed = true
        )
        assertEquals("\\frac{\\alpha}{b}", result.text)
        assertEquals("\\alpha", result.text.substring(result.selectionStart, result.selectionEnd))
    }

    @Test
    fun `template activates first box and keypad typing replaces only that box`() {
        val template = FormulaBoxEditor.insert(
            current = "",
            selectionStart = 0,
            selectionEnd = 0,
            insertion = "\\sqrt{x}",
            activateFirstInsertedBox = true,
            replaceActiveBoxWhenCollapsed = true
        )
        assertEquals("x", template.text.substring(template.selectionStart, template.selectionEnd))

        val typed = FormulaBoxEditor.insert(
            current = template.text,
            selectionStart = template.selectionStart,
            selectionEnd = template.selectionEnd,
            insertion = "25"
        )
        assertEquals("\\sqrt{25}", typed.text)
        assertEquals(typed.text.indexOf("25") + 2, typed.selectionEnd)
    }

    @Test
    fun `empty fraction group remains a selectable input box`() {
        val text = "\\frac{}{b}"
        val ranges = NativeMathParser.editableRanges(text)
        val emptyPosition = text.indexOf("}")
        assertTrue(ranges.any { it.start == emptyPosition && it.endExclusive == emptyPosition })
        val active = FormulaBoxEditor.activeRange(text, emptyPosition, emptyPosition)
        assertEquals(MathSourceRange(emptyPosition, emptyPosition), active)
    }

    @Test
    fun `partial selection inside a tex command expands to its safe visual box`() {
        val text = "\\alpha"
        assertEquals(MathSourceRange(0, text.length), FormulaBoxEditor.activeRange(text, 2, 4))
    }

    @Test
    fun `arrow navigation moves between boxes without entering tex command bytes`() {
        val text = "\\frac{12}{\\alpha}"
        val first = NativeMathParser.editableRanges(text).first()
        val moved = FormulaBoxEditor.moveActiveBox(text, first.start, first.endExclusive, 1)
        assertEquals("\\alpha", text.substring(moved.selectionStart, moved.selectionEnd))
    }

    @Test
    fun `structural typing slash scripts multiplication and arrows match reference`() {
        var edit = FormulaBoxEditor.replaceAll("12")
        edit = FormulaBoxEditor.typeCharacter(edit.text, 2, 2, "/")
        assertEquals("\\frac{12}{}", edit.text)
        edit = FormulaBoxEditor.typeCharacter(edit.text, edit.selectionStart, edit.selectionEnd, "3")
        assertEquals("\\frac{12}{3}", edit.text)
        val power = FormulaBoxEditor.typeCharacter("x", 1, 1, "^")
        assertEquals("x^{}", power.text)
        val multiply = FormulaBoxEditor.typeCharacter("a", 1, 1, "*")
        assertEquals("a\\times ", multiply.text)
        val arrow = FormulaBoxEditor.typeCharacter("x-", 2, 2, ">")
        assertEquals("x\\to ", arrow.text)
    }

    @Test
    fun `spatial navigation moves between fraction numerator and denominator`() {
        val text = "\\frac{a}{b}"
        val ranges = NativeMathParser.editableRanges(text)
        val moved = FormulaBoxEditor.moveSpatialBox(text, ranges.first().start, ranges.first().endExclusive, 1)
        assertEquals("b", text.substring(moved.selectionStart, moved.selectionEnd))
    }

    @Test
    fun `paste imports tex or converts natural input`() {
        assertEquals("\\frac{7}{8}", FormulaBoxEditor.importText("7/8").text)
        assertEquals("\\sqrt{x}", FormulaBoxEditor.importText("${'$'}\\sqrt{x}${'$'}").text)
    }

    @Test
    fun `plain adjacent digits form one editable box`() {
        val ranges = NativeMathParser.editableRanges("123+45")
        assertEquals(
            listOf(MathSourceRange(0, 3), MathSourceRange(3, 4), MathSourceRange(4, 6)),
            ranges
        )
    }
}
