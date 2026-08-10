package ir.exam.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ExamWriteDto(
    val id: String,
    @SerialName("teacher_id") val teacherId: String,
    val title: String,
    val subject: String,
    val duration: Int,
    val code: String,
    @SerialName("total_score") val totalScore: Double,
    @SerialName("is_open") val isOpen: Boolean,
    @SerialName("shuffle_q") val shuffleQuestions: Boolean,
    val questions: JsonElement
)
