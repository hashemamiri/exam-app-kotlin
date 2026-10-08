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
        // V208 — «ایجاد سربرگ» اولِ فهرست و «سربرگ ۱»؛ پنج ردیف در هر ستون؛ فونت و فاصلهٔ ردیف‌ها
        assertEquals("custom", schema.templates.first().id)
        assertEquals("سربرگ ۱ - ایجاد سربرگ", t.label)
        assertEquals("سربرگ ۲ - قالب قبلی دانشگاه آزاد", schema.templates.first { it.id == "classic" }.label)
        val cells = t.fields.filter { Regex("c_[rml][1-5]").matches(it.id) }
        assertEquals(15, cells.size)
        assertTrue(cells.all { it.group == "cells" } && cells.count { it.col == "r" } == 5 && cells.map { it.id } == listOf("c_r1", "c_r2", "c_r3", "c_r4", "c_r5", "c_m1", "c_m2", "c_m3", "c_m4", "c_m5", "c_l1", "c_l2", "c_l3", "c_l4", "c_l5"))
        // c_logo, c_logoData, c_font, c_gap, cells (۱۵ خانه در سه ستون), c_intro
        assertEquals(listOf(1, 1, 1, 1, 15, 1), groupSizes(t.fields))
        assertEquals("select", t.fields.first { it.id == "c_font" }.kind)
        assertEquals("select", t.fields.first { it.id == "c_gap" }.kind)
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

    private fun groupSizes(f: List<ir.exam.app.ui.printing.HeaderField>) = ir.exam.app.ui.printing.groupHeaderFields(f).map { it.size }

    @Test fun renderer_and_clients_support_custom_header() {
        val ms = src("app/src/main/assets/print/web/mainscript.js")
        assertTrue("if (t === 'custom') return buildCustomHeader();" in ms)
        assertTrue("<img class=\"c8-logo-img\" src=\"${'$'}{logo}\" alt=\"لوگو\">" in ms)
        assertTrue("if (logo) {" in ms && "} else mid += line('c_m1', 1) + (rows >= 2 ? line('c_m2', 2) : '');" in ms)
        assertTrue("for (let i = 1; i <= rows; i++) h += line(prefix + i, i);" in ms && "for (let i = 3; i <= rows; i++) mid += line('c_m' + i, i);" in ms)
        assertTrue("const ROWS = 5;" in ms && "let rows = Math.max(lastFilled('c_r'), lastFilled('c_l'), lastFilled('c_m'), logo ? 2 : 0, 1);" in ms)
        assertTrue("style=\"height:${'$'}{44 + gap}px" in ms && "const font = String(v('c_font') || '').trim();" in ms)
        assertFalse("<table class=\"exam-header exam-header8\">" in ms)
        assertTrue("(currentHeaderTemplate() === 'custom' ? 'c_intro' : 'f_intro')" in ms)
        assertTrue("<option value=\\\"custom\\\">" in src("app/src/main/assets/print/web/host_dom.js"))
        assertTrue("if (!/^(f_|h[2-7]_|c_|opt_footerText$)/.test(id)) return;" in src("app/src/main/assets/print/web/webhost.js"))
        val css = src("app/src/main/assets/print/web/main.css")
        assertTrue(".exam-header8 .c8-logo-img" in css && ".exam-header8 { display:flex;" in css && "padding:8px 0 4px; border-bottom:1px solid #000;" in css)
        assertTrue("Modifier\n                .matchParentSize()\n                .clickable { open = true }" in src("app/src/main/java/ir/exam/app/ui/printing/PrintHeaderSettings.kt"))
        assertTrue("\"c_logo\", \"c_logoData\", \"c_font\", \"c_gap\", \"c_r1\"" in src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt") && "\"c_l5\"" in src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt"))
        val hd = src("app/src/main/assets/print/web/host_dom.js")
        assertTrue("id=\\\"c_r5\\\"" in hd && "id=\\\"c_font\\\"" in hd && "id=\\\"c_gap\\\"" in hd && hd.indexOf("<option value=\\\"custom\\\">") < hd.indexOf("<option value=\\\"classic\\\">"))
        val ph = src("app/src/main/java/ir/exam/app/ui/printing/PrintHeaderSettings.kt")
        assertTrue("\"image\" -> FieldImage(f, values) { values[f.id] = it }" in ph)
        assertTrue("items(groups, key = { it.first().id })" in ph && "groupHeaderFields(template.fields).filter { g -> g.any { it.isVisible(values) } }" in ph)
        val app = src("site/src/app.js")
        assertTrue("function visible(f) { return !f.showIf || (f.showIf['in'] || []).indexOf(values[f.showIf.field] || '') >= 0; }" in app)
        assertTrue("else if (f.kind === 'image') {" in app)
        assertTrue("if (f.group && last && last[0].group === f.group) last.push(f); else groups.push([f]);" in app)
        assertTrue("val cols = g.groupBy { it.col ?: it.id }" in src("app/src/main/java/ir/exam/app/ui/printing/PrintHeaderSettings.kt"))
        assertTrue("var key = f.col || f.id;" in app && "grid-template-columns:1fr;gap:12px" in app)
        assertTrue("Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {\n                                cols.values.forEach" in src("app/src/main/java/ir/exam/app/ui/printing/PrintHeaderSettings.kt"))
    }
}
