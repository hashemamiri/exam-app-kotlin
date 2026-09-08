package ir.exam.app.ui.builder

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class QuestionType { ESSAY, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, NUMERIC, MATCHING }

/**
 * V61.6 — رنگ پاستلی اختصاصی هر نوع سؤال (درخواست کاربر):
 * تشریحی=صورتی #FFD1DC، چندگزینه‌ای=آبی #AEC6CF، صحیح/غلط=سبز #B4EEB4،
 * جای خالی=زرد #FDFD96، عددی=بنفش #C3B1E1، جورکردنی=هلویی #FFDAB9؛
 * (نعنایی #98FF98 و لاوندر #E6E6FA برای «وارد کردن» و «بانک سؤال» منوی +).
 */
fun QuestionType.pastelColor(): Long = when (this) {
    QuestionType.ESSAY -> 0xFFFFD1DC
    QuestionType.MULTIPLE_CHOICE -> 0xFFAEC6CF
    QuestionType.TRUE_FALSE -> 0xFFB4EEB4
    QuestionType.FILL_BLANK -> 0xFFFDFD96
    QuestionType.NUMERIC -> 0xFFC3B1E1
    QuestionType.MATCHING -> 0xFFFFDAB9
}

@Serializable
data class MediaDraft(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val xMm: Float = 20f,
    val yMm: Float = 30f,
    val widthMm: Float = 55f
)

/**
 * استایل مستقل هر گزینه: null یعنی «ارث از سؤال».
 * fontSizeSp=null هم یعنی اندازهٔ خود سؤال.
 */
@Serializable
data class OptionStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSizeSp: Float? = null
)

/**
 * استایل تکه‌ای متن سؤال: بازهٔ [start, end) با بولد/ایتالیک.
 * روی خود سؤال ذخیره می‌شود و رندر چاپ آن را مستقیماً مصرف می‌کند.
 */
@Serializable
data class StyleSpan(
    val start: Int,
    /** انحصاری (exclusive). */
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    // V114 — استایل‌های نوارِ پیش‌نمایش (زیرخط، رنگ #rrggbb، اندازه px، فونت).
    // null/false = بدون تغییر؛ ورودی‌های قدیمی بدون این فیلدها سالم خوانده می‌شوند.
    val underline: Boolean = false,
    val color: String? = null,
    val size: Int? = null,
    val font: String? = null
) {
    /** آیا این بازه اثری دارد؟ (برای حذف بازه‌های خالی). */
    val hasStyle: Boolean get() = bold || italic || underline || color != null || size != null || font != null
}

/**
 * V121 — تراز پاراگراف (راست/وسط/چپ/توجیه) برای بازهٔ [start, end) از متنِ
 * سؤال. برخلافِ StyleSpan که خط‌به‌خط/درون‌سطری است، این یک ویژگیِ
 * «پاراگرافی» است: در رندرِ پیش‌نمایش/چاپ، هر پاراگراف (تکهٔ بین دو \n) با
 * آخرین AlignSpanی که رویش همپوشانی دارد ترازبندی می‌شود؛ اگر هیچ‌کدام
 * همپوشانی نداشت، تراز پیش‌فرضِ کل سؤال (textAlign) اعمال می‌شود.
 */
@Serializable
data class AlignSpan(
    val start: Int,
    /** انحصاری (exclusive). */
    val end: Int,
    val align: String
)

/**
 * V121 — منطق خالص نگهداری/جابه‌جاییِ AlignSpan پس از تغییرِ متن.
 * عمداً از StyleSpanOps.adjust کپی شده (نه به آن وابسته) چون نوعِ عنصر
 * فرق دارد و AlignSpan فیلدهای بولی/رنگ ندارد؛ الگوریتمِ دیف پیشوند/پسوندِ
 * مشترک هرچند تکراری، خیلی کوچک و پایدار است (همان توضیحِ عدمِ ادغام که
 * در PrintTextSpanSegments.kt برای موردِ مشابه آمده).
 */
object AlignSpanOps {
    fun adjust(old: String, new: String, spans: List<AlignSpan>): List<AlignSpan> {
        if (spans.isEmpty()) return spans
        var start = 0
        val mp = minOf(old.length, new.length)
        while (start < mp && old[start] == new[start]) start++
        var eo = old.length
        var en = new.length
        while (en > start && eo > start && old[eo - 1] == new[en - 1]) { eo--; en-- }
        val delta = en - eo
        val out = mutableListOf<AlignSpan>()
        spans.forEach { span ->
            when {
                span.end <= start -> out += span
                span.start >= eo -> out += span.copy(start = span.start + delta, end = span.end + delta)
                else -> {
                    if (span.start < start) out += span.copy(start = span.start, end = start)
                    if (span.end > eo) out += span.copy(start = start + delta, end = span.end + delta)
                }
            }
        }
        return out.filter { it.end > it.start && it.start >= 0 && it.end <= new.length }
    }

