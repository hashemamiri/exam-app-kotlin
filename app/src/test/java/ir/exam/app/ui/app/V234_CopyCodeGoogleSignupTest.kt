package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V234 — کلیک/ضربه روی کد آزمون = کپی (سایت و اپ)؛ دکمهٔ گوگل در ثبت‌نام دسکتاپ. */
class V234_CopyCodeGoogleSignupTest {
    private fun src(rel: String): String = File(listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }, rel).readText()

    @Test
    fun siteCopyAndGoogleSignup() {
        val app = src("site/src/app.js")
        assertTrue("e.target.closest('.code')" in app && "navigator.clipboard.writeText(code)" in app && "کپی شد.', 'ok')" in app)
        assertTrue("gb.innerHTML = gb.innerHTML.replace('ورود با گوگل', 'ثبت‌نام با گوگل')" in app)
        assertTrue(".code{cursor:pointer}" in src("site/src/site.css"))
    }

    @Test
    fun appCopyableCode() {
        val helper = src("app/src/main/java/ir/exam/app/core/ui/ExamCodeCopy.kt")
        assertTrue("fun copyExamCode(context: Context, code: String)" in helper && "ClipData.newPlainText(\"کد آزمون\", code)" in helper && "fun CopyableExamCode(" in helper)
        assertTrue("ir.exam.app.core.ui.CopyableExamCode(exam.code)" in src("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt"))
        assertTrue("CopyableExamCode(code, prefix = \"کد آزمون: \"" in src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt"))
    }
}
