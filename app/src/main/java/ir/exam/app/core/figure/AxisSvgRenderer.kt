package ir.exam.app.core.figure

import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * V136 — «محور»‌های درج‌شدنی در متن سؤال (آیکن کنار «درج نمودار»).
 *
 * انواع (شناسهٔ `t`): axnum محور اعداد، axxy محور مختصات (۴ ربع)، axq1 ربع اول،
 * axgrid شبکهٔ شطرنجی، axpol محور قطبی، ax3d سه‌بعدی (x,y,z).
 * کلیدهای X: xmin/xmax/ymin/ymax، step (گام برچسب)، title.
 * رندر مثل بقیهٔ شکل‌ها Native SVG است و در Builder/دانش‌آموز/چاپ یکسان دیده می‌شود.
 */
internal object AxisSvgRenderer {

    val SUPPORTED: Set<String> = setOf(
        "axnum", "axxy", "axq1", "axgrid", "axpol", "ax3d",
        // V137.2 — ۲۰ نوع تازه (محور اعداد: نقطه‌دار/بازه/نامعادله/کسری/اعشاری/بی‌عدد/لگاریتمی/دوتایی؛
        // مختصات: نقطه‌دار/خط/بردار/دایره/بی‌شبکه/نام ربع‌ها/بی‌عدد/دو محور y؛ نیم‌لگاریتمی/تمام‌لگاریتمی؛
        // زمان؛ قطبی نقطه‌دار؛ سه‌بعدی نقطه‌دار).
        "axnumpts", "axnumint", "axnumineq", "axnumfrac", "axnumdec", "axnumblank", "axnumlog", "axnumtwo",
        "axxypts", "axxyline", "axxyvec", "axxycirc", "axxynogrid", "axxyquads", "axxyblank", "axdual",
        "axlog", "axloglog", "axtime", "axpolpts", "ax3dpt"
    )

    private const val STROKE = "#2c3a50"
    private const val GRID = "#d5dce6"
    private const val MUTED = "#4a5870"
    private const val POINT = "#d6336c"
    private const val L = 40f
    private const val T = 26f
    private const val R = 336f
    private const val B = 246f

    fun body(spec: FigureSpec): String {
        val title = spec.xStr("title")
        val head = if (title.isBlank()) "" else text(180f, 14f, title, "#1a2433", "middle", bold = true, size = 13)
        return head + when (spec.type) {
            "axnum" -> numberLine(spec)
            "axq1" -> cartesian(spec, firstQuadrantOnly = true)
            "axgrid" -> grid(spec)
            "axpol" -> polar(spec)
            "ax3d" -> threeD(spec)
            // V137.2
            "axnumpts", "axnumint", "axnumineq", "axnumfrac", "axnumdec", "axnumblank" -> numberLineEx(spec)
            "axnumlog" -> numberLineLog(spec)
            "axnumtwo" -> numberLineTwo(spec)
            "axxypts", "axxyline", "axxyvec", "axxycirc", "axxynogrid", "axxyquads", "axxyblank" -> cartesianEx(spec)
            "axdual" -> dualAxis(spec)
            "axlog", "axloglog" -> logAxes(spec)
            "axtime" -> timeAxis(spec)
            "axpolpts" -> polar(spec, withPoints = true)
            "ax3dpt" -> threeD(spec, withPoint = true)
            else -> cartesian(spec, firstQuadrantOnly = false)
        }
    }

    private fun stepOf(spec: FigureSpec, span: Float): Float {
        val s = spec.xNum("step", 0f)
        if (s > 0f) return s
        return when {
            span <= 12f -> 1f
            span <= 30f -> 2f
            span <= 60f -> 5f
            else -> 10f
        }
    }

    private fun numberLine(spec: FigureSpec): String {
        val xmin = spec.xNum("xmin", -5f)
        val xmax = spec.xNum("xmax", 5f).let { if (it <= xmin) xmin + 2f else it }
        val step = stepOf(spec, xmax - xmin)
        val y = 140f
        fun px(x: Float) = L + (x - xmin) / (xmax - xmin) * (R - L)
        val sb = StringBuilder()
        sb.append(line(L - 12f, y, R + 12f, y, STROKE, 2f))
        sb.append(arrowRight(R + 12f, y)).append(arrowLeft(L - 12f, y))
        var v = kotlin.math.ceil(xmin / step) * step
        while (v <= xmax + 1e-4f) {
            val x = px(v)
            val zero = kotlin.math.abs(v) < 1e-4f
            sb.append(line(x, y - if (zero) 10f else 7f, x, y + if (zero) 10f else 7f, STROKE, if (zero) 2f else 1.4f))
            sb.append(text(x, y + 24f, num(v), MUTED, "middle", size = 11))
            v += step
        }
        return sb.toString()
    }

