package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V256 — سایت دسکتاپ: آیکون‌های ایموجی/متنی در el() به SVG خطی تبدیل می‌شوند (فقط body.dk). */
class V256_SiteDesktopIconsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `el converts leading emoji to svg on desktop only`() {
        val a = source("site/src/app.js")
        assertTrue("var EMOJI_SVG = {" in a)
        assertTrue("if (!document.body.classList.contains('dk')) return null;" in a)
        assertTrue("function dkIconify(str, isHtml) {" in a)
        assertTrue("var _ik = typeof attrs.html === 'string' ? dkIconify(attrs.html, true) : dkIconify(attrs.text, false);" in a)
        for (k in listOf("\"🗑\"", "\"🖨\"", "\"✕\"", "\"➕\"", "\"📥\"", "\"🎓\"", "\"🏫\"", "\"👩‍🏫\"", "\"→\"", "\"←\"", "\"✅\"", "\"⚡\"", "\"💾\"", "\"🃏\"")) assertTrue(k, "$k: \"<" in a)
        val css = source("site/src/site.css")
        assertTrue(".dk svg.ico{width:1.15em;height:1.15em;vertical-align:-.22em;margin-inline-end:.4em;flex:none}" in css)
        assertTrue(".dk svg.ico.lone{margin-inline-end:0}" in css)
    }
}
