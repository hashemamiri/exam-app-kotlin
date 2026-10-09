package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V236 — دکمهٔ گوگل بالای فرم‌های دسکتاپ + انتخاب حساب اجباری. */
class V236_GoogleTopTest {
    private fun src(rel: String): String = File(listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }, rel).readText()

    @Test
    fun googleTopAndSelectAccount() {
        val ex = src("site/src/extras.js")
        assertTrue("provider=google&prompt=select_account&redirect_to=" in ex)
        val app = src("site/src/app.js")
        val login = app.indexOf("var gbl = window.SiteExtras.googleButton('teacher')")
        assertTrue(login > 0 && login < app.indexOf("m.appendChild(id); m.appendChild(pw); m.appendChild(b);"))
        val reg = app.indexOf("gb.innerHTML.replace('ورود با گوگل', 'ثبت‌نام با گوگل')")
        assertTrue(reg > 0 && reg < app.indexOf("m.appendChild(name); m.appendChild(em); m.appendChild(b1);"))
    }
}
