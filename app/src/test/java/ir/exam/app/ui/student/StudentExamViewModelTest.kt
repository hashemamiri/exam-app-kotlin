package ir.exam.app.ui.student

import ir.exam.app.data.repository.InMemoryAnswerDraftRepository
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.StudentDraft
import ir.exam.app.domain.model.SubmittedExam
import ir.exam.app.domain.model.TextAnswer
import ir.exam.app.domain.repository.ExamRepository
import ir.exam.app.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudentExamViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `required response image blocks submit until image exists`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeExamRepository()
        val viewModel = StudentExamViewModel(repository, InMemoryAnswerDraftRepository())
        viewModel.setCode("ABC123")
        viewModel.join()
        advanceUntilIdle()

        viewModel.submit()
        advanceUntilIdle()
        assertFalse(repository.submitted)
        assertTrue(viewModel.state.value.error.orEmpty().contains("اجباری"))

        viewModel.addResponseImages("q1", listOf("content://answer/1"))
        advanceUntilIdle()
        viewModel.submit()
        advanceUntilIdle()
        assertTrue(repository.submitted)
        assertEquals(listOf("content://answer/1"), repository.lastAttempt?.responseImages?.get("q1"))
    }

    @Test
    fun `queued submission finishes exam but reports durable pending state`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeExamRepository(SubmissionOutcome.Queued("action-1"))
        val viewModel = StudentExamViewModel(repository, InMemoryAnswerDraftRepository())
        viewModel.setCode("ABC123")
        viewModel.join()
        advanceUntilIdle()
        viewModel.addResponseImages("q1", listOf("content://answer/1"))
        advanceUntilIdle()
        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.finished)
        assertTrue(viewModel.state.value.queued)
        assertTrue(viewModel.state.value.submissionMessage.orEmpty().contains("صف"))
        assertTrue(repository.activeCleared)
    }

    @Test
    fun `process restart restores active exam and draft without resetting session`() = runTest(mainDispatcherRule.dispatcher) {
        val drafts = InMemoryAnswerDraftRepository()
        drafts.save(
            "e1",
            StudentDraft(answers = mapOf("q1" to TextAnswer("q1", "پاسخ بازیابی‌شده")))
        )
        val repository = FakeExamRepository(restoreOnStart = true)
        val viewModel = StudentExamViewModel(repository, drafts)
        advanceUntilIdle()

        assertEquals("e1", viewModel.state.value.exam?.id)
        assertTrue(viewModel.state.value.resumedExam)
        assertTrue(viewModel.state.value.showPreview)
        assertEquals(
            "پاسخ بازیابی‌شده",
            (viewModel.state.value.answers["q1"] as TextAnswer).value
        )
    }
}

private class FakeExamRepository(
    private val outcome: SubmissionOutcome = SubmissionOutcome.Sent("TEST-RECEIPT"),
    private val restoreOnStart: Boolean = false
) : ExamRepository {
    var submitted = false
    var activeCleared = false
    var lastAttempt: SubmittedExam? = null
    private val exam = Exam(
        id = "e1",
        title = "آزمون",
        code = "ABC123",
        durationMinutes = 10,
        questions = listOf(
            EssayQuestion(
                id = "q1",
                text = "پاسخ تصویری",
                score = 1.0,
                maxAnswerImages = 2,
                answerImagesRequired = true
            )
        )
    )

    override suspend fun joinByCode(code: String) = Result.success(exam)
    override suspend fun restoreActiveExam(): Result<Exam?> =
        Result.success(if (restoreOnStart) exam else null)
    override suspend fun clearActiveExam(examId: String): Result<Unit> {
        activeCleared = true
        return Result.success(Unit)
    }
    override suspend fun submitAttempt(attempt: SubmittedExam): Result<SubmissionOutcome> {
        submitted = true
        lastAttempt = attempt
        return Result.success(outcome)
    }
}
