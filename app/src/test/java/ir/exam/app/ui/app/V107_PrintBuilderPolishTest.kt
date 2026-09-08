package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V107 — اصلاحات آزمون‌سازِ چاپی (بخش «چاپ آزمون»). */
class V107_PrintBuilderPolishTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val renderer by lazy { source("app/src/main/assets/print/exam_print_renderer.html") }
    private val centre by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val media by lazy { source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt") }
    private val textSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }
    private val codec by lazy { source("app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt") }
    private val payload by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt") }

    @Test
    fun `print builder hides online-only answer controls and shows answer space controls`() {
        assertTrue("تصویر/نمودار پاسخ در حالت چاپی پنهان نمی‌شود", "if (printMode) {\n                // V110 — فضای پاسخ" in builder)
        assertTrue("کلید جای‌خالی در حالت چاپی پنهان نیست", "QuestionType.FILL_BLANK -> if (!printMode) Column(" in builder)
        assertTrue("کلید عددی در حالت چاپی پنهان نیست", "QuestionType.NUMERIC -> if (!printMode) {" in builder)
        assertTrue("کنترل فضای پاسخ نیست", "private fun PrintAnswerSpaceControls(" in builder)
        listOf("خط‌چین", "خالی", "شطرنجی", "فاصلهٔ سطر", "valueRange = 0.5f..2.0f").forEach {
            assertTrue("«$it» در فضای پاسخ نیست", it in builder)
        }
        assertTrue("codec فاصلهٔ سطر را ذخیره نمی‌کند", "values[\"answerLineSpacingCm\"]" in codec)
        assertTrue("codec شطرنجی را نمی‌پذیرد", "setOf(\"lined\", \"blank\", \"grid\")" in codec)
        assertTrue("payload شطرنجی/فاصله را نمی‌فرستد", "\"grid\" -> \"grid\"" in payload && "answerLineSpacingCm" in payload)
        assertTrue("رندرر شطرنجی ندارد", ".answer-space.grid{" in renderer && "answerLineSpacingCm:clamp(" in renderer)
    }

    @Test
    fun `question card header icons have more room and image row is icon-only`() {
        assertTrue("فاصلهٔ آیکن‌های سربرگ کارت ۶dp نیست", "horizontalArrangement = Arrangement.spacedBy(6.dp),\n                verticalAlignment = Alignment.CenterVertically\n            ) {\n                Box(" in builder)
        assertTrue("برچسب نوع سؤال بدون شکست خط نیست", "softWrap = false" in builder)
        assertFalse("نوشتهٔ «تصویر» هنوز کنار آیکن است", "Text(\"تصویر\", style = MaterialTheme.typography.labelSmall)" in media)
        assertTrue("کادر انتخاب شیء فشرده نشده", "val compactFigure = spec.kind !in setOf(\"a\", \"s\", \"t\", \"p\")" in textSection)
    }

    @Test
    fun `preview fits whole A4 by default, selects before drag and has no header help`() {
        assertTrue("دکمهٔ کل صفحه/اندازهٔ واقعی نیست", "id=\"fitToggle\"" in renderer && "function applyFit()" in renderer)
        assertTrue("درگ با مقیاس هماهنگ نیست", "function sheetScale()" in renderer)
        assertTrue("لمس اول فقط انتخاب نمی‌کند", "if (!figure.classList.contains('selected')) { selectFigure(figure); return; }" in renderer)
        assertFalse("متن راهنمای سربرگ هنوز هست", "شکل را بکشید تا هرجای برگه ببرید" in renderer)
    }

    @Test
    fun `print centre cards no longer carry printer icons`() {
        assertFalse("Icons.Outlined.Print" in centre)
        assertFalse("PrintTarget" in centre)
        assertFalse("HeadlessExamPrinter" in centre)
    }
}
