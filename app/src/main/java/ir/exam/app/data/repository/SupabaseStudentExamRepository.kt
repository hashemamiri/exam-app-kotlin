package ir.exam.app.data.repository

import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.NumericQuestion
import ir.exam.app.domain.model.Question
import ir.exam.app.domain.model.StudentAnswer
import ir.exam.app.domain.model.SubmittedExam
import ir.exam.app.domain.model.TextAnswer
import ir.exam.app.domain.model.TrueFalseQuestion
import ir.exam.app.domain.repository.ExamRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put

class SupabaseStudentExamRepository : ExamRepository {
    private var activeExam: Exam? = null

    override suspend fun joinByCode(code: String): Result<Exam> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "get_exam_for_student",
            buildJsonObject { put("p_code", code.trim()) }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.content?.let { error(it) }
        val questions = raw["questions"]?.jsonArray.orEmpty().mapIndexed { index, item -> parseQuestion(index, item.jsonObject) }
        Exam(
            id = raw["id"]!!.jsonPrimitive.content,
            title = raw["title"]?.jsonPrimitive?.content.orEmpty(),
            code = raw["code"]?.jsonPrimitive?.content.orEmpty(),
            durationMinutes = raw["duration"]?.jsonPrimitive?.intOrNull ?: 0,
            questions = questions
        ).also { activeExam = it }
    }

    override suspend fun submitAttempt(attempt: SubmittedExam): Result<Unit> = runCatching {
        val exam = activeExam ?: error("آزمون فعال پیدا نشد")
        val responses = buildJsonArray {
            exam.questions.forEach { question ->
                val value = when (val answer = attempt.answers[question.id]) {
                    is TextAnswer -> answer.value
                    is ChoiceAnswer -> answer.selectedIndex.toString()
                    is BooleanAnswer -> answer.value.toString()
                    null -> ""
                }
                add(JsonPrimitive(value))
            }
        }
        val raw = SupabaseProvider.client.postgrest.rpc(
            "submit_answer",
            buildJsonObject {
                put("p_exam_id", attempt.examId)
                put("p_responses", responses)
                put("p_images", JsonObject(emptyMap()))
            }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.content?.let { error(it) }
    }

    private fun parseQuestion(index: Int, obj: JsonObject): Question {
        val id = obj["id"]?.jsonPrimitive?.content ?: index.toString()
        val text = obj["text"]?.jsonPrimitive?.content.orEmpty()
        val score = obj["score"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        return when (obj["type"]?.jsonPrimitive?.content?.lowercase()) {
            "multiple", "multiple_choice" -> MultipleChoiceQuestion(id, text, score, obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty())
            "truefalse", "true_false" -> TrueFalseQuestion(id, text, score)
            "fill", "fill_blank" -> FillBlankQuestion(id, text, score)
            "numeric" -> NumericQuestion(id, text, score)
            else -> EssayQuestion(id, text, score)
        }
    }
}
