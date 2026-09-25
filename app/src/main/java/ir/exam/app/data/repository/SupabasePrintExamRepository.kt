package ir.exam.app.data.repository

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.local.PrintExamRecord
import ir.exam.app.data.local.PrintExamStore
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.builder.QuestionDraft
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * V163 — آزمون‌های چاپی روی سرور (جدول print_exams، جدا از exams) تا اپ، سایت دسکتاپ و
 * سایت گوشی همان فهرست را ببینند. سؤال‌ها مثل بستهٔ .azmoon (public + key ادغام‌شده)
 * ذخیره می‌شوند و با ExamQuestionCodec.decode(combined, combined) برمی‌گردند.
 * V199 — ذخیره کاملاً رایگان است؛ هزینه فقط هنگام چاپ (quote/pay/payStatus: هر سؤال و هر تصویر ۱۰۰۰ تومان،
 * یک پرداخت مشترک برای نسخهٔ دانش‌آموز و کلید؛ تغییر سربرگ = کل هزینه، تغییر سؤال/تصویر = فقط همان).
 * آزمون‌های چاپی قدیمیِ SharedPreferences (PrintExamStore) یک‌بار به سرور منتقل می‌شوند.
 */
class SupabasePrintExamRepository(context: Context) {
    private val appContext = context.applicationContext
    private val legacy = PrintExamStore(appContext)
    private val uploader = SupabaseQuestionImageUploader(appContext)

    data class Summary(
        val id: String,
        val title: String,
        val subject: String,
        val questionCount: Int,
        val sourceExamId: String?,
        val savedAt: Long
    )

    data class SaveResult(val id: String, val billedImages: Int, val costToman: Long, val balanceToman: Long?)

    /** V199 — برآورد بدهی چاپ یک آزمون چاپی برای سربرگ فعلی. */
    data class PayQuote(
        val id: String,
        val dueToman: Long,
        val questionsDue: Int,
        val imagesDue: Int,
        val headerChanged: Boolean,
        val neverPaid: Boolean
    ) { val paid: Boolean get() = dueToman <= 0 }

    data class PayResult(val costToman: Long, val balanceToman: Long?)

    data class PayStatus(val id: String, val dueToman: Long, val paid: Boolean)

    private fun JsonObject.failIfError() {
        this["error"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { msg ->
            val required = this["required"]?.jsonPrimitive?.longOrNull
            val balance = this["balance"]?.jsonPrimitive?.longOrNull
            error(if (required != null && balance != null) "$msg (لازم: $required تومان، موجودی: $balance تومان)" else msg)
        }
    }

    suspend fun quote(id: String, headerFingerprint: String): PayQuote {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_print_quote_v199",
            buildJsonObject { put("p_id", id); put("p_header", headerFingerprint) }
        ).decodeAs<JsonObject>()
        raw.failIfError()
        return PayQuote(
            id = raw["id"]?.jsonPrimitive?.contentOrNull ?: id,
            dueToman = raw["due"]?.jsonPrimitive?.longOrNull ?: 0L,
            questionsDue = raw["questions_due"]?.jsonPrimitive?.intOrNull ?: 0,
            imagesDue = raw["images_due"]?.jsonPrimitive?.intOrNull ?: 0,
            headerChanged = raw["header_changed"]?.jsonPrimitive?.contentOrNull == "true",
            neverPaid = raw["never_paid"]?.jsonPrimitive?.contentOrNull == "true"
        )
    }

