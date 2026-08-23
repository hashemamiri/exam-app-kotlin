package ir.exam.app.core.figure

import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * V54.3 — مرحلهٔ پایانی کتابخانهٔ نمودار Native (`k='g'`): ۲۲ نوع آخر با همان
 * کلیدهای X مرجع:
 * plot(xmin..ymax) / flow(labs) / gantt(labs,vals شروع,vals2 مدت) /
 * time(labs,vals) / dumb|slope(labs,vals,vals2,s1,s2) / spark|rose(labs,vals) /
 * stream(labs,vals,vals2,vals3,s1..s3) / viol|strip(labs,mins,q1s,meds,q3s,maxs) /
 * stem(vals) / smat(xs,ys,zs,s1..s3) / dend(labs) / sank(vals مثل A-C:8) /
 * chrd(labs,vals ماتریس سطری) / netw(labs,vals یال‌ها A-B) /
 * map|bmap(labs,vals) / surf(nrows,ncols,vals) / calh(vals روزها) /
 * word(labs,vals وزن).
 */
internal object ChartSvgRendererStage3 {

    val SUPPORTED: Set<String> = setOf(
        "plot", "flow", "gantt", "time", "dumb", "slope", "spark", "stream",
        "viol", "strip", "stem", "smat", "dend", "sank", "chrd", "netw",
        "map", "bmap", "surf", "calh", "rose", "word"
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
        "plot" -> coordinatePlane(spec)
        "flow" -> flowchart(spec)
        "gantt" -> gantt(spec)
        "time" -> timeline(spec)
        "dumb" -> dumbbell(spec)
        "slope" -> slope(spec)
        "spark" -> sparkline(spec)
        "stream" -> stream(spec)
        "viol" -> violinOrStrip(spec, strip = false)
        "strip" -> violinOrStrip(spec, strip = true)
        "stem" -> stemLeaf(spec)
        "smat" -> scatterMatrix(spec)
        "dend" -> dendrogram(spec)
        "sank" -> sankey(spec)
        "chrd" -> chord(spec)
        "netw" -> network(spec)
        "map" -> regionMap(spec, bubble = false)
        "bmap" -> regionMap(spec, bubble = true)
        "surf" -> surface(spec)
        "calh" -> calendarHeat(spec)
        "rose" -> rose(spec)
        "word" -> wordCloud(spec)
        else -> ""
    }

    // ------------------------------------------------------------- data

    private fun labels(spec: FigureSpec, key: String = "labs", default: String = "A,B,C,D"): List<String> =
        spec.xList(key, default).ifEmpty { listOf("A", "B", "C", "D") }

    private fun faFloat(raw: String): Float {
        val normalized = raw.trim().map { ch ->
            when (ch) {
                in '۰'..'۹' -> ('0' + (ch - '۰'))
                '٫' -> '.'
                else -> ch
            }
        }.joinToString("")
        return normalized.toFloatOrNull() ?: 0f
    }

    private fun nums(spec: FigureSpec, key: String, default: String): List<Float> =
        spec.xList(key, default).map { faFloat(it) }

    private fun pad(values: List<Float>, size: Int): List<Float> =
        values + List((size - values.size).coerceAtLeast(0)) { 0f }

    private fun frame(): String =
        "<rect x=\"${f(L)}\" y=\"${f(T)}\" width=\"${f(R - L)}\" height=\"${f(B - T)}\" fill=\"#fbfcfe\" stroke=\"$GRID\"/>" +
            "<line x1=\"${f(L)}\" y1=\"${f(B)}\" x2=\"${f(R)}\" y2=\"${f(B)}\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>" +
            "<line x1=\"${f(L)}\" y1=\"${f(T)}\" x2=\"${f(L)}\" y2=\"${f(B)}\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>"

    // ------------------------------------------------------------- charts

    private fun coordinatePlane(spec: FigureSpec): String {
        val xmin = spec.xNum("xmin", -5f)
        val xmax = spec.xNum("xmax", 5f).let { if (it <= xmin) xmin + 2f else it }
        val ymin = spec.xNum("ymin", -4f)
        val ymax = spec.xNum("ymax", 4f).let { if (it <= ymin) ymin + 2f else it }
        fun px(x: Float) = L + (x - xmin) / (xmax - xmin) * (R - L)
        fun py(y: Float) = B - (y - ymin) / (ymax - ymin) * (B - T)
        val sb = StringBuilder(frame())
        var gx = kotlin.math.ceil(xmin).toInt()
        while (gx <= xmax) {
            sb.append("<line x1=\"${f(px(gx.toFloat()))}\" y1=\"${f(T)}\" x2=\"${f(px(gx.toFloat()))}\" y2=\"${f(B)}\" stroke=\"$GRID\" stroke-width=\"0.8\"/>")
            if (gx != 0) sb.append(text(px(gx.toFloat()), py(0f).coerceIn(T + 10f, B - 3f) + 11f, gx.toString(), MUTED, "middle", size = 8))
            gx++
        }
        var gy = kotlin.math.ceil(ymin).toInt()
        while (gy <= ymax) {
            sb.append("<line x1=\"${f(L)}\" y1=\"${f(py(gy.toFloat()))}\" x2=\"${f(R)}\" y2=\"${f(py(gy.toFloat()))}\" stroke=\"$GRID\" stroke-width=\"0.8\"/>")
            if (gy != 0) sb.append(text(px(0f).coerceIn(L + 6f, R - 6f) - 5f, py(gy.toFloat()) + 3f, gy.toString(), MUTED, "end", size = 8))
            gy++
        }
        if (ymin < 0f && ymax > 0f) sb.append("<line x1=\"${f(L)}\" y1=\"${f(py(0f))}\" x2=\"${f(R)}\" y2=\"${f(py(0f))}\" stroke=\"$STROKE\" stroke-width=\"1.8\"/>")
        if (xmin < 0f && xmax > 0f) sb.append("<line x1=\"${f(px(0f))}\" y1=\"${f(T)}\" x2=\"${f(px(0f))}\" y2=\"${f(B)}\" stroke=\"$STROKE\" stroke-width=\"1.8\"/>")
        return sb.toString()
    }

