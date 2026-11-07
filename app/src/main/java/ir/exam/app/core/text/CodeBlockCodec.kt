package ir.exam.app.core.text

/**
 * V350 — بلوک کد به‌عنوان یک «شیء» داخل متن سؤال و گزینه‌ها.
 *
 * چه هست: پیدا کردن بازهٔ دقیق بلوک‌های ``` در متن خام، و درج/جایگزینی/حذف
 * آن‌ها؛ همان‌طور که FigureCodec برای شکل‌ها این کار را می‌کند. مقدار واقعی
 * فیلد همان قالب ذخیره‌سازی سایت می‌ماند (بدون تغییر پایگاه داده).
 *
 * چه نیست: کد اجرا نمی‌شود و جایی فرستاده نمی‌شود.
 *
 * چرا اینجا و نه ui/code: لایهٔ core به ui وابسته نمی‌شود. هم‌ارزی این
 * قرارداد با CodeText در تست V350 سنجیده می‌شود.
 */
data class CodeOccurrence(
    val index: Int,
    val start: Int,
    val endExclusive: Int,
    val lang: String,
    val code: String
)

object CodeBlockCodec {

    private const val TICKS = "```"

    /** سه بک‌تیک داخل کد با این نویسه جابه‌جا می‌شود تا بلوک نشکند (مثل سایت). */
    private const val ESCAPED_TICKS = "\u02CB\u02CB\u02CB"

    private val FENCE = Regex("```([A-Za-z0-9_+#.\\-]*)[ \\t]*\\r?\\n([\\s\\S]*?)```")

    /** همهٔ بلوک‌های کد، به ترتیب قرار گرفتن در متن. */
    fun occurrences(text: String): List<CodeOccurrence> {
        val out = mutableListOf<CodeOccurrence>()
        for (m in FENCE.findAll(text)) {
            out += CodeOccurrence(
                index = out.size,
                start = m.range.first,
                endExclusive = m.range.last + 1,
                lang = m.groupValues[1],
                code = m.groupValues[2].replace(ESCAPED_TICKS, TICKS).trimEnd()
            )
        }
        return out
    }

    /** قالب ذخیره‌سازی؛ زبان ناشناخته به python برمی‌گردد (مثل سایت). */
    fun token(code: String, lang: String): String {
        val id = lang.trim().lowercase().ifBlank { "python" }
        val body = code.trimEnd().replace(TICKS, ESCAPED_TICKS)
        return TICKS + id + "\n" + body + "\n" + TICKS
    }

    /**
     * درج در آفست مکان‌نما. مثل سایت، بلوک با یک خط خالی از متن جدا می‌شود
     * تا در متن سؤال به‌عنوان یک شیء مستقل دیده شود.
     */
    fun insertAt(text: String, code: String, lang: String, at: Int): String {
        val pos = at.coerceIn(0, text.length)
        val pre = text.substring(0, pos)
        val post = text.substring(pos)
        val lead = when {
            pre.isEmpty() -> ""
            pre.endsWith("\n\n") -> ""
            pre.endsWith("\n") -> "\n"
            else -> "\n\n"
        }
        val tail = if (post.isNotEmpty() && !post.startsWith("\n")) "\n" else ""
        return pre + lead + token(code, lang) + tail + post
    }

    /** جایگزینی یک بلوک موجود، در همان جای متن. */
    fun replace(text: String, occ: CodeOccurrence, code: String, lang: String): String {
        if (occ.start < 0 || occ.endExclusive > text.length || occ.start > occ.endExclusive) return text
        return text.substring(0, occ.start) + token(code, lang) + text.substring(occ.endExclusive)
    }

    /** حذف بلوک؛ خط خالیِ بلافاصله بعدش هم برداشته می‌شود (مثل حذف شکل). */
    fun remove(text: String, occ: CodeOccurrence): String {
        if (occ.start < 0 || occ.endExclusive > text.length || occ.start > occ.endExclusive) return text
        var end = occ.endExclusive
        if (end < text.length && text[end] == '\n') end++
        return text.substring(0, occ.start) + text.substring(end)
    }
}
