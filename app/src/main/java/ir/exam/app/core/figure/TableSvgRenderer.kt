package ir.exam.app.core.figure

import ir.exam.app.core.math.MathSvgDocument
import java.security.MessageDigest
import java.util.Locale

/**
 * V53.1 — رندر Native جدول درسی (`k='t'`) به SVG امن و مستقل.
 *
 * ۱۸ سبک مرجع (`header/head2/simple/striped/lined/boxed/exam/matrix/truth/freq/
 * check/color/account/round/grid/note/blue/compact`) با همان قواعد سرستون
 * `isHead` مرجع بازتولید می‌شوند. خروجی فقط elementهای `svg/g/rect/line/path/text`
 * دارد؛ بدون style، script، URL خارجی یا foreignObject — سازگار با Coil SvgDecoder
 * و AndroidSVG برای چاپ/PDF.
 */
object TableSvgRenderer {

    /** سبک‌های مرجع با نام فارسی؛ ترتیب همان TYPES فایل مرجع است. */
    val STYLES: List<Pair<String, String>> = listOf(
        "header" to "سرستون",
        "head2" to "سرستون+سرردیف",
        "simple" to "ساده",
        "striped" to "راه‌راه",
        "lined" to "خط‌کشی افقی",
        "boxed" to "کادر ضخیم",
        "exam" to "آزمونی",
        "matrix" to "ماتریس",
        "truth" to "جدول ارزش",
        "freq" to "فراوانی",
        "check" to "چک‌لیست",
        "color" to "رنگی ستونی",
        "account" to "حسابداری",
        "round" to "مدرن گرد",
        "grid" to "خانه‌ای",
        "note" to "یادداشت",
        "blue" to "آبی آموزشی",
        "compact" to "فشرده"
    )

    const val MIN_ROWS = 1
    const val MAX_ROWS = 15
    const val MIN_COLS = 1
    const val MAX_COLS = 10

    private const val ACCENT = "#6c63f5"
    private const val INK = "#263142"
    private const val LINE = "#8995a6"

    /** نمونهٔ اولیهٔ هر سبک — دقیقاً همان تابع `sample` مرجع. */
    fun sampleCells(style: String, rows: Int, cols: Int): List<List<String>> {
        val preset: List<List<String>>? = when (style) {
            "truth" -> listOf(
                listOf("p", "q", "p∧q", "p∨q"),
                listOf("د", "د", "د", "د"),
                listOf("د", "ن", "ن", "د"),
                listOf("ن", "د", "ن", "د"),
                listOf("ن", "ن", "ن", "ن")
            )
            "freq" -> listOf(
                listOf("داده", "فراوانی", "نسبی"),
                listOf("A", "۸", "۰٫۴"),
                listOf("B", "۶", "۰٫۳"),
                listOf("C", "۶", "۰٫۳"),
                listOf("جمع", "۲۰", "۱")
            )
            "exam" -> listOf(
                listOf("#", "گزینه ۱", "گزینه ۲", "گزینه ۳"),
                listOf("۱", "", "", ""),
                listOf("۲", "", "", ""),
                listOf("۳", "", "", "")
            )
            "check" -> listOf(
                listOf("☐", "مورد", "توضیح"),
                listOf("☐", "", ""),
                listOf("☑", "", ""),
                listOf("☐", "", "")
            )
            "matrix" -> listOf(listOf("a", "b"), listOf("c", "d"))
            "account" -> listOf(
                listOf("شرح", "بدهکار", "بستانکار"),
                listOf("", "", ""),
                listOf("", "", ""),
                listOf("جمع", "", "")
            )
            else -> null
        }
        val base = preset ?: buildList {
            add(List(cols) { c -> "ستون ${c + 1}" })
            for (r in 1 until rows) add(List(cols) { c -> if (c == 0) r.toString() else "" })
        }
        return resize(base, rows.coerceIn(MIN_ROWS, MAX_ROWS), cols.coerceIn(MIN_COLS, MAX_COLS))
    }

