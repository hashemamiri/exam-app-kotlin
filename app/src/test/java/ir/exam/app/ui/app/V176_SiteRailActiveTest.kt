package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V176 — ریل دسکتاپ: صفحه‌های بازشده از «منو» (چاپ، تقویم، حساب، تنظیمات، کارنامه، …) مورد «منو» را روشن نگه می‌دارند. */
class V176_SiteRailActiveTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }

    @Test
    fun `rail highlights menu for sub pages`() {
        val a = File(root(), "site/src/app.js").readText()
        assertTrue("var railActive = railKeys.indexOf(view.panel) >= 0 ? view.panel : 'menu';" in a)
        assertTrue("class: 'dk-rail-item' + (railActive === key ? ' active' : '')" in a)
    }
}
