package ir.exam.app.ui.printing

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
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

    fun formulaDataUrl(source: String?): String = runCatching {
        val tex = source.orEmpty().trim().take(MAX_FORMULA_CHARS)
        if (tex.isEmpty()) return ""
        svgDataUrl(NativeMathSvgRenderer.render(tex, fontSizePx = 24f).xml)
    }.getOrDefault("")

    fun figureDataUrl(rawSpec: String?): String = runCatching {
        val source = rawSpec.orEmpty().take(MAX_FIGURE_SPEC_CHARS)
        val spec = FigureSpec.parse(source) ?: return ""
        if (spec.kind in ATLAS_KINDS) {
            AtlasBitmapRenderer.render(appContext, spec)?.let(::bitmapDataUrl).orEmpty()
        } else {
            svgDataUrl(FigureSvgRenderer.render(spec).xml)
        }
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
        const val MAX_FORMULA_CHARS = 8_000
        const val MAX_FIGURE_SPEC_CHARS = 100_000
        val ATLAS_KINDS = setOf("a", "s")
    }
}
