package ir.exam.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExamBuilderDraftDao {
    @Query("SELECT * FROM exam_builder_drafts WHERE ownerUserId=:userId LIMIT 1")
    suspend fun get(userId: String): ExamBuilderDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExamBuilderDraftEntity)

    @Query("DELETE FROM exam_builder_drafts WHERE ownerUserId=:userId")
    suspend fun delete(userId: String)
}
