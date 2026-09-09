package ir.exam.app.core.math

data class FormulaOccurrence(
    val index: Int,
    val start: Int,
    val endExclusive: Int,
    val tex: String
)

/** مدیریت فرمول‌های `$...$` داخل متن سؤال، گزینه و matching برای ویرایش مستقیم. */
object FormulaTextCodec {
    fun occurrences(source: String): List<FormulaOccurrence> {
        val result = mutableListOf<FormulaOccurrence>()
        var cursor = 0
        while (cursor < source.length) {
            if (source[cursor] != '$' || isEscaped(source, cursor)) {
                cursor++
                continue
            }
            if (cursor + 1 < source.length && source[cursor + 1] == '$') {
                cursor += 2
                continue
            }
            val start = cursor
            cursor++
            val contentStart = cursor
            var end = -1
            while (cursor < source.length) {
                if (source[cursor] == '$' && !isEscaped(source, cursor)) {
                    if (cursor + 1 < source.length && source[cursor + 1] == '$') {
                        cursor += 2
                        continue
                    }
                    end = cursor
                    break
                }
                cursor++
            }
            if (end < 0) break
            result += FormulaOccurrence(result.size, start, end + 1, source.substring(contentStart, end))
            cursor = end + 1
        }
        return result
    }

    fun upsert(source: String, occurrenceIndex: Int?, tex: String): String {
        val clean = tex.trim().take(8_000)
        if (clean.isEmpty()) return source
        val wrapped = "${'$'}$clean${'$'}"
        val target = occurrenceIndex?.let { occurrences(source).getOrNull(it) }
        if (target != null) {
            return separateAdjacent(source.substring(0, target.start) + wrapped + source.substring(target.endExclusive))
        }
        return if (source.isBlank()) wrapped else source.trimEnd() + " " + wrapped
    }

    /**
     * V135.4 — دو فرمولِ پشت‌سرهم (`$a$$b$`) توسط occurrences یک فرمول با محتوای `a$$b`
     * دیده می‌شد چون `$$` نادیده گرفته می‌شود؛ ویرایشگر فرمول هم فرمول جدید را دقیقاً
     * چسبیده به قبلی درج می‌کند. این تابع بین «بستهٔ» یک فرمول و «بازِ» فرمول بعدی
     * یک فاصله می‌گذارد تا هر فرمول کادر و ویرایشگر خودش را داشته باشد.
     */
    fun separateAdjacent(source: String): String {
        if ("$$" !in source) return source
        val out = StringBuilder(source.length + 8)
        var inside = false
        var i = 0
        while (i < source.length) {
            val ch = source[i]
            out.append(ch)
            if (ch == '$' && !isEscaped(source, i)) {
                if (inside) {
                    inside = false
                    if (i + 1 < source.length && source[i + 1] == '$') out.append(' ')
                } else {
                    inside = true
                }
            }
            i++
        }
        return out.toString()
    }

    fun delete(source: String, occurrenceIndex: Int): String {
        val target = occurrences(source).getOrNull(occurrenceIndex) ?: return source
        return (source.substring(0, target.start) + source.substring(target.endExclusive))
            .replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex(" ?\\n ?"), "\n")
            .trim()
    }

    private fun isEscaped(value: String, index: Int): Boolean {
        var slashes = 0
        var cursor = index - 1
        while (cursor >= 0 && value[cursor] == '\\') {
            slashes++
            cursor--
        }
        return slashes % 2 == 1
    }
}
