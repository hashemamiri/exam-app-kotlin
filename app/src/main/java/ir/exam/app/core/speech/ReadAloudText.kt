package ir.exam.app.core.speech

import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.Question
import ir.exam.app.domain.model.TrueFalseQuestion

/**
 * V302 — متنِ قابل‌خواندن برای «بخوان» (حالت ابتدایی).
 * فرمول‌های `$…$` به گفتار سادهٔ فارسی تبدیل می‌شوند، شکل‌ها «شکل» خوانده می‌شوند،
 * و برای چندگزینه‌ای/درست‌نادرست، گزینه‌ها هم خوانده می‌شوند.
 */
object ReadAloudText {
    private val optionLabels = listOf("الف", "ب", "ج", "د", "ه", "و")

    fun forQuestion(q: Question): String {
        val sb = StringBuilder(plain(q.text))
        when (q) {
            is MultipleChoiceQuestion -> q.options.forEachIndexed { i, o ->
                val p = plain(o)
                if (p.isNotBlank()) sb.append(". گزینهٔ ").append(optionLabels.getOrElse(i) { (i + 1).toString() }).append(": ").append(p)
            }
            is TrueFalseQuestion -> sb.append(". درست یا نادرست؟")
            else -> Unit
        }
        return sb.toString().trim()
    }

    fun plain(source: String): String {
        if (source.isBlank()) return ""
        val out = StringBuilder()
        runCatching { RichTextSplitter.split(source) }.getOrNull()?.forEach { seg ->
            when (seg) {
                is RichSegment.Text -> out.append(seg.text)
                is RichSegment.Math -> out.append(' ').append(spokenTex(seg.tex)).append(' ')
                is RichSegment.Figure -> out.append(" شکل ")
            }
        } ?: out.append(source)
        return out.toString()
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("[ \t]+"), " ")
            .replace(Regex("\n{2,}"), "\n")
            .trim()
    }

    /** تبدیل سادهٔ LaTeX به گفتار؛ هدف کامل بودن نیست، قابل‌فهم بودن برای کودک است. */
    fun spokenTex(tex: String): String {
        var t = tex
        repeat(3) {
            t = t.replace(Regex("""\\(?:d|t)?frac\{([^{}]*)\}\{([^{}]*)\}""")) { " ${it.groupValues[1]} تقسیم بر ${it.groupValues[2]} " }
            t = t.replace(Regex("""\\sqrt\{([^{}]*)\}""")) { " جذر ${it.groupValues[1]} " }
            t = t.replace(Regex("""\^\{([^{}]*)\}""")) { " به توان ${it.groupValues[1]} " }
            t = t.replace(Regex("""\^(\w)""")) { " به توان ${it.groupValues[1]} " }
            t = t.replace(Regex("""_\{([^{}]*)\}""")) { " اندیس ${it.groupValues[1]} " }
        }
        val words = listOf(
            "\\times" to " ضرب در ", "\\cdot" to " ضرب در ", "\\div" to " تقسیم بر ", "\\pm" to " مثبت یا منفی ",
            "\\leq" to " کوچکتر یا مساوی ", "\\geq" to " بزرگتر یا مساوی ", "\\neq" to " مخالف ", "\\approx" to " تقریباً ",
            "\\le" to " کوچکتر یا مساوی ", "\\ge" to " بزرگتر یا مساوی ", "\\ne" to " مخالف ",
            "\\pi" to " پی ", "\\infty" to " بی‌نهایت ", "\\degree" to " درجه ", "^\\circ" to " درجه ", "\\%" to " درصد ",
            "\\ldots" to " … ", "\\cdots" to " … ", "\\left" to "", "\\right" to "", "\\," to " ", "\\;" to " ", "\\!" to "", "\\quad" to " ",
            "\\text" to "", "\\mathrm" to "", "\\mathbf" to "", "\\overline" to "", "\\underline" to "",
            "\\Box" to " جای خالی ", "\\square" to " جای خالی ", "\\blacksquare" to " جای خالی ",
            "=" to " مساوی ", "+" to " به‌علاوهٔ ", "-" to " منهای ", "−" to " منهای ", "×" to " ضرب در ", "÷" to " تقسیم بر ",
            "<" to " کوچکتر از ", ">" to " بزرگتر از ", "*" to " ضرب در ", "/" to " تقسیم بر "
        )
        words.forEach { (a, b) -> t = t.replace(a, b) }
        t = t.replace(Regex("""\\[a-zA-Z]+"""), " ")
        t = t.replace(Regex("[{}\\\\]"), " ")
        return t.replace(Regex(" +"), " ").trim()
    }
}
