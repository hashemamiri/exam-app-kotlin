package ir.exam.app.ui.math

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ir.exam.app.core.math.NativeMathFormatter

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
    val segments=NativeMathFormatter.segments(source)
    val structural=segments.any { segment -> segment.math && (
        listOf("\\frac","\\sqrt","\\begin","\\sum","\\int").any(segment.text::contains) || '^' in segment.text || '_' in segment.text
    ) }
    val effectiveSize=if(fontSize==TextUnit.Unspecified)18.sp else fontSize
    if(structural){
        Column(modifier){segments.forEach { segment ->
            if(segment.math) NativeFormulaView(segment.text,fontSize=effectiveSize,color=color)
            else Text(segment.text.replace("\\$","$"),color=color,fontSize=fontSize,fontWeight=fontWeight,fontStyle=fontStyle,fontFamily=fontFamily,textAlign=textAlign,style=MaterialTheme.typography.bodyLarge.copy(textDirection=TextDirection.Content))
        }}
    }else{
        Text(
            text = mathAnnotated(source), modifier = modifier, color = color, fontSize = fontSize,
            fontWeight = fontWeight, fontStyle=fontStyle, fontFamily=fontFamily,textAlign=textAlign,
            maxLines = maxLines, overflow = overflow,
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
        )
    }
}

private fun mathAnnotated(source: String): AnnotatedString = buildAnnotatedString {
    NativeMathFormatter.segments(source).forEach { segment ->
        if (segment.math) {
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(NativeMathFormatter.renderTex(segment.text))
            pop()
        } else append(segment.text.replace("\\$", "$"))
    }
}
