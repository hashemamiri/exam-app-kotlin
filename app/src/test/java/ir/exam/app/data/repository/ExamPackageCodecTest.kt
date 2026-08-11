package ir.exam.app.data.repository

import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamPackageCodecTest {
    @Test
    fun `exam package round trips public data and answer key`() {
        val source = ExamPackageCodec.ExportedExam(
            title = "آزمون فرمول",
            subject = "ریاضی",
            duration = 45,
            negativeMarking = 0.25,
            shuffleQuestions = true,
            shuffleOptions = true,
            teacherMessage = "موفق باشید",
            attemptsAllowed = 2,
            attemptOnTimeout = true,
            gradePolicy = "best",
            attemptCooldown = 5,
            questions = listOf(
                QuestionDraft(
                    id = "q1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    text = "${'$'}\\frac{1}{2}${'$'} چند است؟",
                    score = 2.0,
                    options = listOf("۰٫۵", "۱", "۲", "۴"),
                    correctIndex = 0,
                    images = listOf(MediaDraft(uri = "https://example.test/q.webp"))
                )
            ),
            by = "دبیر تست"
        )
        val encoded = ExamPackageCodec.encode(source)
        val decoded = ExamPackageCodec.decode(encoded)

        assertTrue(encoded.startsWith(ExamPackageCodec.TAG))
        assertEquals(source.title, decoded.title)
        assertEquals(0, decoded.questions.single().correctIndex)
        assertEquals("best", decoded.gradePolicy)
        assertEquals("https://example.test/q.webp", decoded.questions.single().images.single().uri)
    }

    @Test
    fun `local and insecure image uris are stripped from imported package`() {
        val source = ExamPackageCodec.ExportedExam(
            "آزمون", "درس", 10, 0.0, false, false, "", 1, false, "last", 0,
            listOf(
                QuestionDraft(
                    type = QuestionType.ESSAY,
                    text = "سؤال",
                    images = listOf(MediaDraft(uri = "file:///private/image.jpg"))
                )
            ),
            ""
        )
        val decoded = ExamPackageCodec.decode(ExamPackageCodec.encode(source))
        assertTrue(decoded.questions.single().images.isEmpty())
    }

    @Test
    fun `foreign json is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExamPackageCodec.decode("{\"_app\":\"other\",\"_kind\":\"exam\",\"exam\":{}}")
        }
    }
}
