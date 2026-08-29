package ir.exam.app.ui.app

import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.math.FormulaTextCodec
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V67.0 — چهار قرارداد کادر متن سؤال در آزمون‌ساز:
 * ۱) مکان‌نما بدون لمس قبلی پیدا باشد (فوکوس خودکار بخش متنی).
 * ۲) درج شکل/فرمول در محل مکان‌نما رخ دهد و ترتیب متن/شیء/ادامهٔ متن حفظ شود.
 * ۳) فرمول‌ها با اندازهٔ طبیعی رندر شوند؛ بزرگ‌ها اسکرول افقی بگیرند نه کوچک‌شدن.
 * ۴) لمس شیء = انتخاب با × حذف؛ لمس دوم = ویرایشگر همان شیء.
 */
class V67_0QuestionFieldCaretOrderTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val section by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }
    private val rich by lazy { source("app/src/main/java/ir/exam/app/core/text/RichText.kt") }
    private val figure by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureSpec.kt") }

    // ---------- اجرایی JVM: بازهٔ آفست بخش‌ها دقیقاً زیررشتهٔ متن خام ----------

    @Test
    fun `segment source ranges match raw substrings`() {
        val spec = FigureSpec.build("tri")
        val source = "با توجه به \$x^2\$ و شکل %%FIG:${spec.toJson()}%% ادامه دهید"
        val formulas = FormulaTextCodec.occurrences(source)
        val figures = FigureCodec.occurrences(source)
        val parts = RichTextSplitter.split(source, formulas, figures)
        val ranges = RichTextSplitter.segmentSourceRanges(parts, formulas, figures)
        assertEquals(parts.size, ranges.size)
        parts.forEachIndexed { i, seg ->
            when (seg) {
                is RichSegment.Text ->
                    assertTrue(
                        "text part $i range mismatch",
                        seg.text == source.substring(ranges[i].first, ranges[i].last + 1)
                    )
                is RichSegment.Math -> {
                    val occ = formulas[seg.index]
                    assertTrue("math start", occ.start == ranges[i].first)
                    assertTrue("math end", occ.endExclusive == ranges[i].last + 1)
                }
                is RichSegment.Figure -> {
                    val occ = figures[seg.index]
                    assertTrue("figure start", occ.start == ranges[i].first)
                    assertTrue("figure end", occ.endExclusive == ranges[i].last + 1)
                }
            }
        }
    }

    // ---------- اجرایی JVM: درج در محل مکان‌نا ترتیب را حفظ می‌کند ----------

    @Test
    fun `insertAt places token at caret and keeps order`() {
        val spec = FigureSpec.build("tri")
        val token = FigureCodec.token(spec)
        val text = "با توجه به شکل پاسخ دهید"
        val caret = "با توجه به شکل".length
        val result = FigureCodec.insertAt(text, spec, caret)
        assertEquals("با توجه به شکل $token پاسخ دهید", result)
        // ترتیب بخش‌ها: متن اول، شکل، ادامهٔ متن.
        val parts = RichTextSplitter.split(result)
        val figAt = parts.indexOfFirst { it is RichSegment.Figure }
        assertTrue("figure missing", figAt > 0)
        assertTrue("before figure is text", parts[figAt - 1] is RichSegment.Text)
        assertTrue("after figure is text", parts[figAt + 1] is RichSegment.Text)
        assertTrue(
            "leading text preserved",
            (parts[figAt - 1] as RichSegment.Text).text.contains("با توجه")
        )
        assertTrue(
            "trailing text preserved",
            (parts[figAt + 1] as RichSegment.Text).text.contains("پاسخ")
        )
    }

    @Test
    fun `insertAt blank and boundary offsets are safe`() {
        val spec = FigureSpec.build("tri")
        val token = FigureCodec.token(spec)
        assertEquals(token, FigureCodec.insertAt("", spec, 0))
        assertEquals("$token سلام", FigureCodec.insertAt("سلام", spec, 0))
        assertEquals("سلام $token", FigureCodec.insertAt("سلام", spec, 4))
        // آفست خارج از بازه به‌صورت امن قلاب می‌شود.
        assertTrue(FigureCodec.insertAt("سلام", spec, 99).endsWith(token))
        // متن خالی همیشه یک بخش متنی قابل تایپ دارد تا مکان‌نا جا داشته باشد.
        assertEquals(listOf<RichSegment>(RichSegment.Text("")), RichTextSplitter.split(""))
    }

    // ---------- قرارداد UI: مکان‌نا، اندازهٔ طبیعی فرمول، انتخاب/حذف ----------

    @Test
    fun `question field auto focuses caret and tracks focused segment`() {
        assertTrue("FocusRequester()" in section)
        assertTrue("requestFocus()" in section)
        assertTrue("onFocusChanged" in section)
        assertTrue("focusedTextSegment" in section)
        assertTrue("focusAtOffset" in section)
    }

    @Test
    fun `formulas render at natural size with scroll for big ones`() {
        assertTrue("NativeFormulaView(" in section)
        assertTrue("NativeMathSvgRenderer.render" in section)
        assertTrue("fun segmentSourceRanges(" in rich)
        assertTrue("fun insertAt(" in figure)
        assertTrue("fun token(" in figure)
        // جعبهٔ ثابت ۸۴×۳۶ که فرمول را کوچک می‌کرد دیگر وجود ندارد.
        assertFalse("size(84.dp, 36.dp)" in section)
    }

    @Test
    fun `tokens select on first tap with close delete and edit on second tap`() {
        assertTrue("selectedPartIndex" in section)
        assertTrue("TokenCloseButton(" in section)
        assertTrue("Icons.Outlined.Close" in section)
        assertTrue("FormulaTextCodec.delete" in section)
        assertTrue("FigureCodec.delete" in section)
        // درج در محل مکان‌نا، نه فقط انتهای متن.
        assertTrue("FigureCodec.insertAt(" in section)
    }
}
