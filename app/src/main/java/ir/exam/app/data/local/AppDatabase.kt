package ir.exam.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AnswerDraftEntity::class,
        PendingActionEntity::class,
        ExamBuilderDraftEntity::class,
        ActiveExamSessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun answerDraftDao(): AnswerDraftDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun examBuilderDraftDao(): ExamBuilderDraftDao
    abstract fun activeExamSessionDao(): ActiveExamSessionDao
}
