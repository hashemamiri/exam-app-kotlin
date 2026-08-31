package ir.exam.app.ui.printing

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * V73.0 — سازندهٔ داده‌های JSON برای صفحهٔ چاپ تعاملی HTML:
 * سؤالات و مشخصات سربرگ را از ساختارهای کاتلین (OfficialExamPrintable / QuestionDraft)
 * به آرایهٔ questions و فیلدهای فرم HTML نگاشت می‌کند (با کتابخانه خالص کاتلین).
 */
object ExamHtmlPrintPayloadBuilder {

    fun build(
        printable: OfficialExamPrintable
    ): JsonObject = buildJsonObject {
        put("template", "ministry")

        val h = printable.header
        val courseName = printable.subject.ifBlank { printable.documentTitle.ifBlank { "آزمون" } }
        val durationStr = if (printable.durationMinutes > 0) {
            "${printable.durationMinutes} دقیقه"
        } else {
            h.examDuration.takeIf(String::isNotBlank)?.let { "$it دقیقه" }.orEmpty()
        }
        val examDate = h.examDate

        val fields = buildJsonObject {
            // Template 7 (ministry - سربرگ وزارت آموزش و پرورش)
            put("h7_course", courseName)
            put("h7_examDate", examDate)
            put("h7_duration", durationStr)
            put("h7_grade", h.grade)
            put("h7_major", h.fieldOfStudy)
            put("h7_ministry", "وزارت آموزش و پرورش جمهوری اسلامی ایران")
            if (h.province.isNotBlank()) {
                put("h7_generalOffice", "اداره کل آموزش و پرورش استان ${h.province}")
            }
            if (h.city.isNotBlank()) {
                val dist = if (h.district.isNotBlank()) " (منطقه ${h.district})" else ""
                put("h7_districtOffice", "مدیریت آموزش و پرورش شهرستان ${h.city}$dist")
            }
            put("h7_schoolName", h.school)

            // Template 4 (school - سربرگ دبیرستان / آموزش و پرورش)
            put("h4_course", courseName)
            put("h4_schoolName", h.school)
            if (h.city.isNotBlank()) {
                put("h4_educationOffice", "مدیریت آموزش و پرورش ${h.city}")
            }
            put("h4_examDate", examDate)
            put("h4_duration", durationStr)
            val classField = listOf(h.grade, h.fieldOfStudy).filter(String::isNotBlank).joinToString(" / ")
            put("h4_class", classField)

            // Template 1 (classic) & Template 2 (formal) & Template 3 (sama) & Template 5 (edu) & Template 6 (detailed-school)
            put("f_course", courseName)
            put("f_branch", h.school)
            put("f_examDate", examDate)
            put("f_duration", durationStr)

            put("h2_course", courseName)
            put("h2_branch", h.school)
            put("h2_major", h.fieldOfStudy)
            put("h2_examDate", examDate)
            put("h2_duration", durationStr)

            put("h3_course", courseName)
            put("h3_unit", h.school)
            put("h3_major", h.fieldOfStudy)
            put("h3_examDate", examDate)
            put("h3_duration", durationStr)

            put("h5_course", courseName)
            put("h5_schoolName", h.school)
            put("h5_grade", h.grade)
            put("h5_branch", h.fieldOfStudy)
            put("h5_examDate", examDate)
            put("h5_duration", durationStr)

            put("h6_course", courseName)
            put("h6_schoolName", h.school)
            put("h6_grade", h.grade)
            put("h6_branch", h.fieldOfStudy)
            put("h6_examDate", examDate)
            put("h6_duration", durationStr)
        }
        put("fields", fields)

        val questionsArray = buildJsonArray {
            printable.questions.forEachIndexed { index, q ->
                add(buildJsonObject {
                    put("id", index + 1)
                    put("text", q.text)
                    put("score", formatScore(q.score))

                    when {
                        q.matchingLeft.isNotEmpty() || q.matchingRight.isNotEmpty() -> {
                            put("type", "matching")
                            put("pairs", buildJsonArray {
                                val maxLen = maxOf(q.matchingLeft.size, q.matchingRight.size)
                                for (i in 0 until maxLen) {
                                    add(buildJsonObject {
                                        put("left", q.matchingLeft.getOrElse(i) { "" })
                                        put("right", q.matchingRight.getOrElse(i) { "" })
                                    })
                                }
                            })
                        }
                        q.options.size == 2 && (q.options[0] == "صحیح" || q.options[0] == "غلط" || q.options.contains("صحیح")) -> {
                            put("type", "truefalse")
                            val correctText = q.answerText.orEmpty()
                            val isTrueCorrect = correctText.contains("صحیح") || correctText == "true"
                            put("options", buildJsonArray {
                                add(buildJsonObject {
                                    put("text", "صحیح")
                                    put("correct", isTrueCorrect)
                                })
                                add(buildJsonObject {
                                    put("text", "غلط")
                                    put("correct", !isTrueCorrect)
                                })
                            })
                        }
                        q.options.isNotEmpty() -> {
                            put("type", "multiple")
                            val correctAns = q.answerText.orEmpty().trim()
                            put("options", buildJsonArray {
                                q.options.forEachIndexed { optIdx, optText ->
                                    val isCorrect = if (correctAns.isNotBlank()) {
                                        correctAns == optText || correctAns == optText.trim() ||
                                        correctAns == "$optIdx" || correctAns == "${optIdx + 1}"
                                    } else false
                                    add(buildJsonObject {
                                        put("text", optText)
                                        put("correct", isCorrect)
                                    })
                                }
                            })
                            put("optionsLayout", if (q.options.size > 2) "2rows" else "1row")
                        }
                        q.answerText != null && q.answerText.any(Char::isDigit) && !q.answerText.contains("\n") -> {
                            put("type", "numeric")
                            put("answer", q.answerText.substringBefore(" ±").trim())
                            put("answerLineHeightCm", 0.75)
                        }
                        q.text.contains("[...]") || q.text.contains("...") || q.text.contains("___") -> {
                            put("type", "fill")
                        }
                        else -> {
                            put("type", "long")
                            put("answerLines", 6)
                            put("answerStyle", "lined")
                            put("answerLineHeightCm", 0.75)
                        }
                    }
                })
            }
        }
        put("questions", questionsArray)
    }

