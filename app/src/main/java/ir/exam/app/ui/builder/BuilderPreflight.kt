package ir.exam.app.ui.builder

/**
 * V321 — «بررسی نهایی» آزمونساز.
 *
 * پیش از ذخیره/انتشار، سؤال‌ها یک‌بار به‌صورت ماشینی بازبینی می‌شوند تا خطاهای رایج
 * (سؤالِ بی‌متن، گزینه‌های تکراری، پاسخ صحیحِ انتخاب‌نشده، نمرهٔ صفر، عددِ نامعتبر و…)
 * پیش از ارسال به سرور گرفته شود. این فایل عمداً **بدون وابستگی اندروید** نوشته شده تا
 * با تست واحد (`V321_BuilderPreflightTest`) قابل بررسی باشد.
 */

enum class BuilderIssueLevel { BLOCKER, WARNING, HINT }

data class BuilderIssue(
    /** ۱ = سؤال اول؛ ۰ = سطح کل آزمون (عنوان، نمرهٔ کل). */
    val questionIndex: Int,
    val code: String,
    val level: BuilderIssueLevel,
    val message: String
) {
    val where: String get() = if (questionIndex <= 0) "کل آزمون" else "سؤال $questionIndex"
}

data class BuilderPreflightReport(
    val issues: List<BuilderIssue>,
    val questionCount: Int,
    val totalScore: Double
) {
    val blockers: List<BuilderIssue> get() = issues.filter { it.level == BuilderIssueLevel.BLOCKER }
    val warnings: List<BuilderIssue> get() = issues.filter { it.level == BuilderIssueLevel.WARNING }
    val hints: List<BuilderIssue> get() = issues.filter { it.level == BuilderIssueLevel.HINT }

    /** ذخیره فقط وقتی مجاز است که خطای بازدارنده نباشد. */
    val canSave: Boolean get() = blockers.isEmpty()
    val isEmpty: Boolean get() = issues.isEmpty()

    /** پیام کوتاه برای نوار بالای پنجره. */
    val summary: String
        get() = when {
            questionCount == 0 -> "هنوز سؤالی اضافه نشده است."
            isEmpty -> "همه‌چیز مرتب است: $questionCount سؤال، جمع نمره ${formatScore(totalScore)}"
            blockers.isNotEmpty() -> "${blockers.size} مورد باید اصلاح شود (${warnings.size} هشدار)"
            else -> "قابل ذخیره است؛ ${warnings.size} هشدار و ${hints.size} پیشنهاد"
        }

    companion object {
        fun formatScore(value: Double): String {
            val rounded = Math.round(value * 100.0) / 100.0
            return if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
        }
    }
}

object BuilderPreflight {

