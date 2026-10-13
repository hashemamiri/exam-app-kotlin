package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V271 — تأخیر تایپ در پیش‌نمایش چاپ (V179): رندر با تأخیر ۲۲۰ms پس از آخرین کلید و لغو موج‌های صفحه‌بندی قبلی. */
class V271_PrintPreviewDebounceTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `typing is debounced and pagination waves are cancellable`() {
        val ms = src("app/src/main/assets/print/web/mainscript.js")
        assertTrue("function renderPreviewSoon(delay) {" in ms)
        assertTrue("window.renderPreviewSoon = renderPreviewSoon;" in ms)
        assertTrue("if (q) q[field] = value; renderPreviewSoon(); } /* V271 */" in ms)
        val pgs = src("app/src/main/assets/print/web/pgs_engine.js")
        assertTrue("var pagWaves = [];" in pgs)
        assertTrue("pagWaves.push(setTimeout(paginateIfChanged, 160));" in pgs)
    }
}
