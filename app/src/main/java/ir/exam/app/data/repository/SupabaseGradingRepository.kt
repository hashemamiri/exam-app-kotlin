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
import ir.exam.app.domain.model.ExamQuestionAnalysis
import ir.exam.app.domain.model.FeedbackPhrase
import ir.exam.app.domain.model.GradingExam
import ir.exam.app.domain.model.GradingQuestion
import ir.exam.app.domain.model.GradingSubmission
import ir.exam.app.domain.model.QuestionAnalysisRow
import ir.exam.app.domain.model.StudentAnswerReview
import ir.exam.app.domain.model.StudentAnswerReviewQuestion
import ir.exam.app.domain.model.StudentAnswerSummary
import ir.exam.app.ui.builder.QuestionDraft
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
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
    suspend fun updateFeedback(id:Long,text:String):Result<Unit> = runCatching {
        rpcObject("native_feedback_update_v1",buildJsonObject{put("p_id",id);put("p_text",text.trim())}).throwRpcError()
    }
    suspend fun deleteFeedback(id:Long):Result<Unit> = runCatching {
        rpcObject("native_feedback_delete_v1",buildJsonObject{put("p_id",id)}).throwRpcError()
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

    suspend fun questionAnalysis(examId: String): Result<ExamQuestionAnalysis> = runCatching {
        val raw = rpcObject(
            "native_question_analysis_v1",
            buildJsonObject { put("p_exam", examId) }
        ).throwRpcError()
        val rows = raw["questions"]?.jsonArray.orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            QuestionAnalysisRow(
                index = item["index"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                text = item["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                maxScore = item["max_score"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                gradedCount = item["graded_count"]?.jsonPrimitive?.intOrNull ?: 0,
                answeredCount = item["answered_count"]?.jsonPrimitive?.intOrNull ?: 0,
                averagePercent = item["average_percent"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                omitPercent = item["omit_percent"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                discrimination = item["discrimination"]?.jsonPrimitive?.doubleOrNull,
                pointBiserial = item["point_biserial"]?.jsonPrimitive?.doubleOrNull,
                level = item["level"]?.jsonPrimitive?.contentOrNull ?: "balanced"
            )
        }
        ExamQuestionAnalysis(
            examId = examId,
            answerCount = raw["answer_count"]?.jsonPrimitive?.intOrNull ?: 0,
            cronbachAlpha = raw["cronbach_alpha"]?.jsonPrimitive?.doubleOrNull,
            questions = rows
        )
    }

    suspend fun bulkSaveQuestion(
        examId: String,
        questionIndex: Int,
        scores: Map<String, Double>
    ): Result<Unit> = runCatching {
        require(scores.isNotEmpty()) { "حداقل یک نمره وارد کنید." }
        rpcObject(
            "native_bulk_save_question_grades_v1",
            buildJsonObject {
                put("p_exam", examId)
                put("p_question_index", questionIndex)
                put("p_items", buildJsonArray {
                    scores.forEach { (answerId, score) ->
                        add(buildJsonObject { put("answer_id", answerId); put("score", score) })
                    }
                })
            }
        ).throwRpcError()
    }

    suspend fun finalizeBulkGrades(examId: String): Result<Unit> = runCatching {
        rpcObject(
            "native_finalize_bulk_grades_v1",
            buildJsonObject { put("p_exam", examId) }
        ).throwRpcError()
    }

    suspend fun myGrades(): Result<List<JsonObject>> = runCatching {
        SupabaseProvider.client.postgrest.rpc("my_grades").decodeList()
    }

    suspend fun myAnswerSummaries(): Result<List<StudentAnswerSummary>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_my_answers_v1")
            .decodeAs<JsonObject>()
            .throwRpcError()
        (raw["items"] as? JsonArray).orEmpty().mapNotNull { item ->
            val row = item as? JsonObject ?: return@mapNotNull null
            val id = row.text("id") ?: return@mapNotNull null
            StudentAnswerSummary(
                id = id,
                examId = row.text("exam_id").orEmpty(),
                title = row.text("title").orEmpty().ifBlank { "آزمون" },
                subject = row.text("subject").orEmpty(),
                submittedAt = row.text("submitted_at"),
                graded = row.boolean("graded"),
                totalGrade = row.number("total_grade") ?: 0.0,
                totalScore = row.number("total_score") ?: 0.0,
                feedback = row.text("feedback").orEmpty()
            )
        }
    }

    suspend fun myAnswerDetail(answerId: String): Result<StudentAnswerReview> = runCatching {
        val raw = rpcObject(
            "native_my_answer_detail_v1",
            buildJsonObject { put("p_answer", answerId) }
        ).throwRpcError()
        val graded = raw.boolean("graded")
        val responses = raw["responses"] as? JsonArray ?: JsonArray(emptyList())
        val grades = raw["grades"] as? JsonArray ?: JsonArray(emptyList())
        val responseImages = raw["response_images"] as? JsonObject ?: JsonObject(emptyMap())
        val questions = (raw["questions"] as? JsonArray).orEmpty().mapIndexedNotNull { index, item ->
            val question = item as? JsonObject ?: return@mapIndexedNotNull null
            val questionId = question.text("id") ?: "q-$index"
            StudentAnswerReviewQuestion(
                index = index,
                id = questionId,
                type = question.text("type").orEmpty().lowercase(),
                text = question.text("text").orEmpty(),
                score = question.number("score") ?: 0.0,
                options = question["options"].strings(),
                leftItems = question["leftItems"].strings(),
                rightItems = question["rightItems"].strings(),
                response = responses.getOrNull(index),
                responseImages = responseImages[questionId].strings().ifEmpty {
                    responseImages[index.toString()].strings()
                },
                earnedScore = if (graded) grades.getOrNull(index)?.jsonPrimitive?.doubleOrNull else null,
                correctAnswer = if (graded) question.correctAnswerText() else null,
                explanation = if (graded) question.text("explanation")?.takeIf(String::isNotBlank) else null
            )
        }
        StudentAnswerReview(
            id = raw.text("id") ?: answerId,
            examId = raw.text("exam_id").orEmpty(),
            title = raw.text("title").orEmpty().ifBlank { "آزمون" },
            subject = raw.text("subject").orEmpty(),
            graded = graded,
            totalGrade = raw.number("total_grade") ?: 0.0,
            totalScore = raw.number("total_score") ?: 0.0,
            feedback = raw.text("feedback").orEmpty(),
            submittedAt = raw.text("submitted_at"),
            questions = questions
        )
    }

    private suspend fun rpcObject(name: String, parameters: JsonObject): JsonObject =
        SupabaseProvider.client.postgrest.rpc(name, parameters).decodeAs()

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

private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.number(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
private fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false
private fun JsonElement?.strings(): List<String> =
    (this as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()

private fun JsonObject.correctAnswerText(): String? = when (text("type")?.lowercase()) {
    "multiple", "multiple_choice", "multiplechoice" -> {
        val index = this["correctOption"]?.jsonPrimitive?.intOrNull
            ?: this["correctIndex"]?.jsonPrimitive?.intOrNull
        index?.let { this["options"].strings().getOrNull(it) }
    }
    "truefalse", "true_false" -> this["correctAnswer"]?.jsonPrimitive?.booleanOrNull?.let {
        if (it) "صحیح" else "غلط"
    }
    "fill", "fill_blank" -> this["accept"].strings().takeIf(List<String>::isNotEmpty)?.joinToString(" یا ")
    "numeric", "number" -> this["answer"]?.jsonPrimitive?.contentOrNull?.let { answer ->
        val tolerance = this["tolerance"]?.jsonPrimitive?.contentOrNull
        if (tolerance.isNullOrBlank() || tolerance == "0" || tolerance == "0.0") answer else "$answer ± $tolerance"
    }
    "matching", "match" -> (this["matchAnswer"] as? JsonObject)?.entries
        ?.sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
        ?.joinToString("، ") { (left, right) ->
            "${(left.toIntOrNull() ?: 0) + 1} ← ${(right.jsonPrimitive.intOrNull ?: 0) + 1}"
        }
        ?.takeIf(String::isNotBlank)
    else -> null
}

private fun JsonObject.throwRpcError(): JsonObject {
    this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
    return this
}
