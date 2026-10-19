package ir.exam.app.core.figure

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * V304 — شکل‌های آمادهٔ ریاضی ابتدایی (پایهٔ اول تا ششم): چوب‌خط، دسته‌های ده‌تایی، جدول ارزش مکانی،
 * ساعت عقربه‌ای، خط‌کش، کسر شکلی، سکه/اسکناس.
 *
 * شناسه‌ها با پیشوند `ax` هستند تا در همهٔ مسیرهای موجود (آیکن «محور»، تشخیص نوع در ExamBuilderScreen/چاپ،
 * `isAxisType` موتور وب و پل ExamPrintBridge.renderFigure) بدون تغییر ساختاری جا بیفتند.
 * بوم همان 360×280 محورهاست. پورت JS یک‌به‌یک: graph_fig.js → primarySvg().
 */
object PrimaryMathSvgRenderer {
    val SUPPORTED: Set<String> = setOf(
        "axtally", "axbase10", "axplace", "axclock", "axruler", "axfrac", "axmoney",
        "axabacus", "axjump", "axbalance", "axthermo", "axsaw", "axpattern"
    )

    private const val ST = "#2c3a50"
    private const val MU = "#4a5870"
    private const val ACC = "#6c63f5"
    private const val ACC_SOFT = "rgba(108,99,245,.18)"
    private const val PINK = "#d6336c"
    private const val GREEN = "#27c4a8"
    private const val GOLD = "#f0a202"

    fun body(spec: FigureSpec): String {
        val title = spec.xStr("title")
        val head = if (title.isBlank()) "" else text(180f, 14f, title, "#1a2433", "middle", bold = true, size = 13)
        return head + when (spec.type) {
            "axtally" -> tally(spec)
            "axbase10" -> base10(spec)
            "axplace" -> placeValue(spec)
            "axclock" -> clock(spec)
            "axruler" -> ruler(spec)
            "axfrac" -> fraction(spec)
            "axmoney" -> money(spec)
            "axabacus" -> abacus(spec)
            "axjump" -> jumpLine(spec)
            "axbalance" -> balanceScale(spec)
            "axthermo" -> thermometer(spec)
            "axsaw" -> magicGrid(spec)
            "axpattern" -> patternTrain(spec)
            else -> ""
        }
    }

    // ---------------------------------------------------------------- چوب‌خط
    private fun tally(spec: FigureSpec): String {
        val n = spec.xNum("n", 7f).toInt().coerceIn(0, 75)
        val sb = StringBuilder()
        val groups = (n + 4) / 5
        val perRow = 5
        val gw = 52f
        val gh = 58f
        val x0 = 32f
        val y0 = 44f
        for (g in 0 until groups) {
            val row = g / perRow
            val col = g % perRow
            val gx = x0 + col * (gw + 12f)
            val gy = y0 + row * (gh + 16f)
            val cnt = min(5, n - g * 5)
            for (i in 0 until min(cnt, 4)) {
                val x = gx + i * 11f
                sb.append(line(x, gy, x, gy + 40f, ST, 3f))
            }
            if (cnt == 5) sb.append(line(gx - 6f, gy + 34f, gx + 40f, gy + 6f, PINK, 3f))
        }
        sb.append(text(180f, 262f, "تعداد: ${n}", MU, "middle", size = 13))
        return sb.toString()
    }

    // ------------------------------------------------------ دسته‌های ده‌تایی (سه‌بعدی تا یک میلیون)
    private class Block(val a: Float, val b: Float, val h: Float, val color: String, val label: String, val cap: Float)

    private val BLOCKS = listOf(
        Block(1f, 1f, 1f, "#ffd54f", "یکان", 6f),
        Block(1f, 10f, 1f, "#66bb6a", "دهگان", 3f),
        Block(10f, 10f, 1f, "#42a5f5", "صدگان", 2.4f),
        Block(10f, 10f, 10f, "#ab47bc", "یکان هزار", 1.8f),
        Block(1f, 10f, 10f, "#ef5350", "دهگان هزار", 2f),
        Block(10f, 10f, 10f, "#26a69a", "صدگان هزار", 1.8f),
        Block(10f, 10f, 10f, "#ff7043", "یکان میلیون", 1.8f),
        Block(10f, 10f, 10f, "#8d6e63", "دهگان میلیون", 1.8f),
        Block(10f, 10f, 10f, "#5c6bc0", "صدگان میلیون", 1.8f)
    )

