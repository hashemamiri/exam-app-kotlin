package ir.exam.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import io.github.jan.supabase.storage.storage
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.builder.QuestionDraft
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** آپلود و فشرده‌سازی مشترک تصاویر سؤال، گزینه، جورکردنی و پاسخ دانش‌آموز. */
class SupabaseQuestionImageUploader(context: Context) {
    private val appContext = context.applicationContext

    suspend fun uploadPending(
        teacherId: String,
        examId: String,
        questions: List<QuestionDraft>,
        onProgress: (done: Int, total: Int) -> Unit
    ): List<QuestionDraft> {
        val pendingCount = questions.sumOf { question ->
            question.images.count { !it.uri.isRemoteUrl() } +
                question.optionImages.countPending() +
                question.matchingLeftImages.countPending() +
                question.matchingRightImages.countPending()
        }
        if (pendingCount == 0) return questions
        var done = 0

        suspend fun upload(value: String?, folder: String): String? {
            if (value.isNullOrBlank() || value.isRemoteUrl()) return value
            val url = uploadAt("$folder/$teacherId/$examId", Uri.parse(value))
            done += 1
            onProgress(done, pendingCount)
            return url
        }

        return questions.map { question ->
            question.copy(
                images = question.images.map { image ->
                    if (image.uri.isRemoteUrl()) image
                    else image.copy(uri = uploadAt("questions/$teacherId/$examId", Uri.parse(image.uri)).also {
                        done += 1
                        onProgress(done, pendingCount)
                    })
                },
                optionImages = question.optionImages.map { upload(it, "option_images") },
                matchingLeftImages = question.matchingLeftImages.map { upload(it, "matching") },
                matchingRightImages = question.matchingRightImages.map { upload(it, "matching") }
            )
        }
    }

    suspend fun uploadAnswer(
        studentId: String,
        examId: String,
        questionId: String,
        uri: String
    ): String {
        if (uri.isRemoteUrl()) return uri
        return uploadAt("answers/$studentId/$examId/$questionId", Uri.parse(uri))
    }

    /** آواتار با مسیر مالک‌محور و برش مرکزی مربع ذخیره می‌شود. */
    suspend fun uploadAvatar(userId: String, uri: Uri): String =
        uploadAt("avatars/$userId", uri, maxDimension = AVATAR_MAX_DIMENSION, quality = AVATAR_QUALITY, forceSquare = true)

