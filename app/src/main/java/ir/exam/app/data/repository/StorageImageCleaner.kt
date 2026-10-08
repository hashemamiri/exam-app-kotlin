package ir.exam.app.data.repository

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.storage.storage
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * V59.3 — پاک‌سازی تصاویر استوریج هنگام حذف سؤال/آزمون/عکس پروفایل.
 * policy جدید (v59_owner_delete_exam_images) فقط به مالک پوشه اجازهٔ حذف
 * می‌دهد؛ همهٔ فراخوان‌ها best-effort اند و شکستشان عملیات اصلی را بلاک
 * نمی‌کند (GC دوره‌ای storage-maintenance پشتیبان نهایی است).
 *
 * V221 — فایل‌های فضای ابری S3/R2 (media-upload) هم پوشش داده می‌شوند: هر آدرسی که مسیرش به شکل
 * `<پوشهٔ مجاز>/<کاربر>/<آزمون>/<نام>` باشد به تابع لبهٔ media-upload با action=delete فرستاده می‌شود
 * (سرور مالکیت را از بخش دوم مسیر بررسی می‌کند). آدرس‌های باکت سوپابیس همچنان مستقیم حذف می‌شوند.
 */
object StorageImageCleaner {
    private const val BUCKET = "exam-images"
    private const val PUBLIC_MARKER = "/storage/v1/object/public/$BUCKET/"
    private val OWNED_PATH = Regex("^https://[^\\s\"']+/(questions|option_images|matching_images|audio|answers|profiles)/[0-9a-fA-F-]{36}/[^\\s\"']+$")

    /** استخراج مسیر شیء از URL عمومی؛ null اگر URL از این باکت نباشد. */
    fun objectPath(url: String): String? {
        val index = url.indexOf(PUBLIC_MARKER)
        if (index < 0) return null
        return url.substring(index + PUBLIC_MARKER.length).substringBefore('?')
            .takeIf { it.isNotBlank() }
    }

    /** V221 — آیا این آدرس یک فایل آپلودشدهٔ ما (سوپابیس یا S3/R2) است؟ */
    fun isOwnedMediaUrl(url: String): Boolean = objectPath(url) != null || OWNED_PATH.matches(url.substringBefore('?'))

    /** استخراج همهٔ URLهای فضای ابری (سوپابیس + S3/R2) داخل یک متن (JSON یا toString دیتاکلاس). */
    fun urlsInText(text: String): List<String> =
        // V221 — متن می‌تواند toString دیتاکلاس باشد (جداکننده‌های , ) ] ' و \) — آدرس در این‌ها تمام می‌شود.
        Regex("https://[^\"'\\s,)\\]\\\\]+")
            .findAll(text).map { it.value.substringBefore('?') }
            .filter(::isOwnedMediaUrl).distinct().toList()

    suspend fun removeByPublicUrls(urls: List<String>) {
        val clean = urls.map { it.substringBefore('?') }.distinct()
        val paths = clean.mapNotNull(::objectPath).distinct()
        if (paths.isNotEmpty()) runCatching {
            SupabaseProvider.client.storage.from(BUCKET).delete(*paths.toTypedArray())
        }
        val remote = clean.filter { objectPath(it) == null && OWNED_PATH.matches(it) }
        remote.chunked(50).forEach { chunk ->
            runCatching {
                SupabaseProvider.client.functions.invoke(
                    "media-upload",
                    body = buildJsonObject {
                        put("action", "delete")
                        put("urls", buildJsonArray { chunk.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                    }
                )
            }
        }
    }
}
