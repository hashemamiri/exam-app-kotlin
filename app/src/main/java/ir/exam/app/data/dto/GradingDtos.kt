package ir.exam.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class AnswerDto(
    val id: String,
    @SerialName("exam_id") val examId: String = "",
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("student_name") val studentName: String = "",
    val responses: JsonElement = JsonArray(emptyList()),
    val grades: JsonElement? = null,
    @SerialName("total_grade") val totalGrade: Double = 0.0,
    val feedback: String = "",
    val graded: Boolean = false,
    @SerialName("auto_graded") val autoGraded: Boolean = false,
    @SerialName("response_images") val responseImages: JsonElement = JsonObject(emptyMap()),
    @SerialName("attempt_no") val attemptNo: Int = 1,
    @SerialName("submitted_at") val submittedAt: String? = null
)

@Serializable
data class FeedbackPhraseDto(val id: Long, val text: String)

@Serializable
data class AttendanceDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("full_name") val fullName: String = "",
    val username: String? = null,
    val status: String = "absent",
    @SerialName("total_grade") val totalGrade: Double? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("minutes_left") val minutesLeft: Int? = null,
    val attempts: Int = 0,
    @SerialName("attempts_allowed") val attemptsAllowed: Int = 1,
    val abandoned: Int = 0
)
