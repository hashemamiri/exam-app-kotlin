package ir.exam.app.ui.app

import ir.exam.app.ui.printing.printInsertTools
import ir.exam.app.ui.printing.printPastelColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V88.9 — کارتِ HTML داخلِ برنامه کنار رفت و فهرستِ بومیِ کارت‌ها جایش را
 * گرفت، با همان ظاهرِ آزمون‌سازِ آنلاین.
 */
class V88_9NativeCardsTest {

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
    /** کدِ کارت بدونِ کامنت، برای سنجهٔ «نیامده است». */
    private val cardsCode by lazy {
        cards.lineSequence().filterNot { it.trimStart().startsWith("*") ||
            it.trimStart().startsWith("//") || it.trimStart().startsWith("/*") }.joinToString("\n")
    }

    @Test
    fun `the html card list steps aside inside the app only`() {
        assertTrue("body.qmf-native-cards #questionsContainer{display:none !important}" in asset)
        assertTrue("document.body.classList.add('qmf-native-cards')" in asset)
        // بیرونِ برنامه دست‌نخورده
        assertTrue("if (window.ExamPrintNative && typeof window.ExamPrintNative.openQuestion === 'function')" in asset)
        // گره می‌ماند چون renderEditor و رندرِ چاپ به آن ارجاع دارند
        assertTrue("id=\"questionsContainer\"" in asset)
    }

    @Test
    fun `the native card uses the same pastel palette as the online one`() {
        val drafts = File(root(), "app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt").readText()
        listOf(
            Triple("long", "ESSAY", 0xFFFFD1DC),
            Triple("multiple", "MULTIPLE_CHOICE", 0xFFAEC6CF),
            Triple("truefalse", "TRUE_FALSE", 0xFFB4EEB4),
            Triple("fill", "FILL_BLANK", 0xFFFDFD96),
            Triple("numeric", "NUMERIC", 0xFFC3B1E1),
            Triple("matching", "MATCHING", 0xFFFFDAB9)
        ).forEach { (html, kotlin, argb) ->
            assertEquals("رنگِ $html", argb, printPastelColor(html).value.toLong() shr 32 and 0xFFFFFFFFL)
            assertTrue("آنلاین $kotlin عوض شده",
                "QuestionType.$kotlin -> 0x${argb.toString(16).uppercase()}" in drafts)
        }
    }

    @Test
    fun `all eight insert tools are present and formula is one of them`() {
        assertEquals(8, printInsertTools.size)
        assertTrue(printInsertTools.any { it.first == "formula" })
        listOf("figure", "graph", "table", "anatomy", "periodic", "physics", "chemistry")
            .forEach { tool -> assertTrue("ابزارِ $tool نیست", printInsertTools.any { it.first == tool }) }
    }

    @Test
    fun `the camera button opens the image studio`() {
        assertTrue("onOpenImageStudio" in cards)
        assertTrue("PhotoCamera" in cards)
        assertTrue("onOpenImageStudio = { studioQuestionId = detail.id }" in dialog)
    }

    @Test
    fun `the print layout controls sit at the end of the card`() {
        assertTrue("ترتیب گزینه‌ها" in cardsCode)
        assertTrue("فضای پاسخ" in cardsCode)
        assertTrue("فاصله (cm)" in cardsCode)
        assertTrue("خط‌دار" in cardsCode)
    }

    @Test
    fun `controls that only make sense online are left out`() {
        listOf("حساس به حروف", "نمودار پاسخ", "تصویر پاسخ", "خطای مجاز", "ذخیره در بانک")
            .forEach { assertTrue("$it نباید در کارتِ چاپی باشد", it !in cardsCode) }
    }

    @Test
    fun `the live preview comes from the page's own renderer`() {
        assertTrue("window.__qmfRichPreview = function" in asset)
        assertTrue("renderRichText(q.text || '', q)" in asset)
        assertTrue("window.__qmfRichPreview?window.__qmfRichPreview(" in dialog)
    }

    @Test
    fun `the preview view runs no scripts`() {
        // V89.0 — نمایشگر به `QuestionTextFieldWebView.kt` منتقل شد تا گاردِ
        // «WebView فقط در فایل‌های تأییدشده» دست‌نخورده بماند.
        val preview = File(root(), "app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt").readText()
        assertTrue("fun PrintRichTextPreview(" in preview)
        assertTrue("settings.javaScriptEnabled = false" in preview)
        assertTrue("settings.allowFileAccess = false" in preview)
    }

    @Test
    fun `adding a question refreshes the native list`() {
        assertTrue(Regex("pickQuestionType[\\s\\S]{0,400}cardsRefresh\\+\\+").containsMatchIn(dialog))
    }
}
