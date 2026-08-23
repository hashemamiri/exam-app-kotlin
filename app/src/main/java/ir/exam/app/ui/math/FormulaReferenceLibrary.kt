package ir.exam.app.ui.math

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class FormulaReferenceEntry(val label: String, val tex: String)
data class FormulaReferenceCategory(val id: String, val label: String, val items: List<FormulaReferenceEntry>)
data class FormulaReferenceCategoryLink(val label: String, val id: String)
data class FormulaReferenceGroup(val key: String, val label: String, val categories: List<FormulaReferenceCategoryLink>)
data class FormulaReferenceGallery(val label: String, val items: List<FormulaReferenceEntry>)
data class FormulaReferenceData(
    val groups: List<FormulaReferenceGroup>,
    val categories: List<FormulaReferenceCategory>,
    val gallery: List<FormulaReferenceGallery>
) {
    val categoryById: Map<String, FormulaReferenceCategory> by lazy {
        categories.associateBy(FormulaReferenceCategory::id)
    }
    val allItems: List<FormulaReferenceEntry> by lazy {
        categories.asSequence()
            .filterNot { it.id == "unicode" || it.id == "letters" }
            .flatMap { it.items.asSequence() }
            .distinctBy { it.label + "¦" + it.tex }
            .toList()
    }
}

object FormulaReferenceLibrary {
    fun load(context: Context): FormulaReferenceData {
        val raw = context.assets.open("formula_library_v13.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return decode(raw)
    }

    /** Decoder خالص برای تست واقعی همان داده‌ای که Runtime مصرف می‌کند. */
    fun decode(raw: String): FormulaReferenceData {
        val root = Json.parseToJsonElement(raw) as? JsonObject
            ?: error("ریشهٔ کتابخانه فرمول معتبر نیست")

        fun entries(value: JsonElement?): List<FormulaReferenceEntry> =
            (value as? JsonArray).orEmpty().mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                FormulaReferenceEntry(item.text("label"), item.text("tex"))
            }

        val categories = (root["categories"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            FormulaReferenceCategory(item.text("id"), item.text("label"), entries(item["items"]))
        }
        val groups = (root["groups"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val links = (item["categories"] as? JsonArray).orEmpty().mapNotNull { link ->
                val row = link as? JsonObject ?: return@mapNotNull null
                FormulaReferenceCategoryLink(row.text("label"), row.text("id"))
            }
            FormulaReferenceGroup(item.text("key"), item.text("label"), links)
        }
        val gallery = (root["gallery"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            FormulaReferenceGallery(item.text("label"), entries(item["items"]))
        }
        return FormulaReferenceData(groups, categories, gallery).also(::validate)
    }

    private fun validate(data: FormulaReferenceData) {
        require(data.groups.size >= 8) { "تعداد گروه‌های کتابخانه فرمول ناقص است" }
        require(data.categories.size >= 77) { "تعداد دسته‌های کتابخانه فرمول ناقص است" }
        val ids = data.categories.map(FormulaReferenceCategory::id)
        require(ids.size == ids.toSet().size) { "شناسه تکراری در کتابخانه فرمول" }
        require(data.groups.map(FormulaReferenceGroup::key).distinct().size == data.groups.size) {
            "گروه تکراری در کتابخانه فرمول"
        }
        val known = ids.toSet()
        require(data.groups.flatMap(FormulaReferenceGroup::categories).all { it.id in known }) {
            "پیوند دسته نامعتبر در کتابخانه فرمول"
        }
        require(data.categories.filterNot { it.id == "letters" }.all { it.items.isNotEmpty() }) {
            "دسته خالی در کتابخانه فرمول"
        }
        require(data.categories.flatMap(FormulaReferenceCategory::items).all {
            it.label.isNotBlank() && it.tex.isNotBlank()
        }) { "نماد ناقص در کتابخانه فرمول" }
        require(data.gallery.flatMap(FormulaReferenceGallery::items).all {
            it.label.isNotBlank() && it.tex.isNotBlank()
        }) { "فرمول ناقص در گالری" }
        require(data.categoryById["unicode"]?.items?.size == 1200) {
            "Unicode 1200 ناقص است"
        }
    }

    private fun JsonObject.text(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}
