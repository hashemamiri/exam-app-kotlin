package ir.exam.app.ui.math

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ir.exam.app.core.math.NativeMathFormatter

/**
 * متن ترکیبی سؤال/گزینه. هر segment محصور در `$...$` بدون استثنا از مسیر SVG عبور می‌کند؛
 * حتی نمادهای ساده‌ای مانند alpha، Delta و times دیگر با Text خام Compose رندر نمی‌شوند.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NativeMathText(
    source: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val segments = NativeMathFormatter.segments(source)
    val effectiveSize = if (fontSize == TextUnit.Unspecified) 18.sp else fontSize
    if (segments.none { it.math }) {
        Text(
            text = source.replace("\\$", "$"),
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
        )
        return
    }

    FlowRow(modifier = modifier) {
        segments.forEach { segment ->
            if (segment.math) {
                NativeFormulaView(
                    tex = segment.text,
                    fontSize = effectiveSize,
                    color = color,
                    contentDescription = "فرمول ریاضی"
                )
            } else if (segment.text.isNotEmpty()) {
                Text(
                    text = segment.text.replace("\\$", "$"),
                    color = color,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    fontFamily = fontFamily,
                    textAlign = textAlign,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
                )
            }
        }
    }
}
