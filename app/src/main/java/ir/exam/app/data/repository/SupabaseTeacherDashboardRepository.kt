package ir.exam.app.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class SupabaseTeacherDashboardRepository {
    suspend fun getMyExams(): Result<List<ExamDashboardDto>> = runCatching {
        val userId = currentTeacherId()
        SupabaseProvider.client.from("exams").select {
            filter { eq("teacher_id", userId) }
        }.decodeList<ExamDashboardDto>().sortedByDescending { it.createdAt.orEmpty() }
    }

    suspend fun setOpen(examId: String, open: Boolean): Result<Unit> = runCatching {
        SupabaseProvider.client.from("exams").update(
            buildJsonObject { put("is_open", open) }
        ) {
            filter {
                eq("id", examId)
                eq("teacher_id", currentTeacherId())
            }
        }
    }

    suspend fun deleteExam(examId: String): Result<Unit> = runCatching {
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

    private suspend fun rpcObject(name: String, parameters: JsonObject): JsonObject =
        SupabaseProvider.client.postgrest.rpc(name, parameters).decodeSingle()

    private fun currentTeacherId(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id
        ?: error("نشست ورود پیدا نشد. دوباره وارد شوید.")
}

data class DuplicateExamResult(val code: String, val costToman: Long, val balanceToman: Long?)

private fun JsonObject.throwIfDashboardError(): JsonObject {
    this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
    return this
}
