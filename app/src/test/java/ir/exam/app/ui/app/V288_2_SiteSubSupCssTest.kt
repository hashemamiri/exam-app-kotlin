package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V288.2 — سایت: قواعد CSS ظرف توان+اندیس هم‌زمان (.msubsup/.ss-base) از موتور چاپ به کادر سؤال/پیش‌نمایش منتقل می‌شود. */
class V288_2_SiteSubSupCssTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun keepRegexCarriesSubSupContainer() {
        for (f in listOf("site/src/student.js", "site/src/builder.js")) {
            val keep = src(f).substringAfter("var keep = /").substringBefore("/;")
            assertTrue(f, "|msubsup|msubsup-[a-z]+|ss-base)" in keep)
            val re = Regex(keep)
            for (sel in listOf(".msubsup", ".msubsup > .ss-base", ".msubsup-tall", ".msubsup > sup.msup", ".mfrac")) assertTrue("$f $sel", re.containsMatchIn(sel))
        }
    }
}
