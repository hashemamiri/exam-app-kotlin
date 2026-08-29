package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * V63.2 — پچ ۳ ویرایشگر سند Word-مانند: قالب متن و ترتیب سؤال‌ها.
 * ۱) لمس کارت سؤال نوار قالب را باز می‌کند: آ-/آ+ (اندازه)، بولد، ایتالیک،
 *    تراز راست/وسط/چپ، جابه‌جایی سؤال بالا/پایین.
 * ۲) همه با توابع موجود ExamBuilderViewModel ذخیره می‌شوند (همان مسیر
 *    چاپ رسمی: RenderBlock از bold/italic/textAlign سؤال می‌خواند).
 * ۳) رندر برگه همان استایل را نشان می‌دهد (weight/style/align روی
 *    NativeMathText متن و گزینه‌ها).
 * ۴) مداد همچنان دیالوگ متن/بارم را باز می‌کند (state جدا).
 */
class V63_2DocFormatReorderTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }

    @Test
    fun `tapping a question opens the format bar with size bold italic align and reorder`() {
        // V63.3 — نوار واحد DocumentToolbar جایگزین QuestionFormatBar شد.
        assertTrue("fun DocumentToolbar(" in editor)
        // V68 — یک‌لمسی مثل ورد: لمس سؤال با موقعیت، مکان‌نا در همان نقطه.
        assertTrue("detectTapGestures(onTap = { pos ->" in editor)
        // اندازه با گام ۲
        assertTrue("onFontSize(+2f)" in editor)
        assertTrue("onFontSize(-2f)" in editor)
        // بولد/ایتالیک/تراز/ترتیب
        assertTrue("Icons.Outlined.FormatBold" in editor)
        assertTrue("Icons.Outlined.FormatItalic" in editor)
        assertTrue("Icons.Outlined.FormatAlignCenter" in editor)
        assertTrue("contentDescription = \"سؤال بالاتر\"" in editor)
        assertTrue("contentDescription = \"سؤال پایین‌تر\"" in editor)
    }

    @Test
    fun `format actions persist through the builder view-model used by print`() {
        assertTrue("builder.setQuestionFontSize(it.id, it.fontSizeSp + delta)" in editor)
        assertTrue("builder.setQuestionBold(it.id, !it.bold)" in editor)
        assertTrue("builder.setQuestionItalic(it.id, !it.italic)" in editor)
        assertTrue("builder.setQuestionAlign(it.id, value)" in editor)
        assertTrue("builder.moveQuestion(it.id, delta)" in editor)
        // ویومدل clamp و ترتیب امن دارد
        assertTrue("fontSizeSp=value.coerceIn(8f,40f)" in builderVm)
        assertTrue("(from + delta).coerceIn(0, state.questions.lastIndex)" in builderVm)
        // چاپ رسمی همین فیلدها را می‌خواند
        assertTrue("bold=question.bold,italic=question.italic,align=question.textAlign" in pdfAdapter)
    }

    @Test
    fun `page render mirrors the styles and the pencil keeps its own dialog`() {
        assertTrue("val weight = if (question.bold) FontWeight.Bold else null" in editor)
        assertTrue("val style = if (question.italic) FontStyle.Italic else null" in editor)
        assertTrue("\"center\" -> TextAlign.Center" in editor)
        assertTrue("fontWeight = weight" in editor)
        assertTrue("fontStyle = style" in editor)
        // متن دیگر همیشه راست‌چین hardcode نیست
        assertFalse("source = textOnly,\n            fontSize = fontSize,\n            textAlign = TextAlign.Right" in editor)
        // V63.4 — پنجرهٔ جدا حذف شد؛ ویرایش متن/بارم درجا روی خود برگه.
        assertTrue("onTextChange = builder::updateText" in editor)
        assertTrue("onScoreChange = builder::updateScore" in editor)
        assertTrue("editingQuestionId = if (editingQuestionId == id) null else id" in editor)
    }
}
