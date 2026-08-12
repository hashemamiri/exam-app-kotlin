package ir.exam.app.core.math

/** مبدل کامل تایپ طبیعی مرجع، بدون JavaScript و با اصلاح تبدیل‌های شیمی. */
object NativeNaturalMathConverter {
    private val words = mapOf(
        "رادیکال" to "\\sqrt", "ریشه" to "\\sqrt", "جذر" to "\\sqrt",
        "پی" to "\\pi", "آلفا" to "\\alpha", "بتا" to "\\beta", "گاما" to "\\gamma",
        "تتا" to "\\theta", "لاندا" to "\\lambda", "دلتا" to "\\Delta",
        "سیگما" to "\\sum", "مجموع" to "\\sum", "انتگرال" to "\\int", "حد" to "\\lim",
        "بینهایت" to "\\infty", "بی‌نهایت" to "\\infty", "زاویه" to "\\angle",
        "درجه" to "\\degree", "سینوس" to "\\sin", "کسینوس" to "\\cos",
        "تانژانت" to "\\tan", "مماس" to "\\tan", "کتانژانت" to "\\cot", "لگاریتم" to "\\log",
        "sqrt" to "\\sqrt", "pi" to "\\pi", "alpha" to "\\alpha", "beta" to "\\beta",
        "gamma" to "\\gamma", "theta" to "\\theta", "lambda" to "\\lambda",
        "delta" to "\\Delta", "sigma" to "\\sum", "sum" to "\\sum", "int" to "\\int",
        "lim" to "\\lim", "inf" to "\\infty", "infty" to "\\infty", "deg" to "\\degree",
        "sin" to "\\sin", "cos" to "\\cos", "tan" to "\\tan", "cot" to "\\cot",
        "sec" to "\\sec", "csc" to "\\csc", "log" to "\\log", "ln" to "\\ln",
        "vec" to "\\vec", "angle" to "\\angle", "det" to "\\det"
    )

    private val bigCommands = setOf("\\sum", "\\int", "\\lim")
    private val argumentCommands = setOf("\\sqrt", "\\vec")

    fun toTex(raw: String, chemistry: Boolean = false): String {
        var source = normalizeDigits(raw).trim().take(2_000)
        if (source.isEmpty()) return ""
        if (chemistry) source = normalizeChemistry(source)
        // برخلاف باگ مرجع، فلش تعادل مستقیماً فرمان معتبر TeX می‌شود.
        source = source.replace("⇌", " \\rightleftharpoons ")
        return Parser(source, chemistry).parse().replace(Regex("\\s+"), " ").trim()
    }

    fun normalizeChemistry(raw: String): String {
        var value = normalizeDigits(raw)
        // یون‌های رایج با بار جدا: SO4 2- → SO_{4}^{2-}
        value = value.replace(
            Regex("\\b(SO4|CO3|PO4|NO3|NH4|HCO3)\\s*(\\d+)([+-])")
        ) { match ->
            match.groupValues[1].replace(Regex("([A-Za-z)])(\\d+)"), "$1_{$2}") +
                "^{${match.groupValues[2]}${match.groupValues[3]}}"
        }
        // بارهای صریح مانند Fe3+ یا Cl- پیش از تبدیل اندیس محافظت می‌شوند.
        value = value.replace(Regex("([A-Za-z)])(\\d+)([+-])"), "$1^{$2$3}")
        value = value.replace(Regex("([A-Za-z)])([+-])"), "$1^{$2}")
        value = value.replace(Regex("\\b([A-Z][A-Za-z0-9()]+)\\b")) { match ->
            val token = match.value
            if (!token.any(Char::isDigit) || "^{" in token) token
            else token.replace(Regex("([A-Za-z)])(\\d+)"), "$1_{$2}")
        }
        return value
    }

    private data class Token(
        val text: String,
        val group: String? = null,
        val space: Boolean = false,
        val power: Boolean = false,
        val lower: Boolean = false,
        val division: Boolean = false
    )

    private class Parser(private val source: String, private val chemistry: Boolean) {
        private var index = 0

        fun parse(): String = assemble(parseTokens(null))

        private fun parseTokens(stop: Char?): MutableList<Token> {
            val tokens = mutableListOf<Token>()
            while (index < source.length) {
                val char = source[index]
                if (stop != null && char == stop) break
                when {
                    char.isWhitespace() -> {
                        index++
                        if (tokens.isNotEmpty()) tokens += Token(" ", space = true)
                    }
                    char == '(' -> {
                        index++
                        val inner = assemble(parseTokens(')'))
                        if (index < source.length && source[index] == ')') index++
                        tokens += Token("\\left( $inner \\right)", group = inner)
                    }
                    char == '{' -> {
                        index++
                        val inner = assemble(parseTokens('}'))
                        if (index < source.length && source[index] == '}') index++
                        tokens += Token("{$inner}", group = inner)
                    }
                    char == '\\' -> {
                        val start = index++
                        while (index < source.length && source[index].isLetter()) index++
                        if (index == start + 1 && index < source.length) index++
                        tokens += Token(source.substring(start, index))
                    }
                    char == ')' || char == ']' || char == '}' -> break
                    isNumberChar(char) -> {
                        val start = index
                        while (index < source.length && isNumberChar(source[index])) index++
                        tokens += Token(source.substring(start, index))
                    }
                    isWordChar(char) -> tokens += readWordToken()
                    else -> readOperator(tokens)
                }
            }
            return tokens
        }

