package ir.exam.app.data.repository

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.dto.ExamDetailDto
import ir.exam.app.data.dto.ExamKeyDto
import ir.exam.app.data.dto.SchoolClassDto
import ir.exam.app.data.dto.StudentProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.builder.AudienceClassOption
import ir.exam.app.ui.builder.AudienceStudentOption
import ir.exam.app.ui.builder.BankCategoryOption
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.ExamBuilderState
import ir.exam.app.ui.builder.ExamSaveResult
import ir.exam.app.ui.builder.QuestionDraft
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.intOrNull
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
        val bank = loadBankSnapshot()

        if (examId == null) {
            return@runCatching ExamBuilderState(
                availableClasses = classes,
                availableStudents = students,
                bankQuestions = bank.questions,
                bankCategories = bank.categories
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
            opensAtIso = exam.opensAt,
            closesAtIso = exam.closesAt,
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
            bankQuestions = bank.questions,
            bankCategories = bank.categories
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
        if (state.opensAtIso != null && state.closesAtIso != null) {
            require(!Instant.parse(state.closesAtIso).isBefore(Instant.parse(state.opensAtIso))) {
                "زمان پایان نمی‌تواند قبل از زمان شروع باشد."
            }
        }

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
            put("opens_at", state.opensAtIso)
            put("closes_at", state.closesAtIso)
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
            "native_save_exam_v2",
            buildJsonObject { put("p_payload", payload) }
        ).decodeAs<JsonObject>()
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

    suspend fun refreshBank(): Result<BankSnapshot> = runCatching { loadBankSnapshot() }

    suspend fun saveToBank(
        question: QuestionDraft,
        subject: String,
        categoryIds: Set<Long> = emptySet()
    ): Result<Unit> = runCatching {
        val encoded = ExamQuestionCodec.encode(listOf(question))
        val public = encoded.publicQuestions.first() as JsonObject
        val key = encoded.answerKey.first() as JsonObject
        val combined = JsonObject(public + key - "i")
        rpcObject("native_bank_add_v2", buildJsonObject {
            put("p_question", combined)
            put("p_subject", subject.trim())
            put("p_cats", JsonArray(categoryIds.sorted().map { kotlinx.serialization.json.JsonPrimitive(it) }))
        })
    }

    suspend fun updateBankQuestion(
        id: Long,
        question: QuestionDraft,
        subject: String,
        categoryIds: Set<Long>
    ): Result<Unit> = runCatching {
        val encoded = ExamQuestionCodec.encode(listOf(question))
        val public = encoded.publicQuestions.first() as JsonObject
        val key = encoded.answerKey.first() as JsonObject
        val combined = JsonObject(public + key - "i")
        rpcObject("native_bank_update_question_v1", buildJsonObject {
            put("p_id", id)
            put("p_question", combined)
            put("p_subject", subject.trim())
            put("p_cats", JsonArray(categoryIds.sorted().map { kotlinx.serialization.json.JsonPrimitive(it) }))
        })
    }

    suspend fun deleteFromBank(id: Long): Result<Unit> = runCatching {
        rpcObject("native_bank_delete_question_v1", buildJsonObject { put("p_id", id) })
    }

    suspend fun addBankCategory(name: String): Result<Unit> = runCatching {
        rpcObject("native_bank_category_add_v1", buildJsonObject { put("p_name", name.trim()) })
    }

    suspend fun setBankCategories(questionId: Long, categoryIds: Set<Long>): Result<Unit> = runCatching {
        rpcObject("native_bank_set_categories_v1", buildJsonObject {
            put("p_id", questionId)
            put("p_cats", JsonArray(categoryIds.sorted().map { kotlinx.serialization.json.JsonPrimitive(it) }))
        })
    }

    suspend fun deleteBankCategory(id: Long, deleteQuestions: Boolean): Result<Unit> = runCatching {
        rpcObject("native_bank_category_delete_v1", buildJsonObject {
            put("p_id", id)
            put("p_delete_questions", deleteQuestions)
        })
    }

    private suspend fun rpcObject(name: String, params: JsonObject): JsonObject {
        val raw = SupabaseProvider.client.postgrest.rpc(name, params).decodeAs<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        return raw
    }

    private suspend fun loadBankSnapshot(): BankSnapshot {
        val raw = SupabaseProvider.client.postgrest.rpc("native_bank_snapshot_v1").decodeAs<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        val categories = (raw["categories"] as? JsonArray).orEmpty().mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            val id = row["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            BankCategoryOption(id, row["name"]?.jsonPrimitive?.contentOrNull.orEmpty(), row["count"]?.jsonPrimitive?.intOrNull ?: 0)
        }
        val questions = (raw["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            val id = row["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val combined = row["question"] as? JsonObject ?: return@mapNotNull null
            val question = ExamQuestionCodec.decode(JsonArray(listOf(combined)), JsonArray(listOf(combined))).firstOrNull()
                ?: return@mapNotNull null
            val catIds = (row["cat_ids"] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.longOrNull }.toSet()
            val catNames = (row["cat_names"] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
            BankQuestionOption(id, row["subject"]?.jsonPrimitive?.contentOrNull, question, catIds, catNames)
        }
        return BankSnapshot(questions, categories)
    }

    private suspend fun loadAudience(examId: String): AudienceValue {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "get_exam_audience",
            buildJsonObject { put("p_exam", examId) }
        ).decodeAs<JsonObject>()
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

    data class BankSnapshot(
        val questions: List<BankQuestionOption>,
        val categories: List<BankCategoryOption>
    )

    private data class AudienceValue(
        val mode: String,
        val classes: Set<String>,
        val students: Set<String>
    )
}
