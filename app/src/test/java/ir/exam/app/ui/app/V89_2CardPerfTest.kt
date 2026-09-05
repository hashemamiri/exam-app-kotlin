package ir.exam.app.ui.app

import ir.exam.app.ui.printing.parsePrintQuestionList
import ir.exam.app.ui.printing.printInsertTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V89.2 — کادرِ خالی زیرِ هدر، لگِ کارت‌ها، بازنشدنِ کارت با لمس، و
 * آیکن‌های متنیِ ابزار.
 */
class V89_2CardPerfTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val cards by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintQuestionCards.kt").readText()
    }

    @Test
    fun `the empty white frame under the header is gone inside the app`() {
        assertTrue("body.qmf-native-cards .questions-area{" in asset)
    }

    @Test
    fun `the whole list arrives in one call instead of one per question`() {
        // با ۲۰ سؤال، ۲۱ رفت‌وبرگشتِ زنجیره‌ای می‌شد
        assertTrue("window.__qmfAllQuestions = function" in asset)
        assertTrue("window.__qmfAllQuestions?window.__qmfAllQuestions()" in dialog)
        assertTrue("حلقهٔ زنجیره‌ای باید رفته باشد", "fun fetch(i: Int)" !in dialog)
    }

    @Test
    fun `the list parser handles a real payload`() {
        val parsed = parsePrintQuestionList(
            """[{"id":"1","type":"long","text":"الف","score":"2","options":[],"pairs":[]},
                {"id":"2","type":"multiple","text":"ب","score":"1",
                 "options":[{"text":"x","correct":true}],"pairs":[]}]"""
        )
        assertEquals(2, parsed.size)
        assertEquals("1", parsed[0].id)
        assertEquals("multiple", parsed[1].type)
        assertTrue(parsed[1].options[0].correct)
    }

    @Test
    fun `an empty or broken payload yields an empty list`() {
        assertTrue(parsePrintQuestionList("[]").isEmpty())
        assertTrue(parsePrintQuestionList("").isEmpty())
        assertTrue(parsePrintQuestionList(null).isEmpty())
        assertTrue(parsePrintQuestionList("not json").isEmpty())
    }

    @Test
    fun `tapping an open card closes it`() {
        // تا V89.1 فقط باز می‌کرد، پس لمس بی‌اثر به نظر می‌رسید
        assertTrue("openCardId = if (openCardId == detail.id) null else detail.id" in dialog)
    }

    @Test
    fun `inserting a figure or formula refreshes the card`() {
        assertTrue(
            Regex("mirrorDraft\\(\\)[\\s\\S]{0,240}cardsRefresh\\+\\+[\\s\\S]{0,90}در سؤال درج شد")
                .containsMatchIn(dialog)
        )
        assertTrue(
            Regex("mirrorDraft\\(\\)[\\s\\S]{0,200}cardsRefresh\\+\\+[\\s\\S]{0,70}فرمول در سؤال درج شد")
                .containsMatchIn(dialog)
        )
    }

    @Test
    fun `typing in an option no longer rebuilds the whole list`() {
        assertTrue("if (field == \"correct\")" in dialog)
    }

    @Test
    fun `the eight tools are vector icons, not text`() {
        assertTrue("List<Triple<String, String, ImageVector>>" in cards)
        assertTrue("AssistChip" !in cards)
        assertTrue("Icon(icon, contentDescription = label)" in cards)
        assertEquals(8, Regex("QuestionToolIcons\\.\\w+").findAll(cards).count())
        assertEquals(8, printInsertTools.size)
    }

    @Test
    fun `those icons are the same ones the online builder uses`() {
        val icons = File(root(), "app/src/main/java/ir/exam/app/ui/math/QuestionToolIcons.kt").readText()
        listOf("Formula", "Figure", "Graph", "Table", "Anatomy", "Periodic", "Physics", "Chemistry")
            .forEach { name ->
                assertTrue("آیکنِ $name در منبعِ مشترک نیست", "val $name: ImageVector" in icons)
                assertTrue("کارت از $name استفاده نمی‌کند", "QuestionToolIcons.$name" in cards)
            }
    }
}
