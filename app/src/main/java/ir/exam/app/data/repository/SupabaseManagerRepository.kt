package ir.exam.app.data.repository

import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal data class SchoolTeacherItem(
    val id: String,
    val fullName: String,
    val username: String,
    val email: String
)

internal data class TeacherInviteResult(val email: String, val code: String)

internal class SupabaseManagerRepository {
    suspend fun teachers(): Result<List<SchoolTeacherItem>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_manager_teachers_v37").decodeAs<JsonObject>().checked()
        (raw["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            SchoolTeacherItem(
                id = item.text("id"),
                fullName = item.text("full_name"),
                username = item.text("username"),
                email = item.text("email")
            )
        }
    }

    suspend fun createInvite(email: String): Result<TeacherInviteResult> = runCatching {
        require('@' in email) { "ایمیل معلم معتبر نیست." }
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_manager_create_teacher_invite_v37",
            buildJsonObject { put("p_email", email.trim().lowercase()) }
        ).decodeAs<JsonObject>().checked()
        TeacherInviteResult(raw.text("email"), raw.text("invite_code"))
    }

    suspend fun disableTeacher(id: String): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "native_manager_disable_teacher_v37",
            buildJsonObject { put("p_teacher", id) }
        ).decodeAs<JsonObject>().checked()
        Unit
    }

    private fun JsonObject.checked(): JsonObject {
        this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        return this
    }
    private fun JsonObject.text(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}
