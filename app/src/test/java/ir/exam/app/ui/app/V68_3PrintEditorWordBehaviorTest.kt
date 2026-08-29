package ir.exam.app.ui.app

import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import ir.exam.app.ui.builder.StyleSpan
import ir.exam.app.ui.builder.StyleSpanOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V68.3 — رفتار ورد در ویرایشگر چاپ (بازنویسی V68 پس از ریشه‌یابی دو شکست CI):
 *  • مکان‌نمای یک‌لمسی (فوکوس نزدیک‌ترین تکهٔ متنی به نقطهٔ لمس)
 *  • انتخاب بازه‌ای متن + بولد/ایتالیک تکه‌ای (spans) با رفتار toggle ورد
 *  • بازه‌ها با تایپ/حذف جابه‌جا یا بریده می‌شوند (adjust)
 *  • ذخیره/بازیابی spans در codec چاپ
 *  • دستگیره‌های گوشه به‌جای دکمه‌های +/− و زوم دو-انگشتی/دوبار-لمس
 */
class V68_3PrintEditorWordBehaviorTest {

    // ریشهٔ شکست دوم CI: File(path) خام به working directory وابسته است؛
    // تست‌های سبز همیشه root() را با دو نامزد "." و ".." حل می‌کنند.
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()
    private fun editor() = source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt")
    private fun vm() = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")

    // ---------- StyleSpanOps.toggle: رفتار ورد ----------

    @Test
    fun `toggle bold adds then removes full coverage`() {
        // متن خالی از استایل: toggle بولد روی 0..4 → یک بازهٔ بولد.
        val added = StyleSpanOps.toggle(emptyList(), 0, 4, bold = true)
        assertEquals(listOf(StyleSpan(0, 4, true, false)), added)
        // toggle دوباره روی همان بازه: پوشش کامل → حذف محور بولد.
        val removed = StyleSpanOps.toggle(added, 0, 4, bold = true)
        assertTrue(removed.isEmpty())
    }

    @Test
    fun `toggle over partial coverage extends like word`() {
        // بازهٔ موجود 0..4 بولد؛ toggle روی 2..6: پوشش ناقص → کل 0..6 بولد.
        val spans = listOf(StyleSpan(0, 4, true, false))
        val out = StyleSpanOps.toggle(spans, 2, 6, bold = true)
        assertEquals(listOf(StyleSpan(0, 6, true, false)), out)
    }

    @Test
    fun `toggle keeps untouched head and tail`() {
        // بولد 0..8؛ toggle حذف روی 3..5 → دو تکهٔ 0..3 و 5..8 می‌ماند.
        val spans = listOf(StyleSpan(0, 8, true, false))
        val out = StyleSpanOps.toggle(spans, 3, 5, bold = true)
        // سرِ قبل و بعد از بازهٔ انتخابی می‌ماند؛ میان‌تکهٔ بدون استایل حذف می‌شود.
        assertEquals(listOf(StyleSpan(0, 3, true, false), StyleSpan(5, 8, true, false)), out)
    }

    @Test
    fun `toggle italic independent of bold axis`() {
        val bolded = listOf(StyleSpan(0, 4, true, false))
        val out = StyleSpanOps.toggle(bolded, 1, 3, italic = true)
        // بازهٔ بولد دست‌نخورده؛ ایتالیک 1..3 اضافه و هم‌مرزی ادغام نمی‌شود (استایل متفاوت).
        assertEquals(
            listOf(StyleSpan(0, 1, true, false), StyleSpan(1, 3, true, true), StyleSpan(3, 4, true, false)),
            out
        )
    }

    // ---------- StyleSpanOps.adjust: بازه‌ها با تایپ هم‌مقیاس می‌مانند ----------

    @Test
    fun `spans shift with caret-ordered text edits`() {
        // درج «brave » قبل از «world»: بازهٔ world باید 6 کاراکتر جابه‌جا شود.
        val old = "hello world"
        val new = "hello brave world"
        val shifted = StyleSpanOps.adjust(old, new, listOf(StyleSpan(6, 11, true, false)))
        assertEquals(listOf(StyleSpan(12, 17, true, false)), shifted)
    }

