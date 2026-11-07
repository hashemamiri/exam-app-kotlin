package ir.exam.app.core.text

import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureOccurrence
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.math.FormulaOccurrence
import ir.exam.app.core.math.FormulaTextCodec

/** یک بخش از متن غنی سؤال: متن عادی، فرمول `$...$`، شکل `%%FIG:...%%` یا بلوک کد (V350). */
sealed interface RichSegment {
    data class Text(val text: String) : RichSegment
    data class Math(val index: Int, val tex: String) : RichSegment
    data class Figure(val index: Int, val spec: FigureSpec) : RichSegment

    /** V350 — بلوک کد هم یک شیء درون‌متنی است، در سؤال و در گزینه‌ها. */
    data class Code(val index: Int, val lang: String, val code: String) : RichSegment
}

/** شکستن متن سؤال به بخش‌های متن/فرمول/شکل بر اساس قالب وب‌اپ. */
object RichTextSplitter {
    fun split(source: String): List<RichSegment> =
        split(source, FormulaTextCodec.occurrences(source), FigureCodec.occurrences(source),
            CodeBlockCodec.occurrences(source))

    /** نسخهٔ قابل cache برای Compose؛ occurrenceها فقط یک‌بار برای هر متن ساخته می‌شوند. */
    fun split(
        source: String,
        formulas: List<FormulaOccurrence>,
        figures: List<FigureOccurrence>,
        codes: List<CodeOccurrence> = CodeBlockCodec.occurrences(source)
    ): List<RichSegment> {
        val items = mutableListOf<Triple<Int, Int, Int>>() // start, end, kind (0=math,1=figure,2=code)
        formulas.forEach { items += Triple(it.start, it.endExclusive, 0) }
        figures.forEach { items += Triple(it.start, it.endExclusive, 1) }
        // V350 — بلوک کد بر شکل/فرمولِ داخل خودش اولویت دارد تا `$` درون کد
        // تراشهٔ فرمول نسازد؛ بازه‌های هم‌پوشان کنار گذاشته می‌شوند.
        val codeRanges = codes.map { it.start to it.endExclusive }
        fun insideCode(start: Int, end: Int): Boolean =
            codeRanges.any { (s, e) -> start >= s && end <= e }
        codes.forEach { items += Triple(it.start, it.endExclusive, 2) }
        val formulaByRange = formulas.associateBy { it.start to it.endExclusive }
        val figureByRange = figures.associateBy { it.start to it.endExclusive }
        val codeByRange = codes.associateBy { it.start to it.endExclusive }

        val order = items.indices.sortedBy { items[it].first }
        val result = mutableListOf<RichSegment>()
        var cursor = 0
        order.forEach { idx ->
            val (start, end, kind) = items[idx]
            if (kind != 2 && insideCode(start, end)) return@forEach
            if (start < cursor) return@forEach
            // حتی بخش خالی را نگه می‌داریم تا ویرایشگر بعد از آخرین فرمول/شکل
            // یک کادر قابل تایپ داشته باشد و توکن مجبور نشود سطر جداگانه بسازد.
            if (start >= cursor) result += RichSegment.Text(source.substring(cursor, start))
            when (kind) {
                0 -> {
                    val occ = formulaByRange[start to end]
                    if (occ != null) result += RichSegment.Math(occ.index, occ.tex)
                }
                1 -> {
                    val occ = figureByRange[start to end]
                    if (occ != null) result += RichSegment.Figure(occ.index, occ.spec)
                }
                else -> {
                    val occ = codeByRange[start to end]
                    if (occ != null) result += RichSegment.Code(occ.index, occ.lang, occ.code)
                }
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
    fun splitRows(source: String): List<List<RichSegment>> =
        splitRows(source, FormulaTextCodec.occurrences(source), FigureCodec.occurrences(source),
            CodeBlockCodec.occurrences(source))

    /** سطربندی با occurrenceهای ازپیش‌محاسبه‌شده برای جلوگیری از parse دوباره. */
    fun splitRows(
        source: String,
        formulas: List<FormulaOccurrence>,
        figures: List<FigureOccurrence>,
        codes: List<CodeOccurrence> = CodeBlockCodec.occurrences(source)
    ): List<List<RichSegment>> {
        val rows = mutableListOf(mutableListOf<RichSegment>())
        split(source, formulas, figures, codes).forEach { seg ->
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
                // V350 — کادر کد مثل شکل، سطر مستقل خودش را می‌گیرد.
                is RichSegment.Code -> {
                    if (rows.last().isNotEmpty()) rows += mutableListOf<RichSegment>()
                    rows.last() += seg
                    rows += mutableListOf<RichSegment>()
                }
            }
        }
        while (rows.size > 1 && rows.last().isEmpty()) rows.removeAt(rows.lastIndex)
        // ویرایشگر معلم بعد از هر توکن شکل خودش '\n' می‌گذارد؛ سطر خالیِ بلافاصله
        // بعد از شکل، فاصلهٔ ناخواسته است و حذف می‌شود (سطرهای خالی عمدی متن می‌مانند).
        val cleaned = mutableListOf<List<RichSegment>>()
        rows.forEachIndexed { i, row ->
            val prev = if (i > 0) rows[i - 1].singleOrNull() else null
            val afterFigure = prev is RichSegment.Figure || prev is RichSegment.Code
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
                    is RichSegment.Code -> append(CodeBlockCodec.token(seg.code, seg.lang))
                }
            }
        }
    }

    /**
     * V67 — بازهٔ آفست هر بخش در متن خام. برای بخش‌های متنی دقیقاً بازهٔ
     * زیررشتهٔ متن است و برای توکن‌ها بازهٔ خود توکن. درج در محل مکان‌نما و
     * بازگشت مکان‌نما پس از درج/ویرایش از همین بازه‌ها استفاده می‌کنند.
     */
    fun segmentSourceRanges(
        segments: List<RichSegment>,
        formulas: List<FormulaOccurrence>,
        figures: List<FigureOccurrence>,
        codes: List<CodeOccurrence> = emptyList()
    ): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var offset = 0
        segments.forEach { seg ->
            when (seg) {
                is RichSegment.Text -> {
                    val end = offset + seg.text.length
                    ranges += offset until end
                    offset = end
                }
                is RichSegment.Math -> {
                    val end = formulas.getOrNull(seg.index)?.endExclusive ?: offset
                    ranges += offset until end
                    offset = end
                }
                is RichSegment.Figure -> {
                    val end = figures.getOrNull(seg.index)?.endExclusive ?: offset
                    ranges += offset until end
                    offset = end
                }
                is RichSegment.Code -> {
                    val end = codes.getOrNull(seg.index)?.endExclusive
                        ?: (offset + CodeBlockCodec.token(seg.code, seg.lang).length)
                    ranges += offset until end
                    offset = end
                }
            }
        }
        return ranges
    }

    /**
     * V67.1 — بازهٔ تغییر میان دو متن با پیشوند/پسوند مشترک؛ پایان بازه محل
     * طبیعی مکان‌نما پس از درج/ویرایش (بلافاصله بعد از توکن درج‌شده) است.
     */
    fun changeRangeAfterEdit(old: String, new: String): IntRange {
        if (old == new) return IntRange.EMPTY
        var start = 0
        val maxPrefix = minOf(old.length, new.length)
        while (start < maxPrefix && old[start] == new[start]) start++
        var endOld = old.length
        var endNew = new.length
        while (endNew > start && endOld > start && old[endOld - 1] == new[endNew - 1]) {
            endOld--
            endNew--
        }
        return IntRange(start, maxOf(endNew - 1, start))
    }
}
