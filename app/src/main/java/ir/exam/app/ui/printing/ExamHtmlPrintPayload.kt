package ir.exam.app.ui.printing

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import org.json.JSONArray
import org.json.JSONObject

/**
 * V73.0 — سازندهٔ داده‌های JSON برای صفحهٔ چاپ تعاملی HTML:
 * سؤالات و مشخصات سربرگ را از ساختارهای کاتلین (OfficialExamPrintable / QuestionDraft)
 * به آرایهٔ questions و فیلدهای فرم HTML نگاشت می‌کند.
 */
object ExamHtmlPrintPayloadBuilder {

    fun build(
        printable: OfficialExamPrintable
    ): JSONObject {
        val root = JSONObject()
        root.put("template", "ministry")

        val fields = JSONObject()
        val h = printable.header
        val courseName = printable.subject.ifBlank { printable.documentTitle.ifBlank { "آزمون" } }
        val durationStr = if (printable.durationMinutes > 0) {
            "${printable.durationMinutes} دقیقه"
        } else {
            h.examDuration.takeIf(String::isNotBlank)?.let { "$it دقیقه" }.orEmpty()
        }
        val examDate = h.examDate

        // Template 7 (ministry - سربرگ وزارت آموزش و پرورش)
        fields.put("h7_course", courseName)
        fields.put("h7_examDate", examDate)
        fields.put("h7_duration", durationStr)
        fields.put("h7_grade", h.grade)
        fields.put("h7_major", h.fieldOfStudy)
        fields.put("h7_ministry", "وزارت آموزش و پرورش جمهوری اسلامی ایران")
        if (h.province.isNotBlank()) {
            fields.put("h7_generalOffice", "اداره کل آموزش و پرورش استان ${h.province}")
        }
        if (h.city.isNotBlank()) {
            val dist = if (h.district.isNotBlank()) " (منطقه ${h.district})" else ""
            fields.put("h7_districtOffice", "مدیریت آموزش و پرورش شهرستان ${h.city}$dist")
        }
        fields.put("h7_schoolName", h.school)

        // Template 4 (school - سربرگ دبیرستان / آموزش و پرورش)
        fields.put("h4_course", courseName)
        fields.put("h4_schoolName", h.school)
        if (h.city.isNotBlank()) {
            fields.put("h4_educationOffice", "مدیریت آموزش و پرورش ${h.city}")
        }
        fields.put("h4_examDate", examDate)
        fields.put("h4_duration", durationStr)
        val classField = listOf(h.grade, h.fieldOfStudy).filter(String::isNotBlank).joinToString(" / ")
        fields.put("h4_class", classField)

        // Template 1 (classic) & Template 2 (formal) & Template 3 (sama) & Template 5 (edu) & Template 6 (detailed-school)
        fields.put("f_course", courseName)
        fields.put("f_branch", h.school)
        fields.put("f_examDate", examDate)
        fields.put("f_duration", durationStr)

        fields.put("h2_course", courseName)
        fields.put("h2_branch", h.school)
        fields.put("h2_major", h.fieldOfStudy)
        fields.put("h2_examDate", examDate)
        fields.put("h2_duration", durationStr)

        fields.put("h3_course", courseName)
        fields.put("h3_unit", h.school)
        fields.put("h3_major", h.fieldOfStudy)
        fields.put("h3_examDate", examDate)
        fields.put("h3_duration", durationStr)

        fields.put("h5_course", courseName)
        fields.put("h5_schoolName", h.school)
        fields.put("h5_grade", h.grade)
        fields.put("h5_branch", h.fieldOfStudy)
        fields.put("h5_examDate", examDate)
        fields.put("h5_duration", durationStr)

        fields.put("h6_course", courseName)
        fields.put("h6_schoolName", h.school)
        fields.put("h6_grade", h.grade)
        fields.put("h6_branch", h.fieldOfStudy)
        fields.put("h6_examDate", examDate)
        fields.put("h6_duration", durationStr)

        root.put("fields", fields)

        val questionsArray = JSONArray()
        printable.questions.forEachIndexed { index, q ->
            val qObj = JSONObject()
            qObj.put("id", index + 1)
            qObj.put("text", q.text)
            qObj.put("score", formatScore(q.score))

            when {
                q.matchingLeft.isNotEmpty() || q.matchingRight.isNotEmpty() -> {
                    qObj.put("type", "matching")
                    val pairs = JSONArray()
                    val maxLen = maxOf(q.matchingLeft.size, q.matchingRight.size)
                    for (i in 0 until maxLen) {
                        val pair = JSONObject()
                        pair.put("left", q.matchingLeft.getOrElse(i) { "" })
                        pair.put("right", q.matchingRight.getOrElse(i) { "" })
                        pairs.put(pair)
                    }
                    qObj.put("pairs", pairs)
                }
                q.options.size == 2 && (q.options[0] == "صحیح" || q.options[0] == "غلط" || q.options.contains("صحیح")) -> {
                    qObj.put("type", "truefalse")
                    val opts = JSONArray()
                    val correctText = q.answerText.orEmpty()
                    val isTrueCorrect = correctText.contains("صحیح") || correctText == "true"
                    opts.put(JSONObject().apply {
                        put("text", "صحیح")
                        put("correct", isTrueCorrect)
                    })
                    opts.put(JSONObject().apply {
                        put("text", "غلط")
                        put("correct", !isTrueCorrect)
                    })
                    qObj.put("options", opts)
                }
                q.options.isNotEmpty() -> {
                    qObj.put("type", "multiple")
                    val opts = JSONArray()
                    val correctAns = q.answerText.orEmpty().trim()
                    q.options.forEachIndexed { optIdx, optText ->
                        val optObj = JSONObject()
                        optObj.put("text", optText)
                        val isCorrect = if (correctAns.isNotBlank()) {
                            correctAns == optText || correctAns == optText.trim() ||
                            correctAns == "$optIdx" || correctAns == "${optIdx + 1}"
                        } else false
                        optObj.put("correct", isCorrect)
                        opts.put(optObj)
                    }
                    qObj.put("options", opts)
                    qObj.put("optionsLayout", if (q.options.size > 2) "2rows" else "1row")
                }
                q.answerText != null && q.answerText.any(Char::isDigit) && !q.answerText.contains("\n") -> {
                    qObj.put("type", "numeric")
                    qObj.put("answer", q.answerText.substringBefore(" ±").trim())
                    qObj.put("answerLineHeightCm", 0.75)
                }
                q.text.contains("[...]") || q.text.contains("...") || q.text.contains("___") -> {
                    qObj.put("type", "fill")
                }
                else -> {
                    qObj.put("type", "long")
                    qObj.put("answerLines", 6)
                    qObj.put("answerStyle", "lined")
                    qObj.put("answerLineHeightCm", 0.75)
                }
            }
            questionsArray.put(qObj)
        }
        root.put("questions", questionsArray)

        return root
    }

