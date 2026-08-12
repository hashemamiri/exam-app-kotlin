package ir.exam.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object NativeDatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "exam-native.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pending_actions (
                    id TEXT NOT NULL PRIMARY KEY,
                    dedupeKey TEXT NOT NULL,
                    ownerUserId TEXT NOT NULL,
                    type TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    state TEXT NOT NULL,
                    attempts INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    lastError TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_pending_actions_dedupeKey " +
                    "ON pending_actions(dedupeKey)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_pending_actions_ownerUserId_state_createdAt " +
                    "ON pending_actions(ownerUserId, state, createdAt)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS exam_builder_drafts (
                    ownerUserId TEXT NOT NULL PRIMARY KEY,
                    examId TEXT,
                    payloadJson TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS active_exam_sessions (
                    ownerUserId TEXT NOT NULL PRIMARY KEY,
                    examId TEXT NOT NULL,
                    code TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    deadlineEpochMs INTEGER,
                    savedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }
    val MIGRATION_3_4=object:Migration(3,4){override fun migrate(db:SupportSQLiteDatabase){db.execSQL("""
        CREATE TABLE IF NOT EXISTS student_notes(
            ownerUserId TEXT NOT NULL,
            studentId TEXT NOT NULL,
            note TEXT NOT NULL,
            updatedAt INTEGER NOT NULL,
            PRIMARY KEY(ownerUserId,studentId)
        )
    """.trimIndent())}}
}
