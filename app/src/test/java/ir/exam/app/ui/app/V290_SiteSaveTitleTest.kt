package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V290 — سایت دسکتاپ: ذخیرهٔ آزمون بدون عنوان → پیام دیده شود (toast) و کادر عنوان فوکوس/هایلایت شود (نوار بالا در دسکتاپ پنهان است). */
class V290_SiteSaveTitleTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun saveErrorIsToastedAndTitleFocused() {
        val b = src("site/src/builder.js")
        assertTrue("te.needTitle = true; throw te;" in b)
        assertTrue("toast(errMsg(e), 'err');\n        if (e && e.needTitle) focusTitleField();" in b)
        assertTrue("function focusTitleField()" in b)
        assertTrue("if (focusIn(settings)) return;" in b)
        assertTrue("openSettings(); setTimeout(function () { focusIn(document.querySelector('.modal.wide')); }, 50);" in b)
        assertTrue(".dk .builder .b-top{display:none}" in src("site/src/site.css"))
        assertTrue(".builder input.need-title,.modal input.need-title{" in src("site/src/site.css"))
    }
}
