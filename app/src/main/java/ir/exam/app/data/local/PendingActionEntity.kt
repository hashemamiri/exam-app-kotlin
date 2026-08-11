package ir.exam.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** صف پایدار عملیات شبکه؛ payload رمز عبور، JWT یا token ندارد. */
@Entity(
    tableName = "pending_actions",
    indices = [
        Index(value = ["dedupeKey"], unique = true),
        Index(value = ["ownerUserId", "state", "createdAt"])
    ]
)
data class PendingActionEntity(
    @PrimaryKey val id: String,
    val dedupeKey: String,
    val ownerUserId: String,
    val type: String,
    val payloadJson: String,
    val state: String = PendingActionState.PENDING,
    val attempts: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val lastError: String? = null
)

object PendingActionType {
    const val SUBMIT_EXAM = "submit_exam"
}

object PendingActionState {
    const val PENDING = "pending"
    const val RUNNING = "running"
    const val BLOCKED_AUTH = "blocked_auth"
    const val FAILED = "failed"
}