    private fun flowchart(spec: FigureSpec): String {
        val steps = labels(spec, default = "شروع,پردازش,تصمیم,پایان")
        val n = steps.size
        val sb = StringBuilder()
        val slot = (B - T - 8f) / n
        val boxH = min(34f, slot * 0.7f)
        val cx = 180f
        steps.forEachIndexed { i, label ->
            val y0 = T + 4f + i * slot + (slot - boxH) / 2f
            val first = i == 0
            val last = i == n - 1
            val decision = !first && !last && label.contains("؟")
            when {
                first || last ->
                    sb.append("<rect x=\"${f(cx - 62f)}\" y=\"${f(y0)}\" width=\"124\" height=\"${f(boxH)}\" rx=\"${f(boxH / 2f)}\" fill=\"${if (first) "#e7f6f2" else "#fdeaea"}\" stroke=\"${if (first) "#27c4a8" else "#e4572e"}\" stroke-width=\"1.6\"/>")
                decision -> {
                    val cy = y0 + boxH / 2f
                    sb.append("<path d=\"M ${f(cx)} ${f(y0 - 3f)} L ${f(cx + 74f)} ${f(cy)} L ${f(cx)} ${f(y0 + boxH + 3f)} L ${f(cx - 74f)} ${f(cy)} Z\" fill=\"#fdf3e3\" stroke=\"#f0a202\" stroke-width=\"1.6\"/>")
                }
                else ->
                    sb.append("<rect x=\"${f(cx - 66f)}\" y=\"${f(y0)}\" width=\"132\" height=\"${f(boxH)}\" rx=\"6\" fill=\"#eceffe\" stroke=\"${COLORS[0]}\" stroke-width=\"1.6\"/>")
            }
            sb.append(text(cx, y0 + boxH / 2f + 4f, label, STROKE, "middle", bold = true, size = 10))
            if (!last) {
                val ay0 = y0 + boxH
                val ay1 = T + 4f + (i + 1) * slot + (slot - boxH) / 2f
                sb.append("<line x1=\"${f(cx)}\" y1=\"${f(ay0)}\" x2=\"${f(cx)}\" y2=\"${f(ay1 - 5f)}\" stroke=\"$STROKE\" stroke-width=\"1.6\"/>")
                sb.append("<path d=\"M ${f(cx - 4.5f)} ${f(ay1 - 6f)} L ${f(cx + 4.5f)} ${f(ay1 - 6f)} L ${f(cx)} ${f(ay1)} Z\" fill=\"$STROKE\"/>")
            }
        }
        return sb.toString()
    }

    private fun gantt(spec: FigureSpec): String {
        val labs = labels(spec, default = "طراحی,ساخت,آزمون,تحویل")
        val starts = pad(nums(spec, "vals", "0,2,5,7"), labs.size)
        val durations = pad(nums(spec, "vals2", "3,4,3,1"), labs.size)
        val end = max(labs.indices.maxOf { starts[it] + durations[it] }, 1f)
        val n = labs.size
        val slot = (B - T) / n
        val bh = min(20f, slot * 0.55f)
        val sb = StringBuilder(frame())
        for (g in 0..end.toInt()) {
            val x = L + g / end * (R - L)
            sb.append("<line x1=\"${f(x)}\" y1=\"${f(T)}\" x2=\"${f(x)}\" y2=\"${f(B)}\" stroke=\"$GRID\" stroke-width=\"0.8\"/>")
            sb.append(text(x, B + 12f, g.toString(), MUTED, "middle", size = 8))
        }
        labs.forEachIndexed { i, lab ->
            val cy = T + slot * i + slot / 2f
            val x0 = L + starts[i] / end * (R - L)
            val w = durations[i] / end * (R - L)
            sb.append("<rect x=\"${f(x0)}\" y=\"${f(cy - bh / 2f)}\" width=\"${f(max(w, 2f))}\" height=\"${f(bh)}\" rx=\"4\" fill=\"${COLORS[i % COLORS.size]}\"/>")
            sb.append(text(L - 5f, cy + 3f, lab, MUTED, "end", size = 9))
        }
        return sb.toString()
    }

    private fun timeline(spec: FigureSpec): String {
        val labs = labels(spec, default = "رویداد ۱,رویداد ۲,رویداد ۳,رویداد ۴")
        val vals = spec.xList("vals", "۱۴۰۰,۱۴۰۱,۱۴۰۲,۱۴۰۳")
        val n = labs.size
        val cy = 130f
        val sb = StringBuilder()
        sb.append("<line x1=\"${f(L)}\" y1=\"${f(cy)}\" x2=\"${f(R)}\" y2=\"${f(cy)}\" stroke=\"$STROKE\" stroke-width=\"2.4\"/>")
        val dx = (R - L) / max(n - 1, 1)
        labs.forEachIndexed { i, lab ->
            val cx = L + i * dx
            val up = i % 2 == 0
            sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"6\" fill=\"${COLORS[i % COLORS.size]}\"/>")
            val ty = if (up) cy - 34f else cy + 44f
            sb.append("<line x1=\"${f(cx)}\" y1=\"${f(cy + if (up) -6f else 6f)}\" x2=\"${f(cx)}\" y2=\"${f(ty + if (up) 12f else -14f)}\" stroke=\"$GRID\"/>")
            sb.append(text(cx, ty, lab, STROKE, "middle", bold = true, size = 9))
            sb.append(text(cx, ty + 12f, vals.getOrElse(i) { "" }, MUTED, "middle", size = 8))
        }
        return sb.toString()
    }

