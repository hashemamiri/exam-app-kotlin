package ir.exam.app.core.printing

import android.icu.text.ArabicShaping

/**
 * شکل‌دهی سبک فارسی/عربی برای خروجی PDF مستقیم.
 *
 * iText 7 Core مسئول bidi/layout است؛ این مرحله فقط اتصال حروف را با موتور
 * Android ICU انجام می‌دهد تا خروجی فارسی روی دستگاه‌هایی که pdfCalligraph
 * ندارند نیز مانند خروجی قبلی جدا و گسسته نشود.
 */
internal object PersianTextShaper {
    private val shaper = ArabicShaping(ArabicShaping.LETTERS_SHAPE)

    fun shape(text: String): String {
        if (text.isEmpty()) return text
        val chars = text.toCharArray()
        val result = StringBuilder(text.length)
        var index = 0
        while (index < chars.size) {
            if (!isArabicRun(chars[index])) {
                result.append(chars[index])
                index++
                continue
            }
            val start = index
            while (index < chars.size && isArabicRun(chars[index])) index++
            val run = String(chars, start, index - start)
            result.append(runCatching { shaper.shape(run) }.getOrDefault(run))
        }
        return result.toString()
    }

    private fun isArabicRun(c: Char): Boolean =
        c in '\u0600'..'\u06FF' ||
            c in '\u0750'..'\u077F' ||
            c in '\u08A0'..'\u08FF' ||
            c in '\uFB50'..'\uFDFF' ||
            c in '\uFE70'..'\uFEFF' ||
            c == '\u200C' || c == '\u200D'
}
