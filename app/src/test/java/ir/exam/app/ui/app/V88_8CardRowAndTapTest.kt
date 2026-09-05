package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V88.8 — سرصفحهٔ کارت روی گوشی عمودی می‌شد، و لمسِ کارت به‌جای بازکردنِ
 * خودِ کارت یک پنجرهٔ بومی می‌آورد.
 */
class V88_8CardRowAndTapTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `the header stays one horizontal row on a phone`() {
        // `@media (max-width:420px)` سرصفحه را column می‌کرد
        assertTrue("#questionsContainer .q-header{" in asset)
        assertTrue("flex-direction:row !important;" in asset)
        assertTrue("flex-wrap:nowrap !important;" in asset)
    }

    @Test
    fun `score and delete stop stretching to full width`() {
        assertTrue("#questionsContainer .q-score{width:74px !important;flex:0 0 auto}" in asset)
        assertTrue("#questionsContainer .q-remove{" in asset)
        assertTrue("width:auto !important;" in asset)
    }

    @Test
    fun `tapping a collapsed card opens the card itself`() {
        assertTrue("window.__qmfOpenCard(qid)" in asset)
        assertTrue("window.__qmfCardOpener = function (id) { openQuestionId(id, true); return 'ok'; };" in asset)
    }

    @Test
    fun `tapping no longer throws the native editor in the way`() {
        val from = asset.indexOf("/* V88.8 — لمسِ کارت خودِ کارت")
        val to = asset.indexOf("window.__qmfQuestionDetail = function")
        assertTrue(from in 1 until to)
        assertTrue("ExamPrintNative.openQuestion" !in asset.substring(from, to))
    }

    @Test
    fun `an open card is not closed by tapping it again`() {
        assertTrue("if (!card.classList.contains('collapsed')) return;" in asset)
    }

    @Test
    fun `typing, tools and figures keep their own handling`() {
        assertTrue(
            "t.closest('input, textarea, select, button, .q-tools, .interactive-figure, .qmf-fig')" in asset
        )
    }

    @Test
    fun `the native editor is still reachable through its bridge`() {
        // حذف نشد: پل و ویرایشگر سرِ جایشان‌اند
        assertTrue("window.__qmfQuestionDetail = function" in asset)
        assertTrue("fun openQuestion(questionId: String?)" in
            File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText())
    }
}