    /** اندازهٔ پیش‌فرض هر سبک — همان تابع `def` مرجع. */
    fun defaultSize(style: String): Pair<Int, Int> {
        val rows = when (style) { "truth" -> 5; "matrix" -> 2; else -> 4 }
        val cols = when (style) { "matrix" -> 2; "account", "freq" -> 3; else -> 4 }
        return rows to cols
    }

    fun resize(cells: List<List<String>>, rows: Int, cols: Int): List<List<String>> {
        val r = rows.coerceIn(MIN_ROWS, MAX_ROWS)
        val c = cols.coerceIn(MIN_COLS, MAX_COLS)
        return List(r) { ri ->
            val row = cells.getOrNull(ri) ?: emptyList()
            List(c) { ci -> row.getOrNull(ci) ?: "" }
        }
    }

    /** قاعدهٔ سرستون مرجع (`isHead`). */
    fun isHead(style: String, row: Int, col: Int): Boolean = when (style) {
        "simple", "matrix", "grid" -> false
        "head2" -> row == 0 || col == 0
        else -> row == 0
    }

    fun render(spec: FigureSpec): MathSvgDocument {
        val style = spec.type.ifBlank { "header" }
        val cells0 = spec.tableCells().ifEmpty { listOf(listOf("")) }
        val cells = cells0.map { if (it.isEmpty()) listOf("") else it }
        val title = spec.xStr("title")
        val rows = cells.size
        val cols = cells.maxOf { it.size }

        val cellW = when {
            cols <= 3 -> 96f
            cols <= 5 -> 76f
            cols <= 7 -> 60f
            else -> 46f
        }
        val cellH = if (style == "compact") 26f else 32f
        val titleH = if (title.isNotBlank()) 26f else 0f
        val pad = 6f
        val tableW = cellW * cols
        val tableH = cellH * rows
        val width = tableW + pad * 2 + (if (style == "matrix") 24f else 0f)
        val height = tableH + titleH + pad * 2
        val x0 = pad + (if (style == "matrix") 12f else 0f)
        val y0 = pad + titleH

        val sb = StringBuilder()
        if (title.isNotBlank()) {
            sb.append(text(width / 2f, pad + 15f, title, INK, "middle", bold = true, size = 13))
        }
        // پس‌زمینه‌های سبک
        val fontScale = if (style == "compact") 10 else 12
        for (r in 0 until rows) for (c in 0 until cols) {
            val cx = x0 + c * cellW
            val cy = y0 + r * cellH
            val head = isHead(style, r, c)
            val fill = cellFill(style, r, c, head)
            if (fill != null) {
                sb.append("<rect x=\"${f(cx)}\" y=\"${f(cy)}\" width=\"${f(cellW)}\" height=\"${f(cellH)}\" fill=\"$fill\"/>")
            }
        }
        // خطوط
        sb.append(gridLines(style, x0, y0, cellW, cellH, rows, cols))
        // متن خانه‌ها
        for (r in 0 until rows) for (c in 0 until cols) {
            val value = cells[r].getOrNull(c).orEmpty()
            if (value.isBlank()) continue
            val head = isHead(style, r, c)
            val cx = x0 + c * cellW + cellW / 2f
            val cy = y0 + r * cellH + cellH / 2f + 4f
            sb.append(text(cx, cy, value, if (head) headInk(style) else INK, "middle", bold = head, size = fontScale))
        }
        // براکت‌های ماتریس
        if (style == "matrix") {
            val top = y0
            val bottom = y0 + tableH
            val lx = x0 - 8f
            val rx = x0 + tableW + 8f
            val bracket = { bx: Float, dir: Float ->
                "<path d=\"M ${f(bx + 6f * dir)} ${f(top)} L ${f(bx)} ${f(top)} L ${f(bx)} ${f(bottom)} L ${f(bx + 6f * dir)} ${f(bottom)}\" fill=\"none\" stroke=\"$INK\" stroke-width=\"2.2\"/>"
            }
            sb.append(bracket(lx, 1f)).append(bracket(rx, -1f))
        }

        val xml = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ${f(width)} ${f(height)}\" width=\"${f(width)}\" height=\"${f(height)}\" overflow=\"hidden\">$sb</svg>"
        return MathSvgDocument(
            xml = xml,
            widthPx = width,
            heightPx = height,
            cacheKey = "table-svg-${sha256(spec.toJson())}",
            editBoxes = emptyList()
        )
    }

