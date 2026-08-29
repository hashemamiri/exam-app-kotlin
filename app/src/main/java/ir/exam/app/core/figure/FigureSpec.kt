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

    /** آرایهٔ عددی داخل X (مثل hid/hidZ/hideCols/hideRows جدول تناوبی مرجع). */
    fun xIntList(key: String): List<Int> =
        ((raw["X"] as? JsonObject)?.get(key) as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() }
            ?: emptyList()

    /**
     * نشانه‌های شماره‌دار آناتومی/علوم (`X.marks` مرجع): مختصات درصدی ۰..۱۰۰
     * نسبت به قاب تصویر، شمارهٔ `n` و برچسب اختیاری `lbl`.
     */
    fun marks(): List<AtlasMark> =
        ((raw["X"] as? JsonObject)?.get("marks") as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                fun num(key: String): Float? =
                    (obj[key] as? JsonPrimitive)?.contentOrNull?.toFloatOrNull()
                AtlasMark(
                    x1 = num("x1") ?: return@mapNotNull null,
                    y1 = num("y1") ?: return@mapNotNull null,
                    x2 = num("x2") ?: return@mapNotNull null,
                    y2 = num("y2") ?: return@mapNotNull null,
                    n = (obj["n"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0,
                    label = (obj["lbl"] as? JsonPrimitive)?.contentOrNull ?: ""
                )
            } ?: emptyList()

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

        /**
         * ساخت spec جدول تناوبی با همان قالب مرجع:
         * `{k:'p', t:preset, X:{title, Z, hid, hidZ, hideCols, hideRows, hideF}}`.
         */
        fun buildPeriodic(
            preset: String,
            title: String,
            showZ: Boolean,
            hideF: Boolean,
            hiddenElements: List<Int>,
            hiddenZ: List<Int>,
            hiddenGroups: List<Int>,
            hiddenPeriods: List<Int>
        ): FigureSpec {
            fun ints(values: List<Int>) =
                kotlinx.serialization.json.JsonArray(values.sorted().map { JsonPrimitive(it) })
            val x = mutableMapOf<String, JsonElement>(
                "title" to JsonPrimitive(title),
                "Z" to JsonPrimitive(if (showZ) "1" else "0"),
                "hideF" to JsonPrimitive(if (hideF) "1" else "0"),
                "hid" to ints(hiddenElements),
                "hidZ" to ints(hiddenZ),
                "hideCols" to ints(hiddenGroups),
                "hideRows" to ints(hiddenPeriods)
            )
            return FigureSpec(
                JsonObject(
                    mapOf(
                        "k" to JsonPrimitive("p"),
                        "t" to JsonPrimitive(preset),
                        "X" to JsonObject(x)
                    )
                )
            )
        }

        /**
         * ساخت spec آناتومی (`k='a'`) یا فیزیک/شیمی (`k='s'`) با قالب مرجع:
         * `{k, t:نوع, X:{title, lab, blank, mkName, marks[]}}`.
         */
        fun buildAtlas(
            kind: String,
            type: String,
            title: String,
            showLabel: Boolean,
            showBlanks: Boolean,
            showMarkNames: Boolean,
            marks: List<AtlasMark>
        ): FigureSpec {
            val marksJson = kotlinx.serialization.json.JsonArray(
                marks.map { m ->
                    JsonObject(
                        mapOf(
                            "x1" to JsonPrimitive(m.x1),
                            "y1" to JsonPrimitive(m.y1),
                            "x2" to JsonPrimitive(m.x2),
                            "y2" to JsonPrimitive(m.y2),
                            "n" to JsonPrimitive(m.n),
                            "lbl" to JsonPrimitive(m.label)
                        )
                    )
                }
            )
            val x = mutableMapOf<String, JsonElement>(
                "title" to JsonPrimitive(title),
                "lab" to JsonPrimitive(if (showLabel) "1" else "0"),
                "blank" to JsonPrimitive(if (showBlanks) "1" else "0"),
                "mkName" to JsonPrimitive(if (showMarkNames) "1" else "0"),
                "marks" to marksJson
            )
            return FigureSpec(
                JsonObject(
                    mapOf(
                        "k" to JsonPrimitive(kind),
                        "t" to JsonPrimitive(type),
                        "X" to JsonObject(x)
                    )
                )
            )
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

/** یک نشانهٔ شماره‌دار روی تصویر اطلس؛ مختصات درصدی ۰..۱۰۰ همان قرارداد مرجع. */
data class AtlasMark(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val n: Int,
    val label: String = ""
)

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

    /** V67 — توکن کامل یک شکل به‌صورت رشتهٔ نهایی. */
    fun token(spec: FigureSpec): String = PREFIX + spec.toJson() + SUFFIX

    /**
     * V67 — درج توکن در محل مکان‌نما (پایان بخش متنی فعال) به‌جای انتهای کل
     * متن تا ترتیب «متن، شکل، ادامهٔ متن» حفظ شود. فاصلهٔ امن فقط وقتی اضافه
     * می‌شود که کاراکتر مجاور فاصله یا newline نباشد.
     */
    fun insertAt(text: String, spec: FigureSpec, at: Int): String {
        val wrapped = token(spec)
        if (text.isBlank()) return wrapped
        val caret = at.coerceIn(0, text.length)
        val before = text.substring(0, caret)
        val after = text.substring(caret)
        val prefix = if (before.isEmpty() || before.last() == ' ' || before.last() == '\n') "" else " "
        val suffix = if (after.isEmpty() || after.first() == ' ' || after.first() == '\n') "" else " "
        return before + prefix + wrapped + suffix + after
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
