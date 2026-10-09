package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V250 — بازبینی کد به کد سایت: رفع ReferenceError ورود اکسل، تازه‌سازی نشست بدون خروج ناخواسته، بدون prompt/confirm مرورگر، تور ایمنی خطا. */
class V250_SiteAuditTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, path).isFile) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `school js imports en used by excel import`() {
        val s = source("site/src/school.js")
        assertTrue("en = S.en" in s)
        assertTrue("var u = en(g(r, 'username'))" in s)
    }

    @Test
    fun `session refresh is shared and network errors keep the session`() {
        val a = source("site/src/app.js")
        assertTrue("var refreshing = null;" in a && "if (refreshing) return refreshing;" in a)
        assertTrue("else if (res.status === 400 || res.status === 401 || res.status === 403) saveSession(null);" in a)
        assertFalse("} catch (e) { saveSession(null); }" in a)
        assertTrue("window.addEventListener('unhandledrejection'" in a)
    }

    @Test
    fun `no native browser prompt or confirm dialogs in site`() {
        for (f in listOf("app", "admin", "builder", "extras", "mobile", "school", "student", "studio", "push")) {
            val s = source("site/src/$f.js")
            assertFalse(f, Regex("[^.a-zA-Z_]prompt\\(").containsMatchIn(s))
            assertFalse(f, Regex("[^.a-zA-Z_]confirm\\('").containsMatchIn(s))
            assertFalse(f, Regex("[^.a-zA-Z_]alert\\(").containsMatchIn(s))
        }
        assertTrue("window.removeEventListener('resize', paintStage)" in source("site/src/studio.js"))
    }
}
