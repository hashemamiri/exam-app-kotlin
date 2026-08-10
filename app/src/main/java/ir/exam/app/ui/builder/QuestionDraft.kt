package ir.exam.app.ui.builder

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
enum class QuestionType { ESSAY, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, NUMERIC, MATCHING }

@Serializable
data class MediaDraft(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val xMm: Float = 20f,
    val yMm: Float = 30f,
    val widthMm: Float = 55f
)

@Serializable
data class QuestionDraft(
    val id: String = UUID.randomUUID().toString(),
    val type: QuestionType,
    val text: String = "",
    val score: Double = 1.0,
    val options: List<String> = emptyList(),
    val correctIndex: Int? = null,
    val expectedText: String = "",
    val expectedNumber: String = "",
    val tolerance: String = "0",
    val images: List<MediaDraft> = emptyList()
)

data class ExamBuilderState(
    val title: String = "", val subject: String = "", val durationMinutes: String = "",
    val questions: List<QuestionDraft> = emptyList(), val saving: Boolean = false,
    val savedCode: String? = null, val error: String? = null
)
