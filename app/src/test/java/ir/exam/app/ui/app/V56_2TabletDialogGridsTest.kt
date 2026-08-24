package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V56.2 — شبکه‌های انتخاب در تبلت (پچ ۳ از ۳):
 * ۱) پنجرهٔ انتخاب شکل/نمودار: هندسه Adaptive(140dp) و نمودار ۳ستونه در
 *    تبلت؛ در گوشی همان Adaptive(104dp) و GridCells.Fixed(2) قرارداد V55.12.
 * ۲) پنجرهٔ انتخاب نوع آناتومی/فیزیک/شیمی: در تبلت ۳ستونه.
 */
class V56_2TabletDialogGridsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val figurePicker by lazy { source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt") }
    private val atlasEditor by lazy { source("app/src/main/java/ir/exam/app/ui/figure/AtlasEditorDialog.kt") }

    @Test
    fun `figure picker grids widen on tablets and keep the v55_12 phone contract`() {
        assertTrue("val tabletPicker = LocalTabletLayout.current" in figurePicker)
        assertTrue(
            "if (tabletPicker) GridCells.Adaptive(140.dp) else GridCells.Adaptive(104.dp)" in figurePicker
        )
        // قرارداد V55.12 گوشی: نمودارها هر سطر ۲ تا — رشتهٔ GridCells.Fixed(2) حفظ شده
        assertTrue(
            "if (tabletPicker) GridCells.Fixed(3) else GridCells.Fixed(2)" in figurePicker
        )
    }

    @Test
    fun `atlas type picker grid widens on tablets`() {
        assertTrue("val tabletAtlas = LocalTabletLayout.current" in atlasEditor)
        assertTrue("if (tabletAtlas) GridCells.Fixed(3) else GridCells.Fixed(2)" in atlasEditor)
    }
}
