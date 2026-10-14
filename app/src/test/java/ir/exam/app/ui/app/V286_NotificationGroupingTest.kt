package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V286 — مرکز اعلان: گروه‌بندی زیر عنوان دسته در تب «همه» (اپ + سایت)؛ نام زنگوله در ریل دسکتاپ؛ جای زنگوله در سایت گوشی مثل اپ. */
class V286_NotificationGroupingTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `app groups notifications under category headers in the All tab`() {
        val kt = src("app/src/main/java/ir/exam/app/ui/notifications/NotificationCenter.kt")
        assertTrue("val groups: List<Pair<String?, List<AppNotification>>> = if (tab == \"همه\")" in kt)
        assertTrue("if (cat != null) item(key = \"h-\$cat\")" in kt)
    }

    @Test
    fun `site groups notifications, names the desktop bell and moves the mobile bell to the top bar`() {
        val push = src("site/src/push.js")
        assertTrue("list.appendChild(el('div', {class: 'nc-cat', text: cc}))" in push)
        assertTrue("b.appendChild(el('span', {class: 'dk-rail-label', text: 'اعلان‌ها'}))" in push)
        val css = src("site/src/site.css")
        assertTrue(".nc-bell{position:fixed;left:12px;top:calc(8px + env(safe-area-inset-top));" in css)
        assertTrue(".m-mode .m-content.no-head{padding-top:calc(54px + env(safe-area-inset-top))}" in css)
        assertTrue(".dk .nc-bell.nc-rail{width:68px;height:auto;" in css)
        assertTrue(".nc-cat{" in css)
    }
}
