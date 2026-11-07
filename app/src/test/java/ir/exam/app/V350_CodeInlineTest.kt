package ir.exam.app

import ir.exam.app.core.speech.ReadAloudText
import ir.exam.app.core.text.CodeBlockCodec
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import ir.exam.app.ui.code.CodeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V350 — کد به‌عنوان شیء داخل متن سؤال و گزینه‌ها.
 *
 * این آزمون منطق خالص را می‌سنجد: پیدا کردن بازهٔ بلوک، درج/ویرایش/حذف،
 * هم‌زیستی با فرمول و شکل، و هم‌ارزی با قرارداد سایت.
 */
class V350_CodeInlineTest {

    @Test
    fun occurrencesFindRangeAndLanguage() {
        val src = "قبل\n```python\nprint(1)\n```\nبعد"
        val occ = CodeBlockCodec.occurrences(src)
        assertEquals(1, occ.size)
        assertEquals("python", occ[0].lang)
        assertEquals("print(1)", occ[0].code)
        assertEquals(src.indexOf("```"), occ[0].start)
        assertEquals(src.indexOf("```", 4) + 3, occ[0].endExclusive)
        assertEquals("```python\nprint(1)\n```", src.substring(occ[0].start, occ[0].endExclusive))
    }

    @Test
    fun severalBlocksAreIndexedInOrder() {
        val src = "```python\na\n```\nوسط\n```sql\nSELECT 1;\n```"
        val occ = CodeBlockCodec.occurrences(src)
        assertEquals(2, occ.size)
        assertEquals(listOf(0, 1), occ.map { it.index })
        assertEquals(listOf("python", "sql"), occ.map { it.lang })
        assertTrue(occ[1].start > occ[0].endExclusive)
    }

    @Test
    fun plainTextHasNoBlock() {
        assertTrue(CodeBlockCodec.occurrences("متن معمولی بدون کد").isEmpty())
        assertTrue(CodeBlockCodec.occurrences("").isEmpty())
    }

    @Test
    fun tokenMatchesTheSiteContract() {
        // همان قرارداد سایت و CodeText: زبان، خط تازه، بدنه، خط تازه، بسته
        assertEquals("```python\nprint(1)\n```", CodeBlockCodec.token("print(1)", "python"))
        assertEquals(CodeText.fence("print(1)", "python"), CodeBlockCodec.token("print(1)", "python"))
    }

    @Test
    fun innerBackticksDoNotBreakTheBlock() {
        val tricky = "let s = \"```\";"
        val token = CodeBlockCodec.token(tricky, "javascript")
        val occ = CodeBlockCodec.occurrences(token)
        assertEquals(1, occ.size)
        assertEquals(tricky, occ[0].code)
        assertEquals(CodeText.parseFence(token)?.code, occ[0].code)
    }

    @Test
    fun blankLanguageFallsBackToPython() {
        assertEquals("```python\nx\n```", CodeBlockCodec.token("x", ""))
        assertEquals("```python\nx\n```", CodeBlockCodec.token("x", "   "))
    }

    @Test
    fun insertAtSeparatesBlockWithBlankLine() {
        val out = CodeBlockCodec.insertAt("سؤال", "print(1)", "python", 4)
        assertTrue(out.startsWith("سؤال\n\n"))
        assertTrue(out.contains("```python\nprint(1)\n```"))
        assertEquals(1, CodeBlockCodec.occurrences(out).size)
    }

    @Test
    fun insertIntoEmptyTextAddsNoLeadingBlankLine() {
        val out = CodeBlockCodec.insertAt("", "x = 1", "python", 0)
        assertTrue(out.startsWith("```python"))
        assertFalse(out.startsWith("\n"))
    }

    @Test
    fun replaceKeepsSurroundingText() {
        val src = "قبل\n```python\nold()\n```\nبعد"
        val occ = CodeBlockCodec.occurrences(src)[0]
        val out = CodeBlockCodec.replace(src, occ, "new()", "python")
        assertTrue(out.startsWith("قبل\n"))
        assertTrue(out.endsWith("\nبعد"))
        assertTrue(out.contains("new()"))
        assertFalse(out.contains("old()"))
        assertEquals(1, CodeBlockCodec.occurrences(out).size)
    }

    @Test
    fun replaceCanChangeLanguageInPlace() {
        val src = "```python\nx\n```"
        val occ = CodeBlockCodec.occurrences(src)[0]
        val out = CodeBlockCodec.replace(src, occ, "x", "sql")
        assertEquals("```sql\nx\n```", out)
    }

    @Test
    fun removeDropsBlockAndItsExtraNewline() {
        val src = "قبل\n```python\nx\n```\nبعد"
        val occ = CodeBlockCodec.occurrences(src)[0]
        val out = CodeBlockCodec.remove(src, occ)
        assertEquals("قبل\nبعد", out)
        assertTrue(CodeBlockCodec.occurrences(out).isEmpty())
    }

    @Test
    fun removeOnlyTouchesTheChosenBlock() {
        val src = "```python\na\n```\nوسط\n```sql\nb\n```"
        val out = CodeBlockCodec.remove(src, CodeBlockCodec.occurrences(src)[0])
        val left = CodeBlockCodec.occurrences(out)
        assertEquals(1, left.size)
        assertEquals("sql", left[0].lang)
        assertTrue(out.contains("وسط"))
    }

    @Test
    fun badOccurrenceLeavesTextUntouched() {
        val src = "متن"
        val bad = ir.exam.app.core.text.CodeOccurrence(0, -5, 99, "python", "x")
        assertEquals(src, CodeBlockCodec.replace(src, bad, "y", "python"))
        assertEquals(src, CodeBlockCodec.remove(src, bad))
    }

