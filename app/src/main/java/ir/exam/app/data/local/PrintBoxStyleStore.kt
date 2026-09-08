package ir.exam.app.data.local

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * V121 — تنظیماتِ «کادر/جدول‌بندیِ» سراسریِ جدولِ سؤال‌های چاپی
 * (`.questions-print-table` در exam_print_renderer.html): ضخامت و رنگِ
 * خطِ دور، پهنای ستونِ شماره/بارم، فاصلهٔ داخلیِ سلولِ متنِ سؤال.
 *
 * مثلِ PrintHeaderStore این‌ها مشخصهٔ خودِ آزمون نیستند (در جدولِ سؤال‌های
 * دیتابیس ذخیره نمی‌شوند) بلکه ترجیحِ نمایشیِ برگهٔ چاپی روی این دستگاه‌اند؛
 * پس با همان الگو در SharedPreferences می‌مانند تا برای آزمون‌های چاپیِ
 * بعدی هم به کار بیایند.
 */
data class PrintBoxStyle(
    /** ضخامتِ خطِ دورِ جدول/سلول‌ها، پیکسل (۰ تا ۶). */
    val borderWidthPx: Float = 1f,
    /** رنگِ خطِ دور، #rrggbb. */
    val borderColor: String = "#000000",
    /** پهنای ستونِ «ردیف»، درصد از عرضِ جدول (۳ تا ۲۰). */
    val numberColWidthPercent: Float = 6f,
    /** پهنای ستونِ «بارم»، درصد از عرضِ جدول (۳ تا ۲۰). */
    val scoreColWidthPercent: Float = 6f,
    /** فاصلهٔ داخلیِ سلولِ متنِ سؤال، پیکسل (۰ تا ۳۰). */
    val cellPaddingPx: Float = 8f
) {
    fun sanitized(): PrintBoxStyle = copy(
        borderWidthPx = borderWidthPx.coerceIn(0f, 6f),
        borderColor = borderColor.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) } ?: "#000000",
        numberColWidthPercent = numberColWidthPercent.coerceIn(3f, 20f),
        scoreColWidthPercent = scoreColWidthPercent.coerceIn(3f, 20f),
        cellPaddingPx = cellPaddingPx.coerceIn(0f, 30f)
    )
}

/**
 * V121 — سریالایزِ یک‌خطیِ PrintBoxStyle برای تزریقِ امن به WebView
 * (رجوع کنید به `window.ExamPrintRenderer.applyBoxStyle` در
 * exam_print_renderer.html که همین شکلِ کلیدها را می‌خواند).
 */
fun printBoxStyleToJson(style: PrintBoxStyle): String {
    val s = style.sanitized()
    val obj = JsonObject(
        mapOf(
            "borderWidthPx" to JsonPrimitive(s.borderWidthPx),
            "borderColor" to JsonPrimitive(s.borderColor),
            "numberColWidthPercent" to JsonPrimitive(s.numberColWidthPercent),
            "scoreColWidthPercent" to JsonPrimitive(s.scoreColWidthPercent),
            "cellPaddingPx" to JsonPrimitive(s.cellPaddingPx)
        )
    )
    return Json.encodeToString(JsonObject.serializer(), obj)
}

class PrintBoxStyleStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "print_box_style",
        Context.MODE_PRIVATE
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun read(): PrintBoxStyle {
        val raw = preferences.getString(KEY, null) ?: return PrintBoxStyle()
        return runCatching {
            val obj = json.decodeFromString(JsonObject.serializer(), raw)
            fun f(key: String, fallback: Float) =
                (obj[key] as? JsonPrimitive)?.floatOrNull ?: (obj[key] as? JsonPrimitive)?.doubleOrNull?.toFloat() ?: fallback
            PrintBoxStyle(
                borderWidthPx = f("borderWidthPx", 1f),
                borderColor = (obj["borderColor"] as? JsonPrimitive)?.jsonPrimitive?.content ?: "#000000",
                numberColWidthPercent = f("numberColWidthPercent", 6f),
                scoreColWidthPercent = f("scoreColWidthPercent", 6f),
                cellPaddingPx = f("cellPaddingPx", 8f)
            ).sanitized()
        }.getOrDefault(PrintBoxStyle())
    }

    fun write(style: PrintBoxStyle) {
        val s = style.sanitized()
        val obj = JsonObject(
            mapOf(
                "borderWidthPx" to JsonPrimitive(s.borderWidthPx),
                "borderColor" to JsonPrimitive(s.borderColor),
                "numberColWidthPercent" to JsonPrimitive(s.numberColWidthPercent),
                "scoreColWidthPercent" to JsonPrimitive(s.scoreColWidthPercent),
                "cellPaddingPx" to JsonPrimitive(s.cellPaddingPx)
            )
        )
        preferences.edit().putString(KEY, json.encodeToString(JsonObject.serializer(), obj)).apply()
    }

    fun clear() = preferences.edit().remove(KEY).apply()

    private companion object {
        const val KEY = "style"
    }
}
