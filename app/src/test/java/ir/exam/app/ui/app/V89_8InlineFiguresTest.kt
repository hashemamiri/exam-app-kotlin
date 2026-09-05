package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V89.8 — اشیاء باید **درونِ** کادرِ متن دیده شوند نه زیرِ آن، و جابه‌جاییِ
 * جدول و جدولِ تناوبی در پیش‌نمایش باید مثلِ آناتومی باشد.
 */
class V89_8InlineFiguresTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val cards by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintQuestionCards.kt").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val section by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt").readText()
    }

    @Test
    fun `the printable card reuses the online text section`() {
        assertTrue("ir.exam.app.ui.builder.QuestionTextWebSection(" in cards)
        // پیش‌نمایشِ جدا زیرِ کادر برداشته شد
        assertTrue("PrintRichTextPreview" !in cards)
    }

    @Test
    fun `that section renders figures inline and needs no view model`() {
        assertTrue("is RichSegment.Figure" in section)
        assertTrue("viewModel" !in section)
    }

    @Test
    fun `both builders speak the same token format`() {
        // اگر قالب فرق کند، هیچ‌کدام شیءِ دیگری را نمی‌بیند
        assertTrue("'%%FIG:' + JSON.stringify(spec) + '%%'" in asset)
        val codec = File(root(), "app/src/main/java/ir/exam/app/ui/builder").listFiles()
            ?.any { it.isFile && "\"%%FIG:" in it.readText() } ?: false
        assertTrue("سمتِ کاتلین همان قالب را نمی‌سازد", codec)
    }

    @Test
    fun `every insert tool is wired to the section`() {
        listOf(
            "onInsertFigure", "onInsertGraph", "onInsertTable", "onInsertPeriodic",
            "onInsertAnatomy", "onInsertPhysics", "onInsertChemistry"
        ).forEach { assertTrue("$it وصل نیست", "$it =" in cards) }
        assertTrue("onOpenFormula = { _, _, _ ->" in cards)
    }

    @Test
    fun `the separate preview state is gone from the dialog`() {
        assertTrue("cardPreviewHtml" !in dialog)
        assertTrue("cardPreviewCss" !in dialog)
    }

    @Test
    fun `a table drags exactly like an anatomy figure`() {
        // آناتومی SVG می‌دهد و لمس عبور می‌کند؛ جدول/تناوبی HTML با td و input
        assertTrue("#previewArea .interactive-figure * {" in asset)
        assertTrue("#previewArea .interactive-figure {" in asset)
    }

    @Test
    fun `but the table editor stays typable`() {
        assertTrue(
            ".tbx-t input[data-r][data-c]{display:block!important;pointer-events:auto!important" in asset
        )
    }

    @Test
    fun `pinch and cursor insertion from V89_7 still stand`() {
        assertTrue("function twoFingerDist(t)" in asset)
        assertTrue("window.__qmfSetInsertPos = function" in asset)
    }
}
