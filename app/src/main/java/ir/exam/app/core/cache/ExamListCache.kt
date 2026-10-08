package ir.exam.app.core.cache

import android.content.Context
import io.github.jan.supabase.auth.auth
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * V213 — فهرست آزمون‌های معلم (همان ۸ ستون کارت) علاوه بر RAM روی دیسک هم نگه داشته می‌شود تا
 * بعد از بستن کامل برنامه هم صفحهٔ «آزمون‌ها»/«تصحیح» بلافاصله با فهرست قبلی باز شود و در پس‌زمینه
 * تازه شود. فایل خصوصی برنامه: files/list_cache/exams-<uid>.json (فقط عنوان/درس/کد/نمره؛ بدون سؤال).
 * خروج از حساب → حذف فایل‌ها.
 */
object ExamListCache {
    private const val KEY = "exams.list"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Volatile private var dir: File? = null

    fun attach(context: Context) {
        dir = File(context.applicationContext.filesDir, "list_cache").apply { mkdirs() }
    }

    private fun file(): File? {
        val base = dir ?: return null
        val uid = runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.id }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return File(base, "exams-" + uid.filter { it.isLetterOrDigit() || it == '-' } + ".json")
    }

    /** اول RAM، بعد دیسک (IO)؛ null = هیچ نسخهٔ قبلی نیست. */
    suspend fun read(): List<ExamDashboardDto>? {
        SessionCache.get<List<ExamDashboardDto>>(KEY)?.let { return it }
        val f = file() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                if (!f.isFile) null
                else json.decodeFromString(ListSerializer(ExamDashboardDto.serializer()), f.readText())
            }.getOrNull()
        }?.also { SessionCache.put(KEY, it) }
    }

    suspend fun write(list: List<ExamDashboardDto>) {
        SessionCache.put(KEY, list)
        val f = file() ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                val tmp = File(f.parentFile, f.name + ".tmp")
                tmp.writeText(json.encodeToString(ListSerializer(ExamDashboardDto.serializer()), list))
                if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            }
        }
    }

    /** خروج از حساب: فایل‌های همهٔ کاربران این دستگاه پاک می‌شوند (RAM را SessionCache.clear پاک می‌کند). */
    fun clearDisk() {
        dir?.listFiles()?.forEach { runCatching { it.delete() } }
    }
}
