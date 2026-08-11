package ir.exam.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import io.github.jan.supabase.storage.storage
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.QuestionDraft
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** آپلود واقعی تصاویر سؤال؛ URLهای قبلی دوباره آپلود نمی‌شوند. */
class SupabaseQuestionImageUploader(context: Context) {
    private val appContext = context.applicationContext

    suspend fun uploadPending(
        teacherId: String,
        examId: String,
        questions: List<QuestionDraft>,
        onProgress: (done: Int, total: Int) -> Unit
    ): List<QuestionDraft> {
        val pendingCount = questions.sumOf { question -> question.images.count { !it.uri.isRemoteUrl() } }
        if (pendingCount == 0) return questions
        var done = 0

        return questions.map { question ->
            question.copy(images = question.images.map { image ->
                if (image.uri.isRemoteUrl()) image
                else {
                    val url = uploadOne(teacherId, examId, Uri.parse(image.uri))
                    done += 1
                    onProgress(done, pendingCount)
                    image.copy(uri = url)
                }
            })
        }
    }

    private suspend fun uploadOne(teacherId: String, examId: String, uri: Uri): String =
        withContext(Dispatchers.IO) {
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

            val path = "questions/$teacherId/$examId/${UUID.randomUUID()}.$extension"
            val bucket = SupabaseProvider.client.storage.from(BUCKET)
            bucket.upload(path, bytes) {
                upsert = false
            }
            bucket.publicUrl(path)
        }

    private fun decodeSampledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxDimension * 2 || bounds.outHeight / sample > maxDimension * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        val largest = maxOf(decoded.width, decoded.height)
        if (largest <= maxDimension) return decoded

        val scale = maxDimension.toFloat() / largest.toFloat()
        val resized = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (resized !== decoded) decoded.recycle()
        return resized
    }

    private fun String.isRemoteUrl(): Boolean =
        startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

    private companion object {
        const val BUCKET = "exam-images"
        const val MAX_DIMENSION = 2200
        const val QUALITY = 90
        const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024
    }
}
