package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V88.6 — ویرایشگرِ بومی فقط با لمسِ عمدیِ کارت باز می‌شود، و سرصفحهٔ کارت
 * مثلِ آزمون‌سازِ آنلاین کوتاه است.
 */
class V88_6CardTapEditorTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `adding a question no longer pops the editor open`() {
        // openQuestionId پس از هر افزودن و هر تغییرِ حالت صدا زده می‌شود
        // (hook('addQuestion')…)، پس نباید میزبان را خبر کند.
        val at = asset.indexOf("function openQuestionId")
        assertTrue(at > 0)
        val head = asset.substring(at, at + 700)
        assertTrue("openQuestionId نباید ویرایشگر را باز کند", "ExamPrintNative.openQuestion" !in head)
    }

    @Test
    fun `a deliberate tap on the card opens it`() {
        // V88.8 — لمس، خودِ کارت را باز می‌کند (accordion)، نه پنجرهٔ بومی را.
        assertTrue("window.__qmfOpenCard(qid)" in asset)
        assertTrue("t.closest('#questionsContainer .question-card')" in asset)
    }

    @Test
    fun `typing, tools and figures keep their own behaviour`() {
        assertTrue(
            "t.closest('input, textarea, select, button, .q-tools, .interactive-figure, .qmf-fig')" in asset
        )
    }

    @Test
    fun `it stays safe without the native host`() {
        assertTrue("if (!(window.ExamPrintNative &&" in asset)
    }

    @Test
    fun `the card header stays short like the online builder`() {
        // چیدمان و فضای پاسخ از V88.4 بومی شدند و اینجا تکراری بودند
        assertTrue("#questionsContainer .q-layout-select," in asset)
        assertTrue("#questionsContainer .q-answer-config{display:none !important}" in asset)
    }

    @Test
    fun `but those controls still exist for a plain browser`() {
        assertTrue("class=\"q-layout-select\"" in asset)
        assertTrue("class=\"q-answer-config\"" in asset)
    }

    @Test
    fun `every question stays a visible card`() {
        assertTrue("card.style.display = '';" in asset)
        assertTrue(".question-card.collapsed > *:not(.q-header)" in asset)
    }
}
