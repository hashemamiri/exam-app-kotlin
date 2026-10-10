package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V259.1 — سایت/دسکتاپ: بخش‌های پایین ویرایشگر سؤال (قالب متن، فضای پاسخ، چیدمان گزینه‌ها، پاسخ تصویری) در کارت‌های مرتب با عنوان و کنترل‌های هم‌ارتفاع. */
class V259_1_SiteEditorSectionsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `builder wraps lower groups in b-sec sections`() {
        val js = source("site/src/builder.js")
        assertTrue("function sec(title, children, cls)" in js)
        assertTrue("'b-sec-fmt'" in js)
        assertTrue("sec('فضای پاسخ در برگهٔ چاپی'" in js)
        assertTrue("sec('چیدمان گزینه‌ها در برگهٔ چاپی'" in js)
        assertTrue("sec('پاسخ تصویری / تختهٔ دانش‌آموز'" in js)
        assertTrue("class: 'field b-sec-chkfield'" in js)
    }

    @Test
    fun `desktop css styles sections and uniform control heights`() {
        val css = source("site/src/site.css")
        assertTrue(".dk .b-editor .b-sec{border:1px solid var(--line);border-radius:16px;" in css)
        assertTrue(".dk .b-editor .b-sec>h4.b-sec-t:after{content:'';flex:1;height:1px;background:var(--line)}" in css)
        assertTrue("height:44px;min-height:44px;padding:0 14px;border-radius:12px;font-size:14px;box-sizing:border-box" in css)
        assertTrue("html body.dk:not(.m-mode) .b-editor .b-sec-chkfield label:has(> input[type=checkbox]){height:44px;" in css)
        assertTrue(".dk .b-editor .b-type{border-top:0;margin-top:14px;" in css)
    }
}
