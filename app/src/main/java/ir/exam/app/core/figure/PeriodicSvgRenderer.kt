package ir.exam.app.core.figure

import ir.exam.app.core.math.MathSvgDocument
import java.security.MessageDigest
import java.util.Locale

/**
 * V53.2 — رندر Native جدول تناوبی (`k='p'`) به SVG امن و مستقل.
 *
 * قرارداد دادهٔ مرجع بدون تغییر مصرف می‌شود:
 * `X.Z` نمایش عدد اتمی، `X.hid` عناصر مخفی، `X.hidZ` عدد اتمی مخفی هر عنصر،
 * `X.hideCols` گروه‌های مخفی (۱..۱۸)، `X.hideRows` دوره‌های مخفی (۱..۷)،
 * `X.hideF` مخفی‌کردن بلوک لانتانید/اکتینید، `X.title` عنوان.
 * ستارهٔ `*`/`**` در خانهٔ گروه ۳ دوره‌های ۶ و ۷ مثل مرجع رسم می‌شود.
 * خروجی فقط `svg/rect/text` است؛ بدون style/script/URL/foreignObject.
 */
object PeriodicSvgRenderer {

    private const val INK = "#263142"
    private const val MUTED = "#5b6478"
    private const val CELL = 34f
    private const val GAP = 2f
    private const val LABEL = 20f
    private const val PAD = 6f

    private val FA_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun faNum(value: Int): String = value.toString().map { ch ->
        if (ch.isDigit()) FA_DIGITS[ch - '0'] else ch
    }.joinToString("")

    fun visibleGroups(spec: FigureSpec): List<Int> {
        val hidden = spec.xIntList("hideCols")
        return (1..18).filter { it !in hidden }
    }

    fun visiblePeriods(spec: FigureSpec): List<Int> {
        val hidden = spec.xIntList("hideRows")
        return (1..7).filter { it !in hidden }
    }

    fun render(spec: FigureSpec): MathSvgDocument {
        val title = spec.xStr("title")
        val showZ = spec.xStr("Z", "1") != "0"
        val hideF = spec.xStr("hideF", "0") == "1"
        val hidden = spec.xIntList("hid").toSet()
        val hiddenZ = spec.xIntList("hidZ").toSet()
        val groups = visibleGroups(spec)
        val periods = visiblePeriods(spec)

        val step = CELL + GAP
        val mainW = LABEL + groups.size * step
        val mainH = LABEL + periods.size * step
        val titleH = if (title.isNotBlank()) 24f else 0f
        // بلوک f: دو ردیف ۱۵تایی (گروه ۳..۱۷) دوره‌های ۸ و ۹ مرجع.
        val fRows = if (hideF) 0 else 2
        val fW = LABEL + 15 * step
        val fH = if (fRows == 0) 0f else fRows * step + 8f
        val width = maxOf(mainW, if (fRows == 0) 0f else fW) + PAD * 2
        val height = titleH + mainH + fH + PAD * 2

        val sb = StringBuilder()
        if (title.isNotBlank()) {
            sb.append(text(width / 2f, PAD + 14f, title, INK, "middle", bold = true, size = 13))
        }
        val topY = PAD + titleH

        // سرستون گروه‌ها
        groups.forEachIndexed { ci, g ->
            val x = PAD + LABEL + ci * step + CELL / 2f
            sb.append(text(x, topY + 13f, faNum(g), MUTED, "middle", bold = false, size = 9))
        }
        // بدنه اصلی
        periods.forEachIndexed { ri, p ->
            val y = topY + LABEL + ri * step
            sb.append(text(PAD + LABEL / 2f, y + CELL / 2f + 3f, faNum(p), MUTED, "middle", bold = false, size = 9))
            groups.forEachIndexed { ci, g ->
                val x = PAD + LABEL + ci * step
                val isFSlot = g == 3 && (p == 6 || p == 7)
                if (isFSlot) {
                    if (!hideF) {
                        sb.append(text(x + CELL / 2f, y + CELL / 2f + 5f, if (p == 6) "*" else "**", MUTED, "middle", bold = true, size = 13))
                    }
                } else {
                    PeriodicElements.at(g, p)?.let { el ->
                        sb.append(cell(x, y, el, showZ, el.z in hidden, el.z in hiddenZ))
                    }
                }
            }
        }
        // بلوک لانتانید/اکتینید (دوره‌های ۸ و ۹ داده)
        if (!hideF) {
            listOf(8, 9).forEachIndexed { ri, p ->
                val y = topY + mainH + 8f + ri * step
                val mark = if (p == 8) "*" else "**"
                sb.append(text(PAD + LABEL / 2f, y + CELL / 2f + 4f, mark, MUTED, "middle", bold = true, size = 12))
                (3..17).forEachIndexed { ci, g ->
                    val x = PAD + LABEL + ci * step
                    PeriodicElements.at(g, p)?.let { el ->
                        sb.append(cell(x, y, el, showZ, el.z in hidden, el.z in hiddenZ))
                    }
                }
            }
        }

        val xml = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ${f(width)} ${f(height)}\" width=\"${f(width)}\" height=\"${f(height)}\" overflow=\"hidden\">$sb</svg>"
        return MathSvgDocument(
            xml = xml,
            widthPx = width,
            heightPx = height,
            cacheKey = "periodic-svg-${sha(spec.toJson())}",
            editBoxes = emptyList()
        )
    }

    private fun cell(x: Float, y: Float, el: PeriodicElements.Element, showZ: Boolean, hidden: Boolean, zHidden: Boolean): String {
        val sb = StringBuilder()
        if (hidden) {
            // عنصر حذف‌شده: خانهٔ خالی خاکستری — همان `is-off` مرجع.
            sb.append("<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(CELL)}\" height=\"${f(CELL)}\" rx=\"4\" fill=\"#f1f3f7\" stroke=\"#d4d9e2\" stroke-width=\"1\"/>")
            return sb.toString()
        }
        val fill = PeriodicElements.CATEGORY_COLORS[el.category] ?: "#e2e8f0"
        sb.append("<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(CELL)}\" height=\"${f(CELL)}\" rx=\"4\" fill=\"$fill\" stroke=\"#b8bfcc\" stroke-width=\"0.8\"/>")
        if (showZ && !zHidden) {
            sb.append(text(x + CELL - 3f, y + 10f, faNum(el.z), MUTED, "end", bold = false, size = 7))
        }
        sb.append(text(x + CELL / 2f, y + CELL / 2f + 7f, el.symbol, INK, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun f(v: Float): String {
        val r = kotlin.math.round(v * 100f) / 100f
        return if (r == r.toInt().toFloat()) r.toInt().toString()
        else String.format(Locale.US, "%.2f", r).trimEnd('0').trimEnd('.')
    }

    private fun text(x: Float, y: Float, s: String, color: String, anchor: String, bold: Boolean, size: Int): String {
        val weight = if (bold) " font-weight=\"700\"" else ""
        return "<text x=\"${f(x)}\" y=\"${f(y)}\" font-family=\"sans-serif\" font-size=\"$size\"$weight fill=\"$color\" text-anchor=\"$anchor\">${escape(s)}</text>"
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

    private fun sha(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(24)
}
