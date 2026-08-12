package ir.exam.app.data.repository

import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.StudentDraft
import ir.exam.app.domain.model.TextAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentDraftJsonCodecTest {
    @Test
    fun `answers matching pairs and image uris round trip`() {
        val source = StudentDraft(
            answers = mapOf(
                "q1" to TextAnswer("q1", "پاسخ"),
                "q2" to MatchingAnswer("q2", mapOf(0 to 2, 1 to 0))
            ),
            responseImages = mapOf("q1" to listOf("content://one", "content://two")),
            flaggedQuestionIds=setOf("q2"),lastQuestionIndex=1
        )
        val decoded = StudentDraftJsonCodec.decode(StudentDraftJsonCodec.encode(source))
        assertEquals(source, decoded)
    }

    @Test
    fun `legacy answer-only json remains readable`() {
        val legacy = """{"q1":{"type":"text","value":"قدیمی"}}"""
        val decoded = StudentDraftJsonCodec.decode(legacy)
        assertEquals("قدیمی", (decoded.answers["q1"] as TextAnswer).value)
        assertTrue(decoded.responseImages.isEmpty())
    }
}
