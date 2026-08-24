package ir.exam.app.ui.builder

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import ir.exam.app.core.figure.FigureSpec

/**
 * V55.17 — گزارش دستگاه: «با درج چیز در گزینه‌ها، کادر متن گزینه پر از کد
 * می‌شود». مقدار واقعی فیلد همان توکن %%FIG:{json}%% می‌ماند (منبع حقیقت،
 * سازگار با رندر/چاپ/بانک)، اما «نمایش» داخل کادر متن به تراشهٔ کوتاه
 * «⟦نوع⟧» تبدیل می‌شود؛ پیش‌نمایش واقعی زیر کادر است.
 */
object FigTokenVisuals {

    internal val TOKEN = Regex("""%%FIG:(\{.*?\})%%""", RegexOption.DOT_MATCHES_ALL)

    /** برچسب کوتاه فارسی توکن بر اساس kind/type آن. */
    fun chipLabel(json: String): String {
        val spec = FigureSpec.parse(json) ?: return "شکل"
        return when (spec.kind) {
            "t" -> "جدول"
            "p" -> "جدول تناوبی"
            "a" -> "آناتومی"
            "s" -> "فیزیک/شیمی"
            else -> spec.xStr("title").ifBlank { "شکل/نمودار" }
        }
    }

    /**
     * هر توکن در «نمایش» با ⟦برچسب⟧ جایگزین می‌شود. نگاشت offset:
     * داخل توکن مقصد/مبدأ به مرز انتهای همان قطعه می‌چسبد (رفتار اتمی تراشه).
     */
    fun transformation(accent: Color): VisualTransformation = VisualTransformation { text ->
        val source = text.text
        val matches = TOKEN.findAll(source).toList()
        if (matches.isEmpty()) return@VisualTransformation TransformedText(text, OffsetMapping.Identity)

        // قطعه: [srcStart, srcEnd) → [outStart, outEnd)
        data class Seg(val srcStart: Int, val srcEnd: Int, val outStart: Int, val outEnd: Int)
        val segments = mutableListOf<Seg>()
        val out = buildAnnotatedString {
            var cursor = 0
            matches.forEach { m ->
                if (m.range.first > cursor) append(source.substring(cursor, m.range.first))
                val outStart = length
                val style = pushStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold))
                append("⟦${chipLabel(m.groupValues[1])}⟧")
                pop()
                check(style >= 0)
                segments += Seg(m.range.first, m.range.last + 1, outStart, length)
                cursor = m.range.last + 1
            }
            if (cursor < source.length) append(source.substring(cursor))
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var shift = 0
                for (seg in segments) {
                    if (offset <= seg.srcStart) return (offset + shift).coerceIn(0, out.length)
                    if (offset < seg.srcEnd) return seg.outEnd
                    shift += (seg.outEnd - seg.outStart) - (seg.srcEnd - seg.srcStart)
                }
                return (offset + shift).coerceIn(0, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var shift = 0
                for (seg in segments) {
                    if (offset <= seg.outStart) return (offset - shift).coerceIn(0, source.length)
                    if (offset < seg.outEnd) return seg.srcEnd
                    shift += (seg.outEnd - seg.outStart) - (seg.srcEnd - seg.srcStart)
                }
                return (offset - shift).coerceIn(0, source.length)
            }
        }
        TransformedText(out, mapping)
    }
}
