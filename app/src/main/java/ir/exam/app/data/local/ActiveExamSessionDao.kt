package ir.exam.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActiveExamSessionDao {
    @Query("SELECT * FROM active_exam_sessions WHERE ownerUserId=:ownerUserId LIMIT 1")
    suspend fun find(ownerUserId: String): ActiveExamSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ActiveExamSessionEntity)

    @Query("DELETE FROM active_exam_sessions WHERE ownerUserId=:ownerUserId")
    suspend fun deleteForOwner(ownerUserId: String)

    @Query("DELETE FROM active_exam_sessions WHERE ownerUserId=:ownerUserId AND examId=:examId")
    suspend fun delete(ownerUserId: String, examId: String)
}
