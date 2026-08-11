package ir.exam.app.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.jan.supabase.auth.auth
import ir.exam.app.core.network.NetworkFailureClassifier
import ir.exam.app.core.network.NetworkMonitor
import ir.exam.app.data.local.NativeDatabaseProvider
import ir.exam.app.data.local.PendingActionState
import ir.exam.app.data.local.PendingActionType
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.data.repository.PendingMediaStore
import ir.exam.app.data.repository.PendingSubmissionCodec
import ir.exam.app.data.repository.SupabaseStudentExamRepository

class PendingActionWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val database = NativeDatabaseProvider.get(applicationContext)
        val dao = database.pendingActionDao()
        if (!NetworkMonitor(applicationContext).isOnline()) return Result.retry()

        runCatching { SupabaseProvider.client.auth.awaitInitialization() }
            .getOrElse { return Result.retry() }
        val currentUserId = SupabaseProvider.client.auth.currentUserOrNull()?.id
        val remote = SupabaseStudentExamRepository(applicationContext)
        val media = PendingMediaStore(applicationContext)
        var shouldRetry = false

        dao.ready().forEach { action ->
            val now = System.currentTimeMillis()
            if (action.type != PendingActionType.SUBMIT_EXAM) {
                dao.markState(action.id, PendingActionState.FAILED, now, "نوع عملیات پشتیبانی نمی‌شود.")
                return@forEach
            }
            val payload = runCatching { PendingSubmissionCodec.decode(action.payloadJson) }
                .getOrElse {
                    dao.markState(action.id, PendingActionState.FAILED, now, "داده صف خراب است.")
                    return@forEach
                }
            if (currentUserId == null || currentUserId != action.ownerUserId || payload.ownerUserId != action.ownerUserId) {
                dao.markState(
                    action.id,
                    PendingActionState.BLOCKED_AUTH,
                    now,
                    "برای ارسال، وارد همان حساب دانش‌آموز شوید."
                )
                return@forEach
            }

            dao.markRunning(action.id, now)
            try {
                remote.sendPrepared(payload)
                dao.delete(action.id)
                database.answerDraftDao().delete(payload.examId)
                media.clear(payload.operationId)
            } catch (error: Throwable) {
                when {
                    NetworkFailureClassifier.isAlreadySubmitted(error) -> {
                        dao.delete(action.id)
                        database.answerDraftDao().delete(payload.examId)
                        media.clear(payload.operationId)
                    }
                    NetworkFailureClassifier.isAuthFailure(error) -> dao.markState(
                        action.id,
                        PendingActionState.BLOCKED_AUTH,
                        System.currentTimeMillis(),
                        safeWorkerError(error)
                    )
                    NetworkFailureClassifier.isNetworkFailure(error) -> {
                        dao.markState(
                            action.id,
                            PendingActionState.PENDING,
                            System.currentTimeMillis(),
                            safeWorkerError(error)
                        )
                        shouldRetry = true
                    }
                    else -> dao.markState(
                        action.id,
                        PendingActionState.FAILED,
                        System.currentTimeMillis(),
                        safeWorkerError(error)
                    )
                }
            }
        }
        return if (shouldRetry) Result.retry() else Result.success()
    }
}

private fun safeWorkerError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .replace(Regex("https?://\\S+"), "")
    .take(220)
    .ifBlank { "ارسال پس‌زمینه ناموفق بود." }
