package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V45.3:
 * ۱) متن عادی قبل و بعد از فرمول، جای تایپ مستقل دارد و فرمول در FlowRow می‌ماند.
 * ۲) آیکن شکل فقط مرحلهٔ انتخاب نوع شکل را باز می‌کند.
 * ۳) آیکن نمودار فقط مرحلهٔ انتخاب نوع نمودار را باز می‌کند.
 * ۴) پس از انتخاب نوع، ویرایشگر همان نوع باز می‌شود.
 */
class V45_3InlineFigureFlowTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val inlineEditor by lazy {
        source("app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt")
    }
    private val richText by lazy {
        source("app/src/main/java/ir/exam/app/core/text/RichText.kt")
    }
    private val builder by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
    }
    private val figurePicker by lazy {
        source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt")
    }

    @Test
    fun `inline editor exposes editable text slots before and after tokens`() {
        assertTrue("FlowRow(" in inlineEditor)
        assertTrue("RichTextSplitter.split(source)" in inlineEditor)
        assertTrue("if (showPlaceholder && part.text.isEmpty())" in inlineEditor)
        assertTrue("Box(contentAlignment = Alignment.CenterStart)" in inlineEditor)
        assertTrue("if (start >= cursor)" in richText)
        assertTrue("if (cursor <= source.length)" in richText)
    }

    @Test
    fun `figure and graph icons open separate type-first flows`() {
        assertTrue("chooseType: Boolean = false" in builder)
        assertTrue("FigureTarget(kind = FigureKind.GEOMETRY, chooseType = true)" in builder)
        assertTrue("FigureTarget(kind = FigureKind.GRAPH, chooseType = true)" in builder)
        assertTrue("if (target.chooseType)" in builder)
        assertTrue("FigureTypePickerDialog(" in builder)
        assertTrue("target.copy(initialSpec = spec, chooseType = false)" in builder)
        assertTrue("ابتدا نوع شکل هندسی را انتخاب کنید" in figurePicker)
        assertTrue("ابتدا نوع نمودار را انتخاب کنید" in figurePicker)
        assertTrue("GeometryEditorPane" in figurePicker)
        assertTrue("GraphEditorPane" in figurePicker)
        // انتخاب‌گر دیگر تب مشترک شکل/نمودار ندارد.
        assertFalse("FilterChip(" in figurePicker)
    }

    @Test
    fun `type picker selects a template before editor receives it`() {
        assertTrue("onTypeSelected: (FigureSpec) -> Unit" in figurePicker)
        assertTrue("onTypeSelected(template.toSpec())" in figurePicker)
        assertTrue("FigurePickerDialog(" in builder)
        assertTrue("initialSpec = target.initialSpec" in builder)
        assertTrue("onInsert = { spec ->" in builder)
    }
}
