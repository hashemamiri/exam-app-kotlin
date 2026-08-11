package ir.exam.app.data.repository

import ir.exam.app.core.network.NetworkFailureClassifier
import ir.exam.app.core.network.NetworkMonitor
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.SubmittedExam
import ir.exam.app.domain.repository.ExamRepository

/** ارسال مستقیم در اینترنت سالم؛ در خطای شبکه، همان payload در Room و WorkManager صف می‌شود. */
class QueuedExamRepository(
    private val remote: SupabaseStudentExamRepository,
    private val network: NetworkMonitor,
    private val pending: PendingActionRepository
) : ExamRepository {
    override suspend fun joinByCode(code: String): Result<Exam> = remote.joinByCode(code)

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
}
