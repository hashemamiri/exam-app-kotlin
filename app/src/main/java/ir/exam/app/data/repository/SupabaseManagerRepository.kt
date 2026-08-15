package ir.exam.app.data.repository

import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.ManagerWalletRules
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal data class SchoolTeacherItem(
    val id: String,
    val fullName: String,
    val username: String,
    val email: String,
    val walletBalanceToman: Long = 0
)

internal data class TeacherInviteResult(val code: String)
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
                walletBalanceToman = item["wallet_balance"]?.jsonPrimitive?.longOrNull ?: 0
            )
        }
    }

    suspend fun createInvite(): Result<TeacherInviteResult> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_manager_create_teacher_invite_v39"
        ).decodeAs<JsonObject>().checked()
        TeacherInviteResult(raw.text("invite_code"))
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
