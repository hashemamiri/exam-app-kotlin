package ir.exam.app.ui.code

/**
 * V349 — منطق خالص محیط کدنویسی اپ (بدون Compose تا در تست واحد سنجیده شود).
 *
 * چه هست: قالب ذخیره‌سازی بلوک کد، درج علائم و قطعه‌کدها، تورفتگی، و
 * کامل‌کردن خودکار. دقیقاً همان قراردادهای سایت (site/src/code.js).
 *
 * چه نیست: کد اجرا نمی‌شود و جایی فرستاده نمی‌شود. تصحیح دستی است.
 */
object CodeText {

    const val MAX = 20000
    const val CARET = "\$0"

    /** سه بک‌تیک داخل کد با این نویسه جایگزین می‌شود تا بلوک نشکند. */
    private const val ESCAPED_TICKS = "\u02CB\u02CB\u02CB"
    private const val TICKS = "```"

    private val FENCE = Regex("```([A-Za-z0-9_+#.\\-]*)[ \\t]*\\r?\\n([\\s\\S]*?)```")

    data class Block(val lang: String, val code: String)
    data class Part(val isCode: Boolean, val lang: String, val text: String)

    /** پاسخ کد به‌شکل بلوک استاندارد ذخیره می‌شود؛ بدون تغییر پایگاه داده. */
    fun fence(code: String, langId: String?): String {
        val lang = CodeLanguages.byIdOr(langId)
        val body = code.trimEnd().replace(TICKS, ESCAPED_TICKS)
        return TICKS + lang.id + "\n" + body + "\n" + TICKS
    }

    fun parseFence(text: String?): Block? {
        val m = FENCE.find(text.orEmpty()) ?: return null
        val lang = CodeLanguages.byIdOr(m.groupValues[1])
        val code = m.groupValues[2].replace(ESCAPED_TICKS, TICKS).trimEnd()
        return Block(lang.id, code)
    }

    fun hasFence(text: String?): Boolean =
        Regex("```[A-Za-z0-9_+#.\\-]*[ \\t]*\\r?\\n").containsMatchIn(text.orEmpty())

    fun split(text: String?): List<Part> {
        val src = text.orEmpty()
        val parts = mutableListOf<Part>()
        var last = 0
        for (m in FENCE.findAll(src)) {
            if (m.range.first > last) {
                parts.add(Part(false, "", src.substring(last, m.range.first)))
            }
            parts.add(Part(true, CodeLanguages.byIdOr(m.groupValues[1]).id,
                m.groupValues[2].replace(ESCAPED_TICKS, TICKS)))
            last = m.range.last + 1
        }
        if (last < src.length) parts.add(Part(false, "", src.substring(last)))
        return parts
    }

    /** پاسخ «هر دو»: متن بیرون بلوک، سپس بلوک کد. */
    fun compose(text: String, fenceText: String): String {
        val t = text.replace(Regex("`{3,}"), "'''").trim()
        val f = fenceText.trim()
        return when {
            t.isNotEmpty() && f.isNotEmpty() -> t + "\n\n" + f
            t.isNotEmpty() -> t
            else -> f
        }
    }

    fun textPartOf(answer: String?): String =
        split(answer).filter { !it.isCode }.joinToString("") { it.text }.trim()

    /**
     * درج متن در جای نشانگر. اگر قطعه نشان $0 داشته باشد، نشانگر آنجا می‌نشیند.
     * خروجی: متن تازه و جای نشانگر.
     */
    fun insert(text: String, selStart: Int, selEnd: Int, piece: String): Pair<String, Int> {
        val start = selStart.coerceIn(0, text.length)
        val end = selEnd.coerceIn(start, text.length)
        val at = piece.indexOf(CARET)
        val clean = if (at >= 0) piece.removeRange(at, at + CARET.length) else piece
        val next = text.substring(0, start) + clean + text.substring(end)
        val caret = (start + if (at >= 0) at else clean.length).coerceIn(0, next.length)
        return next to caret
    }

