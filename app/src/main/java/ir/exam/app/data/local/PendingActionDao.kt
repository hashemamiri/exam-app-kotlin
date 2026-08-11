package ir.exam.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {
    @Query(
        "SELECT * FROM pending_actions WHERE ownerUserId=:userId " +
            "ORDER BY createdAt ASC"
    )
    fun observeForUser(userId: String): Flow<List<PendingActionEntity>>

    @Query(
        "SELECT * FROM pending_actions WHERE state IN ('pending','running') " +
            "ORDER BY createdAt ASC LIMIT :limit"
    )
    suspend fun ready(limit: Int = 20): List<PendingActionEntity>

    @Query("SELECT * FROM pending_actions WHERE dedupeKey=:dedupeKey LIMIT 1")
    suspend fun byDedupeKey(dedupeKey: String): PendingActionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: PendingActionEntity)

    @Query(
        "UPDATE pending_actions SET payloadJson=:payloadJson, state='pending', " +
            "updatedAt=:updatedAt, lastError=NULL WHERE id=:id"
    )
    suspend fun replacePayload(id: String, payloadJson: String, updatedAt: Long)

    @Query(
        "UPDATE pending_actions SET state='running', attempts=attempts+1, " +
            "updatedAt=:updatedAt, lastError=NULL WHERE id=:id"
    )
    suspend fun markRunning(id: String, updatedAt: Long)

    @Query(
        "UPDATE pending_actions SET state=:state, updatedAt=:updatedAt, lastError=:error WHERE id=:id"
    )
    suspend fun markState(id: String, state: String, updatedAt: Long, error: String?)

    @Query(
        "UPDATE pending_actions SET state='pending', updatedAt=:updatedAt, lastError=NULL " +
            "WHERE ownerUserId=:userId AND state IN ('blocked_auth','failed')"
    )
    suspend fun resetForUser(userId: String, updatedAt: Long)

    @Query("DELETE FROM pending_actions WHERE id=:id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM pending_actions WHERE ownerUserId=:userId AND state='failed'")
    suspend fun failedForUser(userId: String): List<PendingActionEntity>

    @Query("DELETE FROM pending_actions WHERE ownerUserId=:userId AND state='failed'")
    suspend fun deleteFailed(userId: String)

    @Transaction
    suspend fun enqueueOrReplace(item: PendingActionEntity): String {
        val existing = byDedupeKey(item.dedupeKey)
        return if (existing == null) {
            insert(item)
            item.id
        } else {
            replacePayload(existing.id, item.payloadJson, item.updatedAt)
            existing.id
        }
    }
}
