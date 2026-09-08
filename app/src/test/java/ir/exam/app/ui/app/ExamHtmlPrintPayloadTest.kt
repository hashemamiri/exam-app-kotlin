package ir.exam.app.ui.app

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import ir.exam.app.domain.model.PrintTextSpan
import ir.exam.app.ui.printing.ExamHtmlPrintPayloadBuilder
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** دادهٔ واقعی که از Kotlin به موتور مستقل چاپ فرستاده می‌شود. */
class ExamHtmlPrintPayloadTest {
    private fun printable(question: OfficialPrintQuestion) = OfficialExamPrintable(
        documentTitle = "آزمون ریاضی",
        subject = "ریاضی ۲",
        durationMinutes = 90,
        totalScore = 20.0,
        header = OfficialPrintHeader(school = "دبیرستان نمونه", examDate = "۱۴۰۵/۰۶/۱۶"),
        footerNote = "نام و امضای دبیر",
        includeAnswerKey = true,
        questions = listOf(question)
    )

    @Test
    fun `header data has canonical defaults and accepts only schema fields`() {
        val payload = ExamHtmlPrintPayloadBuilder.build(
            printable(OfficialPrintQuestion(1, "متن", 2.0)),
            mapOf(
                "f_headerTemplate" to "formal",
                "h2_course" to "ریاضی پیشرفته",
                "f_course" to "",
                "not_a_header_field" to "نباید وارد شود"
            )
        )
        val fields = payload["fields"]!!.jsonObject
        assertEquals("formal", fields["f_headerTemplate"]!!.jsonPrimitive.content)
        assertEquals("ریاضی پیشرفته", fields["h2_course"]!!.jsonPrimitive.content)
        assertEquals("ریاضی ۲", fields["f_course"]!!.jsonPrimitive.content)
        assertFalse("not_a_header_field" in fields)
        assertEquals("آزمون ریاضی", payload["documentTitle"]!!.jsonPrimitive.content)
        assertEquals("20", payload["totalScore"]!!.jsonPrimitive.content)
    }

