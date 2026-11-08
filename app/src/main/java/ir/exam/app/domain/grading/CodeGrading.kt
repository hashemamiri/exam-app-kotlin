package ir.exam.app.domain.grading

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * V356 — تصحیح کد، فاز صفر (بدون اجرای کد).
 *
 * آینهٔ دقیق site/src/codegrade.js. بردارهای آزمون مشترک دو پیاده‌سازی
 * را به هم می‌بندد. **کد دانش‌آموز هرگز اجرا نمی‌شود**؛ معلم نتیجهٔ هر
 * مورد آزمون را خودش علامت می‌زند و این فایل فقط از وزن موردهای
 * قبول‌شده یک پیشنهاد نمره می‌سازد. اعمال و ذخیرهٔ نمره با معلم است.
 */
object CodeGrading {
    const val STEP = 0.25
    const val MAX_TESTS = 40
    const val MAX_TEXT = 4000
    const val MAX_LABEL = 120
    const val MAX_REFERENCE = 20000

    data class Options(
        val trimLines: Boolean = true,
        val collapseBlank: Boolean = true,
        val ignoreCase: Boolean = false
    )

    private val LINE_SPLIT = Regex("\r\n|\r|\n")
    private val WEIGHT_RE = Regex("^[0-9]+(\\.[0-9]+)?$")

    data class TestCase(
        val label: String = "",
        val input: String = "",
        val expected: String = "",
        val weight: Double = 1.0,
        val hidden: Boolean = false
    )

    data class ParseError(val line: Int, val message: String)
    data class ParseResult(val tests: List<TestCase>, val errors: List<ParseError>)

    /**
     * یکسان‌سازی خروجی برای مقایسه.
     * trimLines: فاصلهٔ ابتدا/انتهای هر خط برداشته می‌شود.
     * collapseBlank: خط‌های خالی حذف می‌شوند.
     * ignoreCase: بزرگی/کوچکی حرف لاتین نادیده گرفته می‌شود.
     */
    fun normalizeOutput(text: String?, o: Options = Options()): String {
        val lines = LINE_SPLIT.split(text.orEmpty())
        val out = mutableListOf<String>()
        for (raw in lines) {
            val line = if (o.trimLines) raw.trim(' ', '\t') else raw
            if (o.collapseBlank && line.all { it == ' ' || it == '\t' }) continue
            out.add(line)
        }
        var joined = out.joinToString("\n")
        if (o.ignoreCase) joined = joined.lowercase()
        // پیرایش پایانی تنها با روشن بودن پیرایش؛ وگرنه خاموش‌کردن آن بی‌اثر می‌شد
        return if (o.trimLines) joined.trim(' ', '\t', '\n') else joined
    }

    fun outputMatches(expected: String?, actual: String?, o: Options = Options()): Boolean =
        normalizeOutput(expected, o) == normalizeOutput(actual, o)

    private fun roundStep(v: Double) = Math.round(v / STEP) * STEP

    data class Check(val passed: Boolean)

    data class ScoreResult(
        val suggested: Double,
        val ratio: Double,
        val passedWeight: Double,
        val totalWeight: Double,
        val checkedCount: Int,
        val passedCount: Int,
        val totalCount: Int,
        val complete: Boolean,
        val hasTests: Boolean
    )

    /** یک مورد آزمون با شناسهٔ سرور و وزن. */
    data class WeightedTest(val id: Long, val weight: Double)

    /**
     * پیشنهاد نمره از چک‌لیست معلم. موردهای علامت‌نزده «قبول» حساب
     * نمی‌شوند، ولی complete=false می‌شود تا معلم بداند کار تمام نشده.
     */
    fun scoreFromTests(tests: List<WeightedTest>, checks: Map<Long, Check>, maxScore: Double): ScoreResult {
        var total = 0.0
        var got = 0.0
        var checked = 0
        var passed = 0
        tests.forEach { t ->
            val w = if (t.weight > 0) t.weight else 1.0
            total += w
            val c = checks[t.id]
            if (c != null) {
                checked++
                if (c.passed) { got += w; passed++ }
            }
        }
        val ratio = if (total > 0) got / total else 0.0
        val suggested = maxOf(0.0, minOf(maxScore, roundStep(maxScore * ratio)))
        return ScoreResult(
            suggested, ratio, got, total, checked, passed, tests.size,
            tests.isNotEmpty() && checked == tests.size, tests.isNotEmpty()
        )
    }

