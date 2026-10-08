package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V217 — بازگشت نشانگرهای عادی موس در سایت دسکتاپ + ویرایشگر فرمول تیره در حالت تیرهٔ اپ. */
class V217_CursorsAndDarkFormulaTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun siteUsesDefaultCursors() {
        val css = src("site/src/site.css")
        assertFalse("--cur-arrow" in css)
        assertFalse("--cur-hand" in css)
        assertFalse("--cur-text" in css)
        assertFalse("cursor:url(" in css)
        File(root(), "site/src").listFiles { f -> f.extension == "js" }!!.forEach { f ->
            assertFalse("custom cursor var left in ${f.name}", "--cur-" in f.readText())
        }
    }

    @Test
    fun formulaEditorFollowsDarkTheme() {
        val host = src("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt")
        assertTrue("MaterialTheme.colorScheme.background.luminance() < 0.5f" in host)
        assertTrue("ComposeColor(0xFF0F0C29)" in host)
        assertTrue("view.setBackgroundColor(shellColor.toArgb())" in host)
        assertTrue("if(window.__mathHostTheme){__mathHostTheme.off();}" in host)
        assertTrue("window.__mathHostTheme = {" in src("app/src/main/assets/formula_editor/formula.html"))
    }
}
