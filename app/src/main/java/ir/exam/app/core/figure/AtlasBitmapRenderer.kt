package ir.exam.app.core.figure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * V53.3 — رندر Bitmap آناتومی/فیزیک/شیمی (`k∈{a,s}`) برای چاپ و PDF:
 * تصویر اطلس asset + عنوان + فلش‌های شماره‌دار + سطرهای جای پاسخ.
 * همان قواعد `X.lab` / `X.blank` / `X.mkName` مرجع؛ بدون WebView.
 */
object AtlasBitmapRenderer {

    private const val TARGET_WIDTH = 760

    fun render(context: Context, spec: FigureSpec): Bitmap? = runCatching {
        val assetPath = AtlasCatalog.assetPath(spec) ?: return null
        val source = context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) } ?: return null
        val marks = spec.marks()
        val showLabel = spec.xStr("lab", "1") != "0"
        val title = spec.xStr("title").ifBlank { AtlasCatalog.displayName(spec) }
        val markNames = spec.xStr("mkName", "0") == "1"
        val showBlanks = spec.xStr("blank", "1") != "0" && marks.isNotEmpty()

        val scale = TARGET_WIDTH.toFloat() / source.width
        val imgW = TARGET_WIDTH
        val imgH = (source.height * scale).toInt().coerceAtLeast(1)
        val titleH = if (showLabel && title.isNotBlank()) 44 else 0
        val lineH = 34
        val blanksH = if (showBlanks) marks.size * lineH + 10 else 0
        val out = Bitmap.createBitmap(imgW, titleH + imgH + blanksH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(38, 49, 66) }
        if (titleH > 0) {
            ink.textSize = 24f
            ink.textAlign = Paint.Align.CENTER
            ink.isFakeBoldText = true
            canvas.drawText(title, imgW / 2f, 30f, ink)
        }

        val dst = android.graphics.RectF(0f, titleH.toFloat(), imgW.toFloat(), (titleH + imgH).toFloat())
        canvas.drawBitmap(source, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        source.recycle()

        // نشانه‌ها: مختصات درصدی نسبت به قاب تصویر.
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(228, 87, 46)
            strokeWidth = imgW * 0.006f
            style = Paint.Style.STROKE
        }
        val fillAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(228, 87, 46) }
        val fillWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(194, 59, 23)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        marks.forEach { mark ->
            val x1 = mark.x1 / 100f * imgW
            val y1 = titleH + mark.y1 / 100f * imgH
            val x2 = mark.x2 / 100f * imgW
            val y2 = titleH + mark.y2 / 100f * imgH
            canvas.drawLine(x1, y1, x2, y2, accent)
            val head = AtlasMarkPainter.arrowHead(x1, y1, x2, y2, imgW * 0.024f)
            if (head.size == 6) {
                val path = Path().apply {
                    moveTo(head[0], head[1]); lineTo(head[2], head[3])
                    lineTo(head[4], head[5]); close()
                }
                canvas.drawPath(path, fillAccent)
            }
            val radius = imgW * 0.026f
            canvas.drawCircle(x1, y1, radius, fillAccent)
            canvas.drawCircle(x1, y1, radius * 0.78f, fillWhite)
            numberPaint.textSize = radius * 1.15f
            canvas.drawText(AtlasMarkPainter.faNum(mark.n), x1, y1 + radius * 0.40f, numberPaint)
        }

        if (showBlanks) {
            ink.textSize = 20f
            ink.textAlign = Paint.Align.RIGHT
            ink.isFakeBoldText = false
            var y = titleH + imgH + 28f
            marks.sortedBy { it.n }.forEach { mark ->
                val label = if (markNames && mark.label.isNotBlank()) mark.label else "…………………"
                canvas.drawText("${AtlasMarkPainter.faNum(mark.n)}) $label", imgW - 14f, y, ink)
                y += lineH
            }
        }
        out
    }.getOrNull()
}
