package ir.exam.app.core.media

import android.content.Context
import io.github.jan.supabase.auth.auth
import ir.exam.app.BuildConfig
import ir.exam.app.data.remote.SupabaseProvider
import java.io.File

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
        // V335.20 — اگر دامنهٔ مستقیم در این شبکه بسته باشد، همان رسانه از مسیر جایگزین خوانده می‌شود.
        val target = ir.exam.app.core.network.SupabaseRouter.rewrite(url)
        // V335.27 — دانلود با همان کلاینت مشترک (نشانی تثبیت‌شده) تا تصویر/صوت هم در شبکه‌ای که
        // نام دامنه را پیدا نمی‌کند از راه نشانی درست همان سرور خوانده شود.
        val builder = okhttp3.Request.Builder().url(target).get()
        authHeaders(target).forEach { (k, v) -> builder.header(k, v) }
        val client = ir.exam.app.core.network.RelayHttp.client.newBuilder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        client.newCall(builder.build()).execute().use { response ->
            check(response.isSuccessful) { "دانلود رسانه ناموفق بود (HTTP ${response.code})" }
            val body = response.body ?: error("پاسخ سرور خالی بود.")
            check(body.contentLength() <= MAX_BYTES) { "حجم فایل رسانه بیش از حد مجاز است." }
            val bytes = body.bytes()
            check(bytes.isNotEmpty()) { "پاسخ سرور خالی بود." }
            val tmp = File(dir, cached.name + ".part")
            tmp.writeBytes(bytes)
            tmp.renameTo(cached)
            return bytes
        }
    }

    fun fetchOrNull(context: Context, url: String): ByteArray? = runCatching { fetch(context, url) }.getOrNull()

    /** نشانی Storage همین پروژه → هدر نشست (باکت خصوصی)؛ بقیه بدون هدر. */
    internal fun authHeaders(url: String): Map<String, String> {
        // V335.20 — نشانی مستقیم و مسیر جایگزین هر دو «Storage همین پروژه» شمرده می‌شوند.
        val base = ir.exam.app.core.network.SupabaseRouter.directBase.trimEnd('/')
        val ownStorage = url.startsWith("$base/storage/v1/object/") ||
            ir.exam.app.core.network.SupabaseRouter.isOwnStorageUrl(url)
        if (!ownStorage) return emptyMap()
        val token = runCatching { SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken }.getOrNull()
        if (token.isNullOrBlank()) return emptyMap()
        return mapOf("Authorization" to "Bearer $token", "apikey" to BuildConfig.SUPABASE_ANON_KEY)
    }
}
