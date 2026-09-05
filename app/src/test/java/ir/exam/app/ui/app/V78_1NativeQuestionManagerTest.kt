package ir.exam.app.ui.app

import ir.exam.app.ui.printing.parseQuestionRows
import ir.exam.app.ui.printing.toPersianDigits
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V78.1 — نوارِ بومیِ مدیریت سؤال: حذف، کپی، جابه‌جایی، بارم، چیدمان گزینه‌ها،
 * افزودن گزینه/جفت، و نوارِ شمارهٔ سؤال.
 *
 * اصل طراحی: هیچ منطقی در کاتلین تکرار نشده — همهٔ کارها همان توابعِ موجودِ
 * صفحه را صدا می‌زنند تا رندر و چاپ دقیقاً یکسان بماند. کادرِ متنِ سؤال طبق
 * تصمیم کاربر بومی نشده است.
 */
class V78_1NativeQuestionManagerTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val assetText by lazy { source("app/src/main/assets/print/exam_print.html") }
    private val sheet by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamQuestionManagerSheet.kt") }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }

    @Test
    fun `all three bridges exist`() {
        listOf("__qmfQuestionList", "__qmfQuestionAction", "__qmfTotalScore").forEach {
            assertTrue("پل $it نیست", "window.$it" in assetText)
        }
    }

    @Test
    fun `every action delegates to the existing page function`() {
        // اگر این‌ها را دور بزنیم، رفتار نسخهٔ بومی و HTML واگرا می‌شود
        listOf(
            "removeQuestion(q.id)", "window.moveQuestion(q.id", "addOption(q.id)",
            "removeOption(q.id", "addPair(q.id)", "removePair(q.id"
        ).forEach { fn ->
            assertTrue("کارِ $fn باید به تابع موجود واگذار شود", fn in assetText)
        }
    }

    @Test
    fun `duplicate keeps a fresh id and lands right after the source`() {
        assertTrue("JSON.parse(JSON.stringify(q))" in assetText)
        assertTrue("qIdCounter++" in assetText)
        assertTrue("questions.splice(idx + 1, 0, copy)" in assetText)
    }

    @Test
    fun `kotlin sheet is wired into the toolbar`() {
        // V87.4 — نوارِ دکمه‌ها برداشته شد؛ مدیریتِ سؤال از منویِ + می‌آید.
        assertTrue("showQuestionManager = true" in dialog)
        assertTrue("ExamQuestionManagerSheet(" in dialog)
        assertTrue("parseQuestionRows(" in dialog)
    }

    @Test
    fun `question text box stays out of scope`() {
        // نوار مدیریت نباید کادر متن سؤال را تصاحب کند
        assertFalse("q_text_" in sheet)
        assertTrue("function renderEditor" in assetText)
    }

    @Test
    fun `row parser reads the bridge payload`() {
        val raw = """[{"id":"3","index":1,"type":"multiple","score":"2","preview":"سؤال نمونه"}]"""
        val rows = parseQuestionRows(raw)
        assertEquals(1, rows.size)
        assertEquals("3", rows[0].id)
        assertEquals(1, rows[0].index)
        assertEquals("🔘 چندگزینه‌ای", rows[0].typeLabel)
        assertEquals("2", rows[0].score)
    }

    @Test
    fun `row parser survives junk`() {
        assertTrue(parseQuestionRows(null).isEmpty())
        assertTrue(parseQuestionRows("").isEmpty())
        assertTrue(parseQuestionRows("not json").isEmpty())
        assertTrue(parseQuestionRows("[]").isEmpty())
    }

    @Test
    fun `every question type has a persian label`() {
        listOf("multiple", "truefalse", "fill", "numeric", "matching", "long").forEach { type ->
            val raw = """[{"id":"1","index":1,"type":"$type","score":"","preview":""}]"""
            assertTrue("نوع $type برچسب ندارد", parseQuestionRows(raw)[0].typeLabel.isNotBlank())
        }
    }

    @Test
    fun `persian digits helper matches the builder convention`() {
        assertEquals("۱۲۳", toPersianDigits("123"))
        assertEquals("۰٫۵", toPersianDigits("0٫5"))
        assertEquals("سؤال ۷", toPersianDigits("سؤال 7"))
    }

    @Test
    fun `figure tokens are stripped from the preview text`() {
        // پیش‌نمایشِ سؤال نباید پر از JSON شکل باشد
        assertTrue("replace(/%%FIG:\\{.*?\\}%%/g, ' ⟦شکل⟧ ')" in assetText)
    }
}
