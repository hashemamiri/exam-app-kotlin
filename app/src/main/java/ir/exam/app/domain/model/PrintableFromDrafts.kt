package ir.exam.app.domain.model

import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType

/**
 * V86.9 — ساختِ `OfficialExamPrintable` از سؤال‌های آزمون‌ساز، بدونِ شبکه.
 *
 * `SupabasePortabilityRepository.printableExam` همین نگاشت را داشت ولی درونِ
 * یک تابعِ suspend که اول از Supabase می‌خواند. آزمونِ چاپیِ محلی روی دستگاه
 * است و چیزی برای خواندن ندارد، پس منطق اینجا استخراج شد تا **هر دو مسیر یک
 * نگاشت داشته باشند** و کلیدِ پاسخِ نسخهٔ استاد در هر دو یکسان دربیاید.
 */
object PrintableFromDrafts {

    /** کلیدِ پاسخ برای نسخهٔ استاد؛ تشریحی کلید ندارد. */
    fun answerTextFor(question: QuestionDraft): String? = when (question.type) {
        QuestionType.MULTIPLE_CHOICE -> question.correctIndex?.let { question.options.getOrNull(it) }
        QuestionType.TRUE_FALSE -> if (question.expectedText == "true") "صحیح" else "غلط"
        QuestionType.FILL_BLANK -> question.expectedText.replace('|', '،')
        QuestionType.NUMERIC -> question.expectedNumber + " ± " + question.tolerance
        QuestionType.MATCHING -> question.matchingPairs.entries.sortedBy { it.key }
            .joinToString("، ") { (left, right) -> "${left + 1}←${right + 1}" }
        QuestionType.ESSAY -> null
    }

    fun questionAt(index: Int, question: QuestionDraft): OfficialPrintQuestion =
        OfficialPrintQuestion(
            number = index + 1,
            text = question.text,
            score = question.score,
            options = question.options,
            optionStyles = question.optionStyles.map { style ->
                style?.let { Triple(it.bold, it.italic, it.fontSizeSp) }
            },
            matchingLeft = question.matchingLeft,
            matchingRight = question.matchingRight,
            matchingLeftStyles = question.matchingLeftStyles.map { style ->
                style?.let { Triple(it.bold, it.italic, it.fontSizeSp) }
            },
            matchingRightStyles = question.matchingRightStyles.map { style ->
                style?.let { Triple(it.bold, it.italic, it.fontSizeSp) }
            },
            answerText = answerTextFor(question),
            answerLines = question.answerLines,
            answerLineStyle = question.answerLineStyle,
            textAlign = question.textAlign,
            imagePosition = question.imagePosition,
            fontFamily = question.fontFamily,
            fontSizeSp = question.fontSizeSp,
            bold = question.bold,
            italic = question.italic,
            textSpans = question.textSpans.map {
                PrintTextSpan(it.start, it.end, it.bold, it.italic)
            },
            imageWidthsMm = question.images.map { it.widthMm } +
                question.optionImages.filterNotNull().map { 40f },
            imageXmm = question.images.map { it.xMm } +
                question.optionImages.filterNotNull().map { 20f },
            imageYmm = question.images.map { it.yMm } +
                question.optionImages.filterNotNull().map { 30f },
            imageUrls = question.images.map { it.uri } + question.optionImages.filterNotNull()
        )

    /** آزمونِ چاپیِ محلی: عنوان و درس از خودِ رکورد، بقیه از سربرگِ ذخیره‌شده. */
    fun build(
        title: String,
        subject: String,
        header: OfficialPrintHeader,
        questions: List<QuestionDraft>,
        includeAnswerKey: Boolean = false
    ): OfficialExamPrintable = OfficialExamPrintable(
        documentTitle = title,
        header = header,
        subject = subject,
        durationMinutes = 0,
        totalScore = questions.sumOf { it.score },
        includeAnswerKey = includeAnswerKey,
        questions = questions.mapIndexed { index, q -> questionAt(index, q) }
    )
}
