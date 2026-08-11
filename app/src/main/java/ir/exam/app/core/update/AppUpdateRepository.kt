package ir.exam.app.core.update

/** اطلاعات انتشار فعال که از Supabase دریافت می‌شود. */
data class RemoteVersion(
    val code: Int,
    val name: String,
    val notesFa: List<String>,
    val apkUrl: String,
    val sha256: String? = null,
    val sizeBytes: Long? = null,
    val required: Boolean = false
)

/** منبع نسخه مستقل از UI است تا بررسی نسخه قابل تست بماند. */
interface AppUpdateRepository {
    suspend fun latest(): Result<RemoteVersion>
}
