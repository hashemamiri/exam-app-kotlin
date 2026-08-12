package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Test

class FormulaTextCodecTest {
    @Test fun `finds edits and deletes existing inline formulas without manual selection`() {
        val source = "متن ${'$'}x^2${'$'} و ${'$'}\\frac{1}{2}${'$'} پایان"
        val formulas = FormulaTextCodec.occurrences(source)
        assertEquals(listOf("x^2", "\\frac{1}{2}"), formulas.map(FormulaOccurrence::tex))
        val edited = FormulaTextCodec.upsert(source, 0, "x^3")
        assertEquals(listOf("x^3", "\\frac{1}{2}"), FormulaTextCodec.occurrences(edited).map(FormulaOccurrence::tex))
        val deleted = FormulaTextCodec.delete(edited, 1)
        assertEquals(listOf("x^3"), FormulaTextCodec.occurrences(deleted).map(FormulaOccurrence::tex))
    }

    @Test fun `appends a new formula and ignores doubled literal dollar`() {
        assertEquals("سؤال ${'$'}x+1${'$'}", FormulaTextCodec.upsert("سؤال", null, "x+1"))
        assertEquals(emptyList<FormulaOccurrence>(), FormulaTextCodec.occurrences("قیمت ${'$'}${'$'} 20"))
    }
}
