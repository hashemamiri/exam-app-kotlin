package ir.exam.app.data.repository

import io.github.jan.supabase.storage.storage
import ir.exam.app.data.remote.SupabaseProvider

/**
 * V59.3 — پاک‌سازی تصاویر استوریج هنگام حذف سؤال/آزمون/عکس پروفایل.
 * policy جدید (v59_owner_delete_exam_images) فقط به مالک پوشه اجازهٔ حذف
 * می‌دهد؛ همهٔ فراخوان‌ها best-effort اند و شکستشان عملیات اصلی را بلاک
 * نمی‌کند (GC دوره‌ای storage-maintenance پشتیبان نهایی است).
 */
object StorageImageCleaner {
    private const val BUCKET = "exam-images"
    private const val PUBLIC_MARKER = "/storage/v1/object/public/$BUCKET/"

    /** استخراج مسیر شیء از URL عمومی؛ null اگر URL از این باکت نباشد. */
    fun objectPath(url: String): String? {
        val index = url.indexOf(PUBLIC_MARKER)
        if (index < 0) return null
        return url.substring(index + PUBLIC_MARKER.length).substringBefore('?')
            .takeIf { it.isNotBlank() }
    }

    /** استخراج همهٔ URLهای استوریج داخل یک متن (مثلاً JSON یک سؤال). */
    fun urlsInText(text: String): List<String> =
        Regex("https://[^\"\\s]+$PUBLIC_MARKER[^\"\\s]+")
            .findAll(text).map { it.value }.distinct().toList()

    suspend fun removeByPublicUrls(urls: List<String>) {
        val paths = urls.mapNotNull(::objectPath).distinct()
        if (paths.isEmpty()) return
        runCatching {
            SupabaseProvider.client.storage.from(BUCKET).delete(*paths.toTypedArray())
        }
    }
}
