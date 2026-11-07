package ir.exam.app.ui.code

/**
 * V349 — واژه‌شکن رنگی‌سازی کد (خالص، بدون Compose).
 * همان منطق سایت: توضیح، رشته، عدد، کلیدواژه، نام تابع، نام نوع.
 * هر زبان شکل توضیح خودش را دارد (#، //، --، %، ;، '، !، <!-- -->، (* *)).
 */
object CodeTokens {

    enum class Kind { KEYWORD, COMMENT, STRING, NUMBER, TYPE, FUNCTION }

    data class Span(val start: Int, val end: Int, val kind: Kind)

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '$'

    fun spans(code: String, lang: CodeLanguage): List<Span> {
        val out = mutableListOf<Span>()
        val keywords = HashSet<String>()
        val foldCase = lang.id == "sql" || lang.id == "vb" || lang.id == "pascal" || lang.id == "fortran"
        for (w in lang.keywords) keywords.add(if (foldCase) w.lowercase() else w)
        val n = code.length
        var i = 0
        while (i < n) {
            val c = code[i]
            // توضیح بلوکی
            if (lang.blockOpen.isNotEmpty() && code.startsWith(lang.blockOpen, i)) {
                val close = code.indexOf(lang.blockClose, i + lang.blockOpen.length)
                val end = if (close < 0) n else close + lang.blockClose.length
                out.add(Span(i, end, Kind.COMMENT))
                i = end
                continue
            }
            // توضیح خطی
            if (lang.lineComment.isNotEmpty() && code.startsWith(lang.lineComment, i)) {
                val nl = code.indexOf('\n', i)
                val end = if (nl < 0) n else nl
                out.add(Span(i, end, Kind.COMMENT))
                i = end
                continue
            }
            // رشته
            if (c == '"' || c == '\'' || c == '`') {
                var j = i + 1
                while (j < n) {
                    if (code[j] == '\\') { j += 2; continue }
                    if (code[j] == c) { j++; break }
                    if (code[j] == '\n' && c != '`') break
                    j++
                }
                val end = j.coerceAtMost(n)
                out.add(Span(i, end, Kind.STRING))
                i = end
                continue
            }
            // عدد
            if (c.isDigit() && (i == 0 || !isWordChar(code[i - 1]))) {
                var j = i
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '.')) {
                    if (code[j] == '.' && j + 1 < n && !code[j + 1].isDigit()) break
                    j++
                }
                out.add(Span(i, j, Kind.NUMBER))
                i = j
                continue
            }
            // واژه
            if (isWordChar(c) && (i == 0 || !isWordChar(code[i - 1]))) {
                var j = i
                while (j < n && isWordChar(code[j])) j++
                val word = code.substring(i, j)
                val probe = if (foldCase) word.lowercase() else word
                var k = j
                while (k < n && code[k] == ' ') k++
                when {
                    keywords.contains(probe) -> out.add(Span(i, j, Kind.KEYWORD))
                    k < n && code[k] == '(' -> out.add(Span(i, j, Kind.FUNCTION))
                    word[0].isUpperCase() -> out.add(Span(i, j, Kind.TYPE))
                }
                i = j
                continue
            }
            i++
        }
        return out
    }

    /** شمارهٔ خط‌ها برای ستون کنار کد. */
    fun lineNumbers(code: String): String {
        val count = code.count { it == '\n' } + 1
        return (1..count).joinToString("\n")
    }
}