    /**
     * V121 — بازهٔ [s, e) را طوری به مرزهای پاراگراف (جداشده با \n) گسترش
     * می‌دهد که کل پاراگراف(های) لمس‌شده را دربر بگیرد؛ تراز یک ویژگیِ
     * پاراگرافی است، نه فقط متنِ دقیقاً انتخاب‌شده.
     */
    fun expandToParagraphs(text: String, s: Int, e: Int): Pair<Int, Int> {
        if (s >= e || s < 0 || e > text.length) return s to e
        var start = s
        while (start > 0 && text[start - 1] != '\n') start--
        var end = e
        while (end < text.length && text[end] != '\n') end++
        return start to end
    }

    /** جایگزینیِ بازهٔ [s, e) با یک تراز؛ بازه‌های قبلیِ همپوشان برش می‌خورند. */
    fun setAlign(spans: List<AlignSpan>, s: Int, e: Int, align: String): List<AlignSpan> {
        if (s >= e) return spans
        val out = mutableListOf<AlignSpan>()
        spans.forEach { span ->
            if (span.end <= s || span.start >= e) { out += span; return@forEach }
            if (span.start < s) out += span.copy(start = span.start, end = s)
            if (span.end > e) out += span.copy(start = e, end = span.end)
        }
        out += AlignSpan(s, e, align)
        return out.filter { it.end > it.start }.sortedBy { it.start }
    }

    /** تراز مؤثر یک پاراگراف [pStart, pEnd): آخرین AlignSpanِ همپوشان، وگرنه fallback. */
    fun effectiveAlign(spans: List<AlignSpan>, pStart: Int, pEnd: Int, fallback: String): String {
        var result = fallback
        spans.forEach { span -> if (span.end > pStart && span.start < pEnd) result = span.align }
        return result
    }
}

/** V68 — منطق خالص نگهداری/تغییر بازه‌ها؛ JVM-تست‌پذیر بدون اندروید. */
object StyleSpanOps {

    /** جابه‌جایی/برش بازه‌ها پس از تغییر متن (diff پیشوند/پسوند مشترک). */
    fun adjust(old: String, new: String, spans: List<StyleSpan>): List<StyleSpan> {
        if (spans.isEmpty()) return spans
        var start = 0
        val mp = minOf(old.length, new.length)
        while (start < mp && old[start] == new[start]) start++
        var eo = old.length
        var en = new.length
        while (en > start && eo > start && old[eo - 1] == new[en - 1]) { eo--; en-- }
        val delta = en - eo
        val out = mutableListOf<StyleSpan>()
        spans.forEach { span ->
            when {
                span.end <= start -> out += span
                span.start >= eo -> out += span.copy(start = span.start + delta, end = span.end + delta)
                else -> {
                    // هم‌پوشان با ناحیهٔ تغییرشده: دو سر نگه داشته می‌شوند.
                    if (span.start < start) out += span.copy(start = span.start, end = start)
                    if (span.end > eo) out += span.copy(start = start + delta, end = span.end + delta)
                }
            }
        }
        return out.filter { it.end > it.start && it.start >= 0 && it.end <= new.length }
    }

    private fun coversAxis(spans: List<StyleSpan>, s: Int, e: Int, bold: Boolean): Boolean {
        var cursor = s
        val active = spans.filter { if (bold) it.bold else it.italic }.sortedBy { it.start }
        for (span in active) {
            if (span.start > cursor) return false
            cursor = maxOf(cursor, span.end)
            if (cursor >= e) return true
        }
        return cursor >= e
    }

    /** Toggle ورد: اگر کل بازه پوشش بود → حذف محور؛ وگرنه → افزودن به کل بازه. */
    fun toggle(spans: List<StyleSpan>, s: Int, e: Int, bold: Boolean = false, italic: Boolean = false): List<StyleSpan> {
        if (s >= e) return spans
        val removing = coversAxis(spans, s, e, bold)
        val result = mutableListOf<StyleSpan>()
        var midCoversSelection = false
        spans.forEach { span ->
            if (span.end <= s || span.start >= e) { result += span; return@forEach }
            // سرِ قبل و بعد از بازهٔ انتخابی دست‌نخورده.
            if (span.start < s) result += span.copy(start = span.start, end = s)
            if (span.end > e) result += span.copy(start = e, end = span.end)
            val midS = maxOf(span.start, s)
            val midE = minOf(span.end, e)
            val nb = if (removing && bold) false else (bold || span.bold)
            val ni = if (removing && italic) false else (italic || span.italic)
            if (midE > midS && (nb || ni || span.underline || span.color != null || span.size != null || span.font != null)) {
                result += span.copy(start = midS, end = midE, bold = nb, italic = ni)
                // میان‌تکه اگر کل بازهٔ انتخابی را با محور روشن پوشش داد، افزودن خام لازم نیست.
                val axisOn = if (bold) nb else ni
                if (axisOn && midS <= s && midE >= e) midCoversSelection = true
            }
        }
        if (!removing && !midCoversSelection) result += StyleSpan(s, e, bold, italic)
        // ادغام بازه‌های مجاور هم‌استایل
        val merged = mutableListOf<StyleSpan>()
        result.filter { it.end > it.start }.sortedBy { it.start }.forEach { span ->
            val last = merged.lastOrNull()
            if (last != null && span.start <= last.end && last.copy(start = 0, end = 0) == span.copy(start = 0, end = 0)) {
                merged[merged.lastIndex] = last.copy(end = maxOf(last.end, span.end))
            } else merged += span
        }
        return merged
    }