    /**
     * بازبینی کامل.
     * @param expectedTotal اگر داده شود (مثلاً نمرهٔ کلِ اعلام‌شده در تنظیمات آزمون) با جمع نمره‌ها مقایسه می‌شود.
     */
    fun check(
        title: String,
        questions: List<QuestionDraft>,
        expectedTotal: Double? = null
    ): BuilderPreflightReport {
        val issues = mutableListOf<BuilderIssue>()
        fun add(index: Int, code: String, level: BuilderIssueLevel, message: String) {
            issues += BuilderIssue(index, code, level, message)
        }

        if (title.isBlank()) add(0, "title.blank", BuilderIssueLevel.WARNING, "عنوان آزمون خالی است؛ در فهرست آزمون‌ها با نام «بدون عنوان» دیده می‌شود.")
        if (questions.isEmpty()) add(0, "questions.empty", BuilderIssueLevel.BLOCKER, "هیچ سؤالی اضافه نشده است.")
        if (questions.size > MAX_QUESTIONS) add(0, "questions.tooMany", BuilderIssueLevel.WARNING, "بیش از $MAX_QUESTIONS سؤال: چاپ ممکن است چند صفحه‌ای و کند شود.")

        questions.forEachIndexed { i, q ->
            val n = i + 1
            val hasMedia = q.images.isNotEmpty() || q.hasInlineFigure() || !q.audioUri.isNullOrBlank()
            val hasFigureOnly = q.text.isBlank() && (q.images.isNotEmpty() || q.hasInlineFigure())
            if (q.text.isBlank() && !hasMedia) {
                add(n, "text.blank", BuilderIssueLevel.BLOCKER, "متن سؤال خالی است.")
            } else if (hasFigureOnly) {
                add(n, "text.figureOnly", BuilderIssueLevel.HINT, "سؤال فقط تصویر/شکل دارد؛ متن کوتاه راهنما اضافه کنید.")
            }
            if (q.text.length > MAX_TEXT_LENGTH) add(n, "text.long", BuilderIssueLevel.HINT, "متن سؤال بسیار بلند است (${q.text.length} نویسه)؛ در چاپ ممکن است بریده شود.")

            when {
                q.score.isNaN() || q.score <= 0.0 ->
                    add(n, "score.invalid", BuilderIssueLevel.BLOCKER, "نمرهٔ سؤال باید بزرگ‌تر از صفر باشد.")
                q.score > 100.0 ->
                    add(n, "score.huge", BuilderIssueLevel.WARNING, "نمرهٔ سؤال بزرگ‌تر از ۱۰۰ است؛ اشتباه تایپی نباشد؟")
                (q.score * 4) != Math.floor(q.score * 4) ->
                    add(n, "score.fraction", BuilderIssueLevel.HINT, "نمرهٔ ${BuilderPreflightReport.formatScore(q.score)} مضرب ۰٫۲۵ نیست.")
            }

            when (q.type) {
                QuestionType.MULTIPLE_CHOICE -> if (q.mode == "hotspot") {
                    val activeZones = q.hotspots.mapIndexedNotNull { idx, z -> idx.takeIf { z.w > 0f && z.h > 0f } }
                    if (activeZones.size < 2) add(n, "mc.options", BuilderIssueLevel.BLOCKER, "در انتخاب روی تصویر باید دست‌کم دو ناحیه مشخص شود.")
                    val correct = q.correctIndex
                    if (correct == null || correct !in activeZones) {
                        add(n, "mc.correct", BuilderIssueLevel.BLOCKER, "گزینهٔ صحیح روی تصویر انتخاب نشده است.")
                    }
                } else {
                    val hasOptionContent = q.options.indices.map { idx ->
                        q.options[idx].trim().isNotEmpty() || !q.optionImages.getOrNull(idx).isNullOrBlank()
                    }
                    val filledCount = hasOptionContent.count { it }
                    if (filledCount < 2) add(n, "mc.options", BuilderIssueLevel.BLOCKER, "سؤال چندگزینه‌ای باید دست‌کم دو گزینه داشته باشد.")
                    if (hasOptionContent.any { !it }) add(n, "mc.blankOption", BuilderIssueLevel.WARNING, "یکی از گزینه‌ها خالی است.")
                    val textFilled = q.options.map { it.trim() }.filter { it.isNotEmpty() }
                    if (textFilled.distinct().size != textFilled.size) add(n, "mc.duplicate", BuilderIssueLevel.WARNING, "گزینه‌های تکراری وجود دارد.")
                    val correct = q.correctIndex
                    if (correct == null || correct !in q.options.indices || !hasOptionContent[correct]) {
                        add(n, "mc.correct", BuilderIssueLevel.BLOCKER, "گزینهٔ صحیح انتخاب نشده است (یا خالی است).")
                    }
                }
                QuestionType.TRUE_FALSE -> {
                    // V333.1 — در آزمون‌ساز بومی setTrueFalse مقدار «true/false» را در expectedText می‌گذارد؛
                    // هر دو مسیر (expectedText و correctIndex) معتبرند و فقط وقتی هیچ‌کدام تعیین نشده خطای بازدارنده است.
                    val hasExpected = q.expectedText.equals("true", ignoreCase = true) ||
                        q.expectedText.equals("false", ignoreCase = true)
                    val hasIndex = q.correctIndex != null && q.correctIndex in 0..1
                    if (!hasExpected && !hasIndex) {
                        add(n, "tf.correct", BuilderIssueLevel.BLOCKER, "پاسخ صحیح (درست/نادرست) انتخاب نشده است.")
                    }
                }
                QuestionType.FILL_BLANK -> if (q.mode == "order") {
                    if (q.orderItems.count { it.isNotBlank() } < 2) {
                        add(n, "fill.order", BuilderIssueLevel.BLOCKER, "در سؤال مرتب‌سازی دست‌کم دو مورد بنویسید.")
                    }
                } else {
                    if (q.expectedText.isBlank()) add(n, "fill.expected", BuilderIssueLevel.BLOCKER, "پاسخ درست جای خالی نوشته نشده است.")
                    if (q.expectedText.contains("  ")) add(n, "fill.spaces", BuilderIssueLevel.HINT, "در پاسخ جای خالی فاصلهٔ اضافه هست.")
                }
                QuestionType.NUMERIC -> {
                    val number = q.expectedNumber.trim().replace('٫', '.').replace('،', '.')
                    val parsed = number.toDoubleOrNull()
                    if (number.isBlank() || parsed == null) add(n, "numeric.expected", BuilderIssueLevel.BLOCKER, "پاسخ عددی معتبر نیست.")
                    val tol = q.tolerance.trim().replace('٫', '.').toDoubleOrNull()
                    if (tol == null) add(n, "numeric.tolerance", BuilderIssueLevel.WARNING, "مقدار خطای مجاز عدد نیست.")
                    else if (tol < 0) add(n, "numeric.toleranceNegative", BuilderIssueLevel.WARNING, "خطای مجاز منفی است.")
                }
                QuestionType.MATCHING -> {
                    val left = q.matchingLeft.indices.count { q.matchingLeft[it].isNotBlank() || !q.matchingLeftImages.getOrNull(it).isNullOrBlank() }
                    val right = q.matchingRight.indices.count { q.matchingRight[it].isNotBlank() || !q.matchingRightImages.getOrNull(it).isNullOrBlank() }
                    if (left < 2 || right < 2) add(n, "match.items", BuilderIssueLevel.BLOCKER, "سؤال جورکردنی باید دست‌کم دو مورد در هر ستون داشته باشد.")
                    if (left != right) add(n, "match.balance", BuilderIssueLevel.WARNING, "تعداد موارد دو ستون برابر نیست ($left و $right).")
                    val missing = (0 until left).filter { q.matchingPairs[it] == null }
                    if (missing.isNotEmpty()) add(n, "match.pairs", BuilderIssueLevel.BLOCKER, "${missing.size} مورد از ستون راست جور نشده است.")
                }
                QuestionType.ESSAY -> Unit
            }
        }

        // سؤال‌های کاملاً یکسان (کپی‌شده و فراموش‌شده)
        val seen = HashMap<String, Int>()
        questions.forEachIndexed { i, q ->
            val key = q.type.name + "|" + q.text.trim()
            if (key.length <= "ESSAY|".length) return@forEachIndexed
            val previous = seen.put(key, i + 1)
            if (previous != null) add(i + 1, "question.duplicate", BuilderIssueLevel.WARNING, "همان متن سؤال $previous تکرار شده است.")
        }

        val total = questions.sumOf { if (it.score.isNaN()) 0.0 else it.score }
        if (expectedTotal != null && Math.abs(total - expectedTotal) > 0.001) {
            add(0, "total.mismatch", BuilderIssueLevel.WARNING,
                "جمع نمره‌ها ${BuilderPreflightReport.formatScore(total)} است اما نمرهٔ کل آزمون ${BuilderPreflightReport.formatScore(expectedTotal)} ثبت شده.")
        }

        return BuilderPreflightReport(
            issues = issues.sortedWith(compareBy({ it.questionIndex }, { it.level.ordinal })),
            questionCount = questions.size,
            totalScore = total
        )
    }

    /** متن کامل گزارش برای کپی/ارسال به پشتیبانی. */
    fun asText(report: BuilderPreflightReport): String = buildString {
        appendLine("بررسی نهایی آزمون — ${report.summary}")
        if (report.isEmpty) appendLine("هیچ موردی یافت نشد.")
        report.issues.forEach { issue -> appendLine("• ${issue.where}: ${issue.message}") }
    }

    const val MAX_QUESTIONS = 120
    const val MAX_TEXT_LENGTH = 2000

    private fun QuestionDraft.hasInlineFigure(): Boolean =
        text.contains("qmf-fig") || text.contains("<svg") || text.contains("[شکل]")
}
