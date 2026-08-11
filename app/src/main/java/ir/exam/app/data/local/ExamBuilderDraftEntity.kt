package ir.exam.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_builder_drafts")
data class ExamBuilderDraftEntity(
    @PrimaryKey val ownerUserId: String,
    val examId: String?,
    val payloadJson: String,
    val updatedAt: Long
)
