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
        ).addMigrations(MIGRATION_1_2).build().also { instance = it }
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
}
