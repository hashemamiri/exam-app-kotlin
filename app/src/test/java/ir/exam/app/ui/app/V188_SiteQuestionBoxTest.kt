package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V188 — دسکتاپ: کادر متن سؤال/نوار ابزار مثل فایل مرجع؛ پیش‌نمایش دانش‌آموز با فرمول/شکل/تصویر درست. */
class V188_SiteQuestionBoxTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `question box styled like reference and tools tinted`() {
        val b = source("site/src/builder.js")
        assertTrue("placeholder: 'متن سؤال را بنویسید؛ برای فرمول از دکمهٔ ∑ استفاده کنید'" in b)
        assertTrue("class: 'tool-btn is-fx'" in b && "class: 'tool-btn is-fig'" in b && "class: 'tool-btn is-mic'" in b)
        val css = source("site/src/site.css")
        assertTrue(".dk .b-editor .b-rich{min-height:9.5em;max-height:none;height:9.5em" in css && "resize:vertical" in css)
        assertTrue(".dk .b-editor .tool-btn{width:52px;height:52px;border:0;border-radius:16px" in css && ".dk .b-editor .tool-btn.is-fx{background:#eef2ff;color:#5b52e0}" in css)
    }

    @Test
    fun `student preview gets math css scope and image uri`() {
        val b = source("site/src/builder.js")
        assertTrue("return '.b-live ' + x.trim() + ',.b-sp-card ' + x.trim() + ',.b-rich ' + x.trim(); }).join(',')" in b)
        assertTrue("var src = u && typeof u === 'object' ? u.uri : u;" in b)
        assertTrue("var card = el('div', {class: 'card b-sp-card'}); previewCss();" in b)
        assertTrue(".b-sp-text{white-space:pre-wrap;word-break:break-word" in source("site/src/site.css"))
    }

    @Test
    fun `desktop question box renders tokens inline and site-only commits skip app CI`() {
        val b = source("site/src/builder.js")
        assertTrue("var WYSIWYG = document.body.classList.contains('dk');" in b)
        assertTrue("if (WYSIWYG) ensurePreviewFrame().then(function (w) { if (!w) return; try { var h = w.renderRichText(tok, null); if (h) { c.innerHTML = h; simplifyFigs(c); } } catch (e) {} });" in b)
        assertTrue(",.b-rich ' + x.trim(); }).join(',')" in b)
        val css = source("site/src/site.css")
        assertTrue(".b-chip.live{background:transparent" in css && ".dk .b-editor .b-live{display:none!important}" in css)
        val ci = source(".github/workflows/android.yml")
        assertTrue("'relay/**', 'supabase/**'" in ci && "'app/src/test/**/V1*_Site*Test.kt'" in ci)
    }

    @Test
    fun `desktop exam card actions wrap inside card`() {
        val css = source("site/src/site.css")
        assertTrue(".dk .exam .acts.acts-text{display:flex;flex-wrap:wrap" in css)
        assertTrue(".dk .exam .acts.acts-text .icon-btn{flex:1 1 0;min-width:max-content" in css)
    }

    @Test
    fun `desktop question box edits objects on second click and simplifies atlas figures`() {
        val b = source("site/src/builder.js")
        assertTrue("function simplifyFigs(root)" in source("site/src/student.js") && "window.SiteStudent.simplifyFigs(root)" in b)
        assertTrue("if (c.classList.contains('sel')) { c.classList.remove('sel'); openTokEditor(c); } else selectChip(c);" in b)
        assertTrue("function editFigure(ta, tok)" in b && "insertFigure(kind, ta, null, tok);" in b)
        assertTrue("if (!w.GeoFig.openFromEl(fig)) {" in b && "attributeFilter: ['data-fig']" in b)
        assertTrue("c.innerHTML = h; simplifyFigs(c);" in b && "simplifyFigs(t.content); return t.innerHTML;" in source("site/src/student.js"))
        val css = source("site/src/site.css")
        assertTrue(".b-chip.sel{outline:2px solid var(--brand)" in css && ".b-fig-frame svg.an-ov{position:absolute" in css && ".b-fig-af .an-af-box{" in css)
    }

    @Test
    fun `desktop question box is user-resizable`() {
        val css = source("site/src/site.css")
        assertTrue(".dk .b-editor .b-rich{min-height:9.5em;max-height:none;height:9.5em;overflow-y:auto;resize:vertical;" in css)
        assertTrue(".dk .b-editor .b-rich{max-height:none!important;min-height:9.5em!important}" in css)
    }

    @Test
    fun `desktop grip handle, atlas menu with own photo, larger student preview`() {
        val b = source("site/src/builder.js")
        assertTrue("var grip = el('div', {class: 'b-grip'" in b && "rich.style.height = Math.max(minH, h0 + (ev.clientY - y0)) + 'px';" in b)
        assertTrue("function atlasMenu(anchor, ta, q)" in b && "function pickPhotoForAtlas(ta, q)" in b)
        assertTrue("{k: 'a', t: 'photo', X: {img: data, title: '', lab: '1', marks: [], blank: '1', mkName: '0'}}" in b)
        assertTrue("function insertFigure(kind, ta, q, editTok, presetSpec)" in b && "api.open(presetSpec, null);" in b)
        assertFalse("class: 'tool-btn is-sci'" in b)
        val css = source("site/src/site.css")
        assertTrue(".dk .b-editor .b-rich{resize:none!important}" in css && ".b-grip{" in css && ".b-atlas-menu{" in css)
        assertTrue(".dk .b-sp-modal{max-width:min(1100px,94vw)}" in css)
        assertFalse("::-webkit-resizer" in css)
    }
}
