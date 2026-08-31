package ir.exam.app.ui.app

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import ir.exam.app.ui.printing.ExamHtmlPrintPayloadBuilder
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V73.0 — آزمون‌های یکپارچگی چاپ تعاملی HTML و اتصال خودکار سؤالات آزمون:
 * ۱) نگاشت دقیق هر ۶ نوع سؤال کاتلین به ساختار JSON صفحهٔ چاپ HTML.
 * ۲) نگاشت کامل اطلاعات سربرگ رسمی به فیلدهای فرم HTML.
 * ۳) وجود فایل asset چاپ و توابع setExamData و پل بومی ExamPrintNative.
 * ۴) وجود دکمهٔ «چاپ» در کنار «چاپ برگه» و «چاپ با کلید» روی کارت آزمون.
 */
class V73_0HtmlPrintIntegrationTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val printCenter by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val dialogSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val payloadBuilder by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt") }
    private val htmlAsset by lazy { File(root(), "app/src/main/assets/print/exam_print.html") }

    @Test
    fun `payload builder maps all six question types to json`() {
        val questions = listOf(
            QuestionDraft(
                type = QuestionType.MULTIPLE_CHOICE,
                text = "سؤال چهارگزینه‌ای",
                score = 1.5,
                options = listOf("گزینه ۱", "گزینه ۲", "گزینه ۳", "گزینه ۴"),
                correctIndex = 1
            ),
            QuestionDraft(
                type = QuestionType.TRUE_FALSE,
                text = "سؤال صحیح و غلط",
                score = 1.0,
                expectedText = "true"
            ),
            QuestionDraft(
                type = QuestionType.ESSAY,
                text = "سؤال تشریحی",
                score = 2.0,
                answerLines = 4,
                answerLineStyle = "lined"
            ),
            QuestionDraft(
                type = QuestionType.FILL_BLANK,
                text = "پایتخت ایران [...] است.",
                score = 1.0
            ),
            QuestionDraft(
                type = QuestionType.NUMERIC,
                text = "حاصل ۴ × ۳",
                score = 0.5,
                expectedNumber = "12"
            ),
            QuestionDraft(
                type = QuestionType.MATCHING,
                text = "جورکردنی",
                score = 2.0,
                matchingLeft = listOf("الف", "ب"),
                matchingRight = listOf("۱", "۲"),
                matchingPairs = mapOf(0 to 1, 1 to 0)
            )
        )

        val header = OfficialPrintHeader(
            province = "فارس",
            city = "شیراز",
            district = "۱",
            school = "شهید دستغیب",
            grade = "یازدهم",
            fieldOfStudy = "تجربی",
            subject = "زیست‌شناسی",
            examDate = "۱۴۰۳/۱۰/۲۰",
            examDuration = "۶۰"
        )

        val json = ExamHtmlPrintPayloadBuilder.buildFromDrafts(
            title = "آزمون نوبت اول",
            subject = "زیست‌شناسی",
            durationMinutes = 60,
            header = header,
            questions = questions
        )

        assertEquals("ministry", json.getString("template"))
        val fields = json.getJSONObject("fields")
        assertEquals("زیست‌شناسی", fields.getString("h7_course"))
        assertEquals("۱۴۰۳/۱۰/۲۰", fields.getString("h7_examDate"))
        assertEquals("شهید دستغیب", fields.getString("h7_schoolName"))
        assertEquals("یازدهم", fields.getString("h7_grade"))
        assertEquals("تجربی", fields.getString("h7_major"))

        val qArray = json.getJSONArray("questions")
        assertEquals(6, qArray.length())

        val mc = qArray.getJSONObject(0)
        assertEquals("multiple", mc.getString("type"))
        assertEquals("1.5", mc.getString("score"))
        val mcOpts = mc.getJSONArray("options")
        assertEquals(4, mcOpts.length())
        assertTrue(mcOpts.getJSONObject(1).getBoolean("correct"))
        assertFalse(mcOpts.getJSONObject(0).getBoolean("correct"))

        val tf = qArray.getJSONObject(1)
        assertEquals("truefalse", tf.getString("type"))
        val tfOpts = tf.getJSONArray("options")
        assertTrue(tfOpts.getJSONObject(0).getBoolean("correct"))
        assertFalse(tfOpts.getJSONObject(1).getBoolean("correct"))

        val essay = qArray.getJSONObject(2)
        assertEquals("long", essay.getString("type"))
        assertEquals(4, essay.getInt("answerLines"))
        assertEquals("lined", essay.getString("answerStyle"))

        val fill = qArray.getJSONObject(3)
        assertEquals("fill", fill.getString("type"))

        val num = qArray.getJSONObject(4)
        assertEquals("numeric", num.getString("type"))
        assertEquals("12", num.getString("answer"))

        val match = qArray.getJSONObject(5)
        assertEquals("matching", match.getString("type"))
        val pairs = match.getJSONArray("pairs")
        assertEquals(2, pairs.length())
        assertEquals("الف", pairs.getJSONObject(0).getString("left"))
        assertEquals("۲", pairs.getJSONObject(0).getString("right"))
    }

    @Test
    fun `printable exam payload builder populates json`() {
        val printable = OfficialExamPrintable(
            documentTitle = "فیزیک دهم",
            header = OfficialPrintHeader(
                school = "دبیرستان رازی",
                examDate = "۱۴۰۳/۰۹/۱۵",
                grade = "دهم"
            ),
            subject = "فیزیک",
            durationMinutes = 75,
            questions = listOf(
                OfficialPrintQuestion(
                    number = 1,
                    text = "سؤال تست",
                    score = 2.0,
                    options = listOf("الف", "ب"),
                    answerText = "الف"
                )
            )
        )

        val json = ExamHtmlPrintPayloadBuilder.build(printable)
        assertEquals("ministry", json.getString("template"))
        val fields = json.getJSONObject("fields")
        assertEquals("فیزیک", fields.getString("h7_course"))
        assertEquals("دبیرستان رازی", fields.getString("h7_schoolName"))

        val qArray = json.getJSONArray("questions")
        assertEquals(1, qArray.length())
        val q0 = qArray.getJSONObject(0)
        assertEquals("multiple", q0.getString("type"))
        assertTrue(q0.getJSONArray("options").getJSONObject(0).getBoolean("correct"))
    }

    @Test
    fun `html asset exists and supports native bridge`() {
        assertTrue(htmlAsset.isFile)
        assertTrue(htmlAsset.length() > 400_000L)
        val content = htmlAsset.readText()
        assertTrue("window.setExamData" in content)
        assertTrue("ExamPrintNative" in content)
        assertTrue("updateHeaderSettingsVisibility()" in content)
        assertTrue("renderAll()" in content)
    }

    @Test
    fun `print center screen includes the new print button and html dialog`() {
        assertTrue("Text(\"چاپ\")" in printCenter)
        assertTrue("Text(\"چاپ برگه\")" in printCenter)
        assertTrue("Text(\"چاپ با کلید\")" in printCenter)
        assertTrue("ExamHtmlPrintDialog(" in printCenter)
        assertTrue("htmlPrintExam" in printCenter)
        assertTrue("htmlPrintLoading" in printCenter)
    }

    @Test
    fun `html dialog implements secure webview and print bridge`() {
        assertTrue("fun ExamHtmlPrintDialog(" in dialogSource)
        assertTrue("ExamPrintNative" in dialogSource)
        assertTrue("exam-print.local" in dialogSource)
        assertTrue("print/exam_print.html" in dialogSource)
        assertTrue("createPrintDocumentAdapter" in dialogSource)
        assertTrue("ExamHtmlPrintPayloadBuilder" in dialogSource)
    }
}
