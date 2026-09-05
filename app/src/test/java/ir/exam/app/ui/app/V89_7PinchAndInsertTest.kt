package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V89.7 — درجِ شیء در محلِ مکان‌نما، تغییرِ اندازه با دو انگشت، حذفِ کادر و
 * دستگیره‌ها، و درگ‌پذیر شدنِ جدول در پیش‌نمایش.
 */
class V89_7PinchAndInsertTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val cards by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintQuestionCards.kt").readText()
    }

    @Test
    fun `a figure lands where the cursor is, not at the end`() {
        assertTrue("window.__qmfSetInsertPos = function" in asset)
        assertTrue("var want = window.__qmfInsertPos;" in asset)
        // یک‌بارمصرف: پس از درج پاک می‌شود تا درجِ بعدی اشتباه نیفتد
        assertTrue("window.__qmfInsertPos = null;" in asset)
        assertTrue("onOpenTool(tool, textField.selection.end)" in cards)
    }

    @Test
    fun `the host records the cursor before opening the tool`() {
        assertTrue(
            Regex("__qmfSetInsertPos[\\s\\S]{0,300}figureTool = FigureToolRequest")
                .containsMatchIn(dialog)
        )
    }

    @Test
    fun `two fingers resize and one finger moves`() {
        assertTrue("function twoFingerDist(t)" in asset)
        assertTrue("if (!e.touches || e.touches.length !== 2) return;" in asset)
        assertTrue("var ratio = d / pinch.d0;" in asset)
        // نباید هم‌زمان جابه‌جا شود
        assertTrue("if (pinch) return;" in asset)
    }

    @Test
    fun `the pinched size is stored like any other layout change`() {
        assertTrue(
            Regex("pinch[\\s\\S]{0,400}setFigLayout\\(pinch\\.qid").containsMatchIn(asset)
        )
    }

    @Test
    fun `no selection box or handles clutter the figure`() {
        assertTrue("#previewArea .interactive-figure.selected{outline:none !important}" in asset)
        assertTrue("#previewArea .fig-resize-handle{display:none !important}" in asset)
        assertTrue("#previewArea .fig-size-badge{display:none !important}" in asset)
    }

    @Test
    fun `but the handle code survives untouched`() {
        assertTrue("ensureProfessionalResizeHandles" in asset)
        assertTrue("fig-resize-handle" in asset)
    }

    @Test
    fun `a table can be dragged in the preview`() {
        // خانه‌های جدول pointer-events:auto دارند و لمس را می‌بلعیدند
        assertTrue("#previewArea .interactive-figure .tbx-t input[data-r][data-c]{" in asset)
        // ولی ویرایشگرِ جدول باید همچنان قابلِ تایپ بماند
        assertTrue(".tbx-t input[data-r][data-c]{display:block!important;pointer-events:auto!important" in asset)
    }
}
