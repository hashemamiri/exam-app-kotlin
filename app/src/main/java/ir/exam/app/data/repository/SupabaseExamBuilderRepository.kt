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
import ir.exam.app.ui.builder.AudienceSchoolOption
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SupabaseExamBuilderRepository(context: Context) {
    private val imageUploader = SupabaseQuestionImageUploader(context)

    suspend fun load(examId: String?): Result<ExamBuilderState> = runCatching {
        coroutineScope {
            val teacherId = currentTeacherId()
            // این چهار داده به هم وابسته نیستند؛ بارگذاری موازی زمان ورود سازنده را کم می‌کند.
            val classesDeferred = async {
                SupabaseProvider.client.postgrest.rpc("my_classes")
                    .decodeList<SchoolClassDto>()
                    .map { AudienceClassOption(it.id, it.name) }
            }
            val studentsDeferred = async {
                SupabaseProvider.client.postgrest.rpc("my_students_for_pick")
                    .decodeList<StudentProfileDto>()
                    .map { AudienceStudentOption(it.id, it.fullName, it.classNames) }
            }
            val schoolsDeferred = async { loadSchoolOptions() }
            val bankDeferred = async { loadBankSnapshot() }

            val classes = classesDeferred.await()
            val students = studentsDeferred.await()
            val schools = schoolsDeferred.await()
            val bank = bankDeferred.await()

            if (examId == null) {
                return@coroutineScope ExamBuilderState(
                    availableClasses = classes,
                    availableStudents = students,
                    availableSchools = schools,
                    bankQuestions = bank.questions,
                    bankCategories = bank.categories
                )
            }

            // داده‌های مخصوص آزمون نیز مستقل هستند و هم‌زمان دریافت می‌شوند.
            val examDeferred = async {
                SupabaseProvider.client.from("exams").select {
                    filter {
                        eq("id", examId)
                        eq("teacher_id", teacherId)
                    }
                }.decodeList<ExamDetailDto>().firstOrNull()
                    ?: error("آزمون پیدا نشد یا متعلق به این حساب نیست.")
            }
            val keyDeferred = async {
                SupabaseProvider.client.from("exam_keys").select {
                    filter { eq("exam_id", examId) }
                }.decodeList<ExamKeyDto>().firstOrNull()?.answers ?: JsonArray(emptyList())
            }
            val audienceDeferred = async { loadAudience(examId) }
            val audienceSchoolsDeferred = async { loadAudienceSchools(examId) }

            val exam = examDeferred.await()
            val key = keyDeferred.await()
            val audience = audienceDeferred.await()
            val audienceSchools = audienceSchoolsDeferred.await()

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
                audienceMode = if (audienceSchools.isNotEmpty()) "schools" else audience.mode,
                audienceClasses = audience.classes,
                audienceStudents = if (audienceSchools.isNotEmpty()) emptySet() else audience.students,
                audienceSchools = audienceSchools,
                availableClasses = classes,
                availableStudents = students,
                availableSchools = schools,
                bankQuestions = bank.questions,
                bankCategories = bank.categories
            )
        }
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

    /** V61.0 — مدرسه‌های عضو معلم؛ اگر SQL هنوز اجرا نشده باشد لیست خالی. */
    private suspend fun loadSchoolOptions(): List<AudienceSchoolOption> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_teacher_schools_v61")
            .decodeAs<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        (raw["items"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            AudienceSchoolOption(
                id = id,
                name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                city = obj["city"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            )
        }
    }.getOrDefault(emptyList())

    /** V61.0 — مدرسه‌های ذخیره‌شدهٔ آزمون برای بازیابی حالت «مدارس» در ویرایش. */
    private suspend fun loadAudienceSchools(examId: String): Set<String> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_exam_audience_schools_v61",
            buildJsonObject { put("p_exam", examId) }
        ).decodeAs<JsonObject>()
        raw["schools"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet().orEmpty()
    }.getOrDefault(emptySet())

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
