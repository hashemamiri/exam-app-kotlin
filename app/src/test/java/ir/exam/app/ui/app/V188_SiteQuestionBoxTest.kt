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
        assertTrue("if (c.classList.contains('sel')) { c.classList.remove('sel'); var ox2 = c.querySelector('.b-chip-x'); if (ox2) ox2.remove(); openTokEditor(c); } else selectChip(c);" in b)
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

    @Test
    fun `physics and chemistry are separate and selected objects get a delete cross`() {
        val b = source("site/src/builder.js")
        assertTrue("insertFigure('physics', ta, q)" in b && "insertFigure('chemistry', ta, q)" in b)
        assertTrue("var sciDom = kind === 'physics' ? 'phys' : kind === 'chemistry' ? 'chem' : null;" in b && "api.open(null, null, sciDom);" in b)
        assertTrue("function deleteTok(c)" in b && "class: 'b-chip-x'" in b && "deleteTok(xb.closest('[data-tok]'))" in b)
        assertTrue(".b-chip-x{position:absolute" in source("site/src/site.css"))
    }

    @Test
    fun `jalali picker shared, exam settings and header dates use it, desktop calendar compact`() {
        val a = source("site/src/app.js")
        assertTrue("window.SiteJalali = J;" in a && "function jalaliPicker(o)" in a && "function jalaliDisplay(d, withTime)" in a)
        assertTrue("err.textContent = 'زمان پایان نمی‌تواند قبل از زمان شروع باشد (' + jalaliDisplay(minD) + ').'" in a)
        assertTrue("var pk = /examDate|gradesDate|examDay/i.test(f.id) ? 'date' : /examTime|startTime/i.test(f.id) ? 'time' : null;" in a)
        assertTrue("var J = window.SiteJalali;" in source("site/src/admin.js"))
        val b = source("site/src/builder.js")
        assertTrue("function jdt(label, get, on, minGet)" in b && "jdt('زمان پایان (اختیاری)'" in b && "function () { return state.opensAt; })" in b)
        assertFalse("'datetime-local'" in b)
        val css = source("site/src/site.css")
        assertTrue(".jdp-d.dis,.jdp-d:disabled{" in css && ".dk .cal-d{min-height:64px" in css && ".jdt-btn{" in css)
    }

    @Test
    fun `desktop teacher rail has print above wallet`() {
        val a = source("site/src/app.js")
        assertTrue("railItems.splice(wi < 0 ? railItems.length : wi, 0, ['print', '🖨', 'چاپ آزمون']);" in a)
        assertTrue("[railItem('menu', 'منو')].concat(railItems.map(" in a)
        assertTrue("<rect x=\"3\" y=\"9\" width=\"18\" height=\"8.5\" rx=\"2.2\"/>" in a)
        // منوی گوشی/تبلت بدون تغییر
        assertTrue("['dashboard', '🏠', 'داشبورد'], ['exams', '📝', 'آزمون‌ها'], ['builder', '➕', 'آزمون جدید'], ['wallet', '👛', 'کیف پول'], ['cards', '🃏', 'کارت‌ها']" in a)
    }

    @Test
    fun `teacher dashboard has five uniform stat cards including manager requests`() {
        val a = source("site/src/app.js")
        assertTrue("var reqCard = statCard(r[4] ? fa(pendingN) : '—', 'درخواست مدیر', {onclick: function () { openManagerRequests(); }});" in a)
        assertTrue("c.appendChild(el('div', {class: 'grid5'}, [statCard(fa(r[0].length), 'آزمون', {panel: 'exams'})" in a)
        assertTrue("async function openManagerRequests()" in a && "managerRequestsCard(true)" in a)
        assertFalse("if (window.SiteSchool) c.appendChild(await window.SiteSchool.managerRequestsCard());" in a)
        val css = source("site/src/site.css")
        assertTrue(".grid5{display:grid;grid-template-columns:repeat(5,1fr);gap:16px}" in css && ".grid5 .stat{min-height:112px;justify-content:center}" in css)
        assertTrue(".grid2 .card+.card,.grid3 .card+.card,.grid4 .card+.card,.grid5 .card+.card{margin-top:0}" in css)
    }
}