    private fun cartesian(spec: FigureSpec, firstQuadrantOnly: Boolean): String {
        val xmin = if (firstQuadrantOnly) 0f else spec.xNum("xmin", -5f)
        val xmax = spec.xNum("xmax", if (firstQuadrantOnly) 10f else 5f).let { if (it <= xmin) xmin + 2f else it }
        val ymin = if (firstQuadrantOnly) 0f else spec.xNum("ymin", -4f)
        val ymax = spec.xNum("ymax", if (firstQuadrantOnly) 8f else 4f).let { if (it <= ymin) ymin + 2f else it }
        val sx = stepOf(spec, xmax - xmin)
        val sy = stepOf(spec, ymax - ymin)
        fun px(x: Float) = L + (x - xmin) / (xmax - xmin) * (R - L)
        fun py(y: Float) = B - (y - ymin) / (ymax - ymin) * (B - T)
        val ox = px(0f).coerceIn(L, R)
        val oy = py(0f).coerceIn(T, B)
        val sb = StringBuilder()
        // شبکه
        var gx = kotlin.math.ceil(xmin / sx) * sx
        while (gx <= xmax + 1e-4f) { sb.append(line(px(gx), T, px(gx), B, GRID, 0.8f)); gx += sx }
        var gy = kotlin.math.ceil(ymin / sy) * sy
        while (gy <= ymax + 1e-4f) { sb.append(line(L, py(gy), R, py(gy), GRID, 0.8f)); gy += sy }
        // محورها با پیکان
        sb.append(line(L - 8f, oy, R + 8f, oy, STROKE, 1.8f)).append(arrowRight(R + 8f, oy))
        sb.append(line(ox, B + 8f, ox, T - 8f, STROKE, 1.8f)).append(arrowUp(ox, T - 8f))
        sb.append(text(R + 4f, oy - 8f, "x", STROKE, "end", bold = true, size = 12))
        sb.append(text(ox + 8f, T - 2f, "y", STROKE, "start", bold = true, size = 12))
        // برچسب‌ها
        gx = kotlin.math.ceil(xmin / sx) * sx
        while (gx <= xmax + 1e-4f) {
            if (kotlin.math.abs(gx) > 1e-4f) {
                sb.append(line(px(gx), oy - 4f, px(gx), oy + 4f, STROKE, 1.2f))
                sb.append(text(px(gx), oy + 15f, num(gx), MUTED, "middle", size = 10))
            }
            gx += sx
        }
        gy = kotlin.math.ceil(ymin / sy) * sy
        while (gy <= ymax + 1e-4f) {
            if (kotlin.math.abs(gy) > 1e-4f) {
                sb.append(line(ox - 4f, py(gy), ox + 4f, py(gy), STROKE, 1.2f))
                sb.append(text(ox - 7f, py(gy) + 4f, num(gy), MUTED, "end", size = 10))
            }
            gy += sy
        }
        sb.append(text(ox - 6f, oy + 14f, "0", MUTED, "end", size = 10))
        return sb.toString()
    }

    private fun grid(spec: FigureSpec): String {
        val cols = spec.xNum("xmax", 10f).toInt().coerceIn(2, 40)
        val rows = spec.xNum("ymax", 8f).toInt().coerceIn(2, 40)
        val cell = minOf((R - L) / cols, (B - T) / rows)
        val w = cell * cols; val h = cell * rows
        val x0 = (360f - w) / 2f; val y0 = T + ((B - T) - h) / 2f
        val sb = StringBuilder()
        sb.append("<rect x=\"${f(x0)}\" y=\"${f(y0)}\" width=\"${f(w)}\" height=\"${f(h)}\" fill=\"#fbfcfe\" stroke=\"$STROKE\" stroke-width=\"1.4\"/>")
        for (i in 1 until cols) sb.append(line(x0 + i * cell, y0, x0 + i * cell, y0 + h, GRID, 0.9f))
        for (j in 1 until rows) sb.append(line(x0, y0 + j * cell, x0 + w, y0 + j * cell, GRID, 0.9f))
        return sb.toString()
    }