    private suspend fun uploadAt(
        prefix: String,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION,
        quality: Int = QUALITY,
        forceSquare: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        // قرارداد حافظه مثل LocalImageRepository: OutOfMemoryError یک Error است و
        // runCatching آن را نمی‌گیرد؛ باید صریحاً گرفته و با بودجهٔ کمتر دوباره تلاش شود
        // تا آپلود تصویر بزرگ هرگز برنامه را نکشد.
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < MAX_ATTEMPTS) {
            try {
                return@withContext uploadOnce(prefix, uri, maxDimension, quality, forceSquare, attempt)
            } catch (oom: OutOfMemoryError) {
                lastError = oom
                attempt++
                System.gc()
            }
        }
        throw IllegalStateException(
            "حافظه دستگاه برای این تصویر کافی نیست؛ تصویر کوچک‌تری انتخاب کنید.",
            lastError
        )
    }

    private suspend fun uploadOnce(
        prefix: String,
        uri: Uri,
        maxDimension: Int,
        quality: Int,
        forceSquare: Boolean,
        attempt: Int
    ): String {
        val bitmap = decodeSampledBitmap(uri, maxDimension, forceSquare, attempt)
            ?: error("تصویر انتخاب‌شده قابل خواندن نیست.")
        // bitmap روی هر مسیر (حتی خطا/OutOfMemoryError در مراحل بعدی) آزاد می‌شود
        // تا تلاش‌های بعدی حلقهٔ retry حافظهٔ کافی داشته باشند.
        try {
            val stream = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.JPEG
            }
            val extension = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "webp" else "jpg"
            check(bitmap.compress(format, quality, stream)) { "فشرده‌سازی تصویر ناموفق بود." }
            val bytes = stream.toByteArray()
            check(bytes.size <= MAX_UPLOAD_BYTES) { "حجم تصویر پس از فشرده‌سازی بیش از ۸ مگابایت است." }

            val path = "$prefix/${UUID.randomUUID()}.$extension"
            val bucket = SupabaseProvider.client.storage.from(BUCKET)
            bucket.upload(path, bytes) { upsert = false }
            return bucket.publicUrl(path)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSampledBitmap(
        uri: Uri,
        maxDimension: Int,
        forceSquare: Boolean = false,
        attempt: Int = 0
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInput(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // بودجهٔ پیکسل بر اساس حافظهٔ آزاد واقعی و تلاش جاری؛ در تلاش‌های بعدی نصف
        // می‌شود و پیکسل‌ها به RGB_565 تقلیل می‌یابند. لبهٔ مجاز از همان تلاش اول
        // از maxDimension بیشتر نمی‌شود تا فشاری بی‌مورد به حافظه نیاید.
        val maxEdge = (maxDimension shr attempt).coerceAtLeast(MIN_DECODE_EDGE)
        val runtime = Runtime.getRuntime()
        val freeBytes = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        val affordable = (freeBytes / 4 / SAFETY_DIVISOR).coerceAtLeast(MIN_DECODE_PIXELS)
        val maxPixels = (minOf(MAX_DECODE_PIXELS, affordable) shr attempt).coerceAtLeast(MIN_DECODE_PIXELS)

        var sample = 1
        while (
            bounds.outWidth / sample > maxEdge ||
            bounds.outHeight / sample > maxEdge ||
            bounds.outWidth.toLong() / sample * (bounds.outHeight.toLong() / sample) > maxPixels
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = if (attempt == 0) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        }
        val decoded = openInput(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        // هر bitmap میانی روی هر خطا (به‌ویژه OutOfMemoryError) بازیافت می‌شود
        // تا حلقهٔ retry در uploadAt با نشتی حافظه مواجه نشود.
        var current: Bitmap = decoded
        try {
            val rotation = runCatching {
                openInput(uri)?.use { input ->
                    when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            }.getOrDefault(0f)
            val oriented = if (rotation == 0f) decoded else {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
                    .also { if (it !== decoded) decoded.recycle() }
            }
            current = oriented
            val cropped = if (forceSquare && oriented.width != oriented.height) {
                val side = minOf(oriented.width, oriented.height)
                Bitmap.createBitmap(
                    oriented,
                    (oriented.width - side) / 2,
                    (oriented.height - side) / 2,
                    side,
                    side
                ).also { if (it !== oriented) oriented.recycle() }
            } else oriented
            current = cropped

            val largest = maxOf(cropped.width, cropped.height)
            if (largest <= maxDimension) return cropped

            val scale = maxDimension.toFloat() / largest
            val resized = Bitmap.createScaledBitmap(
                cropped,
                (cropped.width * scale).toInt().coerceAtLeast(1),
                (cropped.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (resized !== cropped) cropped.recycle()
            return resized
        } catch (t: Throwable) {
            current.recycle()
            throw t
        }
    }

    private fun openInput(uri: Uri): InputStream? = if (uri.scheme.equals("file", true)) {
        uri.path?.let(::File)?.takeIf(File::isFile)?.let(::FileInputStream)
    } else {
        appContext.contentResolver.openInputStream(uri)
    }

    private fun List<String?>.countPending(): Int = count { !it.isNullOrBlank() && !it.isRemoteUrl() }
    private fun String.isRemoteUrl(): Boolean = startsWith("https://", true) || startsWith("http://", true)

    private companion object {
        const val BUCKET = "exam-images"
        const val MAX_DIMENSION = 2200
        const val QUALITY = 90
        const val AVATAR_MAX_DIMENSION = 1024
        const val AVATAR_QUALITY = 88
        const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024
        const val MAX_ATTEMPTS = 4
        const val MIN_DECODE_EDGE = 640
        const val MAX_DECODE_PIXELS = 7_000_000L
        const val MIN_DECODE_PIXELS = 480_000L
        const val SAFETY_DIVISOR = 3L
    }
}
