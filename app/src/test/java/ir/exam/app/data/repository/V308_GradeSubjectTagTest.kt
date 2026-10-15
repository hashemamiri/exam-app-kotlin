package ir.exam.app.data.repository

import ir.exam.app.ui.builder.PrimaryBank
import ir.exam.app.ui.builder.PrimaryBankItem
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

/** V308 / ۱۴ — برچسب پایه و درس سؤال: ذخیره در رمزگذار، بازخوانی و انتقال از بانک ابتدایی. */
class V308_GradeSubjectTagTest {
    @Test
    fun `grade and subject survive encode and decode`() {
        val source = QuestionDraft(type = QuestionType.FILL_BLANK, text = "۲ + ۲ = ?", expectedText = "۴", grade = 2, subject = "ریاضی")
        val encoded = ExamQuestionCodec.encode(listOf(source))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertEquals(2, decoded.grade)
        assertEquals("ریاضی", decoded.subject)
    }

    @Test
    fun `empty tags are not written and decode to defaults`() {
        val encoded = ExamQuestionCodec.encode(listOf(QuestionDraft(type = QuestionType.FILL_BLANK, text = "x", expectedText = "y")))
        val json = encoded.publicQuestions.single().toString() + encoded.answerKey.single().toString()
        assertFalse(json.contains("\"grade\""))
        assertFalse(json.contains("\"subject\""))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertEquals(0, decoded.grade)
        assertEquals("", decoded.subject)
    }

    @Test
    fun `primary bank draft carries its grade and subject`() {
        val item = PrimaryBankItem(id = "t1", grade = 4, subject = "علوم", type = "truefalse", text = "آب جوش است", options = emptyList(), correct = null, answer = "true")
        val draft = PrimaryBank.toDraft(item)
        assertNotNull(draft)
        assertEquals(4, draft!!.grade)
        assertEquals("علوم", draft.subject)
        assertEquals(listOf("ریاضی", "فارسی", "علوم", "هدیه‌های آسمان", "مطالعات اجتماعی"), PrimaryBank.SUBJECTS)
    }
}
