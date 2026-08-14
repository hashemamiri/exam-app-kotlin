package ir.exam.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import ir.exam.app.domain.model.CropRect
import ir.exam.app.domain.model.ImageEditRequest
import ir.exam.app.domain.model.PreparedImage
import ir.exam.app.domain.repository.ImageRepository
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ویرایش واقعی bitmap در فضای خصوصی برنامه؛ فایل خروجی تنها یک بار آپلود می‌شود.
 *
 * قرارداد حافظه (V28): هیچ مسیری اجازه ندارد پروسه را بکشد.
 * - سقف decode بر اساس حافظهٔ آزاد واقعی همان لحظه محاسبه می‌شود، نه یک عدد ثابت.
 * - هر مرحله (decode/rotate/crop/scale) در صورت [OutOfMemoryError] با نمونهٔ
 *   بزرگ‌تر دوباره تلاش می‌شود و bitmapهای میانی بلافاصله recycle می‌شوند.
 * - خطای نهایی به‌صورت [Result.failure] فارسی برمی‌گردد تا UI پیام بدهد و بسته نشود.
 */
class LocalImageRepository(context: Context) : ImageRepository {
    private val appContext = context.applicationContext

    override suspend fun prepare(request: ImageEditRequest): Result<PreparedImage> = runCatching {
        withContext(Dispatchers.IO) {
            cleanupOldFiles()
            var attempt = 0
            var lastError: Throwable? = null
            // هر تلاش، بودجهٔ پیکسل را نصف می‌کند تا روی دستگاه کم‌حافظه هم تمام شود.
            while (attempt < MAX_ATTEMPTS) {
                try {
                    return@withContext renderOnce(request, attempt)
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
    }.recoverCatching { error ->
        // OutOfMemoryError یک Error است؛ اینجا به پیام امن فارسی تبدیل می‌شود
        // تا هیچ‌گاه به‌صورت crash به بالا نرود.
        throw when (error) {
            is OutOfMemoryError -> IllegalStateException(
                "حافظه دستگاه برای این تصویر کافی نیست؛ تصویر کوچک‌تری انتخاب کنید.",
                error
            )
            else -> error
        }
    }

    private fun renderOnce(request: ImageEditRequest, attempt: Int): PreparedImage {
        var working: Bitmap? = null
        try {
            val decoded = decodeSampled(request.source, attempt)
            working = decoded

            val exifRotation = readExifRotation(request.source)
            val totalRotation = (exifRotation + request.rotationDegrees) % 360
            working = rotate(working, totalRotation)

            val crop = request.crop ?: if (request.forceSquare) centerSquare(working) else FULL_CROP
            working = applyCrop(working, crop)
            working = downscale(working, maxEdgeFor(attempt))

            val dir = File(appContext.filesDir, "edited-images").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                check(working!!.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    "ذخیره تصویر ناموفق بود."
                }
            }
            return PreparedImage(
                uri = Uri.fromFile(file),
                wasEdited = request.crop != null || request.rotationDegrees != 0 || request.forceSquare,
                wasCompressed = true
            )
        } finally {
            working?.recycle()
        }
    }

    private fun readExifRotation(uri: Uri): Int = runCatching {
        open(uri)?.use { input ->
            when (
                ExifInterface(input)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    private fun decodeSampled(uri: Uri, attempt: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = open(uri) ?: error("تصویر قابل خواندن نیست.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "ابعاد تصویر نامعتبر است." }

        val maxEdge = maxEdgeFor(attempt)
        val maxPixels = maxPixelsFor(attempt)
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
            // روی تلاش‌های بعدی، نصف حافظه هر پیکسل استفاده می‌شود.
            inPreferredConfig = if (attempt == 0) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        }
        return open(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("تصویر قابل خواندن نیست.")
    }

    /** بودجهٔ پیکسل بر اساس حافظهٔ آزاد واقعی JVM و تلاش جاری. */
    private fun maxPixelsFor(attempt: Int): Long {
        val runtime = Runtime.getRuntime()
        val freeBytes = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        // هر پیکسل ARGB_8888 چهار بایت است و همزمان حداکثر دو bitmap زنده داریم.
        val affordable = (freeBytes / 4 / SAFETY_DIVISOR).coerceAtLeast(MIN_DECODE_PIXELS)
        val ceiling = minOf(MAX_DECODE_PIXELS, affordable)
        return (ceiling shr attempt).coerceAtLeast(MIN_DECODE_PIXELS)
    }

    private fun maxEdgeFor(attempt: Int): Int =
        (MAX_DECODE_EDGE shr attempt).coerceAtLeast(MIN_DECODE_EDGE)

    private fun applyCrop(bitmap: Bitmap, crop: CropRect): Bitmap {
        val safeLeft = crop.left.coerceIn(0f, .95f)
        val safeTop = crop.top.coerceIn(0f, .95f)
        val left = (safeLeft * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (safeTop * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = (crop.width.coerceIn(.05f, 1f - safeLeft) * bitmap.width).toInt()
            .coerceIn(1, bitmap.width - left)
        val height = (crop.height.coerceIn(.05f, 1f - safeTop) * bitmap.height).toInt()
            .coerceIn(1, bitmap.height - top)
        if (left == 0 && top == 0 && width == bitmap.width && height == bitmap.height) return bitmap
        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        if (cropped !== bitmap) bitmap.recycle()
        return cropped
    }

    private fun downscale(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / largest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    override suspend fun upload(image: PreparedImage, folder: String): Result<String> =
        Result.failure(UnsupportedOperationException("آپلود توسط repository مالک رسانه انجام می‌شود."))

    private fun open(uri: Uri): InputStream? =
        if (uri.scheme.equals("file", true)) {
            uri.path?.let(::File)?.takeIf(File::isFile)?.let(::FileInputStream)
        } else {
            appContext.contentResolver.openInputStream(uri)
        }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap =
        if (degrees % 360 == 0) {
            bitmap
        } else {
            Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height,
                Matrix().apply { postRotate(degrees.toFloat()) }, true
            ).also { if (it !== bitmap) bitmap.recycle() }
        }

    private fun centerSquare(bitmap: Bitmap): CropRect = if (bitmap.width > bitmap.height) {
        CropRect(
            (bitmap.width - bitmap.height).toFloat() / bitmap.width / 2f,
            0f,
            bitmap.height.toFloat() / bitmap.width,
            1f
        )
    } else {
        CropRect(
            0f,
            (bitmap.height - bitmap.width).toFloat() / bitmap.height / 2f,
            1f,
            bitmap.width.toFloat() / bitmap.height
        )
    }

    private fun cleanupOldFiles() {
        File(appContext.filesDir, "edited-images").listFiles()
            ?.filter { System.currentTimeMillis() - it.lastModified() > 14L * 24 * 60 * 60 * 1000 }
            ?.forEach(File::delete)
    }

    private companion object {
        val FULL_CROP = CropRect(0f, 0f, 1f, 1f)
        const val MAX_DECODE_EDGE = 2_600
        const val MIN_DECODE_EDGE = 640
        const val MAX_DECODE_PIXELS = 7_000_000L
        const val MIN_DECODE_PIXELS = 480_000L
        const val SAFETY_DIVISOR = 3L
        const val JPEG_QUALITY = 92
        const val MAX_ATTEMPTS = 4
    }
}
