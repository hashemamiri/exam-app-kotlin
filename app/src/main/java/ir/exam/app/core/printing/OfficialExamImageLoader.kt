package ir.exam.app.core.printing

import android.content.Context
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.ui.image.PrivateImageLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * بارگذاری Native تصویرهای آزمون برای PDF.
 *
 * مسیر قبلی برای عبور از محدودیت تصویر خصوصی در WebView، bitmap را به data URL
 * و توکن HTML تبدیل می‌کرد. PDF بومی مستقیم همان bitmap را با ImageLoader
 * احرازهویت‌شدهٔ برنامه می‌گیرد؛ بنابراین هم تصویر خصوصی و هم content/file
 * محلی بدون URL واسط در preview و print دیده می‌شوند.
 */
object OfficialExamImageLoader {
    private const val MAX_IMAGES_PER_DOCUMENT = 48

    suspend fun load(context: Context, source: OfficialExamPrintable): OfficialExamPrintable =
        coroutineScope {
            val applicationContext = context.applicationContext
            val loader = PrivateImageLoader.create(applicationContext)
            var remaining = MAX_IMAGES_PER_DOCUMENT
            source.copy(
                questions = source.questions.map { question ->
                    // اگر caller bitmap آماده دارد، دوباره شبکه/دیسک را نخوان.
                    if (question.images.isNotEmpty() || question.imageUrls.isEmpty() || remaining <= 0) {
                        question
                    } else {
                        val urls = question.imageUrls.take(remaining)
                        remaining -= urls.size
                        val images = urls.map { url ->
                            async { loadOne(applicationContext, loader, url) }
                        }.awaitAll().filterNotNull()
                        question.copy(images = images)
                    }
                }
            )
        }

    private suspend fun loadOne(
        context: Context,
        loader: coil.ImageLoader,
        raw: String
    ): android.graphics.Bitmap? = try {
        val uri = Uri.parse(raw)
        val allowed = when (uri.scheme?.lowercase()) {
            "https", "content", "file" -> true
            null -> false
            else -> false
        }
        if (!allowed) {
            null
        } else {
            val request = ImageRequest.Builder(context)
                .data(raw)
                .allowHardware(false)
                .size(1_600, 1_600)
                .build()
            val result = loader.execute(request)
            (result as? SuccessResult)?.drawable?.toBitmap()
        }
    } catch (_: Throwable) {
        // شکست یک فایل خصوصی/خراب، کل PDF را متوقف نمی‌کند.
        null
    }
}
