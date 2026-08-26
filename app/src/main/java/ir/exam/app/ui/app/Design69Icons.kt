package ir.exam.app.ui.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** آیکن‌های خطی Native برگرفته از pathهای SVG مرجع design-69.html. */
object Design69Icons {
    val Wallet: ImageVector by lazy {
        vector("Design69Wallet") {
            strokePath {
                moveTo(4f, 7f); curveTo(4f, 5.9f, 4.9f, 5f, 6f, 5f)
                lineTo(18f, 5f); curveTo(19.1f, 5f, 20f, 5.9f, 20f, 7f)
                lineTo(20f, 18f); curveTo(20f, 19.1f, 19.1f, 20f, 18f, 20f)
                lineTo(6f, 20f); curveTo(4.9f, 20f, 4f, 19.1f, 4f, 18f); close()
                moveTo(4f, 9f); lineTo(18f, 9f)
                moveTo(15f, 12f); lineTo(21f, 12f); lineTo(21f, 17f); lineTo(15f, 17f)
                curveTo(11.7f, 17f, 11.7f, 12f, 15f, 12f); close()
            }
        }
    }

    val Add: ImageVector by lazy {
        vector("Design69Add") { strokePath(1.8f) { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    }

    val Exams: ImageVector by lazy {
        vector("Design69Exams") {
            strokePath {
                moveTo(7f, 5f); lineTo(17f, 5f); curveTo(18.1f, 5f, 19f, 5.9f, 19f, 7f)
                lineTo(19f, 20f); lineTo(5f, 20f); lineTo(5f, 7f); curveTo(5f, 5.9f, 5.9f, 5f, 7f, 5f); close()
                moveTo(9f, 3f); lineTo(15f, 3f); lineTo(15f, 7f); lineTo(9f, 7f); close()
                moveTo(8.5f, 12f); lineTo(10.5f, 14f); lineTo(14.5f, 10f)
                moveTo(8.5f, 17f); lineTo(15.5f, 17f)
            }
        }
    }

    val Cards: ImageVector by lazy {
        vector("Design69Cards") {
            strokePath {
                moveTo(5.5f, 7f); lineTo(16.5f, 7f); curveTo(17.9f, 7f, 19f, 8.1f, 19f, 9.5f)
                lineTo(19f, 16.5f); curveTo(19f, 17.9f, 17.9f, 19f, 16.5f, 19f)
                lineTo(5.5f, 19f); curveTo(4.1f, 19f, 3f, 17.9f, 3f, 16.5f)
                lineTo(3f, 9.5f); curveTo(3f, 8.1f, 4.1f, 7f, 5.5f, 7f); close()
                moveTo(3f, 11f); lineTo(19f, 11f)
                moveTo(8f, 7f); lineTo(8f, 5f); curveTo(8f, 3.9f, 8.9f, 3f, 10f, 3f)
                lineTo(19f, 3f); curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
                lineTo(21f, 14f); curveTo(21f, 15.1f, 20.1f, 16f, 19f, 16f)
            }
        }
    }

    val Calendar: ImageVector by lazy {
        vector("Design69Calendar") {
            strokePath {
                moveTo(6f, 5f); lineTo(18f, 5f); curveTo(19.7f, 5f, 21f, 6.3f, 21f, 8f)
                lineTo(21f, 18f); curveTo(21f, 19.7f, 19.7f, 21f, 18f, 21f)
                lineTo(6f, 21f); curveTo(4.3f, 21f, 3f, 19.7f, 3f, 18f)
                lineTo(3f, 8f); curveTo(3f, 6.3f, 4.3f, 5f, 6f, 5f); close()
                moveTo(7f, 3f); lineTo(7f, 7f); moveTo(17f, 3f); lineTo(17f, 7f)
                moveTo(3f, 10f); lineTo(21f, 10f)
                moveTo(8f, 14f); lineTo(10f, 14f); moveTo(14f, 14f); lineTo(16f, 14f)
                moveTo(8f, 18f); lineTo(10f, 18f)
            }
        }
    }

    val Classes: ImageVector by lazy {
        vector("Design69Classes") {
            strokePath {
                moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 18f); lineTo(4f, 18f); close()
                moveTo(8f, 21f); lineTo(16f, 21f); moveTo(12f, 18f); lineTo(12f, 21f)
                moveTo(8f, 9f); lineTo(16f, 9f); moveTo(8f, 13f); lineTo(13f, 13f)
            }
        }
    }

    val Students: ImageVector by lazy {
        vector("Design69Students") {
            strokePath {
                circle(9f, 8f, 3.5f)
                moveTo(3f, 19f); curveTo(3.6f, 15f, 5.6f, 13f, 9f, 13f); curveTo(12.4f, 13f, 14.4f, 15f, 15f, 19f)
                moveTo(16f, 6.5f); curveTo(19.5f, 7f, 19.5f, 11.8f, 16f, 12.3f)
                moveTo(16f, 14f); curveTo(18.8f, 14.2f, 20.4f, 15.9f, 21f, 19f)
            }
        }
    }

    val InfoUpdate: ImageVector by lazy {
        vector("Design69InfoUpdate") {
            strokePath {
                circle(12f, 12f, 9f)
                moveTo(12f, 11f); lineTo(12f, 16f); moveTo(12f, 8f); lineTo(12.01f, 8f)
                moveTo(18f, 8f); lineTo(18f, 4f); lineTo(14f, 4f)
            }
        }
    }

    val Header: ImageVector by lazy {
        vector("Design69Header") {
            strokePath {
                moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); lineTo(4f, 20f); close()
                moveTo(4f, 9f); lineTo(20f, 9f)
                moveTo(8f, 7f); lineTo(16f, 7f)
                moveTo(8f, 13f); lineTo(16f, 13f)
                moveTo(8f, 17f); lineTo(13f, 17f)
            }
        }
    }

