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

/**
 * منبع نسخه مستقل از UI است تا بررسی نسخه قابل تست بماند.
 * مقدار null یعنی هنوز انتشار فعالی ثبت نشده و برنامه باید آن را «به‌روز» در نظر بگیرد.
 */
interface AppUpdateRepository {
    suspend fun latest(): Result<RemoteVersion?>
}
