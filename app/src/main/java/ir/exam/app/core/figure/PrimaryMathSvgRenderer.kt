package ir.exam.app.core.figure

import kotlin.math.PI
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
    val SUPPORTED: Set<String> = setOf("axtally", "axbase10", "axplace", "axclock", "axruler", "axfrac", "axmoney")

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
            else -> ""
        }
    }

    // ---------------------------------------------------------------- چوب‌خط
    private fun tally(spec: FigureSpec): String {
        val n = spec.xNum("n", 7f).toInt().coerceIn(0, 60)
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

    // ------------------------------------------------------ دسته‌های ده‌تایی
    private fun base10(spec: FigureSpec): String {
        val n = spec.xNum("n", 134f).toInt().coerceIn(0, 999)
        val h = n / 100
        val t = (n / 10) % 10
        val o = n % 10
        val sb = StringBuilder()
        var x = 16f
        // صدگان: صفحهٔ ۱۰×۱۰ (۶۰×۶۰)
        for (i in 0 until h) {
            val gx = x + (i % 3) * 66f
            val gy = 36f + (i / 3) * 66f
            sb.append(rect(gx, gy, 60f, 60f, ACC_SOFT, ACC, 1.4f))
            for (k in 1 until 10) {
                sb.append(line(gx + k * 6f, gy, gx + k * 6f, gy + 60f, ACC, 0.6f))
                sb.append(line(gx, gy + k * 6f, gx + 60f, gy + k * 6f, ACC, 0.6f))
            }
        }
        if (h > 0) x += 3 * 66f + 8f
        // دهگان: میله‌های ۱۲×۶۰
        for (i in 0 until t) {
            val gx = x + i * 16f
            val gy = 36f
            sb.append(rect(gx, gy, 12f, 60f, "rgba(39,196,168,.25)", GREEN, 1.4f))
            for (k in 1 until 10) sb.append(line(gx, gy + k * 6f, gx + 12f, gy + k * 6f, GREEN, 0.8f))
        }
        if (t > 0) x += t * 16f + 10f
        // یکان: مکعب‌های ۱۲×۱۲ در دو ستون
        for (i in 0 until o) {
            val gx = x + (i / 5) * 16f
            val gy = 36f + (i % 5) * 16f
            sb.append(rect(gx, gy, 12f, 12f, "rgba(240,162,2,.3)", GOLD, 1.4f))
        }
        val y = 258f
        sb.append(text(60f, y, "صدگان: $h", MU, "middle", size = 12))
        sb.append(text(180f, y, "دهگان: $t", MU, "middle", size = 12))
        sb.append(text(300f, y, "یکان: $o", MU, "middle", size = 12))
        sb.append(text(180f, 276f, "عدد: $n", ST, "middle", bold = true, size = 13))
        return sb.toString()
    }

    // ------------------------------------------------------ جدول ارزش مکانی
    private fun placeValue(spec: FigureSpec): String {
        // ویرایشگر اپ مقدار عددی را Float ذخیره می‌کند (2457.0) → اول عدد، بعد رشتهٔ رقمی
        val s0 = spec.xStr("n", "2457").trim()
        val raw = s0.toDoubleOrNull()?.toLong()?.toString()
            ?: s0.filter { it.isDigit() || it in '۰'..'۹' }.map { if (it in '۰'..'۹') ('0' + (it - '۰')) else it }.joinToString("")
        val blank = spec.xStr("blank", "0") == "1"
        val names = listOf("یکان", "دهگان", "صدگان", "هزارگان", "ده‌هزارگان", "صدهزارگان", "میلیون")
        val cols = max(3, min(7, max(raw.length, spec.xNum("cols", 0f).toInt())))
        val w = 328f / cols
        val x0 = 16f
        val y0 = 40f
        val hh = 44f
        val rh = 60f
        val sb = StringBuilder()
        for (c in 0 until cols) {
            // ستون ۰ = یکان در سمت راست
            val x = x0 + (cols - 1 - c) * w
            sb.append(rect(x, y0, w, hh, ACC_SOFT, ACC, 1.4f))
            sb.append(text(x + w / 2, y0 + 27f, names[c], ST, "middle", bold = true, size = if (cols > 5) 10 else 12))
            sb.append(rect(x, y0 + hh, w, rh, "#fff", ACC, 1.4f))
            val digit = raw.getOrNull(raw.length - 1 - c)
            if (!blank && digit != null) sb.append(text(x + w / 2, y0 + hh + 40f, digit.toString(), PINK, "middle", bold = true, size = 26))
        }
        if (!blank && raw.isNotBlank()) sb.append(text(180f, 190f, "عدد: $raw", MU, "middle", size = 13))
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
        val len = spec.xNum("len", 10f).toInt().coerceIn(2, 30)
        val a = spec.xStr("a").toFloatOrNull()
        val b = spec.xStr("b").toFloatOrNull()
        val x0 = 24f
        val x1 = 336f
        val y0 = 110f
        val hgt = 70f
        val unit = (x1 - x0) / len
        val sb = StringBuilder()
        sb.append(rect(x0 - 10f, y0, x1 - x0 + 20f, hgt, "rgba(240,162,2,.16)", GOLD, 1.6f))
        val showMm = len <= 15
        for (i in 0..len) {
            val x = x0 + i * unit
            sb.append(line(x, y0, x, y0 + 26f, ST, 1.8f))
            sb.append(text(x, y0 + 46f, i.toString(), ST, "middle", size = 12))
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
        if (shape == "bar") {
            val x0 = 30f
            val w = 300f
            val y0 = 100f
            val h = 70f
            val cw = w / den
            for (i in 0 until den) sb.append(rect(x0 + i * cw, y0, cw, h, if (i < num) ACC else "#fff", ST, 1.6f))
        } else {
            val cx = 180f
            val cy = 140f
            val r = 96f
            if (den == 1) {
                sb.append("<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(r)}\" fill=\"${if (num == 1) ACC else "#fff"}\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
            } else for (i in 0 until den) {
                val a0 = (i * 360.0 / den - 90.0) * PI / 180.0
                val a1 = ((i + 1) * 360.0 / den - 90.0) * PI / 180.0
                val x0 = cx + r * cos(a0)
                val y0 = cy + r * sin(a0)
                val x1 = cx + r * cos(a1)
                val y1 = cy + r * sin(a1)
                val large = if (360.0 / den > 180.0) 1 else 0
                sb.append("<path d=\"M ${f(cx)} ${f(cy)} L ${f(x0.toFloat())} ${f(y0.toFloat())} A ${f(r)} ${f(r)} 0 $large 1 ${f(x1.toFloat())} ${f(y1.toFloat())} Z\" fill=\"${if (i < num) ACC else "#fff"}\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
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
        val notes = spec.xList("notes", "1000,2000,5000,10000").mapNotNull { it.trim().toLongOrNull() }.take(8)
        val coins = spec.xList("coins", "500,1000").mapNotNull { it.trim().toLongOrNull() }.take(8)
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