    // ---------------------------------------------------------------- helpers

    private fun cellFill(style: String, row: Int, col: Int, head: Boolean): String? = when {
        head && style == "blue" -> "#cfe2ff"
        head && style == "truth" -> "#e5e1fb"
        head && style == "exam" -> "#efedfc"
        head -> "#eceffe"
        style == "striped" && row % 2 == 1 -> "#f0f2f8"
        style == "color" -> listOf("#efedfc", "#e7f6f2", "#fdf3e3", "#fdeaea")[col % 4]
        style == "note" -> "#fdf8e3"
        style == "freq" && row == 0 -> "#eceffe"
        else -> null
    }

    private fun headInk(style: String): String = when (style) {
        "blue" -> "#1d4f91"
        else -> ACCENT
    }

    private fun gridLines(style: String, x0: Float, y0: Float, cw: Float, ch: Float, rows: Int, cols: Int): String {
        val w = cw * cols
        val h = ch * rows
        val sb = StringBuilder()
        val strokeW = when (style) { "boxed" -> 2.4f; "compact" -> 0.9f; else -> 1.2f }
        fun hLine(y: Float, sw: Float = strokeW) =
            sb.append("<line x1=\"${f(x0)}\" y1=\"${f(y)}\" x2=\"${f(x0 + w)}\" y2=\"${f(y)}\" stroke=\"$LINE\" stroke-width=\"${f(sw)}\"/>")
        fun vLine(x: Float, sw: Float = strokeW) =
            sb.append("<line x1=\"${f(x)}\" y1=\"${f(y0)}\" x2=\"${f(x)}\" y2=\"${f(y0 + h)}\" stroke=\"$LINE\" stroke-width=\"${f(sw)}\"/>")
        when (style) {
            "matrix" -> Unit // فقط براکت؛ بدون خط
            "lined" -> for (r in 0..rows) hLine(y0 + r * ch)
            "account" -> {
                for (r in 0..rows) hLine(y0 + r * ch, if (r == rows - 1) 2.4f else strokeW)
                vLine(x0); vLine(x0 + w)
                for (c in 1 until cols) vLine(x0 + c * cw)
            }
            "round" -> {
                sb.append("<rect x=\"${f(x0)}\" y=\"${f(y0)}\" width=\"${f(w)}\" height=\"${f(h)}\" rx=\"10\" fill=\"none\" stroke=\"$LINE\" stroke-width=\"1.4\"/>")
                for (r in 1 until rows) hLine(y0 + r * ch)
                for (c in 1 until cols) vLine(x0 + c * cw)
            }
            else -> {
                for (r in 0..rows) hLine(y0 + r * ch)
                for (c in 0..cols) vLine(x0 + c * cw)
            }
        }
        return sb.toString()
    }

    private fun f(v: Float): String {
        val r = kotlin.math.round(v * 100f) / 100f
        return if (r == r.toInt().toFloat()) r.toInt().toString()
        else String.format(Locale.US, "%.2f", r).trimEnd('0').trimEnd('.')
    }

    private fun text(x: Float, y: Float, s: String, color: String, anchor: String, bold: Boolean, size: Int): String {
        val weight = if (bold) " font-weight=\"700\"" else ""
        return "<text x=\"${f(x)}\" y=\"${f(y)}\" font-family=\"sans-serif\" font-size=\"$size\"$weight fill=\"$color\" text-anchor=\"$anchor\" direction=\"rtl\">${escape(s)}</text>"
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

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(24)
}
