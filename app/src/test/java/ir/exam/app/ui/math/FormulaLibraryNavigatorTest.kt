package ir.exam.app.ui.math

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaLibraryNavigatorTest {
    private fun data(): FormulaReferenceData {
        val file = listOf(
            File("src/main/assets/formula_library_v13.json"),
            File("app/src/main/assets/formula_library_v13.json")
        ).first(File::isFile)
        return FormulaReferenceLibrary.decode(file.readText())
    }

    @Test fun `every visible library route opens a concrete list`() {
        val data = data()
        assertEquals(24, FormulaLibraryNavigator.entries(data, "common").size)
        assertEquals(1200, FormulaLibraryNavigator.entries(data, "unicode").size)
        assertEquals(26, FormulaLibraryNavigator.entries(data, "letters").size)
        assertEquals(26, FormulaLibraryNavigator.entries(data, "letters", uppercase = true).size)
        assertTrue(FormulaLibraryNavigator.entries(data, "__all").isNotEmpty())
        data.groups.flatMap { it.categories }.forEach { link ->
            assertTrue("library route is empty: ${link.id}", FormulaLibraryNavigator.entries(data, link.id).isNotEmpty())
        }
    }

    @Test fun `favorites recent and search use the same deterministic navigator`() {
        val data = data()
        val favorite = FormulaReferenceEntry("آلفا", "\\alpha")
        val recent = FormulaReferenceEntry("بتا", "\\beta")
        assertEquals(listOf(favorite), FormulaLibraryNavigator.entries(data, "__favorites", favorites = listOf(favorite)))
        assertEquals(listOf(recent), FormulaLibraryNavigator.entries(data, "__recent_symbols", recent = listOf(recent)))
        val result = FormulaLibraryNavigator.search(data, "PLUS SIGN")
        assertFalse(result.isEmpty())
        assertTrue(result.any { it.tex == "+" })
    }

    @Test fun `full screen library dialog is reachable from all main buttons`() {
        val root = listOf(File("."), File("..")).first {
            File(it, "app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").isFile
        }
        val editor = File(root, "app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").readText()
        val dialog = File(root, "app/src/main/java/ir/exam/app/ui/math/FormulaLibraryDialog.kt").readText()
        listOf("openLibrary(\"common\")", "openLibrary(\"__all\")", "openLibrary(\"unicode\")", "openLibrary(\"__recent_symbols\")", "openLibrary(\"__favorites\")", "openLibrary(link.id").forEach {
            assertTrue("missing open route: $it", it in editor)
        }
        assertTrue("usePlatformDefaultWidth = false" in dialog)
        assertTrue("LazyVerticalGrid" in dialog)
        assertTrue("onClick = { onUse(entry) }" in dialog)
        assertTrue("Text(\"درج\")" in dialog)
    }
}
