package ir.exam.app.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
@Database(entities=[AnswerDraftEntity::class],version=1,exportSchema=false)
abstract class AppDatabase:RoomDatabase(){abstract fun answerDraftDao():AnswerDraftDao}
