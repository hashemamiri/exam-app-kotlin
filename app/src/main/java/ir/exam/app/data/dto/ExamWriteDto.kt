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
    @SerialName("shuffle_opt") val shuffleOptions: Boolean = false,
    @SerialName("neg_marking") val negativeMarking: Double = 0.0,
    @SerialName("opens_at") val opensAt: String? = null,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("class_id") val classId: String? = null,
    val audience: String = "all",
    @SerialName("teacher_message") val teacherMessage: String? = null,
    @SerialName("attempts_allowed") val attemptsAllowed: Int = 1,
    @SerialName("attempt_on_timeout") val attemptOnTimeout: Boolean = false,
    @SerialName("grade_policy") val gradePolicy: String = "last",
    @SerialName("attempt_cooldown") val attemptCooldown: Int = 0,
    val questions: JsonElement
)