    private fun dumbbell(spec: FigureSpec): String {
        val labs = labels(spec)
        val before = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val after = pad(nums(spec, "vals2", "5,4,6,2"), labs.size)
        val maxv = max((before + after).maxOrNull() ?: 1f, 1f) * 1.1f
        val n = labs.size
        val slot = (B - T) / n
        val sb = StringBuilder(frame())
        labs.forEachIndexed { i, lab ->
            val cy = T + slot * i + slot / 2f
            val x0 = L + before[i] / maxv * (R - L - 10f)
            val x1 = L + after[i] / maxv * (R - L - 10f)
            sb.append("<line x1=\"${f(x0)}\" y1=\"${f(cy)}\" x2=\"${f(x1)}\" y2=\"${f(cy)}\" stroke=\"$GRID\" stroke-width=\"2.6\"/>")
            sb.append("<circle cx=\"${f(x0)}\" cy=\"${f(cy)}\" r=\"6\" fill=\"${COLORS[0]}\"/>")
            sb.append("<circle cx=\"${f(x1)}\" cy=\"${f(cy)}\" r=\"6\" fill=\"${COLORS[3]}\"/>")
            sb.append(text(L - 5f, cy + 3f, lab, MUTED, "end", size = 9))
        }
        sb.append(text(R - 8f, T - 6f, "${spec.xStr("s1", "قبل")} ● / ${spec.xStr("s2", "بعد")} ●", MUTED, "end", size = 9))
        return sb.toString()
    }

    private fun slope(spec: FigureSpec): String {
        val labs = labels(spec)
        val before = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val after = pad(nums(spec, "vals2", "5,4,6,2"), labs.size)
        val lo = min(before.minOrNull() ?: 0f, after.minOrNull() ?: 0f)
        val hi = max(max(before.maxOrNull() ?: 1f, after.maxOrNull() ?: 1f), lo + 1f)
        fun py(v: Float) = B - (v - lo) / (hi - lo) * (B - T - 20f) - 10f
        val x0 = L + 44f; val x1 = R - 44f
        val sb = StringBuilder()
        sb.append("<line x1=\"${f(x0)}\" y1=\"${f(T)}\" x2=\"${f(x0)}\" y2=\"${f(B)}\" stroke=\"$GRID\"/>")
        sb.append("<line x1=\"${f(x1)}\" y1=\"${f(T)}\" x2=\"${f(x1)}\" y2=\"${f(B)}\" stroke=\"$GRID\"/>")
        sb.append(text(x0, B + 14f, spec.xStr("s1", "قبل"), MUTED, "middle", size = 10))
        sb.append(text(x1, B + 14f, spec.xStr("s2", "بعد"), MUTED, "middle", size = 10))
        labs.forEachIndexed { i, lab ->
            val color = COLORS[i % COLORS.size]
            sb.append("<line x1=\"${f(x0)}\" y1=\"${f(py(before[i]))}\" x2=\"${f(x1)}\" y2=\"${f(py(after[i]))}\" stroke=\"$color\" stroke-width=\"2.2\"/>")
            sb.append("<circle cx=\"${f(x0)}\" cy=\"${f(py(before[i]))}\" r=\"4\" fill=\"$color\"/>")
            sb.append("<circle cx=\"${f(x1)}\" cy=\"${f(py(after[i]))}\" r=\"4\" fill=\"$color\"/>")
            sb.append(text(x0 - 8f, py(before[i]) + 3f, lab, color, "end", size = 9))
        }
        return sb.toString()
    }

