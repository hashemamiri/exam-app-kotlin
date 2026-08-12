package ir.exam.app.ui.math

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class FormulaReferenceEntry(val label:String,val tex:String)
data class FormulaReferenceCategory(val id:String,val label:String,val items:List<FormulaReferenceEntry>)
data class FormulaReferenceCategoryLink(val label:String,val id:String)
data class FormulaReferenceGroup(val key:String,val label:String,val categories:List<FormulaReferenceCategoryLink>)
data class FormulaReferenceGallery(val label:String,val items:List<FormulaReferenceEntry>)
data class FormulaReferenceData(val groups:List<FormulaReferenceGroup>,val categories:List<FormulaReferenceCategory>,val gallery:List<FormulaReferenceGallery>){
    val categoryById:Map<String,FormulaReferenceCategory> by lazy { categories.associateBy(FormulaReferenceCategory::id) }
    val allItems:List<FormulaReferenceEntry> by lazy { categories.asSequence().filterNot{it.id=="unicode"||it.id=="letters"}.flatMap{it.items.asSequence()}.distinctBy{it.label+"¦"+it.tex}.toList() }
}

object FormulaReferenceLibrary {
    fun load(context:Context):FormulaReferenceData {
        val raw=context.assets.open("formula_library_v13.json").bufferedReader().use{it.readText()}
        val root=Json.parseToJsonElement(raw) as JsonObject
        fun entries(value:JsonElement?):List<FormulaReferenceEntry> {
            val result=mutableListOf<FormulaReferenceEntry>()
            (value as? JsonArray).orEmpty().forEach { element ->
                val item=element as? JsonObject ?: return@forEach
                result+=FormulaReferenceEntry(item.text("label"),item.text("tex"))
            }
            return result
        }
        val categories=mutableListOf<FormulaReferenceCategory>()
        (root["categories"] as? JsonArray).orEmpty().forEach { element ->
            val item=element as? JsonObject ?: return@forEach
            categories+=FormulaReferenceCategory(item.text("id"),item.text("label"),entries(item["items"]))
        }
        val groups=mutableListOf<FormulaReferenceGroup>()
        (root["groups"] as? JsonArray).orEmpty().forEach groupLoop@ { element ->
            val item=element as? JsonObject ?: return@groupLoop
            val links=mutableListOf<FormulaReferenceCategoryLink>()
            (item["categories"] as? JsonArray).orEmpty().forEach linkLoop@ { link ->
                val row=link as? JsonObject ?: return@linkLoop
                links+=FormulaReferenceCategoryLink(row.text("label"),row.text("id"))
            }
            groups+=FormulaReferenceGroup(item.text("key"),item.text("label"),links)
        }
        val gallery=mutableListOf<FormulaReferenceGallery>()
        (root["gallery"] as? JsonArray).orEmpty().forEach galleryLoop@ { element ->
            val item=element as? JsonObject ?: return@galleryLoop
            gallery+=FormulaReferenceGallery(item.text("label"),entries(item["items"]))
        }
        return FormulaReferenceData(groups,categories,gallery).also(::validate)
    }

    private fun validate(data:FormulaReferenceData) {
        val ids=data.categories.map(FormulaReferenceCategory::id)
        require(ids.size==ids.toSet().size){"شناسه تکراری در کتابخانه فرمول"}
        require(data.groups.map(FormulaReferenceGroup::key).distinct().size==data.groups.size){"گروه تکراری در کتابخانه فرمول"}
        val known=ids.toSet()
        require(data.groups.flatMap(FormulaReferenceGroup::categories).all{it.id in known}){"پیوند دسته نامعتبر در کتابخانه فرمول"}
        require(data.categories.filterNot{it.id=="letters"}.all{it.items.isNotEmpty()}){"دسته خالی در کتابخانه فرمول"}
        require(data.categories.flatMap(FormulaReferenceCategory::items).all{it.label.isNotBlank()&&it.tex.isNotBlank()}){"نماد ناقص در کتابخانه فرمول"}
        require(data.gallery.flatMap(FormulaReferenceGallery::items).all{it.label.isNotBlank()&&it.tex.isNotBlank()}){"فرمول ناقص در گالری"}
    }

    private fun JsonObject.text(key:String)=this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}