    /** ساخت Payload مستقیماً از لیست QuestionDraft. */
    fun buildFromDrafts(
        title: String,
        subject: String,
        durationMinutes: Int,
        header: OfficialPrintHeader,
        questions: List<QuestionDraft>
    ): JsonObject = buildJsonObject {
        put("template", "ministry")

        val courseName = subject.ifBlank { title.ifBlank { "آزمون" } }
        val durationStr = if (durationMinutes > 0) {
            "$durationMinutes دقیقه"
        } else {
            header.examDuration.takeIf(String::isNotBlank)?.let { "$it دقیقه" }.orEmpty()
        }
        val examDate = header.examDate

        val fields = buildJsonObject {
            put("h7_course", courseName)
            put("h7_examDate", examDate)
            put("h7_duration", durationStr)
            put("h7_grade", header.grade)
            put("h7_major", header.fieldOfStudy)
            put("h7_ministry", "وزارت آموزش و پرورش جمهوری اسلامی ایران")
            if (header.province.isNotBlank()) {
                put("h7_generalOffice", "اداره کل آموزش و پرورش استان ${header.province}")
            }
            if (header.city.isNotBlank()) {
                val dist = if (header.district.isNotBlank()) " (منطقه ${header.district})" else ""
                put("h7_districtOffice", "مدیریت آموزش و پرورش شهرستان ${header.city}$dist")
            }
            put("h7_schoolName", header.school)

            put("h4_course", courseName)
            put("h4_schoolName", header.school)
            if (header.city.isNotBlank()) {
                put("h4_educationOffice", "مدیریت آموزش و پرورش ${header.city}")
            }
            put("h4_examDate", examDate)
            put("h4_duration", durationStr)
            val classField = listOf(header.grade, header.fieldOfStudy).filter(String::isNotBlank).joinToString(" / ")
            put("h4_class", classField)

            put("f_course", courseName)
            put("f_branch", header.school)
            put("f_examDate", examDate)
            put("f_duration", durationStr)

            put("h2_course", courseName)
            put("h2_branch", header.school)
            put("h2_major", header.fieldOfStudy)
            put("h2_examDate", examDate)
            put("h2_duration", durationStr)

            put("h3_course", courseName)
            put("h3_unit", header.school)
            put("h3_major", header.fieldOfStudy)
            put("h3_examDate", examDate)
            put("h3_duration", durationStr)

            put("h5_course", courseName)
            put("h5_schoolName", header.school)
            put("h5_grade", header.grade)
            put("h5_branch", header.fieldOfStudy)
            put("h5_examDate", examDate)
            put("h5_duration", durationStr)

            put("h6_course", courseName)
            put("h6_schoolName", header.school)
            put("h6_grade", header.grade)
            put("h6_branch", header.fieldOfStudy)
            put("h6_examDate", examDate)
            put("h6_duration", durationStr)
        }
        put("fields", fields)

        val questionsArray = buildJsonArray {
            questions.forEachIndexed { index, q ->
                add(buildJsonObject {
                    put("id", index + 1)
                    put("text", q.text)
                    put("score", formatScore(q.score))

                    when (q.type) {
                        QuestionType.MULTIPLE_CHOICE -> {
                            put("type", "multiple")
                            put("options", buildJsonArray {
                                q.options.forEachIndexed { optIdx, optText ->
                                    add(buildJsonObject {
                                        put("text", optText)
                                        put("correct", q.correctIndex == optIdx)
                                    })
                                }
                            })
                            put("optionsLayout", if (q.options.size > 2) "2rows" else "1row")
                        }
                        QuestionType.TRUE_FALSE -> {
                            put("type", "truefalse")
                            val isTrueCorrect = q.expectedText == "true" || q.correctIndex == 0
                            put("options", buildJsonArray {
                                add(buildJsonObject {
                                    put("text", "صحیح")
                                    put("correct", isTrueCorrect)
                                })
                                add(buildJsonObject {
                                    put("text", "غلط")
                                    put("correct", !isTrueCorrect)
                                })
                            })
                        }
                        QuestionType.ESSAY -> {
                            put("type", "long")
                            put("answerLines", if (q.answerLines in 1..30) q.answerLines else 6)
                            put("answerStyle", if (q.answerLineStyle == "plain") "plain" else "lined")
                            put("answerLineHeightCm", 0.75)
                        }
                        QuestionType.FILL_BLANK -> {
                            put("type", "fill")
                        }
                        QuestionType.NUMERIC -> {
                            put("type", "numeric")
                            put("answer", q.expectedNumber.ifBlank { q.expectedText })
                            put("answerLineHeightCm", 0.75)
                        }
                        QuestionType.MATCHING -> {
                            put("type", "matching")
                            put("pairs", buildJsonArray {
                                val maxLen = maxOf(q.matchingLeft.size, q.matchingRight.size)
                                for (i in 0 until maxLen) {
                                    add(buildJsonObject {
                                        val leftText = q.matchingLeft.getOrElse(i) { "" }
                                        val rightIdx = q.matchingPairs[i] ?: i
                                        val rightText = q.matchingRight.getOrElse(rightIdx) { "" }
                                        put("left", leftText)
                                        put("right", rightText)
                                    })
                                }
                            })
                        }
                    }
                })
            }
        }
        put("questions", questionsArray)
    }

    private fun formatScore(score: Double): String =
        if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
}
