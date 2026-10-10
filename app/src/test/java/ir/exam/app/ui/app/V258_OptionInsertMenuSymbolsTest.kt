package ir.exam.app.ui.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import ir.exam.app.ui.builder.FigTokenVisuals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V258 — گزینه‌ها: فرمول‌ها در کادر گزینه (اپ و سایت) به‌صورت نماد دیده می‌شوند نه کد LaTeX؛ دسکتاپ سایت مثل اپ دکمهٔ + با منوی ابزارهای درج دارد. */
class V258_OptionInsertMenuSymbolsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `app option field shows formulas as symbols and keeps mapping monotonic`() {
        val src = "گزینهٔ \$\\left\\{ \\left[ \\left( x \\right) \\right] \\right\\}\$ و %%FIG:{\"k\":\"t\",\"X\":{}}%% پایان"
        val transformed = FigTokenVisuals.transformation(Color.Black).filter(AnnotatedString(src))
        val out = transformed.text.text
        assertFalse("\\left" in out)
        assertFalse("$" in out)
        assertTrue("{[(x)]}" in out)
        assertTrue("⟦جدول⟧" in out)
        var prev = -1
        for (i in 0..src.length) { val v = transformed.offsetMapping.originalToTransformed(i); assertTrue(v >= prev); prev = v }
        assertEquals(out.length, transformed.offsetMapping.originalToTransformed(src.length))
        prev = -1
        for (i in 0..out.length) { val v = transformed.offsetMapping.transformedToOriginal(i); assertTrue(v >= prev); prev = v }
        assertEquals(src.length, transformed.offsetMapping.transformedToOriginal(out.length))
        assertEquals("فرمول", FigTokenVisuals.mathLabel("   "))
    }

    @Test
    fun `site options use token editor and desktop plus menu`() {
        val js = source("site/src/builder.js")
        assertTrue("function tokenTextarea(ta, opts) {" in js)
        assertTrue("var tt = tokenTextarea(t, {small: true}); tt.wrap.style.flex = '1';" in js)
        assertTrue("DK ? el('button', {class: 'icon-btn b-opt-plus', title: 'درج در گزینهٔ ' + fa(k + 1)" in js)
        assertTrue("function insertMenu(anchor, ta, q, o) {" in js)
        assertTrue("item('fx', 'فرمول', 'is-fx'" in js && "item('periodic', 'جدول تناوبی', 'is-pt'" in js && "item('gallery', 'گالری', 'is-ana'" in js)
        // به insertFigure نباید q داده شود (q.text را بازنویسی می‌کند)
        assertTrue("insertFigure('geo', ta, null)" in js && "atlasMenu(anchor, ta, null)" in js)
        assertTrue("var lt = tokenTextarea(l, {small: true}), rt = tokenTextarea(r, {small: true});" in js)
        assertTrue("if (opts.small) rich.addEventListener('keydown', function (e) { if (e.key === 'Enter') e.preventDefault(); });" in js)
        val css = source("site/src/site.css")
        assertTrue(".b-ta-wrap.sm .b-rich{min-height:42px!important;height:auto!important" in css)
        assertTrue(".b-ins-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}" in css)
    }
}
