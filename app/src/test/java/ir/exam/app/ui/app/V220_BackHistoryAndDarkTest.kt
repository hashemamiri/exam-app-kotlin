package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V220 — برگشت دستگاه به صفحهٔ قبلی (اپ و سایت) بدون خروج؛ رنگ محتوای پیش‌فرض تم؛ ریل‌های دسکتاپ در حالت تیره. */
class V220_BackHistoryAndDarkTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun appBackUsesPageHistoryAndNeverExits() {
        val app = src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("BackHandler(enabled = !menuOpen && !quickAddOpen) {" in app)
        assertTrue("val previous = pageHistory.removeLastOrNull()" in app)
        assertFalse("page != roleHomePage" in app)
    }

    @Test
    fun themeProvidesContentColor() {
        val t = src("app/src/main/java/ir/exam/app/core/ui/ExamAppTheme.kt")
        assertTrue("LocalContentColor provides colors.onBackground" in t)
    }

    @Test
    fun siteBackUsesNavStack() {
        val app = src("site/src/app.js")
        val mob = src("site/src/mobile.js")
        assertTrue("function navTrack()" in app && "navBack: navBack" in app)
        assertTrue("if (S.navBack && S.navBack()) { pushHist(); return; }" in mob)
        assertFalse("از سایت خارج می‌شوید؟" in mob)
        assertTrue("function pushHist() { if (backGuard) return;" in mob)
    }

    @Test
    fun desktopRailsDark() {
        val css = src("site/src/site.css")
        assertTrue(".m-dark .dk .dk-rail,.m-dark .dk .b-rail{background:linear-gradient(180deg,rgba(38,47,59,.97)" in css)
    }
}
