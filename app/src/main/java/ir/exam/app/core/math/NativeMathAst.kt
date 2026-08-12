package ir.exam.app.core.math

data class MathSourceRange(val start: Int, val endExclusive: Int) {
    init {
        require(start >= 0 && endExclusive >= start)
    }
}

sealed interface MathNode {
    data class Sequence(val children: List<MathNode>) : MathNode
    data class Symbol(
        val value: String,
        val bold: Boolean = false,
        val sourceStart: Int = -1,
        val sourceEnd: Int = -1,
        val editable: Boolean = true
    ) : MathNode
    data class Fraction(val top: MathNode, val bottom: MathNode) : MathNode
    data class Radical(val body: MathNode, val index: MathNode? = null) : MathNode
    data class Script(val base: MathNode, val upper: MathNode?, val lower: MathNode?) : MathNode
    data class Matrix(val rows: List<List<MathNode>>, val delimiter: Char = '[') : MathNode
    data class Accent(val body: MathNode, val mark: String) : MathNode
    data class Delimited(val open: String, val body: MathNode, val close: String) : MathNode
    data object LineBreak : MathNode
}

/**
 * Parser ساختاری و امن فرمول‌های آموزشی.
 *
 * علاوه بر AST، هر مقدار قابل‌ویرایش بازهٔ دقیق خود را در متن داخلی نگه می‌دارد. این
 * بازه‌ها برای جعبه‌های لمسی SVG استفاده می‌شوند و باعث می‌شوند انتخاب کتابخانه دقیقاً
 * خانهٔ فعال را جایگزین کند. دستور ناشناخته هرگز به‌صورت نام خام نمایش داده نمی‌شود.
 */
object NativeMathParser {
    private val commandSymbols = mapOf(
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ",
        "epsilon" to "ε", "varepsilon" to "ϵ", "zeta" to "ζ", "eta" to "η",
        "theta" to "θ", "vartheta" to "ϑ", "iota" to "ι", "kappa" to "κ",
        "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ", "pi" to "π",
        "rho" to "ρ", "sigma" to "σ", "tau" to "τ", "upsilon" to "υ",
        "phi" to "φ", "varphi" to "ϕ", "chi" to "χ", "psi" to "ψ", "omega" to "ω",
        "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ",
        "Xi" to "Ξ", "Pi" to "Π", "Sigma" to "Σ", "Upsilon" to "Υ",
        "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω",
        "times" to "×", "div" to "÷", "cdot" to "·", "ast" to "∗",
        "pm" to "±", "mp" to "∓", "le" to "≤", "leq" to "≤", "ge" to "≥",
        "geq" to "≥", "ne" to "≠", "neq" to "≠", "ll" to "≪", "gg" to "≫",
        "approx" to "≈", "approxeq" to "≊", "equiv" to "≡", "sim" to "∼",
        "simeq" to "≃", "cong" to "≅", "fallingdotseq" to "≒", "propto" to "∝",
        "infty" to "∞", "sum" to "∑", "prod" to "∏", "coprod" to "∐",
        "int" to "∫", "iint" to "∬", "iiint" to "∭", "oint" to "∮",
        "bigcup" to "⋃", "bigcap" to "⋂", "partial" to "∂", "nabla" to "∇",
        "rightarrow" to "→", "to" to "→", "longrightarrow" to "→",
        "leftarrow" to "←", "gets" to "←", "longleftarrow" to "←",
        "leftrightarrow" to "↔", "longleftrightarrow" to "↔", "leftarrowshort" to "↔",
        "uparrow" to "↑", "downarrow" to "↓", "updownarrow" to "↕",
        "Rightarrow" to "⇒", "Longrightarrow" to "⇒", "Leftarrow" to "⇐",
        "Longleftarrow" to "⇐", "Leftrightarrow" to "⇔", "Longleftrightarrow" to "⇔",
        "mapsto" to "↦", "mapsfrom" to "↤", "rightleftharpoons" to "⇌",
        "hookleftarrow" to "↩", "hookrightarrow" to "↪", "leftharpoonup" to "↼",
        "rightharpoonup" to "⇀", "leftharpoondown" to "↽", "rightharpoondown" to "⇁",
        "in" to "∈", "notin" to "∉", "ni" to "∋", "subset" to "⊂",
        "subseteq" to "⊆", "supset" to "⊃", "supseteq" to "⊇",
        "cup" to "∪", "cap" to "∩", "emptyset" to "∅", "varnothing" to "∅",
        "forall" to "∀", "exists" to "∃", "therefore" to "∴", "because" to "∵",
        "degree" to "°", "angle" to "∠", "perp" to "⊥", "parallel" to "∥",
        "circ" to "∘", "hbar" to "ℏ", "prime" to "′", "ldots" to "…",
        "cdots" to "⋯", "vdots" to "⋮", "ddots" to "⋱", "langle" to "⟨",
        "rangle" to "⟩", "lfloor" to "⌊", "rfloor" to "⌋", "lceil" to "⌈",
        "rceil" to "⌉", "lvert" to "|", "rvert" to "|", "vert" to "|"
    )

