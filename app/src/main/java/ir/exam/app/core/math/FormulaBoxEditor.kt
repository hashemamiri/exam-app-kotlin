package ir.exam.app.core.math

data class FormulaBoxEditResult(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int
)

/** عملیات متنی قطعی برای ویرایشگر جعبه‌ای و درج از تمام کتابخانه‌ها. */
object FormulaBoxEditor {
    private const val MAX_LENGTH = 8000

    fun activeRange(text: String, selectionStart: Int, selectionEnd: Int): MathSourceRange? {
        val safeStart = selectionStart.coerceIn(0, text.length)
        val safeEnd = selectionEnd.coerceIn(0, text.length)
        val ranges = NativeMathParser.editableRanges(text)
        if (safeStart != safeEnd) {
            val start = minOf(safeStart, safeEnd)
            val end = maxOf(safeStart, safeEnd)
            return ranges.firstOrNull { start >= it.start && end <= it.endExclusive }
                ?: MathSourceRange(start, end)
        }
        val cursor = safeEnd
        return ranges.firstOrNull { cursor >= it.start && cursor < it.endExclusive }
            ?: ranges.lastOrNull { it.endExclusive == cursor }
            ?: ranges.firstOrNull { it.start > cursor }
    }

    fun insert(
        current: String,
        selectionStart: Int,
        selectionEnd: Int,
        insertion: String,
        activateFirstInsertedBox: Boolean = false,
        replaceActiveBoxWhenCollapsed: Boolean = false
    ): FormulaBoxEditResult {
        val selected = if (replaceActiveBoxWhenCollapsed) {
            activeRange(current, selectionStart, selectionEnd)
        } else {
            MathSourceRange(
                minOf(selectionStart, selectionEnd).coerceIn(0, current.length),
                maxOf(selectionStart, selectionEnd).coerceIn(0, current.length)
            )
        }
        val start = selected?.start ?: selectionStart.coerceIn(0, current.length)
        val end = selected?.endExclusive ?: selectionEnd.coerceIn(start, current.length)
        val next = (current.substring(0, start) + insertion + current.substring(end)).take(MAX_LENGTH)
        val insertedEnd = (start + insertion.length).coerceAtMost(next.length)
        val firstBox = if (activateFirstInsertedBox) {
            NativeMathParser.editableRanges(next).firstOrNull { range ->
                range.start >= start && range.endExclusive <= insertedEnd &&
                    (range.start < insertedEnd || range.start == range.endExclusive)
            }
        } else null
        return FormulaBoxEditResult(
            text = next,
            selectionStart = firstBox?.start ?: insertedEnd,
            selectionEnd = firstBox?.endExclusive ?: insertedEnd
        )
    }

    fun moveActiveBox(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        delta: Int
    ): FormulaBoxEditResult {
        if (selectionStart == selectionEnd) {
            val nextPos = (selectionEnd + delta).coerceIn(0, text.length)
            return FormulaBoxEditResult(text, nextPos, nextPos)
        }
        val ranges = NativeMathParser.editableRanges(text)
        if (ranges.isEmpty()) {
            val nextPos = (selectionEnd + delta).coerceIn(0, text.length)
            return FormulaBoxEditResult(text, nextPos, nextPos)
        }
        val active = activeRange(text, selectionStart, selectionEnd)
        val currentIndex = active?.let { selected ->
            ranges.indexOfFirst { it == selected }.takeIf { it >= 0 }
                ?: ranges.indexOfFirst {
                    it.start <= selected.start && it.endExclusive >= selected.endExclusive
                }.takeIf { it >= 0 }
                ?: ranges.indexOfFirst {
                    it.start < selected.endExclusive && it.endExclusive > selected.start
                }.takeIf { it >= 0 }
        }
        if (currentIndex != null && currentIndex >= 0) {
            val nextIndex = currentIndex + delta
            if (nextIndex in ranges.indices) {
                val target = ranges[nextIndex]
                return FormulaBoxEditResult(text, target.start, target.endExclusive)
            }
            if (delta > 0) {
                val after = ranges[currentIndex].endExclusive
                return FormulaBoxEditResult(text, after, after)
            }
            if (delta < 0) {
                val before = ranges[currentIndex].start
                return FormulaBoxEditResult(text, before, before)
            }
        }
        val nextPos = (selectionEnd + delta).coerceIn(0, text.length)
        val matchingRange = ranges.firstOrNull { nextPos >= it.start && nextPos < it.endExclusive }
        return if (matchingRange != null) {
            FormulaBoxEditResult(text, matchingRange.start, matchingRange.endExclusive)
        } else {
            FormulaBoxEditResult(text, nextPos, nextPos)
        }
    }

    fun moveSpatialBox(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        direction: Int
    ): FormulaBoxEditResult {
        if (text.isBlank()) return FormulaBoxEditResult(text, 0, 0)
        val document = NativeMathSvgRenderer.render(text, 32f, showEditBoxes = true)
        val boxes = document.editBoxes
        if (boxes.isEmpty()) return moveActiveBox(text, selectionStart, selectionEnd, direction)
        val active = activeRange(text, selectionStart, selectionEnd)
        val current = active?.let { selected ->
            boxes.firstOrNull { it.sourceStart == selected.start && it.sourceEnd == selected.endExclusive }
                ?: boxes.firstOrNull { it.sourceStart <= selected.start && it.sourceEnd >= selected.endExclusive }
                ?: boxes.firstOrNull { it.sourceStart < selected.endExclusive && it.sourceEnd > selected.start }
        } ?: boxes.first()
        val currentX = current.xPx + current.widthPx / 2f
        val currentY = current.yPx + current.heightPx / 2f
        val candidate = boxes.asSequence().filter { it !== current }.filter { box ->
            val centerY = box.yPx + box.heightPx / 2f
            if (direction < 0) centerY < currentY - 0.5f else centerY > currentY + 0.5f
        }.minByOrNull { box ->
            val centerX = box.xPx + box.widthPx / 2f
            val centerY = box.yPx + box.heightPx / 2f
            kotlin.math.abs(centerY - currentY) * 4f + kotlin.math.abs(centerX - currentX)
        }
        if (candidate != null) {
            return FormulaBoxEditResult(text, candidate.sourceStart, candidate.sourceEnd)
        }
        return moveActiveBox(text, selectionStart, selectionEnd, if (direction < 0) -1 else 1)
    }

