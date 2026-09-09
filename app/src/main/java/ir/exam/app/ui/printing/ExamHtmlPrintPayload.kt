package ir.exam.app.ui.printing

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintQuestion
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * قرارداد دادهٔ موتور مستقل چاپ (`exam_print_renderer.html`).
 *
 * ورودی فقط ساختار چاپیِ canonical است؛ هیچ snapshot یا merge مخصوص چاپ در
 * این مسیر وجود ندارد. متن خام سؤال‌ها و توکن‌های شکل عبور می‌کنند و موتور
 * HTML آن‌ها را با رندررهای بومی نمایش می‌دهد.
 */
object ExamHtmlPrintPayloadBuilder {

    fun build(
        printable: OfficialExamPrintable?,
        extraHeaderFields: Map<String, String> = emptyMap(),
        /** V121 — تنظیمات صفحهٔ موتور چاپ (JSON خامِ [ir.exam.app.data.local.PrintPageSetup.toJson]). */
        pageSetupJson: String? = null
    ): JsonObject {
        if (printable == null) return buildJsonObject { put("reset", true) }

        val header = printable.header
        val courseName = printable.subject.ifBlank { printable.documentTitle.ifBlank { "آزمون" } }
        val duration = when {
            printable.durationMinutes > 0 -> "${printable.durationMinutes} دقیقه"
            header.examDuration.isNotBlank() -> "${header.examDuration} دقیقه"
            else -> ""
        }

        return buildJsonObject {
            put("reset", false)
            put("documentTitle", printable.documentTitle.ifBlank { "آزمون" })
            put("footerNote", printable.footerNote)
            put("totalScore", formatScore(printable.totalScore))
            put("includeAnswerKey", printable.includeAnswerKey)
            if (pageSetupJson != null) put("pageSetup", pageSetupJson)
            put("fields", buildJsonObject {

                put("f_headerTemplate", "classic")
                put("f_course", courseName)
                put("f_branch", header.school)
                put("f_examDate", header.examDate)
                put("f_duration", duration)
                // فقط شناسه‌های شِمای سربرگ پذیرفته می‌شوند. مقدار خالی نباید
                // مقدار مشتق‌شدهٔ قابل‌استفاده را پاک کند.
                extraHeaderFields.forEach { (key, value) ->
                    if (key in HEADER_FIELD_IDS && value.isNotBlank()) put(key, value)
                }
            })
            put("questions", buildJsonArray {
                printable.questions.forEachIndexed { index, question ->
                    add(questionJson(index, question))
                }
            })
        }
    }

