package ir.exam.app.core.math

/** رندر متنی Native برای زیرمجموعهٔ پرکاربرد TeX؛ بدون WebView و بدون اجرای HTML. */
object NativeMathFormatter {
    private val commandMap = linkedMapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\theta" to "θ", "\\lambda" to "λ", "\\mu" to "μ", "\\pi" to "π",
        "\\rho" to "ρ", "\\sigma" to "σ", "\\phi" to "φ", "\\omega" to "ω",
        "\\Delta" to "Δ", "\\Sigma" to "Σ", "\\Omega" to "Ω",
        "\\times" to "×", "\\div" to "÷", "\\pm" to "±", "\\mp" to "∓",
        "\\leq" to "≤", "\\geq" to "≥", "\\neq" to "≠", "\\approx" to "≈",
        "\\infty" to "∞", "\\sum" to "∑", "\\prod" to "∏", "\\int" to "∫",
        "\\partial" to "∂", "\\nabla" to "∇", "\\rightarrow" to "→",
        "\\leftarrow" to "←", "\\Rightarrow" to "⇒", "\\in" to "∈",
        "\\notin" to "∉", "\\subset" to "⊂", "\\cup" to "∪", "\\cap" to "∩",
        "\\degree" to "°", "\\cdot" to "·"
    )
    private val superscript = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ'
    )
    private val subscript = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'x' to 'ₓ'
    )

    data class Segment(val text: String, val math: Boolean)

    fun segments(source: String): List<Segment> {
        if (source.isEmpty()) return emptyList()
        val result = mutableListOf<Segment>()
        var start = 0
        var math = false
        var index = 0
        while (index < source.length) {
            if (source[index] == '$' && (index == 0 || source[index - 1] != '\\')) {
                if (index > start) result += Segment(source.substring(start, index), math)
                math = !math
                start = index + 1
            }
            index++
        }
        if (start < source.length) result += Segment(source.substring(start), math)
        if (math) {
            // دلار بازِ بدون بسته به‌عنوان متن عادی نگه داشته می‌شود.
            val last = result.lastOrNull()
            if (last != null && last.math) {
                result[result.lastIndex] = Segment("$" + last.text, false)
            } else result += Segment("$", false)
        }
        return result
    }

    fun renderText(source: String): String = segments(source).joinToString("") { segment ->
        if (segment.math) renderTex(segment.text) else segment.text.replace("\\$", "$")
    }

    fun renderTex(tex: String): String {
        require(tex.length <= 8_000) { "فرمول بیش از حد بلند است." }
        var value = tex.trim()
        value = renderMatrices(value)
        value = replaceStructured(value, "\\frac") { args ->
            val numerator = renderTex(args[0])
            val denominator = renderTex(args[1])
            "($numerator)⁄($denominator)"
        }
        value = replaceStructured(value, "\\sqrt") { args -> "√(${renderTex(args[0])})" }
        commandMap.forEach { (command, glyph) -> value = value.replace(command, glyph) }
        value = replaceScripts(value, '^', superscript)
        value = replaceScripts(value, '_', subscript)
        value = value.replace("\\left", "").replace("\\right", "")
        value = value.replace(Regex("\\\\(?:mathrm|text|mathbf|bold)\\{([^{}]*)}"), "$1")
        value = value.replace("{", "").replace("}", "")
        value = value.replace(Regex("\\\\[A-Za-z]+")) { match -> match.value.removePrefix("\\") }
        return value.trim()
    }

    fun quickToTex(raw: String): String {
        var value = raw.trim()
        require(value.length <= 2_000) { "ورودی فرمول بیش از حد بلند است." }
        value = value.replace(Regex("sqrt\\(([^()]*)\\)", RegexOption.IGNORE_CASE), "\\\\sqrt{$1}")
        value = value.replace("<=", "\\leq ").replace(">=", "\\geq ").replace("!=", "\\neq ")
        value = value.replace("*", "\\times ")
        return value.replace(Regex("\\s+"), " ").trim()
    }

    fun isBalanced(tex: String): Boolean {
        var depth = 0
        tex.forEach { char ->
            if (char == '{') depth++
            if (char == '}') depth--
            if (depth < 0) return false
        }
        return depth == 0
    }

    private fun replaceStructured(
        input: String,
        command: String,
        transform: (List<String>) -> String
    ): String {
        var value = input
        val argCount = if (command == "\\frac") 2 else 1
        var searchFrom = 0
        while (true) {
            val at = value.indexOf(command, searchFrom)
            if (at < 0) break
            var cursor = at + command.length
            val args = mutableListOf<String>()
            var valid = true
            repeat(argCount) {
                while (cursor < value.length && value[cursor].isWhitespace()) cursor++
                val parsed = readGroup(value, cursor)
                if (parsed == null) valid = false
                else {
                    args += parsed.first
                    cursor = parsed.second
                }
            }
            if (!valid || args.size != argCount) {
                searchFrom = at + command.length
                continue
            }
            value = value.substring(0, at) + transform(args) + value.substring(cursor)
            searchFrom = at
        }
        return value
    }

    private fun readGroup(value: String, start: Int): Pair<String, Int>? {
        if (start >= value.length || value[start] != '{') return null
        var depth = 0
        for (index in start until value.length) {
            when (value[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return value.substring(start + 1, index) to (index + 1)
                }
            }
        }
        return null
    }

    private fun replaceScripts(input: String, marker: Char, table: Map<Char, Char>): String {
        val out = StringBuilder()
        var index = 0
        while (index < input.length) {
            if (input[index] != marker || index + 1 >= input.length) {
                out.append(input[index++])
                continue
            }
            val next = index + 1
            val group = if (input[next] == '{') readGroup(input, next) else null
            val raw = group?.first ?: input[next].toString()
            val converted = raw.mapNotNull(table::get).joinToString("")
            if (converted.length == raw.length) out.append(converted)
            else out.append(if (marker == '^') "^($raw)" else "_($raw)")
            index = group?.second ?: (next + 1)
        }
        return out.toString()
    }

    private fun renderMatrices(input: String): String {
        val regex = Regex(
            "\\\\begin\\{(?:matrix|bmatrix|pmatrix)\\}([\\s\\S]*?)\\\\end\\{(?:matrix|bmatrix|pmatrix)\\}"
        )
        return regex.replace(input) { match ->
            val rows = match.groupValues[1].split("\\\\").map { row ->
                row.split('&').joinToString("  ") { renderTex(it) }
            }
            "[${rows.joinToString("; ")}]"
        }
    }
}
