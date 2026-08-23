package ir.exam.app.core.figure

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * V53.3 — هندسهٔ خالص نشانه‌های اطلس (فلش و شمارهٔ فارسی) برای Compose Canvas
 * و رندر Bitmap چاپ/PDF؛ جدا از UI تا با تست JVM قابل‌اثبات باشد.
 */
object AtlasMarkPainter {

    private val FA_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun faNum(value: Int): String = value.toString().map { ch ->
        if (ch.isDigit()) FA_DIGITS[ch - '0'] else ch
    }.joinToString("")

    /**
     * سه رأس مثلث سر فلش در انتهای پاره‌خط (x1,y1)→(x2,y2).
     * خروجی [ax, ay, bx, by, cx, cy]؛ اگر طول صفر باشد آرایهٔ خالی برمی‌گردد.
     */
    fun arrowHead(x1: Float, y1: Float, x2: Float, y2: Float, headLength: Float): FloatArray {
        val length = hypot(x2 - x1, y2 - y1)
        if (length < 1e-3f) return FloatArray(0)
        val angle = atan2(y2 - y1, x2 - x1)
        val spread = 0.46f
        val ax = x2
        val ay = y2
        val bx = x2 - headLength * cos(angle - spread)
        val by = y2 - headLength * sin(angle - spread)
        val cx = x2 - headLength * cos(angle + spread)
        val cy = y2 - headLength * sin(angle + spread)
        return floatArrayOf(ax, ay, bx, by, cx, cy)
    }
}
