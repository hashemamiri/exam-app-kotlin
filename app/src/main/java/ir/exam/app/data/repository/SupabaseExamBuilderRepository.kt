package ir.exam.app.data.repository

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.dto.ExamDetailDto
import ir.exam.app.data.dto.ExamKeyDto
import ir.exam.app.data.dto.QuestionBankDto
import ir.exam.app.data.dto.SchoolClassDto
import ir.exam.app.data.dto.StudentProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.builder.AudienceClassOption
import ir.exam.app.ui.builder.AudienceStudentOption
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.ExamBuilderState
import ir.exam.app.ui.builder.ExamSaveResult
import ir.exam.app.ui.builder.QuestionDraft
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class SupabaseExamBuilderRepository(context: Context) {
    private val imageUploader = SupabaseQuestionImageUploader(context)

    suspend fun load(examId: String?): Result<ExamBuilderState> = runCatching {
        val teacherId = currentTeacherId()
        val classes = SupabaseProvider.client.postgrest.rpc("my_classes")
            .decodeList<SchoolClassDto>()
            .map { AudienceClassOption(it.id, it.name) }
        val students = SupabaseProvider.client.postgrest.rpc("my_students_for_pick")
            .decodeList<StudentProfileDto>()
            .map { AudienceStudentOption(it.id, it.fullName, it.classNames) }
        val bank = loadBankQuestions()

        if (examId == null) {
            return@runCatching ExamBuilderState(
                availableClasses = classes,
                availableStudents = students,
                bankQuestions = bank
            )
        }

        val exam = SupabaseProvider.client.from("exams").select {
            filter {
                eq("id", examId)
                eq("teacher_id", teacherId)
            }
        }.decodeList<ExamDetailDto>().firstOrNull() ?: error("آزمون پیدا نشد یا متعلق به این حساب نیست.")
        val key = SupabaseProvider.client.from("exam_keys").select {
            filter { eq("exam_id", examId) }
        }.decodeList<ExamKeyDto>().firstOrNull()?.answers ?: JsonArray(emptyList())
        val audience = loadAudience(examId)

        ExamBuilderState(
            examId = exam.id,
            code = exam.code,
            title = exam.title,
            subject = exam.subject.orEmpty(),
            durationMinutes = exam.duration?.toString().orEmpty(),
            questions = ExamQuestionCodec.decode(exam.questions, key),
            shuffleQuestions = exam.shuffleQuestions,
            shuffleOptions = exam.shuffleOptions,
            negativeMarking = exam.negativeMarking.toString(),
            teacherMessage = exam.teacherMessage.orEmpty(),
            attemptsAllowed = exam.attemptsAllowed.coerceIn(1, 5),
            attemptOnTimeout = exam.attemptOnTimeout,
            gradePolicy = exam.gradePolicy,
            attemptCooldown = exam.attemptCooldown.toString(),
            audienceMode = audience.mode,
            audienceClasses = audience.classes,
            audienceStudents = audience.students,
            availableClasses = classes,
            availableStudents = students,
            bankQuestions = bank
        )
    }

    suspend fun save(
        state: ExamBuilderState,
        operationId: String,
        onUploadProgress: (done: Int, total: Int) -> Unit
    ): Result<ExamSaveResult> = runCatching {
        val teacherId = currentTeacherId()
        require(state.title.trim().isNotEmpty()) { "عنوان آزمون را وارد کنید." }
        require(state.questions.isNotEmpty()) { "حداقل یک سؤال اضافه کنید." }
        require(state.questions.all { it.text.isNotBlank() }) { "متن همه سؤال‌ها را وارد کنید." }
        require(state.audienceMode != "classes" || state.audienceClasses.isNotEmpty()) { "حداقل یک کلاس انتخاب کنید." }
        require(state.audienceMode != "students" || state.audienceStudents.isNotEmpty()) { "حداقل یک دانش‌آموز انتخاب کنید." }

        val examId = state.examId ?: UUID.randomUUID().toString()
        val questionsWithUrls = imageUploader.uploadPending(
            teacherId = teacherId,
            examId = examId,
            questions = state.questions,
            onProgress = onUploadProgress
        )
        val encoded = ExamQuestionCodec.encode(questionsWithUrls)
        val duration = state.durationMinutes.toIntOrNull()?.coerceIn(0, 1440) ?: 0
        val totalScore = questionsWithUrls.sumOf { it.score }
        val code = state.code ?: generateCode()
        val payload = buildJsonObject {
            put("operation_id", operationId)
            put("id", examId)
            put("code", code)
            put("title", state.title.trim())
            put("subject", state.subject.trim())
            put("duration", duration)
            put("total_score", totalScore)
            put("shuffle_q", state.shuffleQuestions)
            put("shuffle_opt", state.shuffleOptions)
            put("neg_marking", state.negativeMarking.toDoubleOrNull() ?: 0.0)
            put("teacher_message", state.teacherMessage.trim().ifBlank { null })
            put("attempts_allowed", state.attemptsAllowed.coerceIn(1, 5))
            put("attempt_on_timeout", state.attemptOnTimeout)
            put("grade_policy", state.gradePolicy)
            put("attempt_cooldown", state.attemptCooldown.toIntOrNull()?.coerceIn(0, 1440) ?: 0)
            put("questions", encoded.publicQuestions)
            put("answer_key", encoded.answerKey)
            put("audience", state.audienceMode)
            put("classes", buildJsonArray { state.audienceClasses.sorted().forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            put("students", buildJsonArray { state.audienceStudents.sorted().forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
        }
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_save_exam_v1",
            buildJsonObject { put("p_payload", payload) }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let { message ->
            val balance = raw["balance"]?.jsonPrimitive?.longOrNull
            val required = raw["required"]?.jsonPrimitive?.longOrNull
            if (balance != null && required != null) error("$message؛ موجودی $balance تومان و مبلغ لازم $required تومان است.")
            error(message)
        }
        ExamSaveResult(
            code = raw["code"]?.jsonPrimitive?.contentOrNull ?: code,
            chargedToman = raw["cost"]?.jsonPrimitive?.longOrNull ?: 0,
            walletBalanceToman = raw["balance"]?.jsonPrimitive?.longOrNull
        )
    }

    suspend fun refreshBank(): Result<List<BankQuestionOption>> = runCatching { loadBankQuestions() }

    suspend fun saveToBank(question: QuestionDraft, subject: String): Result<Unit> = runCatching {
        val encoded = ExamQuestionCodec.encode(listOf(question))
        val public = encoded.publicQuestions.first() as JsonObject
        val key = encoded.answerKey.first() as JsonObject
        val combined = JsonObject(public + key - "i")
        val raw = SupabaseProvider.client.postgrest.rpc(
            "bank_add",
            buildJsonObject {
                put("p_question", combined)
                put("p_subject", subject.trim())
            }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.let(::error)
    }

    suspend fun deleteFromBank(id: Long): Result<Unit> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "bank_del",
            buildJsonObject { put("p_id", id) }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.let(::error)
    }

    private suspend fun loadBankQuestions(): List<BankQuestionOption> =
        SupabaseProvider.client.postgrest.rpc("bank_list")
            .decodeList<QuestionBankDto>()
            .mapNotNull { row ->
                val question = ExamQuestionCodec.decode(
                    JsonArray(listOf(row.question)),
                    JsonArray(listOf(row.question))
                ).firstOrNull() ?: return@mapNotNull null
                BankQuestionOption(row.id, row.subject, question)
            }

    private suspend fun loadAudience(examId: String): AudienceValue {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "get_exam_audience",
            buildJsonObject { put("p_exam", examId) }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.let(::error)
        val mode = raw["mode"]?.jsonPrimitive?.contentOrNull ?: "all"
        val classes = raw["classes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet().orEmpty()
        val students = raw["students"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet().orEmpty()
        return AudienceValue(mode, classes, students)
    }

    private fun currentTeacherId(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id
        ?: error("نشست معلم پیدا نشد. دوباره وارد شوید.")

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString { repeat(6) { append(chars.random()) } }
    }

    private data class AudienceValue(
        val mode: String,
        val classes: Set<String>,
        val students: Set<String>
    )
}