    /** شکستن یک تکهٔ متن به زیرتکه‌های استایل‌دار نسبت به آفست تکه در متن کامل. */
    fun splitBySpans(
        text: String,
        offsetInSource: Int,
        spans: List<StyleSpan>
    ): List<Triple<String, Boolean, Boolean>> {
        if (spans.isEmpty() || text.isEmpty()) return listOf(Triple(text, false, false))
        val local = spans.mapNotNull { sp ->
            val s = (sp.start - offsetInSource).coerceIn(0, text.length)
            val e = (sp.end - offsetInSource).coerceIn(0, text.length)
            if (e > s) s to e else null
        }
        if (local.isEmpty()) return listOf(Triple(text, false, false))
        val bounds = sortedSetOf(0, text.length)
        spans.forEach { sp ->
            val s = (sp.start - offsetInSource).coerceIn(0, text.length)
            val e = (sp.end - offsetInSource).coerceIn(0, text.length)
            if (e > s) { bounds.add(s); bounds.add(e) }
        }
        val list = bounds.toList()
        val out = mutableListOf<Triple<String, Boolean, Boolean>>()
        fun clamped(sp: StyleSpan): Pair<Int, Int> =
            ((sp.start - offsetInSource).coerceIn(0, text.length)) to
                ((sp.end - offsetInSource).coerceIn(0, text.length))
        for (i in 0 until list.size - 1) {
            val a = list[i]; val b = list[i + 1]
            if (b <= a) continue
            val bold = spans.any { sp -> val (s, e) = clamped(sp); e > s && s <= a && e >= b && sp.bold }
            val italic = spans.any { sp -> val (s, e) = clamped(sp); e > s && s <= a && e >= b && sp.italic }
            out += Triple(text.substring(a, b), bold, italic)
        }
        return out
    }
}

@Serializable
data class QuestionDraft(
    val id: String = UUID.randomUUID().toString(),
    val type: QuestionType,
    val text: String = "",
    val score: Double = 1.0,
    val options: List<String> = emptyList(),
    val optionIds: List<String> = emptyList(),
    val optionImages: List<String?> = emptyList(),
    // V64.4 — هم‌تراز با options؛ ورودی‌های قدیمی بدون این فیلد = همه null.
    val optionStyles: List<OptionStyle?> = emptyList(),
    // V64.5 — استایل مستقل هر سمت جورکردنی (هم‌تراز matchingLeft/Right).
    val matchingLeftStyles: List<OptionStyle?> = emptyList(),
    val matchingRightStyles: List<OptionStyle?> = emptyList(),
    val correctIndex: Int? = null,
    val expectedText: String = "",
    val expectedNumber: String = "",
    val tolerance: String = "0",
    val caseSensitive: Boolean = false,
    val matchingLeft: List<String> = emptyList(),
    val matchingLeftIds: List<String> = emptyList(),
    val matchingRight: List<String> = emptyList(),
    val matchingRightIds: List<String> = emptyList(),
    val matchingPairs: Map<Int, Int> = emptyMap(),
    val matchingLeftImages: List<String?> = emptyList(),
    val matchingRightImages: List<String?> = emptyList(),
    val answerImageMode: String = "no",
    val maxAnswerImages: Int = 0,
    /** V58.0 — اجازهٔ رسم نمودار پاسخ توسط دانش‌آموز. */
    val allowAnswerGraph: Boolean = false,
    val images: List<MediaDraft> = emptyList(),
    val textAlign: String = "right",
    val imagePosition: String = "below",
    val fontFamily: String = "default",
    val fontSizeSp: Float = 16f,
    val bold: Boolean = false,
    // استایل تکه‌ای متن سؤال.
    val textSpans: List<StyleSpan> = emptyList(),
    val italic: Boolean = false,
    val answerLines: Int = 2,
    /** lined = خط‌چین، blank = خالی، grid = شطرنجی (V107). */
    val answerLineStyle: String = "lined",
    /** V107 — فاصلهٔ سطرهای فضای پاسخ در چاپ، به سانتی‌متر (۰٫۵ تا ۲٫۰). */
    val answerLineSpacingCm: Float = 1.0f,
    // V99.2 — چیدمانِ اشیاء در پیش‌نمایشِ چاپی (JSON: figLayouts + slot)؛
    // از پنجرهٔ پیش‌نمایش به وضعیتِ بومی برمی‌گردد تا موقعیت‌ها ریست نشوند.
    val figLayoutsJson: String = "",
    /** V99.2 — فاصلهٔ اضافیِ خطِ جداکنندهٔ سؤال از پیش‌نمایش (پیکسل). */
    val sepExtraPx: Int = 0,
    /**
     * V121 — تراز پاراگرافیِ تکه‌ای متنِ سؤال (بازه‌های [start,end) با یک
     * تراز مستقل)؛ پیش‌فرض/fallback همان textAlign کلیِ سؤال است. از نوارِ
     * فرمتِ پیش‌نمایشِ چاپی می‌آید (مشابهِ textSpans که از همان‌جا می‌آید).
     */
    val alignSpans: List<AlignSpan> = emptyList(),
    val rawPublic: JsonObject = JsonObject(emptyMap()),
    val rawAnswer: JsonObject = JsonObject(emptyMap())
)