    @Test
    fun `overlapping deletion keeps surviving head`() {
        // حذف «cd» (2..4): بازهٔ 0..4 فقط سرِ سالم 0..2 را نگه می‌دارد.
        val old = "abcdefghij"
        val new = "abefghij"
        val out = StyleSpanOps.adjust(old, new, listOf(StyleSpan(0, 4, true, false)))
        assertEquals(listOf(StyleSpan(0, 2, true, false)), out)
    }

    // ---------- StyleSpanOps.splitBySpans: تکه‌های استایل‌دار ----------

    @Test
    fun `splitBySpans cuts styled and plain pieces`() {
        val out = StyleSpanOps.splitBySpans("abcd", 10, listOf(StyleSpan(11, 13, true, false)))
        assertEquals(
            listOf(
                Triple("a", false, false),
                Triple("bc", true, false),
                Triple("d", false, false)
            ),
            out
        )
    }

    @Test
    fun `splitBySpans keeps axes separate`() {
        // بازهٔ فقط-ایتالیک نباید بولد را روشن کند.
        val out = StyleSpanOps.splitBySpans("abcd", 0, listOf(StyleSpan(0, 2, false, true)))
        assertEquals(
            listOf(
                Triple("ab", false, true),
                Triple("cd", false, false)
            ),
            out
        )
    }

    // ---------- codec: ذخیره/بازیابی spans ----------

    @Test
    fun `spans survive encode decode roundtrip`() {
        val draft = QuestionDraft(type = QuestionType.ESSAY, text = "hello world")
            .copy(textSpans = listOf(StyleSpan(6, 11, true, false)))
        val encoded = ExamQuestionCodec.encode(listOf(draft))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey)
        assertEquals(1, decoded.size)
        assertEquals(listOf(StyleSpan(6, 11, true, false)), decoded.single().textSpans)
    }

    @Test
    fun `legacy questions without spans decode to empty list`() {
        val draft = QuestionDraft(type = QuestionType.ESSAY, text = "بدون استایل")
        val encoded = ExamQuestionCodec.encode(listOf(draft))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey)
        assertTrue(decoded.single().textSpans.isEmpty())
    }

    // ---------- قرارداد صفحهٔ ویرایشگر (رفتار ورد) ----------

    @Test
    fun `one tap places caret and keyboard never hides it`() {
        val editor = editor()
        // یک‌لمسی: لمس با موقعیت، نزدیک‌ترین تکهٔ متنی فوکوس می‌شود.
        assertTrue("detectTapGestures(onTap = { pos ->" in editor)
        assertTrue("focusRequester(requester)" in editor)
        assertTrue("androidx.compose.ui.text.input.TextFieldValue(part.text)" in editor)
        // انتخاب بازه‌ای با هایلایت خودِ BasicTextField.
        assertTrue("onTextRangeChange" in editor)
        // کیبورد مکان‌نا را نمی‌پوشاند.
        assertTrue(".imePadding()" in editor)
        // لمس سطح سؤال دیگر clickable ساده نیست.
        assertTrue(".clickable(onClick = onSelect)" !in editor)
    }

    @Test
    fun `range selection drives toolbar bold and italic`() {
        val editor = editor()
        assertTrue("StyleSpanOps.toggle" in editor)
        assertTrue("builder.setQuestionSpans(" in editor)
        // حذف کامل متن بازه را باطل می‌کند (نه ادعای بازهٔ قدیمی).
        assertTrue(vm().contains("StyleSpanOps.adjust"))
    }

    @Test
    fun `corner handles replace toolbar plus minus for objects`() {
        val editor = editor()
        assertTrue("private fun BoxScope.ObjectCornerHandles(" in editor)
        assertTrue("detectDragGestures(" in editor)
        assertTrue("onObjectGrow" !in editor)
        assertTrue("onObjectShrink" !in editor)
        assertTrue("بزرگ‌کردن شیء" !in editor)
    }

    @Test
    fun `pinch zooms and double tap resets like word`() {
        val editor = editor()
        assertTrue("rememberTransformableState" in editor)
        assertTrue("transformable(zoomState)" in editor)
        assertTrue("detectTapGestures(onDoubleTap = { onResetZoom() })" in editor)
    }

    @Test
    fun `range styling renders in display mode too`() {
        val editor = editor()
        // حالت غیر ویرایش هم استایل تکه‌ای را نشان می‌دهد.
        assertTrue("question.textSpans.isNotEmpty()" in editor)
        assertTrue("StyleSpanOps.splitBySpans(part.text, segRange.first, question.textSpans)" in editor)
    }
}
