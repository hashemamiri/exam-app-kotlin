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

    val SUPPORTED: Set<String> = setOf("axnum", "axxy", "axq1", "axgrid", "axpol", "ax3d")

    private const val STROKE = "#2c3a50"
    private const val GRID = "#d5dce6"
    private const val MUTED = "#4a5870"
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

    private fun polar(spec: FigureSpec): String {
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
        return sb.toString()
    }

    private fun threeD(spec: FigureSpec): String {
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
        val esc = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        return "<text x=\"${f(x)}\" y=\"${f(y)}\" font-family=\"sans-serif\" font-size=\"$size\"$weight fill=\"$color\" text-anchor=\"$anchor\">$esc</text>"
    }
}
