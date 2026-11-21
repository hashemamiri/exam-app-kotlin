package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankRow
import ir.exam.app.ui.bank.QuestionBankUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V380_ReferenceBankMetadataTest {
    @Test
    fun `real grade-specific lesson labels are selectable`() {
        val rows = listOf(
            ReferenceBankRow(id = 1, grade = 3, subject = "هدیه های آسمان سوم"),
            ReferenceBankRow(id = 2, grade = 3, subject = "مطالعات اجتماعی سوم"),
            ReferenceBankRow(id = 3, grade = 2, subject = "هدیه های آسمان دوم")
        )
        val state = QuestionBankUiState(referenceGrade = 3, referenceRows = rows)
        assertEquals(setOf("هدیه های آسمان سوم", "مطالعات اجتماعی سوم"), state.referenceSubjectOptions.toSet())
        assertEquals(listOf(1L), state.copy(referenceSubject = "هدیه های آسمان سوم").referenceFiltered.map { it.id })
    }

    @Test
    fun `track choices and subjects reflect loaded metadata`() {
        val rows = listOf(
            ReferenceBankRow(id = 1, grade = 10, track = "عمومی", subject = "انگلیسی (1)"),
            ReferenceBankRow(id = 2, grade = 10, track = "ریاضی فیزیک", subject = "ریاضی (1)"),
            ReferenceBankRow(id = 3, grade = 10, track = "علوم و معارف اسلامی", subject = "احکام"),
            ReferenceBankRow(id = 4, grade = 11, track = "هنرستان", subject = "نمونه")
        )
        val state = QuestionBankUiState(referenceGrade = 10, referenceRows = rows)
        assertEquals(setOf("عمومی", "ریاضی فیزیک", "علوم و معارف اسلامی"), state.referenceTrackOptions.toSet())
        assertEquals(listOf("انگلیسی (1)"), state.copy(referenceTrack = "عمومی").referenceSubjectOptions)
        assertEquals(listOf("احکام"), state.copy(referenceTrack = "معارف").referenceSubjectOptions)
        assertEquals(listOf(1L), state.copy(referenceTrack = "عمومی").referenceFiltered.map { it.id })
    }

    @Test
    fun `empty grade shows no invented subjects or tracks`() {
        val state = QuestionBankUiState(referenceGrade = 11)
        assertTrue(state.referenceTrackOptions.isEmpty())
        assertTrue(state.referenceSubjectOptions.isEmpty())
    }
}
