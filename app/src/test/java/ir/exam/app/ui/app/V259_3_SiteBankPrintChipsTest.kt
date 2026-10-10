package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V259.3 — سایت/دسکتاپ: بانک سؤال برای آزمون چاپی هم باز می‌شود؛ چیپ‌های تیک‌دار یک‌خطی (✓ کنار جمله). */
class V259_3_SiteBankPrintChipsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `bank allowed on desktop for print exams`() {
        val js = source("site/src/builder.js")
        assertTrue("function bankAllowed() { return state.mode === 'online' || document.body.classList.contains('dk'); }" in js)
        assertTrue("if (key === 'bank') { if (bankAllowed()) openBank();" in js)
        assertTrue(".concat(bankAllowed() ? [el('button', {class: 'btn soft sm', text: '🏦 از بانک سؤال'" in js)
        assertTrue("if (bankAllowed()) list.appendChild(el('button', {class: 'btn soft sm', style: 'width:100%;margin-top:8px', text: '🏦 از بانک سؤال'" in js)
    }

    @Test
    fun `desktop checkbox chips never wrap`() {
        val css = source("site/src/site.css")
        assertTrue("html body.dk:not(.m-mode) label:has(> input[type=checkbox]){flex-wrap:nowrap;white-space:nowrap;flex-direction:row}" in css)
    }
}
