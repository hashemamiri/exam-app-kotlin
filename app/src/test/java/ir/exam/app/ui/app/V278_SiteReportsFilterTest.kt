package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V278 — سایت (دسکتاپ و موبایل): کارنامه با فیلتر پلکانی مدرسه→کلاس→دانش‌آموز→آزمون، بدون عنوان تکراری، آیکن کارنامهٔ تکی. */
class V278_SiteReportsFilterTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `site reports page mirrors app V277`() {
        val x = src("site/src/extras.js")
        assertFalse("rp-title" in x)
        assertTrue("S.rpcObj('native_teacher_schools_v61', {})" in x && "S.rpc('native_teacher_school_classes_v61', {p_school: id})" in x)
        assertTrue("'. مدرسه'" in x && "'. کلاس'" in x && "'. دانش‌آموزان'" in x && "'. آزمون‌ها'" in x)
        assertTrue("text: 'نمایش کارنامه‌ها'" in x && "if (st.classId && st.shown) compute();" in x)
        assertTrue("st.roster.filter(function (s) { return !!st.students[s.id]; })" in x)
        assertTrue("function reportCardModal(r, sel, cls)" in x && "class: 'rp-ic'" in x && "el('th', {class: 'num', text: 'کارنامه'})" in x)
        assertFalse("text: '📈 گزارش کلاس'" in x)
        val css = src("site/src/site.css")
        assertTrue(".rp-rows{" in css && ".rp-ic{" in css && ".rp-pick-modal{" in css) /* V279: ردیف‌های انتخاب جای چیپ‌ها */
    }
}