    private fun shade(hex: String, k: Float): String {
        fun ch(o: Int): Int = min(255f, hex.substring(o, o + 2).toInt(16) * k).toInt()
        return "rgb(${ch(1)},${ch(3)},${ch(5)})"
    }

    private fun isoBox(left: Float, bottom: Float, a: Float, b: Float, h: Float, k: Float, col: String): String {
        val ox = left + b * 0.866f * k
        val oy = bottom - (a + b) * 0.5f * k
        fun p(x: Float, y: Float, z: Float): String = "${f(ox + (x - y) * 0.866f * k)},${f(oy + (x + y) * 0.5f * k - z * k)}"
        fun poly(pts: String, c: String): String = "<polygon points=\"$pts\" fill=\"$c\"/>"
        return poly("${p(a, 0f, 0f)} ${p(a, b, 0f)} ${p(a, b, h)} ${p(a, 0f, h)}", shade(col, 0.72f)) +
            poly("${p(0f, b, 0f)} ${p(a, b, 0f)} ${p(a, b, h)} ${p(0f, b, h)}", shade(col, 0.86f)) +
            poly("${p(0f, 0f, h)} ${p(a, 0f, h)} ${p(a, b, h)} ${p(0f, b, h)}", col)
    }

    /** V310.9 — برچسب ltr: در بیدی RTL، برچسب‌های «start» به چپ کادر بیرون می‌زدند. */
    private fun ltrLabel(x: Float, y: Float, s: String, color: String, size: Int): String =
        "<text x=\"${f(x)}\" y=\"${f(y)}\" direction=\"ltr\" font-family=\"sans-serif\" font-size=\"$size\" font-weight=\"700\" fill=\"$color\" text-anchor=\"start\">$s</text>"

    private fun base10(spec: FigureSpec): String {
        val nb = spec.xNum("n", 134f).toInt().coerceIn(0, 999999999)
        val digits = IntArray(9)
        var q = nb
        for (k in 0 until 9) { digits[k] = q % 10; q /= 10 }
        val sb = StringBuilder()
        for (bp in 0 until 9) {
            val blk = BLOCKS[bp]
            val top = 4f + (8 - bp) * 36f
            val bot = top + 33f
            val n = digits[bp]
            sb.append(ltrLabel(4f, top + 14f, blk.label, ST, 10))
            if (n == 0) continue
            sb.append(ltrLabel(4f, top + 28f, "× $n", MU, 11))
            val kh = 32f / ((blk.a + blk.b) * 0.5f + blk.h)
            val kw = (262f - 4f * n) / (n * (blk.a + blk.b) * 0.866f)
            val k = min(blk.cap, min(kh, kw))
            var slot = 92f
            repeat(n) {
                sb.append(isoBox(slot, bot, blk.a, blk.b, blk.h, k, blk.color))
                slot += (blk.a + blk.b) * 0.866f * k + 4f
            }
        }
        sb.append(text(180f, 362f, "عدد: ${group(nb.toLong())}", ST, "middle", bold = true, size = 13))
        return sb.toString()
    }

    // ------------------------------------------------------ ارزش مکانی (میلیارد تا یکی؛ چند ردیف و علامت + / −)
    /** ارتفاع بوم (۳۶۰ عرض ثابت): دسته‌های ده‌تایی و جدول ارزش مکانی بسته به تعداد ردیف‌ها بلندتر می‌شوند. */
    fun canvasHeight(spec: FigureSpec): Float = when (spec.type) {
        "axbase10" -> 370f
        "axplace" -> max(280f, 80f + 40f * placeRows(spec) + 60f)
        else -> 280f
    }

    private fun placeRows(spec: FigureSpec): Int = spec.xNum("rows", 3f).toInt().coerceIn(1, 6)

    private fun rowDigits(raw: String): String {
        val d = faEnDigits(raw.substringBefore('.')).filter { it.isDigit() }
        if (d.isEmpty()) return ""
        return d.trimStart('0').ifEmpty { "0" }
    }

