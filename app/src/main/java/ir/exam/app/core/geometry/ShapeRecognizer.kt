package ir.exam.app.core.geometry

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * V264 — تشخیص شکل دست‌آزاد روی تخته (آیتم ۱۲): خط، دایره/بیضی، مثلث، مستطیل.
 * خالص (بدون اندروید) تا تست واحد شود؛ تخته نقاط قلم را می‌دهد و در صورت تشخیص، شیء هندسی جایگزین می‌شود.
 * همان الگوریتم در سایت (site/src/student.js recognizeShape) پیاده شده است.
 */
object ShapeRecognizer {
    data class Pt(val x: Float, val y: Float)
    enum class Kind { LINE, CIRCLE, TRIANGLE, RECT }
    /** جعبهٔ شکل: برای LINE دو سر خط، برای بقیه گوشهٔ بالا-چپ و پایین-راست. */
    data class Shape(val kind: Kind, val a: Pt, val b: Pt)

    fun recognize(points: List<Pt>, minSize: Float = 24f): Shape? {
        if (points.size < 8) return null
        val first = points.first(); val last = points.last()
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var length = 0f
        points.forEachIndexed { i, p ->
            minX = min(minX, p.x); minY = min(minY, p.y); maxX = max(maxX, p.x); maxY = max(maxY, p.y)
            if (i > 0) length += dist(points[i - 1], p)
        }
        val w = maxX - minX; val h = maxY - minY
        val size = max(w, h)
        if (size < minSize || length <= 0f) return null

        // ۱) خط راست: بیشینهٔ فاصلهٔ نقاط از پاره‌خطِ ابتدا→انتها کوچک باشد
        val chord = dist(first, last)
        if (chord > 0.7f * length) {
            val maxDev = points.maxOf { pointToSegment(it, first, last) }
            if (maxDev < 0.08f * length + 2f) return Shape(Kind.LINE, first, last)
        }

        // ۲) شکل بسته: فاصلهٔ سر و ته کمتر از ۲۵٪ محیط
        val closed = chord < 0.25f * length || chord < 0.3f * size
        if (!closed) return null
        val tl = Pt(minX, minY); val br = Pt(maxX, maxY)

        // ۳) دایره/بیضی: فاصلهٔ نقاط از مرکز (نرمال‌شده با نیم‌قطرها) نزدیک ۱ باشد و گوشهٔ تیز نداشته باشد
        val cx = (minX + maxX) / 2f; val cy = (minY + maxY) / 2f
        val rx = max(w / 2f, 1f); val ry = max(h / 2f, 1f)
        val radii = points.map { val dx = (it.x - cx) / rx; val dy = (it.y - cy) / ry; sqrt(dx * dx + dy * dy) }
        val mean = radii.average().toFloat()
        val std = sqrt(radii.map { (it - mean) * (it - mean) }.average().toFloat())
        val corners = simplify(points, 0.07f * size)
        val cornerCount = cornerCountOf(corners)
        if (std < 0.14f && mean in 0.75f..1.15f && cornerCount >= 5) return Shape(Kind.CIRCLE, tl, br)

        // ۴) چندضلعی: تعداد گوشه‌های ساده‌سازی‌شده (Douglas–Peucker)
        return when (cornerCount) {
            3 -> Shape(Kind.TRIANGLE, tl, br)
            4 -> Shape(Kind.RECT, tl, br)
            else -> if (std < 0.2f && mean in 0.7f..1.2f) Shape(Kind.CIRCLE, tl, br) else null
        }
    }

    private fun cornerCountOf(simplified: List<Pt>): Int {
        if (simplified.size < 3) return simplified.size
        // اگر نقطهٔ آخر عملاً همان اول است، یک گوشه حساب می‌شود
        val closeEnds = dist(simplified.first(), simplified.last()) < 0.001f || simplified.size > 2 &&
            dist(simplified.first(), simplified.last()) < 0.35f * dist(simplified[0], simplified[1])
        return if (closeEnds) simplified.size - 1 else simplified.size
    }

    /** Douglas–Peucker */
    fun simplify(points: List<Pt>, epsilon: Float): List<Pt> {
        if (points.size < 3) return points
        var maxD = 0f; var index = 0
        val a = points.first(); val b = points.last()
        for (i in 1 until points.size - 1) {
            val d = pointToSegment(points[i], a, b)
            if (d > maxD) { maxD = d; index = i }
        }
        return if (maxD > epsilon) {
            val left = simplify(points.subList(0, index + 1), epsilon)
            val right = simplify(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else listOf(a, b)
    }

    private fun dist(p: Pt, q: Pt) = hypot((p.x - q.x).toDouble(), (p.y - q.y).toDouble()).toFloat()

    private fun pointToSegment(p: Pt, a: Pt, b: Pt): Float {
        val vx = b.x - a.x; val vy = b.y - a.y
        val len2 = vx * vx + vy * vy
        if (len2 == 0f) return dist(p, a)
        val t = (((p.x - a.x) * vx + (p.y - a.y) * vy) / len2).coerceIn(0f, 1f)
        return dist(p, Pt(a.x + t * vx, a.y + t * vy))
    }

    @Suppress("unused")
    private fun sign(v: Float) = if (abs(v) < 1e-6f) 0f else if (v > 0) 1f else -1f
}
