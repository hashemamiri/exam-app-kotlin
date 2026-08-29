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
 * V64.4 — استایل مستقل هر گزینه (Word-مانند): null یعنی «ارث از سؤال».
 * fontSizeSp=null هم یعنی اندازهٔ خود سؤال.
 */
@Serializable
data class OptionStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSizeSp: Float? = null
)

/**
 * V68 — استایل تکه‌ای متن سؤال (Word-مانند): بازهٔ [start, end) با بولد/ایتالیک.
 * فقط در چیدمان چاپی (PrintLayoutStore) نوشته می‌شود و به متن دانش‌آموز
 * سرریز نمی‌کند؛ JSON قدیمی بدون spans = خالی.
 */
@Serializable
data class StyleSpan(
    val start: Int,
    /** انحصاری (exclusive). */
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false
)

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
                    if (span.start < start) out += StyleSpan(span.start, start, span.bold, span.italic)
                    if (span.end > eo) out += StyleSpan(start + delta, span.end + delta, span.bold, span.italic)
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
            if (span.start < s) result += StyleSpan(span.start, s, span.bold, span.italic)
            if (span.end > e) result += StyleSpan(e, span.end, span.bold, span.italic)
            val midS = maxOf(span.start, s)
            val midE = minOf(span.end, e)
            val nb = if (removing && bold) false else (bold || span.bold)
            val ni = if (removing && italic) false else (italic || span.italic)
            if (midE > midS && (nb || ni)) {
                result += StyleSpan(midS, midE, nb, ni)
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
            if (last != null && span.start <= last.end && last.bold == span.bold && last.italic == span.italic) {
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
    // V68 — استایل تکه‌ای متن (فقط چیدمان چاپی؛ JSON قدیمی = خالی).
    val textSpans: List<StyleSpan> = emptyList(),
    val italic: Boolean = false,
    val answerLines: Int = 2,
    val answerLineStyle: String = "lined",
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
    val exportedBy: String? = null
)
