package ir.exam.app.ui.app

import ir.exam.app.core.ui.ExamListFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V237 — گوگل بالای فرم‌های اپ؛ پنجرهٔ «ورود به‌عنوان …/انصراف»؛ فیلتر چند درس؛ سایت: همان پرسش + سرعت بار اول. */
class V237_GoogleTopRoleChoiceTest {
    private fun src(rel: String): String = File(listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }, rel).readText()

    @Test
    fun subjectsFilter() {
        val f = ExamListFilter(subjects = setOf("ریاضی", "فیزیک"))
        assertTrue(f.isActive && f.matchesSubject("ریاضی") && !f.matchesSubject("شیمی") && !f.matchesSubject(null))
        assertTrue(ExamListFilter().matchesSubject(null))
    }

    @Test
    fun appGoogleTopAndRoleChoice() {
        val s = src("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt")
        // ورود: گوگل (StaggeredItem 3) قبل از فیلد ایمیل (4)
        assertTrue(s.indexOf("StaggeredItem(3) {\n        GoogleAuthButton(") < s.indexOf("hint = if (managerRole) \"ایمیل یا نام کاربری مدیر/معاون\""))
        assertTrue(s.indexOf("GoogleRegisterButton(state = state, viewModel = viewModel, role = \"teacher\")") < s.indexOf("hint = \"ایمیل معلم\""))
        assertTrue(s.indexOf("GoogleRegisterButton(state = state, viewModel = viewModel, role = \"manager\")") < s.indexOf("hint = \"ایمیل مدیر/معاون\""))
        assertTrue("state.roleChoice?.let" in s && "Text(\"انصراف\")" in s && "registering = true" in s)
        val vm = src("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt")
        assertTrue("fun confirmRoleChoice()" in vm && "fun cancelRoleChoice()" in vm && "ورود به‌عنوان مدیر" in vm && "ورود به‌عنوان معلم" in vm)
        assertTrue("roleChoiceFor(user, role, registering)" in vm)
    }

    @Test
    fun siteRoleChoiceAndPerf() {
        val app = src("site/src/app.js")
        assertTrue("window.__googleReturn" in app && "ورود به‌عنوان مدیر" in app && "ورود به‌عنوان معلم" in app)
        val ex = src("site/src/extras.js")
        assertTrue("examsite.google.mode" in ex && "function googleButton(role, registering)" in ex)
        val css = src("site/src/site.css")
        assertFalse("filter:blur(26px)" in css)
        assertTrue("rel=\"preload\" href=\"/fonts/Vazirmatn-400.woff2\"" in src("site/src/template.html"))
    }
}
