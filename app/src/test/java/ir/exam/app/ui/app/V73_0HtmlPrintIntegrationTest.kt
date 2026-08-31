package ir.exam.app.ui.app

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import ir.exam.app.ui.printing.ExamHtmlPrintPayloadBuilder
import java.io.File
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

        assertEquals("ministry", json["template"]?.jsonPrimitive?.content)
        val fields = json["fields"]?.jsonObject!!
        assertEquals("زیست‌شناسی", fields["h7_course"]?.jsonPrimitive?.content)
        assertEquals("۱۴۰۳/۱۰/۲۰", fields["h7_examDate"]?.jsonPrimitive?.content)
        assertEquals("شهید دستغیب", fields["h7_schoolName"]?.jsonPrimitive?.content)
        assertEquals("یازدهم", fields["h7_grade"]?.jsonPrimitive?.content)
        assertEquals("تجربی", fields["h7_major"]?.jsonPrimitive?.content)

        val qArray = json["questions"]?.jsonArray!!
        assertEquals(6, qArray.size)

        val mc = qArray[0].jsonObject
        assertEquals("multiple", mc["type"]?.jsonPrimitive?.content)
        assertEquals("1.5", mc["score"]?.jsonPrimitive?.content)
        val mcOpts = mc["options"]?.jsonArray!!
        assertEquals(4, mcOpts.size)
        assertTrue(mcOpts[1].jsonObject["correct"]?.jsonPrimitive?.booleanOrNull == true)
        assertFalse(mcOpts[0].jsonObject["correct"]?.jsonPrimitive?.booleanOrNull == true)

        val tf = qArray[1].jsonObject
        assertEquals("truefalse", tf["type"]?.jsonPrimitive?.content)
        val tfOpts = tf["options"]?.jsonArray!!
        assertTrue(tfOpts[0].jsonObject["correct"]?.jsonPrimitive?.booleanOrNull == true)
        assertFalse(tfOpts[1].jsonObject["correct"]?.jsonPrimitive?.booleanOrNull == true)

        val essay = qArray[2].jsonObject
        assertEquals("long", essay["type"]?.jsonPrimitive?.content)
        assertEquals(4, essay["answerLines"]?.jsonPrimitive?.intOrNull)
        assertEquals("lined", essay["answerStyle"]?.jsonPrimitive?.content)

        val fill = qArray[3].jsonObject
        assertEquals("fill", fill["type"]?.jsonPrimitive?.content)

        val num = qArray[4].jsonObject
        assertEquals("numeric", num["type"]?.jsonPrimitive?.content)
        assertEquals("12", num["answer"]?.jsonPrimitive?.content)

        val match = qArray[5].jsonObject
        assertEquals("matching", match["type"]?.jsonPrimitive?.content)
        val pairs = match["pairs"]?.jsonArray!!
        assertEquals(2, pairs.size)
        assertEquals("الف", pairs[0].jsonObject["left"]?.jsonPrimitive?.content)
        assertEquals("۲", pairs[0].jsonObject["right"]?.jsonPrimitive?.content)
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
            totalScore = 20.0,
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
        assertEquals("ministry", json["template"]?.jsonPrimitive?.content)
        val fields = json["fields"]?.jsonObject!!
        assertEquals("فیزیک", fields["h7_course"]?.jsonPrimitive?.content)
        assertEquals("دبیرستان رازی", fields["h7_schoolName"]?.jsonPrimitive?.content)

        val qArray = json["questions"]?.jsonArray!!
        assertEquals(1, qArray.size)
        val q0 = qArray[0].jsonObject
        assertEquals("multiple", q0["type"]?.jsonPrimitive?.content)
        assertTrue(q0["options"]?.jsonArray?.get(0)?.jsonObject?.get("correct")?.jsonPrimitive?.booleanOrNull == true)
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
