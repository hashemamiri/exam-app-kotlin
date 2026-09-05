package ir.exam.app.ui.printing

import ir.exam.app.domain.model.OfficialExamPrintable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * V76.0 — سازندهٔ دادهٔ JSON برای «نسخهٔ 30» (asset چاپ/آزمون‌ساز تعاملی print/exam_print.html):
 * سؤالات و فیلدهای سربرگ از ساختارهای کاتلین (OfficialExamPrintable) به قالبِ بومیِ
 * همان فایل نگاشت می‌شوند؛ فایل مقصد با window.setExamData آن را مصرف می‌کند.
 *
 * - printable == null یعنی «آزمون جدید»: payload ریست (پاک‌سازی حالت و پیش‌نویس محلی فایل).
 * - فیلدهای سربرگ با شناسه‌های واقعی فرم فایل (f_headerTemplate/f_course/f_branch/
 *   f_examDate/f_duration) پر می‌شوند؛ سربرگ کامل داخل خود فایل ویرایش می‌شود.
 * - شکل‌ها (توکن‌های %%FIG%% در متن سؤال) عیناً عبور می‌کنند؛ موتورهای رندر در فایل
 *   مقصد حفظ شده‌اند. تصاویر سؤال به‌صورت توکن data-URL پیشاپیش در متن قرار می‌گیرند
 *   (ExamHtmlImageInliner) تا با باکت خصوصی تصاویر هم کار کنند.
 * - کاملاً مستقل از پلتفرم (kotlinx.serialization) و تست‌پذیر روی JVM.
 */
object ExamHtmlPrintPayloadBuilder {

    /**
     * V86.8 — `extraHeaderFields` همان ۱۶ میدانِ «تنظیمات سربرگ» است که کاربر
     * روی دستگاه ذخیره کرده. مدلِ `OfficialPrintHeader` فقط چهار میدان دارد،
     * پس بقیه (استاد، گروه آموزشی، نوعِ امتحان، تاریخِ اعلامِ نمرات و…) از این
     * راه به فرمِ فایل می‌رسند. خالی بودنش یعنی رفتارِ قبلی، بیت‌به‌بیت.
     */
    fun build(
        printable: OfficialExamPrintable?,
        extraHeaderFields: Map<String, String> = emptyMap()
    ): JsonObject {
        if (printable == null) {
            return buildJsonObject {
                put("reset", true)
            }
        }
        val h = printable.header
        val courseName = printable.subject.ifBlank { printable.documentTitle.ifBlank { "آزمون" } }
        val durationStr = if (printable.durationMinutes > 0) {
            "${printable.durationMinutes} دقیقه"
        } else {
            h.examDuration.takeIf(String::isNotBlank)?.let { "$it دقیقه" }.orEmpty()
        }

        return buildJsonObject {
            put("reset", false)
            put("fields", buildJsonObject {
                // V76.0 — مقادیر معتبر select#f_headerTemplate در نسخهٔ 30: classic/formal/sama/school/edu/detailed-school؛ («ministry» مال asset قدیمی است).
                put("f_headerTemplate", "classic")
                put("f_course", courseName)
                put("f_branch", h.school)
                put("f_examDate", h.examDate)
                put("f_duration", durationStr)
                // V86.8 — میدان‌های ذخیره‌شدهٔ سربرگ. آخر می‌آیند تا مقدارِ
                // صریحِ کاربر بر مقدارِ مشتق‌شده از آزمون بچربد؛ ولی رشتهٔ خالی
                // نباید مقدارِ خوبِ بالا را پاک کند.
                extraHeaderFields.forEach { (key, value) ->
                    if (key.startsWith("f_") && value.isNotBlank()) put(key, value)
                }
            })
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
                                put("answerLines", q.answerLines.coerceIn(1, 30))
                                put("answerStyle", if (q.answerLineStyle == "plain") "plain" else "lined")
                                put("answerLineHeightCm", 0.75)
                            }
                        }
                    })
                }
            }
            put("questions", questionsArray)
            put("qIdCounter", printable.questions.size)
        }
    }

    private fun formatScore(score: Double): String =
        if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
}
