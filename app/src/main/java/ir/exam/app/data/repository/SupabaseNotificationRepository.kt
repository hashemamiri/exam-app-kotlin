package ir.exam.app.data.repository

import ir.exam.app.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** V284 — یک اعلان در مرکز اعلان (تاریخچهٔ push_outbox). */
data class AppNotification(
    val id: Long,
    val kind: String,
    val title: String,
    val body: String,
    val page: String?,
    val code: String?,
    val createdAt: String,
    val read: Boolean
)

data class NotificationFeed(val unread: Int, val items: List<AppNotification>)

/** V284 — مرکز اعلان: `native_my_notifications_v284` / `native_notifications_read_v284`. */
class SupabaseNotificationRepository {
    suspend fun feed(limit: Int = 50): Result<NotificationFeed> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_my_notifications_v284", buildJsonObject { put("p_limit", limit) }).decodeAs<JsonObject>()
        val items = (raw["items"] as? JsonArray).orEmpty().map { el ->
            val o = el.jsonObject
            val data = (o["data"] as? JsonObject) ?: JsonObject(emptyMap())
            AppNotification(
                id = o["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                kind = o["kind"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                title = o["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                body = o["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                page = data["page"]?.jsonPrimitive?.contentOrNull,
                code = data["code"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                createdAt = o["created_at"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                read = o["read_at"]?.jsonPrimitive?.contentOrNull != null
            )
        }
        NotificationFeed(unread = raw["unread"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0, items = items)
    }

    suspend fun markAllRead(): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc("native_notifications_read_v284", buildJsonObject { }).decodeAs<JsonObject>()
        Unit
    }

    /** V284.4 — فقط همان اعلان لمس‌شده خوانده‌شده می‌شود. */
    suspend fun markRead(ids: List<Long>): Result<Unit> = runCatching {
        if (ids.isEmpty()) return@runCatching
        SupabaseProvider.client.postgrest.rpc(
            "native_notifications_read_v284",
            buildJsonObject { put("p_ids", JsonArray(ids.map { JsonPrimitive(it) })) }
        ).decodeAs<JsonObject>()
        Unit
    }

    companion object {
        /** V284.4 — دسته‌بندی اعلان‌ها بر اساس kind. */
        val CATEGORIES: List<Pair<String, Set<String>>> = listOf(
            "آزمون‌ها" to setOf("exam_open", "exam_soon", "exam_closing", "submit"),
            "نمره‌ها" to setOf("grade"),
            "تقویم" to setOf("calendar", "calendar_reminder"),
            "کلاس و درخواست‌ها" to setOf("class_added", "manager_request", "request_decided", "student_delete_request"),
            // V300 — اعلان محلی «نسخهٔ جدید برنامه» (UpdateNotice)
            "برنامه" to setOf("app_update")
        )
        const val OTHER = "سایر"
        fun categoryOf(kind: String): String = CATEGORIES.firstOrNull { kind in it.second }?.first ?: OTHER
    }
}
