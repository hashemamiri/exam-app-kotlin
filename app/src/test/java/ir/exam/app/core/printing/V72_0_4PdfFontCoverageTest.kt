package ir.exam.app.core.printing

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V72.0.4 — رگرسیون glyphهای فارسی و نمادهای ریاضی در iText 7. */
class V72_0_4PdfFontCoverageTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `direct exporter embeds a dedicated math font instead of relying on fallback`() {
        val exporter = source("app/src/main/java/ir/exam/app/core/printing/DirectPdfExporter.kt")
        val regular = File(root(), "app/src/main/assets/fonts/dejavu_sans.ttf")
        val bold = File(root(), "app/src/main/assets/fonts/dejavu_sans_bold.ttf")
        assertTrue("fonts/dejavu_sans.ttf" in exporter)
        assertTrue("fonts/dejavu_sans_bold.ttf" in exporter)
        assertTrue("val mathBase" in exporter)
        assertTrue("val mathBold" in exporter)
        assertTrue("PERSIAN_SHAPED_GLYPHS" in exporter)
        assertTrue("MATH_GLYPHS" in exporter)
        assertTrue("requiredGlyphs: String" in exporter)
        assertTrue("candidate.containsGlyph(it.code)" in exporter)
        assertTrue("mathBase," in exporter)
        assertTrue("mathBold," in exporter)
        assertTrue(regular.isFile && regular.length() > 500_000L)
        assertTrue(bold.isFile && bold.length() > 500_000L)
    }

    @Test
    fun `formula segments use the math font and text segments keep Persian font`() {
        val exporter = source("app/src/main/java/ir/exam/app/core/printing/DirectPdfExporter.kt")
        assertTrue("NativeMathFormatter.renderTex(segment.tex),\n                        mathBase,\n                        mathBold" in exporter)
        assertTrue("NativeMathFormatter.renderTex(segment.text),\n                            mathBase,\n                            mathBold" in exporter)
        assertTrue("NativeMathFormatter.renderText(right),\n                    mathBase,\n                    mathBold" in exporter)
        assertTrue("NativeMathFormatter.renderText(left),\n                    mathBase,\n                    mathBold" in exporter)
    }
}
