package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V151 — صفحه‌های داخلی موبایل: جدول → کارت نئومورفیک، ورودی‌ها/دکمه‌ها به سبک اپ. */
class V151_SiteMobileInnerPagesTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `tables become cards only inside mobile shell and keep action nodes`() {
        val m = source("site/src/mobile.js")
        assertTrue("function tableToCards(t)" in m && "t.dataset.mCards = '1'" in m && "while (td.firstChild) acts.appendChild(td.firstChild)" in m)
        assertTrue("function upgradeContent() {\n    if (!active()) return;" in m)
        val css = source("site/src/site.css")
        assertTrue(".m-rowcard{border-radius:18px" in css && ".m-mode .m-content .icon-btn{width:40px" in css && ".m-mode .m-content .balance{" in css)
    }
}
