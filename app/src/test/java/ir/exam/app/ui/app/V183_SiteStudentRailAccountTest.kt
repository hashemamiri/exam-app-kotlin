package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V183 — دسکتاپ دانش‌آموز: ریل بدون «ابزارها/پروفایل»؛ حساب بدون تغییر نام کاربری/ایمیل/رمز. */
class V183_SiteStudentRailAccountTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `student rail has no tools or profile`() {
        val a = source("site/src/app.js")
        assertTrue("student: [['dashboard', '🏠', 'داشبورد'], ['join', '🔑', 'شرکت در آزمون'], ['grades', '📊', 'کارنامه'], ['calendar', '📅', 'تقویم و پیام‌ها']]," in a)
        assertFalse("['tools', '🧮', 'ابزارها'], '-', ['profile', '👤', 'پروفایل']" in a)
    }

    @Test
    fun `student account section hides username email password accordions`() {
        val m = source("site/src/mobile.js")
        val i = m.indexOf("if (p.role !== 'student') {\n    c.appendChild(acc('username', 'تغییر نام کاربری'")
        assertTrue(i > 0)
        val j = m.indexOf("c.appendChild(acc('password', 'تغییر رمز عبور'", i)
        assertTrue(j > i)
    }
}
