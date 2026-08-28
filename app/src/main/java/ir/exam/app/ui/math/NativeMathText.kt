package ir.exam.app.ui.math

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import ir.exam.app.ui.figure.AtlasFigureView
import ir.exam.app.ui.figure.InlineFigureView
import ir.exam.app.ui.figure.ZoomableFigureDialog

/**
 * متن ترکیبی سؤال/گزینه. هر `$...$` بدون استثنا از مسیر SVG عبور می‌کند و هر
 * `%%FIG:...%%` نیز به‌صورت شکل SVG رندر می‌شود؛ حتی نمادهای ساده‌ای مانند
 * alpha، Delta و times دیگر با Text خام Compose رندر نمی‌شوند.
 *
 * V57.0 — سطربندی وفادار به معلم: هر اینتر معلم یک سطر جدید است
 * (RichTextSplitter.splitRows)؛ شکل/نمودار همیشه سطر کامل خودش را می‌گیرد و
 * اگر معلم در همان سطر متن نوشته باشد شکل به سطر پایین‌تر می‌رود. با
 * `zoomableFigures=true` (سمت دانش‌آموز) لمس هر شکل پنجرهٔ زوم تمام‌صفحه باز
 * می‌کند و جدول تناوبی گزینهٔ «نمایش افقی» دارد.
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
    overflow: TextOverflow = TextOverflow.Clip,
    zoomableFigures: Boolean = false,
    // V57.0 — پاسخ‌های نامگذاری اطلس (سمت دانش‌آموز): کلید = شمارهٔ نشانه.
    atlasBlankAnswers: Map<Int, String>? = null,
    onAtlasBlankAnswer: ((Int, String) -> Unit)? = null,
    // V58.0 — پنل معلم به جای خالی/کادر نامگذاری نیاز ندارد؛ چاپ و دانش‌آموز دارند.
    showAtlasBlanks: Boolean = true
) {
    // متن ترکیبی در بازترکیب‌های ناشی از تایمر/انتخاب دوباره parse نشود؛
    // فقط با تغییر خود source محاسبه شود.
    val rows = remember(source) { RichTextSplitter.splitRows(source) }
    val effectiveSize = if (fontSize == TextUnit.Unspecified) 18.sp else fontSize
    val flat = rows.flatten()
    val hasMath = flat.any { it is RichSegment.Math }
    val hasFigure = flat.any { it is RichSegment.Figure }
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

    var zoomSpec by remember(source) { mutableStateOf<FigureSpec?>(null) }

    Column(modifier) {
        rows.forEach { segments ->
            val figure = segments.singleOrNull() as? RichSegment.Figure
            if (figure != null) {
                // شکل/نمودار: سطر کامل و تمام‌عرض خودش.
                if (figure.spec.kind in setOf("a", "s")) {
                    AtlasFigureView(
                        spec = figure.spec,
                        modifier = Modifier.fillMaxWidth(),
                        contentDescription = "شکل",
                        showBlanks = showAtlasBlanks,
                        blankAnswers = atlasBlankAnswers,
                        onBlankAnswer = onAtlasBlankAnswer,
                        // زوم فقط با لمس خود تصویر؛ کادرهای تایپ آزاد می‌مانند.
                        onImageTap = if (zoomableFigures) ({ zoomSpec = figure.spec }) else null
                    )
                } else Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (zoomableFigures) Modifier.clickable { zoomSpec = figure.spec }
                            else Modifier
                        )
                        .height(150.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    InlineFigureView(
                        spec = figure.spec,
                        modifier = Modifier.fillMaxWidth(),
                        contentDescription = "شکل"
                    )
                }
            } else if (segments.isEmpty()) {
                // سطر خالی عمدی معلم (اینتر پشت‌سرهم) حفظ می‌شود.
                Text(" ", fontSize = effectiveSize)
            } else FlowRow(Modifier.fillMaxWidth()) {
                segments.forEach { segment ->
                    when (segment) {
                        is RichSegment.Math -> NativeFormulaView(
                            tex = segment.tex,
                            fontSize = effectiveSize,
                            color = color,
                            contentDescription = "فرمول ریاضی"
                        )
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
                        is RichSegment.Figure -> Unit // شکل‌ها همیشه سطر مستقل دارند.
                    }
                }
            }
        }
    }

    zoomSpec?.let { spec ->
        ZoomableFigureDialog(
            onDismiss = { zoomSpec = null },
            title = if (spec.kind == "p") "جدول تناوبی" else "بزرگ‌نمایی شکل",
            rotatable = spec.kind == "p"
        ) {
            if (spec.kind in setOf("a", "s")) {
                AtlasFigureView(
                    spec = spec,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = "شکل بزرگ"
                )
            } else {
                InlineFigureView(
                    spec = spec,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = "شکل بزرگ"
                )
            }
        }
    }
}
