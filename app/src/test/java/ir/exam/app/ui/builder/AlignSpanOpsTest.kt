package ir.exam.app.ui.builder

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * V121 — منطقِ خالصِ AlignSpanOps (تراز پاراگرافیِ تکه‌ای متنِ سؤال).
 * جدا از StyleSpan/StyleSpanOps چون تراز یک ویژگیِ بلوک/پاراگراف است، نه
 * درون‌سطری (رجوع کنید به توضیحِ AlignSpan در QuestionDraft.kt).
 */
class AlignSpanOpsTest {

    @Test
    fun `setAlign splits overlapping spans and inserts the new one`() {
        val spans = listOf(AlignSpan(0, 20, "center"))
        val result = AlignSpanOps.setAlign(spans, 5, 10, "left")
        assertEquals(
            listOf(AlignSpan(0, 5, "center"), AlignSpan(5, 10, "left"), AlignSpan(10, 20, "center")),
            result
        )
    }

    @Test
    fun `effectiveAlign returns fallback when no span overlaps the paragraph`() {
        val spans = listOf(AlignSpan(0, 5, "center"))
        assertEquals("right", AlignSpanOps.effectiveAlign(spans, 10, 15, "right"))
    }

    @Test
    fun `effectiveAlign returns the last overlapping span's align`() {
        val spans = listOf(AlignSpan(0, 10, "center"), AlignSpan(5, 8, "justify"))
        // پاراگرافِ [0,10) با هر دو بازه همپوشانی دارد؛ آخرین‌بار (justify) برنده است.
        assertEquals("justify", AlignSpanOps.effectiveAlign(spans, 0, 10, "right"))
    }

    @Test
    fun `expandToParagraphs grows the selection to full paragraph boundaries`() {
        val text = "خط اول\nخط دوم که طولانی‌تر است\nخط سوم"
        val secondLineStart = text.indexOf("خط دوم")
        val midOfSecondLine = secondLineStart + 3
        val (start, end) = AlignSpanOps.expandToParagraphs(text, midOfSecondLine, midOfSecondLine + 1)
        assertEquals(secondLineStart, start)
        assertEquals(text.indexOf("\n", secondLineStart), end)
    }

    @Test
    fun `adjust shifts spans after an insertion and trims spans inside a deletion`() {
        val spans = listOf(AlignSpan(5, 10, "center"))
        // درج ۳ نویسه در ابتدای متن: بازه باید ۳ واحد جابه‌جا شود.
        val afterInsert = AlignSpanOps.adjust("0123456789", "ABC0123456789", spans)
        assertEquals(listOf(AlignSpan(8, 13, "center")), afterInsert)
    }

    @Test
    fun `adjust drops spans fully covered by a deleted range from string diff`() {
        val spans = listOf(AlignSpan(5, 10, "center"))
        // حذفِ کاملِ ناحیهٔ [5,10): چون بازه با دیف هم‌پوشان است اما دو سرش
        // بیرون از پیشوند/پسوندِ مشترک نمی‌مانند، عملاً حذف می‌شود.
        val afterDelete = AlignSpanOps.adjust("0123456789", "01234", spans)
        assertEquals(emptyList<AlignSpan>(), afterDelete)
    }
}
