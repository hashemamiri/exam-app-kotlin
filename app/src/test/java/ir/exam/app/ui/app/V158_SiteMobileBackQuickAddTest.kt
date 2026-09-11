package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V158 — دکمهٔ برگشت گوشی مثل BackHandler اپ + پنجره‌های «افزودن سریع» (دانش‌آموز/آزمون/مدرسه) مثل اپ. */
class V158_SiteMobileBackQuickAddTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `hardware back closes overlays then goes home then asks to exit`() {
        val m = source("site/src/mobile.js")
        assertTrue("window.addEventListener('popstate', function () { onBack(); });" in m)
        assertTrue("function closeTopOverlay()" in m && "if (closeTopOverlay()) { pushHist(); return; }" in m)
        assertTrue("if (S.user() && view.panel !== home) { pushHist(); go(home); return; }" in m)
        assertTrue("'از سایت خارج می‌شوید؟', 'خروج', true)" in m && "S.logout().then(leave, leave)" in m)
    }

    @Test
    fun `quick add opens app-like dialogs`() {
        val m = source("site/src/mobile.js")
        assertTrue("['آزمون جدید', 'ساخت آزمون آنلاین', 'exams', function () { go('builder', null); }]" in m)
        assertTrue("if (mgr) return managerStudentPicker(); var classes = await S.rpc('native_my_classes_v28', {})" in m && "bulkDialog(classes || []" in m)
        assertTrue("mgr ? createSchoolDialog() : joinSchoolDialog()" in m)
        assertTrue("text: 'عضویت در مدرسه جدید'" in m && "'کد دعوت ۶ حرفی مدیر مدرسه را وارد کنید.'" in m && "'native_join_school_v39'" in m)
        assertTrue("text: 'ساخت مدرسه جدید'" in m && "'native_manager_create_school_v61'" in m)
        assertTrue("text: 'انتخاب معلم و کلاس'" in m && "'دانش‌آموز جدید به کلاس کدام معلم اضافه شود؟'" in m && "'ادامه و ساخت دانش‌آموز' : 'ساخت بدون کلاس'" in m && "'native_manager_set_class_student_v40c'" in m)
    }
}
