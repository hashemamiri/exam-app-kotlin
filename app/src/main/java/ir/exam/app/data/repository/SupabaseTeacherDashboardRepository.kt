package ir.exam.app.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class ManagerApprovalItem(
    val id: String, val targetType: String, val action: String, val status: String, val expiresAt: String, val managerName: String,
    val targetName: String = "", // V261 — نام کلاس/دانش‌آموز هدف (سرور V260.8)
    val changeSummary: String = "" // V261 — «تغییر به: …» از payload
)

class SupabaseTeacherDashboardRepository {
    suspend fun getMyExams(): Result<List<ExamDashboardDto>> = runCatching {
        val userId = currentTeacherId()
        // V212 — فقط ستون‌های کارت (بدون questions)
        SupabaseProvider.client.from("exams").select(Columns.raw(ExamDashboardDto.COLUMNS)) {
            filter { eq("teacher_id", userId) }
        }.decodeList<ExamDashboardDto>()
            // V137 — تازه‌ترین آزمون بالای فهرست؛ ردیف‌های بدون تاریخ (قدیمی) در انتها.
            .sortedWith(compareByDescending<ExamDashboardDto> { it.createdAt != null }.thenByDescending { it.createdAt.orEmpty() })
    }

    suspend fun setOpen(examId: String, open: Boolean): Result<Unit> = runCatching {
        rpcObject(
            "native_set_exam_open_v1",
            buildJsonObject { put("p_exam", examId); put("p_open", open) }
        ).throwIfDashboardError()
    }

    suspend fun deleteExam(examId: String): Result<Unit> = runCatching {
        // V59.3 — قبل از حذف آزمون، تصاویر آن از استوریج پاک می‌شوند
        // (best-effort؛ شکست پاک‌سازی حذف آزمون را بلاک نمی‌کند).
        runCatching {
            // V221 — تابع جدید آدرس‌های S3/R2 را هم می‌دهد؛ اگر هنوز اجرا نشده باشد، به v59 برمی‌گردیم.
            val raw = runCatching { rpcObject("native_exam_media_urls_v221", buildJsonObject { put("p_exam", examId) }) }
                .getOrElse { rpcObject("native_exam_image_paths_v59", buildJsonObject { put("p_exam", examId) }) }
            val urls = (raw["urls"] as? kotlinx.serialization.json.JsonArray).orEmpty()
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
            StorageImageCleaner.removeByPublicUrls(urls)
        }
        rpcObject("native_delete_exam", buildJsonObject { put("p_exam", examId) }).throwIfDashboardError()
    }

    suspend fun duplicateExam(examId: String, operationId: String): Result<DuplicateExamResult> = runCatching {
        val raw = rpcObject("native_duplicate_exam_v2", buildJsonObject {
            put("p_exam", examId)
            put("p_operation", operationId)
        }).throwIfDashboardError()
        DuplicateExamResult(
            code = raw["code"]?.jsonPrimitive?.contentOrNull ?: error("کد آزمون کپی‌شده دریافت نشد."),
            costToman = raw["cost"]?.jsonPrimitive?.longOrNull ?: 0,
            balanceToman = raw["balance"]?.jsonPrimitive?.longOrNull
        )
    }

    /** V264 — کپی یک‌کلیکی آزمون به کلاس دیگر (native_duplicate_exam_to_class_v1؛ مخاطب = همان کلاس). */
    suspend fun duplicateExamToClass(examId: String, classId: String, operationId: String): Result<DuplicateExamResult> = runCatching {
        val raw = rpcObject("native_duplicate_exam_to_class_v1", buildJsonObject {
            put("p_exam", examId)
            put("p_class", classId)
            put("p_operation", operationId)
        }).throwIfDashboardError()
        DuplicateExamResult(
            code = raw["code"]?.jsonPrimitive?.contentOrNull ?: error("کد آزمون کپی‌شده دریافت نشد."),
            costToman = raw["cost"]?.jsonPrimitive?.longOrNull ?: 0,
            balanceToman = raw["balance"]?.jsonPrimitive?.longOrNull,
            className = raw["class_name"]?.jsonPrimitive?.contentOrNull
        )
    }

    suspend fun managerRequests(): Result<List<ManagerApprovalItem>> = runCatching {
        val raw = rpcObject("native_teacher_manager_requests_v41", buildJsonObject { }).throwIfDashboardError()
        (raw["items"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            fun text(key: String) = item[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            val payload = item["payload"] as? JsonObject
            fun p(key: String) = payload?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty()
            val summary = when {
                text("action") == "delete" -> ""
                p("kind") == "reset_password" -> "رمز جدید"
                p("kind") == "profile_update" -> listOf(p("first_name"), p("last_name")).filter { it.isNotBlank() }.joinToString(" ").let { n -> if (p("username").isNotBlank()) "$n (${p("username")})" else n }.let { if (it.isNotBlank()) "تغییر به: $it" else "" }
                text("target_type") == "class" -> listOf(p("name").takeIf { it.isNotBlank() }?.let { "نام «$it»" }, p("grade").takeIf { it.isNotBlank() }?.let { "پایه $it" }, p("field").takeIf { it.isNotBlank() }?.let { "رشته $it" }).filterNotNull().joinToString("، ").let { if (it.isNotBlank()) "تغییر به: $it" else "" }
                else -> ""
            }
            ManagerApprovalItem(text("id"), text("target_type"), text("action"), text("status"), text("expires_at"), text("manager_name"), text("target_name"), summary)
        }
    }

    suspend fun decideManagerRequest(id: String, approve: Boolean): Result<Unit> = runCatching {
        rpcObject("native_teacher_decide_manager_request_v41", buildJsonObject {
            put("p_request", id); put("p_approve", approve)
        }).throwIfDashboardError(); Unit
    }

    private suspend fun rpcObject(name: String, parameters: JsonObject): JsonObject =
        SupabaseProvider.client.postgrest.rpc(name, parameters).decodeAs()

    private fun currentTeacherId(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id
        ?: error("نشست ورود پیدا نشد. دوباره وارد شوید.")
}

data class DuplicateExamResult(val code: String, val costToman: Long, val balanceToman: Long?, val className: String? = null)

private fun JsonObject.throwIfDashboardError(): JsonObject {
    this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
    return this
}
