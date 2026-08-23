package ir.exam.app.core.figure

import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * V54.1 — مرحلهٔ اول تکمیل Native کتابخانهٔ نمودار مرجع (`k='g'`).
 *
 * ۲۰ نوع جدید این مرحله (به‌علاوه ۵ نوع موجود line/quad/sine/exp/bar|col):
 * pie/donut/lchr/area/sarea/hbar/cmp/hcmp/stack/st100/scat/bub/hist/pareto/
 * gauge/radar/combo/step/lolli/funn — همان کلیدهای X مرجع
 * (labs/vals/vals2/vals3/s1..s3/xs/ys/zs/val/vmin/vmax).
 *
 * خروجی فقط بدنهٔ SVG در همان دستگاه مختصات 360×280 مسیر مشترک است؛
 * بدون style/script/URL/foreignObject.
 */
object ChartSvgRenderer {

    private val STAGE1: Set<String> = setOf(
        "pie", "donut", "lchr", "area", "sarea", "hbar", "cmp", "hcmp",
        "stack", "st100", "scat", "bub", "hist", "pareto", "gauge",
        "radar", "combo", "step", "lolli", "funn"
    )

    /** V54.2 — مرحلهٔ دوم به مجموعهٔ پشتیبانی اضافه شد. */
    val SUPPORTED: Set<String> = STAGE1 + ChartSvgRendererStage2.SUPPORTED

    fun supports(type: String): Boolean = type in SUPPORTED

    private const val STROKE = "#2c3a50"
    private const val GRID = "#d5dce6"
    private const val INK = "#1a2433"
    private const val MUTED = "#5b6478"
    private val COLORS = listOf(
        "#6c63f5", "#27c4a8", "#f0a202", "#e4572e",
        "#4c9be8", "#9b5de5", "#00bbf9", "#f15bb5"
    )

    // ناحیهٔ رسم مشترک با renderGraph موجود.
    private const val L = 48f
    private const val T = 28f
    private const val R = 340f
    private const val B = 230f

    fun body(spec: FigureSpec): String {
        val title = spec.xStr("title")
        val head = if (title.isBlank()) "" else
            text(180f, 14f, title, INK, "middle", bold = true, size = 13)
        return head + when (spec.type) {
            "pie" -> pie(spec, donut = false)
            "donut" -> pie(spec, donut = true)
            "lchr" -> lineChart(spec, area = false, step = false)
            "area" -> lineChart(spec, area = true, step = false)
            "step" -> lineChart(spec, area = false, step = true)
            "sarea" -> stackedArea(spec)
            "hbar" -> horizontalBars(spec, series2 = false)
            "hcmp" -> horizontalBars(spec, series2 = true)
            "cmp" -> clusteredColumns(spec)
            "stack" -> stackedColumns(spec, normalized = false)
            "st100" -> stackedColumns(spec, normalized = true)
            "scat" -> scatter(spec, bubble = false)
            "bub" -> scatter(spec, bubble = true)
            "hist" -> histogram(spec)
            "pareto" -> pareto(spec)
            "gauge" -> gauge(spec)
            "radar" -> radar(spec)
            "combo" -> combo(spec)
            "lolli" -> lollipop(spec)
            "funn" -> funnel(spec)
            // V54.2 — انواع مرحلهٔ دوم.
            in ChartSvgRendererStage2.SUPPORTED -> ChartSvgRendererStage2.body(spec)
            else -> ""
        }
    }

    // ------------------------------------------------------------- data

    private fun labels(spec: FigureSpec, default: String = "A,B,C,D"): List<String> =
        spec.xList("labs", default).ifEmpty { listOf("A", "B", "C", "D") }

    private fun nums(spec: FigureSpec, key: String, default: String): List<Float> =
        spec.xList(key, default).map { it.toFloatOrNull() ?: 0f }

    private fun pad(values: List<Float>, size: Int): List<Float> =
        values + List((size - values.size).coerceAtLeast(0)) { 0f }

    // ------------------------------------------------------------- frames

