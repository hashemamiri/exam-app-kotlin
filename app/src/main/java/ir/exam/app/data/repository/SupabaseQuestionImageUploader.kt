package ir.exam.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
            (if (!question.audioUri.isNullOrBlank() && !question.audioUri.isRemoteUrl()) 1 else 0) +
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
                // V135 — فایل صوتی فشرده‌شده (m4a ≤ 3MB) بدون تغییر آپلود می‌شود.
                audioUri = question.audioUri?.let { a ->
                    if (a.isBlank() || a.isRemoteUrl()) a
                    else uploadAudioAt("audio/$teacherId/$examId", Uri.parse(a)).also {
                        done += 1
                        onProgress(done, pendingCount)
                    }
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
            ?.let(::flattenOnWhite)
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

            // V145 — اول ذخیره‌ساز S3 (ابر آروان) از طریق media-upload؛ اگر پیکربندی نشده بود، Supabase Storage.
            return uploadBytes(prefix, bytes, extension, if (extension == "webp") "image/webp" else "image/jpeg", "image")
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * V137.1 — تصویرِ دارای کانال شفافیت (PNG تخته/اسکرین‌شات) پیش از فشرده‌سازیِ JPEG/WEBP_LOSSY
     * روی سفید می‌نشیند؛ در غیر این صورت پیکسل‌های شفاف سیاه می‌شوند (تصویرِ سیاهِ تخته نزد معلم).
     */
    private fun flattenOnWhite(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val flat = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(flat).apply {
            drawColor(android.graphics.Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        source.recycle()
        flat.setHasAlpha(false)
        return flat
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

    /** V135 — آپلود فایل صوتی آمادهٔ سؤال (خروجی AudioTranscoder؛ سقف ۳MB دوباره بررسی می‌شود). */
    private suspend fun uploadAudioAt(prefix: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = openInput(uri)?.use { it.readBytes() } ?: error("فایل صوتی سؤال قابل خواندن نیست.")
        check(bytes.isNotEmpty()) { "فایل صوتی خالی است." }
        check(bytes.size <= MAX_AUDIO_BYTES) { "حجم فایل صوتی بیش از ۳ مگابایت است." }
        uploadBytes(prefix, bytes, "m4a", "audio/mp4", "audio")
    }

    /**
     * V145 — مسیر مشترک آپلود (تصویر و صدا): ابتدا از Edge Function `media-upload` لینک PUT امضاشده برای
     * ذخیره‌ساز S3 (ابر آروان) گرفته می‌شود و فایل مستقیم PUT می‌شود — دقیقاً همان کاری که سایت می‌کند، پس
     * رسانهٔ برنامه و سایت در یک جا می‌نشیند و در هر دو دیده می‌شود. اگر سرور 503/`r2_not_configured`
     * برگرداند، تا پایان عمر فرایند به Supabase Storage (باکت exam-images، مسیر قبلی) برمی‌گردیم.
     * prefix = "<folder>/<teacherId>/<examId>" — همان قرارداد سایت و storage-maintenance.
     */
    private suspend fun uploadBytes(prefix: String, bytes: ByteArray, extension: String, contentType: String, kind: String): String {
        val parts = prefix.split('/')
        // فقط رسانهٔ سؤال معلم (سرور برای نقش دانش‌آموز/پوشه‌های دیگر 403 می‌دهد)؛ پاسخ دانش‌آموز همچنان در Supabase Storage.
        val folder = when (parts.getOrNull(0)) {
            "questions" -> "questions"
            "option_images" -> "option_images"
            "matching" -> "matching_images"
            "audio" -> "audio"
            else -> ""
        }
        val examId = parts.getOrNull(2).orEmpty()
        if (!s3Disabled && folder.isNotBlank() && parts.size == 3 && examId.isNotBlank()) {
            try {
                val response: HttpResponse = SupabaseProvider.client.functions.invoke(
                    "media-upload",
                    body = buildJsonObject {
                        put("kind", kind)
                        put("folder", folder)
                        put("exam_id", examId)
                        put("ext", extension)
                        put("size", bytes.size)
                    }
                )
                val obj: JsonObject = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val uploadUrl = obj["upload_url"]?.jsonPrimitive?.contentOrNull
                val publicUrl = obj["public_url"]?.jsonPrimitive?.contentOrNull
                if (!uploadUrl.isNullOrBlank() && !publicUrl.isNullOrBlank()) {
                    val signedType = obj["headers"]?.jsonObject?.get("Content-Type")?.jsonPrimitive?.contentOrNull ?: contentType
                    val putResponse = SupabaseProvider.client.httpClient.httpClient.put(uploadUrl) {
                        header("Content-Type", signedType)
                        setBody(bytes)
                    }
                    check(putResponse.status.isSuccess()) {
                        "آپلود به فضای ابری ناموفق بود (S3 PUT ${putResponse.status.value}): " +
                            putResponse.bodyAsText().replace(Regex("<[^>]+>"), " ").trim().take(200)
                    }
                    return publicUrl
                }
                val code = obj["error"]?.jsonPrimitive?.contentOrNull
                if (code == "r2_not_configured") {
                    s3Disabled = true
                } else {
                    error(obj["message"]?.jsonPrimitive?.contentOrNull ?: code ?: "media-upload: پاسخ نامعتبر")
                }
            } catch (e: RestException) {
                // 503 = ذخیره‌ساز S3 پیکربندی نشده → بازگشت به Supabase Storage؛ سایر خطاها واقعی‌اند.
                if (e.statusCode == HttpStatusCode.ServiceUnavailable.value) {
                    s3Disabled = true
                } else {
                    throw IllegalStateException("آپلود به فضای ابری ناموفق بود: ${e.message}", e)
                }
            }
        }
        val path = "$prefix/${UUID.randomUUID()}.$extension"
        val bucket = SupabaseProvider.client.storage.from(BUCKET)
        val slash = contentType.indexOf('/')
        // V135.4 — نوع محتوا صریح؛ باکت باید audio/mp4 را مجاز داشته باشد (SQL_NATIVE_MEDIA_COST_V135_MIME.sql).
        bucket.upload(path, bytes) {
            upsert = false
            this.contentType = ContentType(contentType.substring(0, slash), contentType.substring(slash + 1))
        }
        return bucket.publicUrl(path)
    }

    private fun openInput(uri: Uri): InputStream? = if (uri.scheme.equals("file", true)) {
        uri.path?.let(::File)?.takeIf(File::isFile)?.let(::FileInputStream)
    } else if (uri.scheme.equals("data", true)) {
        // V145 — data:image/... هم مثل فایل محلی آپلود می‌شود تا در سایت هم دیده شود.
        ir.exam.app.ui.image.DataUrlFetcher.decodeBytes(uri.toString())?.let(::java.io.ByteArrayInputStream)
    } else {
        appContext.contentResolver.openInputStream(uri)
    }

    private fun List<String?>.countPending(): Int = count { !it.isNullOrBlank() && !it.isRemoteUrl() }
    private fun String.isRemoteUrl(): Boolean = startsWith("https://", true) || startsWith("http://", true)

    private companion object {
        /** V145 — پس از اولین پاسخ «S3 پیکربندی نشده» دیگر تلاش نمی‌کنیم (تا راه‌اندازی بعدی برنامه). */
        @Volatile private var s3Disabled = false
        const val BUCKET = "exam-images"
        const val MAX_DIMENSION = 2200
        const val QUALITY = 90
        const val AVATAR_MAX_DIMENSION = 1024
        const val AVATAR_QUALITY = 88
        const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024
        const val MAX_AUDIO_BYTES = 3 * 1024 * 1024
        const val MAX_ATTEMPTS = 4
        const val MIN_DECODE_EDGE = 640
        const val MAX_DECODE_PIXELS = 7_000_000L
        const val MIN_DECODE_PIXELS = 480_000L
        const val SAFETY_DIVISOR = 3L
    }
}
