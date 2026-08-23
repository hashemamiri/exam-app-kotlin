package ir.exam.app.core.figure

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * V54.2 — مرحلهٔ دوم کتابخانهٔ نمودار Native (`k='g'`): ۱۴ نوع جدید با همان
 * کلیدهای X مرجع:
 * box(labs,mins,q1s,meds,q3s,maxs) / ohlc(labs,opens,highs,lows,closes) /
 * fall(labs,vals منفی مجاز) / ctrl(labs,vals,mean,ucl,lcl خالی=خودکار) /
 * venn(n,s1..s3,ab,ac,bc,abc) / tree(labs,vals) / sun(labs,vals,labs2,vals2) /
 * waff(labs,vals) / pict(labs,vals,unit) / heat|hmap(rows,cols,vals سطری) /
 * bull(labs,vals,vals2) / pyra(labs,vals,vals2,s1,s2) /
 * mekko(labs,vals پهنا+سری۱,vals2,vals3,s1..s3).
 * خروجی فقط بدنهٔ SVG امن در دستگاه مختصات مشترک 360×280 است.
 */
internal object ChartSvgRendererStage2 {

    val SUPPORTED: Set<String> = setOf(
        "box", "ohlc", "fall", "ctrl", "venn", "tree", "sun",
        "waff", "pict", "heat", "hmap", "bull", "pyra", "mekko"
    )

    private const val STROKE = "#2c3a50"
    private const val GRID = "#d5dce6"
    private const val MUTED = "#5b6478"
    private val COLORS = listOf(
        "#6c63f5", "#27c4a8", "#f0a202", "#e4572e",
        "#4c9be8", "#9b5de5", "#00bbf9", "#f15bb5"
    )
    private const val L = 48f
    private const val T = 28f
    private const val R = 340f
    private const val B = 230f

    fun body(spec: FigureSpec): String = when (spec.type) {
        "box" -> boxPlot(spec)
        "ohlc" -> ohlc(spec)
        "fall" -> waterfall(spec)
        "ctrl" -> control(spec)
        "venn" -> venn(spec)
        "tree" -> treemap(spec)
        "sun" -> sunburst(spec)
        "waff" -> waffle(spec)
        "pict" -> pictogram(spec)
        "heat", "hmap" -> heatGrid(spec, contour = spec.type == "heat")
        "bull" -> bullet(spec)
        "pyra" -> pyramid(spec)
        "mekko" -> mekko(spec)
        else -> ""
    }

    // ------------------------------------------------------------- data

    private fun labels(spec: FigureSpec, key: String = "labs", default: String = "A,B,C,D"): List<String> =
        spec.xList(key, default).ifEmpty { listOf("A", "B", "C", "D") }

    private fun nums(spec: FigureSpec, key: String, default: String): List<Float> =
        spec.xList(key, default).map { faFloat(it) }

    /** ارقام فارسی مرجع (مثل ۰٫۴ یا ۸) نیز عدد معتبرند. */
    private fun faFloat(raw: String): Float {
        val normalized = raw.trim()
            .map { ch ->
                when (ch) {
                    in '۰'..'۹' -> ('0' + (ch - '۰'))
                    '٫' -> '.'
                    else -> ch
                }
            }.joinToString("")
        return normalized.toFloatOrNull() ?: 0f
    }

    private fun pad(values: List<Float>, size: Int): List<Float> =
        values + List((size - values.size).coerceAtLeast(0)) { 0f }

    private fun frame(): String =
        "<rect x=\"${f(L)}\" y=\"${f(T)}\" width=\"${f(R - L)}\" height=\"${f(B - T)}\" fill=\"#fbfcfe\" stroke=\"$GRID\"/>" +
            "<line x1=\"${f(L)}\" y1=\"${f(B)}\" x2=\"${f(R)}\" y2=\"${f(B)}\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>" +
            "<line x1=\"${f(L)}\" y1=\"${f(T)}\" x2=\"${f(L)}\" y2=\"${f(B)}\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>"

    // ------------------------------------------------------------- charts

