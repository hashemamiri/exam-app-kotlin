package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V150 — مدیر/معاون در پوستهٔ موبایل سایت (منوی ۶کارتی + کارت ویژهٔ داشبورد، داک با «معلم‌ها»، کارت‌های مدیریتی مدیر). */
class V150_SiteMobileManagerTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `manager shell mirrors app`() {
        val m = source("site/src/mobile.js")
        assertTrue("u.role === 'teacher' || u.role === 'student' || u.role === 'manager'" in m)
        assertTrue("item('معلم‌ها', 'students'" in m && "'دعوت معلم', 'ساخت کد دعوت برای معلم'" in m && "['مدرسه جدید'," in m)
        for (t in listOf("['کلاس‌ها', 'فهرست و مدیریت'", "['دانش‌آموزان', 'فهرست و مدیریت'", "text: 'داشبورد'}), el('span', {text: 'اطلاعات مدرسه و آمار'}", "'پروفایل مدیر/معاون'")) assertTrue(t, t in m)
        for (t in listOf("['مدارس',", "['کارنامه', 'آمار پاسخ‌ها", "['وضعیت',")) assertTrue(t, t in m)
        assertTrue(".m-featured .m-tile{width:52%}" in source("site/src/site.css"))
        assertTrue("if (arg.students) setTimeout" in source("site/src/admin.js"))
    }
}
