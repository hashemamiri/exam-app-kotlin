package ir.exam.app.domain.bank

/**
 * V361 — تبدیل فرمولهای MathJax به توکن ویرایشگر فرمول.
 *
 * شاردهای بانک مرجع فرمولها را به شکل `\(...\)`، `\[...\]` و `$$...$$`
 * نگه میدارند؛ ویرایشگر فرمول اپ و سایت فقط توکن `$...$` را میشناسد و
 * آن را به نماد SVG/تراشه تبدیل میکند. این مبدل خالص کاتلین است (بدون
 * هیچ وابستگی اندرویدی) تا هم در اپ و هم در تست واحد اجرا شود.
 *
 * تبدیل دوبارهبیضرر است: متنی که از قبل `$...$` داشته باشد همان میماند.
 * بین دو توکن چسبیده (`$a$$b$`) یک فاصله گذاشته میشود تا هر فرمول
 * ویرایشگر جداگانهٔ خودش را داشته باشد (همان قرارداد FormulaTextCodec).
 */
object ReferenceBankFormula {

    fun mathJaxToTokens(raw: String): String {
        if (raw.isBlank()) return raw
        val out = StringBuilder(raw.length + 16)
        var i = 0
        while (i < raw.length) {
            var open: String? = null
            var close: String? = null
            when {
                raw.startsWith("\\(", i) -> { open = "\\("; close = "\\)" }
                raw.startsWith("\\[", i) -> { open = "\\["; close = "\\]" }
                raw.startsWith("$$", i) -> { open = "$$"; close = "$$" }
            }
            if (open != null && close != null) {
                val end = raw.indexOf(close, i + open.length)
                if (end >= 0) {
                    val body = raw.substring(i + open.length, end)
                    val clean = body
                        .split(' ', '\n', '\t', '\r')
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    if (clean.isNotBlank()) out.append('$').append(clean).append('$')
                    i = end + close.length
                    continue
                }
            }
            out.append(raw[i])
            i++
        }
        return separateAdjacent(out.toString())
    }

    /** جلوگیری از چسبیدن دو فرمول به هم، مثل `$a$$b$` → `$a$ $b$`. */
    fun separateAdjacent(value: String): String {
        if ("$$" !in value) return value
        val out = StringBuilder(value.length + 8)
        var inside = false
        for (i in value.indices) {
            val ch = value[i]
            out.append(ch)
            if (ch == '$') {
                if (inside) {
                    inside = false
                    if (i + 1 < value.length && value[i + 1] == '$') out.append(' ')
                } else {
                    inside = true
                }
            }
        }
        return out.toString()
    }

    fun hasMathJax(raw: String): Boolean =
        raw.contains("\\(") || raw.contains("\\[") || raw.contains("$$")

    /** شمار فرمولهای تبدیلشده (برای گزارش و نگهبان). */
    fun tokenCount(raw: String): Int {
        val tokens = mathJaxToTokens(raw)
        var n = 0
        var i = 0
        while (i < tokens.length) {
            if (tokens[i] != '$') { i++; continue }
            val end = tokens.indexOf('$', i + 1)
            if (end < 0) break
            if (end > i + 1) n++
            i = end + 1
        }
        return n
    }
}

/** V361 — تبدیل همهٔ فیلدهای متنی یک ردیف سؤال به توکن ویرایشگر فرمول. */
fun ReferenceBankQuestion.withFormulaTokens(): ReferenceBankQuestion = copy(
    text = ReferenceBankFormula.mathJaxToTokens(text),
    qHtml = ReferenceBankFormula.mathJaxToTokens(qHtml),
    answerHtml = ReferenceBankFormula.mathJaxToTokens(answerHtml),
    options = options.map { ReferenceBankFormula.mathJaxToTokens(it) },
    answerText = ReferenceBankFormula.mathJaxToTokens(answerText)
)

/** V361 — فهرست کوچک هم متن سؤال را توکندار نشان میدهد. */
fun ReferenceBankRow.withFormulaTokens(): ReferenceBankRow = copy(
    text = ReferenceBankFormula.mathJaxToTokens(text)
)
