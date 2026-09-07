package ir.exam.app.core.printing

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintQuestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.math.roundToInt

/**
 * وضعیتِ چیدمانِ بومیِ پیش‌نمایش PDF برای یک شکل.
 *
 * مقادیر بر حسب میلی‌متر و نسبت به ابتدای همان سؤال ذخیره می‌شوند. این قالب
 * عمداً در همان فیلد پایدارِ [OfficialPrintQuestion.figLayoutsJson] نگه‌داری
 * می‌شود تا چیدمان‌های ذخیره‌شدهٔ آزمون‌های چاپی با ذخیره‌سازهای فعلی سازگار
 * بمانند؛ هیچ WebView یا واحد CSS در مسیر جدید مصرف نمی‌شود.
 */
data class NativePrintFigureLayout(
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float,
    val free: Boolean
)

/**
 * قرارداد کوچک و خالصِ layout پیش‌نمایش PDF.
 *
 * نسخهٔ قبلی renderer HTML برای هر شکل `{x,y,w,h,free}` با پیکسل CSS ذخیره
 * می‌کرد. [decode] آن مقدار را فقط برای مهاجرت دادهٔ موجود می‌خواند و به
 * میلی‌متر تبدیل می‌کند. تمام خروجی‌های جدید با `nativePdf=true` نوشته
 * می‌شوند، بنابراین renderer بومی هرگز به مختصات CSS وابسته نیست.
 */
object PrintPreviewLayoutCodec {
    private val json = Json { ignoreUnknownKeys = true }

    /** 96px CSS در هر inch؛ فقط برای خواندن چیدمان‌های پیش از PDF بومی. */
    private const val CSS_PX_PER_MM = 96f / 25.4f
    private const val CONTENT_WIDTH_MM = 181.76f
    private const val MIN_WIDTH_MM = 12f
    private const val MAX_WIDTH_MM = 176f
    private const val MIN_HEIGHT_MM = 8f
    private const val MAX_HEIGHT_MM = 900f
    private const val MAX_Y_MM = 2_000f

    fun decode(source: String): Map<Int, NativePrintFigureLayout> {
        val objectValue = runCatching {
            if (source.isBlank()) JsonObject(emptyMap())
            else json.parseToJsonElement(source).jsonObject
        }.getOrDefault(JsonObject(emptyMap()))

        return buildMap {
            objectValue.forEach { (rawIndex, rawValue) ->
                val index = rawIndex.toIntOrNull() ?: return@forEach
                if (index < 0) return@forEach
                val item = rawValue as? JsonObject ?: return@forEach
                layoutFrom(item)?.let { put(index, it) }
            }
        }
    }

    /** JSON جدید فقط میلی‌متر دارد و برای خواندن انسان هم کوتاه می‌ماند. */
    fun encode(layouts: Map<Int, NativePrintFigureLayout>): String = buildJsonObject {
        layouts.toSortedMap().forEach { (index, layout) ->
            put(index.toString(), buildJsonObject {
                put("nativePdf", true)
                put("xMm", layout.xMm.roundForStorage())
                put("yMm", layout.yMm.roundForStorage())
                put("widthMm", layout.widthMm.roundForStorage())
                put("heightMm", layout.heightMm.roundForStorage())
                put("free", layout.free)
            })
        }
    }.toString()

    fun updateFigure(
        question: OfficialPrintQuestion,
        figureIndex: Int,
        layout: NativePrintFigureLayout
    ): OfficialPrintQuestion {
        val values = decode(question.figLayoutsJson).toMutableMap()
        values[figureIndex] = sanitize(layout)
        return question.copy(figLayoutsJson = encode(values))
    }

    /**
     * همان snapshot سطح سؤال که ExamBuilderViewModel از قبل ذخیره می‌کند:
     * شمارهٔ سؤال → figLayouts و فاصلهٔ separator. بنابراین تغییر gesture
     * مستقیم به draft موجود می‌رسد و در چاپ بعدی نیز دیده می‌شود.
     */
    fun snapshot(printable: OfficialExamPrintable): String = buildJsonObject {
        printable.questions.forEachIndexed { index, question ->
            put((index + 1).toString(), buildJsonObject {
                put("figLayouts", safeElement(question.figLayoutsJson))
                put("sepExtraPx", question.sepExtraPx.coerceIn(0, 1_500))
            })
        }
    }.toString()

    /** تبدیل تغییرِ فاصلهٔ gesture PDF (pt) به پیکسل پایدارِ separator. */
    fun separatorPxDeltaFromPdfPoints(points: Float): Int =
        (points / 0.75f).roundToInt()

    private fun layoutFrom(item: JsonObject): NativePrintFigureLayout? {
        val isNative = item.bool("nativePdf") || item.string("unit") == "mm"
        val free = item.bool("free")
        return if (isNative) {
            val width = item.number("widthMm") ?: item.number("wMm") ?: return null
            val height = item.number("heightMm") ?: item.number("hMm") ?: width * 0.75f
            NativePrintFigureLayout(
                xMm = item.number("xMm") ?: item.number("x") ?: 0f,
                yMm = item.number("yMm") ?: item.number("y") ?: 0f,
                widthMm = width,
                heightMm = height,
                free = free
            ).let(::sanitize)
        } else {
            // مهاجرت یک‌باره از snapshot HTML قدیمی: x فاصله از سمت راست بود.
            val widthCss = item.number("w") ?: return null
            val heightCss = item.number("h") ?: widthCss * 0.75f
            val widthMm = widthCss / CSS_PX_PER_MM
            val xFromRightMm = (item.number("x") ?: 0f) / CSS_PX_PER_MM
            NativePrintFigureLayout(
                xMm = CONTENT_WIDTH_MM - xFromRightMm - widthMm,
                yMm = (item.number("y") ?: 0f) / CSS_PX_PER_MM,
                widthMm = widthMm,
                heightMm = heightCss / CSS_PX_PER_MM,
                free = free
            ).let(::sanitize)
        }
    }

    private fun sanitize(value: NativePrintFigureLayout): NativePrintFigureLayout {
        val width = value.widthMm.finiteOr(55f).coerceIn(MIN_WIDTH_MM, MAX_WIDTH_MM)
        val height = value.heightMm.finiteOr(width * .75f).coerceIn(MIN_HEIGHT_MM, MAX_HEIGHT_MM)
        val x = value.xMm.finiteOr(0f).coerceIn(0f, (CONTENT_WIDTH_MM - width).coerceAtLeast(0f))
        val y = value.yMm.finiteOr(0f).coerceIn(0f, MAX_Y_MM)
        return NativePrintFigureLayout(x, y, width, height, value.free)
    }

    private fun safeElement(source: String): JsonElement = runCatching {
        if (source.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(source)
    }.getOrDefault(JsonObject(emptyMap()))

    private fun JsonObject.number(key: String): Float? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.toFloatOrNull()?.takeIf { it.isFinite() }

    private fun JsonObject.string(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun JsonObject.bool(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull
            ?: string(key).equals("true", ignoreCase = true)

    private fun Float.finiteOr(default: Float): Float = if (isFinite()) this else default

    private fun Float.roundForStorage(): Float = (this * 100f).roundToInt() / 100f
}
