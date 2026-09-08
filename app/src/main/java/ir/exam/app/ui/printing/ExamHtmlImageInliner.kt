package ir.exam.app.ui.printing

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.ui.image.PrivateImageLoader
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * آماده‌سازی تصاویر خصوصیِ سؤال برای موتور مستقل چاپ:
 *
 * بعد از خصوصی‌شدن باکت exam-images (V75.8)، نشانی مستقیم تصویر در WebView بدون
 * توکن نشست باز نمی‌شود. این ابزار هر تصویر را با همان بارگذارِ احرازهویت‌شدهٔ برنامه
 * (PrivateImageLoader + SupabaseAuthImageInterceptor) می‌خواند، تا سایز ۱۲۸۰ کوچک
 * می‌کند، به data-URL جی‌پگ تبدیل و به‌صورت توکن درون‌متنیِ %%FIG:{"k":"img",...}%%
 * به انتهای متن سؤال می‌چسباند. موتور مستقل رندر این توکن را در پیش‌نمایش و
 * چاپ می‌کشد؛ بنابراین تصاویر آزمون در برگهٔ چاپ‌شده هم دیده می‌شوند.
 *
 * هر شکستِ تک‌تصویر فقط همان تصویر را حذف می‌کند و هرگز جریان چاپ را نمی‌شکند.
 */
object ExamHtmlImageInliner {

    /** سقف تصاویر هر عملیات چاپ برای محافظت از حافظهٔ WebView. */
    const val MAX_IMAGES = 24
    const val IMAGE_WIDTH_PX = 420

    private const val TAG = "ExamHtmlImageInliner"
    private const val MAX_EDGE = 1280
    private const val JPEG_QUALITY = 85
    private const val MAX_TOTAL_CHARS = 14_000_000L

    /**
     * توکن تصویر درون‌متنیِ قرارداد رندرر:
     * %%FIG:{"k":"img","src":"data:image/jpeg;base64,...","w":420}%%
     * خالص و بدون وابستگی اندروید تا روی JVM هم تست‌پذیر باشد.
     */
    fun imageToken(dataUrl: String): String {
        val json = buildJsonObject {
            put("k", "img")
            put("src", dataUrl)
            put("w", IMAGE_WIDTH_PX)
        }
        return " %%FIG:$json%%"
    }

    // V120 — قبلاً خطای هر عملیات (حتی خطاهایی که ربطی به شبکه/تصویر
    // نداشتند، مثلاً باگ برنامه‌نویسی) با `getOrDefault(printable)` بی‌صدا
    // بلعیده می‌شد و آزمون بدون هیچ تصویری (و بدون هیچ پیام خطایی) چاپ
    // می‌شد. حالا حداقل با Log.w قابل‌ردیابی است.
    suspend fun inline(context: Context, printable: OfficialExamPrintable): OfficialExamPrintable =
        runCatching {
            val loader = PrivateImageLoader.create(context)
            var used = 0
            var budget = MAX_TOTAL_CHARS
            printable.copy(
                questions = printable.questions.map { question ->
                    if (question.imageUrls.isEmpty() || used >= MAX_IMAGES || budget <= 0) {
                        question
                    } else {
                        val tokens = StringBuilder()
                        for (url in question.imageUrls) {
                            if (used >= MAX_IMAGES || budget <= 0) break
                            val dataUrl = loadBitmapDataUrl(loader, url, context)
                            if (dataUrl == null) {
                                Log.w(TAG, "بارگذاریِ تصویرِ سؤال برای چاپ ناموفق بود و از برگه حذف می‌شود: $url")
                                continue
                            }
                            budget -= dataUrl.length
                            used++
                            tokens.append(imageToken(dataUrl))
                        }
                        if (tokens.isEmpty()) question else question.copy(text = question.text + tokens)
                    }
                }
            )
        }.onFailure { error ->
            Log.e(TAG, "آماده‌سازیِ تصاویرِ چاپ کاملاً ناموفق بود؛ آزمون بدون هیچ تصویرِ درون‌متنی چاپ می‌شود.", error)
        }.getOrDefault(printable)

    private suspend fun loadBitmapDataUrl(loader: ImageLoader, url: String, appContext: Context): String? =
        runCatching {
            // V118 — تصویرِ استودیو از قبل data-URL است: بدون Coil، مستقیم دیکد و
            // (در صورت بزرگ‌بودن) کوچک می‌شود؛ پیش‌تر Coil آن را نمی‌شناخت و تصویر
            // به‌کل از چاپ حذف می‌شد.
            if (ir.exam.app.ui.image.DataUrlFetcher.isDataUrl(url)) {
                val bytes = ir.exam.app.ui.image.DataUrlFetcher.decodeBytes(url) ?: return null
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                if (maxOf(bounds.outWidth, bounds.outHeight) <= MAX_EDGE && url.startsWith("data:image/jpeg", true)) return url
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
                val decoded = android.graphics.BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                ) ?: return null
                val scaledData = scaleDown(decoded, MAX_EDGE)
                val out = java.io.ByteArrayOutputStream()
                scaledData.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                return "data:image/jpeg;base64," +
                    android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
            }
            val request = ImageRequest.Builder(appContext)
                .data(url)
                .allowHardware(false)
                .size(MAX_EDGE, MAX_EDGE)
                .build()
            val result = loader.execute(request)
            val bitmap = (result as? SuccessResult)?.drawable?.toBitmap() ?: return null
            val scaled = scaleDown(bitmap, MAX_EDGE)
            val output = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            "data:image/jpeg;base64," +
                android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP)
        }.onFailure { error ->
            Log.w(TAG, "دیکد/کوچک‌سازیِ تصویرِ چاپ ناموفق بود: $url", error)
        }.getOrNull()

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