    suspend fun pay(id: String, headerFingerprint: String): PayResult {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_print_pay_v199",
            buildJsonObject { put("p_id", id); put("p_operation", UUID.randomUUID().toString()); put("p_header", headerFingerprint) }
        ).decodeAs<JsonObject>()
        raw.failIfError()
        return PayResult(
            costToman = raw["cost"]?.jsonPrimitive?.longOrNull ?: 0L,
            balanceToman = raw["balance"]?.jsonPrimitive?.longOrNull
        )
    }

    /** وضعیت پرداخت همهٔ آزمون‌های چاپی معلم برای سربرگ فعلی (کارت‌ها: چاپگر قرمز/سبز). */
    suspend fun payStatus(headerFingerprint: String): Map<String, PayStatus> {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_print_pay_status_v199",
            buildJsonObject { put("p_header", headerFingerprint) }
        ).decodeAs<JsonElement>()
        (raw as? JsonObject)?.failIfError()
        return (raw as? JsonArray ?: JsonArray(emptyList())).mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            val id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            PayStatus(id, o["due"]?.jsonPrimitive?.longOrNull ?: 0L, o["paid"]?.jsonPrimitive?.contentOrNull == "true")
        }.associateBy { it.id }
    }

    private fun uid(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: error("ابتدا وارد شوید.")

    suspend fun list(): List<Summary> {
        migrateLegacy()
        val raw = SupabaseProvider.client.postgrest.rpc("native_print_exams_list_v163").decodeAs<JsonElement>()
        (raw as? JsonObject)?.get("error")?.jsonPrimitive?.contentOrNull?.let { error(it) }
        return (raw as? JsonArray ?: JsonArray(emptyList())).mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            Summary(
                id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                title = o["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                subject = o["subject"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                questionCount = o["question_count"]?.jsonPrimitive?.intOrNull ?: 0,
                sourceExamId = o["source_exam_id"]?.jsonPrimitive?.contentOrNull,
                savedAt = o["saved_at"]?.jsonPrimitive?.longOrNull ?: 0L
            )
        }
    }

    suspend fun get(id: String): PrintExamRecord? {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_print_exam_get_v163",
            buildJsonObject { put("p_id", id) }
        ).decodeAs<JsonObject>()
        if (raw["error"] != null) return null
        val combined = raw["questions"]?.jsonArray ?: JsonArray(emptyList())
        return PrintExamRecord(
            id = raw["id"]?.jsonPrimitive?.contentOrNull ?: id,
            title = raw["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            subject = raw["subject"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            questions = ExamQuestionCodec.decode(combined, combined),
            savedAt = raw["saved_at"]?.jsonPrimitive?.longOrNull ?: 0L,
            sourceExamId = raw["source_exam_id"]?.jsonPrimitive?.contentOrNull
        )
    }

    /** تعداد تصاویر محلی (هنوز آپلودنشده) — برای پیام هزینه پیش از ذخیره. */
    fun pendingImageCount(questions: List<QuestionDraft>): Int = questions.sumOf { q ->
        q.images.count { !it.uri.isRemote() } +
            q.optionImages.count { !it.isNullOrBlank() && !it.isRemote() } +
            q.matchingLeftImages.count { !it.isNullOrBlank() && !it.isRemote() } +
            q.matchingRightImages.count { !it.isNullOrBlank() && !it.isRemote() }
    }

    suspend fun save(record: PrintExamRecord, onProgress: (Int, Int) -> Unit = { _, _ -> }): SaveResult {
        val teacherId = uid()
        val id = record.id.takeIf { runCatching { UUID.fromString(it) }.isSuccess } ?: UUID.randomUUID().toString()
        val withUrls = uploader.uploadPending(teacherId, id, record.questions, onProgress)
        val encoded = ExamQuestionCodec.encode(withUrls)
        val combined = JsonArray(encoded.publicQuestions.mapIndexed { index, item ->
            val answer = encoded.answerKey.getOrNull(index)?.jsonObject ?: JsonObject(emptyMap())
            JsonObject(item.jsonObject + (answer - "i"))
        })
        val payload = buildJsonObject {
            put("id", id)
            put("operation_id", UUID.randomUUID().toString())
            put("title", record.title.trim())
            put("subject", record.subject.trim())
            put("duration", 0)
            put("questions", combined)
            record.sourceExamId?.let { put("source_exam_id", it) }
        }
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_print_exam_save_v163",
            buildJsonObject { put("p_payload", payload) }
        ).decodeAs<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let { message ->
            val balance = raw["balance"]?.jsonPrimitive?.longOrNull
            val required = raw["required"]?.jsonPrimitive?.longOrNull
            if (balance != null && required != null) error("$message؛ موجودی $balance تومان و مبلغ لازم $required تومان است.")
            error(message)
        }
        return SaveResult(
            id = id,
            billedImages = raw["billed_images"]?.jsonPrimitive?.intOrNull ?: 0,
            costToman = raw["cost"]?.jsonPrimitive?.longOrNull ?: 0L,
            balanceToman = raw["balance"]?.jsonPrimitive?.longOrNull
        )
    }

    suspend fun delete(id: String) {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_print_exam_delete_v163",
            buildJsonObject { put("p_id", id) }
        ).decodeAs<JsonObject>()
        raw["error"]?.jsonPrimitive?.contentOrNull?.let { error(it) }
    }

    /** انتقال یک‌بارهٔ رکوردهای قدیمی دستگاه به سرور؛ شناسه‌های «local-…» به uuid تبدیل می‌شوند. */
    private suspend fun migrateLegacy() {
        val old = legacy.list()
        if (old.isEmpty()) return
        for (rec in old) {
            runCatching { save(rec) }.onSuccess { legacy.delete(rec.id) }.onFailure { return }
        }
    }

    private fun String.isRemote(): Boolean {
        val scheme = runCatching { Uri.parse(this).scheme?.lowercase() }.getOrNull()
        return scheme == "http" || scheme == "https"
    }
}
