package ir.exam.app.ui.figure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ir.exam.app.core.figure.AtlasCatalog
import ir.exam.app.core.figure.AtlasMarkPainter
import ir.exam.app.core.figure.FigureSpec

/**
 * V53.3 — نمایش Native شکل‌های آناتومی (`k='a'`) و فیزیک/شیمی (`k='s'`):
 * تصویر اطلس از asset + لایهٔ نشانه‌های شماره‌دار (فلش + دایرهٔ شماره) با
 * Compose Canvas — بدون WebView. عنوان و جای پاسخ‌ها با همان قواعد
 * `X.lab` / `X.blank` / `X.mkName` مرجع نمایش داده می‌شوند.
 */
@Composable
fun AtlasFigureView(
    spec: FigureSpec,
    modifier: Modifier = Modifier,
    contentDescription: String = "شکل",
    showBlanks: Boolean = true
) {
    val context = LocalContext.current
    val assetPath = AtlasCatalog.assetPath(spec) ?: return
    val marks = remember(spec) { spec.marks() }
    val showLabel = spec.xStr("lab", "1") != "0"
    val title = spec.xStr("title").ifBlank { AtlasCatalog.displayName(spec) }
    val markNames = spec.xStr("mkName", "0") == "1"
    val blanks = showBlanks && spec.xStr("blank", "1") != "0" && marks.isNotEmpty()

    Column(modifier) {
        if (showLabel && title.isNotBlank()) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp)
            )
        }
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/$assetPath")
                    .crossfade(false)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            if (marks.isNotEmpty()) {
                Canvas(Modifier.fillMaxSize()) {
                    marks.forEach { mark ->
                        val start = Offset(mark.x1 / 100f * size.width, mark.y1 / 100f * size.height)
                        val end = Offset(mark.x2 / 100f * size.width, mark.y2 / 100f * size.height)
                        // خط فلش
                        drawLine(
                            color = Color(0xFFE4572E),
                            start = start,
                            end = end,
                            strokeWidth = size.minDimension * 0.008f
                        )
                        // سر فلش در انتها
                        val head = AtlasMarkPainter.arrowHead(
                            start.x, start.y, end.x, end.y, size.minDimension * 0.030f
                        )
                        if (head.size == 6) {
                            drawPath(
                                Path().apply {
                                    moveTo(head[0], head[1]); lineTo(head[2], head[3])
                                    lineTo(head[4], head[5]); close()
                                },
                                color = Color(0xFFE4572E)
                            )
                        }
                        // V55.12 — دایرهٔ شماره در «انتهای» پیکان (درخواست کاربر).
                        val radius = size.minDimension * 0.040f
                        drawCircle(Color(0xFFE4572E), radius, end)
                        drawCircle(Color.White, radius * 0.78f, end)
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.parseColor("#C23B17")
                                textSize = radius * 1.15f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText(
                                AtlasMarkPainter.faNum(mark.n),
                                end.x,
                                end.y + radius * 0.40f,
                                paint
                            )
                        }
                    }
                }
            }
        }
        if (blanks) {
            marks.sortedBy { it.n }.forEach { mark ->
                Text(
                    buildString {
                        append(AtlasMarkPainter.faNum(mark.n)).append(") ")
                        append(if (markNames && mark.label.isNotBlank()) mark.label else "…………………")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
        }
    }
}