    val Account: ImageVector by lazy {
        vector("Design69Account") {
            strokePath {
                circle(12f, 8f, 3.5f)
                moveTo(5f, 20f); curveTo(5.8f, 15.3f, 8.1f, 13f, 12f, 13f)
                curveTo(15.9f, 13f, 18.2f, 15.3f, 19f, 20f)
                moveTo(18f, 5f); lineTo(20f, 5f); lineTo(20f, 7f)
            }
        }
    }

    val Data: ImageVector by lazy {
        vector("Design69Data") {
            strokePath {
                moveTo(4f, 6f); curveTo(4f, 3.8f, 20f, 3.8f, 20f, 6f)
                curveTo(20f, 8.2f, 4f, 8.2f, 4f, 6f); close()
                moveTo(4f, 6f); lineTo(4f, 12f); curveTo(4f, 14.2f, 20f, 14.2f, 20f, 12f)
                lineTo(20f, 6f)
                moveTo(4f, 12f); lineTo(4f, 18f); curveTo(4f, 20.2f, 20f, 20.2f, 20f, 18f)
                lineTo(20f, 12f)
            }
        }
    }

    val Settings: ImageVector by lazy {
        vector("Design69Settings") {
            strokePath {
                circle(12f, 12f, 3f)
                moveTo(12f, 2.8f); lineTo(13.5f, 3.4f); lineTo(14.1f, 5.3f); lineTo(15.5f, 5.9f)
                lineTo(17.4f, 5f); lineTo(19.5f, 7.1f); lineTo(18.6f, 9f); lineTo(19.2f, 10.4f)
                lineTo(21.2f, 11.1f); lineTo(21.2f, 14.1f); lineTo(19.2f, 14.8f); lineTo(18.6f, 16.2f)
                lineTo(19.5f, 18.1f); lineTo(17.4f, 20.2f); lineTo(15.5f, 19.3f); lineTo(14.1f, 19.9f)
                lineTo(13.5f, 21.2f); lineTo(10.5f, 21.2f); lineTo(9.9f, 19.9f); lineTo(8.5f, 19.3f)
                lineTo(6.6f, 20.2f); lineTo(4.5f, 18.1f); lineTo(5.4f, 16.2f); lineTo(4.8f, 14.8f)
                lineTo(2.8f, 14.1f); lineTo(2.8f, 11.1f); lineTo(4.8f, 10.4f); lineTo(5.4f, 9f)
                lineTo(4.5f, 7.1f); lineTo(6.6f, 5f); lineTo(8.5f, 5.9f); lineTo(9.9f, 5.3f)
                lineTo(10.5f, 3.4f); close()
            }
        }
    }

