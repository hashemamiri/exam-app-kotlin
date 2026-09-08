package ir.exam.app.ui.printing

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import ir.exam.app.core.figure.AtlasBitmapRenderer
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer
import ir.exam.app.core.math.NativeMathSvgRenderer
import java.io.ByteArrayOutputStream

/**
 * دادهٔ تصویریِ امن برای موتور HTML چاپ.
 *
 * موتور HTML فقط مسئول صفحه‌بندی، سربرگ و تعامل پیش‌نمایش است. فرمول و شکل
 * در این لایه با رندررهای بومیِ فعلی به data URL تبدیل می‌شوند؛ بنابراین asset
 * چاپ هیچ ویرایشگر، iframe یا کتابخانهٔ رندر تکراری ندارد.
 */
internal class ExamPrintAssetRenderer(context: Context) {
    private val appContext = context.applicationContext

    // V120 — قبلاً هر خطا در رندر فرمول/شکل بی‌صدا بلعیده می‌شد و برگه فقط
    // خالی نشان داده می‌شد؛ «منبع خالی» و «رندر خراب» از دیدِ کاربر یکسان
    // بودند. حالا حداقل با Log.w قابل‌ردیابی است (بدونِ تغییرِ قراردادِ پلِ
    // WebView که همچنان یک رشتهٔ خالی برای «چیزی برای نمایش نیست» می‌خواهد).
    fun formulaDataUrl(source: String?): String = runCatching {
        val tex = source.orEmpty().trim().take(MAX_FORMULA_CHARS)
        if (tex.isEmpty()) return ""
        svgDataUrl(NativeMathSvgRenderer.render(tex, fontSizePx = 24f).xml)
    }.onFailure { error ->
        Log.w(TAG, "رندرِ فرمول ناموفق بود؛ در چاپ به‌جای فرمول جای خالی نشان داده می‌شود.", error)
    }.getOrDefault("")

    fun figureDataUrl(rawSpec: String?): String = runCatching {
        val source = rawSpec.orEmpty().take(MAX_FIGURE_SPEC_CHARS)
        val spec = FigureSpec.parse(source) ?: return ""
        if (spec.kind in ATLAS_KINDS) {
            AtlasBitmapRenderer.render(appContext, spec)?.let(::bitmapDataUrl).orEmpty()
        } else {
            svgDataUrl(FigureSvgRenderer.render(spec).xml)
        }
    }.onFailure { error ->
        Log.w(TAG, "رندرِ شکل ناموفق بود؛ در چاپ به‌جای شکل جای خالی نشان داده می‌شود.", error)
    }.getOrDefault("")

    private fun svgDataUrl(svg: String): String =
        "data:image/svg+xml;base64," + Base64.encodeToString(svg.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun bitmapDataUrl(bitmap: Bitmap): String = try {
        val bytes = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes))
        "data:image/png;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
    }

    private companion object {
        const val TAG = "ExamPrintAssetRenderer"
        const val MAX_FORMULA_CHARS = 8_000
        const val MAX_FIGURE_SPEC_CHARS = 100_000
        val ATLAS_KINDS = setOf("a", "s")
    }
}
