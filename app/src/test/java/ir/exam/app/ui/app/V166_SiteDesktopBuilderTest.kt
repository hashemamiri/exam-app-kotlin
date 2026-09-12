package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V166 — سازندهٔ آزمون دسکتاپ سایت به سبک «نسخهٔ یکپارچه»: ریل چپ، «مشخصات آزمون» در ستون راست، تراشه‌های نوع سؤال. گوشی دست‌نخورده. */
class V166_SiteDesktopBuilderTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `builder has shared settings form left rail and type chips`() {
        val b = source("site/src/builder.js")
        assertTrue("function settingsForm(m_)" in b && "settingsForm(m);" in b && "settingsForm(settings);" in b)
        assertTrue("class: 'b-settings card'" in b && "settings.addEventListener('input', syncMeta)" in b && "function syncMeta()" in b)
        assertTrue("class: 'b-rail', 'aria-label': 'ابزار سازنده'" in b && "function drawRail()" in b && "function addMenu(anchor)" in b)
        assertTrue("'افزودن سؤال', function (e) { addMenu(e.currentTarget); }, 'add'" in b && "'سؤال بعدی'" !in b && "preview('teacher'); }" in b)
        assertTrue("class: 'b-rail-num' + (i === state.selected ? ' active' : '')" in b)
        assertTrue("class: 'b-types'" in b && "function changeType(q, t)" in b && "changeType(q, t[0]); mark(); drawList(); drawEditor();" in b)
        assertTrue("' has-settings'" in b && "wrap.appendChild(rail);" in b)
    }

    @Test
    fun `desktop builder css is scoped to body dk`() {
        val css = source("site/src/site.css")
        assertTrue(".b-rail,.b-types,.b-settings{display:none}" in css)
        assertTrue(".dk .b-body.has-settings{grid-template-columns:290px minmax(0,1fr)}" in css && ".dk .b-list{display:none}" in css)
        assertTrue(".dk .b-rail{display:flex;flex-direction:column;align-items:center;gap:8px;position:fixed;left:14px;" in css)
        assertTrue(".dk .b-rail-num.active{color:#0e93b8;border-color:#22c6ef;" in css && ".dk .tool-btn{width:56px;height:56px;border-radius:16px;" in css)
        assertTrue(".m-builder .b-body{display:flex;flex-direction:column;gap:12px}" in css)
    }
}
