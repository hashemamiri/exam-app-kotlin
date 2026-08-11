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

    private suspend fun uploadAt(prefix: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeSampledBitmap(uri, MAX_DIMENSION)
            ?: error("تصویر انتخاب‌شده قابل خواندن نیست.")
        val stream = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.JPEG
        }
        val extension = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "webp" else "jpg"
        check(bitmap.compress(format, QUALITY, stream)) { "فشرده‌سازی تصویر ناموفق بود." }
        bitmap.recycle()
        val bytes = stream.toByteArray()
        check(bytes.size <= MAX_UPLOAD_BYTES) { "حجم تصویر پس از فشرده‌سازی بیش از ۸ مگابایت است." }

        val path = "$prefix/${UUID.randomUUID()}.$extension"
        val bucket = SupabaseProvider.client.storage.from(BUCKET)
        bucket.upload(path, bytes) { upsert = false }
        bucket.publicUrl(path)
    }

    private fun decodeSampledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxDimension * 2 || bounds.outHeight / sample > maxDimension * 2) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { input ->
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
        val largest = maxOf(oriented.width, oriented.height)
        if (largest <= maxDimension) return oriented

        val scale = maxDimension.toFloat() / largest
        val resized = Bitmap.createScaledBitmap(
            oriented,
            (oriented.width * scale).toInt().coerceAtLeast(1),
            (oriented.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (resized !== oriented) oriented.recycle()
        return resized
    }

    private fun List<String?>.countPending(): Int = count { !it.isNullOrBlank() && !it.isRemoteUrl() }
    private fun String.isRemoteUrl(): Boolean = startsWith("https://", true) || startsWith("http://", true)

    private companion object {
        const val BUCKET = "exam-images"
        const val MAX_DIMENSION = 2200
        const val QUALITY = 90
        const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024
    }
}
