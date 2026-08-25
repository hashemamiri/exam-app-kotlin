package ir.exam.app.data.repository

import ir.exam.app.core.network.NetworkFailureClassifier
import ir.exam.app.core.network.NetworkMonitor
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.SubmittedExam
import ir.exam.app.domain.repository.ExamRepository

/**
 * ورود نخست به آزمون نیازمند سرور است؛ آزمون امنی که قبلاً باز شده در Room قابل ادامه است.
 * ارسال مستقیم در اینترنت سالم و در خطای شبکه با همان payload در WorkManager صف می‌شود.
 */
class QueuedExamRepository(
    private val remote: SupabaseStudentExamRepository,
    private val network: NetworkMonitor,
    private val pending: PendingActionRepository
) : ExamRepository {
    override suspend fun joinByCode(code: String): Result<Exam> {
        if (!network.isOnline()) return cachedOrOfflineError(code)
        val result = remote.joinByCode(code)
        val error = result.exceptionOrNull()
        return if (error != null && NetworkFailureClassifier.isNetworkFailure(error)) {
            cachedOrOfflineError(code)
        } else result
    }

    override suspend fun restoreActiveExam(): Result<Exam?> = remote.restoreActiveExam()

    override suspend fun refreshActiveExam(): Result<Exam?> =
        if (network.isOnline()) remote.refreshActiveExam() else remote.restoreActiveExam()

    override suspend fun clearActiveExam(examId: String): Result<Unit> = remote.clearActiveExam(examId)

    override suspend fun reportMonitor(examId: String, reportJson: String): Result<Unit> =
        if (network.isOnline()) remote.reportMonitor(examId, reportJson) else Result.success(Unit)

    override suspend fun submitAttempt(attempt: SubmittedExam): Result<SubmissionOutcome> = runCatching {
        val payload = remote.prepareSubmission(attempt)
        if (!network.isOnline()) {
            return@runCatching SubmissionOutcome.Queued(pending.enqueueSubmission(payload))
        }
        try {
            remote.sendPrepared(payload)
        } catch (error: Throwable) {
            when {
                NetworkFailureClassifier.isAlreadySubmitted(error) -> SubmissionOutcome.Sent()
                NetworkFailureClassifier.isNetworkFailure(error) ->
                    SubmissionOutcome.Queued(pending.enqueueSubmission(payload))
                else -> throw error
            }
        }
    }

    private suspend fun cachedOrOfflineError(code: String): Result<Exam> {
        val cached = remote.cachedExamByCode(code).getOrElse { return Result.failure(it) }
        return if (cached != null) Result.success(cached)
        else Result.failure(IllegalStateException("برای اولین ورود به این آزمون اتصال اینترنت لازم است."))
    }
}