data class AudienceClassOption(val id: String, val name: String)
data class AudienceStudentOption(val id: String, val name: String, val classNames: String? = null)
data class AudienceSchoolOption(val id: String, val name: String, val city: String? = null)
data class BankCategoryOption(val id: Long, val name: String, val count: Int = 0)
data class BankQuestionOption(
    val id: Long,
    val subject: String?,
    val question: QuestionDraft,
    val categoryIds: Set<Long> = emptySet(),
    val categoryNames: List<String> = emptyList()
)

data class ExamBuilderState(
    val examId: String? = null,
    val code: String? = null,
    val loading: Boolean = false,
    val title: String = "",
    val subject: String = "",
    val durationMinutes: String = "",
    val opensAtIso: String? = null,
    val closesAtIso: String? = null,
    val questions: List<QuestionDraft> = emptyList(),
    val shuffleQuestions: Boolean = false,
    val shuffleOptions: Boolean = false,
    val negativeMarking: String = "",
    val teacherMessage: String = "",
    val attemptsAllowed: Int = 1,
    val attemptOnTimeout: Boolean = false,
    val gradePolicy: String = "last",
    val attemptCooldown: String = "",
    val audienceMode: String = "all",
    val audienceClasses: Set<String> = emptySet(),
    val audienceStudents: Set<String> = emptySet(),
    // V61.0 — مخاطب «مدارس»: همهٔ دانش‌آموزان ثبت‌شده در مدرسه‌های انتخابی.
    val audienceSchools: Set<String> = emptySet(),
    val availableClasses: List<AudienceClassOption> = emptyList(),
    val availableStudents: List<AudienceStudentOption> = emptyList(),
    val availableSchools: List<AudienceSchoolOption> = emptyList(),
    val bankQuestions: List<BankQuestionOption> = emptyList(),
    val bankCategories: List<BankCategoryOption> = emptyList(),
    val bankQuery: String = "",
    val selectedBankCategory: Long? = null,
    val importedBy: String? = null,
    val recoverableDraft: ir.exam.app.data.repository.ExamBuilderDraftPayload? = null,
    val saving: Boolean = false,
    val bankLoading: Boolean = false,
    val uploadProgress: String? = null,
    val savedCode: String? = null,
    val chargedToman: Long = 0,
    val walletBalanceToman: Long? = null,
    val error: String? = null,
    /** V58.0 — پیام گذرای موفقیت (مثلاً «به بانک سؤال اضافه شد»). */
    val notice: String? = null
) {
    val maximumChargeToman: Long get() = questions.size * 1_000L
}

data class ExamSaveResult(
    val code: String,
    val chargedToman: Long,
    val walletBalanceToman: Long?
)

data class ExamImportDraft(
    val title: String,
    val subject: String,
    val durationMinutes: Int,
    val negativeMarking: Double,
    val shuffleQuestions: Boolean,
    val shuffleOptions: Boolean,
    val teacherMessage: String,
    val attemptsAllowed: Int,
    val attemptOnTimeout: Boolean,
    val gradePolicy: String,
    val attemptCooldown: Int,
    val questions: List<QuestionDraft>,
    val opensAtIso: String? = null,
    val closesAtIso: String? = null,
    val exportedBy: String? = null,
    // V101 — اگر این import یک «آزمون چاپیِ محلی» است، شناسهٔ رکوردش:
    // ذخیرهٔ بعدیِ چاپی به‌جای ساختِ رکوردِ تازه، همان را به‌روز می‌کند.
    val localPrintExamId: String? = null
)
