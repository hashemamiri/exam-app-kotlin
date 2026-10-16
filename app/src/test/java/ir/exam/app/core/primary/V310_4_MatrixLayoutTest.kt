package ir.exam.app.core.primary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V310.4 — جدول ماتریسی: اندازه از محتوا، نه از کادر؛ ویرایشگر و پیش‌نمایش دسکتاپ و موبایل.
 */
class V310_4_MatrixLayoutTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    private val web = "app/src/main/assets/print/web/"

    @Test
    fun `matrix markup sizes to content with inline styles`() {
        val js = File(root(), web + "table_fig.js").readText()
        assertTrue(js.contains("width:max-content;max-width:100%;margin:0 auto"))
        assertTrue(js.contains("style=\"width:auto;max-width:100%;table-layout:auto;margin:0\""))
    }

    @Test
    fun `matrix wrap no longer stretches to full width in stylesheet`() {
        val css = File(root(), web + "tools_styles.css").readText()
        assertTrue(css.contains(".tbx-wrap{display:flex;align-items:stretch;justify-content:center;gap:7px;width:max-content;max-width:100%;margin:0 auto}"))
        assertTrue(!css.contains("gap:7px;width:100%}"))
    }

    @Test
    fun `editor matrix inputs are capped and mobile friendly`() {
        val css = File(root(), web + "tools_styles.css").readText()
        assertTrue(css.contains(".tb-edit .tbx-matrix .tbx-t input{width:auto;min-width:44px;max-width:120px}"))
        assertTrue(css.contains("@media (max-width:600px){.tb-edit .tbx-matrix .tbx-t input{min-width:36px;max-width:88px}}"))
    }
}