    private val namedFunctions = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc", "log", "ln", "exp",
        "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh",
        "det", "gcd", "deg", "dim", "mod", "lim", "max", "min"
    )

    private val structuralCommands = setOf(
        "frac", "dfrac", "tfrac", "sqrt", "mathbf", "bold", "boldsymbol",
        "mathrm", "text", "operatorname", "mathbb", "mathcal", "hat", "bar",
        "overline", "vec", "dot", "left", "right", "begin", "end"
    )

    val supportedCommands: Set<String> =
        (commandSymbols.keys + namedFunctions + structuralCommands).toSet()

    fun parse(tex: String): MathNode {
        require(tex.length <= 8000) { "فرمول بیش از حد بلند است." }
        return Parser(tex, 0).parseSequence()
    }

    /** بازه‌های قابل لمس/ویرایش، مرتب بر اساس محل واقعی در TeX داخلی. */
    fun editableRanges(tex: String): List<MathSourceRange> {
        val ranges = buildList { collectEditable(parse(tex), this) }
            .distinct()
            .sortedWith(compareBy<MathSourceRange> { it.start }.thenBy { it.endExclusive })
        if (ranges.size < 2) return ranges
        val merged = mutableListOf<MathSourceRange>()
        ranges.forEach { range ->
            val previous = merged.lastOrNull()
            val combinedIsPlainValue = previous != null &&
                previous.endExclusive == range.start &&
                tex.substring(previous.start, range.endExclusive).codePoints()
                    .allMatch { Character.isLetterOrDigit(it) }
            if (previous != null && combinedIsPlainValue) {
                merged[merged.lastIndex] = MathSourceRange(previous.start, range.endExclusive)
            } else {
                merged += range
            }
        }
        return merged
    }

    /** دستورهای واقعی ناشناخته را بدون اشتباه گرفتن `\\` سطر تازه گزارش می‌کند. */
    fun unsupportedCommands(tex: String): Set<String> {
        val result = linkedSetOf<String>()
        var index = 0
        while (index < tex.length) {
            if (tex[index] != '\\') {
                index++
                continue
            }
            if (index + 1 < tex.length && tex[index + 1] == '\\') {
                index += 2
                continue
            }
            index++
            val start = index
            while (index < tex.length && tex[index].isLetter()) index++
            if (index > start) {
                val name = tex.substring(start, index)
                if (name !in supportedCommands) result += name
            } else if (index < tex.length) {
                index++
            }
        }
        return result
    }

    private fun collectEditable(node: MathNode, output: MutableList<MathSourceRange>) {
        when (node) {
            is MathNode.Symbol -> if (
                node.editable && node.sourceStart >= 0 && node.sourceEnd >= node.sourceStart
            ) output += MathSourceRange(node.sourceStart, node.sourceEnd)
            is MathNode.Sequence -> node.children.forEach { collectEditable(it, output) }
            is MathNode.Fraction -> {
                collectEditable(node.top, output)
                collectEditable(node.bottom, output)
            }
            is MathNode.Radical -> {
                node.index?.let { collectEditable(it, output) }
                collectEditable(node.body, output)
            }
            is MathNode.Script -> {
                collectEditable(node.base, output)
                node.upper?.let { collectEditable(it, output) }
                node.lower?.let { collectEditable(it, output) }
            }
            is MathNode.Matrix -> node.rows.flatten().forEach { collectEditable(it, output) }
            is MathNode.Accent -> collectEditable(node.body, output)
            is MathNode.Delimited -> collectEditable(node.body, output)
            MathNode.LineBreak -> Unit
        }
    }

    private class Parser(
        private val source: String,
        private val sourceOffset: Int
    ) {
        private var index = 0

        fun parseSequence(stop: Char? = null): MathNode {
            val output = mutableListOf<MathNode>()
            val emptyPosition = global(index)
            while (index < source.length && (stop == null || source[index] != stop)) {
                var node = atom()
                var upper: MathNode? = null
                var lower: MathNode? = null
                while (index < source.length && (source[index] == '^' || source[index] == '_')) {
                    val marker = source[index++]
                    val script = groupOrAtom()
                    if (marker == '^') upper = script else lower = script
                }
                if (upper != null || lower != null) node = MathNode.Script(node, upper, lower)
                output += node
            }
            if (stop != null && index < source.length && source[index] == stop) index++
            return when (output.size) {
                0 -> MathNode.Symbol(
                    value = "",
                    sourceStart = emptyPosition,
                    sourceEnd = emptyPosition,
                    editable = true
                )
                1 -> output.first()
                else -> MathNode.Sequence(output)
            }
        }

        private fun atom(): MathNode {
            if (index >= source.length) {
                val position = global(index)
                return MathNode.Symbol("", sourceStart = position, sourceEnd = position)
            }
            val start = index
            val codePoint = source.codePointAt(index)
            val charCount = Character.charCount(codePoint)
            index += charCount
            val value = String(Character.toChars(codePoint))
            return when (value) {
                "{" -> parseSequence('}')
                "\\" -> command(start)
                "~" -> sourceSymbol(" ", start, index, editable = false)
                else -> sourceSymbol(
                    value,
                    start,
                    index,
                    editable = !Character.isWhitespace(codePoint)
                )
            }
        }

        private fun groupOrAtom(): MathNode {
            skipWhitespace()
            return if (index < source.length && source[index] == '{') {
                index++
                parseSequence('}')
            } else {
                atom()
            }
        }

        private fun command(slashStart: Int): MathNode {
            if (index >= source.length) {
                return sourceSymbol("□", slashStart, index)
            }
            val escaped = source[index]
            if (!escaped.isLetter()) {
                index++
                return when (escaped) {
                    '\\' -> MathNode.LineBreak
                    ' ' -> sourceSymbol(" ", slashStart, index, editable = false)
                    '{', '}', '%', '$', '#', '&', '_', '|' ->
                        sourceSymbol(escaped.toString(), slashStart, index)
                    else -> sourceSymbol("□", slashStart, index)
                }
            }

            val nameStart = index
            while (index < source.length && source[index].isLetter()) index++
            val name = source.substring(nameStart, index)
            return when (name) {
                "frac", "dfrac", "tfrac" -> MathNode.Fraction(groupOrAtom(), groupOrAtom())
                "sqrt" -> parseRadical()
                "mathbf", "bold", "boldsymbol" -> {
                    val body = groupOrAtom()
                    sourceSymbol(flat(body), slashStart, index, bold = true)
                }
                "mathrm", "text", "operatorname" -> {
                    val body = groupOrAtom()
                    sourceSymbol(flat(body), slashStart, index)
                }
                "mathbb" -> {
                    val body = groupOrAtom()
                    sourceSymbol(blackboard(flat(body)), slashStart, index)
                }
                "mathcal" -> {
                    val body = groupOrAtom()
                    sourceSymbol(calligraphic(flat(body)), slashStart, index)
                }
                "hat" -> MathNode.Accent(groupOrAtom(), "hat")
                "bar", "overline" -> MathNode.Accent(groupOrAtom(), "bar")
                "vec" -> MathNode.Accent(groupOrAtom(), "vec")
                "dot" -> MathNode.Accent(groupOrAtom(), "dot")
                "left" -> parseDelimited()
                "right" -> {
                    readDelimiter()
                    MathNode.Symbol("", editable = false)
                }
                "begin" -> parseEnvironment(slashStart)
                "end" -> {
                    groupOrAtom()
                    MathNode.Symbol("", editable = false)
                }
                in namedFunctions -> sourceSymbol(name, slashStart, index)
                else -> sourceSymbol(commandSymbols[name] ?: "□", slashStart, index)
            }
        }

        private fun parseRadical(): MathNode {
            skipWhitespace()
            var rootIndex: MathNode? = null
            if (index < source.length && source[index] == '[') {
                index++
                rootIndex = parseSequence(']')
            }
            return MathNode.Radical(groupOrAtom(), rootIndex)
        }

        private fun parseDelimited(): MathNode {
            val open = readDelimiter()
            val bodyStart = index
            var cursor = index
            var depth = 1
            while (cursor < source.length) {
                if (source[cursor] != '\\') {
                    cursor++
                    continue
                }
                if (cursor + 1 < source.length && source[cursor + 1] == '\\') {
                    cursor += 2
                    continue
                }
                val slash = cursor++
                val commandStart = cursor
                while (cursor < source.length && source[cursor].isLetter()) cursor++
                val command = source.substring(commandStart, cursor)
                if (command == "left") depth++
                if (command == "right") {
                    depth--
                    if (depth == 0) {
                        val body = Parser(
                            source.substring(bodyStart, slash),
                            global(bodyStart)
                        ).parseSequence()
                        index = cursor
                        val close = readDelimiter()
                        return MathNode.Delimited(open, body, close)
                    }
                }
            }
            index = source.length
            val body = Parser(source.substring(bodyStart), global(bodyStart)).parseSequence()
            return MathNode.Delimited(open, body, matchingDelimiter(open))
        }

        private fun readDelimiter(): String {
            skipWhitespace()
            if (index >= source.length) return ""
            var result: String
            if (source[index] == '\\') {
                index++
                val start = index
                while (index < source.length && source[index].isLetter()) index++
                if (index == start && index < source.length) {
                    result = source[index++].toString()
                } else {
                    val name = source.substring(start, index)
                    result = commandSymbols[name] ?: when (name) {
                        "lbrace" -> "{"
                        "rbrace" -> "}"
                        else -> ""
                    }
                }
            } else {
                val codePoint = source.codePointAt(index)
                index += Character.charCount(codePoint)
                result = String(Character.toChars(codePoint))
            }
            if (result == ".") result = ""
            return result
        }

        private fun parseEnvironment(commandStart: Int): MathNode {
            val environment = flat(groupOrAtom())
            val end = "\\end{$environment}"
            val endAt = source.indexOf(end, index)
            if (endAt < 0) return sourceSymbol("□", commandStart, index)
            val bodyStart = index
            val body = source.substring(bodyStart, endAt)
            index = endAt + end.length
            if (environment in setOf("matrix", "bmatrix", "pmatrix", "vmatrix", "cases", "aligned", "align")) {
                val rows = splitSlices(body, "\\\\").map { rowSlice ->
                    val rowText = body.substring(rowSlice.first, rowSlice.last + 1)
                    splitSlices(rowText, "&").map { cellSlice ->
                        val raw = rowText.substring(cellSlice.first, cellSlice.last + 1)
                        val leading = raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
                        val trailing = raw.indexOfLast { !it.isWhitespace() }.let { if (it < leading) leading - 1 else it }
                        val trimmed = if (trailing >= leading) raw.substring(leading, trailing + 1) else ""
                        val globalStart = global(bodyStart + rowSlice.first + cellSlice.first + leading)
                        Parser(trimmed, globalStart).parseSequence()
                    }
                }
                val delimiter = when (environment) {
                    "pmatrix" -> '('
                    "vmatrix" -> '|'
                    "cases" -> '{'
                    "matrix", "aligned", "align" -> ' '
                    else -> '['
                }
                return MathNode.Matrix(rows, delimiter)
            }
            return sourceSymbol("□", commandStart, index)
        }

        private fun sourceSymbol(
            value: String,
            localStart: Int,
            localEnd: Int,
            bold: Boolean = false,
            editable: Boolean = true
        ) = MathNode.Symbol(
            value = value,
            bold = bold,
            sourceStart = global(localStart),
            sourceEnd = global(localEnd),
            editable = editable
        )

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index++
        }

        private fun global(local: Int): Int = sourceOffset + local

        private fun flat(node: MathNode): String = when (node) {
            is MathNode.Symbol -> node.value
            is MathNode.Sequence -> node.children.joinToString("") { flat(it) }
            is MathNode.Delimited -> node.open + flat(node.body) + node.close
            MathNode.LineBreak -> " "
            else -> ""
        }
    }

    private fun splitSlices(value: String, delimiter: String): List<IntRange> {
        if (value.isEmpty()) return listOf(0..-1)
        val result = mutableListOf<IntRange>()
        var start = 0
        while (start <= value.length) {
            val at = value.indexOf(delimiter, start)
            if (at < 0) {
                result += start until value.length
                break
            }
            result += start until at
            start = at + delimiter.length
            if (start == value.length) {
                result += start..(start - 1)
                break
            }
        }
        return result
    }

    private fun matchingDelimiter(open: String): String = when (open) {
        "(" -> ")"
        "[" -> "]"
        "{" -> "}"
        "⌊" -> "⌋"
        "⌈" -> "⌉"
        "⟨" -> "⟩"
        "|" -> "|"
        else -> ""
    }

    private fun blackboard(value: String): String = when (value) {
        "N" -> "ℕ"
        "Z" -> "ℤ"
        "Q" -> "ℚ"
        "R" -> "ℝ"
        "C" -> "ℂ"
        else -> value
    }

    private fun calligraphic(value: String): String = when (value) {
        "F" -> "ℱ"
        "L" -> "ℒ"
        "P" -> "℘"
        else -> value
    }
}
