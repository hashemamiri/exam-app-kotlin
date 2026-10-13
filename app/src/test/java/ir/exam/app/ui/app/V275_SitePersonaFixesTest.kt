package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V275 — یافته‌های تست پرسونا (معلم تازه‌کار/دانش‌آموز/مدیر) روی سایت دسکتاپ. */
class V275_SitePersonaFixesTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `pick rules refresh, closed-exam hint and unlimited duration`() {
        val js = src("site/src/builder.js")
        assertTrue("function refreshPickRules() {" in js)
        assertTrue("if (typeof refreshPickRules === 'function') refreshPickRules(); /* V275 */" in js)
        assertTrue("var prw = el('div', {class: 'b-pr-wrap'});" in js)
        assertTrue("var wasNew = !state.examId; /* V275 */" in js)
        assertTrue("قفل آن را باز کنید تا دانش‌آموزان بتوانند با این کد وارد شوند" in js)
        val sql = src("supabase/migrations/20261013_native_pick_rules_v272.sql")
        assertTrue("v_exp := case when coalesce(v_exam.duration, 0) > 0" in sql)
    }
}
