package ir.exam.app.data.repository

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.dto.ExamDetailDto
import ir.exam.app.data.dto.ExamKeyDto
import ir.exam.app.data.dto.ExamUpdateDto
import ir.exam.app.data.dto.ExamWriteDto
import ir.exam.app.data.dto.QuestionBankDto
import ir.exam.app.data.dto.SchoolClassDto
import ir.exam.app.data.dto.StudentProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.builder.AudienceClassOption
import ir.exam.app.ui.builder.AudienceStudentOption
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.ExamBuilderState
import ir.exam.app.ui.builder.QuestionDraft
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
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
        onUploadProgress: (done: Int, total: Int) -> Unit
    ): Result<String> = runCatching {
        val teacherId = currentTeacherId()
        require(state.title.trim().isNotEmpty()) { "عنوان آزمون را وارد کنید." }
        require(state.questions.isNotEmpty()) { "حداقل یک سؤال اضافه کنید." }
        require(state.questions.all { it.text.isNotBlank() }) { "متن همه سؤال‌ها را وارد کنید." }

        val examId = state.examId ?: UUID.randomUUID().toString()
        val questionsWithUrls = imageUploader.uploadPending(
            teacherId = teacherId,
            examId = examId,
            questions = state.questions,
            onProgress = onUploadProgress
        )
        val encoded = ExamQuestionCodec.encode(questionsWithUrls)
        val duration = state.durationMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val totalScore = questionsWithUrls.sumOf { it.score }
        val code = state.code ?: generateCode()

        if (state.examId == null) {
            SupabaseProvider.client.from("exams").insert(
                ExamWriteDto(
                    id = examId,
                    teacherId = teacherId,
                    title = state.title.trim(),
                    subject = state.subject.trim(),
                    duration = duration,
                    code = code,
                    totalScore = totalScore,
                    isOpen = false,
                    shuffleQuestions = state.shuffleQuestions,
                    shuffleOptions = state.shuffleOptions,
                    negativeMarking = state.negativeMarking.toDoubleOrNull() ?: 0.0,
                    teacherMessage = state.teacherMessage.trim().ifBlank { null },
                    attemptsAllowed = state.attemptsAllowed.coerceIn(1, 5),
                    attemptOnTimeout = state.attemptOnTimeout,
                    gradePolicy = state.gradePolicy,
                    attemptCooldown = state.attemptCooldown.toIntOrNull()?.coerceIn(0, 1440) ?: 0,
                    questions = encoded.publicQuestions
                )
            )
        } else {
            SupabaseProvider.client.from("exams").update(
                ExamUpdateDto(
                    title = state.title.trim(),
                    subject = state.subject.trim(),
                    duration = duration,
                    totalScore = totalScore,
                    questions = encoded.publicQuestions,
                    shuffleQuestions = state.shuffleQuestions,
                    shuffleOptions = state.shuffleOptions,
                    negativeMarking = state.negativeMarking.toDoubleOrNull() ?: 0.0,
                    teacherMessage = state.teacherMessage.trim().ifBlank { null },
                    attemptsAllowed = state.attemptsAllowed.coerceIn(1, 5),
                    attemptOnTimeout = state.attemptOnTimeout,
                    gradePolicy = state.gradePolicy,
                    attemptCooldown = state.attemptCooldown.toIntOrNull()?.coerceIn(0, 1440) ?: 0
                )
            ) {
                filter {
                    eq("id", examId)
                    eq("teacher_id", teacherId)
                }
            }
        }

        SupabaseProvider.client.from("exam_keys").upsert(
            ExamKeyDto(examId = examId, answers = encoded.answerKey)
        )
        saveAudience(examId, state)
        code
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

    private suspend fun saveAudience(examId: String, state: ExamBuilderState) {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "set_exam_audience",
            buildJsonObject {
                put("p_exam", examId)
                put("p_mode", state.audienceMode)
                put("p_classes", buildJsonArray { state.audienceClasses.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                put("p_students", buildJsonArray { state.audienceStudents.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            }
        ).decodeSingle<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.let(::error)
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