    val Logout: ImageVector by lazy {
        vector("Design69Logout") {
            strokePath {
                moveTo(10f, 4f); lineTo(5f, 4f); curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f)
                lineTo(3f, 18f); curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f); lineTo(10f, 20f)
                moveTo(14f, 8f); lineTo(18f, 12f); lineTo(14f, 16f)
                moveTo(8f, 12f); lineTo(18f, 12f)
            }
        }
    }

    val Reports: ImageVector by lazy {
        vector("Design69Reports") {
            strokePath {
                moveTo(4f, 20f); lineTo(20f, 20f)
                moveTo(6f, 17f); lineTo(6f, 12f); lineTo(9f, 12f); lineTo(9f, 17f)
                moveTo(11f, 17f); lineTo(11f, 7f); lineTo(14f, 7f); lineTo(14f, 17f)
                moveTo(16f, 17f); lineTo(16f, 4f); lineTo(19f, 4f); lineTo(19f, 17f)
            }
        }
    }

    val Grading: ImageVector by lazy {
        vector("Design69Grading") {
            strokePath {
                moveTo(6f, 4f); lineTo(18f, 4f); lineTo(18f, 20f); lineTo(6f, 20f); close()
                moveTo(9f, 3f); lineTo(15f, 3f); lineTo(15f, 7f); lineTo(9f, 7f); close()
                moveTo(9f, 13f); lineTo(11f, 15f); lineTo(15.5f, 10.5f)
                moveTo(9f, 18f); lineTo(15f, 18f)
            }
        }
    }

    val Dashboard: ImageVector by lazy {
        vector("Design69Dashboard") {
            strokePath {
                moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
                moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
                moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
                moveTo(14f, 14f); lineTo(20f, 14f); lineTo(20f, 20f); lineTo(14f, 20f); close()
            }
        }
    }

    // V61.9 — آیکن‌های حرفه‌ای پنجرهٔ +: نشانِ افزودن یکدست (دایرهٔ کوچک با +
    // در گوشهٔ پایین-چپ بوم) و فرم‌های تمیزتر برای هر عمل.
    private fun PathBuilder.addBadge(cx: Float = 18.6f, cy: Float = 17.2f) {
        circle(cx, cy, 4.1f)
        moveTo(cx, cy - 2.1f); lineTo(cx, cy + 2.1f)
        moveTo(cx - 2.1f, cy); lineTo(cx + 2.1f, cy)
    }

    val PersonAdd: ImageVector by lazy {
        vector("Design69PersonAdd") {
            strokePath {
                // دانش‌آموز: سر و شانه‌های نرم + نشان افزودن.
                circle(9f, 7.6f, 3.4f)
                moveTo(3.4f, 19.6f)
                curveTo(4.2f, 15.6f, 6.3f, 13.6f, 9f, 13.6f)
                curveTo(11.1f, 13.6f, 12.8f, 14.8f, 13.9f, 17f)
                addBadge()
            }
        }
    }

    val ClassAdd: ImageVector by lazy {
        vector("Design69ClassAdd") {
            strokePath {
                // کلاس: تختهٔ ارائه با آویز و پایه‌ها + نشان افزودن.
                moveTo(10f, 3f); lineTo(10f, 4.6f)
                moveTo(3.6f, 4.6f); lineTo(16.4f, 4.6f)
                lineTo(16.4f, 13.6f); lineTo(3.6f, 13.6f); close()
                moveTo(6.4f, 7.6f); lineTo(13.6f, 7.6f)
                moveTo(6.4f, 10.4f); lineTo(11.2f, 10.4f)
                moveTo(7.6f, 17.2f); lineTo(10f, 13.6f); lineTo(12.4f, 17.2f)
                addBadge()
            }
        }
    }

    val ExamAdd: ImageVector by lazy {
        vector("Design69ExamAdd") {
            strokePath {
                // آزمون: برگهٔ تاخورده با سطرها و تیک + نشان افزودن.
                moveTo(4.6f, 3f); lineTo(12.4f, 3f); lineTo(15.8f, 6.4f)
                lineTo(15.8f, 20.6f); lineTo(4.6f, 20.6f); close()
                moveTo(12.4f, 3f); lineTo(12.4f, 6.4f); lineTo(15.8f, 6.4f)
                moveTo(7f, 10.2f); lineTo(13.4f, 10.2f)
                moveTo(7f, 13.4f); lineTo(12.2f, 13.4f)
                moveTo(7f, 16.8f); lineTo(8.6f, 18.2f); lineTo(11.4f, 15.4f)
                addBadge()
            }
        }
    }

    /** V61.9 — مدرسه جدید: ساختمان با سقف شیب‌دار، پرچم و در + نشان افزودن. */
    val SchoolAdd: ImageVector by lazy {
        vector("Design69SchoolAdd") {
            strokePath {
                moveTo(3.4f, 10.8f); lineTo(10f, 5.2f); lineTo(16.6f, 10.8f)
                moveTo(10f, 5.2f); lineTo(10f, 2.8f); lineTo(13.2f, 2.8f)
                lineTo(13.2f, 4.4f); lineTo(10f, 4.4f)
                moveTo(5f, 10.8f); lineTo(5f, 19.6f); lineTo(15f, 19.6f); lineTo(15f, 10.8f)
                moveTo(8.4f, 19.6f); lineTo(8.4f, 15.4f)
                curveTo(8.4f, 13.6f, 11.6f, 13.6f, 11.6f, 15.4f)
                lineTo(11.6f, 19.6f)
                addBadge(19f, 16.8f)
            }
        }
    }

    /** V61.9 — دعوت معلم: معلم + پاکت دعوت‌نامه. */
    val TeacherInvite: ImageVector by lazy {
        vector("Design69TeacherInvite") {
            strokePath {
                circle(8.6f, 7.8f, 3.3f)
                moveTo(3.2f, 19.8f)
                curveTo(4f, 15.9f, 6f, 14f, 8.6f, 14f)
                curveTo(10.6f, 14f, 12.2f, 15.1f, 13.3f, 17.2f)
                // پاکت دعوت
                moveTo(14.4f, 13.8f); lineTo(21.6f, 13.8f); lineTo(21.6f, 19.4f)
                lineTo(14.4f, 19.4f); close()
                moveTo(14.4f, 14.2f); lineTo(18f, 16.9f); lineTo(21.6f, 14.2f)
            }
        }
    }

    val ChevronLeft: ImageVector by lazy {
        vector("Design69Chevron") { strokePath(2f) { moveTo(15f, 6f); lineTo(9f, 12f); lineTo(15f, 18f) } }
    }

    private fun vector(
        name: String,
        content: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(content).build()

    private fun ImageVector.Builder.strokePath(
        strokeWidth: Float = 1.9f,
        block: PathBuilder.() -> Unit
    ) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, radius: Float) {
        val k = radius * .5522848f
        moveTo(cx + radius, cy)
        curveTo(cx + radius, cy + k, cx + k, cy + radius, cx, cy + radius)
        curveTo(cx - k, cy + radius, cx - radius, cy + k, cx - radius, cy)
        curveTo(cx - radius, cy - k, cx - k, cy - radius, cx, cy - radius)
        curveTo(cx + k, cy - radius, cx + radius, cy - k, cx + radius, cy)
        close()
    }
}