    private fun polar(spec: FigureSpec, withPoints: Boolean = false): String {
        val rings = spec.xNum("xmax", 4f).toInt().coerceIn(1, 12)
        val cx = 180f; val cy = (T + B) / 2f
        val rMax = minOf(R - L, B - T) / 2f - 6f
        val sb = StringBuilder()
        for (i in 1..rings) {
            val r = rMax * i / rings
            sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(r)}\" fill=\"none\" stroke=\"$GRID\" stroke-width=\"0.9\"/>")
            sb.append(text(cx + r + 2f, cy - 3f, i.toString(), MUTED, "start", size = 9))
        }
        val labels = listOf("0°", "30°", "60°", "90°", "120°", "150°", "180°", "210°", "240°", "270°", "300°", "330°")
        for (k in 0 until 12) {
            val a = k * PI / 6.0
            val x1 = cx + (rMax * cos(a)).toFloat(); val y1 = cy - (rMax * sin(a)).toFloat()
            sb.append(line(cx, cy, x1, y1, if (k % 3 == 0) STROKE else GRID, if (k % 3 == 0) 1.4f else 0.8f))
            val lx = cx + ((rMax + 13f) * cos(a)).toFloat(); val ly = cy - ((rMax + 13f) * sin(a)).toFloat() + 3f
            sb.append(text(lx, ly, labels[k], MUTED, "middle", size = 9))
        }
        sb.append(arrowRight(cx + rMax, cy))
        if (withPoints) {
            // V137.2 — نقطه‌های (r,θ°) از کلید pts؛ مثل «2,30;3,120».
            val labs = spec.xList("labs")
            pairsOf(spec.xStr("pts", "2,30;3,120")).forEachIndexed { i, (r, deg) ->
                val rr = (r / rings.toFloat() * rMax).coerceIn(0f, rMax + 20f)
                val a = deg * PI / 180.0
                val x = cx + (rr * cos(a)).toFloat(); val y = cy - (rr * sin(a)).toFloat()
                sb.append(line(cx, cy, x, y, POINT, 1.2f))
                sb.append(dot(x, y))
                val lab = labs.getOrNull(i) ?: "(${num(r)}, ${num(deg)}°)"
                sb.append(text(x + 6f, y - 6f, lab, POINT, "start", bold = true, size = 10))
            }
        }
        return sb.toString()
    }

    private fun threeD(spec: FigureSpec, withPoint: Boolean = false): String {
        val cx = 170f; val cy = 150f
        val len = 100f
        val sb = StringBuilder()
        // x به راست، y بالا، z به سمت بیننده (چپ‌پایین با زاویهٔ ۲۲۵°)
        val zx = cx - len * 0.62f; val zy = cy + len * 0.62f
        sb.append(line(cx - 30f, cy, cx + len, cy, STROKE, 1.8f)).append(arrowRight(cx + len, cy))
        sb.append(line(cx, cy + 30f, cx, cy - len, STROKE, 1.8f)).append(arrowUp(cx, cy - len))
        sb.append(line(cx + 20f, cy - 20f, zx, zy, STROKE, 1.8f))
        sb.append("<polygon points=\"${f(zx)},${f(zy)} ${f(zx + 9f)},${f(zy - 1f)} ${f(zx + 2f)},${f(zy - 9f)}\" fill=\"$STROKE\"/>")
        sb.append(text(cx + len + 4f, cy + 4f, "x", STROKE, "start", bold = true, size = 12))
        sb.append(text(cx + 6f, cy - len - 2f, "y", STROKE, "start", bold = true, size = 12))
        sb.append(text(zx - 4f, zy + 12f, "z", STROKE, "end", bold = true, size = 12))
        sb.append(text(cx - 6f, cy + 14f, "O", MUTED, "end", size = 10))
        val step = spec.xNum("step", 1f).coerceAtLeast(0.5f)
        val n = spec.xNum("xmax", 4f).toInt().coerceIn(1, 10)
        for (i in 1..n) {
            val d = len * i / (n + 0.6f)
            sb.append(line(cx + d, cy - 3f, cx + d, cy + 3f, STROKE, 1.1f))
            sb.append(text(cx + d, cy + 13f, num(i * step), MUTED, "middle", size = 9))
            sb.append(line(cx - 3f, cy - d, cx + 3f, cy - d, STROKE, 1.1f))
            sb.append(text(cx - 6f, cy - d + 3f, num(i * step), MUTED, "end", size = 9))
            val tx = cx - d * 0.62f; val ty = cy + d * 0.62f
            sb.append(line(tx - 2.5f, ty - 2.5f, tx + 2.5f, ty + 2.5f, STROKE, 1.1f))
        }
        if (withPoint) {
            // V137.2 — نقطهٔ P(x1,y1,z1) با خط‌چین‌های تصویر روی محورها.
            val unit = len / (n + 0.6f) / step
            val px = spec.xNum("x1", 3f) * unit; val py = spec.xNum("y1", 2f) * unit; val pz = spec.xNum("z1", 2f) * unit
            val bx = cx + px - pz * 0.62f; val by = cy + pz * 0.62f       // (x,0,z) روی کف
            val qx = bx; val qy = by - py                                  // P
            val dash = " stroke-dasharray=\"4 3\""
            sb.append(line(cx + px, cy, bx, by, POINT, 1f).replace("/>", "$dash/>"))
            sb.append(line(cx - pz * 0.62f, cy + pz * 0.62f, bx, by, POINT, 1f).replace("/>", "$dash/>"))
            sb.append(line(bx, by, qx, qy, POINT, 1f).replace("/>", "$dash/>"))
            sb.append(line(cx, cy - py, qx, qy, POINT, 1f).replace("/>", "$dash/>"))
            sb.append(line(cx, cy, qx, qy, POINT, 1.6f))
            sb.append(dot(qx, qy))
            val lab = spec.xStr("labs").ifBlank { "P(${num(spec.xNum("x1", 3f))}, ${num(spec.xNum("y1", 2f))}, ${num(spec.xNum("z1", 2f))})" }
            sb.append(text(qx + 7f, qy - 6f, lab, POINT, "start", bold = true, size = 10))
        }
        return sb.toString()
    }

    // ============================================================ V137.2 — انواع تازه

    /** جفت‌های عددی از متن؛ «1,2;-3,1» یا «(1,2) (-3,1)». */
    private fun pairsOf(s: String): List<Pair<Float, Float>> =
        Regex("(-?\\d+(?:\\.\\d+)?)\\s*[,،]\\s*(-?\\d+(?:\\.\\d+)?)").findAll(s)
            .map { it.groupValues[1].toFloat() to it.groupValues[2].toFloat() }.toList()

    private fun dot(x: Float, y: Float, color: String = POINT, r: Float = 4f) =
        "<circle cx=\"${f(x)}\" cy=\"${f(y)}\" r=\"${f(r)}\" fill=\"$color\" stroke=\"#fff\" stroke-width=\"1.2\"/>"

    private fun openDot(x: Float, y: Float) =
        "<circle cx=\"${f(x)}\" cy=\"${f(y)}\" r=\"4.5\" fill=\"#fff\" stroke=\"$POINT\" stroke-width=\"1.8\"/>"

    private fun gcd(a: Int, b: Int): Int = if (b == 0) kotlin.math.abs(a) else gcd(b, a % b)

    /** محور اعداد پایه؛ px را برمی‌گرداند. labels=false → بی‌عدد. */
    private fun numberBase(
        sb: StringBuilder, xmin: Float, xmax: Float, step: Float, y: Float,
        labels: Boolean = true, den: Int = 0, decimals: Int = -1
    ): (Float) -> Float {
        fun px(x: Float) = L + (x - xmin) / (xmax - xmin) * (R - L)
        sb.append(line(L - 12f, y, R + 12f, y, STROKE, 2f))
        sb.append(arrowRight(R + 12f, y)).append(arrowLeft(L - 12f, y))
        var v = kotlin.math.ceil(xmin / step - 1e-4f) * step
        while (v <= xmax + 1e-4f) {
            val x = px(v)
            val zero = kotlin.math.abs(v) < 1e-4f
            sb.append(line(x, y - if (zero) 10f else 7f, x, y + if (zero) 10f else 7f, STROKE, if (zero) 2f else 1.4f))
            if (labels) {
                val lab = if (decimals >= 0) String.format(Locale.US, "%.${decimals}f", v) else num(v)
                sb.append(text(x, y + 24f, lab, MUTED, "middle", size = 11))
            }
            if (den > 1) {
                for (k in 1 until den) {
                    val fx = v + step * k / den
                    if (fx > xmax + 1e-4f) break
                    val xx = px(fx)
                    sb.append(line(xx, y - 4f, xx, y + 4f, STROKE, 1f))
                    if (labels) {
                        val g = gcd(k, den)
                        val base = kotlin.math.round(v).toInt()
                        val whole = if (base == 0) "" else "$base "
                        sb.append(text(xx, y + 21f, "$whole${k / g}/${den / g}", MUTED, "middle", size = 8))
                    }
                }
            }
            v += step
        }
        return ::px
    }

    private fun numberLineEx(spec: FigureSpec): String {
        val type = spec.type
        val dec = type == "axnumdec"
        val xmin = spec.xNum("xmin", if (dec) 0f else -5f)
        val xmax = spec.xNum("xmax", if (dec) 1f else 5f).let { if (it <= xmin) xmin + (if (dec) 0.5f else 2f) else it }
        val step = if (dec) spec.xNum("step", 0f).takeIf { it > 0f } ?: 0.1f else stepOf(spec, xmax - xmin)
        val y = 140f
        val sb = StringBuilder()
        val px = numberBase(
            sb, xmin, xmax, step, y,
            labels = type != "axnumblank",
            den = if (type == "axnumfrac") spec.xNum("den", 4f).toInt().coerceIn(2, 12) else 0,
            decimals = if (dec) 1 else -1
        )
        when (type) {
            "axnumpts" -> {
                val labs = spec.xList("labs")
                spec.xList("pts").ifEmpty { listOf("-2", "1.5", "4") }.mapNotNull { it.toFloatOrNull() }.forEachIndexed { i, v ->
                    if (v in xmin..xmax) {
                        sb.append(dot(px(v), y))
                        sb.append(text(px(v), y - 14f, labs.getOrNull(i) ?: num(v), POINT, "middle", bold = true, size = 11))
                    }
                }
            }
            "axnumint" -> {
                val lo = spec.xNum("lo", -2f).coerceIn(xmin, xmax); val hi = spec.xNum("hi", 3f).coerceIn(xmin, xmax)
                val lc = spec.xNum("lc", 1f) > 0f; val hc = spec.xNum("hc", 0f) > 0f
                sb.append(line(px(lo), y, px(hi), y, POINT, 4f))
                sb.append(if (lc) dot(px(lo), y) else openDot(px(lo), y))
                sb.append(if (hc) dot(px(hi), y) else openDot(px(hi), y))
                val lab = spec.xStr("labs").ifBlank { (if (lc) "[" else "(") + num(lo) + " , " + num(hi) + (if (hc) "]" else ")") }
                sb.append(text((px(lo) + px(hi)) / 2f, y - 16f, lab, POINT, "middle", bold = true, size = 11))
            }
            "axnumineq" -> {
                val x0 = spec.xNum("x0", 1f).coerceIn(xmin, xmax)
                val right = spec.xNum("dir", 1f) >= 0f
                val closed = spec.xNum("cl", 0f) > 0f
                val end = if (right) R + 12f else L - 12f
                sb.append(line(px(x0), y, end, y, POINT, 4f))
                sb.append(if (closed) dot(px(x0), y) else openDot(px(x0), y))
                val sym = if (right) (if (closed) "x ≥ " else "x > ") else (if (closed) "x ≤ " else "x < ")
                val lab = spec.xStr("labs").ifBlank { sym + num(x0) }
                sb.append(text(px(x0), y - 16f, lab, POINT, "middle", bold = true, size = 11))
            }
        }
        return sb.toString()
    }

    private fun numberLineLog(spec: FigureSpec): String {
        val nDec = spec.xNum("xmax", 4f).toInt().coerceIn(1, 8)
        val y = 140f
        val sb = StringBuilder()
        sb.append(line(L - 12f, y, R + 12f, y, STROKE, 2f)).append(arrowRight(R + 12f, y))
        val decW = (R - L) / nDec
        for (d in 0..nDec) {
            val x = L + d * decW
            sb.append(line(x, y - 9f, x, y + 9f, STROKE, 1.8f))
            sb.append(text(x, y + 26f, "10", MUTED, "middle", size = 11))
            sb.append(text(x + 8f, y + 20f, d.toString(), MUTED, "start", size = 8))
            if (d < nDec) for (k in 2..9) {
                val xx = x + decW * kotlin.math.log10(k.toFloat())
                sb.append(line(xx, y - 4f, xx, y + 4f, STROKE, 0.9f))
            }
        }
        return sb.toString()
    }

    /** دو محور اعداد موازی (تبدیل واحد): پایینی = m × بالایی + b. */
    private fun numberLineTwo(spec: FigureSpec): String {
        val xmin = spec.xNum("xmin", 0f)
        val xmax = spec.xNum("xmax", 10f).let { if (it <= xmin) xmin + 2f else it }
        val step = stepOf(spec, xmax - xmin)
        val m = spec.xNum("m", 2f); val b = spec.xNum("b", 0f)
        val sb = StringBuilder()
        val px = numberBase(sb, xmin, xmax, step, 105f)
        sb.append(text(L - 14f, 92f, spec.xStr("s1", "A"), STROKE, "end", bold = true, size = 11))
        val y2 = 185f
        sb.append(line(L - 12f, y2, R + 12f, y2, STROKE, 2f)).append(arrowRight(R + 12f, y2)).append(arrowLeft(L - 12f, y2))
        var v = kotlin.math.ceil(xmin / step - 1e-4f) * step
        while (v <= xmax + 1e-4f) {
            val x = px(v)
            sb.append(line(x, y2 - 7f, x, y2 + 7f, STROKE, 1.4f))
            sb.append(text(x, y2 + 24f, num(m * v + b), MUTED, "middle", size = 11))
            sb.append(line(x, 105f + 12f, x, y2 - 12f, GRID, 0.8f))
            v += step
        }
        sb.append(text(L - 14f, y2 - 13f, spec.xStr("s2", "B"), STROKE, "end", bold = true, size = 11))
        return sb.toString()
    }

    /** پایهٔ دستگاه مختصات؛ (px, py) برمی‌گرداند. */
    private fun cartesianBase(
        sb: StringBuilder, xmin: Float, xmax: Float, ymin: Float, ymax: Float, sx: Float, sy: Float,
        grid: Boolean = true, labels: Boolean = true, quads: Boolean = false
    ): Pair<(Float) -> Float, (Float) -> Float> {
        fun px(x: Float) = L + (x - xmin) / (xmax - xmin) * (R - L)
        fun py(y: Float) = B - (y - ymin) / (ymax - ymin) * (B - T)
        val ox = px(0f).coerceIn(L, R)
        val oy = py(0f).coerceIn(T, B)
        if (grid) {
            var gx = kotlin.math.ceil(xmin / sx) * sx
            while (gx <= xmax + 1e-4f) { sb.append(line(px(gx), T, px(gx), B, GRID, 0.8f)); gx += sx }
            var gy = kotlin.math.ceil(ymin / sy) * sy
            while (gy <= ymax + 1e-4f) { sb.append(line(L, py(gy), R, py(gy), GRID, 0.8f)); gy += sy }
        }
        sb.append(line(L - 8f, oy, R + 8f, oy, STROKE, 1.8f)).append(arrowRight(R + 8f, oy))
        sb.append(line(ox, B + 8f, ox, T - 8f, STROKE, 1.8f)).append(arrowUp(ox, T - 8f))
        sb.append(text(R + 4f, oy - 8f, "x", STROKE, "end", bold = true, size = 12))
        sb.append(text(ox + 8f, T - 2f, "y", STROKE, "start", bold = true, size = 12))
        var gx = kotlin.math.ceil(xmin / sx) * sx
        while (gx <= xmax + 1e-4f) {
            if (kotlin.math.abs(gx) > 1e-4f) {
                sb.append(line(px(gx), oy - 4f, px(gx), oy + 4f, STROKE, 1.2f))
                if (labels) sb.append(text(px(gx), oy + 15f, num(gx), MUTED, "middle", size = 10))
            }
            gx += sx
        }
        var gy = kotlin.math.ceil(ymin / sy) * sy
        while (gy <= ymax + 1e-4f) {
            if (kotlin.math.abs(gy) > 1e-4f) {
                sb.append(line(ox - 4f, py(gy), ox + 4f, py(gy), STROKE, 1.2f))
                if (labels) sb.append(text(ox - 7f, py(gy) + 4f, num(gy), MUTED, "end", size = 10))
            }
            gy += sy
        }
        if (labels) sb.append(text(ox - 6f, oy + 14f, "0", MUTED, "end", size = 10))
        if (quads) {
            sb.append(text((ox + R) / 2f, (T + oy) / 2f, "I", "#8a94a6", "middle", bold = true, size = 22))
            sb.append(text((L + ox) / 2f, (T + oy) / 2f, "II", "#8a94a6", "middle", bold = true, size = 22))
            sb.append(text((L + ox) / 2f, (oy + B) / 2f, "III", "#8a94a6", "middle", bold = true, size = 22))
            sb.append(text((ox + R) / 2f, (oy + B) / 2f, "IV", "#8a94a6", "middle", bold = true, size = 22))
        }
        return ::px to ::py
    }

    private fun cartesianEx(spec: FigureSpec): String {
        val type = spec.type
        val xmin = spec.xNum("xmin", -5f)
        val xmax = spec.xNum("xmax", 5f).let { if (it <= xmin) xmin + 2f else it }
        val ymin = spec.xNum("ymin", -4f)
        val ymax = spec.xNum("ymax", 4f).let { if (it <= ymin) ymin + 2f else it }
        val sx = stepOf(spec, xmax - xmin); val sy = stepOf(spec, ymax - ymin)
        val sb = StringBuilder()
        val (px, py) = cartesianBase(
            sb, xmin, xmax, ymin, ymax, sx, sy,
            grid = type != "axxynogrid", labels = type != "axxyblank", quads = type == "axxyquads"
        )
        fun clipPoly(fn: (Float) -> Float): String {
            val pts = StringBuilder()
            var x = xmin
            val n = 120
            for (i in 0..n) {
                x = xmin + (xmax - xmin) * i / n
                val yv = fn(x)
                if (yv in ymin..ymax) pts.append(f(px(x))).append(',').append(f(py(yv))).append(' ')
            }
            return "<polyline points=\"${pts.toString().trim()}\" fill=\"none\" stroke=\"$POINT\" stroke-width=\"2\"/>"
        }
        when (type) {
            "axxypts" -> {
                val labs = spec.xList("labs")
                pairsOf(spec.xStr("pts", "1,2;-3,1;2,-2")).forEachIndexed { i, (x, y) ->
                    if (x in xmin..xmax && y in ymin..ymax) {
                        sb.append(dot(px(x), py(y)))
                        val lab = labs.getOrNull(i) ?: "(${num(x)}, ${num(y)})"
                        sb.append(text(px(x) + 6f, py(y) - 6f, lab, POINT, "start", bold = true, size = 10))
                    }
                }
            }
            "axxyline" -> {
                val m = spec.xNum("m", 1f); val b = spec.xNum("b", 1f)
                sb.append(clipPoly { m * it + b })
                val lab = spec.xStr("labs").ifBlank { "y = ${num(m)}x ${if (b < 0f) "− ${num(-b)}" else "+ ${num(b)}"}" }
                sb.append(text(R - 4f, T + 12f, lab, POINT, "end", bold = true, size = 11))
            }
            "axxyvec" -> {
                val x1 = spec.xNum("x1", 0f); val y1 = spec.xNum("y1", 0f)
                val x2 = spec.xNum("x2", 3f); val y2 = spec.xNum("y2", 2f)
                val ax = px(x1); val ay = py(y1); val bx = px(x2); val by = py(y2)
                sb.append(line(ax, ay, bx, by, POINT, 2.2f))
                val ang = kotlin.math.atan2((by - ay).toDouble(), (bx - ax).toDouble())
                val hx1 = bx - (10 * cos(ang - 0.45)).toFloat(); val hy1 = by - (10 * sin(ang - 0.45)).toFloat()
                val hx2 = bx - (10 * cos(ang + 0.45)).toFloat(); val hy2 = by - (10 * sin(ang + 0.45)).toFloat()
                sb.append("<polygon points=\"${f(bx)},${f(by)} ${f(hx1)},${f(hy1)} ${f(hx2)},${f(hy2)}\" fill=\"$POINT\"/>")
                sb.append(dot(ax, ay, POINT, 3f))
                val lab = spec.xStr("labs").ifBlank { "v = (${num(x2 - x1)}, ${num(y2 - y1)})" }
                sb.append(text(bx + 8f, by - 4f, lab, POINT, "start", bold = true, size = 11))
            }
            "axxycirc" -> {
                val cx = spec.xNum("cx", 1f); val cy = spec.xNum("cy", 1f); val r = spec.xNum("r", 2f).coerceAtLeast(0.1f)
                val rx = r / (xmax - xmin) * (R - L); val ry = r / (ymax - ymin) * (B - T)
                sb.append("<ellipse cx=\"${f(px(cx))}\" cy=\"${f(py(cy))}\" rx=\"${f(rx)}\" ry=\"${f(ry)}\" fill=\"rgba(214,51,108,.08)\" stroke=\"$POINT\" stroke-width=\"2\"/>")
                sb.append(dot(px(cx), py(cy), POINT, 3f))
                sb.append(line(px(cx), py(cy), px(cx) + rx, py(cy), POINT, 1.2f).replace("/>", " stroke-dasharray=\"4 3\"/>"))
                sb.append(text(px(cx) + rx / 2f, py(cy) - 6f, "r = ${num(r)}", POINT, "middle", bold = true, size = 10))
            }
        }
        return sb.toString()
    }

    /** دو محور y (چپ: s1، راست: s2) با یک محور x مشترک. */
    private fun dualAxis(spec: FigureSpec): String {
        val xmin = spec.xNum("xmin", 0f)
        val xmax = spec.xNum("xmax", 10f).let { if (it <= xmin) xmin + 2f else it }
        val ymin = spec.xNum("ymin", 0f); val ymax = spec.xNum("ymax", 100f).let { if (it <= ymin) ymin + 2f else it }
        val lo = spec.xNum("lo", 0f); val hi = spec.xNum("hi", 10f).let { if (it <= lo) lo + 2f else it }
        val sx = stepOf(spec, xmax - xmin)
        val ny = 5
        val l = L + 10f; val r = R - 10f
        fun px(x: Float) = l + (x - xmin) / (xmax - xmin) * (r - l)
        val sb = StringBuilder()
        for (i in 0..ny) {
            val y = B - (B - T) * i / ny
            sb.append(line(l, y, r, y, GRID, 0.8f))
            sb.append(text(l - 6f, y + 4f, num(ymin + (ymax - ymin) * i / ny), "#2563eb", "end", size = 10))
            sb.append(text(r + 6f, y + 4f, num(lo + (hi - lo) * i / ny), "#dc2626", "start", size = 10))
        }
        sb.append(line(l, B, r, B, STROKE, 1.8f)).append(arrowRight(r + 8f, B).let { line(r, B, r + 8f, B, STROKE, 1.8f) + it })
        sb.append(line(l, B, l, T - 8f, "#2563eb", 1.8f)).append(arrowUp(l, T - 8f).replace(STROKE, "#2563eb"))
        sb.append(line(r, B, r, T - 8f, "#dc2626", 1.8f)).append(arrowUp(r, T - 8f).replace(STROKE, "#dc2626"))
        var v = kotlin.math.ceil(xmin / sx) * sx
        while (v <= xmax + 1e-4f) {
            sb.append(line(px(v), B - 4f, px(v), B + 4f, STROKE, 1.2f))
            sb.append(text(px(v), B + 15f, num(v), MUTED, "middle", size = 10))
            v += sx
        }
        sb.append(text(l + 6f, T - 2f, spec.xStr("s1", "y₁"), "#2563eb", "start", bold = true, size = 11))
        sb.append(text(r - 6f, T - 2f, spec.xStr("s2", "y₂"), "#dc2626", "end", bold = true, size = 11))
        sb.append(text(r + 8f, B - 8f, "x", STROKE, "end", bold = true, size = 12))
        return sb.toString()
    }

    /** نیم‌لگاریتمی (y لگاریتمی) یا تمام‌لگاریتمی. */
    private fun logAxes(spec: FigureSpec): String {
        val both = spec.type == "axloglog"
        val ny = spec.xNum("ymax", 3f).toInt().coerceIn(1, 6)
        val nx = spec.xNum("xmax", if (both) 3f else 10f).let { if (both) it.toInt().coerceIn(1, 6).toFloat() else it.coerceAtLeast(1f) }
        val sb = StringBuilder()
        sb.append("<rect x=\"${f(L)}\" y=\"${f(T)}\" width=\"${f(R - L)}\" height=\"${f(B - T)}\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"1.4\"/>")
        val decH = (B - T) / ny
        for (d in 0..ny) {
            val y = B - d * decH
            sb.append(line(L, y, R, y, STROKE, 1f))
            sb.append(text(L - 6f, y + 4f, "10", MUTED, "end", size = 10))
            sb.append(text(L - 5f, y - 3f, d.toString(), MUTED, "start", size = 7))
            if (d < ny) for (k in 2..9) { val yy = y - decH * kotlin.math.log10(k.toFloat()); sb.append(line(L, yy, R, yy, GRID, 0.7f)) }
        }
        if (both) {
            val decW = (R - L) / nx
            for (d in 0..nx.toInt()) {
                val x = L + d * decW
                sb.append(line(x, T, x, B, STROKE, 1f))
                sb.append(text(x, B + 16f, "10", MUTED, "middle", size = 10))
                sb.append(text(x + 7f, B + 11f, d.toString(), MUTED, "start", size = 7))
                if (d < nx.toInt()) for (k in 2..9) { val xx = x + decW * kotlin.math.log10(k.toFloat()); sb.append(line(xx, T, xx, B, GRID, 0.7f)) }
            }
        } else {
            val sx = stepOf(spec, nx)
            var v = 0f
            while (v <= nx + 1e-4f) {
                val x = L + v / nx * (R - L)
                sb.append(line(x, T, x, B, GRID, 0.8f))
                sb.append(text(x, B + 16f, num(v), MUTED, "middle", size = 10))
                v += sx
            }
        }
        sb.append(text(R, B + 30f, "x", STROKE, "end", bold = true, size = 12))
        sb.append(text(L - 24f, T - 8f, "y", STROKE, "start", bold = true, size = 12))
        return sb.toString()
    }

    /** محور زمان: برچسب‌های متنی (labs) با فاصلهٔ برابر. */
    private fun timeAxis(spec: FigureSpec): String {
        val labs = spec.xList("labs").ifEmpty { listOf("۸:۰۰", "۹:۰۰", "۱۰:۰۰", "۱۱:۰۰", "۱۲:۰۰") }
        val y = 140f
        val sb = StringBuilder()
        sb.append(line(L - 6f, y, R + 12f, y, STROKE, 2f)).append(arrowRight(R + 12f, y))
        val n = labs.size
        labs.forEachIndexed { i, lab ->
            val x = if (n == 1) L else L + (R - L) * i / (n - 1)
            sb.append(line(x, y - 8f, x, y + 8f, STROKE, 1.5f))
            sb.append(text(x, y + 24f, lab, MUTED, "middle", size = 10))
            if (i < n - 1) for (k in 1..3) { val xx = x + (R - L) / (n - 1) * k / 4f; sb.append(line(xx, y - 3f, xx, y + 3f, STROKE, 0.9f)) }
        }
        sb.append(text(R + 12f, y - 10f, spec.xStr("s1", "t"), STROKE, "end", bold = true, size = 12))
        return sb.toString()
    }

    // ---------------------------------------------------------------- helpers

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: String, w: Float) =
        "<line x1=\"${f(x1)}\" y1=\"${f(y1)}\" x2=\"${f(x2)}\" y2=\"${f(y2)}\" stroke=\"$color\" stroke-width=\"${f(w)}\"/>"

    private fun arrowRight(x: Float, y: Float) = "<polygon points=\"${f(x)},${f(y)} ${f(x - 8f)},${f(y - 4.5f)} ${f(x - 8f)},${f(y + 4.5f)}\" fill=\"$STROKE\"/>"
    private fun arrowLeft(x: Float, y: Float) = "<polygon points=\"${f(x)},${f(y)} ${f(x + 8f)},${f(y - 4.5f)} ${f(x + 8f)},${f(y + 4.5f)}\" fill=\"$STROKE\"/>"
    private fun arrowUp(x: Float, y: Float) = "<polygon points=\"${f(x)},${f(y)} ${f(x - 4.5f)},${f(y + 8f)} ${f(x + 4.5f)},${f(y + 8f)}\" fill=\"$STROKE\"/>"

    private fun num(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

    private fun f(v: Float): String {
        val r = kotlin.math.round(v * 100f) / 100f
        return if (r == r.toInt().toFloat()) r.toInt().toString()
        else String.format(Locale.US, "%.2f", r).trimEnd('0').trimEnd('.')
    }

    private fun text(x: Float, y: Float, s: String, color: String, anchor: String, bold: Boolean = false, size: Int = 11): String {
        if (s.isBlank()) return ""
        val weight = if (bold) " font-weight=\"700\"" else ""
        val esc = FigureDigits.apply(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        return "<text x=\"${f(x)}\" y=\"${f(y)}\" font-family=\"sans-serif\" font-size=\"$size\"$weight fill=\"$color\" text-anchor=\"$anchor\">$esc</text>"
    }
}
