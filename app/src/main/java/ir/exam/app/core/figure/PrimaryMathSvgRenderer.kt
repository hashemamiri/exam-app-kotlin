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
        "axabacus", "axjump", "axbalance", "axthermo", "axsaw", "axpattern",
        "axtenframe", "axsym", "axspin", "axbeaker", "axgeoboard", "axdice", "axmularea", "axvenn",
        "axnumgrid", "axfacttree", "axfnmachine", "axfracwall", "axtangram", "axangle"
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
            "axtenframe" -> tenFrame(spec)
            "axsym" -> symmetryGrid(spec)
            "axspin" -> probabilitySpinner(spec)
            "axbeaker" -> graduatedBeaker(spec)
            "axgeoboard" -> geoBoard(spec)
            "axdice" -> dicePair(spec)
            "axmularea" -> areaModelMultiply(spec)
            "axvenn" -> vennDiagram(spec)
            "axnumgrid" -> hundredChart(spec)
            "axfacttree" -> factorTree(spec)
            "axfnmachine" -> functionMachine(spec)
            "axfracwall" -> fractionWall(spec)
            "axtangram" -> tangramPuzzle(spec)
            "axangle" -> protractorAngle(spec)
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
        val lbl = spec.xStr("label", "1")
        if (lbl == "2") sb.append(text(180f, 262f, "تعداد: ؟", PINK, "middle", bold = true, size = 14))
        else if (lbl != "0") sb.append(text(180f, 262f, "تعداد: ${n}", MU, "middle", size = 13))
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
        val lbl = spec.xStr("label", "1")
        if (lbl == "2") sb.append(text(180f, 362f, "عدد: ؟", PINK, "middle", bold = true, size = 14))
        else if (lbl != "0") sb.append(text(180f, 362f, "عدد: ${group(nb.toLong())}", ST, "middle", bold = true, size = 13))
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
        val cLbl = spec.xStr("label", "0")
        if (cLbl == "2") sb.append(text(cx, 272f, "ساعت = ؟ : ؟", PINK, "middle", bold = true, size = 13))
        else if (cLbl == "1") sb.append(text(cx, 272f, "%d:%02d".format(if (h == 0) 12 else h, m), MU, "middle", size = 13))
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
        sb.append(text(x1, y0 + hgt - 8f, "cm", MU, "middle", size = 11))
        if (a != null && b != null) {
            val ca = a.coerceIn(0f, len.toFloat())
            val cb = b.coerceIn(0f, len.toFloat())
            val xa = x0 + ca * unit
            val xb = x0 + cb * unit
            sb.append(line(xa, y0 - 22f, xb, y0 - 22f, PINK, 4f))
            sb.append(line(xa, y0 - 32f, xa, y0 - 12f, PINK, 2f))
            sb.append(line(xb, y0 - 32f, xb, y0 - 12f, PINK, 2f))
            val rLbl = spec.xStr("label", "1")
            val span = kotlin.math.abs(cb - ca)
            val spanTxt = if (span == Math.round(span).toFloat()) Math.round(span).toString() else span.toString()
            if (rLbl == "2") sb.append(text((xa + xb) / 2f, y0 - 30f, "؟ cm", PINK, "middle", bold = true, size = 12))
            else if (rLbl != "0") sb.append(text((xa + xb) / 2f, y0 - 30f, "$spanTxt cm", PINK, "middle", bold = true, size = 12))
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
                val y0 = 96f
                val h = 70f
                val cw = w / den
                for (i in 0 until den) sb.append(rect(x0 + i * cw, y0, cw, h, fillOf(i), ST, 1.6f))
            }
            "square" -> {
                val side = 168f
                val x0 = 96f
                val y0 = 40f
                val cols = ceil(sqrt(den.toDouble())).toInt()
                val rows = ceil(den.toDouble() / cols).toInt()
                val cwd = side / cols
                val chd = side / rows
                for (i in 0 until den) sb.append(rect(x0 + (i % cols) * cwd, y0 + (i / cols) * chd, cwd, chd, fillOf(i), ST, 1.6f))
            }
            "ring" -> {
                val cx = 180f
                val cy = 124f
                val ro = 88f
                val ri = 44f
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
                val cy = 124f
                val rr = 88f
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
        val fLbl = spec.xStr("label", "1")
        if (fLbl == "2") {
            sb.append(text(180f, 242f, "؟", PINK, "middle", bold = true, size = 18))
            sb.append(line(166f, 248f, 194f, 248f, PINK, 2f))
            sb.append(text(180f, 268f, "؟", PINK, "middle", bold = true, size = 18))
        } else if (fLbl != "0") {
            sb.append(text(180f, 242f, num.toString(), ST, "middle", bold = true, size = 18))
            sb.append(line(166f, 248f, 194f, 248f, ST, 2f))
            sb.append(text(180f, 268f, den.toString(), ST, "middle", bold = true, size = 18))
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
        val mSum = spec.xStr("sum", "0")
        if (mSum == "2") sb.append(text(180f, 272f, "جمع: ؟ $unit", PINK, "middle", bold = true, size = 13))
        else if (mSum == "1") sb.append(text(180f, 272f, "جمع: ${group(total)} $unit", PINK, "middle", bold = true, size = 13))
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
        val lblMode = spec.xStr("label", "1")
        if (lblMode == "2") {
            sb.append(text(180f, 252f, "$start + ($jumps × $step) = ؟", PINK, "middle", bold = true, size = 14))
        } else if (lblMode != "0") {
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
        val bLbl = spec.xStr("label", "1")
        if (bLbl == "2") sb.append(text(180f, 268f, "؟", PINK, "middle", bold = true, size = 20))
        else if (bLbl != "0") sb.append(text(180f, 268f, cmp, PINK, "middle", bold = true, size = 20))
        return sb.toString()
    }

    // ------------------------------------------------------ V314.0 — دماسنج
    private fun thermometer(spec: FigureSpec): String {
        val mn = spec.xNum("min", -10f)
        val mx = max(mn + 5f, spec.xNum("max", 50f))
        val v = spec.xNum("val", 25f).coerceIn(mn, mx)
        val sb = StringBuilder()
        val cx = 136f
        val top = 34f
        val bot = 204f
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
            sb.append(text(cx + 42f, y + 4f, "${tk.toInt()}°", ST, "middle", bold = true, size = 11))
            tk += step
        }
        sb.append(rect(224f, 102f, 94f, 44f, "#fff8e1", col, 1.8f, rx = 10f))
        sb.append(text(271f, 131f, "${v.toInt()}°C", col, "middle", bold = true, size = 20))
        return sb.toString()
    }

    // --------------- V314.0 / V317.0 — جدول شگفت‌انگیز و سودوکو (۳×۳ تا ۹×۹ + رنگی/شکلی/عددی)
    private class SawToken(val sym: String, val hex: String, val tint: String, val nameFa: String)

    private val SAW_PALETTE = mapOf(
        "1" to SawToken("●", "#e53935", "#ffebee", "قرمز"),
        "2" to SawToken("▲", "#1e88e5", "#e3f2fd", "آبی"),
        "3" to SawToken("■", "#43a047", "#e8f5e9", "سبز"),
        "4" to SawToken("★", "#fbc02d", "#fffde7", "زرد"),
        "5" to SawToken("◆", "#8e24aa", "#f3e5f5", "بنفش"),
        "6" to SawToken("♥", "#fb8c00", "#fff3e0", "نارنجی"),
        "7" to SawToken("⬢", "#00acc1", "#e0f7fa", "فیروزه‌ای"),
        "8" to SawToken("✿", "#ec407a", "#fce4ec", "صورتی"),
        "9" to SawToken("▼", "#6d4c41", "#efebe9", "قهوه‌ای")
    )

    private fun normalizeSawCell(rawCell: String): String {
        val c = faEnDigits(rawCell.trim())
        if (c.isEmpty() || c == "?" || c == "؟") return "?"
        if (c in SAW_PALETTE) return c
        return when {
            "قرمز" in c || "سرخ" in c || c == "●" -> "1"
            "آبی" in c || c == "▲" -> "2"
            "سبز" in c || c == "■" -> "3"
            "زرد" in c || c == "★" -> "4"
            "بنفش" in c || c == "◆" -> "5"
            "نارنجی" in c || c == "♥" -> "6"
            "فیروزه" in c || c == "⬢" -> "7"
            "صورتی" in c || c == "✿" -> "8"
            "قهوه" in c || c == "▼" -> "9"
            else -> c
        }
    }

    internal fun defaultSudokuRows(n: Int): List<List<String>> {
        val sz = n.coerceIn(3, 9)
        if (sz == 4) {
            return "1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1".split(';').map { it.split(',') }
        }
        return List(sz) { r ->
            List(sz) { c ->
                val v = when (sz) {
                    9 -> ((r * 3 + r / 3 + c) % 9) + 1
                    6 -> ((r * 3 + r / 2 + c) % 6) + 1
                    else -> ((r + c) % sz) + 1
                }
                if ((r * 2 + c + 1) % (sz + 1) == 0) "?" else v.toString()
            }
        }
    }

    private fun magicGrid(spec: FigureSpec): String {
        val raw = spec.xStr("vals", "1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1")
        val parsedRows = raw.split(';', '؛').map { it.split(',', '،').map { c -> normalizeSawCell(c) } }.filter { it.isNotEmpty() }
        val explicitSize = spec.xNum("size", 0f).toInt()
        val n = (if (explicitSize in 3..9) explicitSize else parsedRows.size.coerceIn(3, 9)).coerceIn(3, 9)
        val rows = if (parsedRows.size == n && parsedRows.all { it.size >= n }) parsedRows else defaultSudokuRows(n)
        val mode = spec.xStr("mode", "shape").lowercase()
        val showLegend = mode == "color" || mode == "color_shape"
        val maxGridPx = if (showLegend) 196f else 216f
        val cs = min(52f, maxGridPx / n)
        val gw = cs * n
        val x0 = (360f - gw) / 2f
        val y0 = (if (showLegend) 24f else 28f) + (maxGridPx - gw) / 2f
        val sb = StringBuilder()
        val fontSz = when {
            n <= 4 -> 21
            n <= 6 -> 16
            else -> 12
        }
        for (r in 0 until n) {
            for (c in 0 until n) {
                val cell = rows.getOrNull(r)?.getOrNull(c).orEmpty().ifBlank { "?" }
                val x = x0 + c * cs
                val y = y0 + r * cs
                val isQ = cell == "?" || cell == "؟"
                val tok = SAW_PALETTE[cell]
                val cellBg = when {
                    isQ -> "#fff8e1"
                    mode == "color" && tok != null -> tok.tint
                    mode == "color_shape" && tok != null -> tok.tint
                    else -> "#ffffff"
                }
                sb.append(rect(x, y, cs, cs, cellBg, "#cbd5e1", 1f))
                if (isQ) {
                    sb.append(text(x + cs / 2f, y + cs * 0.66f, "؟", PINK, "middle", bold = true, size = fontSz))
                } else if (mode == "color" && tok != null) {
                    val pad = max(3.5f, cs * 0.16f)
                    sb.append(rect(x + pad, y + pad, cs - pad * 2f, cs - pad * 2f, tok.hex, ST, 1.2f, rx = max(3f, cs * 0.16f)))
                } else if ((mode == "shape" || mode == "color_shape") && tok != null) {
                    sb.append(text(x + cs / 2f, y + cs * 0.68f, tok.sym, tok.hex, "middle", bold = true, size = fontSz + 2))
                } else {
                    sb.append(text(x + cs / 2f, y + cs * 0.66f, cell, ST, "middle", bold = true, size = fontSz))
                }
            }
        }
        val boxR = when (n) { 9 -> 3; 6 -> 2; 4 -> 2; else -> n }
        val boxC = when (n) { 9 -> 3; 6 -> 3; 4 -> 2; else -> n }
        for (r in 1 until n) {
            val thick = r % boxR == 0
            sb.append(line(x0, y0 + r * cs, x0 + gw, y0 + r * cs, ST, if (thick) 2.5f else 0.9f))
        }
        for (c in 1 until n) {
            val thick = c % boxC == 0
            sb.append(line(x0 + c * cs, y0, x0 + c * cs, y0 + gw, ST, if (thick) 2.5f else 0.9f))
        }
        sb.append(rect(x0, y0, gw, gw, "none", ST, 2.8f))
        if (showLegend) {
            val itemW = min(36f, 320f / n)
            val totalLW = itemW * n
            val lx0 = (360f - totalLW) / 2f
            val ly = 246f
            for (i in 1..n) {
                val tok = SAW_PALETTE[i.toString()] ?: continue
                val cx = lx0 + (i - 0.5f) * itemW
                sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(ly)}\" r=\"7.5\" fill=\"${tok.hex}\" stroke=\"$ST\" stroke-width=\"1.2\"/>")
                if (n <= 6) {
                    sb.append(text(cx, ly + 19f, tok.nameFa, MU, "middle", bold = true, size = 9))
                } else {
                    sb.append(text(cx, ly + 18f, i.toString(), MU, "middle", bold = true, size = 9))
                }
            }
        }
        return sb.toString()
    }

    // ----------------------------- V314.0 / V317.0 — الگویابی رنگی، شکلی و عددی
    private fun patternTrain(spec: FigureSpec): String {
        val items = spec.xList("seq", "●,▲,●,▲,●,?").map { it.trim() }.filter { it.isNotEmpty() }.take(12)
        if (items.isEmpty()) return ""
        val mode = spec.xStr("mode", "shape").lowercase()
        val sb = StringBuilder()
        val n = items.size
        val cw = min(42f, 300f / n)
        val gap = if (n <= 8) 6f else 4f
        val totalW = n * cw + (n - 1) * gap
        val x0 = (360f - totalW) / 2f
        val y0 = 112f
        val palette = listOf("#e53935", "#1e88e5", "#43a047", "#fbc02d", "#8e24aa", "#fb8c00")
        if (mode == "bead") {
            sb.append(line(x0 - 14f, y0 + 27f, x0 + totalW + 14f, y0 + 27f, ST, 3f))
        }
        items.forEachIndexed { i, it ->
            val x = x0 + i * (cw + gap)
            val isQ = it == "?" || it == "؟"
            val norm = normalizeSawCell(it)
            val tok = SAW_PALETTE[norm]
            val col = when {
                isQ -> PINK
                tok != null -> tok.hex
                else -> palette[i % palette.size]
            }
            if (mode == "bead") {
                val r = min(20f, cw * 0.45f)
                val cx = x + cw / 2f
                val cy = y0 + 27f
                sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(r)}\" fill=\"${if (isQ) "#fff8e1" else col}\" stroke=\"$ST\" stroke-width=\"2\"/>")
                if (isQ) sb.append(text(cx, cy + 6f, "؟", PINK, "middle", bold = true, size = 16))
            } else if (mode == "color") {
                sb.append(rect(x, y0, cw, 54f, if (isQ) "#fff8e1" else col, ST, 2f, rx = 10f))
                if (isQ) sb.append(text(x + cw / 2f, y0 + 34f, "؟", PINK, "middle", bold = true, size = 20))
            } else {
                sb.append(rect(x, y0, cw, 54f, if (isQ) "#fff8e1" else "rgba(108,99,245,.10)", col, 2f, rx = 9f))
                val label = if (isQ) "؟" else if (tok != null && it in SAW_PALETTE.keys) tok.sym else it
                sb.append(text(x + cw / 2f, y0 + 34f, label, col, "middle", bold = true, size = if (n > 8) 15 else 20))
            }
        }
        return sb.toString()
    }

    // --------------------------------------------------- V315.0 — قاب ده‌تایی
    private fun tenFrame(spec: FigureSpec): String {
        val frames = spec.xNum("frames", 2f).toInt().coerceIn(1, 2)
        val cap = frames * 10
        val a = spec.xNum("a", 7f).toInt().coerceIn(0, cap)
        val b = spec.xNum("b", 5f).toInt().coerceIn(0, cap - a)
        val sb = StringBuilder()
        val cw = 44f
        val ch = 44f
        val fw = cw * 5f
        val fh = ch * 2f
        val x0 = (360f - fw) / 2f
        for (fr in 0 until frames) {
            val fy = if (frames == 1) 88f else 32f + fr * (fh + 18f)
            sb.append(rect(x0, fy, fw, fh, "#ffffff", ST, 2.2f, rx = 6f))
            for (r in 0..1) {
                for (c in 0..4) {
                    val cx = x0 + c * cw
                    val cy = fy + r * ch
                    sb.append(rect(cx, cy, cw, ch, "none", ST, 1.2f))
                    val idx = fr * 10 + r * 5 + c
                    if (idx < a + b) {
                        val col = if (idx < a) "#e53935" else "#1e88e5"
                        sb.append("<circle cx=\"${f(cx + cw / 2f)}\" cy=\"${f(cy + ch / 2f)}\" r=\"14\" fill=\"$col\" stroke=\"$ST\" stroke-width=\"1.4\"/>")
                    }
                }
            }
        }
        val tfLbl = spec.xStr("label", "1")
        if (tfLbl == "2") sb.append(text(180f, 262f, "$a + $b = ؟", PINK, "middle", bold = true, size = 15))
        else if (tfLbl != "0") sb.append(text(180f, 262f, "$a + $b = ${a + b}", ST, "middle", bold = true, size = 15))
        return sb.toString()
    }

    // -------------------------------------------------- V315.0 — تقارن شطرنجی
    private fun symmetryGrid(spec: FigureSpec): String {
        val axis = spec.xStr("axis", "v").lowercase()
        val mirror = spec.xNum("mirror", 1f).toInt() != 0
        val rawCells = spec.xStr("cells", "1,1;2,1;2,2;3,2")
        val pairs = rawCells.split(';', '؛').mapNotNull { token ->
            val p = token.split(',', '،').mapNotNull { it.trim().toIntOrNull() }
            if (p.size >= 2) (p[0].coerceIn(1, 4) to p[1].coerceIn(1, 4)) else null
        }
        val sb = StringBuilder()
        val cell = 26f
        val n = 8
        val gw = cell * n
        val x0 = (360f - gw) / 2f
        val y0 = 32f
        sb.append(rect(x0, y0, gw, gw, "#f8fafc", ST, 1.8f))
        for (r in 0 until n) {
            for (c in 0 until n) {
                sb.append(rect(x0 + c * cell, y0 + r * cell, cell, cell, "none", "#cbd5e1", 1f))
            }
        }
        pairs.forEach { (r1, d1) ->
            if (axis == "h") {
                val rTop = (4 - d1).coerceIn(0, 3)
                val cPos = (r1 - 1).coerceIn(0, 7)
                sb.append(rect(x0 + cPos * cell + 2f, y0 + rTop * cell + 2f, cell - 4f, cell - 4f, ACC, ST, 1.2f, rx = 3f))
                val rBot = (3 + d1).coerceIn(4, 7)
                if (mirror) {
                    sb.append(rect(x0 + cPos * cell + 2f, y0 + rBot * cell + 2f, cell - 4f, cell - 4f, GREEN, ST, 1.2f, rx = 3f))
                } else {
                    sb.append(text(x0 + cPos * cell + cell / 2f, y0 + rBot * cell + cell * 0.68f, "؟", PINK, "middle", bold = true, size = 14))
                }
            } else {
                val rPos = (r1 - 1).coerceIn(0, 7)
                val cLeft = (4 - d1).coerceIn(0, 3)
                val cRight = (3 + d1).coerceIn(4, 7)
                sb.append(rect(x0 + cLeft * cell + 2f, y0 + rPos * cell + 2f, cell - 4f, cell - 4f, ACC, ST, 1.2f, rx = 3f))
                if (mirror) {
                    sb.append(rect(x0 + cRight * cell + 2f, y0 + rPos * cell + 2f, cell - 4f, cell - 4f, GREEN, ST, 1.2f, rx = 3f))
                } else {
                    sb.append(text(x0 + cRight * cell + cell / 2f, y0 + rPos * cell + cell * 0.68f, "؟", PINK, "middle", bold = true, size = 14))
                }
            }
        }
        if (axis == "h") {
            val my = y0 + 4f * cell
            sb.append("<line x1=\"${f(x0 - 12f)}\" y1=\"${f(my)}\" x2=\"${f(x0 + gw + 12f)}\" y2=\"${f(my)}\" stroke=\"$PINK\" stroke-width=\"3.2\" stroke-dasharray=\"6 4\"/>")
        } else {
            val mx = x0 + 4f * cell
            sb.append("<line x1=\"${f(mx)}\" y1=\"${f(y0 - 10f)}\" x2=\"${f(mx)}\" y2=\"${f(y0 + gw + 10f)}\" stroke=\"$PINK\" stroke-width=\"3.2\" stroke-dasharray=\"6 4\"/>")
        }
        return sb.toString()
    }

    // ------------------------------------------------- V315.0 — چرخندهٔ احتمال
    private fun probabilitySpinner(spec: FigureSpec): String {
        val parts = spec.xStr("parts", "قرمز,آبی,سبز,زرد,قرمز,آبی").split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("قرمز", "آبی", "سبز", "زرد") }.take(12)
        val n = parts.size.coerceAtLeast(2)
        val arrowIdx = (spec.xNum("arrow", 1f).toInt() - 1).mod(n)
        val cx = 180f
        val cy = 142f
        val r = 98f
        val palette = listOf("#ef5350", "#42a5f5", "#66bb6a", "#ffca28", "#ab47bc", "#26a69a", "#ffa726", "#ec407a")
        fun colorFor(lbl: String, idx: Int): String = when {
            "قرمز" in lbl || "سرخ" in lbl -> "#ef5350"
            "آبی" in lbl -> "#42a5f5"
            "سبز" in lbl -> "#66bb6a"
            "زرد" in lbl -> "#ffca28"
            "بنفش" in lbl -> "#ab47bc"
            "نارنجی" in lbl -> "#ffa726"
            else -> palette[idx % palette.size]
        }
        val sb = StringBuilder()
        for (i in 0 until n) {
            val a0 = (i * 360.0 / n - 90.0) * PI / 180.0
            val a1 = ((i + 1) * 360.0 / n - 90.0) * PI / 180.0
            val x1 = cx + r * cos(a0).toFloat()
            val y1 = cy + r * sin(a0).toFloat()
            val x2 = cx + r * cos(a1).toFloat()
            val y2 = cy + r * sin(a1).toFloat()
            val lg = if (360.0 / n > 180.0) 1 else 0
            val col = colorFor(parts[i], i)
            sb.append("<path d=\"M ${f(cx)} ${f(cy)} L ${f(x1)} ${f(y1)} A ${f(r)} ${f(r)} 0 $lg 1 ${f(x2)} ${f(y2)} Z\" fill=\"$col\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
            val am = (a0 + a1) / 2.0
            val tx = cx + (r * 0.62f) * cos(am).toFloat()
            val ty = cy + (r * 0.62f) * sin(am).toFloat() + 4f
            sb.append(text(tx, ty, parts[i], "#fff", "middle", bold = true, size = 11))
        }
        val ang = ((arrowIdx + 0.5) * 360.0 / n - 90.0) * PI / 180.0
        val ax = cx + (r * 0.48f) * cos(ang).toFloat()
        val ay = cy + (r * 0.48f) * sin(ang).toFloat()
        sb.append(line(cx, cy, ax, ay, ST, 4.2f))
        sb.append("<circle cx=\"${f(ax)}\" cy=\"${f(ay)}\" r=\"5.5\" fill=\"$GOLD\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
        sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"9\" fill=\"#fff\" stroke=\"$ST\" stroke-width=\"2.4\"/>")
        return sb.toString()
    }

    // --------------------------------------------- V315.0 — استوانهٔ مدرج (حجم)
    private fun graduatedBeaker(spec: FigureSpec): String {
        val mx = max(10f, spec.xNum("max", 500f))
        val v = spec.xNum("val", 350f).coerceIn(0f, mx)
        val unit = spec.xStr("unit", "mL")
        val sb = StringBuilder()
        val x0 = 96f
        val w = 112f
        val top = 36f
        val bot = 216f
        val h = bot - top
        val vh = h * (v / mx)
        val vy = bot - vh
        sb.append(rect(x0, top, w, h, "#f8fafc", "none", 0f))
        if (v > 0f) {
            sb.append(rect(x0 + 3f, vy, w - 6f, vh, "rgba(38,166,154,.42)", "#00897b", 1.5f))
        }
        sb.append("<path d=\"M ${f(x0 - 8f)} ${f(top)} L ${f(x0)} ${f(top)} L ${f(x0)} ${f(bot)} L ${f(x0 + w)} ${f(bot)} L ${f(x0 + w)} ${f(top)} L ${f(x0 + w + 8f)} ${f(top)}\" fill=\"none\" stroke=\"$ST\" stroke-width=\"3\"/>")
        sb.append(text(x0 + w + 34f, top - 8f, "($unit)", MU, "middle", bold = true, size = 11))
        for (i in 0..10) {
            val ty = bot - h * (i / 10f)
            val major = i % 2 == 0
            sb.append(line(x0 + w - (if (major) 18f else 10f), ty, x0 + w, ty, ST, if (major) 1.8f else 1.1f))
            if (major && i > 0) {
                val tv = Math.round(mx * i / 10f)
                sb.append(text(x0 + w + 34f, ty + 4f, tv.toString(), ST, "middle", bold = true, size = 12))
            }
        }
        sb.append(rect(x0 - 4f, 230f, w + 8f, 30f, "#e0f2f1", "#00897b", 1.5f, rx = 8f))
        sb.append(text(x0 + w / 2f, 250f, "${v.toInt()} $unit", "#00695c", "middle", bold = true, size = 15))
        return sb.toString()
    }

    // ------------------------------------------ V315.0 — ژئوبرد (تختهٔ میخی)
    private fun geoBoard(spec: FigureSpec): String {
        val g = spec.xNum("grid", 5f).toInt().coerceIn(3, 7)
        val pts = spec.xStr("pts", "1,1;4,1;4,3;1,3").split(';', '؛').mapNotNull { token ->
            val p = token.split(',', '،').mapNotNull { it.trim().toFloatOrNull() }
            if (p.size >= 2) (p[0].coerceIn(1f, g.toFloat()) to p[1].coerceIn(1f, g.toFloat())) else null
        }
        val sb = StringBuilder()
        val bw = 200f
        val x0 = (360f - bw) / 2f
        val y0 = 36f
        val step = bw / (g - 1).coerceAtLeast(1)
        sb.append(rect(x0 - 18f, y0 - 18f, bw + 36f, bw + 36f, "#fff8e1", ST, 2f, rx = 12f))
        if (pts.size >= 2) {
            val polyPts = pts.joinToString(" ") { (px, py) ->
                "${f(x0 + (px - 1f) * step)},${f(y0 + (g - py) * step)}"
            }
            sb.append("<polygon points=\"$polyPts\" fill=\"$ACC_SOFT\" stroke=\"$PINK\" stroke-width=\"3\"/>")
        }
        for (r in 0 until g) {
            for (c in 0 until g) {
                val px = x0 + c * step
                val py = y0 + r * step
                sb.append("<circle cx=\"${f(px)}\" cy=\"${f(py)}\" r=\"4.2\" fill=\"$ST\"/>")
            }
        }
        return sb.toString()
    }

    // ------------------------------------------- V315.0 — تاس و مکعب شمارش
    private fun dicePair(spec: FigureSpec): String {
        val list = spec.xStr("dice", "4,6").split(',', '،').mapNotNull { it.trim().toIntOrNull()?.coerceIn(1, 6) }.ifEmpty { listOf(4, 6) }.take(4)
        val sumMode = spec.xNum("sum", 1f).toInt()
        val sb = StringBuilder()
        val size = 66f
        val gap = 22f
        val totalW = list.size * size + (list.size - 1) * gap
        val x0 = (360f - totalW) / 2f
        val y0 = 92f
        val pipPos = mapOf(
            1 to listOf(0.5f to 0.5f),
            2 to listOf(0.26f to 0.26f, 0.74f to 0.74f),
            3 to listOf(0.24f to 0.24f, 0.5f to 0.5f, 0.76f to 0.76f),
            4 to listOf(0.26f to 0.26f, 0.74f to 0.26f, 0.26f to 0.74f, 0.74f to 0.74f),
            5 to listOf(0.24f to 0.24f, 0.76f to 0.24f, 0.5f to 0.5f, 0.24f to 0.76f, 0.76f to 0.76f),
            6 to listOf(0.26f to 0.22f, 0.74f to 0.22f, 0.26f to 0.5f, 0.74f to 0.5f, 0.26f to 0.78f, 0.74f to 0.78f)
        )
        list.forEachIndexed { idx, v ->
            val dx = x0 + idx * (size + gap)
            sb.append(rect(dx, y0, size, size, "#ffffff", ST, 2.2f, rx = 12f))
            pipPos[v].orEmpty().forEach { (rx, ry) ->
                sb.append("<circle cx=\"${f(dx + rx * size)}\" cy=\"${f(y0 + ry * size)}\" r=\"5.6\" fill=\"$PINK\"/>")
            }
            if (idx < list.lastIndex && sumMode != 0) {
                sb.append(text(dx + size + gap / 2f, y0 + size * 0.58f, "+", ST, "middle", bold = true, size = 18))
            }
        }
        if (sumMode == 1) {
            sb.append(text(180f, 214f, "${list.joinToString(" + ")} = ${list.sum()}", ST, "middle", bold = true, size = 16))
        } else if (sumMode == 2) {
            sb.append(text(180f, 214f, "${list.joinToString(" + ")} = ؟", PINK, "middle", bold = true, size = 16))
        }
        return sb.toString()
    }

    // ----------------------------------------- V315.0 — ضرب مساحتی (مستطیلی)
    private fun areaModelMultiply(spec: FigureSpec): String {
        val a = spec.xNum("a", 24f).toInt().coerceIn(10, 99)
        val b = spec.xNum("b", 13f).toInt().coerceIn(10, 99)
        val blank = spec.xNum("blank", 0f).toInt() != 0
        val a1 = (a / 10) * 10
        val a2 = a % 10
        val b1 = (b / 10) * 10
        val b2 = b % 10
        val sb = StringBuilder()
        val x0 = 76f
        val y0 = 56f
        val w1 = 136f
        val w2 = 76f
        val h1 = 96f
        val h2 = 58f
        sb.append(rect(x0, y0, w1, h1, "rgba(108,99,245,.16)", ST, 1.8f))
        sb.append(rect(x0 + w1, y0, w2, h1, "rgba(39,196,168,.18)", ST, 1.8f))
        sb.append(rect(x0, y0 + h1, w1, h2, "rgba(240,162,2,.18)", ST, 1.8f))
        sb.append(rect(x0 + w1, y0 + h1, w2, h2, "rgba(214,51,108,.15)", ST, 1.8f))
        sb.append(text(x0 + w1 / 2f, y0 - 10f, a1.toString(), ST, "middle", bold = true, size = 14))
        sb.append(text(x0 + w1 + w2 / 2f, y0 - 10f, a2.toString(), ST, "middle", bold = true, size = 14))
        sb.append(text(x0 - 26f, y0 + h1 / 2f + 5f, b1.toString(), ST, "middle", bold = true, size = 14))
        sb.append(text(x0 - 26f, y0 + h1 + h2 / 2f + 5f, b2.toString(), ST, "middle", bold = true, size = 14))
        sb.append(text(x0 + w1 / 2f, y0 + h1 / 2f + 6f, if (blank) "؟" else (a1 * b1).toString(), ST, "middle", bold = true, size = 15))
        sb.append(text(x0 + w1 + w2 / 2f, y0 + h1 / 2f + 6f, if (blank) "؟" else (a2 * b1).toString(), ST, "middle", bold = true, size = 15))
        sb.append(text(x0 + w1 / 2f, y0 + h1 + h2 / 2f + 6f, if (blank) "؟" else (a1 * b2).toString(), ST, "middle", bold = true, size = 15))
        sb.append(text(x0 + w1 + w2 / 2f, y0 + h1 + h2 / 2f + 6f, if (blank) "؟" else (a2 * b2).toString(), ST, "middle", bold = true, size = 15))
        sb.append(text(180f, 248f, if (blank) "$a × $b = ؟" else "$a × $b = ${a * b}", ST, "middle", bold = true, size = 15))
        return sb.toString()
    }

    // ----------------------------------------- V315.0 — نمودار ون (مجموعه‌ها)
    private fun vennDiagram(spec: FigureSpec): String {
        val leftStr = spec.xStr("left", "پرنده: کبوتر، گنجشک")
        val rightStr = spec.xStr("right", "آبزی: ماهی، میگو")
        val midStr = spec.xStr("mid", "اردک")
        val sb = StringBuilder()
        sb.append("<circle cx=\"128\" cy=\"150\" r=\"84\" fill=\"rgba(66,165,245,.20)\" stroke=\"#1e88e5\" stroke-width=\"2.2\"/>")
        sb.append("<circle cx=\"232\" cy=\"150\" r=\"84\" fill=\"rgba(102,187,106,.20)\" stroke=\"#43a047\" stroke-width=\"2.2\"/>")
        fun renderSide(raw: String, titleX: Float, itemX: Float, col: String, bg: String) {
            val hasColon = ':' in raw
            val titlePart = if (hasColon) raw.substringBefore(':').trim() else ""
            val bodyPart = if (hasColon) raw.substringAfter(':') else raw
            if (titlePart.isNotEmpty()) {
                sb.append(rect(titleX - 44f, 32f, 88f, 24f, bg, col, 1.5f, rx = 12f))
                sb.append(text(titleX, 48f, titlePart, col, "middle", bold = true, size = 12))
            }
            val items = bodyPart.split('،', ',').map { it.trim() }.filter { it.isNotEmpty() }.take(4)
            val startY = 154f - (items.size - 1) * 11f
            items.forEachIndexed { idx, ln ->
                sb.append(text(itemX, startY + idx * 22f, ln, ST, "middle", bold = true, size = 12))
            }
        }
        renderSide(leftStr, 114f, 82f, "#1565c0", "#e3f2fd")
        renderSide(rightStr, 246f, 278f, "#2e7d32", "#e8f5e9")
        val midItems = midStr.split(':', '،', ',').map { it.trim() }.filter { it.isNotEmpty() }.take(4)
        val midStartY = 154f - (midItems.size - 1) * 11f
        midItems.forEachIndexed { idx, ln ->
            sb.append(text(180f, midStartY + idx * 22f, ln, PINK, "middle", bold = true, size = 12))
        }
        return sb.toString()
    }

    // ----------------------------- V317.0 — جدول صدتایی و الگوهای عددی (۱ تا ۱۰۰)
    private fun isPrime(v: Int): Boolean {
        if (v < 2) return false
        var d = 2
        while (d * d <= v) {
            if (v % d == 0) return false
            d++
        }
        return true
    }

    private fun hundredChart(spec: FigureSpec): String {
        val start = spec.xNum("start", 1f).toInt().coerceIn(0, 900)
        val cols = faEnDigits(spec.xStr("cols", "10")).toIntOrNull()?.coerceIn(4, 10) ?: 10
        val rows = faEnDigits(spec.xStr("rows", "10")).toIntOrNull()?.coerceIn(3, 10) ?: 10
        val rule = faEnDigits(spec.xStr("rule", "5")).trim().lowercase()
        val stepVal = rule.toIntOrNull() ?: 0
        val blanks = spec.xStr("blanks", "15,35,60,85")
            .split(',', '،', ';', ' ')
            .mapNotNull { faEnDigits(it.trim()).toIntOrNull() }
            .toSet()
        val cs = min(280f / cols, 218f / rows)
        val gw = cs * cols
        val gh = cs * rows
        val x0 = (360f - gw) / 2f
        val y0 = 26f + (222f - gh) / 2f
        val fontSz = if (max(cols, rows) >= 9) 10 else 13
        val sb = StringBuilder()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val num = start + r * cols + c
                val x = x0 + c * cs
                val y = y0 + r * cs
                val isBlank = num in blanks
                val highlighted = when {
                    rule == "prime" -> isPrime(num)
                    rule == "even" -> num % 2 == 0
                    rule == "odd" -> num % 2 != 0
                    stepVal > 0 -> num > 0 && num % stepVal == 0
                    else -> false
                }
                val bg = when {
                    isBlank -> "#fff8e1"
                    highlighted -> "rgba(39,196,168,.26)"
                    else -> "#ffffff"
                }
                sb.append(rect(x, y, cs, cs, bg, "#cbd5e1", 0.9f))
                if (isBlank) {
                    sb.append(text(x + cs / 2f, y + cs * 0.68f, "؟", PINK, "middle", bold = true, size = fontSz + 1))
                } else {
                    val col = if (highlighted) "#00796b" else ST
                    sb.append(text(x + cs / 2f, y + cs * 0.68f, num.toString(), col, "middle", bold = highlighted, size = fontSz))
                }
            }
        }
        sb.append(rect(x0, y0, gw, gh, "none", ST, 2.2f))
        return sb.toString()
    }

    // ----------------------------------- V317.0 — درخت تجزیهٔ عوامل اول
    private fun smallestFactor(n: Int): Int {
        if (n % 2 == 0) return 2
        var d = 3
        while (d * d <= n) {
            if (n % d == 0) return d
            d += 2
        }
        return n
    }

    private fun factorTree(spec: FigureSpec): String {
        val n = spec.xNum("n", 36f).toInt().coerceIn(4, 999)
        val blankMode = spec.xNum("blank", 0f).toInt().coerceIn(0, 2)
        val sb = StringBuilder()

        fun splitBalanced(v: Int): Pair<Int, Int>? {
            if (isPrime(v) || v < 4) return null
            var best = smallestFactor(v)
            var d = 2
            while (d * d <= v) {
                if (v % d == 0) best = d
                d++
            }
            return best to (v / best)
        }

        fun drawNode(v: Int, cx: Float, cy: Float, spread: Float, depth: Int) {
            val sp = if (depth < 3) splitBalanced(v) else null
            if (sp != null) {
                val lx = cx - spread
                val rx = cx + spread
                val ny = cy + 62f
                sb.append(line(cx, cy + 16f, lx, ny - 16f, ST, 2f))
                sb.append(line(cx, cy + 16f, rx, ny - 16f, ST, 2f))
                drawNode(sp.first, lx, ny, spread * 0.52f, depth + 1)
                drawNode(sp.second, rx, ny, spread * 0.52f, depth + 1)
            }
            val leaf = sp == null
            val hide = (blankMode == 1 && leaf) || (blankMode == 2 && depth > 0 && !leaf)
            val fill = when {
                hide -> "#fff8e1"
                depth == 0 -> "rgba(108,99,245,.18)"
                leaf -> "rgba(39,196,168,.24)"
                else -> "#ffffff"
            }
            val stroke = if (hide) PINK else if (leaf) "#00897b" else ST
            if (leaf) {
                sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"17\" fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"2\"/>")
            } else {
                sb.append(rect(cx - 21f, cy - 15f, 42f, 30f, fill, stroke, 2f, rx = 8f))
            }
            sb.append(text(cx, cy + 5f, if (hide) "؟" else v.toString(), if (hide) PINK else ST, "middle", bold = true, size = 13))
        }

        drawNode(n, 180f, 44f, 78f, 0)
        return sb.toString()
    }

    // ----------------------------- V317.0 — ماشین ورودی و خروجی (یک و دو مرحله‌ای)
    private fun applyOp(v: Int, opRaw: String): Int {
        val s = faEnDigits(opRaw).trim()
        if (s.isEmpty()) return v
        val num = Regex("""-?\d+""").find(s)?.value?.toIntOrNull() ?: 0
        return when {
            s.startsWith("×") || s.startsWith("*") -> v * num
            s.startsWith("÷") || s.startsWith("/") -> if (num != 0) v / num else v
            s.startsWith("-") || s.startsWith("−") -> v - kotlin.math.abs(num)
            else -> v + num
        }
    }

    private fun functionMachine(spec: FigureSpec): String {
        val inputs = spec.xStr("in", "2,4,6,8")
            .split(',', '،', ';')
            .mapNotNull { faEnDigits(it.trim()).toIntOrNull() }
            .ifEmpty { listOf(2, 4, 6, 8) }
            .take(5)
        val op1 = spec.xStr("op1", "+ 3").trim()
        val op2 = spec.xStr("op2", "").trim()
        val blank = spec.xNum("blank", 1f).toInt().coerceIn(0, 2)
        val sb = StringBuilder()
        val twoStep = op2.isNotEmpty()
        // کادر ماشین در بالا
        sb.append(text(40f, 64f, "ورودی", MU, "middle", bold = true, size = 11))
        sb.append(line(62f, 60f, 92f, 60f, ST, 2.2f))
        if (twoStep) {
            sb.append(rect(94f, 38f, 74f, 44f, "rgba(108,99,245,.15)", ACC, 2.2f, rx = 10f))
            sb.append(text(131f, 65f, if (blank == 2) "؟" else op1, if (blank == 2) PINK else ACC, "middle", bold = true, size = 15))
            sb.append(line(170f, 60f, 192f, 60f, ST, 2.2f))
            sb.append(rect(194f, 38f, 74f, 44f, "rgba(39,196,168,.18)", "#00897b", 2.2f, rx = 10f))
            sb.append(text(231f, 65f, if (blank == 2) "؟" else op2, if (blank == 2) PINK else "#00897b", "middle", bold = true, size = 15))
            sb.append(line(270f, 60f, 298f, 60f, ST, 2.2f))
        } else {
            sb.append(rect(112f, 36f, 136f, 48f, "rgba(108,99,245,.15)", ACC, 2.2f, rx = 12f))
            sb.append(text(180f, 66f, if (blank == 2) "؟" else op1, if (blank == 2) PINK else ACC, "middle", bold = true, size = 17))
            sb.append(line(62f, 60f, 110f, 60f, ST, 2.2f))
            sb.append(line(250f, 60f, 298f, 60f, ST, 2.2f))
        }
        sb.append(text(322f, 64f, "خروجی", MU, "middle", bold = true, size = 11))

        // جدول ورودی و خروجی در پایین
        val cols = inputs.size + 1
        val cw = min(52f, 290f / cols)
        val tw = cw * cols
        val x0 = (360f - tw) / 2f
        val y0 = 116f
        val rh = 44f
        sb.append(rect(x0, y0, tw, rh * 2f, "#ffffff", ST, 2f, rx = 8f))
        sb.append(line(x0, y0 + rh, x0 + tw, y0 + rh, ST, 1.6f))
        sb.append(text(x0 + cw / 2f, y0 + 27f, "ورودی", ACC, "middle", bold = true, size = 12))
        sb.append(text(x0 + cw / 2f, y0 + rh + 27f, "خروجی", "#00897b", "middle", bold = true, size = 12))
        inputs.forEachIndexed { idx, inp ->
            val cx = x0 + (idx + 1) * cw
            sb.append(line(cx, y0, cx, y0 + rh * 2f, ST, 1.2f))
            val out = applyOp(applyOp(inp, op1), op2)
            sb.append(text(cx + cw / 2f, y0 + 28f, inp.toString(), ST, "middle", bold = true, size = 14))
            val hideOut = blank == 1 && idx >= inputs.size / 2
            if (hideOut) {
                sb.append(rect(cx + 3f, y0 + rh + 3f, cw - 6f, rh - 6f, "#fff8e1", "none", 0f, rx = 5f))
                sb.append(text(cx + cw / 2f, y0 + rh + 28f, "؟", PINK, "middle", bold = true, size = 16))
            } else {
                sb.append(text(cx + cw / 2f, y0 + rh + 28f, out.toString(), ST, "middle", bold = true, size = 14))
            }
        }
        return sb.toString()
    }

    // --------------------------------- V317.0 — دیوار کسرها (مقایسه و تساوی)
    private fun fractionWall(spec: FigureSpec): String {
        val dens = spec.xStr("dens", "1,2,3,4,6,8")
            .split(',', '،', ';')
            .mapNotNull { faEnDigits(it.trim()).toIntOrNull()?.coerceIn(1, 12) }
            .ifEmpty { listOf(1, 2, 3, 4, 6, 8) }
            .take(7)
        val fills = spec.xStr("fills", "1,1,2,2,3,4")
            .split(',', '،', ';')
            .mapNotNull { faEnDigits(it.trim()).toIntOrNull() }
        val mode = spec.xStr("mode", "label").lowercase()
        val sb = StringBuilder()
        val x0 = 30f
        val totalW = 300f
        val rh = min(34f, 210f / dens.size)
        val y0 = 30f + (214f - rh * dens.size) / 2f
        val rowPalette = listOf("#ef5350", "#42a5f5", "#66bb6a", "#ffa726", "#ab47bc", "#26a69a", "#ec407a")
        dens.forEachIndexed { rIdx, den ->
            val fillCnt = (fills.getOrElse(rIdx) { 1 }).coerceIn(0, den)
            val partW = totalW / den
            val ry = y0 + rIdx * rh
            val col = rowPalette[rIdx % rowPalette.size]
            for (c in 0 until den) {
                val px = x0 + c * partW
                val active = c < fillCnt
                sb.append(rect(px, ry, partW, rh, if (active) col else "#ffffff", ST, 1.3f))
                if (mode != "color") {
                    val lbl = if (mode == "blank" && active && rIdx > 0) "؟" else if (den == 1) "۱" else "1/$den"
                    val tCol = if (active) "#ffffff" else ST
                    sb.append(text(px + partW / 2f, ry + rh * 0.66f, lbl, tCol, "middle", bold = true, size = if (den >= 8) 9 else 11))
                }
            }
        }
        sb.append(rect(x0, y0, totalW, rh * dens.size, "none", ST, 2.2f))
        return sb.toString()
    }

    // -------------------------------------- V317.0 — تانگرام (پازل ۷ تکهٔ هندسی)
    private fun tangramPuzzle(spec: FigureSpec): String {
        val shape = spec.xStr("shape", "square").lowercase()
        val mode = spec.xStr("mode", "color").lowercase()
        val colors = listOf("#ef5350", "#42a5f5", "#66bb6a", "#fdd835", "#ab47bc", "#fb8c00", "#26a69a")
        val polys: List<Pair<String, Pair<Float, Float>>> = when (shape) {
            "house" -> listOf(
                "110,120 180,50 250,120" to (180f to 96f),
                "110,120 180,120 110,190" to (132f to 144f),
                "180,120 250,120 250,190" to (226f to 144f),
                "110,190 180,120 250,190" to (180f to 168f),
                "110,190 155,190 155,235 110,235" to (132f to 216f),
                "155,190 250,190 225,235 155,235" to (198f to 216f),
                "205,65 230,65 230,100 205,75" to (218f to 78f)
            )
            "boat" -> listOf(
                "175,45 175,155 105,155" to (152f to 122f),
                "183,65 245,155 183,155" to (204f to 128f),
                "90,165 270,165 235,210 125,210" to (180f to 192f),
                "125,210 235,210 210,235 150,235" to (180f to 226f),
                "175,45 198,45 175,68" to (183f to 54f),
                "105,155 135,130 135,155" to (125f to 148f),
                "245,155 268,140 258,165" to (255f to 154f)
            )
            "cat" -> listOf(
                "145,55 165,85 145,85" to (152f to 76f),
                "195,55 195,85 175,85" to (188f to 76f),
                "145,85 195,85 195,125 145,125" to (170f to 108f),
                "170,125 225,175 145,175" to (180f to 158f),
                "145,175 225,175 185,225" to (185f to 196f),
                "185,225 235,195 235,235" to (218f to 218f),
                "235,215 278,185 268,225 225,235" to (252f to 215f)
            )
            else -> listOf(
                "90,46 270,46 180,136" to (180f to 82f),
                "90,46 180,136 90,226" to (124f to 136f),
                "270,46 270,136 225,91" to (252f to 91f),
                "180,136 225,91 270,136 225,181" to (225f to 140f),
                "135,181 180,136 225,181" to (180f to 166f),
                "90,226 135,181 225,181 180,226" to (158f to 206f),
                "180,226 270,136 270,226" to (238f to 198f)
            )
        }
        val sb = StringBuilder()
        polys.forEachIndexed { idx, (pts, center) ->
            val fill = if (mode == "outline") "#ffffff" else colors[idx % colors.size]
            sb.append("<polygon points=\"$pts\" fill=\"$fill\" stroke=\"$ST\" stroke-width=\"2\" stroke-linejoin=\"round\"/>")
            if (mode == "num" || mode == "blank") {
                val lbl = if (mode == "blank" && idx % 2 == 1) "؟" else (idx + 1).toString()
                val tCol = if (mode == "outline") ST else "#ffffff"
                sb.append(text(center.first, center.second, lbl, tCol, "middle", bold = true, size = 13))
            }
        }
        return sb.toString()
    }

    // ---------------------------------- V317.0 — نقاله و اندازه‌گیری زاویه
    private fun protractorAngle(spec: FigureSpec): String {
        val deg = spec.xNum("deg", 65f).coerceIn(0f, 180f)
        val showPro = spec.xNum("pro", 1f).toInt() != 0
        val labelMode = spec.xStr("label", "1")
        val cx = 180f
        val cy = 212f
        val r = 132f
        val sb = StringBuilder()
        if (showPro) {
            sb.append("<path d=\"M ${f(cx - r)} ${f(cy)} A ${f(r)} ${f(r)} 0 0 1 ${f(cx + r)} ${f(cy)} Z\" fill=\"rgba(227,242,253,.55)\" stroke=\"#1e88e5\" stroke-width=\"1.8\"/>")
            sb.append("<path d=\"M ${f(cx - r * 0.55f)} ${f(cy)} A ${f(r * 0.55f)} ${f(r * 0.55f)} 0 0 1 ${f(cx + r * 0.55f)} ${f(cy)} Z\" fill=\"#ffffff\" stroke=\"#90caf9\" stroke-width=\"1.2\"/>")
            for (d in 0..180 step 10) {
                val rad = d * PI / 180.0
                val x1 = cx + (r - (if (d % 30 == 0) 12f else 6f)) * cos(rad).toFloat()
                val y1 = cy - (r - (if (d % 30 == 0) 12f else 6f)) * sin(rad).toFloat()
                val x2 = cx + r * cos(rad).toFloat()
                val y2 = cy - r * sin(rad).toFloat()
                sb.append(line(x1, y1, x2, y2, "#1565c0", if (d % 30 == 0) 1.6f else 0.9f))
                if (d % 30 == 0) {
                    val tx = cx + (r - 24f) * cos(rad).toFloat()
                    val ty = cy - (r - 24f) * sin(rad).toFloat() + 4f
                    sb.append(text(tx, ty, d.toString(), "#1565c0", "middle", bold = true, size = 10))
                }
            }
        }
        val rad = deg * PI / 180.0
        val rx = cx + (r + 10f) * cos(rad).toFloat()
        val ry = cy - (r + 10f) * sin(rad).toFloat()
        // کمان زاویه
        val ar = 38f
        val ax = cx + ar * cos(rad).toFloat()
        val ay = cy - ar * sin(rad).toFloat()
        if (kotlin.math.abs(deg - 90f) < 0.5f) {
            sb.append(rect(cx, cy - 22f, 22f, 22f, "rgba(214,51,108,.16)", PINK, 1.8f))
        } else if (deg > 0.5f) {
            sb.append("<path d=\"M ${f(cx + ar)} ${f(cy)} A ${f(ar)} ${f(ar)} 0 0 0 ${f(ax)} ${f(ay)} L ${f(cx)} ${f(cy)} Z\" fill=\"rgba(214,51,108,.18)\" stroke=\"$PINK\" stroke-width=\"1.8\"/>")
        }
        // دو ضلع زاویه
        sb.append(line(cx, cy, cx + r + 10f, cy, ST, 3f))
        sb.append(line(cx, cy, rx, ry, PINK, 3f))
        sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"4.5\" fill=\"$ST\"/>")
        val kindFa = when {
            kotlin.math.abs(deg - 90f) < 0.5f -> "زاویهٔ راست (قائمه)"
            kotlin.math.abs(deg - 180f) < 0.5f -> "زاویهٔ نیم‌صفحه"
            deg < 90f -> "زاویهٔ تند"
            else -> "زاویهٔ باز"
        }
        if (labelMode == "2") {
            sb.append(text(180f, 254f, "اندازهٔ زاویه = ؟°", PINK, "middle", bold = true, size = 14))
        } else if (labelMode != "0") {
            sb.append(text(180f, 254f, "${deg.toInt()}° ($kindFa)", ST, "middle", bold = true, size = 14))
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
        return "<text x=\"${f(x)}\" y=\"${f(y)}\" direction=\"ltr\" font-family=\"sans-serif\" font-size=\"$size\"$weight fill=\"$color\" text-anchor=\"$anchor\">$esc</text>"
    }
}
