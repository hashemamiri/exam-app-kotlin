package ir.exam.app.domain.repository

import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.SubmittedExam

interface ExamRepository {
    suspend fun joinByCode(code: String): Result<Exam>
    suspend fun restoreActiveExam(): Result<Exam?> = Result.success(null)
    suspend fun refreshActiveExam(): Result<Exam?> = Result.success(null)
    suspend fun clearActiveExam(examId: String): Result<Unit> = Result.success(Unit)
    suspend fun submitAttempt(attempt: SubmittedExam): Result<SubmissionOutcome>
}
