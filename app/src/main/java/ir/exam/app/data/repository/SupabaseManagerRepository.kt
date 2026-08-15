package ir.exam.app.data.repository

import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.ManagerWalletRules
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal data class SchoolTeacherItem(
    val id: String,
    val fullName: String,
    val username: String = "",
    val email: String = "",
    val employeeCode: String = "",
    val phone: String = "",
    val active: Boolean = true,
    val walletBalanceToman: Long = 0
)

internal data class TeacherInviteResult(val code: String)
internal data class ManagerInviteItem(
    val id: String, val code: String, val expiresAt: String, val used: Boolean, val revoked: Boolean
)
internal data class WalletTransferResult(
    val amountToman: Long,
    val managerBalanceToman: Long,
    val teacherBalanceToman: Long
)

internal class SupabaseManagerRepository {
    suspend fun teachers(): Result<List<SchoolTeacherItem>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_manager_teachers_v37").decodeAs<JsonObject>().checked()
        (raw["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            SchoolTeacherItem(
                id = item.text("id"),
                fullName = item.text("full_name"),
                username = item.text("username"),
                email = item.text("email"),
                employeeCode = item.text("employee_code"),
                phone = item.text("phone"),
                active = item.text("status") == "active",
                walletBalanceToman = item["wallet_balance"]?.jsonPrimitive?.longOrNull ?: 0
            )
        }
    }

    suspend fun createInvites(count: Int): Result<List<ManagerInviteItem>> = runCatching {
        require(count in 1..5) { "تعداد کد باید بین ۱ تا ۵ باشد." }
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_manager_create_teacher_invites_v40b",
            buildJsonObject { put("p_count", count) }
        ).decodeAs<JsonObject>().checked()
        raw.inviteItems()
    }

    suspend fun invites(): Result<List<ManagerInviteItem>> = runCatching {
        SupabaseProvider.client.postgrest.rpc("native_manager_invites_v40b")
            .decodeAs<JsonObject>().checked().inviteItems()
    }

    suspend fun revokeInvite(id: String): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "native_manager_revoke_invite_v40b", buildJsonObject { put("p_invite", id) }
        ).decodeAs<JsonObject>().checked(); Unit
    }

    suspend fun transferWallet(teacherId: String, amountToman: Long): Result<WalletTransferResult> = runCatching {
        ManagerWalletRules.validateTransfer(amountToman)
        val operationId = java.util.UUID.randomUUID().toString()
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_manager_transfer_wallet_v38",
            buildJsonObject {
                put("p_teacher", teacherId)
                put("p_amount_toman", amountToman)
                put("p_operation", operationId)
            }
        ).decodeAs<JsonObject>().checked()
        WalletTransferResult(
            amountToman = raw["amount"]?.jsonPrimitive?.longOrNull ?: amountToman,
            managerBalanceToman = raw["manager_balance"]?.jsonPrimitive?.longOrNull ?: 0,
            teacherBalanceToman = raw["teacher_balance"]?.jsonPrimitive?.longOrNull ?: 0
        )
    }

    suspend fun setTeacherActive(id: String, active: Boolean): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "native_manager_set_teacher_active_v40b",
            buildJsonObject { put("p_teacher", id); put("p_active", active) }
        ).decodeAs<JsonObject>().checked(); Unit
    }

    suspend fun removeTeacher(id: String): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "native_manager_remove_teacher_v40b", buildJsonObject { put("p_teacher", id) }
        ).decodeAs<JsonObject>().checked(); Unit
    }

    private fun JsonObject.inviteItems(): List<ManagerInviteItem> =
        (this["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            ManagerInviteItem(
                id = item.text("id"), code = item.text("code"), expiresAt = item.text("expires_at"),
                used = item["used"]?.jsonPrimitive?.booleanOrNull ?: false,
                revoked = item["revoked"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        }

    private fun JsonObject.checked(): JsonObject {
        this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        return this
    }
    private fun JsonObject.text(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}
