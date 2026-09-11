package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V148 — پوستهٔ موبایل پنل معلم در سایت، آینهٔ Design69 اپ (داک، منوی ۸کارتی، کارت‌های مدیریتی). */
class V148_SiteMobileShellTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `mobile shell mirrors app menu cards dock and management cards`() {
        val m = source("site/src/mobile.js")
        for (t in listOf("'تقویم', 'رویدادها و پیام‌ها'", "'چاپ آزمون', 'اطلاعات رسمی چاپ آزمون'", "'دانش‌آموزان', 'فهرست و وضعیت'", "'کلاس‌ها', 'فهرست و مدیریت'", "'حساب', 'مشخصات و امنیت حساب'", "'سایت', 'onlineexam.ir'", "'تنظیمات', 'ظاهر، داده و درباره'", "'خروج', 'خروج امن و تعویض حساب'")) assertTrue(t, t in m)
        for (t in listOf("item('منو'", "item('کیف پول'", "item('آزمون‌ها'", "item('کارت‌ها'", "'افزودن سریع'")) assertTrue(t, t in m)
        for (t in listOf("['آمار',", "['کارنامه',", "['بانک سؤال',", "['تصحیح',", "['مانده',", "['پاسخ',", "['درخواست‌ها',")) assertTrue(t, t in m)
        assertTrue("'آزمون‌های چاپی'" in m && "'واردکردن'" in m && "'ساخت آزمون جدید'" in m)
        assertTrue("window.matchMedia('(max-width: 860px)')" in m && "role === 'teacher'" in m)
        val app = source("site/src/app.js")
        assertTrue("window.SiteMobile.active()) { document.body.classList.add('m-mode'); window.SiteMobile.paint(); return; }" in app)
        assertTrue("function renderPage(c)" in app && "render: render, renderPage: renderPage, printExam: printExam, logout: doLogout" in app)
        assertTrue("read(os.path.join(SITE, \"src\", \"mobile.js\"))" in source("site/build_site.py"))
        assertTrue("if (filt === 'pending') exams = exams.filter" in source("site/src/admin.js"))
        val css = source("site/src/site.css")
        assertTrue("--m-bg:#E9EEF5" in css && "--m-acc:#6C63F5" in css && ".m-dock-panel{" in css && ".m-tile{height:116px" in css && ".m-profile{width:100%;height:148px" in css)
    }
}
