package ir.exam.app.ui.math

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaSmartReferenceTest {
    @Test fun `smart hub contains every lesson template pack delimiter and big key from reference`() {
        assertEquals(listOf("ریاضی", "هندسه", "آمار", "فیزیک", "شیمی"), FormulaSmartReference.lessons.map { it.label })
        assertEquals(30, FormulaSmartReference.lessons.sumOf { it.templates.size })
        assertEquals(8, FormulaSmartReference.packs.size)
        assertEquals(6, FormulaSmartReference.delimiters.size)
        assertEquals(8, FormulaSmartReference.bigKeyLabels.size)
        assertEquals(6, FormulaSmartReference.defaultFavorites.size)
        assertTrue(FormulaSmartReference.lessons.all { it.categoryIds.isNotEmpty() && it.templates.size == 6 })
    }

    @Test fun `every smart lesson and pack points to a real reference category`() {
        val file=listOf(File("src/main/assets/formula_library_v13.json"),File("app/src/main/assets/formula_library_v13.json")).first(File::isFile)
        val root=Json.parseToJsonElement(file.readText()).jsonObject
        val ids=root["categories"]!!.jsonArray.map{it.jsonObject["id"]!!.jsonPrimitive.content}.toSet()
        val linked=FormulaSmartReference.lessons.flatMap{it.categoryIds}+FormulaSmartReference.packs.flatMap{it.categoryIds}
        assertTrue("broken Smart Hub ids: ${linked.filterNot(ids::contains)}",linked.all(ids::contains))
    }

    @Test fun `delimiter presets produce native editable tex`() {
        val values = FormulaSmartReference.delimiters.map { delimiterTex(it, "x") }
        assertTrue("\\left(x\\right)" in values)
        assertTrue("\\lfloor x \\rfloor" in values)
        assertTrue("\\lceil x \\rceil" in values)
    }
}
