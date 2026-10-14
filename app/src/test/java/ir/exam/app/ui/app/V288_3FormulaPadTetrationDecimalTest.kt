package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V288.3 — تتراسیون در منوی دکمهٔ «توان» (گوشی/اپ) و «ممیز فارسی ٫» در منوی «٫ ٪» (همه‌جا). */
class V288_3FormulaPadTetrationDecimalTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private val html by lazy { File(root(), "app/src/main/assets/formula_editor/formula.html").readText() }

    @Test
    fun powerMenuHasTetration() {
        val menu = html.substringAfter("} else if (kind === 'sup') {").substringBefore("} else if (kind === 'sqrt') {")
        assertTrue("['تتراسیون ⁿa', '{}^{n}a', ['tpl', '{}^{n}a']]" in menu)
        assertTrue("['زیرنویس', 'x_{1}', ['sub']]" in menu)
    }

    @Test
    fun percentMenuHasPersianDecimalInBothCopies() {
        assertTrue(Regex("\\['ممیز فارسی', '0٫5', \\['let', '٫'\\]\\]").findAll(html).count() == 2)
    }
}
