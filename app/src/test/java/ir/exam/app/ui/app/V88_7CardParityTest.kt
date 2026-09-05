package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V88.7 — نوارِ شمارهٔ سؤالات حذف شد و کارتِ سؤالِ چاپی همان زبانِ بصریِ
 * آزمون‌سازِ آنلاین را گرفت.
 */
class V88_7CardParityTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val drafts by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt").readText()
    }

    /** نگاشتِ نوعِ سؤال به رنگِ پاستلی، همان‌که آنلاین استفاده می‌کند. */
    private val pastel = listOf(
        Triple("long", "ESSAY", "FFD1DC"),
        Triple("multiple", "MULTIPLE_CHOICE", "AEC6CF"),
        Triple("truefalse", "TRUE_FALSE", "B4EEB4"),
        Triple("fill", "FILL_BLANK", "FDFD96"),
        Triple("numeric", "NUMERIC", "C3B1E1"),
        Triple("matching", "MATCHING", "FFDAB9")
    )

    @Test
    fun `the question number strip is gone`() {
        // سؤال‌ها از V87.7 کارت‌اند و شماره روی سرصفحهٔ هر کارت دیده می‌شود
        assertTrue(".qnum-strip-wrap{margin:4px 0 14px;display:none !important}" in asset)
    }

    @Test
    fun `but its code stays because other paths call it`() {
        assertTrue("function syncStrip" in asset)
        assertTrue("id=\"questionNumberStripWrap\"" in asset)
    }

    @Test
    fun `each card carries its question type`() {
        assertTrue("card.dataset.qtype = String(q.type || 'long');" in asset)
    }

    @Test
    fun `the printable card uses the very colours the online card uses`() {
        pastel.forEach { (htmlType, kotlinType, hex) ->
            assertTrue(
                "رنگِ $kotlinType در آزمونِ آنلاین عوض شده",
                "QuestionType.$kotlinType -> 0xFF$hex" in drafts
            )
            assertTrue(
                "رنگِ $htmlType در کارتِ چاپی جا افتاده",
                "[data-qtype=\"$htmlType\"] .q-number{background:#$hex}" in asset
            )
        }
    }

    @Test
    fun `the card shape matches the online one`() {
        assertTrue("border-radius:14px" in asset)
        assertTrue("border-right:6px solid" in asset)
    }

    @Test
    fun `the duplicated header controls stay hidden`() {
        assertTrue("#questionsContainer .q-answer-config{display:none !important}" in asset)
    }
}
