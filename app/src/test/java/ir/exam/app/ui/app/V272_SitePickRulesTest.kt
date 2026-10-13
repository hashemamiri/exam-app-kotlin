package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V272 — سایت: فیلدهای «چند تا از N» به تفکیک نوع در مشخصات آزمون (حالت POOL). */
class V272_SitePickRulesTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `site builder has pick rules box`() {
        val js = src("site/src/builder.js")
        assertTrue("function pickRulesBox(st) {" in js)
        assertTrue("text: 'انتخاب تصادفی بر اساس نوع سؤال (خالی = همهٔ آن نوع)'" in js)
        assertTrue("var prb = pickRulesBox(state); if (prb) m_.appendChild(prb); /* V272 */" in js)
    }
}
