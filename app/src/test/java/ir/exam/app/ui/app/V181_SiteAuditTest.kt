package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V181 — ممیزی سایت: نشت شنوندهٔ selectionchange در ویرایشگر تراشه‌ای، کلید تکراری، قلم base64 تکراری. */
class V181_SiteAuditTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `builder registers one global selectionchange listener`() {
        val b = source("site/src/builder.js")
        assertTrue("if (!window.__bRichSel)" in b && "rich.__caretToRaw = caretToRaw;" in b)
        assertEquals(1, Regex("addEventListener\\('selectionchange'").findAll(b).count())
        assertEquals(1, Regex("var LS_PRINT = ").findAll(b).count())
    }

    @Test
    fun `mobile icon map has no duplicate keys and index has no embedded vazir font`() {
        val m = source("site/src/mobile.js")
        assertEquals(1, Regex("\\n    lock: '").findAll(m).count())
        val build = source("site/build_site.py")
        assertFalse("read(os.path.join(WEB, \"vazirmatn_embed.css\"))" in build)
        assertTrue("/fonts/Vazirmatn-400.woff2" in source("site/src/site.css"))
    }
}
