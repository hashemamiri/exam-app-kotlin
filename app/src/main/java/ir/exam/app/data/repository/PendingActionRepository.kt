package ir.exam.app.data.repository

import android.content.Context
import ir.exam.app.data.local.PendingActionDao
import ir.exam.app.data.local.PendingActionEntity
import ir.exam.app.data.local.PendingActionState
import ir.exam.app.data.local.PendingActionType
import ir.exam.app.data.work.PendingActionScheduler
import ir.exam.app.domain.model.PendingSubmissionStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PendingActionRepository(
    context: Context,
    private val dao: PendingActionDao,
    private val mediaStore: PendingMediaStore = PendingMediaStore(context),
    private val scheduler: PendingActionScheduler = PendingActionScheduler(context)
) {
    suspend fun enqueueSubmission(payload: PendingSubmissionPayload): String {
        val dedupeKey = "submit:${payload.ownerUserId}:${payload.examId}"
        val previous = dao.byDedupeKey(dedupeKey)
        val durable = mediaStore.materialize(payload)
        val now = System.currentTimeMillis()
        val item = PendingActionEntity(
            id = UUID.randomUUID().toString(),
            dedupeKey = dedupeKey,
            ownerUserId = durable.ownerUserId,
            type = PendingActionType.SUBMIT_EXAM,
            payloadJson = PendingSubmissionCodec.encode(durable),
            createdAt = now,
            updatedAt = now
        )
        val id = dao.enqueueOrReplace(item)
        previous?.let { old ->
            runCatching { PendingSubmissionCodec.decode(old.payloadJson).operationId }
                .getOrNull()?.takeIf { it != durable.operationId }?.let(mediaStore::clear)
        }
        scheduler.schedule()
        return id
    }

    fun observeSubmissions(userId: String): Flow<List<PendingSubmissionStatus>> =
        dao.observeForUser(userId).map { rows ->
            rows.filter { it.type == PendingActionType.SUBMIT_EXAM }.mapNotNull { row ->
                val examId = runCatching { PendingSubmissionCodec.decode(row.payloadJson).examId }.getOrNull()
                    ?: return@mapNotNull null
                PendingSubmissionStatus(
                    id = row.id,
                    examId = examId,
                    state = row.state,
                    attempts = row.attempts,
                    createdAt = row.createdAt,
                    lastError = row.lastError
                )
            }
        }

    suspend fun retryAll(userId: String) {
        dao.resetForUser(userId, System.currentTimeMillis())
        scheduler.retryNow()
    }

    suspend fun deleteFailed(userId: String) {
        dao.failedForUser(userId).forEach { row ->
            runCatching { PendingSubmissionCodec.decode(row.payloadJson).operationId }
                .getOrNull()?.let(mediaStore::clear)
        }
        dao.deleteFailed(userId)
    }

    fun schedule() = scheduler.schedule()
    fun clearMedia(operationId: String) = mediaStore.clear(operationId)
}
