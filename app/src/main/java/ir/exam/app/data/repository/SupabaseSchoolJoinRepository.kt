package ir.exam.app.data.repository

import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class SchoolInvitePreview(
    val schoolName: String,
    val province: String,
    val city: String
)

class SupabaseSchoolJoinRepository {
    suspend fun preview(code: String): Result<SchoolInvitePreview> = runCatching {
        require(code.matches(Regex("^[A-Z0-9]{6}$"))) { "کد دعوت باید ۶ حرف یا عدد باشد." }
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_school_invite_preview_v39",
            buildJsonObject { put("p_code", code) }
        ).decodeAs<JsonObject>().checked()
        SchoolInvitePreview(raw.text("school_name"), raw.text("province"), raw.text("city"))
    }

    suspend fun join(code: String): Result<String> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_join_school_v39",
            buildJsonObject { put("p_code", code) }
        ).decodeAs<JsonObject>().checked()
        raw.text("school_name")
    }

    private fun JsonObject.checked(): JsonObject {
        this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        return this
    }
    private fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}
