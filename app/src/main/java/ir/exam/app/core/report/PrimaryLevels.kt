package ir.exam.app.core.report

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * V305 — کارنامهٔ توصیفی ۴ سطحی ابتدایی (مقیاس رسمی):
 * خیلی خوب / خوب / قابل قبول / نیاز به تلاش بیشتر.
 *
 * آستانه‌ها (درصد) در `exams.options.levels = [a, b, c]` ذخیره می‌شوند؛ پیش‌فرض [85, 70, 50].
 * درصد ≥ a → سطح ۴، ≥ b → ۳، ≥ c → ۲، وگرنه ۱. دانش‌آموز سطح + ستاره می‌بیند، معلم عدد + سطح.
 * محاسبهٔ سطح در سرور است (RPC `native_my_levels_v1`، SQL V305) با همین قاعده؛ نسخهٔ وب فقط نمایش می‌دهد.
 */
object PrimaryLevels {
    val DEFAULT = listOf(85.0, 70.0, 50.0)
    val NAMES = listOf("نیاز به تلاش بیشتر", "قابل قبول", "خوب", "خیلی خوب")

    data class Level(val rank: Int, val name: String) {
        /** ستاره‌های تزئینی: ★ به تعداد rank، ☆ تا ۴. */
        val stars: String get() = "★".repeat(rank) + "☆".repeat(4 - rank)
    }

    /** «85,70,50» → [85,70,50]؛ نامعتبر/خالی/غیرنزولی/برابر پیش‌فرض → null. */
    fun parseThresholds(text: String): List<Double>? {
        val parts = text.replace("،", ",").replace("٫", ".").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size != 3) return null
        val nums = parts.map { p ->
            p.map { c -> if (c in '۰'..'۹') ('0' + (c - '۰')) else c }.joinToString("").toDoubleOrNull() ?: return null
        }
        if (nums.any { it <= 0.0 || it > 100.0 }) return null
        if (!(nums[0] > nums[1] && nums[1] > nums[2])) return null
        return if (nums == DEFAULT) null else nums
    }

    /** آستانه‌ها از options آزمون (JSON)؛ نبود/نامعتبر = پیش‌فرض. */
    fun thresholdsOf(options: JsonObject?): List<Double> {
        val arr = options?.get("levels") as? JsonArray ?: return DEFAULT
        val nums = arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() }
        return if (nums.size == 3 && nums[0] > nums[1] && nums[1] > nums[2]) nums else DEFAULT
    }

    fun isPrimary(options: JsonObject?): Boolean =
        (options?.get("primary") as? JsonPrimitive)?.contentOrNull == "true"

    fun levelOf(percent: Double, thresholds: List<Double> = DEFAULT): Level {
        val t = if (thresholds.size == 3) thresholds else DEFAULT
        val rank = when {
            percent >= t[0] -> 4
            percent >= t[1] -> 3
            percent >= t[2] -> 2
            else -> 1
        }
        return Level(rank, NAMES[rank - 1])
    }
}
