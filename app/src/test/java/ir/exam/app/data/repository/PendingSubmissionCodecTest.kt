package ir.exam.app.data.repository

import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.MatchingQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.SubmittedExam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingSubmissionCodecTest {
    @Test
    fun `ordered responses images owner and operation survive process restart`() {
        val exam = Exam(
            id = "exam-1",
            title = "آزمون",
            code = "ABC123",
            durationMinutes = 20,
            questions = listOf(
                MultipleChoiceQuestion("q1", "گزینه", 1.0, listOf("الف", "ب")),
                MatchingQuestion("q2", "وصل", 2.0, listOf("چپ"), listOf("راست"))
            )
        )
        val payload = PendingSubmissionCodec.fromAttempt(
            ownerUserId = "student-1",
            exam = exam,
            attempt = SubmittedExam(
                examId = exam.id,
                answers = mapOf(
                    "q1" to ChoiceAnswer("q1", 1),
                    "q2" to MatchingAnswer("q2", mapOf(0 to 0))
                ),
                responseImages = mapOf("q2" to listOf("content://answer/one")),
                submittedAtEpochMs = 1234L
            )
        )
        val restored = PendingSubmissionCodec.decode(PendingSubmissionCodec.encode(payload))

        assertEquals(payload, restored)
        assertEquals("1", restored.responses[0].toString())
        assertTrue(restored.operationId.isNotBlank())
    }
}
