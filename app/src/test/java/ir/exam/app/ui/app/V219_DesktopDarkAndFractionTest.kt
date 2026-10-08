package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V219 — خط کسر به عرض کامل در جعبهٔ سؤال سایت، پالت تیرهٔ پوستهٔ دسکتاپ (body.dk) و متن سؤال اپ با رنگ تم. */
class V219_DesktopDarkAndFractionTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun siteCopiesQmfAtomFractionRules() {
        val b = src("site/src/builder.js")
        val s = src("site/src/student.js")
        assertTrue(".b-rich .mathx .mfrac{align-items:stretch !important}" in b)
        assertTrue(".b-rich .mathx .mfrac>.mnum,.b-rich .mathx .mfrac>.mden{width:100%;box-sizing:border-box;text-align:center}" in b)
        assertTrue(".st-exam .mathx .mfrac{align-items:stretch !important}" in s)
    }

    @Test
    fun desktopShellHasDarkPalette() {
        val css = src("site/src/site.css")
        assertTrue(".m-dark body.dk{--bg:#1B222C;--card:#262F3B;--ink:#E6EAF2" in css)
        assertTrue("/* V218.2-AUTO" in css)
        assertTrue(".wb-canvas{background:#FFFFFF;" in css)
    }

    @Test
    fun builderQuestionFieldUsesThemeColor() {
        val q = src("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt")
        assertTrue("textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)," in q)
    }
}