        private fun readWordToken(): Token {
            val start = index
            while (index < source.length && isWordChar(source[index])) index++
            val word = source.substring(start, index)
            val command = words[word] ?: words[word.lowercase()]
            if (command != null) {
                if (command in argumentCommands) {
                    while (index < source.length && source[index].isWhitespace()) index++
                    val argument = when {
                        index < source.length && source[index] == '(' -> {
                            index++
                            val inner = assemble(parseTokens(')'))
                            if (index < source.length && source[index] == ')') index++
                            inner
                        }
                        index < source.length && isNumberChar(source[index]) -> {
                            val numberStart = index
                            while (index < source.length && isNumberChar(source[index])) index++
                            source.substring(numberStart, index)
                        }
                        index < source.length && isWordChar(source[index]) -> {
                            val wordStart = index
                            while (index < source.length && isWordChar(source[index])) index++
                            source.substring(wordStart, index)
                        }
                        else -> ""
                    }
                    return Token("$command{$argument}")
                }
                return Token(command + if (command in bigCommands) " " else " ")
            }
            val chemicalWord = chemistry && word.firstOrNull()?.isUpperCase() == true && word.all {
                it.isLetterOrDigit() || it in "(){}^_+-"
            }
            return Token(if (word.length > 1 && !chemicalWord) "\\text{$word}" else word)
        }

        private fun readOperator(tokens: MutableList<Token>) {
            val pair = source.substring(index, (index + 2).coerceAtMost(source.length))
            val mapped = when (pair) {
                ">=" -> "\\geq "
                "<=" -> "\\leq "
                "!=" -> "\\neq "
                "+-" -> "\\pm "
                "->" -> "\\to "
                "<-" -> "\\leftarrow "
                "=>" -> "\\Rightarrow "
                "~=" -> "\\approx "
                "**" -> "^"
                else -> null
            }
            if (mapped != null) {
                index += 2
                tokens += when (mapped) {
                    "^" -> Token("^", power = true)
                    else -> Token(mapped)
                }
                return
            }
            val char = source[index++]
            tokens += when (char) {
                '/' -> Token("/", division = true)
                '^' -> Token("^", power = true)
                '_' -> Token("_", lower = true)
                '*', '×' -> Token("\\times ")
                '÷' -> Token("\\div ")
                else -> Token(char.toString())
            }
        }

        private fun assemble(input: List<Token>): String {
            val tokens = input.toMutableList()
            for (at in tokens.indices.reversed()) {
                val marker = tokens.getOrNull(at) ?: continue
                if (!marker.power && !marker.lower) continue
                var leftIndex = at - 1
                while (leftIndex >= 0 && tokens[leftIndex].space) leftIndex--
                var rightIndex = at + 1
                while (rightIndex < tokens.size && tokens[rightIndex].space) rightIndex++
                val left = tokens.getOrNull(leftIndex) ?: Token("")
                val right = tokens.getOrNull(rightIndex) ?: Token("")
                val operator = if (marker.power) '^' else '_'
                val rightValue = right.group ?: right.text
                val previousMarker = tokens.getOrNull(leftIndex - 1)
                if (previousMarker != null && (previousMarker.power || previousMarker.lower) && previousMarker.power != marker.power) {
                    val count = (rightIndex - at + 1).coerceAtMost(tokens.size - at)
                    repeat(count) { tokens.removeAt(at) }
                    tokens.add(at, Token("$operator{$rightValue}"))
                    continue
                }
                val replacement = Token(left.text + operator + "{$rightValue}")
                val from = if (leftIndex >= 0) leftIndex else at
                val count = (rightIndex - from + 1).coerceAtMost(tokens.size - from)
                repeat(count) { tokens.removeAt(from) }
                tokens.add(from, replacement)
            }
            var at = 0
            while (at < tokens.size) {
                if (!tokens[at].division) {
                    at++
                    continue
                }
                var leftIndex = at - 1
                while (leftIndex >= 0 && tokens[leftIndex].space) leftIndex--
                var rightIndex = at + 1
                while (rightIndex < tokens.size && tokens[rightIndex].space) rightIndex++
                val left = tokens.getOrNull(leftIndex) ?: Token("")
                val right = tokens.getOrNull(rightIndex) ?: Token("")
                val replacement = Token("\\frac{${left.group ?: left.text}}{${right.group ?: right.text}}")
                val from = if (leftIndex >= 0) leftIndex else at
                val count = (rightIndex - from + 1).coerceAtMost(tokens.size - from)
                repeat(count) { tokens.removeAt(from) }
                tokens.add(from, replacement)
                at = from + 1
            }
            return tokens.joinToString("") { it.text }
        }
    }

    private fun normalizeDigits(value: String): String = value.map { char ->
        when (char) {
            in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
            in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
            else -> char
        }
    }.joinToString("")

    private fun isNumberChar(char: Char): Boolean = char.isDigit() || char == '.'
    private fun isWordChar(char: Char): Boolean = char.isLetter() || char == '\u200c'
}
