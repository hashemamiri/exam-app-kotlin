package ir.exam.app.core.media

import android.content.Context
import ir.exam.app.BuildConfig
import ir.exam.app.data.remote.SupabaseProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * V160 — خواندن بایت‌های یک رسانهٔ راه‌دور (تصویر/صوت سؤال) برای ویرایش دوباره.
 *
 * ریشهٔ باگ «تصویر قابل خواندن نیست»: تصویرهایی که قبلاً ذخیره (آپلود) شده‌اند — چه از سایت
 * و چه از خودِ برنامه — نشانی `https://…` دارند، ولی `LocalImageRepository.open` و
 * `decodeDataUrlBounded` فقط `file:`/`data:`/content را می‌فهمیدند.
 *
 * - برای نشانی‌های Storage همین پروژه (باکت خصوصی از V75.8) هدر نشست فرستاده می‌شود
 *   (همان قرارداد SupabaseAuthImageInterceptor / QuestionAudioPlayer).
 * - برای S3 عمومی (ابر آروان، media-upload) بدون هدر.
 * - نتیجه در cacheDir/remote_media_cache ذخیره می‌شود تا decodeِ دوباره (bounds + واقعی) و
 *   بازکردن‌های بعدی بدون شبکه انجام شود.
 */
object RemoteMediaBytes {
    private const val CACHE_DIR = "remote_media_cache"
    private const val MAX_BYTES = 32L * 1024 * 1024

    fun isRemote(value: String?): Boolean =
        value != null && (value.startsWith("https://", true) || value.startsWith("http://", true))

    fun fetch(context: Context, url: String): ByteArray {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val cached = File(dir, url.hashCode().toUInt().toString(16) + ".bin")
        if (cached.isFile && cached.length() > 0) return cached.readBytes()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        authHeaders(url).forEach { (k, v) -> conn.setRequestProperty(k, v) }
        try {
            val code = conn.responseCode
            check(code in 200..299) { "دانلود رسانه ناموفق بود (HTTP $code)" }
            check(conn.contentLengthLong <= MAX_BYTES) { "حجم فایل رسانه بیش از حد مجاز است." }
            val bytes = conn.inputStream.use { it.readBytes() }
            check(bytes.isNotEmpty()) { "پاسخ سرور خالی بود." }
            val tmp = File(dir, cached.name + ".part")
            tmp.writeBytes(bytes)
            tmp.renameTo(cached)
            return bytes
        } finally {
            conn.disconnect()
        }
    }

    fun fetchOrNull(context: Context, url: String): ByteArray? = runCatching { fetch(context, url) }.getOrNull()

    /** نشانی Storage همین پروژه → هدر نشست (باکت خصوصی)؛ بقیه بدون هدر. */
    internal fun authHeaders(url: String): Map<String, String> {
        val base = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        if (base.isBlank() || !url.startsWith("$base/storage/v1/object/")) return emptyMap()
        val token = runCatching { SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken }.getOrNull()
        if (token.isNullOrBlank()) return emptyMap()
        return mapOf("Authorization" to "Bearer $token", "apikey" to BuildConfig.SUPABASE_ANON_KEY)
    }
}
