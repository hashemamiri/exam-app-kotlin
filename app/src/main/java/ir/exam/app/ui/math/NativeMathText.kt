package ir.exam.app.ui.math

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import ir.exam.app.core.math.NativeMathFormatter

@Composable
fun NativeMathText(
    source: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val annotated = rememberMathAnnotated(source)
    Text(
        text = annotated,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
    )
}

private fun rememberMathAnnotated(source: String): AnnotatedString = buildAnnotatedString {
    NativeMathFormatter.segments(source).forEach { segment ->
        if (segment.math) {
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(NativeMathFormatter.renderTex(segment.text))
            pop()
        } else append(segment.text.replace("\\$", "$"))
    }
}
