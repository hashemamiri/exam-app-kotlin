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

    /**
     * V120 — گزینه‌های قابل‌چاپِ سؤال. برای «صحیح/غلط» خودِ `QuestionDraft`
     * هیچ‌وقت `options` را پر نمی‌کند (فقط برای MULTIPLE_CHOICE پر می‌شود)،
     * پس تا امروز `ExamHtmlPrintPayloadBuilder` هیچ‌وقت شاخهٔ «truefalse» را
     * برای این نوع پیدا نمی‌کرد و آن را به‌اشتباه به‌عنوان سؤال تشریحی چاپ
     * می‌کرد. این‌جا برای TRUE_FALSE گزینه‌های ثابت «صحیح»/«غلط» ساخته
     * می‌شوند تا هم حدسِ نوع در payload درست کار کند و هم — با اضافه‌شدنِ
     * `questionType` صریح — دیگر به این حدس متکی نباشیم.
     */
    private fun printableOptionsFor(question: QuestionDraft): List<String> = when (question.type) {
        QuestionType.TRUE_FALSE -> listOf("صحیح", "غلط")
        else -> question.options
    }

    /** V120 — نوعِ صریحِ سؤال برای موتور چاپ؛ حدسِ متنی را زائد می‌کند. */
    private fun printableTypeFor(question: QuestionDraft): String = when (question.type) {
        QuestionType.MULTIPLE_CHOICE -> "multiple"
        QuestionType.TRUE_FALSE -> "truefalse"
        QuestionType.FILL_BLANK -> "fill"
        QuestionType.NUMERIC -> "numeric"
        QuestionType.MATCHING -> "matching"
        QuestionType.ESSAY -> "long"
    }

    fun questionAt(index: Int, question: QuestionDraft): OfficialPrintQuestion {
        // V120 — تصاویرِ گالری و تصاویرِ گزینه‌ها با اندازه/موقعیتِ پیش‌فرضِ
        // مخصوصِ خودشان با هم زیپ می‌شوند (نه این‌که دو منبعِ جدا با طول‌های
        // متفاوت به هم بچسبند) تا اندیسِ url هیچ‌وقت از اندیسِ اندازه/موقعیت
        // عقب یا جلو نیفتد.
        val galleryImages = question.images.map { media ->
            ImagePlacement(media.uri, media.widthMm, media.xMm, media.yMm)
        }
        val optionImages = question.optionImages.filterNotNull().map { uri ->
            ImagePlacement(uri, widthMm = 40f, xMm = 20f, yMm = 30f)
        }
        val images = galleryImages + optionImages

        return OfficialPrintQuestion(
            number = index + 1,
            text = question.text,
            score = question.score,
            options = printableOptionsFor(question),
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
            answerLineSpacingCm = question.answerLineSpacingCm,
            textAlign = question.textAlign,
            imagePosition = question.imagePosition,
            fontFamily = question.fontFamily,
            fontSizeSp = question.fontSizeSp,
            bold = question.bold,
            italic = question.italic,
            textSpans = question.textSpans.map {
                PrintTextSpan(it.start, it.end, it.bold, it.italic, it.underline, it.color, it.size, it.font)
            },
            // V121 — تراز پاراگرافیِ تکه‌ای متن سؤال.
            alignSpans = question.alignSpans.map { PrintAlignSpan(it.start, it.end, it.align) },
            imageWidthsMm = images.map { it.widthMm },
            imageXmm = images.map { it.xMm },
            // V99.2 — چیدمانِ پیش‌نمایش (اشیاء + جداکننده) با printable می‌رود.
            figLayoutsJson = question.figLayoutsJson,
            sepExtraPx = question.sepExtraPx,
            imageYmm = images.map { it.yMm },
            imageUrls = images.map { it.uri },
            // V120 — نوعِ صریح؛ ExamHtmlPrintPayloadBuilder دیگر مجبور به حدس نیست.
            questionType = printableTypeFor(question)
        )
    }

    /** V120 — یک تصویر به‌همراه اندازه/موقعیتِ خودش (برای زیپ‌کردنِ ایمن). */
    private data class ImagePlacement(val uri: String, val widthMm: Float, val xMm: Float, val yMm: Float)

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
