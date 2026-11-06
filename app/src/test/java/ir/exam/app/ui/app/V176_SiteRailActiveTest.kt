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
        /* V344.1 — کارنامهٔ PDF زیرمجموعهٔ «کارت‌ها» است، پس همان دکمه روشن می‌ماند؛
           بقیهٔ صفحه‌های فرعی همچنان «منو» را روشن نگه می‌دارند. */
        assertTrue("var railPanel = view.panel === 'reportcards' ? 'cards' : view.panel;" in a)
        assertTrue("var railActive = railKeys.indexOf(railPanel) >= 0 ? railPanel : 'menu';" in a)
        assertTrue("class: 'dk-rail-item' + (railActive === key ? ' active' : '')" in a)
    }
}
