package ir.exam.app.ui.math

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import ir.exam.app.ui.figure.InlineFigureView

/**
 * متن ترکیبی سؤال/گزینه. هر `$...$` بدون استثنا از مسیر SVG عبور می‌کند و هر
 * `%%FIG:...%%` نیز به‌صورت شکل SVG رندر می‌شود؛ حتی نمادهای ساده‌ای مانند
 * alpha، Delta و times دیگر با Text خام Compose رندر نمی‌شوند.
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
    val segments = RichTextSplitter.split(source)
    val effectiveSize = if (fontSize == TextUnit.Unspecified) 18.sp else fontSize
    val hasMath = segments.any { it is RichSegment.Math }
    val hasFigure = segments.any { it is RichSegment.Figure }
    if (!hasMath && !hasFigure) {
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
            when (segment) {
                is RichSegment.Math -> NativeFormulaView(
                    tex = segment.tex,
                    fontSize = effectiveSize,
                    color = color,
                    contentDescription = "فرمول ریاضی"
                )
                is RichSegment.Figure -> Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    InlineFigureView(
                        spec = segment.spec,
                        modifier = Modifier.fillMaxWidth(),
                        contentDescription = "شکل"
                    )
                }
                is RichSegment.Text -> if (segment.text.isNotEmpty()) {
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
}
