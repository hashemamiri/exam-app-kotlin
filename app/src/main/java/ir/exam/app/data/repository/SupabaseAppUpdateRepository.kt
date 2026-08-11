package ir.exam.app.data.repository

import io.github.jan.supabase.postgrest.from
import ir.exam.app.core.update.AppUpdateRepository
import ir.exam.app.core.update.RemoteVersion
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * نسخه فعال را از جدول عمومی app_version می‌خواند.
 * RLS این جدول باید فقط اجازه SELECT ردیف‌های فعال را بدهد.
 */
class SupabaseAppUpdateRepository : AppUpdateRepository {
    override suspend fun latest(): Result<RemoteVersion?> = runCatching {
        val latest = SupabaseProvider.client
            .from("app_version")
            .select {
                filter { eq("is_active", true) }
            }
            .decodeList<AppVersionDto>()
            .maxByOrNull(AppVersionDto::versionCode)
            ?: return@runCatching null

        require(latest.versionCode in 1..Int.MAX_VALUE.toLong()) {
            "version_code نسخه سرور نامعتبر است."
        }
        require(latest.versionName.isNotBlank()) { "version_name نسخه سرور خالی است." }
        require(latest.apkUrl.isSecureDownloadUrl()) {
            "نشانی دانلود نسخه جدید باید یک نشانی امن HTTPS باشد."
        }
        latest.apkSha256?.let { checksum ->
            require(checksum.matches(Regex("^[A-Fa-f0-9]{64}$"))) {
                "مقدار SHA-256 ثبت‌شده برای APK نامعتبر است."
            }
        }

        RemoteVersion(
            code = latest.versionCode.toInt(),
            name = latest.versionName.trim(),
            notesFa = latest.notesFa.toNotesList(),
            apkUrl = latest.apkUrl.trim(),
            sha256 = latest.apkSha256?.lowercase(),
            sizeBytes = latest.apkSizeBytes?.takeIf { it > 0 },
            required = latest.isRequired
        )
    }
}

@Serializable
private data class AppVersionDto(
    @SerialName("version_code") val versionCode: Long,
    @SerialName("version_name") val versionName: String,
    @SerialName("notes_fa") val notesFa: JsonElement = JsonArray(emptyList()),
    @SerialName("apk_url") val apkUrl: String,
    @SerialName("apk_sha256") val apkSha256: String? = null,
    @SerialName("apk_size_bytes") val apkSizeBytes: Long? = null,
    @SerialName("is_required") val isRequired: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true
)

private fun JsonElement.toNotesList(): List<String> {
    val values = when (this) {
        is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonPrimitive -> {
            val text = contentOrNull.orEmpty().trim()
            if (text.startsWith("[") && text.endsWith("]")) {
                val parsed = runCatching { Json.parseToJsonElement(text) }.getOrNull()
                if (parsed is JsonArray) {
                    parsed.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                } else {
                    listOf(text)
                }
            } else {
                text.lines()
            }
        }
        else -> emptyList()
    }
    return values.map(String::trim).filter(String::isNotBlank)
}

private fun String.isSecureDownloadUrl(): Boolean {
    val value = trim()
    return value.startsWith("https://", ignoreCase = true) &&
        value.substringAfter("https://", "").substringBefore('/').isNotBlank()
}