    private fun sparkline(spec: FigureSpec): String {
        val vals = nums(spec, "vals", "4,7,3,6,5,8,4")
        val lo = vals.minOrNull() ?: 0f
        val hi = max(vals.maxOrNull() ?: 1f, lo + 1f)
        val n = vals.size
        val dx = (R - L) / max(n - 1, 1)
        fun py(v: Float) = 170f - (v - lo) / (hi - lo) * 90f
        val path = StringBuilder()
        vals.forEachIndexed { i, v -> path.append(if (i == 0) "M" else "L").append(" ${f(L + i * dx)} ${f(py(v))} ") }
        val sb = StringBuilder()
        sb.append("<path d=\"$path\" fill=\"none\" stroke=\"${COLORS[0]}\" stroke-width=\"2.4\" stroke-linejoin=\"round\"/>")
        val lastX = L + (n - 1) * dx
        sb.append("<circle cx=\"${f(lastX)}\" cy=\"${f(py(vals.last()))}\" r=\"4.5\" fill=\"${COLORS[3]}\"/>")
        sb.append(text(lastX, py(vals.last()) - 9f, f(vals.last()), COLORS[3], "middle", bold = true, size = 10))
        return sb.toString()
    }

    private fun stream(spec: FigureSpec): String {
        val labs = labels(spec)
        val series = listOf(
            pad(nums(spec, "vals", "4,7,3,6"), labs.size),
            pad(nums(spec, "vals2", "5,4,6,2"), labs.size),
            pad(nums(spec, "vals3", "1,2,2,1"), labs.size)
        )
        val totals = labs.indices.map { i -> series.sumOf { it[i].toDouble() }.toFloat() }
        val maxT = max(totals.maxOrNull() ?: 1f, 1f)
        val n = labs.size
        val dx = (R - L) / max(n - 1, 1)
        val mid = (T + B) / 2f
        val scale = (B - T - 16f) / maxT
        val sb = StringBuilder()
        // جریان متقارن حول محور میانی
        val lower = FloatArray(n) { i -> -totals[i] / 2f }
        series.forEachIndexed { si, s ->
            val top = StringBuilder()
            val base = lower.copyOf()
            for (i in 0 until n) lower[i] += s[i]
            for (i in 0 until n) top.append(if (i == 0) "M" else "L").append(" ${f(L + i * dx)} ${f(mid - lower[i] * scale)} ")
            for (i in n - 1 downTo 0) top.append("L ${f(L + i * dx)} ${f(mid - base[i] * scale)} ")
            sb.append("<path d=\"${top}Z\" fill=\"${COLORS[si]}\" fill-opacity=\"0.62\" stroke=\"${COLORS[si]}\"/>")
        }
        labs.forEachIndexed { i, lab -> sb.append(text(L + i * dx, B + 14f, lab, MUTED, "middle", size = 9)) }
        return sb.toString()
    }

    private fun violinOrStrip(spec: FigureSpec, strip: Boolean): String {
        val labs = labels(spec)
        val meds = pad(nums(spec, "meds", "4,5,3,4"), labs.size)
        val mins = pad(nums(spec, "mins", "2,3,1,2"), labs.size)
        val q1s = pad(nums(spec, "q1s", "3,4,2,3"), labs.size)
        val q3s = pad(nums(spec, "q3s", "5,6,4,5"), labs.size)
        val maxs = pad(nums(spec, "maxs", "7,8,5,6"), labs.size)
        val lo = mins.minOrNull() ?: 0f
        val hi = max(maxs.maxOrNull() ?: 1f, lo + 1f)
        fun py(v: Float) = B - (v - lo) / (hi - lo) * (B - T - 16f) - 8f
        val n = labs.size
        val slot = (R - L) / n
        val sb = StringBuilder(frame())
        for (i in 0 until n) {
            val cx = L + slot * i + slot / 2f
            val color = COLORS[i % COLORS.size]
            if (strip) {
                // نوار نقطه‌ای: نقاط پخش‌شده بین min و max با تراکم نزدیک میانه.
                listOf(mins[i], q1s[i], (q1s[i] + meds[i]) / 2f, meds[i], (meds[i] + q3s[i]) / 2f, q3s[i], maxs[i])
                    .forEachIndexed { k, v ->
                        val jitter = ((k * 37 + i * 17) % 12 - 6).toFloat()
                        sb.append("<circle cx=\"${f(cx + jitter)}\" cy=\"${f(py(v))}\" r=\"3.4\" fill=\"$color\" fill-opacity=\"0.8\"/>")
                    }
            } else {
                // ویولن: پهنای متغیر — بیشینه در میانه، باریک در دو انتها.
                val w = min(24f, slot * 0.34f)
                val path = "M ${f(cx)} ${f(py(mins[i]))} " +
                    "C ${f(cx + w * 0.35f)} ${f(py(q1s[i]))} ${f(cx + w)} ${f(py(meds[i]) + 8f)} ${f(cx + w)} ${f(py(meds[i]))} " +
                    "C ${f(cx + w)} ${f(py(meds[i]) - 8f)} ${f(cx + w * 0.35f)} ${f(py(q3s[i]))} ${f(cx)} ${f(py(maxs[i]))} " +
                    "C ${f(cx - w * 0.35f)} ${f(py(q3s[i]))} ${f(cx - w)} ${f(py(meds[i]) - 8f)} ${f(cx - w)} ${f(py(meds[i]))} " +
                    "C ${f(cx - w)} ${f(py(meds[i]) + 8f)} ${f(cx - w * 0.35f)} ${f(py(q1s[i]))} ${f(cx)} ${f(py(mins[i]))} Z"
                sb.append("<path d=\"$path\" fill=\"$color\" fill-opacity=\"0.4\" stroke=\"$color\" stroke-width=\"1.6\"/>")
                sb.append("<line x1=\"${f(cx - 7f)}\" y1=\"${f(py(meds[i]))}\" x2=\"${f(cx + 7f)}\" y2=\"${f(py(meds[i]))}\" stroke=\"$color\" stroke-width=\"2.4\"/>")
            }
            sb.append(text(cx, B + 14f, labs[i], MUTED, "middle", size = 9))
        }
        return sb.toString()
    }