    // ---------- متن غنی: کد در کنار فرمول و شکل ----------

    @Test
    fun splitterMakesCodeASegment() {
        val src = "سؤال\n```python\nprint(1)\n```\nپایان"
        val parts = RichTextSplitter.split(src)
        val code = parts.filterIsInstance<RichSegment.Code>()
        assertEquals(1, code.size)
        assertEquals("python", code[0].lang)
        assertEquals("print(1)", code[0].code)
        assertTrue(parts.first() is RichSegment.Text)
        assertTrue(parts.last() is RichSegment.Text)
    }

    @Test
    fun dollarInsideCodeDoesNotBecomeFormula() {
        val src = "```python\ns = \"\$x\$\"\n```"
        val parts = RichTextSplitter.split(src)
        assertTrue("فرمول نباید ساخته شود", parts.none { it is RichSegment.Math })
        assertEquals(1, parts.filterIsInstance<RichSegment.Code>().size)
    }

    @Test
    fun formulaOutsideCodeStillWorks() {
        val src = "قبل \$x^2\$ وسط\n```python\nprint(1)\n```\nبعد \$y_1\$ پایان"
        val parts = RichTextSplitter.split(src)
        assertEquals(2, parts.filterIsInstance<RichSegment.Math>().size)
        assertEquals(1, parts.filterIsInstance<RichSegment.Code>().size)
    }

    @Test
    fun codeGetsItsOwnRowLikeFigure() {
        val src = "سؤال\n```python\nprint(1)\n```\nپایان"
        val rows = RichTextSplitter.splitRows(src)
        val codeRows = rows.filter { it.singleOrNull() is RichSegment.Code }
        assertEquals(1, codeRows.size)
    }

    @Test
    fun reconstructPreservesCodeBlock() {
        val src = "قبل\n```python\nprint(1)\n```\nبعد"
        val parts = RichTextSplitter.split(src)
        val textIndex = parts.indexOfFirst { it is RichSegment.Text && (it as RichSegment.Text).text.contains("قبل") }
        val out = RichTextSplitter.reconstruct(parts, textIndex, "پیش")
        assertTrue(out.contains("```python\nprint(1)\n```"))
        assertTrue(out.contains("پیش"))
        assertFalse(out.contains("قبل"))
    }

    @Test
    fun segmentRangesMatchTheRawTextForCode() {
        val src = "قبل \$x\$ وسط\n```python\nprint(1)\n```\nبعد"
        val formulas = ir.exam.app.core.math.FormulaTextCodec.occurrences(src)
        val figures = ir.exam.app.core.figure.FigureCodec.occurrences(src)
        val codes = CodeBlockCodec.occurrences(src)
        val parts = RichTextSplitter.split(src, formulas, figures, codes)
        val ranges = RichTextSplitter.segmentSourceRanges(parts, formulas, figures, codes)
        assertEquals(parts.size, ranges.size)
        parts.forEachIndexed { i, seg ->
            if (seg is RichSegment.Code) {
                assertEquals(codes[seg.index].start, ranges[i].first)
                assertEquals(codes[seg.index].endExclusive, ranges[i].last + 1)
                assertEquals(
                    src.substring(ranges[i].first, ranges[i].last + 1),
                    CodeBlockCodec.token(seg.code, seg.lang)
                )
            }
        }
    }

    @Test
    fun codeIsNotReadAloud() {
        val spoken = ReadAloudText.plain("سؤال\n```python\nprint(1)\n```\nپایان")
        assertFalse("کد بلند خوانده نمی‌شود", spoken.contains("print"))
        assertFalse(spoken.contains("```"))
        assertTrue(spoken.contains("بلوک کد"))
        assertTrue(spoken.contains("سؤال"))
    }

    @Test
    fun optionTextUsesTheSameContract() {
        // گزینه‌ها همان رشتهٔ خام را نگه می‌دارند؛ هیچ فیلد تازه‌ای لازم نیست
        val option = "```python\nx = 5\n```"
        val parts = RichTextSplitter.split(option)
        val code = parts.filterIsInstance<RichSegment.Code>().single()
        assertEquals("x = 5", code.code)
        val out = RichTextSplitter.reconstruct(parts, -1, "")
        assertEquals(option, out)
    }

    @Test
    fun roundTripThroughInsertEditDeleteRestoresText() {
        val base = "متن سؤال"
        val inserted = CodeBlockCodec.insertAt(base, "a()", "python", base.length)
        val occ = CodeBlockCodec.occurrences(inserted).single()
        val edited = CodeBlockCodec.replace(inserted, occ, "b()", "sql")
        assertEquals("```sql\nb()\n```", CodeBlockCodec.occurrences(edited).single().let {
            edited.substring(it.start, it.endExclusive)
        })
        val removed = CodeBlockCodec.remove(edited, CodeBlockCodec.occurrences(edited).single())
        assertEquals(base, removed.trimEnd())
    }

    @Test
    fun parityWithCodeTextOnALanguageCorpus() {
        val ids = ir.exam.app.ui.code.CodeLanguages.all.map { it.id }
        ids.forEach { id ->
            val body = "x = 1"
            val mine = CodeBlockCodec.token(body, id)
            val theirs = CodeText.fence(body, id)
            assertEquals("قالب زبان $id", theirs, mine)
            val parsed = CodeText.parseFence(mine)
            assertNotNull(parsed)
            assertEquals(id, parsed!!.lang)
            assertEquals(body, parsed.code)
        }
    }
}
