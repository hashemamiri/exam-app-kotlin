package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V174 — هدر بالای دسکتاپ سایت (عنوان/نشان/☰) کامل حذف شد. */
class V174_SiteDesktopNoHeaderTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }

    @Test
    fun `desktop top header is hidden`() {
        val c = File(root(), "site/src/site.css").readText()
        assertTrue(".dk .main .head.dk-top{display:none}" in c)
        assertTrue(".dk .b-settings{top:24px;max-height:calc(100vh - 48px)}" in c)
    }
}
