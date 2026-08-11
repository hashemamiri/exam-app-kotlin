package ir.exam.app.data.repository

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.MatchingQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.NumericQuestion
import ir.exam.app.domain.model.Question
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.SubmittedExam
import ir.exam.app.domain.model.TrueFalseQuestion
import ir.exam.app.domain.repository.ExamRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseStudentExamRepository(context: Context) : ExamRepository {
    private var activeExam: Exam? = null
    private val uploader = SupabaseQuestionImageUploader(context)

    override suspend fun joinByCode(code: String): Result<Exam> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "get_exam_for_student",
            buildJsonObject { put("p_code", code.trim()) }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.let(::error)
        val questions = raw["questions"]?.jsonArray.orEmpty().mapIndexed { index, item -> parseQuestion(index, item.jsonObject) }
        Exam(
            id = raw["id"]!!.jsonPrimitive.content,
            title = raw["title"]?.jsonPrimitive?.content.orEmpty(),
            code = raw["code"]?.jsonPrimitive?.content.orEmpty(),
            durationMinutes = raw["duration"]?.jsonPrimitive?.intOrNull ?: 0,
            questions = questions
        ).also { activeExam = it }
    }

    fun prepareSubmission(attempt: SubmittedExam): PendingSubmissionPayload {
        val exam = activeExam ?: error("آزمون فعال پیدا نشد")
        val studentId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("نشست دانش‌آموز پیدا نشد")
        return PendingSubmissionCodec.fromAttempt(studentId, exam, attempt)
    }

    suspend fun sendPrepared(payload: PendingSubmissionPayload): SubmissionOutcome.Sent {
        val studentId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("نشست دانش‌آموز پیدا نشد")
        require(studentId == payload.ownerUserId) { "صف پاسخ متعلق به حساب دیگری است." }

        val uploadedImages = buildMap<String, List<String>> {
            payload.responseImages.forEach { (questionId, uris) ->
                put(
                    questionId,
                    uris.map { uri ->
                        if (uri.startsWith("https://", true)) uri
                        else uploader.uploadAnswer(studentId, payload.examId, questionId, uri)
                    }
                )
            }
        }
        val imagesJson = JsonObject(
            uploadedImages.mapValues { (_, urls) -> JsonArray(urls.map(::JsonPrimitive)) }
        )
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_submit_queued_answer_v1",
            buildJsonObject {
                put("p_operation", payload.operationId)
                put("p_exam", payload.examId)
                put("p_responses", payload.responses)
                put("p_images", imagesJson)
                put("p_meta", buildJsonObject {
                    put("native", true)
                    put("queued", true)
                    put("created_at_epoch_ms", payload.createdAt)
                })
            }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        return SubmissionOutcome.Sent(raw["receipt"]?.jsonPrimitive?.contentOrNull)
    }

    override suspend fun submitAttempt(attempt: SubmittedExam): Result<SubmissionOutcome> = runCatching {
        sendPrepared(prepareSubmission(attempt))
    }

    private fun parseQuestion(index: Int, obj: JsonObject): Question {
        val id = obj["id"]?.jsonPrimitive?.content ?: index.toString()
        val text = obj["text"]?.jsonPrimitive?.content.orEmpty()
        val score = obj["score"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val images = obj["images"].arrayStrings().ifEmpty {
            obj["image"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::listOf).orEmpty()
        }
        val allowImages = obj["allowImages"]?.jsonPrimitive?.contentOrNull
        val maxAnswerImages = if (allowImages == null || allowImages == "no") 0
            else (obj["maxImages"]?.jsonPrimitive?.intOrNull ?: 1).coerceIn(0, 10)
        val answerImagesRequired = allowImages == "required"

        return when (obj["type"]?.jsonPrimitive?.content?.lowercase()) {
            "multiple", "multiple_choice" -> MultipleChoiceQuestion(
                id, text, score,
                options = obj["options"].arrayStrings(),
                optionImages = obj["optionImages"].arrayNullableStrings(),
                images = images,
                maxAnswerImages = maxAnswerImages,
                answerImagesRequired = answerImagesRequired
            )
            "truefalse", "true_false" -> TrueFalseQuestion(id, text, score, images, maxAnswerImages, answerImagesRequired)
            "fill", "fill_blank" -> FillBlankQuestion(id, text, score, images, maxAnswerImages, answerImagesRequired)
            "numeric" -> NumericQuestion(id, text, score, images, maxAnswerImages, answerImagesRequired)
            "matching" -> MatchingQuestion(
                id, text, score,
                leftItems = obj["leftItems"].arrayStrings(),
                rightItems = obj["rightItems"].arrayStrings(),
                leftImages = obj["leftImages"].arrayNullableStrings(),
                rightImages = obj["rightImages"].arrayNullableStrings(),
                images = images,
                maxAnswerImages = maxAnswerImages,
                answerImagesRequired = answerImagesRequired
            )
            else -> EssayQuestion(id, text, score, images, maxAnswerImages, answerImagesRequired)
        }
    }
}

private fun JsonElement?.arrayStrings(): List<String> =
    (this as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()

private fun JsonElement?.arrayNullableStrings(): List<String?> =
    (this as? JsonArray)?.map { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }.orEmpty()