    /** پویش تک‌گذر؛ replace پشت‌سرهم «\\n» تحت‌اللفظی را خراب می‌کند. */
    fun unescapeText(s: String?): String {
        val src = s.orEmpty()
        val out = StringBuilder()
        var i = 0
        while (i < src.length) {
            if (src[i] == '\\' && i + 1 < src.length) {
                when (val c = src[i + 1]) {
                    'n' -> out.append('\n')
                    't' -> out.append('\t')
                    else -> out.append(c)
                }
                i += 2
                continue
            }
            out.append(src[i])
            i++
        }
        return out.toString()
    }

    fun escapeText(s: String?): String =
        s.orEmpty().replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")

    private fun isWeight(s: String): Boolean {
        if (!WEIGHT_RE.matches(s)) return false
        val v = s.toDoubleOrNull() ?: return false
        return v > 0 && v <= 100
    }

    /**
     * قالب موارد آزمون — هر خط یک مورد:
     *   برچسب | وزن | ورودی => خروجی
     *   برچسب | وزن | پنهان | ورودی => خروجی
     * ترتیب بخش‌ها آزاد است. برای خط جدید «\n» بنویسید.
     */
    fun parseTests(text: String?): ParseResult {
        val tests = mutableListOf<TestCase>()
        val errors = mutableListOf<ParseError>()
        val lines = LINE_SPLIT.split(text.orEmpty())
        for (idx in lines.indices) {
            val raw = lines[idx].trim()
            if (raw.isEmpty() || raw[0] == '#') continue
            val parts = raw.split('|').map { it.trim() }
            var io: String? = null
            var duplicateIo = false
            var weight = 1.0
            var hidden = false
            var label = ""
            for (p in parts) {
                if (p.contains("=>")) {
                    if (io != null) {
                        errors.add(ParseError(idx + 1, "در هر خط فقط یک «=>» بگذارید."))
                        duplicateIo = true
                        break
                    }
                    io = p
                } else if (isWeight(p)) {
                    weight = p.toDouble()
                } else if (p.lowercase() == "hidden" || p == "پنهان") {
                    hidden = true
                } else if (p.isNotEmpty() && label.isEmpty()) {
                    label = p
                }
            }
            if (duplicateIo) continue
            if (io == null) {
                errors.add(ParseError(idx + 1, "ورودی و خروجی را با «=>» جدا کنید."))
                continue
            }
            val cut = io.indexOf("=>")
            val input = unescapeText(io.substring(0, cut).trim())
            val expected = unescapeText(io.substring(cut + 2).trim())
            if (expected.isEmpty()) {
                errors.add(ParseError(idx + 1, "خروجی انتظار خالی است.")); continue
            }
            if (label.length > MAX_LABEL) {
                errors.add(ParseError(idx + 1, "برچسب بلندتر از ۱۲۰ نویسه است.")); continue
            }
            if (input.length > MAX_TEXT || expected.length > MAX_TEXT) {
                errors.add(ParseError(idx + 1, "ورودی یا خروجی بیش از حد بلند است.")); continue
            }
            tests.add(TestCase(label, input, expected, weight, hidden))
            if (tests.size > MAX_TESTS) {
                errors.add(ParseError(idx + 1, "بیش از ۴۰ مورد آزمون مجاز نیست."))
                tests.removeAt(tests.lastIndex)
                break
            }
        }
        return ParseResult(tests, errors)
    }

    /** برگرداندن موارد به همان قالب متنی (رفت‌وبرگشت پایدار). */
    fun formatTests(tests: List<TestCase>): String =
        tests.joinToString("\n") { t ->
            val bits = mutableListOf<String>()
            if (t.label.isNotEmpty()) bits.add(t.label)
            val w = if (t.weight > 0) t.weight else 1.0
            if (w != 1.0) bits.add(trimNumber(w))
            if (t.hidden) bits.add("پنهان")
            bits.add(escapeText(t.input) + " => " + escapeText(t.expected))
            bits.joinToString(" | ")
        }

