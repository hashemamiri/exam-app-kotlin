package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V253 — لوگوی جدید «آزمون» در اپ (آیکون تطبیقی + لایهٔ تک‌رنگ) و سایت (favicon، PWA، برند). */
class V253_NewLogoTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()
    private fun exists(path: String) = File(root(), path).exists()

    @Test
    fun `app uses adaptive launcher icon with monochrome layer`() {
        val m = source("app/src/main/AndroidManifest.xml")
        assertTrue("android:icon=\"@mipmap/ic_launcher\"" in m && "android:roundIcon=\"@mipmap/ic_launcher_round\"" in m)
        assertTrue("android:resource=\"@drawable/ic_launcher_monochrome\"" in m)
        val icon = source("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        assertTrue("<monochrome android:drawable=\"@drawable/ic_launcher_monochrome\" />" in icon && "@mipmap/ic_launcher_foreground" in icon)
        for (d in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")) for (f in listOf("ic_launcher", "ic_launcher_round", "ic_launcher_foreground")) assertTrue(exists("app/src/main/res/mipmap-$d/$f.png"))
        assertFalse(exists("app/src/main/res/drawable/ic_exam_app.xml"))
        assertTrue(".setSmallIcon(R.drawable.ic_launcher_monochrome)" in source("app/src/main/java/ir/exam/app/core/push/PushMessagingService.kt"))
        assertTrue(exists("store/play-icon-512.png"))
    }

    @Test
    fun `desktop student exam head does not cover question numbers and rail labels wrap`() {
        val css = source("site/src/site.css")
        assertTrue(".dk .st-head{top:12px}" in css && ".dk .st-nav{position:relative;z-index:1" in css)
        assertTrue("white-space:normal;max-width:66px;text-align:center;overflow:visible" in css)
        assertFalse("white-space:nowrap;max-width:66px;overflow:hidden;text-overflow:ellipsis" in css)
        assertTrue(".pwa-bar{bottom:auto;top:16px;inset-inline:0;margin-inline:auto;width:max-content" in css) // V253.2
    }

    @Test
    fun `desktop calendar shows month on one half and day messages on the other`() {
        val a = source("site/src/admin.js")
        assertTrue("else c.appendChild(el('div', {class: 'cal-wrap'}, [el('div', {class: 'card'}, [grid]), side]));" in a)
        // V253.4 — دسکتاپ: ناوبری ماه بالای تقویم، «پیام جدید» وسطِ بالای پیام‌ها، بدون «پیام برای این روز»
        assertTrue("[el('div', {class: 'cal-col'}, [navRow, el('div', {class: 'card'}, [grid])]), el('div', {class: 'cal-col'}, [el('div', {class: 'row cal-newrow', style: 'justify-content:center;margin-bottom:12px'}, [newBtn].filter(Boolean)), side])]" in a)
        assertFalse("side.appendChild(el('div', {class: 'row', style: 'margin-top:12px'}, [el('button', {class: 'btn sm', text: '➕ پیام برای این روز'" in a)
        assertTrue("if (!isDk()) { if (newBtn) navRow.appendChild(newBtn); c.appendChild(navRow); }" in a)
        assertTrue("if (isDk()) showDay(d); else dayDlg(iso, y, m, d, ns, hs, isTeacher" in a)
        assertTrue("showDay(arg.d || (today.jy === y && today.jm === m ? today.jd : 1));" in a)
        val css = source("site/src/site.css")
        assertTrue(".dk .cal-nav>button:nth-child(3){order:1}" in css && ".dk .cal-nav>button:nth-child(1){order:3}" in css) // V253.5
        assertTrue(".cal-side{display:none}" in css && ".dk .cal-wrap{display:grid;grid-template-columns:1fr 1fr" in css && ".dk .cal-side{display:block" in css)
    }

    @Test
    fun `site brand favicon and pwa use v3 icons`() {
        for (f in listOf("icon-192", "icon-512", "maskable-192", "maskable-512", "apple-touch-icon", "favicon-48", "mark-256")) assertTrue(exists("site/pwa/v3/$f.png"))
        assertFalse("/pwa/v2/" in source("site/src/template.html") || "/pwa/v2/" in source("site/pwa/manifest.webmanifest") || "/pwa/v2/" in source("site/pwa/sw.js"))
        val a = source("site/src/app.js")
        assertTrue("href=\"/pwa/v3/mark-256.png\"" in a && "src: '/pwa/v3/icon-192.png'" in a)
        assertFalse("src: '/pwa/icon-192.png'" in a)
    }
}
