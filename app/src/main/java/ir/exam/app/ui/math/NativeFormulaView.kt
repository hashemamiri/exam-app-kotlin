package ir.exam.app.ui.math

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
import ir.exam.app.core.math.MathSvgEditBox
import ir.exam.app.core.math.NativeMathSvgRenderer

/** نمایش برداری فرمول بدون جعبه‌های ویرایش. */
@Composable
fun NativeFormulaView(
    tex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    color: Color = LocalContentColor.current,
    contentDescription: String = "فرمول ریاضی"
) {
    val rendered = rememberFormulaSvg(tex, fontSize, color)
    NaturalSvgImage(rendered, modifier, contentDescription)
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

/**
 * نسخهٔ تعاملی: همهٔ بخش‌های قابل‌ویرایش rect دارند و مختصات لمس مستقیماً به بازهٔ
 * متن داخلی نگاشت می‌شود. تغییر selection باعث تغییر رنگ خانهٔ فعال در SVG می‌شود.
 */
@Composable
fun NativeFormulaEditorView(
    tex: String,
    selectionStart: Int,
    selectionEnd: Int,
    onBoxTap: (MathSvgEditBox) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    color: Color = LocalContentColor.current,
    contentDescription: String = "ویرایشگر جعبه‌ای فرمول"
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val activeColor = MaterialTheme.colorScheme.primary
    val rendered = rememberFormulaSvg(
        tex = tex,
        fontSize = fontSize,
        color = color,
        showEditBoxes = true,
        activeStart = selectionStart,
        activeEnd = selectionEnd,
        boxColor = outlineColor,
        activeBoxColor = activeColor
    )
    NaturalSvgImage(
        rendered = rendered,
        modifier = modifier,
        contentDescription = contentDescription,
        onBoxTap = onBoxTap
    )
}

private data class RememberedFormulaSvg(
    val document: MathSvgDocument,
    val request: ImageRequest
)

@Composable
private fun NaturalSvgImage(
    rendered: RememberedFormulaSvg,
    modifier: Modifier,
    contentDescription: String,
    onBoxTap: ((MathSvgEditBox) -> Unit)? = null
) {
    val density = LocalDensity.current
    val width = with(density) { rendered.document.widthPx.toDp() }.coerceAtLeast(1.dp)
    val height = with(density) { rendered.document.heightPx.toDp() }.coerceAtLeast(1.dp)
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val gestureModifier = if (onBoxTap == null) {
            Modifier
        } else {
            Modifier.pointerInput(rendered.document.cacheKey) {
                detectTapGestures { point ->
                    rendered.document.editBoxes
                        .asReversed()
                        .firstOrNull { it.contains(point.x, point.y) }
                        ?.let(onBoxTap)
                }
            }
        }
        AsyncImage(
            model = rendered.request,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = gestureModifier.width(width).height(height)
        )
    }
}

@Composable
private fun rememberFormulaSvg(
    tex: String,
    fontSize: TextUnit,
    color: Color,
    showEditBoxes: Boolean = false,
    activeStart: Int = -1,
    activeEnd: Int = activeStart,
    boxColor: Color = Color.Gray,
    activeBoxColor: Color = Color(0xFFFF8F00)
): RememberedFormulaSvg {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }.coerceAtLeast(8f)
    val formulaStyle = color.toSvgStyle()
    val boxStyle = boxColor.toSvgStyle()
    val activeStyle = activeBoxColor.toSvgStyle()
    val document = remember(
        tex,
        fontSizePx,
        formulaStyle,
        showEditBoxes,
        activeStart,
        activeEnd,
        boxStyle,
        activeStyle
    ) {
        NativeMathSvgRenderer.render(
            tex = tex,
            fontSizePx = fontSizePx,
            color = formulaStyle.hex,
            opacity = formulaStyle.alpha,
            showEditBoxes = showEditBoxes,
            activeStart = activeStart,
            activeEnd = activeEnd,
            boxColor = boxStyle.hex,
            activeBoxColor = activeStyle.hex
        )
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

private data class SvgColorStyle(val hex: String, val alpha: Float)

private fun Color.toSvgStyle(): SvgColorStyle {
    val argb = toArgb()
    val red = (argb shr 16) and 0xff
    val green = (argb shr 8) and 0xff
    val blue = argb and 0xff
    val alpha = ((argb ushr 24) and 0xff) / 255f
    return SvgColorStyle("#%02X%02X%02X".format(red, green, blue), alpha)
}
