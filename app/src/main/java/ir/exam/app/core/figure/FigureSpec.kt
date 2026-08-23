package ir.exam.app.core.figure

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * مشخصات یک شکل/نمودار با همان قالب وب‌اپ:
 * `{"t":نوع,"V":{برچسب رأس‌ها},"S":{برچسب ضلع‌ها},"A":{برچسب زاویه‌ها},"X":{پارامترهای اضافی}}`
 */
data class FigureSpec(val raw: JsonObject) {
    val type: String get() = (raw["t"] as? JsonPrimitive)?.contentOrNull ?: "tri"

    /**
     * کد ماژول وب‌اپ: خالی=هندسه/نمودار، `t`=جدول، `a`=آناتومی، `p`=جدول تناوبی،
     * `s`=فیزیک/شیمی. همان فیلد `k` مرجع است.
     */
    val kind: String get() = (raw["k"] as? JsonPrimitive)?.contentOrNull ?: ""

    val isTable: Boolean get() = kind == "t"

    fun vertex(key: String): String = strOf((raw["V"] as? JsonObject)?.get(key))
    fun side(key: String): String = strOf((raw["S"] as? JsonObject)?.get(key))
    fun angle(key: String): String = strOf((raw["A"] as? JsonObject)?.get(key))

    fun xStr(key: String, default: String = ""): String =
        strOf((raw["X"] as? JsonObject)?.get(key)).ifBlank { default }

    fun xNum(key: String, default: Float): Float =
        ((raw["X"] as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull?.toFloatOrNull() ?: default

    fun xList(key: String, default: String = ""): List<String> = splitList(xStr(key, default))

    /** خانه‌های جدول (`C` مرجع): آرایهٔ سطرها؛ هر سطر آرایه‌ای از رشته‌ها. */
    fun tableCells(): List<List<String>> =
        (raw["C"] as? kotlinx.serialization.json.JsonArray)?.map { row ->
            (row as? kotlinx.serialization.json.JsonArray)?.map { cell ->
                (cell as? JsonPrimitive)?.contentOrNull ?: ""
            } ?: emptyList()
        } ?: emptyList()

    fun toJson(): String = raw.toString()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): FigureSpec? = runCatching {
            FigureSpec(json.parseToJsonElement(text).jsonObject)
        }.getOrNull()

        fun build(
            type: String,
            vertices: Map<String, String> = emptyMap(),
            sides: Map<String, String> = emptyMap(),
            angles: Map<String, String> = emptyMap(),
            extra: Map<String, JsonPrimitive> = emptyMap()
        ): FigureSpec {
            val map = mutableMapOf<String, JsonElement>()
            map["t"] = JsonPrimitive(type)
            if (vertices.isNotEmpty()) map["V"] = JsonObject(vertices.mapValues { JsonPrimitive(it.value) })
            if (sides.isNotEmpty()) map["S"] = JsonObject(sides.mapValues { JsonPrimitive(it.value) })
            if (angles.isNotEmpty()) map["A"] = JsonObject(angles.mapValues { JsonPrimitive(it.value) })
            if (extra.isNotEmpty()) map["X"] = JsonObject(extra)
            return FigureSpec(JsonObject(map))
        }

        /** ساخت spec جدول با همان قالب مرجع: `{k:'t', t:سبک, X:{title}, C:[[...]]}`. */
        fun buildTable(
            style: String,
            title: String,
            cells: List<List<String>>
        ): FigureSpec {
            val map = mutableMapOf<String, JsonElement>()
            map["k"] = JsonPrimitive("t")
            map["t"] = JsonPrimitive(style)
            map["X"] = JsonObject(mapOf("title" to JsonPrimitive(title)))
            map["C"] = kotlinx.serialization.json.JsonArray(
                cells.map { row -> kotlinx.serialization.json.JsonArray(row.map { JsonPrimitive(it) }) }
            )
            return FigureSpec(JsonObject(map))
        }
    }
}

private fun strOf(el: JsonElement?): String = (el as? JsonPrimitive)?.contentOrNull ?: ""

fun splitList(s: String): List<String> =
    s.split(Regex("[,،;|\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }

data class FigureOccurrence(
    val index: Int,
    val start: Int,
    val endExclusive: Int,
    val spec: FigureSpec,
    val rawJson: String
)

/** مدیریت شکل‌های درون‌متنی `%%FIG:{json}%%` — دقیقاً همان قالب وب‌اپ. */
object FigureCodec {
    private const val PREFIX = "%%FIG:"
    private const val SUFFIX = "%%"

    fun occurrences(text: String): List<FigureOccurrence> {
        val result = mutableListOf<FigureOccurrence>()
        var cursor = 0
        while (true) {
            val at = text.indexOf(PREFIX, cursor)
            if (at < 0) break
            val contentStart = at + PREFIX.length
            val end = text.indexOf(SUFFIX, contentStart)
            if (end < 0) break
            val raw = text.substring(contentStart, end)
            val spec = FigureSpec.parse(raw)
            if (spec != null) {
                result += FigureOccurrence(result.size, at, end + SUFFIX.length, spec, raw)
                cursor = end + SUFFIX.length
            } else {
                cursor = contentStart
            }
        }
        return result
    }

    fun insert(text: String, spec: FigureSpec): String {
        val wrapped = PREFIX + spec.toJson() + SUFFIX
        return if (text.isBlank()) wrapped else text.trimEnd() + " " + wrapped
    }

    fun replace(text: String, occurrenceIndex: Int, spec: FigureSpec): String {
        val target = occurrences(text).getOrNull(occurrenceIndex) ?: return text
        val wrapped = PREFIX + spec.toJson() + SUFFIX
        return text.substring(0, target.start) + wrapped + text.substring(target.endExclusive)
    }

    fun delete(text: String, occurrenceIndex: Int): String {
        val target = occurrences(text).getOrNull(occurrenceIndex) ?: return text
        return (text.substring(0, target.start) + text.substring(target.endExclusive))
            .replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex(" ?\\n ?"), "\n")
            .trim()
    }
}
