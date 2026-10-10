package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V260.1 — سایت/دسکتاپ: کارت‌های صفحهٔ منو بزرگ‌تر با «پنل سریع» (میان‌برها؛ روی تنظیمات: روشن/تیره/دستگاه). */
class V260_1_SiteMenuQuickPanelTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `menu cards carry quick actions and theme toggles`() {
        val a = source("site/src/app.js")
        assertTrue("function quick(key) { var q = (QUICK[key] || []).filter(Boolean); return q.length ? el('span', {class: 'dk-mquick'}, q) : null; }" in a)
        assertTrue("[tb('LIGHT', 'روشن', QI.sun), tb('DARK', 'تیره', QI.moon), tb('SYSTEM', 'دستگاه', QI.auto)]" in a)
        assertTrue("qbtn('آزمون چاپی جدید', QI.plus" in a && "qbtn('ویرایش پروفایل', QI.user" in a)
        assertTrue("onclick: function (e) { e.stopPropagation(); on(); }" in a)
        val m = source("site/src/mobile.js")
        assertTrue("setSettingsTab: function (t) { settingsTab = t; }, appearance: appearance, setAppearance: setAppearance," in m)
        val css = source("site/src/site.css")
        assertTrue(".dk .dk-grid{grid-auto-rows:minmax(176px,auto);gap:14px}" in css)
        assertTrue(".dk .dk-qa.on{background:#39758a;border-color:#39758a;color:#fff}" in css)
    }
}
