package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V279 — فیلتر کارنامهٔ سایت: ردیف‌های انتخاب که پنجرهٔ انتخاب (جست‌وجو، تیک/دایره، همه/هیچ‌کدام) باز می‌کنند. */
class V279_SiteReportsPickerTest {
    private fun src(p: String) = File(p).readText()

    @Test
    fun `site reports filter uses picker rows and dialog`() {
        val x = src("site/src/extras.js")
        assertTrue("function pickerDlg(opts)" in x && "function pickRow(label, value, enabled, fn)" in x)
        assertTrue("class: 'rp-pick-list'" in x && "placeholder: 'جست‌وجو…'" in x && "text: 'همه'" in x && "text: 'هیچ‌کدام'" in x)
        assertTrue("text: 'تأیید'" in x && "text: 'انصراف'" in x)
        assertTrue("title: 'انتخاب مدرسه', multi: false" in x && "title: 'انتخاب کلاس', multi: false" in x)
        assertTrue("title: 'انتخاب دانش‌آموزان', multi: true" in x && "title: 'انتخاب آزمون‌ها', multi: true" in x)
        assertTrue("resetFrom(1); st.classId = ids[0];" in x)
        assertFalse("function stepBox(" in x)
        assertFalse("function chip(text, on, fn)" in x)
        val css = src("site/src/site.css")
        assertTrue(".rp-rows{" in css && ".rp-row{" in css && ".rp-pick-modal{" in css && ".rp-pick-row{" in css)
        assertFalse(".rp-step{" in css)
    }
}
