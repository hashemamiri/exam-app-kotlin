package ir.exam.app.ui.math

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * V53.1 — آیکن‌های Native نوار ابزار متن سؤال؛ بازتولید خط‌به‌خط SVGهای مرجع
 * (`viewBox 24×24`) به ImageVector تا هیچ آیکنی از HTML رندر نشود.
 * فرمول عمداً همین‌جاست تا هر ۸ آیکن یک‌دست باشند؛ فقط «عملکرد» فرمول و کادر متن
 * سؤال WebView است.
 */
object QuestionToolIcons {

    private val stroke = SolidColor(Color.Black)

    /** ∑‑مانند مرجع: مسیر «M3.4 11.2 6.1 18.6 10.4 4.6h10.2». */
    val Formula: ImageVector by lazy {
        ImageVector.Builder("QTool.Formula", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = stroke, strokeLineWidth = 1.95f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                moveTo(3.4f, 11.2f); lineTo(6.1f, 18.6f); lineTo(10.4f, 4.6f); horizontalLineToRelative(10.2f)
            }
        }.build()
    }

    /** شکل: مربع + دایره + مثلث مرجع. */
    val Figure: ImageVector by lazy {
        ImageVector.Builder("QTool.Figure", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.8f, fill = null) {
                // rect x=2.6 y=12.2 w=8.2 h=8.2 rx≈1.3 (بدون گوشهٔ گرد در vector ساده)
                moveTo(2.6f, 12.2f); horizontalLineToRelative(8.2f); verticalLineToRelative(8.2f)
                horizontalLineToRelative(-8.2f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.8f, fill = null) {
                // circle cx=16.8 cy=16.3 r=4.15
                moveTo(20.95f, 16.3f)
                arcToRelative(4.15f, 4.15f, 0f, true, true, -8.3f, 0f)
                arcToRelative(4.15f, 4.15f, 0f, true, true, 8.3f, 0f)
            }
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(12f, 2.8f); lineTo(7.35f, 11.1f); horizontalLineToRelative(9.3f); close()
            }
        }.build()
    }

    /** نمودار: محورها + سه ستون مرجع. */
    val Graph: ImageVector by lazy {
        ImageVector.Builder("QTool.Graph", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, fill = null) {
                moveTo(4.2f, 19.4f); verticalLineTo(5.2f)
                moveTo(4.2f, 19.4f); horizontalLineToRelative(15.6f)
            }
            path(fill = stroke) { moveTo(6.4f, 11.2f); horizontalLineToRelative(3f); verticalLineToRelative(6.4f); horizontalLineToRelative(-3f); close() }
            path(fill = stroke) { moveTo(10.8f, 7.4f); horizontalLineToRelative(3f); verticalLineToRelative(10.2f); horizontalLineToRelative(-3f); close() }
            path(fill = stroke) { moveTo(15.2f, 9.4f); horizontalLineToRelative(3f); verticalLineToRelative(8.2f); horizontalLineToRelative(-3f); close() }
        }.build()
    }

    /** جدول: قاب + دو خط افقی + یک خط عمودی مرجع. */
    val Table: ImageVector by lazy {
        ImageVector.Builder("QTool.Table", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.8f, fill = null) {
                moveTo(3.4f, 4.2f); horizontalLineToRelative(17.2f); verticalLineToRelative(15.6f)
                horizontalLineToRelative(-17.2f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.8f, fill = null) {
                moveTo(3.4f, 9.2f); horizontalLineToRelative(17.2f)
                moveTo(3.4f, 14.2f); horizontalLineToRelative(17.2f)
                moveTo(10f, 4.2f); verticalLineToRelative(15.6f)
            }
        }.build()
    }

    /** آناتومی: سر + تنه + دست‌وپا مرجع. */
    val Anatomy: ImageVector by lazy {
        ImageVector.Builder("QTool.Anatomy", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.8f, fill = null) {
                moveTo(14.15f, 4.4f)
                arcToRelative(2.15f, 2.15f, 0f, true, true, -4.3f, 0f)
                arcToRelative(2.15f, 2.15f, 0f, true, true, 4.3f, 0f)
            }
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(8.2f, 8.6f); horizontalLineToRelative(7.6f); lineTo(14.8f, 14.8f); lineTo(9.2f, 14.8f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, fill = null) {
                moveTo(9.3f, 14.8f); lineTo(7.6f, 20.4f)
                moveTo(14.7f, 14.8f); lineTo(16.4f, 20.4f)
            }
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, fill = null) {
                moveTo(8.3f, 9.6f); lineTo(5.4f, 13.2f)
                moveTo(15.7f, 9.6f); lineTo(18.6f, 13.2f)
            }
        }.build()
    }

    /** جدول تناوبی: خانه‌های پراکنده + نوار f پایینی مرجع. */
    val Periodic: ImageVector by lazy {
        ImageVector.Builder("QTool.Periodic", 24.dp, 24.dp, 24f, 24f).apply {
            val cells = listOf(
                2f to 2.2f, 18.9f to 2.2f,
                2f to 6f, 5.4f to 6f, 15.5f to 6f, 18.9f to 6f,
                2f to 9.8f, 5.4f to 9.8f, 8.8f to 9.8f, 12.2f to 9.8f, 15.5f to 9.8f, 18.9f to 9.8f,
                2f to 13.6f, 5.4f to 13.6f, 8.8f to 13.6f, 12.2f to 13.6f, 15.5f to 13.6f, 18.9f to 13.6f
            )
            cells.forEach { (x, y) ->
                path(fill = stroke, pathFillType = PathFillType.NonZero) {
                    moveTo(x, y); horizontalLineToRelative(3.1f); verticalLineToRelative(3.1f)
                    horizontalLineToRelative(-3.1f); close()
                }
            }
            path(fill = stroke) {
                moveTo(8.8f, 18.4f); horizontalLineToRelative(13.2f); verticalLineToRelative(2.4f)
                horizontalLineToRelative(-13.2f); close()
            }
        }.build()
    }

    /** فیزیک: آذرخش مرجع. */
    val Physics: ImageVector by lazy {
        ImageVector.Builder("QTool.Physics", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(13.2f, 2.8f); lineTo(6.4f, 13.4f); horizontalLineToRelative(4.6f)
                lineTo(10.2f, 21.2f); lineTo(18f, 10.4f); horizontalLineToRelative(-4.8f); close()
            }
        }.build()
    }

    /** شیمی: ارلن آزمایشگاهی مرجع. */
    val Chemistry: ImageVector by lazy {
        ImageVector.Builder("QTool.Chemistry", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = stroke, strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null
            ) {
                moveTo(9f, 3.6f); horizontalLineToRelative(6f)
                moveTo(10.2f, 3.6f); verticalLineToRelative(5.4f)
                lineTo(6.4f, 19.2f)
                curveToRelative(-0.6f, 1.2f, 0.3f, 2.4f, 1.6f, 2.4f)
                horizontalLineToRelative(8f)
                curveToRelative(1.3f, 0f, 2.2f, -1.2f, 1.6f, -2.4f)
                lineTo(13.8f, 9f); verticalLineTo(3.6f)
            }
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, fill = null) {
                moveTo(9.4f, 14.6f); horizontalLineToRelative(5.2f)
            }
        }.build()
    }
}