    private fun frame(): String =
        "<rect x=\"${f(L)}\" y=\"${f(T)}\" width=\"${f(R - L)}\" height=\"${f(B - T)}\" fill=\"#fbfcfe\" stroke=\"$GRID\"/>" +
            "<line x1=\"${f(L)}\" y1=\"${f(B)}\" x2=\"${f(R)}\" y2=\"${f(B)}\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>" +
            "<line x1=\"${f(L)}\" y1=\"${f(T)}\" x2=\"${f(L)}\" y2=\"${f(B)}\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>"

    private fun legend(names: List<String>): String {
        if (names.size < 2) return ""
        val sb = StringBuilder()
        var x = R - 8f
        names.forEachIndexed { i, name ->
            if (name.isBlank()) return@forEachIndexed
            sb.append(text(x, 24f, name, MUTED, "end", size = 9))
            x -= name.length * 6f + 14f
            sb.append("<rect x=\"${f(x)}\" y=\"17\" width=\"8\" height=\"8\" rx=\"2\" fill=\"${COLORS[i % COLORS.size]}\"/>")
            x -= 10f
        }
        return sb.toString()
    }

    // ------------------------------------------------------------- charts

    private fun pie(spec: FigureSpec, donut: Boolean): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val total = vals.sum().takeIf { it > 0f } ?: 1f
        val cx = 140f; val cy = 138f; val r = 88f
        val sb = StringBuilder()
        var angle = -90f
        vals.forEachIndexed { i, v ->
            val sweep = v / total * 360f
            if (sweep <= 0f) { return@forEachIndexed }
            val a0 = angle * PI.toFloat() / 180f
            val a1 = (angle + sweep) * PI.toFloat() / 180f
            val large = if (sweep > 180f) 1 else 0
            val x0 = cx + r * cos(a0); val y0 = cy + r * sin(a0)
            val x1 = cx + r * cos(a1); val y1 = cy + r * sin(a1)
            sb.append(
                "<path d=\"M ${f(cx)} ${f(cy)} L ${f(x0)} ${f(y0)} A ${f(r)} ${f(r)} 0 $large 1 ${f(x1)} ${f(y1)} Z\" " +
                    "fill=\"${COLORS[i % COLORS.size]}\" stroke=\"#ffffff\" stroke-width=\"1.5\"/>"
            )
            angle += sweep
        }
        if (donut) sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(r * 0.55f)}\" fill=\"#ffffff\"/>")
        labs.forEachIndexed { i, lab ->
            val y = 60f + i * 22f
            sb.append("<rect x=\"252\" y=\"${f(y - 8f)}\" width=\"10\" height=\"10\" rx=\"2\" fill=\"${COLORS[i % COLORS.size]}\"/>")
            sb.append(text(268f, y, "$lab (${f(vals.getOrElse(i) { 0f })})", MUTED, "", size = 10))
        }
        return sb.toString()
    }

    private fun lineChart(spec: FigureSpec, area: Boolean, step: Boolean): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val maxv = max(vals.maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val dx = (R - L) / max(n - 1, 1)
        fun px(i: Int) = L + i * dx
        fun py(v: Float) = B - (v / maxv) * (B - T - 12f)
        val sb = StringBuilder(frame())
        val pts = StringBuilder()
        vals.forEachIndexed { i, v ->
            if (i == 0) pts.append("M ${f(px(0))} ${f(py(v))}")
            else if (step) pts.append(" L ${f(px(i))} ${f(py(vals[i - 1]))} L ${f(px(i))} ${f(py(v))}")
            else pts.append(" L ${f(px(i))} ${f(py(v))}")
        }
        if (area) {
            sb.append("<path d=\"$pts L ${f(px(n - 1))} ${f(B)} L ${f(L)} ${f(B)} Z\" fill=\"rgba(108,99,245,.22)\"/>")
        }
        sb.append("<path d=\"$pts\" fill=\"none\" stroke=\"${COLORS[0]}\" stroke-width=\"2.4\" stroke-linejoin=\"round\"/>")
        vals.forEachIndexed { i, v ->
            sb.append("<circle cx=\"${f(px(i))}\" cy=\"${f(py(v))}\" r=\"3.4\" fill=\"${COLORS[0]}\"/>")
            sb.append(text(px(i), B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        return sb.toString()
    }

    private fun stackedArea(spec: FigureSpec): String {
        val labs = labels(spec)
        val s = listOf(
            pad(nums(spec, "vals", "4,7,3,6"), labs.size),
            pad(nums(spec, "vals2", "5,4,6,2"), labs.size),
            pad(nums(spec, "vals3", "1,2,2,1"), labs.size)
        )
        val totals = labs.indices.map { i -> s.sumOf { it[i].toDouble() }.toFloat() }
        val maxv = max(totals.maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val dx = (R - L) / max(n - 1, 1)
        fun px(i: Int) = L + i * dx
        fun py(v: Float) = B - (v / maxv) * (B - T - 12f)
        val sb = StringBuilder(frame())
        val cumulative = FloatArray(n)
        s.forEachIndexed { si, series ->
            val top = StringBuilder()
            val bottomPts = (0 until n).map { i -> f(px(i)) to f(py(cumulative[i])) }
            series.forEachIndexed { i, v -> cumulative[i] += v }
            (0 until n).forEach { i ->
                top.append(if (i == 0) "M" else "L").append(" ${f(px(i))} ${f(py(cumulative[i]))} ")
            }
            (n - 1 downTo 0).forEach { i -> top.append("L ${bottomPts[i].first} ${bottomPts[i].second} ") }
            sb.append("<path d=\"${top}Z\" fill=\"${COLORS[si % COLORS.size]}\" fill-opacity=\"0.55\" stroke=\"${COLORS[si % COLORS.size]}\"/>")
        }
        labs.forEachIndexed { i, lab -> sb.append(text(px(i), B + 14f, lab, MUTED, "middle", size = 9)) }
        return sb.toString()
    }

    private fun clusteredColumns(spec: FigureSpec): String {
        val labs = labels(spec)
        val s1 = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val s2 = pad(nums(spec, "vals2", "5,4,6,2"), labs.size)
        val maxv = max((s1 + s2).maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val slot = (R - L) / n
        val bw = min(22f, slot * 0.32f)
        val sb = StringBuilder(frame())
        for (i in 0 until n) {
            val cx = L + slot * i + slot / 2f
            listOf(s1[i] to -1f, s2[i] to 1f).forEachIndexed { si, (v, dir) ->
                val h = (v / maxv) * (B - T - 12f)
                val x = cx + dir * (bw / 2f + 1.5f) - bw / 2f
                sb.append("<rect x=\"${f(x)}\" y=\"${f(B - h)}\" width=\"${f(bw)}\" height=\"${f(h)}\" rx=\"2\" fill=\"${COLORS[si]}\"/>")
            }
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        sb.append(legend(listOf(spec.xStr("s1", "سری ۱"), spec.xStr("s2", "سری ۲"))))
        return sb.toString()
    }

    private fun stackedColumns(spec: FigureSpec, normalized: Boolean): String {
        val labs = labels(spec)
        val s = listOf(
            pad(nums(spec, "vals", "4,7,3,6"), labs.size),
            pad(nums(spec, "vals2", "5,4,6,2"), labs.size),
            pad(nums(spec, "vals3", "1,2,2,1"), labs.size)
        )
        val totals = labs.indices.map { i -> s.sumOf { it[i].toDouble() }.toFloat().takeIf { it > 0f } ?: 1f }
        val maxv = if (normalized) 1f else max(totals.maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val slot = (R - L) / n
        val bw = min(30f, slot * 0.5f)
        val sb = StringBuilder(frame())
        for (i in 0 until n) {
            val cx = L + slot * i + slot / 2f
            var yCursor = B
            s.forEachIndexed { si, series ->
                val share = if (normalized) series[i] / totals[i] else series[i] / maxv
                val h = share * (B - T - 12f)
                yCursor -= h
                sb.append("<rect x=\"${f(cx - bw / 2f)}\" y=\"${f(yCursor)}\" width=\"${f(bw)}\" height=\"${f(h)}\" fill=\"${COLORS[si]}\"/>")
            }
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        sb.append(legend(listOf(spec.xStr("s1", "سری ۱"), spec.xStr("s2", "سری ۲"), spec.xStr("s3", "سری ۳"))))
        return sb.toString()
    }

    private fun horizontalBars(spec: FigureSpec, series2: Boolean): String {
        val labs = labels(spec)
        val s1 = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val s2 = if (series2) pad(nums(spec, "vals2", "5,4,6,2"), labs.size) else emptyList()
        val maxv = max(((s1 + s2).maxOrNull() ?: 1f), 1f)
        val n = labs.size
        val slot = (B - T) / n
        val bh = min(if (series2) 12f else 20f, slot * if (series2) 0.3f else 0.55f)
        val sb = StringBuilder(frame())
        for (i in 0 until n) {
            val cy = T + slot * i + slot / 2f
            val w1 = (s1[i] / maxv) * (R - L - 14f)
            val y1 = if (series2) cy - bh - 1f else cy - bh / 2f
            sb.append("<rect x=\"${f(L)}\" y=\"${f(y1)}\" width=\"${f(w1)}\" height=\"${f(bh)}\" rx=\"2\" fill=\"${COLORS[0]}\"/>")
            if (series2) {
                val w2 = (s2[i] / maxv) * (R - L - 14f)
                sb.append("<rect x=\"${f(L)}\" y=\"${f(cy + 1f)}\" width=\"${f(w2)}\" height=\"${f(bh)}\" rx=\"2\" fill=\"${COLORS[1]}\"/>")
            }
            sb.append(text(L - 5f, cy + 3f, labs[i], MUTED, "end", size = 9))
        }
        if (series2) sb.append(legend(listOf(spec.xStr("s1", "سری ۱"), spec.xStr("s2", "سری ۲"))))
        return sb.toString()
    }

    private fun scatter(spec: FigureSpec, bubble: Boolean): String {
        val xs = nums(spec, "xs", "1,2,3,4,5")
        val ys = pad(nums(spec, "ys", "2,3,1,5,4"), xs.size)
        val zs = if (bubble) pad(nums(spec, "zs", "8,14,6,18,10"), xs.size) else emptyList()
        val xmin = (xs.minOrNull() ?: 0f) - 1f; val xmax = (xs.maxOrNull() ?: 1f) + 1f
        val ymin = (ys.minOrNull() ?: 0f) - 1f; val ymax = (ys.maxOrNull() ?: 1f) + 1f
        val zmax = max(zs.maxOrNull() ?: 1f, 1f)
        fun px(x: Float) = L + (x - xmin) / (xmax - xmin) * (R - L)
        fun py(y: Float) = B - (y - ymin) / (ymax - ymin) * (B - T)
        val sb = StringBuilder(frame())
        xs.forEachIndexed { i, x ->
            val r = if (bubble) 4f + (zs[i] / zmax) * 14f else 4f
            sb.append(
                "<circle cx=\"${f(px(x))}\" cy=\"${f(py(ys[i]))}\" r=\"${f(r)}\" " +
                    "fill=\"${COLORS[i % COLORS.size]}\" fill-opacity=\"${if (bubble) "0.6" else "1"}\" stroke=\"${COLORS[i % COLORS.size]}\"/>"
            )
        }
        return sb.toString()
    }

    private fun histogram(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val maxv = max(vals.maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val bw = (R - L) / n
        val sb = StringBuilder(frame())
        vals.forEachIndexed { i, v ->
            val h = (v / maxv) * (B - T - 12f)
            sb.append(
                "<rect x=\"${f(L + i * bw)}\" y=\"${f(B - h)}\" width=\"${f(bw)}\" height=\"${f(h)}\" " +
                    "fill=\"${COLORS[0]}\" fill-opacity=\"0.75\" stroke=\"#ffffff\" stroke-width=\"1\"/>"
            )
            sb.append(text(L + i * bw + bw / 2f, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        return sb.toString()
    }

    private fun pareto(spec: FigureSpec): String {
        val labs = labels(spec)
        val raw = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val order = raw.indices.sortedByDescending { raw[it] }
        val vals = order.map { raw[it] }
        val names = order.map { labs[it] }
        val total = vals.sum().takeIf { it > 0f } ?: 1f
        val maxv = max(vals.maxOrNull() ?: 1f, 1f)
        val n = vals.size
        val slot = (R - L) / n
        val bw = min(26f, slot * 0.5f)
        val sb = StringBuilder(frame())
        var cumulative = 0f
        val line = StringBuilder()
        vals.forEachIndexed { i, v ->
            val cx = L + slot * i + slot / 2f
            val h = (v / maxv) * (B - T - 12f)
            sb.append("<rect x=\"${f(cx - bw / 2f)}\" y=\"${f(B - h)}\" width=\"${f(bw)}\" height=\"${f(h)}\" fill=\"${COLORS[0]}\"/>")
            sb.append(text(cx, B + 14f, names[i], MUTED, "middle", size = 9))
            cumulative += v
            val cy = B - (cumulative / total) * (B - T - 12f)
            line.append(if (i == 0) "M" else "L").append(" ${f(cx)} ${f(cy)} ")
            sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"3\" fill=\"${COLORS[3]}\"/>")
        }
        sb.append("<path d=\"$line\" fill=\"none\" stroke=\"${COLORS[3]}\" stroke-width=\"2\"/>")
        return sb.toString()
    }

    private fun gauge(spec: FigureSpec): String {
        val vmin = spec.xNum("vmin", 0f)
        val vmax = spec.xNum("vmax", 100f).let { if (it <= vmin) vmin + 1f else it }
        val value = spec.xNum("val", 65f).coerceIn(vmin, vmax)
        val cx = 180f; val cy = 200f; val r = 118f
        val sb = StringBuilder()
        // کمان پس‌زمینه + سه ناحیهٔ رنگی
        listOf(
            Triple(180f, 240f, "#27c4a8"),
            Triple(240f, 300f, "#f0a202"),
            Triple(300f, 360f, "#e4572e")
        ).forEach { (a0, a1, color) ->
            val r0 = a0 * PI.toFloat() / 180f; val r1 = a1 * PI.toFloat() / 180f
            val x0 = cx + r * cos(r0); val y0 = cy + r * sin(r0)
            val x1 = cx + r * cos(r1); val y1 = cy + r * sin(r1)
            sb.append("<path d=\"M ${f(x0)} ${f(y0)} A ${f(r)} ${f(r)} 0 0 1 ${f(x1)} ${f(y1)}\" fill=\"none\" stroke=\"$color\" stroke-width=\"16\"/>")
        }
        val angle = (180f + (value - vmin) / (vmax - vmin) * 180f) * PI.toFloat() / 180f
        val nx = cx + (r - 26f) * cos(angle); val ny = cy + (r - 26f) * sin(angle)
        sb.append("<line x1=\"${f(cx)}\" y1=\"${f(cy)}\" x2=\"${f(nx)}\" y2=\"${f(ny)}\" stroke=\"$STROKE\" stroke-width=\"3.4\" stroke-linecap=\"round\"/>")
        sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"7\" fill=\"$STROKE\"/>")
        sb.append(text(cx, cy + 28f, f(value), INK, "middle", bold = true, size = 16))
        sb.append(text(cx - r, cy + 18f, f(vmin), MUTED, "middle", size = 9))
        sb.append(text(cx + r, cy + 18f, f(vmax), MUTED, "middle", size = 9))
        return sb.toString()
    }

    private fun radar(spec: FigureSpec): String {
        val labs = labels(spec, "A,B,C,D,E")
        val vals = pad(nums(spec, "vals", "4,7,3,6,5"), labs.size)
        val maxv = max(vals.maxOrNull() ?: 1f, 1f)
        val cx = 180f; val cy = 140f; val r = 92f
        val n = labs.size
        val sb = StringBuilder()
        // حلقه‌های شبکه
        listOf(0.33f, 0.66f, 1f).forEach { k ->
            val ring = (0 until n).joinToString(" ") { i ->
                val a = (-90f + 360f * i / n) * PI.toFloat() / 180f
                "${f(cx + r * k * cos(a))},${f(cy + r * k * sin(a))}"
            }
            sb.append("<polygon points=\"$ring\" fill=\"none\" stroke=\"$GRID\"/>")
        }
        val poly = StringBuilder()
        (0 until n).forEach { i ->
            val a = (-90f + 360f * i / n) * PI.toFloat() / 180f
            sb.append("<line x1=\"${f(cx)}\" y1=\"${f(cy)}\" x2=\"${f(cx + r * cos(a))}\" y2=\"${f(cy + r * sin(a))}\" stroke=\"$GRID\"/>")
            sb.append(text(cx + (r + 14f) * cos(a), cy + (r + 14f) * sin(a) + 3f, labs[i], MUTED, "middle", size = 9))
            val k = vals[i] / maxv
            poly.append("${f(cx + r * k * cos(a))},${f(cy + r * k * sin(a))} ")
        }
        sb.append("<polygon points=\"$poly\" fill=\"rgba(108,99,245,.3)\" stroke=\"${COLORS[0]}\" stroke-width=\"2\"/>")
        return sb.toString()
    }

    private fun combo(spec: FigureSpec): String {
        val labs = labels(spec)
        val cols = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val line = pad(nums(spec, "vals2", "5,4,6,2"), labs.size)
        val maxv = max((cols + line).maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val slot = (R - L) / n
        val bw = min(26f, slot * 0.5f)
        val sb = StringBuilder(frame())
        val path = StringBuilder()
        for (i in 0 until n) {
            val cx = L + slot * i + slot / 2f
            val h = (cols[i] / maxv) * (B - T - 12f)
            sb.append("<rect x=\"${f(cx - bw / 2f)}\" y=\"${f(B - h)}\" width=\"${f(bw)}\" height=\"${f(h)}\" rx=\"2\" fill=\"${COLORS[0]}\"/>")
            val ly = B - (line[i] / maxv) * (B - T - 12f)
            path.append(if (i == 0) "M" else "L").append(" ${f(cx)} ${f(ly)} ")
            sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(ly)}\" r=\"3.4\" fill=\"${COLORS[3]}\"/>")
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        sb.append("<path d=\"$path\" fill=\"none\" stroke=\"${COLORS[3]}\" stroke-width=\"2.4\"/>")
        sb.append(legend(listOf(spec.xStr("s1", "ستون"), spec.xStr("s2", "خط"))))
        return sb.toString()
    }

    private fun lollipop(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val maxv = max(vals.maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val slot = (R - L) / n
        val sb = StringBuilder(frame())
        vals.forEachIndexed { i, v ->
            val cx = L + slot * i + slot / 2f
            val cy = B - (v / maxv) * (B - T - 12f)
            sb.append("<line x1=\"${f(cx)}\" y1=\"${f(B)}\" x2=\"${f(cx)}\" y2=\"${f(cy)}\" stroke=\"${COLORS[0]}\" stroke-width=\"2.4\"/>")
            sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"6\" fill=\"${COLORS[0]}\"/>")
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        return sb.toString()
    }

    private fun funnel(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val maxv = max(vals.maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val slotH = (B - T - 8f) / n
        val cx = 180f
        val maxW = 232f
        val sb = StringBuilder()
        vals.forEachIndexed { i, v ->
            val wTop = (if (i == 0) v else vals[i - 1]) / maxv * maxW
            val wBottom = v / maxv * maxW
            val y0 = T + 4f + i * slotH
            val y1 = y0 + slotH - 4f
            sb.append(
                "<path d=\"M ${f(cx - wTop / 2f)} ${f(y0)} L ${f(cx + wTop / 2f)} ${f(y0)} " +
                    "L ${f(cx + wBottom / 2f)} ${f(y1)} L ${f(cx - wBottom / 2f)} ${f(y1)} Z\" " +
                    "fill=\"${COLORS[i % COLORS.size]}\" fill-opacity=\"0.85\"/>"
            )
            sb.append(text(cx, (y0 + y1) / 2f + 3f, "${labs[i]} (${f(v)})", "#ffffff", "middle", bold = true, size = 10))
        }
        return sb.toString()
    }

    // ------------------------------------------------------------- utils

    private fun f(v: Float): String {
        val r = kotlin.math.round(v * 100f) / 100f
        return if (r == r.toInt().toFloat()) r.toInt().toString()
        else String.format(Locale.US, "%.2f", r).trimEnd('0').trimEnd('.')
    }

    private fun text(x: Float, y: Float, s: String, color: String, anchor: String, bold: Boolean = false, size: Int = 11): String {
        if (s.isBlank()) return ""
        val weight = if (bold) " font-weight=\"700\"" else ""
        val anchorAttr = if (anchor.isNotBlank()) " text-anchor=\"$anchor\"" else ""
        return "<text x=\"${f(x)}\" y=\"${f(y)}\" font-family=\"sans-serif\" font-size=\"$size\"$weight fill=\"$color\"$anchorAttr>${escape(s)}</text>"
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}
