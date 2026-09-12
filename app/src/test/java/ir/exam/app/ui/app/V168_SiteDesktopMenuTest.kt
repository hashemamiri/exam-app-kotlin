package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V168 — ریل دسکتاپ معلم کوتاه شد (داشبورد، آزمون‌ها، آزمون جدید، کیف پول، کارت‌ها، پروفایل)؛ صفحهٔ «منو» = منوی همبرگری اپ (تقویم، چاپ آزمون، دانش‌آموزان، کلاس‌ها، حساب، تنظیمات، خروج). */
class V168_SiteDesktopMenuTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `teacher rail and hamburger menu mirror the app`() {
        val a = source("site/src/app.js")
        assertTrue("['dashboard', '🏠', 'داشبورد'], ['exams', '📝', 'آزمون‌ها'], ['builder', '➕', 'آزمون جدید'], ['wallet', '👛', 'کیف پول'], ['cards', '🃏', 'کارت‌ها']\n    ]," in a)
        assertTrue("teacherMenu: [['calendar', '📅', 'تقویم', 'رویدادها و پیام‌ها'], ['print', '🖨', 'چاپ آزمون', 'آزمون‌های چاپی و برگه'], ['students', '🎓', 'دانش‌آموزان', 'فهرست و وضعیت'], ['classes', '🏫', 'کلاس‌ها', 'فهرست و مدیریت'], ['profile', '👤', 'حساب', 'مشخصات و امنیت حساب'], ['tools', '⚙', 'تنظیمات', 'ظاهر، داده و درباره']]" in a)
        assertTrue("var menu = user.role === 'teacher' ? MENUS.teacherMenu : (MENUS[user.role] || MENUS.student)" in a)
        assertTrue("items.concat(MENUS.teacherMenu).filter(" in a)
        assertTrue("function pagePrint(c)" in a && "print: pagePrint" in a && "window.SiteBuilder.printExamsSection(function () { pagePrint(c); })" in a && "view.arg = {mode: 'print', fresh: true}; render();" in a)
    }
}
