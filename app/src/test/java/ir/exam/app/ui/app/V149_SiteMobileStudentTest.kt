package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V149 — دانش‌آموز در پوستهٔ موبایل سایت (آینهٔ StudentHomeScreen و منوی ۶کارتی) + رفع کادر خاکستری دسکتاپ. */
class V149_SiteMobileStudentTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `student shell mirrors app home and menu`() {
        val m = source("site/src/mobile.js")
        for (t in listOf("'آزمون', 'ورود با کد آزمون'", "'نتایج من', 'پاسخ‌ها و کارنامه'", "'تقویم', 'رویدادها و پیام‌ها'", "'حساب', 'مشخصات و امنیت حساب'", "'تنظیمات', 'ظاهر، داده و درباره'", "'خروج', 'خروج امن و تعویض حساب'")) assertTrue(t, t in m)
        assertTrue("'داشبورد دانش‌آموز'" in m && "'پیام جدید دارید'" in m && "S.rpcObj('cal_unseen_v59', {})" in m && "S.rpcObj('cal_mark_seen_v59', {p_note: n.id})" in m)
        assertTrue("u.role === 'teacher' || u.role === 'student'" in m && "function paintStudent(shell, page)" in m && "window.SiteStudent.inExam()" in m)
        assertTrue("if (window.SiteMobile && window.SiteMobile.active()) { window.SiteMobile.paint(); c = document.getElementById('content'); }" in source("site/src/student.js"))
    }

    @Test
    fun `desktop sidebar backdrop is hidden unless opened`() {
        val css = source("site/src/site.css")
        assertTrue(".sb-bg{position:fixed;inset:0;background:rgba(0,0,0,.3);z-index:39;display:none}" in css && ".sb-bg.on{display:block}" in css)
    }
}