    private fun boxPlot(spec: FigureSpec): String {
        val labs = labels(spec)
        val mins = pad(nums(spec, "mins", "2,3,1,2"), labs.size)
        val q1s = pad(nums(spec, "q1s", "3,4,2,3"), labs.size)
        val meds = pad(nums(spec, "meds", "4,5,3,4"), labs.size)
        val q3s = pad(nums(spec, "q3s", "5,6,4,5"), labs.size)
        val maxs = pad(nums(spec, "maxs", "7,8,5,6"), labs.size)
        val lo = mins.minOrNull() ?: 0f
        val hi = max(maxs.maxOrNull() ?: 1f, lo + 1f)
        fun py(v: Float) = B - (v - lo) / (hi - lo) * (B - T - 16f) - 8f
        val n = labs.size
        val slot = (R - L) / n
        val bw = min(30f, slot * 0.44f)
        val sb = StringBuilder(frame())
        for (i in 0 until n) {
            val cx = L + slot * i + slot / 2f
            val color = COLORS[i % COLORS.size]
            sb.append(line(cx, py(mins[i]), cx, py(q1s[i])))
            sb.append(line(cx, py(q3s[i]), cx, py(maxs[i])))
            sb.append(line(cx - bw / 2f, py(mins[i]), cx + bw / 2f, py(mins[i])))
            sb.append(line(cx - bw / 2f, py(maxs[i]), cx + bw / 2f, py(maxs[i])))
            sb.append(
                "<rect x=\"${f(cx - bw / 2f)}\" y=\"${f(py(q3s[i]))}\" width=\"${f(bw)}\" " +
                    "height=\"${f(py(q1s[i]) - py(q3s[i]))}\" fill=\"$color\" fill-opacity=\"0.4\" stroke=\"$color\" stroke-width=\"1.6\"/>"
            )
            sb.append(line(cx - bw / 2f, py(meds[i]), cx + bw / 2f, py(meds[i]), color, 2.4f))
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        return sb.toString()
    }

    private fun ohlc(spec: FigureSpec): String {
        val labs = labels(spec)
        val opens = pad(nums(spec, "opens", "3,5,4,6"), labs.size)
        val highs = pad(nums(spec, "highs", "6,7,6,8"), labs.size)
        val lows = pad(nums(spec, "lows", "2,4,3,5"), labs.size)
        val closes = pad(nums(spec, "closes", "5,4,6,7"), labs.size)
        val lo = lows.minOrNull() ?: 0f
        val hi = max(highs.maxOrNull() ?: 1f, lo + 1f)
        fun py(v: Float) = B - (v - lo) / (hi - lo) * (B - T - 16f) - 8f
        val n = labs.size
        val slot = (R - L) / n
        val bw = min(18f, slot * 0.36f)
        val sb = StringBuilder(frame())
        for (i in 0 until n) {
            val cx = L + slot * i + slot / 2f
            val up = closes[i] >= opens[i]
            val color = if (up) "#27c4a8" else "#e4572e"
            sb.append(line(cx, py(lows[i]), cx, py(highs[i]), color, 1.8f))
            val top = py(max(opens[i], closes[i]))
            val bottom = py(min(opens[i], closes[i]))
            sb.append(
                "<rect x=\"${f(cx - bw / 2f)}\" y=\"${f(top)}\" width=\"${f(bw)}\" " +
                    "height=\"${f(max(bottom - top, 1.5f))}\" fill=\"$color\"/>"
            )
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        return sb.toString()
    }

    private fun waterfall(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,-2,3,-1"), labs.size)
        var cumulative = 0f
        val levels = vals.map { v -> val start = cumulative; cumulative += v; start to cumulative }
        val lo = min(0f, levels.minOf { min(it.first, it.second) })
        val hi = max(levels.maxOf { max(it.first, it.second) }, lo + 1f)
        fun py(v: Float) = B - (v - lo) / (hi - lo) * (B - T - 16f) - 8f
        val n = labs.size
        val slot = (R - L) / n
        val bw = min(30f, slot * 0.5f)
        val sb = StringBuilder(frame())
        levels.forEachIndexed { i, (start, end) ->
            val cx = L + slot * i + slot / 2f
            val up = end >= start
            val color = if (up) "#27c4a8" else "#e4572e"
            val top = py(max(start, end))
            val h = max(abs(py(start) - py(end)), 1.5f)
            sb.append("<rect x=\"${f(cx - bw / 2f)}\" y=\"${f(top)}\" width=\"${f(bw)}\" height=\"${f(h)}\" fill=\"$color\"/>")
            if (i < n - 1) {
                sb.append(line(cx + bw / 2f, py(end), L + slot * (i + 1) + slot / 2f - bw / 2f, py(end), GRID, 1.2f))
            }
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        sb.append(line(L, py(0f), R, py(0f), STROKE, 1.2f))
        return sb.toString()
    }

    private fun control(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val mean = spec.xStr("mean").takeIf { it.isNotBlank() }?.let { faFloat(it) }
            ?: (vals.sum() / vals.size)
        val sd = sqrt(vals.map { (it - mean) * (it - mean) }.sum() / vals.size)
        val ucl = spec.xStr("ucl").takeIf { it.isNotBlank() }?.let { faFloat(it) } ?: (mean + 2f * sd)
        val lcl = spec.xStr("lcl").takeIf { it.isNotBlank() }?.let { faFloat(it) } ?: (mean - 2f * sd)
        val lo = min(vals.minOrNull() ?: 0f, lcl) - 1f
        val hi = max(max(vals.maxOrNull() ?: 1f, ucl) + 1f, lo + 1f)
        fun py(v: Float) = B - (v - lo) / (hi - lo) * (B - T)
        val n = labs.size
        val dx = (R - L) / max(n - 1, 1)
        val sb = StringBuilder(frame())
        listOf(ucl to "#e4572e", mean to "#27c4a8", lcl to "#e4572e").forEach { (v, color) ->
            sb.append(
                "<line x1=\"${f(L)}\" y1=\"${f(py(v))}\" x2=\"${f(R)}\" y2=\"${f(py(v))}\" " +
                    "stroke=\"$color\" stroke-width=\"1.4\" stroke-dasharray=\"5,4\"/>"
            )
            sb.append(text(R - 3f, py(v) - 4f, f(v), color, "end", size = 8))
        }
        val path = StringBuilder()
        vals.forEachIndexed { i, v ->
            val cx = L + i * dx
            path.append(if (i == 0) "M" else "L").append(" ${f(cx)} ${f(py(v))} ")
            val out = v > ucl || v < lcl
            sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(py(v))}\" r=\"3.6\" fill=\"${if (out) "#e4572e" else COLORS[0]}\"/>")
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        sb.append("<path d=\"$path\" fill=\"none\" stroke=\"${COLORS[0]}\" stroke-width=\"2\"/>")
        return sb.toString()
    }

    private fun venn(spec: FigureSpec): String {
        val n = spec.xStr("n", "3").let { faFloat(it).roundToInt() }.coerceIn(2, 3)
        val s1 = spec.xStr("s1", "A"); val s2 = spec.xStr("s2", "B"); val s3 = spec.xStr("s3", "C")
        val ab = spec.xStr("ab", "۳"); val ac = spec.xStr("ac", "۲")
        val bc = spec.xStr("bc", "۲"); val abc = spec.xStr("abc", "۱")
        val r = 62f
        val sb = StringBuilder()
        fun circle(cx: Float, cy: Float, color: String) =
            "<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(r)}\" fill=\"$color\" fill-opacity=\"0.35\" stroke=\"$color\" stroke-width=\"2\"/>"
        if (n == 2) {
            sb.append(circle(140f, 140f, COLORS[0])).append(circle(220f, 140f, COLORS[1]))
            sb.append(text(104f, 140f, s1, STROKE, "middle", bold = true, size = 12))
            sb.append(text(256f, 140f, s2, STROKE, "middle", bold = true, size = 12))
            sb.append(text(180f, 144f, ab, STROKE, "middle", bold = true, size = 13))
        } else {
            sb.append(circle(148f, 118f, COLORS[0])).append(circle(212f, 118f, COLORS[1]))
            sb.append(circle(180f, 176f, COLORS[2]))
            sb.append(text(112f, 96f, s1, STROKE, "middle", bold = true, size = 11))
            sb.append(text(248f, 96f, s2, STROKE, "middle", bold = true, size = 11))
            sb.append(text(180f, 226f, s3, STROKE, "middle", bold = true, size = 11))
            sb.append(text(180f, 108f, ab, STROKE, "middle", bold = true, size = 12))
            sb.append(text(148f, 164f, ac, STROKE, "middle", bold = true, size = 12))
            sb.append(text(212f, 164f, bc, STROKE, "middle", bold = true, size = 12))
            sb.append(text(180f, 144f, abc, STROKE, "middle", bold = true, size = 12))
        }
        return sb.toString()
    }

    private fun treemap(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val total = vals.sum().takeIf { it > 0f } ?: 1f
        val sb = StringBuilder()
        // چیدمان نواری: ستون‌های عمودی با پهنای سهم و تقسیم عمودی داخل نیمه‌ها.
        var x = L
        val order = vals.indices.sortedByDescending { vals[it] }
        order.forEach { i ->
            val w = (vals[i] / total) * (R - L)
            sb.append(
                "<rect x=\"${f(x)}\" y=\"${f(T)}\" width=\"${f(max(w, 2f))}\" height=\"${f(B - T)}\" " +
                    "fill=\"${COLORS[i % COLORS.size]}\" fill-opacity=\"0.8\" stroke=\"#ffffff\" stroke-width=\"2\"/>"
            )
            if (w > 28f) {
                sb.append(text(x + w / 2f, (T + B) / 2f, labs[i], "#ffffff", "middle", bold = true, size = 10))
                sb.append(text(x + w / 2f, (T + B) / 2f + 14f, f(vals[i]), "#ffffff", "middle", size = 9))
            }
            x += w
        }
        return sb.toString()
    }

    private fun sunburst(spec: FigureSpec): String {
        val cx = 180f; val cy = 140f
        val sb = StringBuilder()
        fun ring(labsKey: String, valsKey: String, defaultLabs: String, defaultVals: String, r0: Float, r1: Float, colorShift: Int): String {
            val labs = labels(spec, labsKey, defaultLabs)
            val vals = pad(nums(spec, valsKey, defaultVals), labs.size)
            val total = vals.sum().takeIf { it > 0f } ?: 1f
            val out = StringBuilder()
            var angle = -90f
            vals.forEachIndexed { i, v ->
                val sweep = v / total * 360f
                if (sweep <= 0f) return@forEachIndexed
                val a0 = angle * PI.toFloat() / 180f
                val a1 = (angle + sweep) * PI.toFloat() / 180f
                val large = if (sweep > 180f) 1 else 0
                out.append(
                    "<path d=\"M ${f(cx + r0 * cos(a0))} ${f(cy + r0 * sin(a0))} " +
                        "L ${f(cx + r1 * cos(a0))} ${f(cy + r1 * sin(a0))} " +
                        "A ${f(r1)} ${f(r1)} 0 $large 1 ${f(cx + r1 * cos(a1))} ${f(cy + r1 * sin(a1))} " +
                        "L ${f(cx + r0 * cos(a1))} ${f(cy + r0 * sin(a1))} " +
                        "A ${f(r0)} ${f(r0)} 0 $large 0 ${f(cx + r0 * cos(a0))} ${f(cy + r0 * sin(a0))} Z\" " +
                        "fill=\"${COLORS[(i + colorShift) % COLORS.size]}\" stroke=\"#ffffff\" stroke-width=\"1.5\"/>"
                )
                val mid = (angle + sweep / 2f) * PI.toFloat() / 180f
                if (sweep > 22f) {
                    val tr = (r0 + r1) / 2f
                    out.append(text(cx + tr * cos(mid), cy + tr * sin(mid) + 3f, labs[i], "#ffffff", "middle", bold = true, size = 9))
                }
                angle += sweep
            }
            return out.toString()
        }
        sb.append(ring("labs", "vals", "A,B,C", "4,7,3", 26f, 62f, 0))
        sb.append(ring("labs2", "vals2", "A1,A2,B1,B2,C1", "2,2,4,3,3", 66f, 100f, 3))
        return sb.toString()
    }

    private fun waffle(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val total = vals.sum().takeIf { it > 0f } ?: 1f
        val counts = vals.map { (it / total * 100f).roundToInt() }
        val sb = StringBuilder()
        val cell = 16f; val gap = 3f
        val x0 = 62f; val y0 = 40f
        var series = 0
        var used = 0
        var remaining = counts.getOrElse(0) { 0 }
        for (idx in 0 until 100) {
            while (remaining <= 0 && series < counts.size - 1) { series++; remaining = counts[series] }
            val row = idx / 10; val col = idx % 10
            val color = if (used < 100) COLORS[series % COLORS.size] else "#e2e8f0"
            sb.append(
                "<rect x=\"${f(x0 + col * (cell + gap))}\" y=\"${f(y0 + row * (cell + gap))}\" " +
                    "width=\"${f(cell)}\" height=\"${f(cell)}\" rx=\"3\" fill=\"$color\"/>"
            )
            remaining--; used++
        }
        labs.forEachIndexed { i, lab ->
            val y = 48f + i * 20f
            sb.append("<rect x=\"258\" y=\"${f(y - 8f)}\" width=\"10\" height=\"10\" rx=\"2\" fill=\"${COLORS[i % COLORS.size]}\"/>")
            sb.append(text(272f, y, lab, MUTED, "", size = 9))
        }
        return sb.toString()
    }

    private fun pictogram(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val unit = max(faFloat(spec.xStr("unit", "1")), 0.001f)
        val sb = StringBuilder()
        val rowH = (B - T) / labs.size
        labs.forEachIndexed { i, lab ->
            val cy = T + rowH * i + rowH / 2f
            sb.append(text(L - 5f, cy + 3f, lab, MUTED, "end", size = 9))
            val count = (vals[i] / unit).roundToInt().coerceIn(0, 18)
            for (k in 0 until count) {
                sb.append("<circle cx=\"${f(L + 14f + k * 15f)}\" cy=\"${f(cy)}\" r=\"5.6\" fill=\"${COLORS[i % COLORS.size]}\"/>")
            }
        }
        sb.append(text(180f, B + 16f, "هر نماد = ${f(unit)}", MUTED, "middle", size = 9))
        return sb.toString()
    }

    private fun heatGrid(spec: FigureSpec, contour: Boolean): String {
        val rows = labels(spec, "rows", "A,B,C")
        val cols = labels(spec, "cols", "۱,۲,۳,۴")
        val vals = nums(spec, "vals", "1,2,3,4,5,6,7,8,9,10,11,12")
        val cellVals = List(rows.size * cols.size) { vals.getOrElse(it) { 0f } }
        val lo = cellVals.minOrNull() ?: 0f
        val hi = max(cellVals.maxOrNull() ?: 1f, lo + 0.001f)
        val cw = (R - L) / cols.size
        val ch = (B - T) / rows.size
        val sb = StringBuilder()
        rows.forEachIndexed { ri, rowLab ->
            sb.append(text(L - 5f, T + ch * ri + ch / 2f + 3f, rowLab, MUTED, "end", size = 9))
            cols.forEachIndexed { ci, _ ->
                val v = cellVals[ri * cols.size + ci]
                val k = (v - lo) / (hi - lo)
                val color = mix(if (contour) "#e7f6f2" else "#eceffe", if (contour) "#0f766e" else "#4338ca", k)
                val rx = if (contour) (min(cw, ch) / 2f * (0.35f + 0.6f * k)) else 4f
                if (contour) {
                    sb.append(
                        "<circle cx=\"${f(L + cw * ci + cw / 2f)}\" cy=\"${f(T + ch * ri + ch / 2f)}\" " +
                            "r=\"${f(rx)}\" fill=\"$color\" fill-opacity=\"0.85\"/>"
                    )
                } else {
                    sb.append(
                        "<rect x=\"${f(L + cw * ci + 1.5f)}\" y=\"${f(T + ch * ri + 1.5f)}\" " +
                            "width=\"${f(cw - 3f)}\" height=\"${f(ch - 3f)}\" rx=\"4\" fill=\"$color\"/>"
                    )
                }
                sb.append(text(L + cw * ci + cw / 2f, T + ch * ri + ch / 2f + 3f, f(v), if (k > 0.55f) "#ffffff" else STROKE, "middle", size = 9))
            }
        }
        cols.forEachIndexed { ci, colLab ->
            sb.append(text(L + cw * ci + cw / 2f, B + 14f, colLab, MUTED, "middle", size = 9))
        }
        return sb.toString()
    }

    private fun bullet(spec: FigureSpec): String {
        val labs = labels(spec)
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val targets = pad(nums(spec, "vals2", "5,4,6,2"), labs.size)
        val maxv = max((vals + targets).maxOrNull() ?: 1f, 1f) * 1.15f
        val n = labs.size
        val slot = (B - T) / n
        val sb = StringBuilder(frame())
        for (i in 0 until n) {
            val cy = T + slot * i + slot / 2f
            sb.append(
                "<rect x=\"${f(L)}\" y=\"${f(cy - 10f)}\" width=\"${f(R - L - 8f)}\" height=\"20\" fill=\"#eef1f6\"/>"
            )
            val w = (vals[i] / maxv) * (R - L - 8f)
            sb.append("<rect x=\"${f(L)}\" y=\"${f(cy - 4.5f)}\" width=\"${f(w)}\" height=\"9\" fill=\"${COLORS[0]}\"/>")
            val tx = L + (targets[i] / maxv) * (R - L - 8f)
            sb.append(line(tx, cy - 11f, tx, cy + 11f, "#e4572e", 2.6f))
            sb.append(text(L - 5f, cy + 3f, labs[i], MUTED, "end", size = 9))
        }
        return sb.toString()
    }

    private fun pyramid(spec: FigureSpec): String {
        val labs = labels(spec, default = "۰-۱۴,۱۵-۲۹,۳۰-۴۴,۴۵-۵۹,۶۰+")
        val left = pad(nums(spec, "vals", "4,7,6,5,3"), labs.size)
        val right = pad(nums(spec, "vals2", "4,6,7,5,4"), labs.size)
        val maxv = max((left + right).maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val mid = 180f
        val half = 118f
        val slot = (B - T) / n
        val bh = min(20f, slot * 0.6f)
        val sb = StringBuilder()
        for (i in 0 until n) {
            val cy = T + slot * i + slot / 2f
            val lw = (left[i] / maxv) * half
            val rw = (right[i] / maxv) * half
            sb.append("<rect x=\"${f(mid - 16f - lw)}\" y=\"${f(cy - bh / 2f)}\" width=\"${f(lw)}\" height=\"${f(bh)}\" fill=\"${COLORS[0]}\"/>")
            sb.append("<rect x=\"${f(mid + 16f)}\" y=\"${f(cy - bh / 2f)}\" width=\"${f(rw)}\" height=\"${f(bh)}\" fill=\"${COLORS[3]}\"/>")
            sb.append(text(mid, cy + 3f, labs[i], MUTED, "middle", size = 8))
        }
        sb.append(text(mid - 70f, T - 6f, spec.xStr("s1", "مرد"), COLORS[0], "middle", bold = true, size = 10))
        sb.append(text(mid + 70f, T - 6f, spec.xStr("s2", "زن"), COLORS[3], "middle", bold = true, size = 10))
        return sb.toString()
    }

    private fun mekko(spec: FigureSpec): String {
        val labs = labels(spec)
        val s1 = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val s2 = pad(nums(spec, "vals2", "5,4,6,2"), labs.size)
        val s3 = pad(nums(spec, "vals3", "1,2,2,1"), labs.size)
        val widths = s1.map { max(it, 0.001f) }
        val widthTotal = widths.sum()
        val sb = StringBuilder(frame())
        var x = L
        labs.forEachIndexed { i, lab ->
            val w = widths[i] / widthTotal * (R - L)
            val columnTotal = max(s1[i] + s2[i] + s3[i], 0.001f)
            var y = B
            listOf(s1[i], s2[i], s3[i]).forEachIndexed { si, v ->
                val h = v / columnTotal * (B - T)
                y -= h
                sb.append(
                    "<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(max(w - 2f, 1f))}\" height=\"${f(h)}\" " +
                        "fill=\"${COLORS[si]}\" fill-opacity=\"0.85\" stroke=\"#ffffff\"/>"
                )
            }
            if (w > 24f) sb.append(text(x + w / 2f, B + 14f, lab, MUTED, "middle", size = 9))
            x += w
        }
        return sb.toString()
    }

    // ------------------------------------------------------------- utils

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: String = STROKE, width: Float = 1.6f): String =
        "<line x1=\"${f(x1)}\" y1=\"${f(y1)}\" x2=\"${f(x2)}\" y2=\"${f(y2)}\" stroke=\"$color\" stroke-width=\"${f(width)}\"/>"

    /** ترکیب خطی دو رنگ #RRGGBB برای مقیاس حرارتی. */
    private fun mix(from: String, to: String, k: Float): String {
        fun ch(s: String, i: Int) = s.substring(i, i + 2).toInt(16)
        val t = k.coerceIn(0f, 1f)
        val r = (ch(from, 1) + (ch(to, 1) - ch(from, 1)) * t).roundToInt()
        val g = (ch(from, 3) + (ch(to, 3) - ch(from, 3)) * t).roundToInt()
        val b = (ch(from, 5) + (ch(to, 5) - ch(from, 5)) * t).roundToInt()
        return String.format(Locale.US, "#%02x%02x%02x", r, g, b)
    }

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
