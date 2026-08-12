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
        val ranges = NativeMathParser.editableRanges(text)
        if (ranges.isEmpty()) {
            val position = selectionEnd.coerceIn(0, text.length)
            return FormulaBoxEditResult(text, position, position)
        }
        val active = activeRange(text, selectionStart, selectionEnd)
        val currentIndex = active?.let { selected ->
            ranges.indexOfFirst { it == selected }.takeIf { it >= 0 }
                ?: ranges.indexOfFirst {
                    it.start < selected.endExclusive && it.endExclusive > selected.start
                }.takeIf { it >= 0 }
        } ?: 0
        val target = ranges[(currentIndex + delta).coerceIn(0, ranges.lastIndex)]
        return FormulaBoxEditResult(text, target.start, target.endExclusive)
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
}
