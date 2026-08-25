package ir.exam.app.data.repository

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.local.ActiveExamSessionEntity
import ir.exam.app.data.local.NativeDatabaseProvider
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.SubmittedExam
import ir.exam.app.domain.repository.ExamRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseStudentExamRepository(context: Context) : ExamRepository {
    private var activeExam: Exam? = null
    private val appContext = context.applicationContext
    private val uploader = SupabaseQuestionImageUploader(appContext)
    private val activeDao = NativeDatabaseProvider.get(appContext).activeExamSessionDao()

    override suspend fun joinByCode(code: String): Result<Exam> = runCatching {
        val owner = currentStudentId()
        val raw = SupabaseProvider.client.postgrest.rpc(
            "get_exam_for_student",
            buildJsonObject { put("p_code", code.trim()) }
        ).decodeAs<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        val now = System.currentTimeMillis()
        val safeRaw = StudentExamPayloadCodec.sanitize(raw)
        val exam = StudentExamPayloadCodec.decodeFresh(safeRaw, owner, now)
        activeDao.upsert(
            ActiveExamSessionEntity(
                ownerUserId = owner,
                examId = exam.id,
                code = exam.code,
                payloadJson = safeRaw.toString(),
                deadlineEpochMs = exam.deadlineEpochMs,
                savedAt = now
            )
        )
        exam.also { activeExam = it }
    }

    override suspend fun restoreActiveExam(): Result<Exam?> = runCatching {
        val owner = currentStudentId()
        val entity = activeDao.find(owner) ?: return@runCatching null
        val raw = StudentExamPayloadCodec.parse(entity.payloadJson)
        val exam = StudentExamPayloadCodec.decodeCached(
            raw = raw,
            studentId = owner,
            restoredAtEpochMs = System.currentTimeMillis(),
            deadlineEpochMs = entity.deadlineEpochMs
        )
        check(exam.id == entity.examId && exam.code.equals(entity.code, ignoreCase = true)) {
            "داده محلی آزمون با نشست ذخیره‌شده سازگار نیست."
        }
        exam.also { activeExam = it }
    }

    suspend fun cachedExamByCode(code: String): Result<Exam?> = runCatching {
        val owner = currentStudentId()
        val entity = activeDao.find(owner) ?: return@runCatching null
        if (!entity.code.equals(code.trim(), ignoreCase = true)) return@runCatching null
        val exam = StudentExamPayloadCodec.decodeCached(
            raw = StudentExamPayloadCodec.parse(entity.payloadJson),
            studentId = owner,
            restoredAtEpochMs = System.currentTimeMillis(),
            deadlineEpochMs = entity.deadlineEpochMs
        )
        exam.also { activeExam = it }
    }

    override suspend fun refreshActiveExam(): Result<Exam?> {
        val current = activeExam ?: restoreActiveExam().getOrNull() ?: return Result.success(null)
        return joinByCode(current.code).map { it }
    }

    override suspend fun clearActiveExam(examId: String): Result<Unit> = runCatching {
        val owner = currentStudentId()
        activeDao.delete(owner, examId)
        if (activeExam?.id == examId) activeExam = null
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
                    // V58.0 — گزارش نظارتی (رویدادهای امنیتی/زمان سؤال‌ها) فقط
                    // در متادیتای پاسخ ذخیره می‌شود و معلم آن را می‌بیند.
                    payload.monitorReport?.let { put("monitor_report", it) }
                })
            }
        ).decodeAs<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        return SubmissionOutcome.Sent(raw["receipt"]?.jsonPrimitive?.contentOrNull)
    }

    override suspend fun submitAttempt(attempt: SubmittedExam): Result<SubmissionOutcome> = runCatching {
        sendPrepared(prepareSubmission(attempt))
    }

    /** V58.0 — ثبت گزارش نظارتی؛ فقط معلمِ همان آزمون آن را می‌خواند. */
    override suspend fun reportMonitor(examId: String, reportJson: String): Result<Unit> = runCatching {
        val report = runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(reportJson) as? JsonObject
        }.getOrNull() ?: error("گزارش نامعتبر")
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_monitor_upsert_v1",
            buildJsonObject {
                put("p_exam", examId)
                put("p_report", report)
            }
        ).decodeAs<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        Unit
    }

    private fun currentStudentId(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id
        ?: error("نشست دانش‌آموز پیدا نشد")
}
