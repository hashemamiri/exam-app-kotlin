package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V259.2 — سایت/دسکتاپ: پیش‌نمایش دانش‌آموز بلند با سربرگ ثابت و ✗ قرمز، بدنهٔ اسکرول بدون نوار، بدون دکمهٔ بستن پایین؛ ریل با نام زیر دکمه‌ها و کارت «بارم». */
class V259_2_SitePreviewRailTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `student preview has fixed head and hidden footer on desktop`() {
        val js = source("site/src/builder.js")
        val css = source("site/src/site.css")
        assertTrue("class: 'row b-sp-head'" in js && "class: 'x b-sp-x'" in js && "class: 'row b-sp-foot'" in js)
        assertTrue(".dk .b-sp-modal{height:92vh;max-height:92vh;display:flex;flex-direction:column;overflow:hidden;" in css)
        assertTrue(".dk .b-sp-modal .b-sp-body{flex:1 1 auto;min-height:0;max-height:none;overflow-y:auto;overflow-x:hidden;scrollbar-width:none;" in css)
        assertTrue(".dk .b-sp-modal .b-sp-foot{display:none}" in css)
        assertTrue(".dk .b-sp-modal .b-sp-x{position:static;" in css && "color:#d6336c" in css)
    }

    @Test
    fun `rail buttons carry labels and a total score card`() {
        val js = source("site/src/builder.js")
        val css = source("site/src/site.css")
        assertTrue("function rb(icon, label, on, cls, short)" in js)
        assertTrue("class: 'b-rail-lbl', text: short || label" in js)
        assertTrue("'پیش‌نمایش'));" in js && "'افزودن'));" in js && "'ذخیره'));" in js && "'چاپ'));" in js)
        assertTrue("class: 'b-rail-score', title: 'جمع بارم آزمون'" in js && "function totalScore()" in js)
        assertTrue("rail.querySelector('.b-rail-score-v')" in js)
        assertTrue(".dk .b-rail-btn{width:68px;height:auto;min-height:56px;flex-direction:column;" in css)
        assertTrue(".dk .b-rail-score{width:68px;" in css)
    }
}
