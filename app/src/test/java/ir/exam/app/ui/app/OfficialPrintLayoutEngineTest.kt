package ir.exam.app.ui.app

import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.printing.PrintFigureMetrics
import ir.exam.app.core.printing.PrintTextSpanSegments
import ir.exam.app.domain.model.PrintTextSpan
import java.io.File
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** پوشش مسیر PDF بومی که همچنان در چاپ رسمی و کارنامه استفاده می‌شود. */
class OfficialPrintLayoutEngineTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    @Test
    fun `pdf figure metrics preserve stored width and free position safely`() {
        assertEquals(95f, PrintFigureMetrics.figureWidthMm(FigureSpec.build("tri")), 0.001f)
        assertEquals(
            40f,
            PrintFigureMetrics.figureWidthMm(FigureSpec.build("tri", extra = mapOf("wmm" to JsonPrimitive("2")))),
            0.001f
        )
        assertEquals(
            180f,
            PrintFigureMetrics.figureWidthMm(FigureSpec.build("tri", extra = mapOf("wmm" to JsonPrimitive("999")))),
            0.001f
        )
        val positioned = FigureSpec.build(
            "tri",
            extra = mapOf("wmm" to JsonPrimitive("120"), "fx" to JsonPrimitive("12.5"), "fy" to JsonPrimitive("34"))
        )
        assertEquals(120f, PrintFigureMetrics.figureWidthMm(positioned), 0.001f)
        assertEquals(12.5f, PrintFigureMetrics.figurePosMm(positioned)!!.first, 0.001f)
        assertEquals(34f, PrintFigureMetrics.figurePosMm(positioned)!!.second, 0.001f)
        assertEquals(
            95f,
            PrintFigureMetrics.figureWidthMm(FigureSpec.build("tri", extra = mapOf("wmm" to JsonPrimitive("NaN")))),
            0.001f
        )
        assertNull(PrintFigureMetrics.figurePosMm(FigureSpec.build("tri", extra = mapOf("fx" to JsonPrimitive("12")))))
        assertNull(
            PrintFigureMetrics.figurePosMm(
                FigureSpec.build("tri", extra = mapOf("fx" to JsonPrimitive("NaN"), "fy" to JsonPrimitive("34")))
            )
        )
    }

    @Test
    fun `PDF rich text segmentation uses the domain print spans without UI editor helpers`() {
        assertEquals(
            listOf(
                Triple("a", false, false),
                Triple("bc", true, false),
                Triple("d", true, true),
                Triple("e", false, true),
                Triple("f", false, false)
            ),
            PrintTextSpanSegments.split(
                text = "abcdef",
                offsetInSource = 0,
                spans = listOf(
                    PrintTextSpan(start = 1, end = 4, bold = true),
                    PrintTextSpan(start = 3, end = 5, italic = true)
                )
            )
        )
        assertEquals(
            listOf(Triple("de", true, false), Triple("f", false, false)),
            PrintTextSpanSegments.split(
                text = "def",
                offsetInSource = 3,
                spans = listOf(PrintTextSpan(start = 1, end = 5, bold = true))
            )
        )
        assertEquals(
            listOf(Triple("همه", true, true)),
            PrintTextSpanSegments.split(
                text = "همه",
                offsetInSource = 0,
                spans = listOf(PrintTextSpan(start = 0, end = 3, bold = true, italic = true))
            )
        )
    }

    @Test
    fun `active PDF engine keeps native A4 formula and figure drawing without the retired editor API`() {
        val pdf = File(root(), "app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt").readText()
        listOf(
            "class OfficialPrintLayoutEngine",
            "fun layoutExam(printable: OfficialExamPrintable)",
            "fun drawFlowWindow(",
            "NativeMathCanvasRenderer",
            "FigureSvgRenderer",
            "AtlasBitmapRenderer",
            "computeSlices(total, boundaries.toList(), firstContentTop)",
            // شکل آزاد باید در صفحهٔ مقصد، نه فقط صفحهٔ slot اولیه، رسم شود.
            "freeImageIntersects",
            "total = maxOf(total, imageRect.bottom)"
        ).forEach { marker -> assertTrue("موتور PDF ناقص است: $marker", marker in pdf) }
        listOf(
            "WordPageLayout",
            "UnifiedDocumentEngine",
            "layoutExamForEditor",
            "drawEditorPage",
            "editorObjects",
            "ir.exam.app.ui.builder",
            "StyleSpanOps"
        )
            .forEach { retired -> assertTrue("API بازنشستهٔ سند باقی مانده: $retired", retired !in pdf) }
    }
}
