package ir.exam.app.data.repository

import ir.exam.app.ui.builder.AlignSpan
import ir.exam.app.ui.builder.AssessmentPartDraft
import ir.exam.app.ui.builder.ConstructionTargetDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamQuestionCodecTest {
    @Test
    fun `answer keys are removed from public questions`() {
        val encoded = ExamQuestionCodec.encode(
            listOf(
                QuestionDraft(
                    type = QuestionType.MULTIPLE_CHOICE,
                    text = "دو بعلاوه دو؟",
                    options = listOf("۳", "۴", "۵", "۶"),
                    correctIndex = 1
                )
            )
        )

        val public = encoded.publicQuestions.first() as JsonObject
        val key = encoded.answerKey.first() as JsonObject
        assertFalse(public.containsKey("correctOption"))
        assertEquals(1, (key["correctOption"] as JsonPrimitive).content.toInt())
    }

    @Test
    fun `matching pairs option images and response image policy round trip`() {
        val source = QuestionDraft(
            type = QuestionType.MATCHING,
            text = "وصل کنید",
            matchingLeft = listOf("الف", "ب"),
            matchingRight = listOf("۱", "۲"),
            matchingPairs = mapOf(0 to 1, 1 to 0),
            matchingLeftImages = listOf("https://example.test/l.webp", null),
            answerImageMode = "required",
            maxAnswerImages = 2
        )
        val encoded = ExamQuestionCodec.encode(listOf(source))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()

        assertEquals(source.matchingPairs, decoded.matchingPairs)
        assertEquals("required", decoded.answerImageMode)
        assertEquals(2, decoded.maxAnswerImages)
        assertEquals(source.matchingLeftImages, decoded.matchingLeftImages)
    }

    @Test
    fun `print style answer lines and case sensitivity round trip`() {
        val source=QuestionDraft(type=QuestionType.FILL_BLANK,text="Name",expectedText="Tehran",caseSensitive=true,textAlign="center",imagePosition="free",fontFamily="shabnam",fontSizeSp=21f,bold=true,italic=true,answerLines=7,answerLineStyle="blank")
        val encoded=ExamQuestionCodec.encode(listOf(source));val decoded=ExamQuestionCodec.decode(encoded.publicQuestions,encoded.answerKey).single()
        assertTrue(decoded.caseSensitive);assertEquals("center",decoded.textAlign);assertEquals("free",decoded.imagePosition);assertEquals("shabnam",decoded.fontFamily);assertEquals(21f,decoded.fontSizeSp);assertTrue(decoded.bold);assertTrue(decoded.italic);assertEquals(7,decoded.answerLines);assertEquals("blank",decoded.answerLineStyle)
    }

    @Test
    fun `align spans round trip and legacy JSON without them decodes to empty list`() {
        // V121 — تراز پاراگرافیِ تکه‌ای؛ باید سالم رفت‌و‌برگشت کند و JSON قدیمی
        // (بدونِ کلید alignSpans) نباید کرش کند یا مقداری جعلی بسازد.
        val source = QuestionDraft(
            type = QuestionType.ESSAY,
            text = "بند اول\nبند دوم\nبند سوم",
            textAlign = "right",
            alignSpans = listOf(AlignSpan(0, 8, "center"), AlignSpan(9, 17, "justify"))
        )
        val encoded = ExamQuestionCodec.encode(listOf(source))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertEquals(source.alignSpans, decoded.alignSpans)

        val legacyPublic = JsonArray(listOf(JsonObject(mapOf(
            "type" to JsonPrimitive("essay"),
            "text" to JsonPrimitive("بدون بازهٔ تراز")
        ))))
        val legacyDecoded = ExamQuestionCodec.decode(legacyPublic, JsonArray(emptyList())).single()
        assertTrue(legacyDecoded.alignSpans.isEmpty())
    }

    @Test
    fun `legacy public question and separate key are decoded`() {
        val public = JsonArray(listOf(JsonObject(mapOf(
            "type" to JsonPrimitive("fill"),
            "text" to JsonPrimitive("پایتخت ایران"),
            "score" to JsonPrimitive(2),
            "images" to JsonArray(listOf(JsonPrimitive("https://example.test/q.webp")))
        ))))
        val keys = JsonArray(listOf(JsonObject(mapOf(
            "i" to JsonPrimitive(0),
            "accept" to JsonArray(listOf(JsonPrimitive("تهران"), JsonPrimitive("طهران")))
        ))))

        val decoded = ExamQuestionCodec.decode(public, keys).single()
        assertEquals(QuestionType.FILL_BLANK, decoded.type)
        assertEquals("تهران|طهران", decoded.expectedText)
        assertEquals(1, decoded.images.size)
        assertTrue(decoded.images.first().uri.startsWith("https://"))
    }
    @Test
    fun `assessment parts and private rubrics round trip without exposing rubrics`() {
        val source = QuestionDraft(
            type = QuestionType.ESSAY,
            text = "به پرسش‌های زیر پاسخ دهید.",
            score = 5.0,
            mode = "proof",
            proofAssumptions = "AB = AC",
            proofGoal = "زاویه‌های پایه برابرند",
            assessmentParts = listOf(
                AssessmentPartDraft(id = "p-a", label = "الف", prompt = "معادله را حل کنید", score = 2.0, rubric = "روش درست", carriesForward = false, reasonPrompt = "دلیل", requiresReason = true),
                AssessmentPartDraft(id = "p-b", label = "ب", prompt = "نتیجهٔ بخش قبل را به‌کار ببرید", score = 3.0, rubric = "استدلال کامل", carriesForward = true, reasonPrompt = "قضیه", requiresReason = true)
            )
        )
        val encoded = ExamQuestionCodec.encode(listOf(source))
        val public = encoded.publicQuestions.single() as JsonObject
        val key = encoded.answerKey.single() as JsonObject
        assertTrue(public.containsKey("assessmentParts"))
        assertEquals("AB = AC", (public["proofAssumptions"] as JsonPrimitive).content)
        assertEquals("زاویه‌های پایه برابرند", (public["proofGoal"] as JsonPrimitive).content)
        assertTrue(((public["assessmentParts"] as JsonArray).first() as JsonObject).containsKey("reasonPrompt"))
        assertFalse(((public["assessmentParts"] as JsonArray).first() as JsonObject).containsKey("rubric"))
        assertFalse(public.containsKey("partRubrics"))
        assertEquals(listOf("روش درست", "استدلال کامل"), (key["partRubrics"] as JsonArray).map { (it as JsonPrimitive).content })
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertEquals(source.assessmentParts, decoded.assessmentParts)
    }

    @Test
    fun `construction point targets remain private and round trip through answer key`() {
        val source = QuestionDraft(
            type = QuestionType.ESSAY,
            text = "نقطهٔ A را روی دستگاه مختصات مشخص کنید.",
            score = 2.0,
            mode = "construct",
            constructionPrompt = "نقطه‌ها را دقیق روی نمودار بگذارید.",
            constructionTargets = listOf(ConstructionTargetDraft(label = "A", x = 2.0, y = -1.0, tolerance = 0.35, score = 2.0))
        )
        val encoded = ExamQuestionCodec.encode(listOf(source))
        val public = encoded.publicQuestions.single() as JsonObject
        val key = encoded.answerKey.single() as JsonObject
        assertEquals(listOf("A"), (public["constructionLabels"] as JsonArray).map { (it as JsonPrimitive).content })
        assertFalse(public.containsKey("constructionTargets"))
        assertEquals(2.0, ((key["constructionTargets"] as JsonArray).single() as JsonObject)["x"]?.let { (it as JsonPrimitive).content.toDouble() } ?: 0.0, 0.0)
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertEquals(source.constructionTargets, decoded.constructionTargets)
    }

}
