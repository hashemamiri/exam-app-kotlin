package ir.exam.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * نسخه امن آزمون فعال بدون answer key؛ برای ادامه پس از process death.
 * deadlineEpochMs زمان محلی متناظر با expires_at سرور است و با حساب مالک جدا می‌شود.
 */
@Entity(tableName = "active_exam_sessions")
data class ActiveExamSessionEntity(
    @PrimaryKey val ownerUserId: String,
    val examId: String,
    val code: String,
    val payloadJson: String,
    val deadlineEpochMs: Long?,
    val savedAt: Long
)
