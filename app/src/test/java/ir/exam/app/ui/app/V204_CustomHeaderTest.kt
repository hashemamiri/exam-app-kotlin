package ir.exam.app.ui.app

import ir.exam.app.ui.printing.HeaderSchema
import ir.exam.app.ui.printing.isVisible
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V204 — سربرگ ۸ «ایجاد سربرگ»: سه ستون × پنج ردیف، لوگو در ستون وسط ردیف ۱–۲، لوگوی دلخواه محلی؛ مشترک اپ/سایت. */
class V204_CustomHeaderTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun src(path: String) = File(root(), path).readText()
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun schema_has_custom_template_with_15_cells_and_conditional_fields() {
        val schema = json.decodeFromString(HeaderSchema.serializer(), src("app/src/main/assets/print/header_settings_schema.json"))
        val t = schema.templates.first { it.id == "custom" }
        val cells = t.fields.filter { Regex("c_[rml][1-5]").matches(it.id) }
        assertEquals(15, cells.size)
        assertEquals("select", t.fields.first { it.id == "c_logo" }.kind)
        assertEquals("image", t.fields.first { it.id == "c_logoData" }.kind)
        val m1 = t.fields.first { it.id == "c_m1" }
        assertTrue(m1.isVisible(mapOf()))
        assertFalse(m1.isVisible(mapOf("c_logo" to "azad")))
        val logoData = t.fields.first { it.id == "c_logoData" }
        assertFalse(logoData.isVisible(mapOf()))
        assertTrue(logoData.isVisible(mapOf("c_logo" to "custom")))
        assertTrue(t.fields.any { it.id == "c_intro" && it.kind == "textarea" })
    }

    @Test fun renderer_and_clients_support_custom_header() {
        val ms = src("app/src/main/assets/print/web/mainscript.js")
        assertTrue("if (t === 'custom') return buildCustomHeader();" in ms)
        assertTrue("rowspan=\"2\"><img class=\"c8-logo-img\"" in ms)
        assertTrue("(currentHeaderTemplate() === 'custom' ? 'c_intro' : 'f_intro')" in ms)
        assertTrue("<option value=\\\"custom\\\">" in src("app/src/main/assets/print/web/host_dom.js"))
        assertTrue(".exam-header8 .c8-logo-img" in src("app/src/main/assets/print/web/main.css"))
        assertTrue("\"c_logo\", \"c_logoData\", \"c_r1\"" in src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt"))
        val ph = src("app/src/main/java/ir/exam/app/ui/printing/PrintHeaderSettings.kt")
        assertTrue("\"image\" -> FieldImage(f, values) { values[f.id] = it }" in ph)
        assertTrue("items(template.fields.filter { it.isVisible(values) }, key = { it.id })" in ph)
        val app = src("site/src/app.js")
        assertTrue("function visible(f) { return !f.showIf || (f.showIf['in'] || []).indexOf(values[f.showIf.field] || '') >= 0; }" in app)
        assertTrue("else if (f.kind === 'image') {" in app)
    }
}
