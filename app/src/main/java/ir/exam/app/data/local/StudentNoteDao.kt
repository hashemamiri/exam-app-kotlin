package ir.exam.app.data.local
import androidx.room.*
@Dao interface StudentNoteDao{
 @Query("SELECT * FROM student_notes WHERE ownerUserId=:owner") suspend fun list(owner:String):List<StudentNoteEntity>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(item:StudentNoteEntity)
 @Query("DELETE FROM student_notes WHERE ownerUserId=:owner AND studentId=:student") suspend fun delete(owner:String,student:String)
}
