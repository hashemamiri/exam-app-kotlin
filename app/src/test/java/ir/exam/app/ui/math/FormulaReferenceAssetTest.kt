package ir.exam.app.ui.math

import ir.exam.app.core.math.FormulaBoxEditor
import ir.exam.app.core.math.NativeMathParser
import ir.exam.app.core.math.NativeMathSvgRenderer
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
        val categoryIds=categories.map{it.jsonObject["id"]!!.jsonPrimitive.content}
        assertEquals(categoryIds.size,categoryIds.toSet().size)
        val links=groups.flatMap{group->group.jsonObject["categories"]!!.jsonArray.map{it.jsonObject["id"]!!.jsonPrimitive.content}}
        assertTrue("broken library category link",links.all{it in categoryIds})
        assertTrue("unreachable library category",categoryIds.filterNot{it in setOf("common","unicode","letters")}.all{it in links})
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

    @Test fun `every reference tex command is supported by native svg parser`() {
        val file=listOf(File("src/main/assets/formula_library_v13.json"),File("app/src/main/assets/formula_library_v13.json")).first(File::isFile)
        val root=Json.parseToJsonElement(file.readText()).jsonObject
        val formulas=buildList {
            root["categories"]!!.jsonArray.forEach { category ->
                category.jsonObject["items"]!!.jsonArray.forEach { entry ->
                    add(entry.jsonObject["tex"]!!.jsonPrimitive.content)
                }
            }
            root["gallery"]!!.jsonArray.forEach { group ->
                group.jsonObject["items"]!!.jsonArray.forEach { entry ->
                    add(entry.jsonObject["tex"]!!.jsonPrimitive.content)
                }
            }
        }
        val unsupported=formulas.flatMap(NativeMathParser::unsupportedCommands).toSortedSet()
        assertTrue("unsupported SVG commands: $unsupported",unsupported.isEmpty())
        assertTrue("reference formulas missing",formulas.size>=2118)
        val host="\\sqrt{x}"
        val placeholder=NativeMathParser.editableRanges(host).single()
        formulas.forEachIndexed { index, tex ->
            val document=NativeMathSvgRenderer.render(tex,20f)
            assertTrue("invalid SVG at $index: $tex",document.xml.startsWith("<svg")&&document.xml.endsWith("</svg>"))
            assertTrue("invalid SVG size at $index",document.widthPx>0f&&document.heightPx>0f)
            assertTrue("missing editable SVG box at $index: $tex",document.editBoxes.isNotEmpty())
            assertTrue("invalid SVG box at $index",document.editBoxes.all{it.widthPx>0f&&it.heightPx>0f&&it.xPx>=0f&&it.yPx>=0f})
            assertTrue("invalid SVG source range at $index",document.editBoxes.all{it.sourceStart in 0..tex.length&&it.sourceEnd in it.sourceStart..tex.length})
            assertTrue("raw TeX leaked at $index: $tex",Regex("\\\\[A-Za-z]+").find(document.xml)==null)
            val inserted=FormulaBoxEditor.insert(host,placeholder.start,placeholder.endExclusive,tex,true,true)
            assertEquals("library insertion failed at $index","\\sqrt{$tex}",inserted.text)
            assertTrue("library selection failed at $index",inserted.selectionStart in 0..inserted.text.length&&inserted.selectionEnd in inserted.selectionStart..inserted.text.length)
        }
    }

    @Test fun `complete reference features are reachable in native ui`() {
        val root=listOf(File("."),File("..")).first { File(it,"app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").isFile }
        val editor=File(root,"app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").readText()
        val smart=File(root,"app/src/main/java/ir/exam/app/ui/math/FormulaSmartHubDialog.kt").readText()
        val view=File(root,"app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").readText()
        val builder=File(root,"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
        val matching=File(root,"app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt").readText()
        listOf("مرکز هوشمند","ماتریس دلخواه ۱ تا ۱۰","recentDialogOpen","delimiterPickerOpen","onPreviewKeyEvent","combinedClickable","smartQuickToTex").forEach{assertTrue("missing $it",it in editor)}
        listOf("کتابخانهٔ درس‌به‌درس","قالب‌های آماده","بسته‌های آماده","کلیدهای درشت","فرمول آخر","نمایش نهایی").forEach{assertTrue("missing Smart Hub $it",it in smart)}
        assertTrue("animateScrollTo" in view&&"verticalScroll" in view)
        assertTrue("ExistingFormulaEditor" in builder&&"occurrenceIndex" in builder)
        assertTrue("ExistingFormulaEditor" in matching&&"matching_" in builder)
    }

    @Test fun `formula library buttons and editor use svg instead of raw tex`() {
        val root=listOf(File("."),File("..")).first { File(it,"app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").isFile }
        val editor=File(root,"app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").readText()
        val view=File(root,"app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").readText()
        val mixedText=File(root,"app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt").readText()
        val renderer=File(root,"app/src/main/java/ir/exam/app/core/math/NativeMathSvgRenderer.kt").readText()
        val gradle=File(root,"app/build.gradle.kts").readText()
        assertTrue("NativeFormulaIcon" in editor)
        assertTrue("SvgFormulaEditorSurface" in editor)
        assertTrue("NativeFormulaEditorView" in editor)
        assertTrue("replaceActiveBox = true" in editor)
        assertTrue("BasicTextField" in editor&&".size(1.dp)" in editor)
        assertTrue("detectTapGestures" in view&&"onBoxTap" in view)
        assertTrue("SvgDecoder.Factory" in view)
        assertTrue("NativeMathSvgRenderer.render" in view)
        assertTrue("NativeFormulaView" in mixedText)
        assertTrue("mathAnnotated" !in mixedText)
        assertTrue("<svg" in renderer)
        assertTrue("io.coil-kt:coil-svg:2.7.0" in gradle)
        assertTrue("Text(entry.tex" !in editor)
    }
}
