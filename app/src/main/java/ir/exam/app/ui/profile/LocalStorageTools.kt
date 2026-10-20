package ir.exam.app.ui.profile

import android.content.Context
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.core.push.PushRegistrar
import ir.exam.app.data.local.NativeDatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * V326 — «حافظه و داده‌های محلی» برای صفحهٔ تنظیمات.
 *
 * پیش از این نه حجم مصرفی اپ دیده می‌شد و نه راهی برای پاک‌سازی امن بود؛ در حالی که تصاویر آزمون،
 * پیش‌نمایش‌های چاپ و فایل‌های موقت جمع می‌شوند و روی گوشی‌های کوچک مدرسه محسوس است.
 * همهٔ پاک‌سازی‌ها فقط روی کش و فایل‌های موقت‌اند؛ دادهٔ آزمون‌ها روی سرور است و دست نمی‌خورد.
 */
object LocalStorageTools {

    data class Report(
        val cacheBytes: Long,
        val filesBytes: Long,
        val pendingCount: Int,
        val lastBackupText: String?
    ) {
        val totalBytes: Long get() = cacheBytes + filesBytes
    }

    private fun size(root: File?): Long {
        if (root == null || !root.exists()) return 0
        if (root.isFile) return root.length()
        var sum = 0L
        val stack = ArrayDeque<File>()
        stack.add(root)
        var guard = 0
        while (stack.isNotEmpty() && guard < 20000) {
            guard++
            val kids = stack.removeLast().listFiles() ?: continue
            for (k in kids) if (k.isDirectory) stack.add(k) else sum += k.length()
        }
        return sum
    }

    private fun previewDirs(context: Context): List<File> = listOfNotNull(
        File(context.filesDir, "print"),
        File(context.filesDir, "preview"),
        File(context.filesDir, "tmp"),
        File(context.filesDir, "pending_media")
    )

    suspend fun report(context: Context, userId: String?): Report = withContext(Dispatchers.IO) {
        Report(
            cacheBytes = size(context.cacheDir),
            filesBytes = previewDirs(context).sumOf { size(it) },
            pendingCount = if (userId.isNullOrBlank()) 0 else pendingCount(context, userId),
            lastBackupText = lastBackupText(context)
        )
    }

    /** پاک‌سازی امن: کش + فایل‌های موقت/پیش‌نمایش. صف ارسال و دادهٔ آزمون‌ها دست نمی‌خورند. */
    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        val freed = size(context.cacheDir) + previewDirs(context).sumOf { size(it) }
        runCatching { context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() } }
        previewDirs(context).forEach { runCatching { it.deleteRecursively() } }
        freed
    }

    /** فایل‌های پیش‌نمایش/چاپ قدیمی‌تر از [days] روز. */
    suspend fun clearOldPreviews(context: Context, days: Int = 7): Long = withContext(Dispatchers.IO) {
        val limit = System.currentTimeMillis() - days * 24L * 3600 * 1000
        var freed = 0L
        previewDirs(context).forEach { d ->
            d.listFiles()?.forEach { f ->
                if (f.lastModified() < limit) { freed += size(f); runCatching { f.deleteRecursively() } }
            }
        }
        freed
    }

    /** «آخرین پشتیبان: ۱۲ روز پیش» — زمان در فایل کوچکی کنار پشتیبان ذخیره می‌شود. */
    fun lastBackupText(context: Context): String? {
        val f = File(context.filesDir, "backup_last.txt")
        if (!f.exists()) return null
        val ms = runCatching { f.readText().trim().toLong() }.getOrNull() ?: return null
        val days = ((System.currentTimeMillis() - ms) / (24L * 3600 * 1000)).toInt()
        val date = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(ms))
        val rel = when {
            days <= 0 -> "امروز"
            days == 1 -> "دیروز"
            days < 30 -> "${PersianDigits.convert(days)} روز پیش"
            else -> "بیش از یک ماه پیش"
        }
        return "$date — $rel"
    }

    fun markBackupNow(context: Context) {
        runCatching { File(context.filesDir, "backup_last.txt").writeText(System.currentTimeMillis().toString()) }
    }

    fun humanBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes بایت"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.0f کیلوبایت", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f مگابایت", mb)
        return String.format(Locale.US, "%.2f گیگابایت", mb / 1024.0)
    }

    /** چند مورد در صف ارسال آمادهٔ تلاش است (برای کارت «در انتظار ارسال» در تنظیمات). */
    suspend fun pendingCount(context: Context, userId: String): Int = withContext(Dispatchers.IO) {
        runCatching {
            val db = NativeDatabaseProvider.get(context)
            db.pendingActionDao().ready(limit = 100).count { it.ownerUserId == userId }
        }.getOrDefault(0)
    }

    /** فهرست موارد در انتظار/ناموفق برای نمایش در تنظیمات. */
    suspend fun pendingList(context: Context, userId: String): List<PendingRow> = withContext(Dispatchers.IO) {
        runCatching {
            val db = NativeDatabaseProvider.get(context)
            val dao = db.pendingActionDao()
            (dao.ready(limit = 100).filter { it.ownerUserId == userId } +
                dao.failedForUser(userId)).distinctBy { it.id }.map { PendingRow(it.id, it.state, it.lastError) }
        }.getOrDefault(emptyList())
    }

    data class PendingRow(val id: String, val state: String, val lastError: String?)

    /** تلاش دوباره برای همهٔ موارد صف. */
    suspend fun retryAll(context: Context, userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val db = NativeDatabaseProvider.get(context)
            val repo = ir.exam.app.data.repository.PendingActionRepository(context.applicationContext, db.pendingActionDao())
            repo.retryAll(userId)
        }
    }

    /** حذف موارد ناموفق. */
    suspend fun deleteFailed(context: Context, userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val db = NativeDatabaseProvider.get(context)
            val repo = ir.exam.app.data.repository.PendingActionRepository(context.applicationContext, db.pendingActionDao())
            repo.deleteFailed(userId)
        }
    }

    /** «حذف داده‌های محلی»: کش، پیش‌نمایش‌ها، ثبت اعلان دستگاه و پیش‌نویس‌های Room. */
    suspend fun clearEverythingLocal(context: Context): Long = withContext(Dispatchers.IO) {
        val freed = clearCache(context)
        runCatching { PushRegistrar.unregisterAll(context.applicationContext) }
        runCatching {
            val db = NativeDatabaseProvider.get(context)
            db.clearAllTables()
        }
        freed
    }
}
