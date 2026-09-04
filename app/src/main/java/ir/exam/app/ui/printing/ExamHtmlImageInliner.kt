package ir.exam.app.ui.printing

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.ui.image.PrivateImageLoader
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * V76.0 — تزریق تصاویر سؤال به «نسخهٔ 30»:
 *
 * بعد از خصوصی‌شدن باکت exam-images (V75.8)، نشانی مستقیم تصویر در WebView بدون
 * توکن نشست باز نمی‌شود. این ابزار هر تصویر را با همان بارگذارِ احرازهویت‌شدهٔ برنامه
 * (PrivateImageLoader + SupabaseAuthImageInterceptor) می‌خواند، تا سایز ۱۲۸۰ کوچک
 * می‌کند، به data-URL جی‌پگ تبدیل و به‌صورت توکن درون‌متنیِ %%FIG:{"k":"img",...}%%
 * به انتهای متن سؤال می‌چسباند. موتور رندر نسخهٔ 30 همین توکن را در پیش‌نمایش و
 * چاپ می‌کشد؛ بنابراین تصاویر آزمون در برگهٔ چاپ‌شده هم دیده می‌شوند.
 *
 * هر شکستِ تک‌تصویر فقط همان تصویر را حذف می‌کند و هرگز جریان چاپ را نمی‌شکند.
 */
object ExamHtmlImageInliner {

    /** سقف تعداد تصاویر در هر بار ورود به نسخهٔ 30 (محافظ حافظهٔ WebView). */
    const val MAX_IMAGES = 24
    const val IMAGE_WIDTH_PX = 420

    private const val MAX_EDGE = 1280
    private const val JPEG_QUALITY = 85
    private const val MAX_TOTAL_CHARS = 14_000_000L

    /**
     * توکن تصویر درون‌متنی — قالبی که imgNode نسخهٔ 30 می‌شناسد:
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
                            val dataUrl = loadBitmapDataUrl(loader, url, context) ?: continue
                            budget -= dataUrl.length
                            used++
                            tokens.append(imageToken(dataUrl))
                        }
                        if (tokens.isEmpty()) question else question.copy(text = question.text + tokens)
                    }
                }
            )
        }.getOrDefault(printable)

    private suspend fun loadBitmapDataUrl(loader: ImageLoader, url: String, appContext: Context): String? =
        runCatching {
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