    @Test
    fun `question payload preserves rich text styles layout and answer key`() {
        val question = OfficialPrintQuestion(
            number = 1,
            text = "اگر \$x^2\$ باشد %%FIG:{\"t\":\"tri\"}%% را پیدا کنید.",
            score = 3.0,
            options = listOf("۱", "۲", "۳", "۴"),
            optionStyles = listOf<Triple<Boolean, Boolean, Float?>?>(Triple(true, false, 18f), null, null, null),
            answerText = "۲",
            textAlign = "center",
            fontFamily = "Vazirmatn",
            fontSizeSp = 18f,
            bold = true,
            italic = true,
            textSpans = listOf(PrintTextSpan(start = 0, end = 3, bold = true)),
            figLayoutsJson = "{\"0\":{\"x\":12,\"y\":34,\"w\":220,\"h\":160,\"free\":true}}",
            sepExtraPx = 26
        )
        val item = ExamHtmlPrintPayloadBuilder.build(printable(question))["questions"]!!.jsonArray.single().jsonObject
        assertEquals("multiple", item["type"]!!.jsonPrimitive.content)
        assertEquals("center", item["textAlign"]!!.jsonPrimitive.content)
        assertEquals("Vazirmatn", item["fontFamily"]!!.jsonPrimitive.content)
        assertEquals("18.0", item["fontSizeSp"]!!.jsonPrimitive.content)
        assertTrue(item["bold"]!!.jsonPrimitive.boolean)
        assertTrue(item["italic"]!!.jsonPrimitive.boolean)
        assertEquals(question.figLayoutsJson, item["figLayoutsJson"]!!.jsonPrimitive.content)
        assertEquals(26, item["sepExtraPx"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, item["textSpans"]!!.jsonArray.size)
        val firstOption = item["options"]!!.jsonArray.first().jsonObject
        assertTrue(firstOption["correct"]!!.jsonPrimitive.boolean.not())
        assertTrue(firstOption["bold"]!!.jsonPrimitive.boolean)
        assertEquals("18.0", firstOption["size"]!!.jsonPrimitive.content)
        val secondOption = item["options"]!!.jsonArray[1].jsonObject
        assertTrue(secondOption["correct"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `true false question prints as a two-option truefalse type, not essay`() {
        // V120 — رگرسیون: قبلاً «صحیح/غلط» چون هیچ‌وقت options نداشت، هیچ
        // شرطی از heuristicType برایش صدق نمی‌کرد و به‌اشتباه «تشریحی»
        // (type=long) چاپ می‌شد. با questionType صریح این دیگر تکرار نمی‌شود.
        val trueFalse = OfficialPrintQuestion(
            number = 1,
            text = "زمین گرد است.",
            score = 1.0,
            options = listOf("صحیح", "غلط"),
            answerText = "صحیح",
            questionType = "truefalse"
        )
        val item = ExamHtmlPrintPayloadBuilder.build(printable(trueFalse))["questions"]!!.jsonArray.single().jsonObject
        assertEquals("truefalse", item["type"]!!.jsonPrimitive.content)
        val options = item["options"]!!.jsonArray
        assertEquals(2, options.size)
        assertEquals("صحیح", options[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertTrue(options[0].jsonObject["correct"]!!.jsonPrimitive.boolean)
        assertEquals("غلط", options[1].jsonObject["text"]!!.jsonPrimitive.content)
        assertFalse(options[1].jsonObject["correct"]!!.jsonPrimitive.boolean)

        // پاسخِ «غلط» باید گزینهٔ دوم را صحیح علامت بزند.
        val falseAnswer = trueFalse.copy(answerText = "غلط")
        val falseItem = ExamHtmlPrintPayloadBuilder.build(printable(falseAnswer))["questions"]!!.jsonArray.single().jsonObject
        val falseOptions = falseItem["options"]!!.jsonArray
        assertFalse(falseOptions[0].jsonObject["correct"]!!.jsonPrimitive.boolean)
        assertTrue(falseOptions[1].jsonObject["correct"]!!.jsonPrimitive.boolean)

        // بدونِ questionType صریح، حدسِ قدیمی (سازگاریِ عقب‌رو) هم باید
        // «صحیح/غلط» را با موفقیت به truefalse تشخیص دهد، نه multiple/long.
        val legacyGuess = trueFalse.copy(questionType = null)
        val legacyItem = ExamHtmlPrintPayloadBuilder.build(printable(legacyGuess))["questions"]!!.jsonArray.single().jsonObject
        assertEquals("truefalse", legacyItem["type"]!!.jsonPrimitive.content)
    }

    @Test
    fun `matching numeric and empty payloads retain their print semantics`() {
        val matching = OfficialPrintQuestion(
            number = 1,
            text = "موارد را وصل کنید",
            score = 4.0,
            matchingLeft = listOf("الف", "ب"),
            matchingRight = listOf("۱", "۲"),
            matchingLeftStyles = listOf<Triple<Boolean, Boolean, Float?>?>(Triple(true, false, 16f)),
            answerText = "۱←۲، ۲←۱"
        )
        val matchingItem = ExamHtmlPrintPayloadBuilder.build(printable(matching))["questions"]!!.jsonArray.single().jsonObject
        assertEquals("matching", matchingItem["type"]!!.jsonPrimitive.content)
        assertEquals(2, matchingItem["pairs"]!!.jsonArray.size)
        assertEquals("۱←۲، ۲←۱", matchingItem["answer"]!!.jsonPrimitive.content)
        assertTrue(matchingItem["pairs"]!!.jsonArray.first().jsonObject["leftBold"]!!.jsonPrimitive.boolean)

        val numeric = OfficialPrintQuestion(1, "حاصل را بنویسید", 1.0, answerText = "42 ± 0")
        val numericItem = ExamHtmlPrintPayloadBuilder.build(printable(numeric))["questions"]!!.jsonArray.single().jsonObject
        assertEquals("numeric", numericItem["type"]!!.jsonPrimitive.content)
        assertEquals("42 ± 0", numericItem["answer"]!!.jsonPrimitive.content)

        val reset = ExamHtmlPrintPayloadBuilder.build(null)
        assertTrue(reset["reset"]!!.jsonPrimitive.boolean)
        assertFalse("questions" in reset)
    }
}
