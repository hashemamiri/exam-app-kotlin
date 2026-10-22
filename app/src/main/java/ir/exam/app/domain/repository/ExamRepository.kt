package ir.exam.app.domain.repository

import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.ServerSectionSync
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.SubmittedExam

interface ExamRepository {
    suspend fun joinByCode(code: String): Result<Exam>
    suspend fun restoreActiveExam(): Result<Exam?> = Result.success(null)
    suspend fun refreshActiveExam(): Result<Exam?> = Result.success(null)
    suspend fun clearActiveExam(examId: String): Result<Unit> = Result.success(Unit)
    suspend fun submitAttempt(attempt: SubmittedExam): Result<SubmissionOutcome>

    /** همگام‌سازی زمان شروع/قفل هر بخش در سرور و ثبت snapshot پاسخ‌های بخش پیش از اتمام مهلت. */
    suspend fun syncSectionTiming(
        examId: String,
        sectionTitle: String,
        sectionSnapshot: Map<Int, String> = emptyMap()
    ): Result<ServerSectionSync?> = Result.success(null)

    /** V58.0 — ثبت گزارش نظارتی آزمون (best-effort؛ فقط معلم می‌خواند). */
    suspend fun reportMonitor(examId: String, reportJson: String): Result<Unit> = Result.success(Unit)
}