    private fun placeValue(spec: FigureSpec): String {
        val rowsN = placeRows(spec)
        val blank = spec.xStr("blank", "0") == "1"
        val vals = listOf(
            rowDigits(spec.xStr("n", "2457")).ifEmpty { "0" },
            rowDigits(spec.xStr("n2")),
            rowDigits(spec.xStr("n3"))
        )
        val ops = listOf(spec.xStr("op2").trim(), spec.xStr("op3").trim())
        val per = listOf("میلیارد" to "#d62828", "میلیون" to "#e0a800", "هزار" to "#2e9e44", "یکی" to "#7b3fbf")
        val labels = listOf("ص", "د", "ی")
        val cw = 326f / 12f
        val x0 = 30f
        val pw = 3 * cw
        val rh = 40f
        val top = 80f
        val bot = top + rh * rowsN
        val sb = StringBuilder()
        sb.append(rect(x0, 22f, 12 * cw, bot - 22f + 4f, "#f7f9fc", ST, 1.2f))
        for (pq in 0 until 4) {
            val ppx = x0 + pq * pw
            val pgrp = 3 - pq
            sb.append(text(ppx + pw / 2, 40f, per[pq].first, per[pq].second, "middle", bold = true, size = 13))
            sb.append(rect(ppx, 48f, pw, 26f, ACC_SOFT, per[pq].second, 1.6f))
            for (pj in 0 until 3) {
                val pcx = ppx + pj * cw
                sb.append(text(pcx + cw / 2, 66f, labels[pj], ST, "middle", bold = true, size = 11))
                if (pj > 0) sb.append(line(pcx, 48f, pcx, bot, "#8fa3c7", 0.8f))
                for (pr in 0 until rowsN) {
                    sb.append(rect(pcx, top + pr * rh, cw, rh, "#fff", "#8fa3c7", 0.8f))
                    val str = if (pr < 3) vals[pr] else ""
                    val idx = str.length - 1 - (pgrp * 3 + (2 - pj))
                    val dg = if (idx >= 0) str[idx].toString() else null
                    if (!(pr == 0 && blank) && dg != null) {
                        sb.append(text(pcx + cw / 2, top + pr * rh + 27f, dg, PINK, "middle", bold = true, size = 22))
                    }
                }
            }
            sb.append(rect(ppx, 48f, pw, bot - 48f, "none", per[pq].second, 2f))
        }
        for (pr in 1 until min(rowsN, 3)) {
            val op = ops[pr - 1].replace('-', '−')
            if (op == "+" || op == "−") sb.append(text(16f, top + pr * rh + 27f, op, ST, "middle", bold = true, size = 22))
        }
        if (vals.any { it.length > 12 }) {
            sb.append(text(180f, bot + 22f, "بیشتر از ۱۲ رقم: فقط ۱۲ رقم آخر نمایش داده می‌شود", MU, "middle", size = 11))
        }
        return sb.toString()
    }

