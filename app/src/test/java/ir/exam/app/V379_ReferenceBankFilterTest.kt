package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankRow
import ir.exam.app.ui.bank.QuestionBankUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V379_ReferenceBankFilterTest {
    @Test
    fun `first grade lists only lessons actually in its rows`() {
        val state = QuestionBankUiState(referenceGrade = 1, referenceRows = listOf(
            ReferenceBankRow(id = 1, grade = 1, subject = "ریاضی"),
            ReferenceBankRow(id = 2, grade = 1, subject = "فارسی"),
            ReferenceBankRow(id = 3, grade = 1, subject = "علوم تجربی"),
            ReferenceBankRow(id = 5, grade = 2, subject = "قرآن")
        ))
        assertEquals(setOf("ریاضی", "فارسی", "علوم تجربی"), state.referenceSubjectOptions.toSet())
    }

    @Test
    fun `empty first grade does not invent lessons`() {
        assertTrue(QuestionBankUiState(referenceGrade = 1).referenceSubjectOptions.isEmpty())
    }
}
