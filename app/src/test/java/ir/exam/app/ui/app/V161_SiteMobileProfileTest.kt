package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V161 — صفحهٔ «حساب» سایت گوشی آینهٔ ProfileSettingsScreen اپ: چیپ‌ها، آکاردئون‌ها، عکس محلی، تأیید رمز فعلی. CI اپ برای نسخهٔ فقط-سایت بسته است. */
class V161_SiteMobileProfileTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `mobile profile screen mirrors app sections`() {
        val m = source("site/src/mobile.js")
        assertTrue("async function profileScreen(" in m && "class: 'm-chip" in m && "function acc(" in m)
        for (t in listOf("مشخصات حساب", "پیوستن به مدرسه", "تغییر نام کاربری", "تغییر ایمیل", "تغییر رمز عبور", "حذف حساب")) assertTrue(t, t in m)
        assertTrue("localStorage" in m && "examsite.avatar." in m && "localAvatar(u.id)" in m)
        assertTrue("page === 'profile' || page === 'account') profileScreen(content)" in m)
        val a = source("site/src/app.js")
        assertTrue("verifyCurrentPassword:" in a && "grant_type=password" in a && "updateEmail:" in a)
        val css = source("site/src/site.css")
        assertTrue(".m-acc-h{" in css && ".m-chip.on{" in css && ".m-avatar.xl{" in css)
    }

    @Test
    fun `site-only version keeps android ci closed`() {
        val y = source(".github/workflows/android.yml")
        // V164 — نسخهٔ فقط-سایت: CI بسته (قاعدهٔ V160.5)
        assertTrue("workflow_dispatch:" in y && "\n  push:" in y) // V179: assets/print تغییر کرد → CI اپ باز
    }
}
