package ir.exam.app.data.repository

import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.MatchingQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.SubmittedExam
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentExamPayloadCodecTest {
    private val raw = Json.parseToJsonElement(
        """
        {
          "id":"exam-1","title":"آزمون","subject":"ریاضی","code":"ABC123","duration":20,
          "teacher_message":"با دقت پاسخ دهید","shuffle_q":true,"shuffle_opt":true,
          "server_now":"2026-08-12T10:00:00Z","expires_at":"2026-08-12T10:10:00Z",
          "questions":[
            {"i":0,"id":"q0","type":"essay","text":"تشریحی","score":2,"correctAnswer":"LEAK"},
            {"i":1,"id":"q1","type":"multiple","text":"چهارگزینه‌ای","score":1,
             "options":["الف","ب","ج","د"],"optionImages":["a","b","c","d"],"correctOption":2},
            {"i":2,"id":"q2","type":"matching","text":"جورکردنی","score":2,
             "leftItems":["چپ۱","چپ۲"],"rightItems":["راست۱","راست۲"],"matchAnswer":{"0":1,"1":0}}
          ]
        }
        """.trimIndent()
    ).jsonObject

    @Test
    fun `server deadline message and stable shuffled canonical mappings survive restore`() {
        val localNow = 1_000_000L
        val first = StudentExamPayloadCodec.decodeFresh(raw, "student-1", localNow)
        val second = StudentExamPayloadCodec.decodeFresh(raw, "student-1", localNow)

        assertEquals(first.questions.map { it.id }, second.questions.map { it.id })
        assertEquals(localNow + 600_000L, first.deadlineEpochMs)
        assertEquals("با دقت پاسخ دهید", first.teacherMessage)
        assertTrue(first.shuffleQuestions)
        assertTrue(first.shuffleOptions)

        val multiple = first.questions.filterIsInstance<MultipleChoiceQuestion>().single()
        assertEquals((0..3).toSet(), multiple.optionOriginalIndices.toSet())
        assertEquals(multiple.options.size, multiple.optionOriginalIndices.size)
        val matching = first.questions.filterIsInstance<MatchingQuestion>().single()
        assertEquals(setOf(0, 1), matching.rightOriginalIndices.toSet())

        val restored = StudentExamPayloadCodec.decodeCached(
            StudentExamPayloadCodec.sanitize(raw),
            "student-1",
            localNow + 500_000L,
            first.deadlineEpochMs
        )
        assertEquals(first.deadlineEpochMs, restored.deadlineEpochMs)
        assertEquals(first.questions.map { it.id }, restored.questions.map { it.id })
    }

    @Test
    fun `cache strips every answer key field`() {
        val safe = StudentExamPayloadCodec.sanitize(raw).toString()
        assertFalse("correctOption" in safe)
        assertFalse("correctAnswer" in safe)
        assertFalse("matchAnswer" in safe)
        assertFalse("LEAK" in safe)
    }

    @Test
    fun `submission is restored to original question order and canonical option indices`() {
        val exam = StudentExamPayloadCodec.decodeFresh(raw, "student-1", 1_000_000L)
        val multiple = exam.questions.filterIsInstance<MultipleChoiceQuestion>().single()
        val matching = exam.questions.filterIsInstance<MatchingQuestion>().single()
        val pickedOriginalOption = multiple.optionOriginalIndices.last()
        val pickedOriginalRight = matching.rightOriginalIndices.last()

        val payload = PendingSubmissionCodec.fromAttempt(
            ownerUserId = "student-1",
            exam = exam,
            attempt = SubmittedExam(
                examId = exam.id,
                answers = mapOf(
                    multiple.id to ChoiceAnswer(multiple.id, pickedOriginalOption),
                    matching.id to MatchingAnswer(matching.id, mapOf(0 to pickedOriginalRight))
                ),
                responseImages = emptyMap(),
                submittedAtEpochMs = 2_000_000L
            )
        )

        assertEquals("\"\"", payload.responses[0].toString())
        assertEquals(pickedOriginalOption.toString(), payload.responses[1].toString())
        assertTrue(payload.responses[2].toString().contains(pickedOriginalRight.toString()))
        assertEquals(listOf(2, 0, 1), exam.questions.map { it.originalIndex })
    }
}
