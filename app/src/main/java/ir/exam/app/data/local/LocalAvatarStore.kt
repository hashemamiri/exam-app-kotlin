package ir.exam.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * V137.6 — عکس پروفایل فقط روی حافظهٔ محلی دستگاه نگه داشته می‌شود (برای معلم، دانش‌آموز و مدیر/معاون)
 * و هرگز به سرور/Storage آپلود نمی‌شود. فایل: filesDir/avatars/<userId>.jpg
 */
object LocalAvatarStore {
    private const val DIR = "avatars"
    private const val MAX_DIM = 512

    fun file(context: Context, userId: String): File =
        File(File(context.filesDir, DIR), userId.filter { it.isLetterOrDigit() || it == '-' || it == '_' } + ".jpg")

    /** اگر عکس محلی وجود دارد، مسیر فایل (برای Coil) وگرنه null. */
    fun path(context: Context, userId: String?): File? =
        userId?.takeIf { it.isNotBlank() }?.let { file(context, it) }?.takeIf { it.isFile && it.length() > 0 }

    /** عکس انتخاب‌شده را کوچک (حداکثر ۵۱۲px)، مربع و JPEG می‌کند و محلی ذخیره می‌کند. */
    fun save(context: Context, userId: String, source: Uri): Result<File> = runCatching {
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
            ?: error("خواندن عکس ممکن نشد.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > MAX_DIM * 2 || bounds.outHeight / sample > MAX_DIM * 2) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("عکس معتبر نیست.")
        val side = minOf(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        val scaled = if (side > MAX_DIM) Bitmap.createScaledBitmap(square, MAX_DIM, MAX_DIM, true) else square
        val out = file(context, userId)
        out.parentFile?.mkdirs()
        out.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        out
    }

    fun remove(context: Context, userId: String) {
        runCatching { file(context, userId).delete() }
    }
}
