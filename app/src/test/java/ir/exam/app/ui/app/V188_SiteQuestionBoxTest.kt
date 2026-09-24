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
        assertTrue(".dk .b-editor .b-rich{min-height:9.5em;max-height:9.5em" in css && "resize:vertical" in css)
        assertTrue(".dk .b-editor .tool-btn{width:52px;height:52px;border:0;border-radius:16px" in css && ".dk .b-editor .tool-btn.is-fx{background:#eef2ff;color:#5b52e0}" in css)
    }

    @Test
    fun `student preview gets math css scope and image uri`() {
        val b = source("site/src/builder.js")
        assertTrue("return '.b-live ' + x.trim() + ',.b-sp-card ' + x.trim(); }).join(',')" in b)
        assertTrue("var src = u && typeof u === 'object' ? u.uri : u;" in b)
        assertTrue("var card = el('div', {class: 'card b-sp-card'}); previewCss();" in b)
        assertTrue(".b-sp-text{white-space:pre-wrap;word-break:break-word" in source("site/src/site.css"))
    }
}