    private fun stemLeaf(spec: FigureSpec): String {
        val vals = nums(spec, "vals", "12,15,21,23,24,31,35,42").map { it.roundToInt() }.sorted()
        val groups = vals.groupBy { it / 10 }.toSortedMap()
        val sb = StringBuilder()
        sb.append("<line x1=\"150\" y1=\"${f(T + 6f)}\" x2=\"150\" y2=\"${f(T + 14f + groups.size * 24f)}\" stroke=\"$STROKE\" stroke-width=\"1.8\"/>")
        sb.append(text(138f, T + 2f, "ساقه", MUTED, "end", size = 9))
        sb.append(text(162f, T + 2f, "برگ", MUTED, "", size = 9))
        var y = T + 26f
        groups.forEach { (stemDigit, members) ->
            sb.append(text(138f, y, stemDigit.toString(), STROKE, "end", bold = true, size = 12))
            sb.append(text(162f, y, members.joinToString(" ") { (it % 10).toString() }, STROKE, "", size = 12))
            y += 24f
        }
        return sb.toString()
    }

    private fun scatterMatrix(spec: FigureSpec): String {
        val series = listOf(
            nums(spec, "xs", "1,2,3,4,5"),
            nums(spec, "ys", "2,3,1,5,4"),
            nums(spec, "zs", "8,14,6,18,10")
        )
        val names = listOf(spec.xStr("s1", "X"), spec.xStr("s2", "Y"), spec.xStr("s3", "Z"))
        val cell = 62f
        val gap = 8f
        val x0 = 76f; val y0 = 40f
        val sb = StringBuilder()
        for (r in 0 until 3) for (c in 0 until 3) {
            val cx0 = x0 + c * (cell + gap)
            val cy0 = y0 + r * (cell + gap)
            sb.append("<rect x=\"${f(cx0)}\" y=\"${f(cy0)}\" width=\"${f(cell)}\" height=\"${f(cell)}\" fill=\"#fbfcfe\" stroke=\"$GRID\"/>")
            if (r == c) {
                sb.append(text(cx0 + cell / 2f, cy0 + cell / 2f + 4f, names[r], STROKE, "middle", bold = true, size = 11))
            } else {
                val a = series[c]; val b = series[r]
                val aLo = a.minOrNull() ?: 0f; val aHi = max(a.maxOrNull() ?: 1f, aLo + 1f)
                val bLo = b.minOrNull() ?: 0f; val bHi = max(b.maxOrNull() ?: 1f, bLo + 1f)
                val m = min(a.size, b.size)
                for (i in 0 until m) {
                    val px = cx0 + 5f + (a[i] - aLo) / (aHi - aLo) * (cell - 10f)
                    val py = cy0 + cell - 5f - (b[i] - bLo) / (bHi - bLo) * (cell - 10f)
                    sb.append("<circle cx=\"${f(px)}\" cy=\"${f(py)}\" r=\"2.6\" fill=\"${COLORS[i % COLORS.size]}\"/>")
                }
            }
        }
        return sb.toString()
    }

    private fun dendrogram(spec: FigureSpec): String {
        val leaves = labels(spec, default = "A,B,C,D,E")
        val n = leaves.size
        val slot = (R - L) / n
        val xs = FloatArray(n) { L + slot * it + slot / 2f }
        val sb = StringBuilder()
        leaves.forEachIndexed { i, leaf -> sb.append(text(xs[i], B + 12f, leaf, MUTED, "middle", size = 9)) }
        // ادغام سلسله‌مراتبی جفت‌به‌جفت چپ‌به‌راست
        var currentX = xs.toMutableList()
        var currentY = MutableList(n) { B - 4f }
        var level = 1
        while (currentX.size > 1) {
            val h = B - 4f - level * ((B - T - 20f) / n)
            val mergedX = mutableListOf<Float>()
            val mergedY = mutableListOf<Float>()
            var i = 0
            while (i < currentX.size) {
                if (i + 1 < currentX.size) {
                    val xA = currentX[i]; val xB = currentX[i + 1]
                    sb.append("<path d=\"M ${f(xA)} ${f(currentY[i])} L ${f(xA)} ${f(h)} L ${f(xB)} ${f(h)} L ${f(xB)} ${f(currentY[i + 1])}\" fill=\"none\" stroke=\"${COLORS[level % COLORS.size]}\" stroke-width=\"1.8\"/>")
                    mergedX.add((xA + xB) / 2f); mergedY.add(h)
                    i += 2
                } else {
                    mergedX.add(currentX[i]); mergedY.add(currentY[i]); i++
                }
            }
            currentX = mergedX; currentY = mergedY; level++
        }
        return sb.toString()
    }

