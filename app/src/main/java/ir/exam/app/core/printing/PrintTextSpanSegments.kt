package ir.exam.app.core.printing

import ir.exam.app.domain.model.PrintTextSpan

/**
 * برش متنِ قابل‌چاپ به قطعه‌های بولد و ایتالیک.
 *
 * این منطق عمداً فقط مدل domain را می‌شناسد تا PDF رسمی برای رندر متن به
 * state یا ابزارهای رابط کاربری وابسته نباشد.
 */
internal object PrintTextSpanSegments {
    fun split(
        text: String,
        offsetInSource: Int,
        spans: List<PrintTextSpan>
    ): List<Triple<String, Boolean, Boolean>> {
        if (text.isEmpty() || spans.isEmpty()) return listOf(Triple(text, false, false))

        fun range(span: PrintTextSpan): Pair<Int, Int> =
            (span.start - offsetInSource).coerceIn(0, text.length) to
                (span.end - offsetInSource).coerceIn(0, text.length)

        val bounds = sortedSetOf(0, text.length)
        var hasStyledRange = false
        spans.forEach { span ->
            val (start, end) = range(span)
            if (end > start) {
                hasStyledRange = true
                bounds += start
                bounds += end
            }
        }
        if (!hasStyledRange) return listOf(Triple(text, false, false))

        val result = mutableListOf<Triple<String, Boolean, Boolean>>()
        val points = bounds.toList()
        for (index in 0 until points.lastIndex) {
            val start = points[index]
            val end = points[index + 1]
            if (end <= start) continue
            val bold = spans.any { span ->
                val (left, right) = range(span)
                right > left && left <= start && right >= end && span.bold
            }
            val italic = spans.any { span ->
                val (left, right) = range(span)
                right > left && left <= start && right >= end && span.italic
            }
            result += Triple(text.substring(start, end), bold, italic)
        }
        return result
    }
}
