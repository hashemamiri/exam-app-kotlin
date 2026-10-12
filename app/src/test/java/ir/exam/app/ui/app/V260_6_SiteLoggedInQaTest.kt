package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V260.6 — سایت: رفع خطای «از آزمون آنلاین» در منوی دسکتاپ، کش نام کاربری→ایمیل، متن چیدمان فعلی، مهاجرت سقف ورود. */
class V260_6_SiteLoggedInQaTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `logged-in qa fixes are present`() {
        val m = source("site/src/mobile.js")
        assertTrue("if (!Array.isArray(list)) { try { list = await window.SiteBuilder.printExamsList(); } catch (e) { list = []; } }" in m)
        assertTrue("text: 'چیدمان فعلی: ' + (MQ.matches ? 'گوشی' : 'دسکتاپ')" in m)
        val a = source("site/src/app.js")
        assertTrue("var LS_UL = 'examsite.ulogin.v1', ul = {};" in a)
        assertTrue("if (ul[clean]) { email = ul[clean]; fromCache = true; }" in a)
        assertTrue("if (!fromCache) throw e;" in a)
        val sql = source("supabase/migrations/20261012_native_staff_login_throttle_v260_6.sql")
        assertTrue("if v_recent >= 12 or v_global >= 300 then" in sql)
    }
}
