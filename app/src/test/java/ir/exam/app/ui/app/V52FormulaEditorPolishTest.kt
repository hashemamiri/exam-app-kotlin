package ir.exam.app.ui.app

import ir.exam.app.core.math.FormulaBoxEditor
import ir.exam.app.core.math.NativeMathParser
import ir.exam.app.core.math.NativeMathSvgRenderer
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V52FormulaEditorPolishTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").isFile
    }

    @Test
    fun `soft keyboard button never touches a detached webview`() {
        val editor = File(root(), "app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").readText()
        assertFalse(editor.contains("android.webkit.WebView"))
        assertFalse(editor.contains("evaluateJavascript"))
        assertTrue(editor.contains("runCatching { focusRequester.requestFocus() }"))
        assertTrue(editor.contains("keyboard?.show()"))
    }

    @Test
    fun `library dialog is icon-only with two-second favorite press`() {
        val dialog = File(root(), "app/src/main/java/ir/exam/app/ui/math/FormulaLibraryDialog.kt").readText()
        assertFalse(dialog.contains("جست‌وجو"))
        assertFalse(dialog.contains("OutlinedTextField"))
        assertFalse(dialog.contains("Text(\"درج\")"))
        assertFalse(dialog.contains("title, style"))
        assertTrue(dialog.contains("withTimeoutOrNull(2000L)"))
        assertTrue(dialog.contains("onToggleFavorite(entry)"))
    }

    @Test
    fun `parenthesis keypad opens left right or pair and has space`() {
        val editor = File(root(), "app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").readText()
        assertTrue(editor.contains("listOf(\"( )\", \"7\""))
        assertTrue(editor.contains("\"␠\" -> onInsert(\" \")"))
        assertTrue(editor.contains("parenPickerOpen"))
        assertTrue(editor.contains("چپ ("))
        assertTrue(editor.contains("راست )"))
        assertTrue(editor.contains("جفت ( )"))
        assertFalse(editor.contains("خانهٔ خالی را لمس کنید"))
        assertFalse(editor.contains("فرمول درج شد"))
    }

    @Test
    fun `limit sits under lim and integral takes over under limits`() {
        val lim = NativeMathSvgRenderer.render("\\lim_{x \\to 0} f(x)", 32f)
        val limBoxes = lim.editBoxes
        val xBox = limBoxes.first { it.sourceStart >= 0 && "x" in "\\lim_{x \\to 0} f(x)".substring(it.sourceStart, it.sourceEnd.coerceAtMost("\\lim_{x \\to 0} f(x)".length)) }
        val limY = limBoxes.minOf { it.yPx }
        assertTrue("limit subscript should sit below the operator", xBox.yPx > limY)

        val integral = NativeMathSvgRenderer.render("\\int_{a}^{b} x dx", 32f)
        val aBox = integral.editBoxes.first { "\\int_{a}^{b} x dx".substring(it.sourceStart, it.sourceEnd) == "a" }
        val bBox = integral.editBoxes.first { "\\int_{a}^{b} x dx".substring(it.sourceStart, it.sourceEnd) == "b" }
        assertTrue("integral lower limit is below upper limit", aBox.yPx > bBox.yPx)
        assertTrue(NativeMathParser.unsupportedCommands("\\lim_{x \\to 0}").isEmpty())
    }

    @Test
    fun `insert leaves caret after formula so equals can follow`() {
        val inserted = FormulaBoxEditor.insert(
            current = "",
            selectionStart = 0,
            selectionEnd = 0,
            insertion = "\\alpha",
            activateFirstInsertedBox = false,
            replaceActiveBoxWhenCollapsed = true
        )
        assertEquals("\\alpha", inserted.text)
        assertEquals(inserted.text.length, inserted.selectionStart)
        val afterArrow = FormulaBoxEditor.moveActiveBox(inserted.text, inserted.selectionStart, inserted.selectionEnd, 1)
        assertEquals(inserted.text.length, afterArrow.selectionStart)
        val typed = FormulaBoxEditor.insert(inserted.text, inserted.selectionStart, inserted.selectionEnd, "=")
        assertEquals("\\alpha=", typed.text)
    }

    @Test
    fun `repaired library formulas no longer end with a dangling backslash`() {
        val file = File(root(), "app/src/main/assets/formula_library_v13.json").readText()
        assertFalse(file.contains("هوپیتال") && file.contains("\\\\lim\\\\frac{f}{g}=\\\\lim\\\\frac{f\\\\"))
        assertTrue(file.contains("f'(x)=\\\\lim_{h \\\\to 0}"))
    }
}