    private fun questionJson(index: Int, question: OfficialPrintQuestion) = buildJsonObject {
        put("id", index + 1)
        put("text", question.text)
        put("score", formatScore(question.score))
        put("textAlign", question.textAlign)
        put("fontFamily", question.fontFamily)
        put("fontSizeSp", question.fontSizeSp)
        put("bold", question.bold)
        put("italic", question.italic)
        if (question.answerText != null) put("answer", question.answerText)
        if (question.figLayoutsJson.isNotBlank()) put("figLayoutsJson", question.figLayoutsJson)
        if (question.sepExtraPx > 0) put("sepExtraPx", question.sepExtraPx)
        if (question.textSpans.isNotEmpty()) {
            put("textSpans", buildJsonArray {
                question.textSpans.forEach { span ->
                    add(buildJsonObject {
                        put("start", span.start)
                        put("end", span.end)
                        put("bold", span.bold)
                        put("italic", span.italic)
                        if (span.underline) put("underline", true)
                        span.color?.let { put("color", it) }
                        span.size?.let { put("size", it) }
                        span.font?.let { put("font", it) }
                    })
                }
            })
        }
        // V121 — تراز پاراگرافیِ تکه‌ای؛ اگر چیزی تنظیم نشده، رندرر از textAlign
        // کلیِ سؤال استفاده می‌کند (رجوع کنید به exam_print_renderer.html).
        if (question.alignSpans.isNotEmpty()) {
            put("alignSpans", buildJsonArray {
                question.alignSpans.forEach { span ->
                    add(buildJsonObject {
                        put("start", span.start)
                        put("end", span.end)
                        put("align", span.align)
                    })
                }
            })
        }
        // V120 — منبعِ اصلیِ تشخیصِ نوع اکنون فیلدِ صریح `questionType` است
        // (از `PrintableFromDrafts` می‌آید). قبلاً هیچ نشانهٔ صریحی نبود و
        // نوع فقط از روی محتوا حدس زده می‌شد؛ این حدس دو باگ داشت:
        //  ۱) سؤال «صحیح/غلط» چون هیچ‌وقت `options` نداشت، هیچ‌گاه با شرطِ
        //     زیر تطبیق نمی‌یافت و به‌اشتباه «تشریحی» چاپ می‌شد.
        //  ۲) هر سؤالِ تشریحیِ حاویِ «...» (خیلی رایج در فارسی) به‌اشتباه
        //     «جای‌خالی» تشخیص داده می‌شد.
        // برای سازگاری با فراخوان‌های قدیمی/تست‌هایی که `questionType` را
        // نمی‌فرستند (null)، حدسِ قبلی به‌عنوان fallback باقی مانده — با
        // اصلاح ۲ (نشانهٔ ضعیفِ «...» ساده حذف شد، فقط نشانه‌های صریحِ
        // «[...]» و «___» می‌مانند).
        when (question.questionType ?: heuristicType(question)) {
            "matching" -> {
                put("type", "matching")
                put("pairs", buildJsonArray {
                    val count = maxOf(question.matchingLeft.size, question.matchingRight.size)
                    repeat(count) { itemIndex ->
                        add(buildJsonObject {
                            put("left", question.matchingLeft.getOrElse(itemIndex) { "" })
                            put("right", question.matchingRight.getOrElse(itemIndex) { "" })
                            question.matchingLeftStyles.getOrNull(itemIndex)?.let { style ->
                                put("leftBold", style.first)
                                put("leftItalic", style.second)
                                style.third?.let { put("leftSize", it) }
                            }
                            question.matchingRightStyles.getOrNull(itemIndex)?.let { style ->
                                put("rightBold", style.first)
                                put("rightItalic", style.second)
                                style.third?.let { put("rightSize", it) }
                            }
                        })
                    }
                })
            }
            "truefalse" -> {
                put("type", "truefalse")
                val correct = question.answerText.orEmpty()
                val trueCorrect = correct.contains("صحیح") || correct == "true"
                put("options", buildJsonArray {
                    add(optionJson("صحیح", trueCorrect, null))
                    add(optionJson("غلط", !trueCorrect, null))
                })
            }
            "multiple" -> {
                put("type", "multiple")
                val correct = question.answerText.orEmpty().trim()
                put("options", buildJsonArray {
                    question.options.forEachIndexed { optionIndex, optionText ->
                        val isCorrect = correct.isNotBlank() && (
                            correct == optionText || correct == optionText.trim() ||
                                correct == "$optionIndex" || correct == "${optionIndex + 1}"
                            )
                        add(optionJson(optionText, isCorrect, question.optionStyles.getOrNull(optionIndex)))
                    }
                })
                put("optionsLayout", if (question.options.size > 2) "2rows" else "1row")
            }
            "numeric" -> {
                put("type", "numeric")
                put("answerLines", question.answerLines.coerceIn(0, 30))
                put("answerStyle", when (question.answerLineStyle) { "blank", "plain" -> "plain"; "grid" -> "grid"; else -> "lined" })
                put("answerLineSpacingCm", question.answerLineSpacingCm.coerceIn(0.5f, 2.0f))
            }
            "fill" -> {
                put("type", "fill")
                put("answerLines", question.answerLines.coerceIn(0, 30))
                put("answerStyle", when (question.answerLineStyle) { "blank", "plain" -> "plain"; "grid" -> "grid"; else -> "lined" })
                put("answerLineSpacingCm", question.answerLineSpacingCm.coerceIn(0.5f, 2.0f))
            }
            else -> {
                put("type", "long")
                put("answerLines", question.answerLines.coerceIn(0, 30))
                put("answerStyle", when (question.answerLineStyle) { "blank", "plain" -> "plain"; "grid" -> "grid"; else -> "lined" })
                put("answerLineSpacingCm", question.answerLineSpacingCm.coerceIn(0.5f, 2.0f))
            }
        }
    }

