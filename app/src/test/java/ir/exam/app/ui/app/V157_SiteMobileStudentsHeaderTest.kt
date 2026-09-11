package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V157 — «تنظیمات سربرگ» در سازندهٔ چاپی سایت (HeaderSettingsDialog)؛ «دانش‌آموزان» گوشی مثل StudentsContent (Excel/+/🔍/فیلتر). */
class V157_SiteMobileStudentsHeaderTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `print builder has header settings and online has exam settings only`() {
        val b = source("site/src/builder.js")
        assertTrue("state.mode === 'print' && !state.bankEdit ? el('button', {class: 'btn light sm', text: '🏷 تنظیمات سربرگ'" in b)
        assertTrue("state.mode === 'online' && !state.bankEdit ? el('button', {class: 'btn light sm', text: '⚙ مشخصات آزمون'" in b)
        val a = source("site/src/app.js")
        assertTrue("function openHeaderSettings(onApply)" in a && "'اطلاعات سربرگ آزمون'" in a && "'انتخاب نوع سربرگ'" in a && "var savedHeader = readPrintHeader();" in a)
        assertTrue("window.__HEADER_SCHEMA" in source("site/build_site.py"))
        val m = source("site/src/mobile.js")
        assertTrue("text: 'تنظیمات سربرگ', onclick: function () { hdrBtn.click(); }" in m)
        assertTrue("var prev = isPrint && prevBtn ?" in m && "var printFab = isPrint && prevBtn ?" in m)
    }

    @Test
    fun `mobile students screen mirrors StudentsContent`() {
        val m = source("site/src/mobile.js")
        assertTrue("async function studentsScreen(c)" in m && "else if (page === 'students' && !mgr) studentsScreen(content);" in m)
        assertTrue("text: 'Excel'" in m && "'افزودن گروهی دانش‌آموز'" !in m /* V163: حذف گروهی از گوشی */ && "'جست‌وجوی دانش‌آموز'" in m && "'فیلتر دانش‌آموزان'" in m)
        assertTrue("'جست‌وجوی نام، نام کاربری، پایه یا پدر'" in m && "'دانش‌آموزی یافت نشد.'" in m)
        assertTrue("text: 'حذف فیلترها'" in m && "text: 'اعمال فیلتر'" in m && "'فقط دانش‌آموزانی که عضو هیچ کلاسی نیستند'" in m)
        assertTrue("'اطلاعات ورودی اکسل'" in m && "'ذخیره Excel'" in m && "function suggestUsername(first, last, suffix)" in m)
        assertTrue("'نام همه ردیف‌ها لازم است.'" in m && "'نام کاربری تکراری در ردیف‌ها وجود دارد.'" in m)
        for (name in listOf("نام", "نام کاربری", "جنسیت", "پایه", "رشته", "نام پدر", "کلاس", "وضعیت")) assertTrue("['$name', function" in m)
        assertTrue("classPickDlg: classPickDlg, credentialDlg: credentialDlg" in source("site/src/school.js"))
    }
}
