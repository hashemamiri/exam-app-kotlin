package ir.exam.app.data.repository

import ir.exam.app.ui.audio.isAnswerAudioUrl
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V309 / ۱۰ — پاسخ صوتی: حالت و سقف ضبط در رمزگذار، و تشخیص نشانی صوتی برای نمایش. */
class V309_VoiceAnswerTest {
    @Test
    fun `voice mode and max seconds survive encode and decode`() {
        val source = QuestionDraft(type = QuestionType.ESSAY, text = "داستان کوتاهی را بگو", mode = "voice", voiceMaxSeconds = 90)
        val encoded = ExamQuestionCodec.encode(listOf(source))
        assertTrue(encoded.publicQuestions.single().toString().contains("\"mode\":\"voice\""))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertEquals("voice", decoded.mode)
        assertEquals(90, decoded.voiceMaxSeconds)
    }

    @Test
    fun `voice question without a chosen limit stores the default of sixty seconds`() {
        val encoded = ExamQuestionCodec.encode(listOf(QuestionDraft(type = QuestionType.ESSAY, text = "x", mode = "voice")))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertEquals(60, decoded.voiceMaxSeconds)
    }

    @Test
    fun `audio answer urls are recognised and images are not`() {
        assertTrue(isAnswerAudioUrl("https://cdn.example.ir/answers/u/e/q/abc.m4a"))
        assertTrue(isAnswerAudioUrl("https://cdn.example.ir/answers/u/e/q/abc.webm?t=1"))
        assertTrue(isAnswerAudioUrl("file:/data/cache/voice_q1_1.m4a"))
        assertFalse(isAnswerAudioUrl("https://cdn.example.ir/answers/u/e/q/abc.webp"))
    }
}