    /**
     * V120 — حدسِ نوعِ سؤال، فقط وقتی `questionType` صریح نیست (سازگاریِ
     * عقب‌رو). نسبت به نسخهٔ قبلی یک اصلاح دارد: نشانهٔ ضعیفِ «هر رشتهٔ
     * حاویِ سه‌نقطهٔ ساده» حذف شده چون سؤال‌های تشریحیِ عادیِ فارسی هم
     * معمولاً «...» دارند و به‌اشتباه «جای‌خالی» تشخیص داده می‌شدند؛
     * نشانه‌های صریح «[...]» و «___» باقی مانده‌اند.
     */
    private fun heuristicType(question: OfficialPrintQuestion): String = when {
        question.matchingLeft.isNotEmpty() || question.matchingRight.isNotEmpty() -> "matching"
        question.options.size == 2 &&
            (question.options.firstOrNull() == "صحیح" || question.options.contains("صحیح")) -> "truefalse"
        question.options.isNotEmpty() -> "multiple"
        question.answerText != null && question.answerText.any(Char::isDigit) && !question.answerText.contains("\n") -> "numeric"
        question.text.contains("[...]") || question.text.contains("___") -> "fill"
        else -> "long"
    }

    private fun optionJson(
        text: String,
        correct: Boolean,
        style: Triple<Boolean, Boolean, Float?>?
    ) = buildJsonObject {
        put("text", text)
        put("correct", correct)
        style?.let {
            put("bold", it.first)
            put("italic", it.second)
            it.third?.let { size -> put("size", size) }
        }
    }

    private fun formatScore(score: Double): String =
        if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()

    /** همهٔ شناسه‌های مجاز assets/print/header_settings_schema.json. */
    private val HEADER_FIELD_IDS = setOf(
        "f_headerTemplate", "f_name", "f_studentId", "f_professor", "f_course", "f_department",
        "f_examType", "f_branch", "f_studentCount", "f_examDate", "f_examTime", "f_duration",
        "f_tools", "f_sheets", "f_gradesDate", "f_intro",
        "h2_branch", "h2_chair", "h2_area", "h2_examLine", "h2_name", "h2_studentId",
        "h2_major", "h2_degree", "h2_course", "h2_professor", "h2_examDate", "h2_duration",
        "h2_totalScore", "h2_tools", "h2_intro",
        "h3_unit", "h3_office", "h3_semester", "h3_quote", "h3_course", "h3_professor",
        "h3_examDate", "h3_examTime", "h3_duration", "h3_name", "h3_studentId", "h3_degree",
        "h3_major", "h3_chair", "h3_intro",
        "h4_page", "h4_examTurn", "h4_course", "h4_educationOffice", "h4_schoolName",
        "h4_schoolYear", "h4_examDay", "h4_examDate", "h4_duration", "h4_startTime", "h4_name",
        "h4_father", "h4_class", "h4_note", "h4_intro",
        "h5_firstName", "h5_lastName", "h5_father", "h5_course", "h5_page", "h5_examTurn",
        "h5_grade", "h5_branch", "h5_examDate", "h5_startTime", "h5_duration", "h5_generalOffice",
        "h5_assessment", "h5_districtOffice", "h5_schoolName", "h5_intro",
        "h6_name", "h6_family", "h6_father", "h6_studentNo", "h6_course", "h6_educationOffice",
        "h6_schoolName", "h6_sealText", "h6_grade", "h6_branch", "h6_examDate", "h6_examTime",
        "h6_duration", "h6_pageCount", "h6_pageNumber", "h6_intro",
        "h7_name", "h7_family", "h7_father", "h7_course", "h7_examDate", "h7_duration", "h7_grade",
        "h7_major", "h7_ministry", "h7_generalOffice", "h7_districtOffice", "h7_schoolName", "h7_intro",
        "opt_footerText"
    )
}