    /** تورفتگی خط جاری (فاصله‌های ابتدای خط). */
    fun leadingSpaces(text: String, caret: Int): String {
        val safe = caret.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (safe - 1).coerceAtLeast(0)).let {
            if (safe == 0) -1 else it
        } + 1
        val line = text.substring(lineStart, safe)
        return line.takeWhile { it == ' ' || it == '\t' }
    }

    /**
     * متنِ درجی برای Enter: تورفتگی خط قبل حفظ می‌شود و بعد از { یا : یا ( یک پله
     * بیشتر می‌شود (به اندازهٔ تورفتگی مخصوص همان زبان).
     */
    fun enterInsert(text: String, caret: Int, indent: Int): Pair<String, Int> {
        val safe = caret.coerceIn(0, text.length)
        val lead = leadingSpaces(text, safe)
        val lineStart = if (safe == 0) 0 else text.lastIndexOf('\n', safe - 1) + 1
        val current = text.substring(lineStart, safe)
        val opensBlock = current.trimEnd().lastOrNull()?.let { it in "{:([" } ?: false
        val extra = if (opensBlock) " ".repeat(indent) else ""
        val nextCloses = text.substring(safe).trimStart(' ', '\t').firstOrNull()?.let { it in "})]" } ?: false
        val pad = if (opensBlock && nextCloses) "\n" + lead else ""
        val piece = "\n" + lead + extra + pad
        val next = text.substring(0, safe) + piece + text.substring(safe)
        return next to (safe + 1 + lead.length + extra.length)
    }

    /** بستن خودکار پرانتز/کروشه/آکولاد. */
    fun autoClose(open: Char): Char? = when (open) {
        '(' -> ')'
        '[' -> ']'
        '{' -> '}'
        else -> null
    }

    /** واژهٔ در حال تایپ، پیش از نشانگر (برای کامل‌کردن خودکار). */
    fun wordBefore(text: String, caret: Int): String {
        val safe = caret.coerceIn(0, text.length)
        var i = safe
        while (i > 0 && (text[i - 1].isLetterOrDigit() || text[i - 1] == '_')) i--
        val w = text.substring(i, safe)
        return if (w.isNotEmpty() && (w[0].isLetter() || w[0] == '_')) w else ""
    }

    /**
     * پیشنهادهای کلیدواژه برای واژهٔ نیمه‌نوشته. دست‌کم دو نویسه لازم است.
     * بزرگی/کوچی حروفِ نوشتهٔ کاربر حفظ می‌شود (مثلاً SELECT در SQL).
     */
    fun suggestions(langId: String?, text: String, caret: Int, limit: Int = 8): List<String> {
        val typed = wordBefore(text, caret)
        if (typed.length < 2) return emptyList()
        val lower = typed.lowercase()
        val lang = CodeLanguages.byIdOr(langId)
        val allUpper = typed.length > 1 && typed == typed.uppercase()
        return lang.keywords
            .filter { it.length > 1 && it.lowercase().startsWith(lower) && it.lowercase() != lower }
            .sortedWith(compareBy({ it.length }, { it }))
            .take(limit)
            .map { if (allUpper) it.uppercase() else it }
    }

    /** پذیرش پیشنهاد: واژهٔ نیمه‌نوشته جای خود را به کلیدواژه می‌دهد. */
    fun accept(text: String, caret: Int, word: String): Pair<String, Int> {
        val typed = wordBefore(text, caret)
        if (typed.isEmpty()) return text to caret
        val start = caret - typed.length
        val next = text.substring(0, start) + word + text.substring(caret)
        return next to (start + word.length)
    }

    /** تورفتگی یک خط با Tab؛ Shift+Tab یک پله برمی‌دارد. */
    fun tab(text: String, caret: Int, indent: Int, unindent: Boolean): Pair<String, Int> {
        val safe = caret.coerceIn(0, text.length)
        if (!unindent) {
            val pad = " ".repeat(indent)
            return (text.substring(0, safe) + pad + text.substring(safe)) to (safe + indent)
        }
        val lineStart = if (safe == 0) 0 else text.lastIndexOf('\n', safe - 1) + 1
        val line = text.substring(lineStart)
        val remove = line.takeWhile { it == ' ' }.length.coerceAtMost(indent)
        if (remove == 0) return text to safe
        val next = text.removeRange(lineStart, lineStart + remove)
        return next to (safe - remove).coerceAtLeast(lineStart)
    }
}