    private fun sankey(spec: FigureSpec): String {
        // قالب مرجع: "A-C:8,B-D:5"
        val flows = spec.xList("vals", "A-C:8,B-C:5,B-D:4").mapNotNull { raw ->
            val m = Regex("(.+)-(.+):(.+)").find(raw.trim()) ?: return@mapNotNull null
            Triple(m.groupValues[1].trim(), m.groupValues[2].trim(), faFloat(m.groupValues[3]))
        }
        if (flows.isEmpty()) return frame()
        val sources = flows.map { it.first }.distinct()
        val targets = flows.map { it.second }.distinct()
        val total = flows.sumOf { it.third.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
        val sb = StringBuilder()
        val srcH = mutableMapOf<String, Float>(); val tgtH = mutableMapOf<String, Float>()
        flows.forEach { (s, t, v) ->
            srcH[s] = (srcH[s] ?: 0f) + v; tgtH[t] = (tgtH[t] ?: 0f) + v
        }
        val usable = B - T - (sources.size + targets.size) * 4f
        var y = T
        val srcY = mutableMapOf<String, Float>()
        sources.forEach { s ->
            val h = srcH[s]!! / total * usable
            sb.append("<rect x=\"${f(L)}\" y=\"${f(y)}\" width=\"16\" height=\"${f(h)}\" rx=\"3\" fill=\"${COLORS[sources.indexOf(s) % COLORS.size]}\"/>")
            sb.append(text(L - 4f, y + h / 2f + 3f, s, MUTED, "end", size = 9))
            srcY[s] = y; y += h + 8f
        }
        y = T
        val tgtY = mutableMapOf<String, Float>()
        targets.forEach { t ->
            val h = tgtH[t]!! / total * usable
            sb.append("<rect x=\"${f(R - 16f)}\" y=\"${f(y)}\" width=\"16\" height=\"${f(h)}\" rx=\"3\" fill=\"${COLORS[(targets.indexOf(t) + 4) % COLORS.size]}\"/>")
            sb.append(text(R + 4f - 16f + 20f, y + h / 2f + 3f, t, MUTED, "", size = 9))
            tgtY[t] = y; y += h + 8f
        }
        val srcUsed = mutableMapOf<String, Float>(); val tgtUsed = mutableMapOf<String, Float>()
        flows.forEachIndexed { fi, (s, t, v) ->
            val h = v / total * usable
            val y0 = srcY[s]!! + (srcUsed[s] ?: 0f)
            val y1 = tgtY[t]!! + (tgtUsed[t] ?: 0f)
            srcUsed[s] = (srcUsed[s] ?: 0f) + h; tgtUsed[t] = (tgtUsed[t] ?: 0f) + h
            val x0 = L + 16f; val x1 = R - 16f
            val cxm = (x0 + x1) / 2f
            sb.append(
                "<path d=\"M ${f(x0)} ${f(y0)} C ${f(cxm)} ${f(y0)} ${f(cxm)} ${f(y1)} ${f(x1)} ${f(y1)} " +
                    "L ${f(x1)} ${f(y1 + h)} C ${f(cxm)} ${f(y1 + h)} ${f(cxm)} ${f(y0 + h)} ${f(x0)} ${f(y0 + h)} Z\" " +
                    "fill=\"${COLORS[fi % COLORS.size]}\" fill-opacity=\"0.4\"/>"
            )
        }
        return sb.toString()
    }

    private fun chord(spec: FigureSpec): String {
        val nodes = labels(spec, default = "A,B,C,D")
        val matrix = nums(spec, "vals", "0,2,1,1,2,0,2,1,1,2,0,1,1,1,1,0")
        val n = nodes.size
        val cx = 180f; val cy = 138f; val r = 92f
        val sb = StringBuilder()
        val angle = { i: Int -> (-90f + 360f * i / n) * PI.toFloat() / 180f }
        for (i in 0 until n) {
            val a = angle(i)
            sb.append("<circle cx=\"${f(cx + r * cos(a))}\" cy=\"${f(cy + r * sin(a))}\" r=\"11\" fill=\"${COLORS[i % COLORS.size]}\"/>")
            sb.append(text(cx + (r + 20f) * cos(a), cy + (r + 20f) * sin(a) + 3f, nodes[i], STROKE, "middle", bold = true, size = 10))
        }
        for (i in 0 until n) for (j in i + 1 until n) {
            val w = matrix.getOrElse(i * n + j) { 0f }
            if (w <= 0f) continue
            val a0 = angle(i); val a1 = angle(j)
            sb.append(
                "<path d=\"M ${f(cx + r * cos(a0))} ${f(cy + r * sin(a0))} Q ${f(cx)} ${f(cy)} ${f(cx + r * cos(a1))} ${f(cy + r * sin(a1))}\" " +
                    "fill=\"none\" stroke=\"${COLORS[i % COLORS.size]}\" stroke-opacity=\"0.55\" stroke-width=\"${f(min(w * 1.8f, 8f))}\"/>"
            )
        }
        return sb.toString()
    }

    private fun network(spec: FigureSpec): String {
        val nodes = labels(spec, default = "A,B,C,D,E")
        val edges = spec.xList("vals", "A-B,B-C,C-D,B-E").mapNotNull { raw ->
            val parts = raw.split("-").map { it.trim() }
            if (parts.size == 2) parts[0] to parts[1] else null
        }
        val n = nodes.size
        val cx = 180f; val cy = 134f; val r = 84f
        val pos = nodes.mapIndexed { i, _ ->
            val a = (-90f + 360f * i / n) * PI.toFloat() / 180f
            (cx + r * cos(a)) to (cy + r * sin(a))
        }
        val sb = StringBuilder()
        edges.forEach { (a, b) ->
            val i = nodes.indexOf(a); val j = nodes.indexOf(b)
            if (i < 0 || j < 0) return@forEach
            sb.append("<line x1=\"${f(pos[i].first)}\" y1=\"${f(pos[i].second)}\" x2=\"${f(pos[j].first)}\" y2=\"${f(pos[j].second)}\" stroke=\"$GRID\" stroke-width=\"2\"/>")
        }
        nodes.forEachIndexed { i, node ->
            sb.append("<circle cx=\"${f(pos[i].first)}\" cy=\"${f(pos[i].second)}\" r=\"14\" fill=\"${COLORS[i % COLORS.size]}\"/>")
            sb.append(text(pos[i].first, pos[i].second + 4f, node, "#ffffff", "middle", bold = true, size = 10))
        }
        return sb.toString()
    }

    private fun regionMap(spec: FigureSpec, bubble: Boolean): String {
        val labs = labels(spec, default = "شمال,مرکز,جنوب,شرق")
        val vals = pad(nums(spec, "vals", "4,7,3,6"), labs.size)
        val maxv = max(vals.maxOrNull() ?: 1f, 1f)
        // چیدمان شبکه‌ای مناطق (نقشهٔ نمادین مرجع نیز شبکهٔ ساده است).
        val cols = kotlin.math.ceil(sqrtF(labs.size.toFloat())).toInt().coerceAtLeast(1)
        val rows = kotlin.math.ceil(labs.size / cols.toFloat()).toInt()
        val cw = (R - L) / cols
        val ch = (B - T) / rows
        val sb = StringBuilder()
        labs.forEachIndexed { i, lab ->
            val cx0 = L + (i % cols) * cw
            val cy0 = T + (i / cols) * ch
            val k = vals[i] / maxv
            if (bubble) {
                sb.append("<rect x=\"${f(cx0 + 2f)}\" y=\"${f(cy0 + 2f)}\" width=\"${f(cw - 4f)}\" height=\"${f(ch - 4f)}\" rx=\"8\" fill=\"#f3f5fa\" stroke=\"$GRID\"/>")
                sb.append("<circle cx=\"${f(cx0 + cw / 2f)}\" cy=\"${f(cy0 + ch / 2f - 6f)}\" r=\"${f(8f + k * min(cw, ch) * 0.26f)}\" fill=\"${COLORS[i % COLORS.size]}\" fill-opacity=\"0.7\"/>")
            } else {
                val color = mix("#eceffe", "#4338ca", k)
                sb.append("<rect x=\"${f(cx0 + 2f)}\" y=\"${f(cy0 + 2f)}\" width=\"${f(cw - 4f)}\" height=\"${f(ch - 4f)}\" rx=\"8\" fill=\"$color\" stroke=\"#ffffff\" stroke-width=\"2\"/>")
            }
            sb.append(text(cx0 + cw / 2f, cy0 + ch - 16f, lab, if (bubble) MUTED else "#ffffff", "middle", bold = true, size = 10))
            sb.append(text(cx0 + cw / 2f, cy0 + ch - 4f, f(vals[i]), if (bubble) MUTED else "#ffffff", "middle", size = 9))
        }
        return sb.toString()
    }

    private fun surface(spec: FigureSpec): String {
        val nrows = faFloat(spec.xStr("nrows", "4")).roundToInt().coerceIn(2, 10)
        val ncols = faFloat(spec.xStr("ncols", "4")).roundToInt().coerceIn(2, 10)
        val vals = nums(spec, "vals", "1,2,3,2,2,4,5,3,3,5,6,4,2,3,4,3")
        val lo = vals.minOrNull() ?: 0f
        val hi = max(vals.maxOrNull() ?: 1f, lo + 0.001f)
        // تصویر ایزومتریک سادهٔ سطح: هر خانه یک لوزی رنگی با ارتفاع.
        val ox = 180f; val oy = 70f
        val dx = 22f; val dy = 11f; val hScale = 34f
        val sb = StringBuilder()
        for (r in 0 until nrows) for (c in 0 until ncols) {
            val v = vals.getOrElse(r * ncols + c) { 0f }
            val k = (v - lo) / (hi - lo)
            val px = ox + (c - r) * dx
            val py = oy + (c + r) * dy + (1f - k) * hScale
            val color = mix("#e7f6f2", "#0f766e", k)
            sb.append(
                "<path d=\"M ${f(px)} ${f(py)} L ${f(px + dx)} ${f(py + dy)} L ${f(px)} ${f(py + 2 * dy)} L ${f(px - dx)} ${f(py + dy)} Z\" " +
                    "fill=\"$color\" stroke=\"#ffffff\" stroke-width=\"1\"/>"
            )
        }
        return sb.toString()
    }

    private fun calendarHeat(spec: FigureSpec): String {
        val vals = nums(spec, "vals", "1,3,0,2,4,1,0,2,5,3,1,0,4,2,1,3,0,2,1,4,5,2,3,1,0,2,3,4")
        val cols = 7
        val rows = kotlin.math.ceil(vals.size / cols.toFloat()).toInt().coerceAtLeast(1)
        val lo = vals.minOrNull() ?: 0f
        val hi = max(vals.maxOrNull() ?: 1f, lo + 0.001f)
        val days = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val cell = min(30f, (R - L - 20f) / cols)
        val x0 = 180f - cols * cell / 2f
        val y0 = 52f
        val sb = StringBuilder()
        days.forEachIndexed { c, d -> sb.append(text(x0 + c * cell + cell / 2f, y0 - 8f, d, MUTED, "middle", size = 9)) }
        vals.forEachIndexed { i, v ->
            val r = i / cols; val c = i % cols
            if (r >= rows) return@forEachIndexed
            val k = (v - lo) / (hi - lo)
            sb.append(
                "<rect x=\"${f(x0 + c * cell + 1.5f)}\" y=\"${f(y0 + r * cell + 1.5f)}\" " +
                    "width=\"${f(cell - 3f)}\" height=\"${f(cell - 3f)}\" rx=\"4\" fill=\"${mix("#e7f6f2", "#166534", k)}\"/>"
            )
        }
        return sb.toString()
    }

    private fun rose(spec: FigureSpec): String {
        val labs = labels(spec, default = "شمال,شمال‌شرق,شرق,جنوب‌شرق,جنوب,جنوب‌غرب,غرب,شمال‌غرب")
        val vals = pad(nums(spec, "vals", "4,7,3,6,5,2,4,3"), labs.size)
        val maxv = max(vals.maxOrNull() ?: 1f, 1f)
        val cx = 180f; val cy = 138f; val maxR = 92f
        val n = labs.size
        val sb = StringBuilder()
        listOf(0.33f, 0.66f, 1f).forEach { k ->
            sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(maxR * k)}\" fill=\"none\" stroke=\"$GRID\"/>")
        }
        vals.forEachIndexed { i, v ->
            val a0 = (-90f + 360f * i / n) * PI.toFloat() / 180f
            val a1 = (-90f + 360f * (i + 1) / n) * PI.toFloat() / 180f
            val r = v / maxv * maxR
            sb.append(
                "<path d=\"M ${f(cx)} ${f(cy)} L ${f(cx + r * cos(a0))} ${f(cy + r * sin(a0))} " +
                    "A ${f(r)} ${f(r)} 0 0 1 ${f(cx + r * cos(a1))} ${f(cy + r * sin(a1))} Z\" " +
                    "fill=\"${COLORS[i % COLORS.size]}\" fill-opacity=\"0.65\" stroke=\"#ffffff\"/>"
            )
            val mid = (a0 + a1) / 2f
            if (labs[i].isNotBlank()) {
                sb.append(text(cx + (maxR + 15f) * cos(mid), cy + (maxR + 15f) * sin(mid) + 3f, labs[i], MUTED, "middle", size = 8))
            }
        }
        return sb.toString()
    }

    private fun wordCloud(spec: FigureSpec): String {
        val words = labels(spec, default = "ریاضی,فیزیک,شیمی,زیست,ادبیات,تاریخ")
        val weights = pad(nums(spec, "vals", "8,6,5,4,3,2"), words.size)
        val maxw = max(weights.maxOrNull() ?: 1f, 1f)
        // چیدمان مارپیچی ساده و قطعی (بدون تصادف) حول مرکز.
        val cx = 180f; val cy = 134f
        val sb = StringBuilder()
        val order = weights.indices.sortedByDescending { weights[it] }
        order.forEachIndexed { rank, i ->
            val k = weights[i] / maxw
            val size = (11f + k * 17f).roundToInt()
            val angle = rank * 2.4f
            val radius = 14f * kotlin.math.sqrt(rank.toFloat())
            val x = cx + radius * cos(angle)
            val y = cy + radius * sin(angle) * 0.62f
            sb.append(text(x, y, words[i], COLORS[i % COLORS.size], "middle", bold = k > 0.55f, size = size))
        }
        return sb.toString()
    }

    // ------------------------------------------------------------- utils

    private fun sqrtF(v: Float): Float = kotlin.math.sqrt(v)

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