/** Morph واقعی سه خط همبرگر به ×، بدون SVG یا WebView. */
@Composable
fun Design69MorphingMenuIcon(
    open: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(180),
        label = "design69-menu-morph"
    )
    Canvas(modifier.graphicsLayer { clip = false }) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val center = Offset(12f * sx, 12f * sy)
        fun rotate(point: Offset, degrees: Float): Offset {
            val radians = Math.toRadians(degrees.toDouble()).toFloat()
            val dx = point.x - center.x
            val dy = point.y - center.y
            return Offset(
                center.x + dx * cos(radians) - dy * sin(radians),
                center.y + dx * sin(radians) + dy * cos(radians)
            )
        }
        fun line(yStart: Float, targetAngle: Float) {
            val y = (yStart + (12f - yStart) * progress) * sy
            val start = rotate(Offset(4f * sx, y), targetAngle * progress)
            val end = rotate(Offset(20f * sx, y), targetAngle * progress)
            drawLine(tint, start, end, strokeWidth = 2f * sx, cap = StrokeCap.Round)
        }
        line(6f, 45f)
        val middleHalf = 8f * (1f - .80f * progress) * sx
        drawLine(
            tint.copy(alpha = 1f - progress),
            Offset(center.x - middleHalf, center.y),
            Offset(center.x + middleHalf, center.y),
            strokeWidth = 2f * sx,
            cap = StrokeCap.Round
        )
        line(18f, -45f)
    }
}
