package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V179 — کارایی پیش‌نمایش چاپ: ناظرهای DOM بندانگشتی‌ها را نادیده می‌گیرند، برگه‌ها contain:paint، پوستهٔ سایت زیر موتور پنهان. */
class V179_PrintPreviewPerfTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `preview engine avoids redundant work`() {
        assertTrue("t.closest('#pgsViewer, #previewArea, .pgs-thumbs')" in source("app/src/main/assets/print/web/qimg_uploader.js"))
        assertTrue("t.closest('.pgs-thumbs')" in source("app/src/main/assets/print/web/ui_v2_runtime.js"))
        val css = source("app/src/main/assets/print/web/pgs_style.css")
        assertTrue("#pgsViewer .pgs-sheet{contain:paint;}" in css && "content-visibility:auto" in css)
        val a = source("site/src/app.js")
        assertTrue("document.body.classList.add('engine-open')" in a && "document.body.classList.remove('engine-open')" in a)
        assertTrue("body.engine-open > #root{visibility:hidden;pointer-events:none}" in source("site/src/site.css"))
    }
}
