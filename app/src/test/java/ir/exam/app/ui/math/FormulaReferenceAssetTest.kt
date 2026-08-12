package ir.exam.app.ui.math

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaReferenceAssetTest {
    @Test fun `reference groups symbols unicode and gallery are complete and ordered`() {
        val file=listOf(File("src/main/assets/formula_library_v13.json"),File("app/src/main/assets/formula_library_v13.json")).first(File::isFile)
        val root=Json.parseToJsonElement(file.readText()).jsonObject
        val groups=root["groups"]!!.jsonArray
        assertEquals(listOf("🔢 اعداد و محاسبات","∫ آنالیز و توابع","𝑥 جبر و معادلات","∿ مثلثات و یونانی","⊆ مجموعه و منطق","📐 هندسه و بردار","🚀 فیزیک","🧪 شیمی"),groups.map{it.jsonObject["label"]!!.jsonPrimitive.content})
        val categories=root["categories"]!!.jsonArray
        assertTrue(categories.size>=77)
        val unicode=categories.first{it.jsonObject["id"]!!.jsonPrimitive.content=="unicode"}.jsonObject["items"]!!.jsonArray
        assertEquals(1200,unicode.size)
        assertTrue(categories.sumOf{it.jsonObject["items"]!!.jsonArray.size}>=2000)
        val gallery=root["gallery"]!!.jsonArray
        assertEquals(listOf("📐 هندسه","🔢 جبر","📏 مثلثات","📊 آمار و نسبت","∫ آنالیز"),gallery.map{it.jsonObject["label"]!!.jsonPrimitive.content})
    }

    @Test fun `native editor keeps the requested visual section order`() {
        val file=listOf(File("src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt"),File("app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt")).first(File::isFile)
        val text=file.readText()
        val markers=listOf("↩ بازگشت","⭐ موارد پرکاربرد","🕘 اخیر","درج\")","FixedFormulaKeypad","جست‌وجوی نماد","کد فرمول")
        var position=-1
        markers.forEach{marker->val next=text.indexOf(marker,position+1);assertTrue("missing/order: $marker",next>position);position=next}
        listOf("(",")","7","8","9","⌫","↑","↓","4","5","6","÷","←","→","1","2","3","×","⌨","C","0","=","+","−").forEach{assertTrue(it in text)}
    }
}
