package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V171 — صفحهٔ «منو»ی دسکتاپ برای مدیر و دانش‌آموز هم آینهٔ منوی همبرگری اپ است (ExamApp.kt:995-1060). */
class V171_SiteDesktopRoleMenusTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `manager and student menu cards mirror the app`() {
        val a = source("site/src/app.js")
        assertTrue("managerMenu: [['classes', '🏫', 'کلاس‌ها', 'فهرست و مدیریت', 'school'], ['students', '🎓', 'دانش‌آموزان', 'فهرست و مدیریت', 'school', {students: true}], ['account', '👤', 'حساب', 'مشخصات و امنیت حساب'], ['site', '🌐', 'سایت', 'onlineexam.ir'], ['settings', '⚙', 'تنظیمات', 'ظاهر، داده و درباره']]" in a)
        assertTrue("studentMenu: [['join', '🔑', 'آزمون', 'ورود با کد آزمون'], ['grades', '📊', 'نتایج من', 'پاسخ‌ها و کارنامه'], ['calendar', '📅', 'تقویم', 'رویدادها و پیام‌ها'], ['account', '👤', 'حساب', 'مشخصات و امنیت حساب'], ['settings', '⚙', 'تنظیمات', 'ظاهر، داده و درباره']]" in a)
        assertTrue("var menu = MENUS[user.role + 'Menu'] || MENUS.teacherMenu;" in a)
        assertTrue("else if (it[0] === 'site') { toast('شما هم‌اکنون در سایت هستید.', 'ok'); } else if (it[4]) { view.panel = it[4]; view.arg = it[5] || null; render(); }" in a)
        assertTrue("((MENUS[user.role + 'Menu'] || []).concat(items, MENUS.teacherMenu).filter(" in a)
    }
}
