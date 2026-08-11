package ir.exam.app.data.repository

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
}
