package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V156 — کیف پول سایت فارسی + شارژ؛ سازندهٔ گوشی مثل ExamBuilderScreen: FAB چشم/چاپ فقط در حالت چاپ، بدون داک. */
class V156_SiteWalletBuilderFabsTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `wallet page is persian and has top up`() {
        val a = source("site/src/app.js")
        assertTrue("function faReason(r)" in a && "'👛 موجودی کیف پول'" in a && "'🧾 گردش‌های اخیر'" in a && "'هنوز تراکنشی ثبت نشده است.'" in a)
        assertTrue("opts.printMode === 'teacher'" in a && "w.printStudent()" in a)
        val ad = source("site/src/admin.js")
        assertTrue("'💳 شارژ امن کیف پول'" in ad && "'رفتن به درگاه امن'" in ad)
    }

    @Test
    fun `mobile builder fabs and dock mirror app`() {
        val m = source("site/src/mobile.js")
        assertTrue("if (page !== 'builder') shell.appendChild(dock());" in m)
        assertTrue("class: 'm-fab prev'" in m && "[ic('eye')]" in m)
        assertTrue("class: 'm-fab print'" in m && "'چاپ آزمون (دانش‌آموز)'" in m && "'چاپ با کلید (پاسخ‌نامه)'" in m)
        assertFalse("'پیش‌نمایش آزمون', onclick: function () { prevBtn.click(); }}, [ic('print')]" in m)
        val b = source("site/src/builder.js")
        assertTrue("function preview(printMode)" in b && "window.__builderPreview = preview;" in b)
    }
}
