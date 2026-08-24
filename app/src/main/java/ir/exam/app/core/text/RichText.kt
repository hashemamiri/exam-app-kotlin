package ir.exam.app.core.text

import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.math.FormulaTextCodec

/** یک بخش از متن غنی سؤال: متن عادی، فرمول `$...$` یا شکل `%%FIG:...%%`. */
sealed interface RichSegment {
    data class Text(val text: String) : RichSegment
    data class Math(val index: Int, val tex: String) : RichSegment
    data class Figure(val index: Int, val spec: FigureSpec) : RichSegment
}

/** شکستن متن سؤال به بخش‌های متن/فرمول/شکل بر اساس قالب وب‌اپ. */
object RichTextSplitter {
    fun split(source: String): List<RichSegment> {
        val formulas = FormulaTextCodec.occurrences(source)
        val figures = FigureCodec.occurrences(source)
        val items = mutableListOf<Triple<Int, Int, Int>>() // start, end, kind (0=math,1=figure)
        formulas.forEach { items += Triple(it.start, it.endExclusive, 0) }
        figures.forEach { items += Triple(it.start, it.endExclusive, 1) }

        val order = items.indices.sortedBy { items[it].first }
        val result = mutableListOf<RichSegment>()
        var cursor = 0
        order.forEach { idx ->
            val (start, end, kind) = items[idx]
            // حتی بخش خالی را نگه می‌داریم تا ویرایشگر بعد از آخرین فرمول/شکل
            // یک کادر قابل تایپ داشته باشد و توکن مجبور نشود سطر جداگانه بسازد.
            if (start >= cursor) result += RichSegment.Text(source.substring(cursor, start))
            if (kind == 0) {
                val occ = formulas.firstOrNull { it.start == start && it.endExclusive == end }
                if (occ != null) result += RichSegment.Math(occ.index, occ.tex)
            } else {
                val occ = figures.firstOrNull { it.start == start && it.endExclusive == end }
                if (occ != null) result += RichSegment.Figure(occ.index, occ.spec)
            }
            cursor = end
        }
        // متن خالی انتهای توکن، محل نوشتن ادامهٔ جمله در همان ردیف است.
        if (cursor <= source.length) result += RichSegment.Text(source.substring(cursor))
        return result
    }

    /**
     * V57.0 — سطربندی نمایش: هر `\n` که معلم تایپ کرده یک سطر جدید برای
     * دانش‌آموز است و هر شکل/نمودار اگر در سطرش محتوای دیگری باشد به سطر
     * خودش منتقل می‌شود تا کامل و تمام‌عرض دیده شود؛ فرمول‌ها داخل سطر
     * می‌مانند. سطرهای خالی میانی (اینتر پشت‌سرهم) حفظ می‌شوند.
     */
    fun splitRows(source: String): List<List<RichSegment>> {
        val rows = mutableListOf(mutableListOf<RichSegment>())
        split(source).forEach { seg ->
            when (seg) {
                is RichSegment.Text -> seg.text.split('\n').forEachIndexed { i, part ->
                    if (i > 0) rows += mutableListOf<RichSegment>()
                    if (part.isNotEmpty()) rows.last() += RichSegment.Text(part)
                }
                is RichSegment.Figure -> {
                    if (rows.last().isNotEmpty()) rows += mutableListOf<RichSegment>()
                    rows.last() += seg
                    rows += mutableListOf<RichSegment>()
                }
                is RichSegment.Math -> rows.last() += seg
            }
        }
        while (rows.size > 1 && rows.last().isEmpty()) rows.removeAt(rows.lastIndex)
        // ویرایشگر معلم بعد از هر توکن شکل خودش '\n' می‌گذارد؛ سطر خالیِ بلافاصله
        // بعد از شکل، فاصلهٔ ناخواسته است و حذف می‌شود (سطرهای خالی عمدی متن می‌مانند).
        val cleaned = mutableListOf<List<RichSegment>>()
        rows.forEachIndexed { i, row ->
            val afterFigure = i > 0 && rows[i - 1].singleOrNull() is RichSegment.Figure
            if (row.isEmpty() && afterFigure) return@forEachIndexed
            cleaned += row
        }
        return cleaned
    }

    /** بازسازی متن کامل پس از ویرایش بخش متن عادی؛ فرمول‌ها و شکل‌ها حفظ می‌شوند. */
    fun reconstruct(segments: List<RichSegment>, editedIndex: Int, newText: String): String {
        val clean = newText.replace("$", "")
        return buildString {
            segments.forEachIndexed { index, seg ->
                when (seg) {
                    is RichSegment.Text -> append(if (index == editedIndex) clean else seg.text)
                    is RichSegment.Math -> append('$').append(seg.tex).append('$')
                    is RichSegment.Figure -> append("%%FIG:").append(seg.spec.toJson()).append("%%")
                }
            }
        }
    }
}
