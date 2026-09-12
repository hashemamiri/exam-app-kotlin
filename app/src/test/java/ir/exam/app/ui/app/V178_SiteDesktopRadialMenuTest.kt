package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V178 — دکمهٔ + ریل سازندهٔ دسکتاپ همان منوی شعاعی اپ (BuilderRadialMenuOverlay) را باز می‌کند؛ منوی شعاعی گوشی و دسکتاپ مشترک شد. */
class V178_SiteDesktopRadialMenuTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `desktop plus opens the radial menu`() {
        val m = source("site/src/mobile.js")
        assertTrue("function radialMenu(onPick, onClose, opts)" in m && "window.SiteMobile = {radialMenu: radialMenu," in m)
        assertTrue("radial = radialMenu(function (key) {" in m)
        val b = source("site/src/builder.js")
        assertTrue("window.SiteMobile.radialMenu(function (key) {" in b && "{cls: 'dk-radial', emoji: true}" in b)
        assertTrue("state.questions.push(newQuestion(key)); state.selected = state.questions.length - 1; mark(); drawList(); drawEditor();" in b)
        assertTrue(".dk .m-radial-bg{z-index:70" in source("site/src/site.css"))
    }
}