    fun firstBox(text: String): FormulaBoxEditResult {
        return FormulaBoxEditResult(text, 0, 0)
    }

    fun lastBox(text: String): FormulaBoxEditResult {
        val end = text.length
        return FormulaBoxEditResult(text, end, end)
    }

    fun backspace(text: String, selectionStart: Int, selectionEnd: Int): FormulaBoxEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        if (start != end) {
            val next = text.removeRange(start, end)
            return FormulaBoxEditResult(next, start, start)
        }
        if (start == 0) return FormulaBoxEditResult(text, 0, 0)
        val active = activeRange(text, start, end)
        if (active != null && active.start < active.endExclusive && text.getOrNull(active.start) == '\\') {
            val next = text.removeRange(active.start, active.endExclusive)
            return FormulaBoxEditResult(next, active.start, active.start)
        }
        val deleteAt = text.offsetByCodePoints(start, -1)
        val next = text.removeRange(deleteAt, start)
        return FormulaBoxEditResult(next, deleteAt, deleteAt)
    }

    fun typeCharacter(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        character: String
    ): FormulaBoxEditResult {
        if (character.isEmpty()) return FormulaBoxEditResult(text, selectionStart, selectionEnd)
        val selectedStart = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val selectedEnd = maxOf(selectionStart, selectionEnd).coerceIn(selectedStart, text.length)
        return when (character) {
            "/" -> {
                val atom = if (selectedStart != selectedEnd) MathSourceRange(selectedStart, selectedEnd)
                else NativeMathParser.editableRanges(text).lastOrNull { it.endExclusive <= selectedStart }
                val numerator = atom?.let { text.substring(it.start, it.endExclusive) }.orEmpty()
                val from = atom?.start ?: selectedStart
                val base = text.substring(0, from) + "\\frac{$numerator}{}" + text.substring(selectedEnd)
                val empty = NativeMathParser.editableRanges(base).firstOrNull {
                    it.start >= from && it.start == it.endExclusive
                }
                FormulaBoxEditResult(base, empty?.start ?: from, empty?.endExclusive ?: from)
            }
            "^", "_" -> {
                val marker = character.first()
                val baseText = text.substring(selectedStart, selectedEnd)
                val insertion = if (baseText.isNotEmpty()) "$baseText$marker{}" else "$marker{}"
                val result = insert(text, selectedStart, selectedEnd, insertion)
                val empty = NativeMathParser.editableRanges(result.text).firstOrNull {
                    it.start >= selectedStart && it.start == it.endExclusive
                }
                result.copy(selectionStart = empty?.start ?: result.selectionStart, selectionEnd = empty?.endExclusive ?: result.selectionEnd)
            }
            "(" -> {
                val body = text.substring(selectedStart, selectedEnd)
                val result = insert(text, selectedStart, selectedEnd, "\\left($body\\right)")
                val range = NativeMathParser.editableRanges(result.text).firstOrNull { it.start >= selectedStart }
                result.copy(selectionStart = range?.start ?: result.selectionStart, selectionEnd = range?.endExclusive ?: result.selectionEnd)
            }
            ")" -> moveActiveBox(text, selectionStart, selectionEnd, 1)
            "*", "×" -> insert(text, selectedStart, selectedEnd, "\\times ")
            "÷" -> insert(text, selectedStart, selectedEnd, "\\div ")
            ">", "<" -> {
                if (selectedStart == selectedEnd && selectedStart > 0 && text[selectedStart - 1] == '-') {
                    insert(
                        text,
                        selectedStart - 1,
                        selectedStart,
                        if (character == ">") "\\to " else "\\leftarrow "
                    )
                } else insert(text, selectedStart, selectedEnd, character)
            }
            else -> insert(text, selectedStart, selectedEnd, character)
        }
    }

    fun importText(raw: String): FormulaBoxEditResult {
        val stripped = raw.trim().removeSurrounding("$")
        val tex = if (Regex("\\\\[A-Za-z]+").containsMatchIn(stripped)) stripped
        else NativeNaturalMathConverter.toTex(stripped)
        return replaceAll(tex, activateFirstBox = false)
    }

    fun replaceAll(text: String, activateFirstBox: Boolean = false): FormulaBoxEditResult {
        val safe = text.take(MAX_LENGTH)
        val first = if (activateFirstBox) NativeMathParser.editableRanges(safe).firstOrNull() else null
        return FormulaBoxEditResult(
            safe,
            first?.start ?: safe.length,
            first?.endExclusive ?: safe.length
        )
    }

    fun caretAtEnd(text: String): FormulaBoxEditResult {
        val end = text.take(MAX_LENGTH).length
        return FormulaBoxEditResult(text.take(MAX_LENGTH), end, end)
    }
}
