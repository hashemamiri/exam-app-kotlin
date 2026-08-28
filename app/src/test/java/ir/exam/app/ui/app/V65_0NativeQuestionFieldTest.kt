package ir.exam.app.ui.app

import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V65_0NativeQuestionFieldTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val section by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val controller by lazy { source("app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt") }

    @Test
    fun `builder question text field is compose native without webview`() {
        assertTrue("QuestionTextWebSection(" in builder)
        assertFalse("QuestionTextFieldWebView(" in section)
        assertFalse("android.webkit" in section)
        assertTrue("BasicTextField(" in section)
        assertTrue("RichTextSplitter.split" in section)
        assertTrue("nativeInsert" in controller)
        assertTrue("nativeOpenFormula" in controller)
        assertTrue("controller.openTool(\"formula\")" in section)
    }

    @Test
    fun `eight native tool icons remain in reference order`() {
        val order = listOf(
            "درج فرمول", "درج شکل", "درج نمودار", "درج جدول",
            "درج آناتومی بدن", "درج جدول تناوبی", "درج فیزیک", "درج شیمی"
        )
        var cursor = -1
        order.forEach { label ->
            val at = section.indexOf(label)
            assertTrue("missing tool: $label", at >= 0)
            assertTrue("out of order: $label", at > cursor)
            cursor = at
        }
    }

    @Test
    fun `figure insert and reconstruct keep formula tokens in place`() {
        val spec = FigureSpec.build("tri")
        val withFig = FigureCodec.insert("مساحت $a^2$", spec)
        assertTrue(withFig.contains("%%FIG:"))
        val parts = RichTextSplitter.split(withFig)
        assertTrue(parts.any { it is RichSegment.Math })
        assertTrue(parts.any { it is RichSegment.Figure })
        val textIndex = parts.indexOfFirst { it is RichSegment.Text && (it as RichSegment.Text).text.contains("مساحت") }
        val rebuilt = RichTextSplitter.reconstruct(parts, textIndex, "مساحت ")
        assertTrue(rebuilt.contains("\$a^2\$") || rebuilt.contains("a^2"))
        assertTrue(rebuilt.contains("%%FIG:"))
    }
}
