package ir.exam.app.ui.app

import ir.exam.app.ui.printing.parsePrintQuestionDetail
import ir.exam.app.ui.printing.printQuestionTypeLabel
import ir.exam.app.ui.printing.printTypeHasAnswerSpace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V88.1 — ویرایشگرِ بومیِ سؤال در آزمون‌سازِ چاپی. سؤال‌ها در جاوااسکریپت
 * می‌مانند تا موتورِ چاپ دست‌نخورده بماند؛ سمتِ بومی از پل می‌خواند و
 * می‌نویسد.
 */
class V88_1NativeQuestionEditorTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val sheet by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintQuestionEditorSheet.kt").readText()
    }

    @Test
    fun `the page exposes a full read and write bridge for one question`() {
        listOf(
            "__qmfQuestionDetail", "__qmfQuestionEdit",
            "__qmfOptionEdit", "__qmfOptionCount", "__qmfPairEdit"
        ).forEach { assertTrue("پل $it نیست", "window.$it = function" in asset) }
    }

    @Test
    fun `every write goes through the page's own function so print stays identical`() {
        listOf(
            "updateQ(q.id, field, v)", "updateOpt(id,", "addOption(id)",
            "removeOption(id,", "addPair(id)", "removePair(id,", "updatePair(id,"
        ).forEach { assertTrue("$it از تابعِ صفحه نمی‌گذرد", it in asset) }
    }

    @Test
    fun `a multiple choice question round trips`() {
        val d = parsePrintQuestionDetail(
            """{"id":"3","type":"multiple","text":"پایتخت؟","score":"2",
               "optionsLayout":"2rows","answerLines":null,"answerStyle":"lined",
               "answerLineHeightCm":null,"answer":"",
               "options":[{"text":"تهران","correct":true},{"text":"شیراز","correct":false}],
               "pairs":[]}"""
        )
        assertTrue(d != null)
        assertEquals("3", d!!.id)
        assertEquals("multiple", d.type)
        assertEquals("پایتخت؟", d.text)
        assertEquals(2, d.options.size)
        assertTrue(d.options[0].correct)
        assertFalse(d.options[1].correct)
    }

    @Test
    fun `a matching question keeps both sides`() {
        val d = parsePrintQuestionDetail(
            """{"id":"4","type":"matching","text":"","score":"","options":[],
               "pairs":[{"left":"آب","right":"H2O"},{"left":"نمک","right":"NaCl"}]}"""
        )!!
        assertEquals(2, d.pairs.size)
        assertEquals("آب", d.pairs[0].left)
        assertEquals("NaCl", d.pairs[1].right)
    }

    @Test
    fun `a missing question yields null rather than an empty editor`() {
        assertNull(parsePrintQuestionDetail("{}"))
        assertNull(parsePrintQuestionDetail(""))
        assertNull(parsePrintQuestionDetail(null))
        // ورودیِ خراب نباید بترکاند
        assertNull(parsePrintQuestionDetail("not json"))
    }

    @Test
    fun `answer space belongs only to essay and fill in the blank`() {
        assertTrue(printTypeHasAnswerSpace("long"))
        assertTrue(printTypeHasAnswerSpace("fill"))
        assertFalse(printTypeHasAnswerSpace("multiple"))
        assertFalse(printTypeHasAnswerSpace("matching"))
        assertFalse(printTypeHasAnswerSpace("numeric"))
    }

    @Test
    fun `every type has a persian label`() {
        listOf("long", "multiple", "truefalse", "fill", "numeric", "matching")
            .forEach { assertTrue(printQuestionTypeLabel(it).isNotBlank()) }
        assertEquals("جورکردنی", printQuestionTypeLabel("matching"))
    }

    @Test
    fun `the formula constant comes from the request type that declares it`() {
        // V88.2 — `ExamFigureToolHost.FORMULA` کامپایل نمی‌شد: آن یک Composable
        // است و FORMULA در companion objectِ FigureToolRequest زندگی می‌کند.
        assertTrue("FigureToolRequest(qid, FigureToolRequest.FORMULA)" in dialog)
        assertTrue("ExamFigureToolHost.FORMULA" !in dialog)
    }

    @Test
    fun `arguments are escaped before they reach the page`() {
        // متنِ سؤال می‌تواند نقل‌قول یا `</script>` داشته باشد
        assertTrue("org.json.JSONObject.quote(value)" in dialog)
        assertTrue("jsArg(qid)" in dialog)
    }

    @Test
    fun `touching a card opens the native editor and degrades outside the app`() {
        assertTrue("window.ExamPrintNative.openQuestion(String(id))" in asset)
        assertTrue("typeof window.ExamPrintNative.openQuestion === 'function'" in asset)
        assertTrue("fun openQuestion(questionId: String?)" in dialog)
        assertTrue("editingQuestionId = qid" in dialog)
    }

    @Test
    fun `the editor covers every question type`() {
        assertTrue("متن سؤال" in sheet)
        assertTrue("RadioButton(" in sheet)
        assertTrue("افزودن گزینه" in sheet)
        assertTrue("افزودن جفت" in sheet)
        assertTrue("پاسخ صحیح" in sheet)
        // V88.4 — فضای پاسخ حالا سه کنترل دارد: سطر، فاصله و سبک
        assertTrue("فضای پاسخ" in sheet)
        assertTrue("فاصله (cm)" in sheet)
        // ابزارهای درج همان بومی‌های موجود
        assertTrue("onOpenFormula" in sheet)
        assertTrue("onOpenFigureTool(\"figure\")" in sheet)
    }
}
