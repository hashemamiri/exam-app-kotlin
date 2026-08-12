package ir.exam.app.ui.math

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import ir.exam.app.core.math.MathSvgDocument
import ir.exam.app.core.math.NativeMathSvgRenderer

/**
 * نمایش برداری فرمول. TeX هرگز مستقیماً در UI چاپ نمی‌شود؛ ابتدا به AST و سپس به SVG
 * مستقل تبدیل و با decoder بومی Coil/AndroidSVG نمایش داده می‌شود.
 */
@Composable
fun NativeFormulaView(
    tex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    color: Color = LocalContentColor.current,
    contentDescription: String = "فرمول ریاضی"
) {
    val rendered = rememberFormulaSvg(tex, fontSize, color)
    val density = LocalDensity.current
    val width = with(density) { rendered.document.widthPx.toDp() }.coerceAtLeast(1.dp)
    val height = with(density) { rendered.document.heightPx.toDp() }.coerceAtLeast(1.dp)

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = rendered.request,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.width(width).height(height)
        )
    }
}

/** SVG فرمول با کادر ثابت برای کتابخانه، منوها، keypad و دکمه‌های نماد. */
@Composable
fun NativeFormulaIcon(
    tex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    color: Color = LocalContentColor.current,
    contentDescription: String = "نماد ریاضی"
) {
    val rendered = rememberFormulaSvg(tex, fontSize, color)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = rendered.request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private data class RememberedFormulaSvg(
    val document: MathSvgDocument,
    val request: ImageRequest
)

@Composable
private fun rememberFormulaSvg(tex: String, fontSize: TextUnit, color: Color): RememberedFormulaSvg {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }.coerceAtLeast(8f)
    val argb = color.toArgb()
    val red = (argb shr 16) and 0xff
    val green = (argb shr 8) and 0xff
    val blue = argb and 0xff
    val alpha = ((argb ushr 24) and 0xff) / 255f
    val hex = "#%02X%02X%02X".format(red, green, blue)
    val document = remember(tex, fontSizePx, hex, alpha) {
        NativeMathSvgRenderer.render(tex, fontSizePx, hex, alpha)
    }
    val bytes = remember(document.cacheKey) { document.xml.toByteArray(Charsets.UTF_8) }
    val request = remember(context, document.cacheKey) {
        ImageRequest.Builder(context)
            .data(bytes)
            .decoderFactory(SvgDecoder.Factory())
            .memoryCacheKey(document.cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .crossfade(false)
            .build()
    }
    return RememberedFormulaSvg(document, request)
}
