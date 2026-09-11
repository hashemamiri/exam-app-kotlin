package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V165 — پوستهٔ دسکتاپ سایت به سبک «نسخهٔ یکپارچه»: ریل عمودی سمت راست، نوار بالا، صفحهٔ «منو» با کارت‌ها، پالت سبزآبی/بنفش. حالت گوشی دست‌نخورده. */
class V165_SiteDesktopRailTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `desktop panel renders rail topbar and menu page`() {
        val a = source("site/src/app.js")
        assertTrue("function renderPanel()" in a && "document.body.classList.add('dk')" in a && "document.body.classList.remove('dk')" in a)
        assertTrue("function railItem(key, label)" in a && "[railItem('menu', 'منو')].concat(items.map(" in a)
        assertTrue("class: 'dk-rail', 'aria-label': 'نوار اصلی'" in a && "'--n:' + (items.length + 1)" in a)
        assertTrue("class: 'head dk-top'" in a && "dkIcon('brand', 'dk-mark')" in a && "dkGo(view.panel === 'menu' ? 'dashboard' : 'menu')" in a)
        assertTrue("function pageMenu(c)" in a && "var pages = {menu: pageMenu, dashboard: pageDashboard" in a)
        assertTrue("class: 'dk-profile'" in a && "'پروفایل ' + ROLE_LABEL[user.role]" in a && "mcard('logout', 'خروج', 'خروج امن و تعویض حساب', true, doLogout)" in a)
        assertTrue("[side, sbBg, main, bottom, rail]" in a)
        for (k in listOf("dashboard", "exams", "builder", "classes", "students", "bank", "reports", "grading", "calendar", "wallet", "tools", "profile", "join", "grades", "teachers", "school", "logout", "menu", "back", "brand"))
            assertTrue("icon $k", "    $k: '<" in a)
        assertFalse("(فاز ۲ سایت)" in a)
    }

    @Test
    fun `desktop css is scoped to body dk and wide screens`() {
        val css = source("site/src/site.css")
        assertTrue("body.dk{--bg:#eef2f5;--card:#fff;--ink:#1d2935;--muted:#6c7985;--line:#d9e1e6;--brand:#39758a;" in css)
        assertTrue("@media (min-width:861px){" in css && ".dk .sidebar,.dk .sb-bg,.dk .bottom-nav,.dk .hamb{display:none !important}" in css)
        assertTrue(".dk .dk-rail{--size:52px;position:fixed;z-index:50;right:14px;top:22px;bottom:22px;width:72px;" in css)
        assertTrue(".dk .dk-rail-item.active{color:var(--dk-neon-ink);border-color:var(--dk-neon);" in css)
        assertTrue(".dk .dk-rail-item .dk-rail-label{position:absolute;top:50%;right:calc(100% + 10px);" in css)
        assertTrue(".dk .dk-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));grid-auto-rows:108px;gap:9px}" in css)
        assertTrue(".dk .app{display:block;min-height:100vh;overflow-x:clip}" in css)
        // پوستهٔ گوشی (V149–V161) دست‌نخورده
        assertTrue(".m-mode{--m-bg:#E9EEF5;" in css && ".m-dock-panel{pointer-events:auto;height:64px" in css)
    }
}
