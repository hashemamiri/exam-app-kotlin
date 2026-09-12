package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V170 — هر کارت منوی دسکتاپ همان صفحهٔ اپ را باز می‌کند: «حساب» و «تنظیمات» صفحه‌های گوشی (پروفایل/حساب/سربرگ و ظاهر/داده‌ها/درباره) هستند؛ «خروج» با تأیید. */
class V170_SiteDesktopMenuTargetsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `menu cards open the same screens as the app`() {
        val a = source("site/src/app.js")
        assertTrue("account: pageAccount, settings: pageSettings" in a)
        assertTrue("function pageAccount(c)" in a && "function pageSettings(c)" in a)
        assertTrue("M.profileScreen(w)" in a && "M.settingsScreen(w)" in a)
        assertTrue("view.arg = {tab: 'account'}" in a && "view.arg = {tab: 'profile'}" in a)
        assertTrue("confirmDlg('خروج از حساب', 'از حساب خارج می‌شوید؟', 'خروج', true)" in a)
        val m = source("site/src/mobile.js")
        assertTrue("profileScreen: profileScreen" in m && "settingsScreen: settingsScreen" in m && "setProfileTab" in m)
        assertTrue("!MQ.matches && !document.getElementById('m-shell')" in m)
        assertTrue(".dk .dk-mwrap" in source("site/src/site.css"))
    }
}
