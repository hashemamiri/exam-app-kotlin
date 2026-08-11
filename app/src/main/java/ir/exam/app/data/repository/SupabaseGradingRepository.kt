package ir.exam.app.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.dto.AnswerDto
import ir.exam.app.data.dto.AttendanceDto
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.dto.ExamDetailDto
import ir.exam.app.data.dto.ExamKeyDto
import ir.exam.app.data.dto.FeedbackPhraseDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.AnalyticsSummary
import ir.exam.app.domain.model.AttendanceRow
import ir.exam.app.domain.model.FeedbackPhrase
import ir.exam.app.domain.model.GradingExam
import ir.exam.app.domain.model.GradingQuestion
import ir.exam.app.domain.model.GradingSubmission
import ir.exam.app.ui.builder.QuestionDraft
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseGradingRepository {
    suspend fun getExams(): Result<List<ExamDashboardDto>> = runCatching {
        val uid = currentTeacherId()
        SupabaseProvider.client.from("exams").select {
            filter { eq("teacher_id", uid) }
        }.decodeList<ExamDashboardDto>().sortedByDescending { it.createdAt.orEmpty() }
    }

    suspend fun getExam(examId: String): Result<GradingExam> = runCatching {
        val uid = currentTeacherId()
        val exam = SupabaseProvider.client.from("exams").select {
            filter { eq("id", examId); eq("teacher_id", uid) }
        }.decodeList<ExamDetailDto>().firstOrNull() ?: error("آزمون یافت نشد.")
        val key = SupabaseProvider.client.from("exam_keys").select {
            filter { eq("exam_id", examId) }
        }.decodeList<ExamKeyDto>().firstOrNull()?.answers ?: JsonArray(emptyList())
        val questions = ExamQuestionCodec.decode(exam.questions, key).map(QuestionDraft::toGradingQuestion)
        GradingExam(exam.id, exam.title, exam.subject, exam.totalScore, questions)
    }

    suspend fun getAnswers(examId: String): Result<List<GradingSubmission>> = runCatching {
        SupabaseProvider.client.from("answers").select {
            filter { eq("exam_id", examId) }
        }.decodeList<AnswerDto>().map(AnswerDto::toDomain)
    }

    suspend fun saveGrade(answerId: String, grades: List<Double>, feedback: String): Result<Unit> = runCatching {
        rpcObject("native_save_grade", buildJsonObject {
            put("p_answer", answerId)
            put("p_grades", JsonArray(grades.map(::JsonPrimitive)))
            put("p_feedback", feedback.trim())
        }).throwRpcError()
    }

    suspend fun feedbackBank(): Result<List<FeedbackPhrase>> = runCatching {
        SupabaseProvider.client.postgrest.rpc("fb_list")
            .decodeList<FeedbackPhraseDto>()
            .map { FeedbackPhrase(it.id, it.text) }
    }

    suspend fun addFeedback(text: String): Result<Unit> = runCatching {
        require(text.trim().isNotEmpty()) { "متن بازخورد خالی است." }
        rpcObject("fb_add", buildJsonObject { put("p_text", text.trim()) }).throwRpcError()
    }

    suspend fun autoGradeInfo(examId: String): Result<JsonObject> = runCatching {
        rpcObject("exam_autograde_info", buildJsonObject { put("p_exam", examId) }).throwRpcError()
    }

    suspend fun approveAutoGrades(examId: String): Result<Unit> = runCatching {
        rpcObject("approve_auto_grades", buildJsonObject {
            put("p_exam", examId)
            put("p_mode", "auto_only")
        }).throwRpcError()
    }

    suspend fun unapprove(answerId: String): Result<Unit> = runCatching {
        rpcObject("unapprove_grade", buildJsonObject { put("p_answer", answerId) }).throwRpcError()
    }

    suspend fun attendance(examId: String): Result<List<AttendanceRow>> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "exam_attendance",
            buildJsonObject { put("p_exam", examId) }
        ).decodeList<AttendanceDto>().map(AttendanceDto::toDomain)
    }

    suspend fun liveStatus(examId: String): Result<JsonObject> = runCatching {
        rpcObject("exam_live_status", buildJsonObject { put("p_exam", examId) }).throwRpcError()
    }

    suspend fun extendTime(examId: String, studentId: String, minutes: Int): Result<Unit> = runCatching {
        require(minutes in 1..240) { "زمان تمدید باید بین ۱ تا ۲۴۰ دقیقه باشد." }
        rpcObject("extend_student_time", buildJsonObject {
            put("p_exam", examId); put("p_student", studentId); put("p_minutes", minutes)
        }).throwRpcError()
    }

    suspend fun resetAttempt(examId: String, studentId: String): Result<Unit> = runCatching {
        rpcObject("reset_student_attempt", buildJsonObject {
            put("p_exam", examId); put("p_student", studentId); put("p_keep_copy", true)
        }).throwRpcError()
    }

    suspend fun analytics(): Result<AnalyticsSummary> = runCatching {
        val exams = getExams().getOrThrow()
        val answers = exams.flatMap { getAnswers(it.id).getOrThrow() }
        val graded = answers.count(GradingSubmission::graded)
        val maxByExam = exams.associate { it.id to it.totalScore.coerceAtLeast(0.0) }
        val percentages = answers.filter { it.graded }.mapNotNull { answer ->
            val max = maxByExam[answer.examId] ?: 0.0
            if (max > 0) answer.totalGrade * 100.0 / max else null
        }
        AnalyticsSummary(
            examCount = exams.size,
            answerCount = answers.size,
            gradedCount = graded,
            pendingCount = answers.size - graded,
            averagePercent = percentages.average().takeUnless(Double::isNaN) ?: 0.0
        )
    }

    suspend fun myGrades(): Result<List<JsonObject>> = runCatching {
        SupabaseProvider.client.postgrest.rpc("my_grades").decodeList()
    }

    suspend fun myAnswers(): Result<List<JsonObject>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("my_answers").decodeSingle<JsonElement>()
        when (raw) {
            is JsonArray -> raw.mapNotNull { it as? JsonObject }
            is JsonObject -> (raw["items"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
            else -> emptyList()
        }
    }

    private suspend fun rpcObject(name: String, parameters: JsonObject): JsonObject =
        SupabaseProvider.client.postgrest.rpc(name, parameters).decodeSingle()

    private fun currentTeacherId(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id
        ?: error("نشست معلم پیدا نشد.")
}

private fun QuestionDraft.toGradingQuestion() = GradingQuestion(
    id = id,
    type = type.name,
    text = text,
    score = score,
    options = options,
    images = images.map { it.uri }
)

private fun AnswerDto.toDomain(): GradingSubmission {
    val responseList = (responses as? JsonArray)?.toList().orEmpty()
    val gradeList = (grades as? JsonArray)?.map { it.jsonPrimitive.doubleOrNull ?: 0.0 }.orEmpty()
    val imageMap = (responseImages as? JsonObject)?.mapValues { (_, value) ->
        (value as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
    }.orEmpty()
    return GradingSubmission(
        id, examId, studentId, studentName, responseList, imageMap, gradeList,
        totalGrade, feedback, graded, autoGraded, attemptNo, submittedAt
    )
}

private fun AttendanceDto.toDomain() = AttendanceRow(
    studentId, fullName, username, status, totalGrade, submittedAt, expiresAt,
    minutesLeft, attempts, attemptsAllowed, abandoned
)

private fun JsonObject.throwRpcError(): JsonObject {
    this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
    return this
}
