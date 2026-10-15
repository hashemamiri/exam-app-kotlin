package ir.exam.app.core.update

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * V300 — پل بین «بررسی نسخه» و مرکز اعلان: وقتی نسخهٔ جدیدی روی سرور باشد، زنگولهٔ اعلان یک مورد محلی
 * «نسخهٔ جدید برنامه» (دستهٔ «برنامه») نشان می‌دهد که فقط تیترهای تغییرات مربوط به اپ را دارد
 * (بدون شمارهٔ داخلی Vxxx و بدون تغییرات «فقط سایت»). لمس آن پنجرهٔ بروزرسانی را دوباره باز می‌کند.
 * هیچ‌چیز به سرور نمی‌رود؛ وضعیت «دیده‌شده» فقط در همین اجرا نگه داشته می‌شود.
 */
object UpdateNotice {
    data class Info(val versionName: String, val bullets: List<String>, val createdAt: String)

    const val KIND = "app_update"
    const val LOCAL_ID = -300L
    const val TITLE = "نسخهٔ جدید برنامه آماده است"

    val info = MutableStateFlow<Info?>(null)
    val seen = MutableStateFlow(false)

    /** توسط ExamApp تنظیم می‌شود: باز کردن دوبارهٔ پنجرهٔ بروزرسانی. */
    @Volatile var onOpen: (() -> Unit)? = null

    fun publish(versionName: String, notesFa: List<String>) {
        val bullets = ReleaseNotes.noticeBullets(notesFa).ifEmpty { listOf("نسخهٔ تازه‌ای از برنامه برای دریافت آماده است.") }
        val current = info.value
        if (current?.versionName != versionName) {
            seen.value = false
            info.value = Info(versionName, bullets, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString())
        } else if (current.bullets != bullets) {
            info.value = current.copy(bullets = bullets)
        }
    }

    fun clear() { info.value = null }

    /** متن بدنهٔ اعلان: هر تیتر در یک سطر. */
    fun body(i: Info): String = i.bullets.joinToString("\n") { "• $it" }
}
