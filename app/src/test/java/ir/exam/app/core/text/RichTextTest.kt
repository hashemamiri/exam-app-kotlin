package ir.exam.app.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {
    @Test
    fun `text slots remain around an inline formula for continued typing`() {
        val source = "قبل ${'$'}x${'$'}"
        val parts = RichTextSplitter.split(source)

        assertEquals(3, parts.size)
        assertTrue(parts[0] == RichSegment.Text("قبل "))
        assertTrue(parts[1] == RichSegment.Math(0, "x"))
        assertTrue(parts[2] == RichSegment.Text(""))

        val continued = RichTextSplitter.reconstruct(parts, editedIndex = 2, newText = "بعد")
        assertEquals("قبل ${'$'}x${'$'}بعد", continued)
    }

    @Test
    fun `an empty source still exposes one editable text slot`() {
        assertEquals(listOf<RichSegment>(RichSegment.Text("")), RichTextSplitter.split(""))
    }
}
