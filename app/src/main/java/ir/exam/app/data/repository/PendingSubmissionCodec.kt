package ir.exam.app.data.repository

import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.SubmittedExam
import ir.exam.app.domain.model.TextAnswer
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class PendingSubmissionPayload(
    @SerialName("operation_id") val operationId: String = UUID.randomUUID().toString(),
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("exam_id") val examId: String,
    val responses: JsonArray,
    @SerialName("response_images") val responseImages: Map<String, List<String>>,
    @SerialName("created_at") val createdAt: Long
)

object PendingSubmissionCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun fromAttempt(ownerUserId: String, exam: Exam, attempt: SubmittedExam): PendingSubmissionPayload {
        require(attempt.examId == exam.id) { "شناسه آزمون پاسخ با آزمون فعال سازگار نیست." }
        val originalIndices = exam.questions.map { it.originalIndex }
        val canonicalQuestions = if (
            originalIndices.all { it >= 0 } && originalIndices.distinct().size == exam.questions.size
        ) exam.questions.sortedBy { it.originalIndex } else exam.questions
        val responses = JsonArray(canonicalQuestions.map { question ->
            when (val answer = attempt.answers[question.id]) {
                is TextAnswer -> JsonPrimitive(answer.value)
                is ChoiceAnswer -> JsonPrimitive(answer.selectedIndex)
                is BooleanAnswer -> JsonPrimitive(answer.value)
                is MatchingAnswer -> JsonObject(
                    answer.pairs.mapKeys { it.key.toString() }.mapValues { JsonPrimitive(it.value) }
                )
                null -> JsonPrimitive("")
            }
        })
        return PendingSubmissionPayload(
            ownerUserId = ownerUserId,
            examId = exam.id,
            responses = responses,
            responseImages = attempt.responseImages,
            createdAt = attempt.submittedAtEpochMs
        )
    }

    fun encode(payload: PendingSubmissionPayload): String = json.encodeToString(payload)
    fun decode(raw: String): PendingSubmissionPayload = json.decodeFromString(raw)

    fun withImages(payload: PendingSubmissionPayload, images: Map<String, List<String>>): PendingSubmissionPayload =
        payload.copy(responseImages = images)
}
