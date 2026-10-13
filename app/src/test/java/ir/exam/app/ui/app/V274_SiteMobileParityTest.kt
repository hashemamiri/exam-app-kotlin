package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V274 — هم‌ترازی سایت گوشی با دسکتاپ: منوی + گزینه‌ها/جورکردنی، چشم پیش‌نمایش دانش‌آموز در سازندهٔ آنلاین، بانک در آزمون چاپی. */
class V274_SiteMobileParityTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `mobile gets insert menu student preview and bank`() {
        val js = src("site/src/builder.js")
        assertTrue("function bankAllowed() { return true; }" in js)
        assertTrue("window.__builderStudentPreview = studentPreview;" in js)
        assertTrue("var lp = el('button', {class: 'icon-btn b-opt-plus', title: 'درج در مورد راست ' + fa(k + 1)" in js)
        assertTrue(!js.contains("DK ? el('button', {class: 'icon-btn b-opt-plus'"))
        val m = src("site/src/mobile.js")
        assertTrue("'aria-label': 'پیش‌نمایش دانش‌آموز', onclick: function () { window.__builderStudentPreview(); }" in m)
        val css = src("site/src/site.css")
        assertTrue(".m-mode .b-ins-menu{width:min(300px,calc(100vw - 16px))}" in css)
    }
}
