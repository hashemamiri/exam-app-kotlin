package ir.exam.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

@Serializable
data class ExamDetailDto(
    val id: String,
    val title: String = "",
    val subject: String? = null,
    val questions: JsonElement = JsonArray(emptyList()),
    val code: String? = null,
    @SerialName("total_score") val totalScore: Double = 0.0,
    @SerialName("is_open") val isOpen: Boolean = false,
    val duration: Int? = null,
    @SerialName("shuffle_q") val shuffleQuestions: Boolean = false,
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
    @SerialName("attempt_cooldown") val attemptCooldown: Int = 0
)

@Serializable
data class ExamUpdateDto(
    val title: String,
    val subject: String,
    val duration: Int,
    @SerialName("total_score") val totalScore: Double,
    val questions: JsonElement,
    @SerialName("shuffle_q") val shuffleQuestions: Boolean,
    @SerialName("shuffle_opt") val shuffleOptions: Boolean,
    @SerialName("neg_marking") val negativeMarking: Double,
    @SerialName("teacher_message") val teacherMessage: String?,
    @SerialName("attempts_allowed") val attemptsAllowed: Int,
    @SerialName("attempt_on_timeout") val attemptOnTimeout: Boolean,
    @SerialName("grade_policy") val gradePolicy: String,
    @SerialName("attempt_cooldown") val attemptCooldown: Int
)

@Serializable
data class ExamKeyDto(
    @SerialName("exam_id") val examId: String,
    val answers: JsonElement = JsonArray(emptyList())
)