    // -------------------------------------------------------------- ساعت
    private fun clock(spec: FigureSpec): String {
        val h = spec.xNum("h", 3f).toInt().let { ((it % 12) + 12) % 12 }
        val m = spec.xNum("m", 0f).toInt().coerceIn(0, 59)
        val nums = spec.xStr("nums", "1") != "0"
        val cx = 180f
        val cy = 150f
        val r = 104f
        val sb = StringBuilder()
        sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(r)}\" fill=\"#fff\" stroke=\"$ST\" stroke-width=\"3\"/>")
        for (i in 0 until 60) {
            val a = i * 6.0 * PI / 180.0
            val big = i % 5 == 0
            val r1 = if (big) r - 12f else r - 6f
            sb.append(line(cx + (r1 * sin(a)).toFloat(), cy - (r1 * cos(a)).toFloat(), cx + ((r - 2f) * sin(a)).toFloat(), cy - ((r - 2f) * cos(a)).toFloat(), ST, if (big) 2.2f else 1f))
        }
        if (nums) for (i in 1..12) {
            val a = i * 30.0 * PI / 180.0
            val rr = r - 26f
            sb.append(text(cx + (rr * sin(a)).toFloat(), cy - (rr * cos(a)).toFloat() + 5f, i.toString(), ST, "middle", bold = true, size = 14))
        }
        val ah = ((h % 12) * 30.0 + m * 0.5) * PI / 180.0
        val am = m * 6.0 * PI / 180.0
        sb.append(line(cx, cy, cx + (r * 0.5f * sin(ah)).toFloat(), cy - (r * 0.5f * cos(ah)).toFloat(), ST, 6f))
        sb.append(line(cx, cy, cx + (r * 0.78f * sin(am)).toFloat(), cy - (r * 0.78f * cos(am)).toFloat(), PINK, 3.5f))
        sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"5\" fill=\"$ST\"/>")
        if (spec.xStr("label", "0") == "1") sb.append(text(cx, 272f, "%d:%02d".format(if (h == 0) 12 else h, m), MU, "middle", size = 13))
        return sb.toString()
    }

    // -------------------------------------------------------------- خط‌کش
    private fun ruler(spec: FigureSpec): String {
        val len = spec.xNum("len", 10f).toInt().coerceIn(2, 50)
        val a = faEnDigits(spec.xStr("a")).toFloatOrNull()
        val b = faEnDigits(spec.xStr("b")).toFloatOrNull()
        val x0 = 24f
        val x1 = 336f
        val y0 = 110f
        val hgt = 70f
        val unit = (x1 - x0) / len
        val lstep = if (len <= 15) 1 else if (len <= 30) 2 else 5
        val sb = StringBuilder()
        sb.append(rect(x0 - 10f, y0, x1 - x0 + 20f, hgt, "rgba(240,162,2,.16)", GOLD, 1.6f))
        val showMm = len <= 15
        for (i in 0..len) {
            val x = x0 + i * unit
            sb.append(line(x, y0, x, y0 + (if (i % lstep == 0) 26f else 14f), ST, 1.8f))
            if (i % lstep == 0 || i == len) sb.append(text(x, y0 + 46f, i.toString(), ST, "middle", size = if (len > 30) 10 else 12))
            if (showMm && i < len) for (k in 1 until 10) {
                val xx = x + k * unit / 10f
                sb.append(line(xx, y0, xx, y0 + (if (k == 5) 17f else 10f), ST, if (k == 5) 1.2f else 0.8f))
            }
        }
        sb.append(text(x1 + 4f, y0 + hgt - 8f, "cm", MU, "end", size = 11))
        if (a != null && b != null) {
            val xa = x0 + a.coerceIn(0f, len.toFloat()) * unit
            val xb = x0 + b.coerceIn(0f, len.toFloat()) * unit
            sb.append(line(xa, y0 - 22f, xb, y0 - 22f, PINK, 4f))
            sb.append(line(xa, y0 - 32f, xa, y0 - 12f, PINK, 2f))
            sb.append(line(xb, y0 - 32f, xb, y0 - 12f, PINK, 2f))
        }
        return sb.toString()
    }

    // ------------------------------------------------------------ کسر شکلی
    private fun fraction(spec: FigureSpec): String {
        val den = spec.xNum("den", 4f).toInt().coerceIn(1, 24)
        val num = spec.xNum("num", 1f).toInt().coerceIn(0, den)
        val shape = spec.xStr("shape", "circle")
        val sb = StringBuilder()
        fun fillOf(i: Int): String = if (i < num) ACC else "#fff"
        when (shape) {
            "bar" -> {
                val x0 = 30f
                val w = 300f
                val y0 = 100f
                val h = 70f
                val cw = w / den
                for (i in 0 until den) sb.append(rect(x0 + i * cw, y0, cw, h, fillOf(i), ST, 1.6f))
            }
            "square" -> {
                val side = 180f
                val x0 = 90f
                val y0 = 50f
                val cols = ceil(sqrt(den.toDouble())).toInt()
                val rows = ceil(den.toDouble() / cols).toInt()
                val cwd = side / cols
                val chd = side / rows
                for (i in 0 until den) sb.append(rect(x0 + (i % cols) * cwd, y0 + (i / cols) * chd, cwd, chd, fillOf(i), ST, 1.6f))
            }
            "ring" -> {
                val cx = 180f
                val cy = 140f
                val ro = 96f
                val ri = 48f
                if (den == 1) {
                    sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(ro)}\" fill=\"${fillOf(0)}\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
                    sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(ri)}\" fill=\"#fff\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
                } else for (i in 0 until den) {
                    val a0 = (i * 360.0 / den - 90.0) * PI / 180.0
                    val a1 = ((i + 1) * 360.0 / den - 90.0) * PI / 180.0
                    val large = if (360.0 / den > 180.0) 1 else 0
                    fun px(rad: Float, a: Double): String = "${f(cx + rad * cos(a).toFloat())} ${f(cy + rad * sin(a).toFloat())}"
                    sb.append("<path d=\"M ${px(ro, a0)} A ${f(ro)} ${f(ro)} 0 $large 1 ${px(ro, a1)} L ${px(ri, a1)} A ${f(ri)} ${f(ri)} 0 $large 0 ${px(ri, a0)} Z\" fill=\"${fillOf(i)}\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
                }
            }
            else -> {
                val cx = 180f
                val cy = 140f
                val rr = 96f
                if (den == 1) {
                    sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(rr)}\" fill=\"${fillOf(0)}\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
                } else for (i in 0 until den) {
                    val a0 = (i * 360.0 / den - 90.0) * PI / 180.0
                    val a1 = ((i + 1) * 360.0 / den - 90.0) * PI / 180.0
                    val large = if (360.0 / den > 180.0) 1 else 0
                    val x0 = cx + rr * cos(a0).toFloat()
                    val y0 = cy + rr * sin(a0).toFloat()
                    val x1 = cx + rr * cos(a1).toFloat()
                    val y1 = cy + rr * sin(a1).toFloat()
                    sb.append("<path d=\"M ${f(cx)} ${f(cy)} L ${f(x0)} ${f(y0)} A ${f(rr)} ${f(rr)} 0 $large 1 ${f(x1)} ${f(y1)} Z\" fill=\"${fillOf(i)}\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
                }
            }
        }
        if (spec.xStr("label", "1") != "0") {
            sb.append(text(180f, 250f, num.toString(), ST, "middle", bold = true, size = 20))
            sb.append(line(166f, 256f, 194f, 256f, ST, 2f))
            sb.append(text(180f, 276f, den.toString(), ST, "middle", bold = true, size = 20))
        }
        return sb.toString()
    }

    // --------------------------------------------------------- سکه/اسکناس
    private fun money(spec: FigureSpec): String {
        val notes = spec.xList("notes", "1000,2000,5000,10000").mapNotNull { faEnDigits(it.trim()).toLongOrNull() }.take(8)
        val coins = spec.xList("coins", "500,1000").mapNotNull { faEnDigits(it.trim()).toLongOrNull() }.take(8)
        val unit = spec.xStr("unit", "تومان")
        val sb = StringBuilder()
        var x = 18f
        var y = 36f
        val nw = 100f
        val nh = 50f
        notes.forEachIndexed { i, v ->
            if (i > 0 && i % 3 == 0) { x = 18f; y += nh + 14f }
            sb.append(rect(x, y, nw, nh, "rgba(39,196,168,.18)", GREEN, 1.6f, rx = 8f))
            sb.append(rect(x + 6f, y + 6f, nw - 12f, nh - 12f, "none", GREEN, 0.8f, rx = 5f))
            sb.append(text(x + nw / 2, y + 24f, group(v), ST, "middle", bold = true, size = 14))
            sb.append(text(x + nw / 2, y + 40f, unit, MU, "middle", size = 10))
            x += nw + 12f
        }
        if (notes.isNotEmpty()) y += nh + 24f else y = 50f
        x = 18f
        var total = notes.sum()
        coins.forEach { v ->
            if (x + 50f > 350f) { x = 18f; y += 60f }
            sb.append("<circle cx=\"${f(x + 24f)}\" cy=\"${f(y + 24f)}\" r=\"24\" fill=\"rgba(240,162,2,.28)\" stroke=\"$GOLD\" stroke-width=\"2\"/>")
            sb.append("<circle cx=\"${f(x + 24f)}\" cy=\"${f(y + 24f)}\" r=\"18\" fill=\"none\" stroke=\"$GOLD\" stroke-width=\"0.8\"/>")
            sb.append(text(x + 24f, y + 28f, group(v), ST, "middle", bold = true, size = 11))
            x += 60f
            total += v
        }
        if (spec.xStr("sum", "0") == "1") sb.append(text(180f, 272f, "جمع: ${group(total)} $unit", PINK, "middle", bold = true, size = 13))
        return sb.toString()
    }

    // --------------------------------------------------- V314.0 — چرتکهٔ ستونی
    private fun abacus(spec: FigureSpec): String {
        val cols = spec.xNum("cols", 4f).toInt().coerceIn(2, 6)
        val n = spec.xNum("n", 2435f).toLong().coerceIn(0L, 999999L)
        val labels = listOf("یکان", "دهگان", "صدگان", "هزار", "ده‌هزار", "صدهزار")
        val colors = listOf("#ef5350", "#42a5f5", "#66bb6a", "#ab47bc", "#ffa726", "#26a69a")
        val sb = StringBuilder()
        val x0 = 36f
        val w = 288f
        val step = w / cols
        sb.append(rect(24f, 214f, 312f, 22f, "#8d6e63", ST, 1.8f, rx = 6f))
        var rem = n
        val digits = IntArray(cols)
        for (i in 0 until cols) { digits[i] = (rem % 10L).toInt(); rem /= 10L }
        for (c in 0 until cols) {
            val idx = cols - 1 - c
            val cx = x0 + c * step + step / 2f
            sb.append(line(cx, 44f, cx, 214f, ST, 3.2f))
            val d = digits[idx]
            val col = colors[idx % colors.size]
            for (b in 0 until d) {
                val by = 204f - b * 16f
                sb.append(rect(cx - 16f, by - 7f, 32f, 14f, col, ST, 1.2f, rx = 7f))
            }
            sb.append(text(cx, 230f, labels[idx], "#fff", "middle", bold = true, size = 10))
            if (spec.xStr("blank", "0") != "1") {
                sb.append(text(cx, 258f, d.toString(), ST, "middle", bold = true, size = 15))
            }
        }
        return sb.toString()
    }

    // ---------------------------------------------- V314.0 — پرش روی محور اعداد
    private fun jumpLine(spec: FigureSpec): String {
        val start = spec.xNum("start", 0f).toInt().coerceIn(0, 50)
        val step = spec.xNum("step", 3f).toInt().coerceIn(1, 20)
        val jumps = spec.xNum("jumps", 4f).toInt().coerceIn(1, 10)
        val maxVal = max(start + step * jumps, spec.xNum("max", (start + step * jumps).toFloat()).toInt()).coerceIn(2, 100)
        val sb = StringBuilder()
        val x0 = 28f
        val x1 = 332f
        val y = 186f
        sb.append(line(x0 - 8f, y, x1 + 10f, y, ST, 2.2f))
        fun px(v: Int): Float = x0 + (x1 - x0) * (v.toFloat() / maxVal.toFloat())
        val tickStep = if (maxVal <= 15) 1 else if (maxVal <= 30) 2 else 5
        for (v in 0..maxVal) {
            val x = px(v)
            val major = v % tickStep == 0
            sb.append(line(x, y - (if (major) 7f else 4f), x, y + (if (major) 7f else 4f), ST, if (major) 1.8f else 1f))
            if (major) sb.append(text(x, y + 22f, v.toString(), ST, "middle", bold = true, size = 11))
        }
        for (j in 0 until jumps) {
            val a = start + j * step
            val b = a + step
            if (b > maxVal) break
            val xa = px(a)
            val xb = px(b)
            val xm = (xa + xb) / 2f
            val h = min(85f, max(34f, (xb - xa) * 0.65f))
            sb.append("<path d=\"M ${f(xa)} ${f(y - 4f)} Q ${f(xm)} ${f(y - h)} ${f(xb)} ${f(y - 4f)}\" fill=\"none\" stroke=\"$ACC\" stroke-width=\"2.4\"/>")
            sb.append("<circle cx=\"${f(xb)}\" cy=\"${f(y)}\" r=\"4\" fill=\"$PINK\"/>")
            sb.append(text(xm, y - h * 0.58f, "+$step", PINK, "middle", bold = true, size = 11))
        }
        val endVal = start + jumps * step
        if (spec.xStr("label", "1") != "0") {
            sb.append(text(180f, 252f, "$start + ($jumps × $step) = $endVal", ST, "middle", bold = true, size = 14))
        }
        return sb.toString()
    }

    // ----------------------------------------------- V314.0 — ترازوی دوکفه‌ای
    private fun balanceScale(spec: FigureSpec): String {
        val left = spec.xNum("left", 5f)
        val right = spec.xNum("right", 3f)
        val unit = spec.xStr("unit", "کیلوگرم")
        val diff = (right - left).coerceIn(-10f, 10f)
        val angle = (diff / 10f) * 14f * (PI.toFloat() / 180f)
        val cx = 180f
        val cy = 116f
        val arm = 104f
        val lx = cx - arm * cos(angle)
        val ly = cy - arm * sin(angle)
        val rx = cx + arm * cos(angle)
        val ry = cy + arm * sin(angle)
        val sb = StringBuilder()
        sb.append("<polygon points=\"${f(cx)},${f(cy)} ${f(cx - 26f)},236 ${f(cx + 26f)},236\" fill=\"#90a4ae\" stroke=\"$ST\" stroke-width=\"2\"/>")
        sb.append(rect(cx - 56f, 236f, 112f, 14f, ST, ST, 1f, rx = 5f))
        sb.append(line(lx, ly, rx, ry, ST, 4f))
        sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"7\" fill=\"$GOLD\" stroke=\"$ST\" stroke-width=\"2\"/>")
        fun pan(px: Float, py: Float, v: Float, col: String) {
            sb.append(line(px, py, px - 28f, py + 44f, MU, 1.5f))
            sb.append(line(px, py, px + 28f, py + 44f, MU, 1.5f))
            sb.append(rect(px - 36f, py + 44f, 72f, 10f, GOLD, ST, 1.6f, rx = 4f))
            sb.append(rect(px - 22f, py + 14f, 44f, 30f, col, ST, 1.6f, rx = 5f))
            val lbl = if (v == Math.round(v).toFloat()) Math.round(v).toString() else v.toString()
            sb.append(text(px, py + 33f, lbl, "#fff", "middle", bold = true, size = 13))
            sb.append(text(px, py + 68f, "$lbl $unit", ST, "middle", bold = true, size = 11))
        }
        pan(lx, ly, left, ACC)
        pan(rx, ry, right, GREEN)
        val cmp = if (left > right) ">" else if (left < right) "<" else "="
        sb.append(text(180f, 268f, cmp, PINK, "middle", bold = true, size = 20))
        return sb.toString()
    }

    // ------------------------------------------------------ V314.0 — دماسنج
    private fun thermometer(spec: FigureSpec): String {
        val mn = spec.xNum("min", -10f)
        val mx = max(mn + 5f, spec.xNum("max", 50f))
        val v = spec.xNum("val", 25f).coerceIn(mn, mx)
        val sb = StringBuilder()
        val cx = 160f
        val top = 34f
        val bot = 206f
        fun py(t: Float): Float = bot - (bot - top) * ((t - mn) / (mx - mn))
        val vy = py(v)
        val col = if (v <= 0f) "#1e88e5" else "#e53935"
        sb.append(rect(cx - 12f, top - 8f, 24f, bot - top + 16f, "#f8fafc", ST, 2f, rx = 12f))
        sb.append(rect(cx - 7f, vy, 14f, bot - vy + 12f, col, "none", 0f, rx = 6f))
        sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(bot + 22f)}\" r=\"22\" fill=\"$col\" stroke=\"$ST\" stroke-width=\"2\"/>")
        val step = if (mx - mn <= 20f) 2f else if (mx - mn <= 60f) 10f else 20f
        var tk = ceil(mn / step) * step
        while (tk <= mx + 0.01f) {
            val y = py(tk)
            sb.append(line(cx + 12f, y, cx + 22f, y, ST, 1.6f))
            sb.append(text(cx + 28f, y + 4f, "${tk.toInt()}°", ST, "start", bold = true, size = 11))
            tk += step
        }
        sb.append(text(255f, 130f, "${v.toInt()}°C", col, "middle", bold = true, size = 22))
        return sb.toString()
    }

    // ------------------------------------ V314.0 — جدول شگفت‌انگیز (سودوکو ۴×۴)
    private fun magicGrid(spec: FigureSpec): String {
        val raw = spec.xStr("vals", "1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1")
        val shapeMode = spec.xStr("mode", "shape") == "shape"
        val shapes = mapOf("1" to ("●" to "#ef5350"), "2" to ("▲" to "#42a5f5"), "3" to ("■" to "#66bb6a"), "4" to ("★" to "#ffa726"))
        val rows = raw.split(';').map { it.split(',').map { c -> faEnDigits(c.trim()) } }
        val sb = StringBuilder()
        val x0 = 80f
        val y0 = 36f
        val cs = 50f
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val cell = rows.getOrNull(r)?.getOrNull(c).orEmpty().ifBlank { "?" }
                val x = x0 + c * cs
                val y = y0 + r * cs
                val isQ = cell == "?"
                sb.append(rect(x, y, cs, cs, if (isQ) "#fff8e1" else "#ffffff", ST, 1.2f))
                if (isQ) {
                    sb.append(text(x + cs / 2f, y + 32f, "؟", PINK, "middle", bold = true, size = 22))
                } else if (shapeMode && cell in shapes) {
                    val pair = shapes.getValue(cell)
                    sb.append(text(x + cs / 2f, y + 33f, pair.first, pair.second, "middle", bold = true, size = 24))
                } else {
                    sb.append(text(x + cs / 2f, y + 32f, cell, ST, "middle", bold = true, size = 20))
                }
            }
        }
        sb.append(rect(x0, y0, cs * 4f, cs * 4f, "none", ST, 2.8f))
        sb.append(line(x0 + cs * 2f, y0, x0 + cs * 2f, y0 + cs * 4f, ST, 2.6f))
        sb.append(line(x0, y0 + cs * 2f, x0 + cs * 4f, y0 + cs * 2f, ST, 2.6f))
        return sb.toString()
    }

    // ----------------------------------------- V314.0 — الگویابی شکلی و عددی
    private fun patternTrain(spec: FigureSpec): String {
        val items = spec.xList("seq", "●,▲,●,▲,●,?").map { it.trim() }.filter { it.isNotEmpty() }.take(8)
        if (items.isEmpty()) return ""
        val sb = StringBuilder()
        val n = items.size
        val cw = min(42f, 290f / n)
        val gap = 6f
        val totalW = n * cw + (n - 1) * gap
        val x0 = (360f - totalW) / 2f
        val y0 = 112f
        val palette = listOf("#6c63f5", "#27c4a8", "#f0a202", "#d6336c", "#42a5f5")
        items.forEachIndexed { i, it ->
            val x = x0 + i * (cw + gap)
            val isQ = it == "?" || it == "؟"
            val col = if (isQ) PINK else palette[i % palette.size]
            sb.append(rect(x, y0, cw, 54f, if (isQ) "#fff8e1" else "rgba(108,99,245,.10)", col, 2f, rx = 9f))
            sb.append(text(x + cw / 2f, y0 + 34f, if (isQ) "؟" else it, col, "middle", bold = true, size = 20))
        }
        return sb.toString()
    }

    private fun group(v: Long): String {
        val s = v.toString()
        val sb = StringBuilder()
        s.reversed().forEachIndexed { i, c -> if (i > 0 && i % 3 == 0) sb.append('،'); sb.append(c) }
        return sb.reverse().toString()
    }

    // -------------------------------------------------------------- helpers
    private fun f(v: Float): String {
        val r = Math.round(v * 100f) / 100f
        return if (r == Math.round(r).toFloat()) Math.round(r).toString() else r.toString()
    }

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: String, w: Float): String =
        "<line x1=\"${f(x1)}\" y1=\"${f(y1)}\" x2=\"${f(x2)}\" y2=\"${f(y2)}\" stroke=\"$color\" stroke-width=\"${f(w)}\" stroke-linecap=\"round\"/>"

    private fun rect(x: Float, y: Float, w: Float, h: Float, fill: String, stroke: String, sw: Float, rx: Float = 0f): String =
        "<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(w)}\" height=\"${f(h)}\"${if (rx > 0f) " rx=\"${f(rx)}\"" else ""} fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"${f(sw)}\"/>"

    private fun text(x: Float, y: Float, s: String, color: String, anchor: String, bold: Boolean = false, size: Int = 11): String {
        if (s.isBlank()) return ""
        val weight = if (bold) " font-weight=\"700\"" else ""
        val esc = FigureDigits.apply(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        return "<text x=\"${f(x)}\" y=\"${f(y)}\" font-family=\"sans-serif\" font-size=\"$size\"$weight fill=\"$color\" text-anchor=\"$anchor\">$esc</text>"
    }
}