    /** مانند String(number) جاوااسکریپت: عدد صحیح بدون «٫۰». */
    private fun trimNumber(v: Double): String =
        if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()

    fun summary(tests: List<WeightedTest>, checks: Map<Long, Check>): String {
        val r = scoreFromTests(tests, checks, 1.0)
        return when {
            !r.hasTests -> "مورد آزمونی تعریف نشده"
            r.checkedCount == 0 -> "هنوز بررسی نشده"
            !r.complete -> "بررسی ناقص"
            else -> "بررسی کامل"
        }
    }
}

/** مشخصات تصحیح کد یک سؤال (فقط معلم می‌بیند). */
data class CodeSpecRow(
    val questionIndex: Int,
    val language: String,
    val reference: String,
    val ignoreCase: Boolean,
    val trimLines: Boolean,
    val collapseBlank: Boolean
) {
    val options: CodeGrading.Options
        get() = CodeGrading.Options(trimLines, collapseBlank, ignoreCase)

    companion object {
        fun listFrom(array: JsonArray): List<CodeSpecRow> =
            array.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                CodeSpecRow(
                    questionIndex = o.int("question_index") ?: 0,
                    language = o.str("language") ?: "python",
                    reference = o.str("reference").orEmpty(),
                    ignoreCase = o.bool("ignore_case") ?: false,
                    trimLines = o.bool("trim_lines") ?: true,
                    collapseBlank = o.bool("collapse_blank") ?: true
                )
            }
    }
}

/** یک مورد آزمون با شناسهٔ سرور. */
data class CodeTestRow(
    val id: Long,
    val questionIndex: Int,
    val ord: Int,
    val label: String,
    val input: String,
    val expected: String,
    val weight: Double,
    val hidden: Boolean
) {
    val weighted: CodeGrading.WeightedTest get() = CodeGrading.WeightedTest(id, weight)

    companion object {
        fun listFrom(array: JsonArray): List<CodeTestRow> =
            array.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val id = o.str("id")?.toLongOrNull() ?: return@mapNotNull null
                CodeTestRow(
                    id = id,
                    questionIndex = o.int("question_index") ?: 0,
                    ord = o.int("ord") ?: 0,
                    label = o.str("label").orEmpty(),
                    input = o.str("input").orEmpty(),
                    expected = o.str("expected").orEmpty(),
                    weight = o.str("weight")?.toDoubleOrNull() ?: 1.0,
                    hidden = o.bool("hidden") ?: false
                )
            }
    }
}

/** یک ردیف چک‌لیست معلم. */
data class CodeCheckRow(
    val answerId: String,
    val questionIndex: Int,
    val testId: Long,
    val passed: Boolean,
    val actual: String
) {
    val lookupKey: String get() = key(answerId, questionIndex)

    companion object {
        fun key(answerId: String, questionIndex: Int) = "$answerId:$questionIndex"

        fun listFrom(array: JsonArray): List<CodeCheckRow> =
            array.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val aid = o.str("answer_id") ?: return@mapNotNull null
                val tid = o.str("test_id")?.toLongOrNull() ?: return@mapNotNull null
                CodeCheckRow(
                    answerId = aid,
                    questionIndex = o.int("question_index") ?: 0,
                    testId = tid,
                    passed = o.bool("passed") ?: false,
                    actual = o.str("actual").orEmpty()
                )
            }

        /** گروه‌بندی به نقشه‌ای که CodeGrading.scoreFromTests می‌فهمد. */
        fun group(rows: List<CodeCheckRow>): Map<String, Map<Long, CodeGrading.Check>> =
            rows.groupBy { it.lookupKey }
                .mapValues { (_, list) -> list.associate { it.testId to CodeGrading.Check(it.passed) } }
    }
}

private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.int(key: String): Int? = str(key)?.toIntOrNull()

private fun JsonObject.bool(key: String): Boolean? = str(key)?.let { it == "true" || it == "t" }
