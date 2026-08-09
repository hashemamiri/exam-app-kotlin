package ir.exam.app.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao interface AnswerDraftDao {
 @Query("SELECT * FROM answer_drafts WHERE examId=:examId") fun observe(examId:String):Flow<AnswerDraftEntity?>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(item:AnswerDraftEntity)
 @Query("DELETE FROM answer_drafts WHERE examId=:examId") suspend fun delete(examId:String)
}
