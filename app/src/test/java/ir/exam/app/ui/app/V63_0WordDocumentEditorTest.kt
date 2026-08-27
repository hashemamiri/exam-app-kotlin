package ir.exam.app.ui.app

import ir.exam.app.core.printing.WordPageLayout
import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V63.0 — پچ ۱ نقشهٔ «ویرایشگر سند Word-مانند»:
 * ۱) در منوی همبرگری → «چاپ آزمون»، روی کارت هر آزمون یک آیکن مداد برای
 *    ویرایش آزمون هست.
 * ۲) لمس مداد صفحهٔ جدید «ویرایشگر سند» را باز می‌کند؛ این صفحه عمداً صفحهٔ
 *    «ایجاد آزمون» نیست: همهٔ سؤال‌ها پشت‌سرهم، در اندازهٔ واقعی A4 و با
 *    صفحه‌بندی خودکار چیده می‌شوند و روی کارت هر سؤال یک مداد است.
 *
 * بخش دوم این فایل تست «اجرایی» موتور صفحه‌بندی است (WordPageLayout عمداً
 * بدون وابستگی اندروید نوشته شده تا همین‌جا روی JVM اجرا شود).
 */
class V63_0WordDocumentEditorTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val printCenter by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val layout by lazy { source("app/src/main/java/ir/exam/app/core/printing/WordPageLayout.kt") }

    // ---- ۱) قراردادهای منبع ----

    @Test
    fun `print center exam card has an edit pencil that opens the document editor`() {
        assertTrue(
            "fun ExamPrintCenterScreen(\n    onEditExamDocument: (String) -> Unit" in printCenter ||
                "onEditExamDocument: (String) -> Unit" in printCenter
        )
        assertTrue("Icons.Outlined.Edit" in printCenter)
        assertTrue("ویرایش آزمون" in printCenter)
        assertTrue("onEditExamDocument(exam.id)" in printCenter)
        // چاپ‌های V62.7 دست‌نخورده باقی می‌مانند
        assertTrue("Text(\"چاپ برگه\")" in printCenter)
        assertTrue("Text(\"چاپ با کلید\")" in printCenter)
    }

    @Test
    fun `app shell routes the pencil to the new document editor page`() {
        assertTrue("DOC_EDITOR" in appShell)
        assertTrue("editingDocumentExamId" in appShell)
        assertTrue("ExamDocumentEditorScreen(" in appShell)
        assertTrue("onEditExamDocument = { examId ->" in appShell)
        // ویرایشگر سند به‌جای صفحهٔ ایجاد آزمون، به صفحهٔ چاپ برمی‌گردد
        assertTrue("MainPage.DOC_EDITOR -> \"ویرایش آزمون\"" in appShell)
        assertTrue("ExamBuilderViewModel(appContext, editingDocumentExamId)" in appShell)
    }

    @Test
    fun `document editor is a word-like paged view with a pencil on every question card`() {
        assertTrue("fun ExamDocumentEditorScreen(" in editor)
        // صفحه‌بندی واقعی A4 از موتور مشترک
        assertTrue("WordPageLayout.documentOf(state.questions)" in editor)
        assertTrue("WordPageLayout.PAGE_WIDTH_MM" in editor)
        assertTrue("WordPageLayout.PAGE_HEIGHT_MM" in editor)
        // V63.4 — مداد هر سؤال حذف شد؛ ویرایش درجا با انتخاب سؤال.
        assertTrue("BasicTextField(" in editor)
        assertTrue("editable = editingQuestionId == question.id" in editor)
        // این صفحه سازندهٔ آزمون نیست: هیچ پردهٔ سازنده اینجا نیست
        assertTrue("ExamBuilderScreen(" !in editor)
        // شمارهٔ صفحه در پاصفحهٔ سند
        assertTrue("صفحهٔ \${page.number} از \$pageCount" in editor)
    }

    @Test
    fun `layout engine is real a4 and android-free`() {
        assertTrue("const val PAGE_WIDTH_MM: Float = 210f" in layout)
        assertTrue("const val PAGE_HEIGHT_MM: Float = 297f" in layout)
        assertTrue("fun paginate(blocks: List<WordBlock>" in layout)
        assertTrue("fun questionHeightMm(question: QuestionDraft): Float" in layout)
        // بدون وابستگی اندرویدی تا تست JVM ممکن بماند
        assertTrue("android." !in layout)
        assertTrue("androidx." !in layout)
    }

    // ---- ۲) تست‌های اجرایی موتور صفحه‌بندی ----

    @Test
    fun `empty exam has no page and every question becomes one block`() {
        assertEquals(0, WordPageLayout.documentOf(emptyList()).pageCount)
        val questions = listOf(essay("q1"), essay("q2"), essay("q3"))
        val document = WordPageLayout.documentOf(questions)
        assertEquals(3, document.blockCount)
        assertEquals(listOf(1, 2, 3), document.pages.flatMap { it.blocks }.map { it.row })
        assertEquals(1, document.pageOf("q1"))
    }

    @Test
    fun `no page exceeds usable content height unless a single block is bigger`() {
        val questions = (1..25).map { essay("q$it", text = "سؤال شمارهٔ $it با متن بلند ".repeat(12)) }
        val document = WordPageLayout.documentOf(questions)
        assertTrue("باید بیش از یک صفحه شود", document.pageCount > 1)
        document.pages.forEach { page ->
            val blocks = page.blocks
            val used = blocks.sumOf { it.heightMm.toDouble() }.toFloat() +
                (blocks.size - 1) * WordPageLayout.BLOCK_GAP_MM
            assertTrue(
                "صفحهٔ ${page.number} از ارتفاع مفید گذشت: $used",
                used <= WordPageLayout.CONTENT_HEIGHT_MM || blocks.size == 1
            )
        }
        assertEquals((1..document.pageCount).toList(), document.pages.map { it.number })
    }

    @Test
    fun `an oversized question occupies its own page instead of being cut`() {
        val huge = essay("big", text = "خط بلند ".repeat(400))
        val document = WordPageLayout.documentOf(listOf(essay("small"), huge, essay("small2")))
        val bigPage = document.pages.first { page -> page.blocks.any { it.questionId == "big" } }
        assertEquals(1, bigPage.blocks.size)
        assertTrue(bigPage.blocks.first().heightMm > WordPageLayout.CONTENT_HEIGHT_MM)
    }

    @Test
    fun `text height counts teacher line breaks and figure rows`() {
        val oneLine = WordPageLayout.textHeightMm("سلام", 16f)
        val threeLines = WordPageLayout.textHeightMm("سلام\nسلام\nسلام", 16f)
        assertEquals(oneLine * 3f, threeLines, 0.001f)
        assertEquals(1, WordPageLayout.lineCount("", 16f))

        val figureToken = "%%FIG:{\"t\":\"tri\"}%%"
        assertEquals(1, WordPageLayout.figureCount(figureToken))
        // توکن شکل به‌اندازهٔ طول خامش (۲۰ نویسه) شمرده نمی‌شود
        assertTrue(WordPageLayout.visibleLength(figureToken) < figureToken.length)
        val withFigure = WordPageLayout.questionHeightMm(essay("f", text = figureToken))
        val withoutFigure = WordPageLayout.questionHeightMm(essay("f"))
        // V63.1 — بدون wmm ارتفاع همان 42mm قبلی است (عرض پیش‌فرض 95mm).
        assertEquals(WordPageLayout.FIGURE_BLOCK_HEIGHT_MM, withFigure - withoutFigure, 0.5f)
    }

    @Test
    fun `options matching media and answer lines all add real height`() {
        val base = essay("base")
        val choice = base.copy(
            type = QuestionType.MULTIPLE_CHOICE,
            options = listOf("گزینهٔ یک", "گزینهٔ دو", "گزینهٔ سه", "گزینهٔ چهار")
        )
        assertTrue(WordPageLayout.questionHeightMm(choice) > WordPageLayout.questionHeightMm(base))

        val matching = base.copy(
            type = QuestionType.MATCHING,
            matchingLeft = listOf("الف", "ب", "پ"),
            matchingRight = listOf("۱", "۲", "۳")
        )
        assertTrue(WordPageLayout.questionHeightMm(matching) > WordPageLayout.questionHeightMm(base))

        val withImage = base.copy(images = listOf(MediaDraft(uri = "file:///tmp/a.png", widthMm = 60f)))
        assertTrue(WordPageLayout.questionHeightMm(withImage) > WordPageLayout.questionHeightMm(base))

        val withAnswerLines = base.copy(answerLines = 5)
        // پایه از قبل ۲ خط پاسخ دارد؛ پس تفاوت باید دقیقاً ۳ خط باشد.
        val expected = WordPageLayout.questionHeightMm(base) +
            (5 - base.answerLines) * WordPageLayout.ANSWER_LINE_HEIGHT_MM
        assertEquals(expected, WordPageLayout.questionHeightMm(withAnswerLines), 0.001f)

        // هیچ سؤالی کمتر از کف ارتفاع نیست
        assertTrue(WordPageLayout.questionHeightMm(base.copy(text = "")) >= WordPageLayout.MIN_BLOCK_HEIGHT_MM)
    }

    @Test
    fun `bigger font produces fewer characters per line and taller lines`() {
        assertTrue(WordPageLayout.charsPerLine(12f) > WordPageLayout.charsPerLine(24f))
        assertTrue(WordPageLayout.lineHeightMm(24f) > WordPageLayout.lineHeightMm(12f))
        assertTrue(WordPageLayout.charsPerLine(16f) >= 8)
        // mm → dp با بزرگ‌نمایی
        assertEquals(210f, WordPageLayout.mmToDp(210f, 1f), 0.001f)
        assertEquals(420f, WordPageLayout.mmToDp(210f, 2f), 0.001f)
    }

    private fun essay(id: String, text: String = "متن پیش‌فرض سؤال") = QuestionDraft(
        id = id,
        type = QuestionType.ESSAY,
        text = text,
        score = 1.0,
        answerLines = 2
    )
}