    /** ساخت Payload مستقیماً از لیست QuestionDraft. */
    fun buildFromDrafts(
        title: String,
        subject: String,
        durationMinutes: Int,
        header: OfficialPrintHeader,
        questions: List<QuestionDraft>
    ): JSONObject {
        val root = JSONObject()
        root.put("template", "ministry")

        val fields = JSONObject()
        val courseName = subject.ifBlank { title.ifBlank { "آزمون" } }
        val durationStr = if (durationMinutes > 0) {
            "$durationMinutes دقیقه"
        } else {
            header.examDuration.takeIf(String::isNotBlank)?.let { "$it دقیقه" }.orEmpty()
        }
        val examDate = header.examDate

        fields.put("h7_course", courseName)
        fields.put("h7_examDate", examDate)
        fields.put("h7_duration", durationStr)
        fields.put("h7_grade", header.grade)
        fields.put("h7_major", header.fieldOfStudy)
        fields.put("h7_ministry", "وزارت آموزش و پرورش جمهوری اسلامی ایران")
        if (header.province.isNotBlank()) {
            fields.put("h7_generalOffice", "اداره کل آموزش و پرورش استان ${header.province}")
        }
        if (header.city.isNotBlank()) {
            val dist = if (header.district.isNotBlank()) " (منطقه ${header.district})" else ""
            fields.put("h7_districtOffice", "مدیریت آموزش و پرورش شهرستان ${header.city}$dist")
        }
        fields.put("h7_schoolName", header.school)

        fields.put("h4_course", courseName)
        fields.put("h4_schoolName", header.school)
        if (header.city.isNotBlank()) {
            fields.put("h4_educationOffice", "مدیریت آموزش و پرورش ${header.city}")
        }
        fields.put("h4_examDate", examDate)
        fields.put("h4_duration", durationStr)
        val classField = listOf(header.grade, header.fieldOfStudy).filter(String::isNotBlank).joinToString(" / ")
        fields.put("h4_class", classField)

        fields.put("f_course", courseName)
        fields.put("f_branch", header.school)
        fields.put("f_examDate", examDate)
        fields.put("f_duration", durationStr)

        fields.put("h2_course", courseName)
        fields.put("h2_branch", header.school)
        fields.put("h2_major", header.fieldOfStudy)
        fields.put("h2_examDate", examDate)
        fields.put("h2_duration", durationStr)

        fields.put("h3_course", courseName)
        fields.put("h3_unit", header.school)
        fields.put("h3_major", header.fieldOfStudy)
        fields.put("h3_examDate", examDate)
        fields.put("h3_duration", durationStr)

        fields.put("h5_course", courseName)
        fields.put("h5_schoolName", header.school)
        fields.put("h5_grade", header.grade)
        fields.put("h5_branch", header.fieldOfStudy)
        fields.put("h5_examDate", examDate)
        fields.put("h5_duration", durationStr)

        fields.put("h6_course", courseName)
        fields.put("h6_schoolName", header.school)
        fields.put("h6_grade", header.grade)
        fields.put("h6_branch", header.fieldOfStudy)
        fields.put("h6_examDate", examDate)
        fields.put("h6_duration", durationStr)

        root.put("fields", fields)

        val questionsArray = JSONArray()
        questions.forEachIndexed { index, q ->
            val qObj = JSONObject()
            qObj.put("id", index + 1)
            qObj.put("text", q.text)
            qObj.put("score", formatScore(q.score))

            when (q.type) {
                QuestionType.MULTIPLE_CHOICE -> {
                    qObj.put("type", "multiple")
                    val opts = JSONArray()
                    q.options.forEachIndexed { optIdx, optText ->
                        val optObj = JSONObject()
                        optObj.put("text", optText)
                        optObj.put("correct", q.correctIndex == optIdx)
                        opts.put(optObj)
                    }
                    qObj.put("options", opts)
                    qObj.put("optionsLayout", if (q.options.size > 2) "2rows" else "1row")
                }
                QuestionType.TRUE_FALSE -> {
                    qObj.put("type", "truefalse")
                    val opts = JSONArray()
                    val isTrueCorrect = q.expectedText == "true" || q.correctIndex == 0
                    opts.put(JSONObject().apply {
                        put("text", "صحیح")
                        put("correct", isTrueCorrect)
                    })
                    opts.put(JSONObject().apply {
                        put("text", "غلط")
                        put("correct", !isTrueCorrect)
                    })
                    qObj.put("options", opts)
                }
                QuestionType.ESSAY -> {
                    qObj.put("type", "long")
                    qObj.put("answerLines", if (q.answerLines in 1..30) q.answerLines else 6)
                    qObj.put("answerStyle", if (q.answerLineStyle == "plain") "plain" else "lined")
                    qObj.put("answerLineHeightCm", 0.75)
                }
                QuestionType.FILL_BLANK -> {
                    qObj.put("type", "fill")
                }
                QuestionType.NUMERIC -> {
                    qObj.put("type", "numeric")
                    qObj.put("answer", q.expectedNumber.ifBlank { q.expectedText })
                    qObj.put("answerLineHeightCm", 0.75)
                }
                QuestionType.MATCHING -> {
                    qObj.put("type", "matching")
                    val pairs = JSONArray()
                    val maxLen = maxOf(q.matchingLeft.size, q.matchingRight.size)
                    for (i in 0 until maxLen) {
                        val pair = JSONObject()
                        val leftText = q.matchingLeft.getOrElse(i) { "" }
                        val rightIdx = q.matchingPairs[i] ?: i
                        val rightText = q.matchingRight.getOrElse(rightIdx) { "" }
                        pair.put("left", leftText)
                        pair.put("right", rightText)
                        pairs.put(pair)
                    }
                    qObj.put("pairs", pairs)
                }
            }
            questionsArray.put(qObj)
        }
        root.put("questions", questionsArray)

        return root
    }

    private fun formatScore(score: Double): String =
        if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
}
