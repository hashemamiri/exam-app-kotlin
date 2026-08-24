package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.12 — پنج درخواست کاربر:
 * ۱) نمودار/آناتومی/فیزیک/شیمی مثل «درج شکل» دومرحله‌ای شوند: اول پنجرهٔ
 *    انتخاب نوع (AtlasTypePickerDialog با دسته‌ها؛ نمودار همان
 *    FigureTypePickerDialog موجود)، بعد پنجرهٔ ویرایش.
 * ۲) در پنجرهٔ ویرایش، انتخاب نوع نمایش داده نشود (LazyRow انواع و چیپ
 *    دسته‌ها از AtlasEditorDialog حذف شد؛ typeId از پنجرهٔ اول می‌آید).
 * ۳) شمارهٔ نشانه‌گذاری در «انتهای» پیکان باشد نه ابتدا — در هر سه رندرکننده:
 *    بوم ویرایشگر، نمای دانش‌آموز (AtlasFigureView)، چاپ/PDF (AtlasBitmapRenderer).
 * ۴) کادرهای برچسب تصاویر در پنجرهٔ ویرایش نمایش داده نشود (OutlinedTextField
 *    برچسب نشانه‌ها حذف؛ حذف نشانه با ردیف فشردهٔ شماره+✕ ممکن ماند).
 * ۵) نمودارها در پنجرهٔ انتخاب هر سطر ۲ تا (LazyVerticalGrid Fixed(2)) و
 *    جملهٔ «برای ویرایش انتخاب کنید» حذف شد.
 */
class V55_12AtlasFlowArrowTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val atlasEditor by lazy { source("app/src/main/java/ir/exam/app/ui/figure/AtlasEditorDialog.kt") }
    private val figurePicker by lazy { source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt") }
    private val atlasView by lazy { source("app/src/main/java/ir/exam/app/ui/figure/AtlasFigureView.kt") }
    private val bitmapRenderer by lazy { source("app/src/main/java/ir/exam/app/core/figure/AtlasBitmapRenderer.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }

    @Test
    fun `atlas tools open a type picker first and editor has no type selection`() {
        assertTrue("fun AtlasTypePickerDialog(" in atlasEditor)
        assertTrue("presetType: String? = null" in atlasEditor)
        assertTrue("AtlasTarget(kind = \"a\", chooseType = true)" in builder)
        assertTrue("AtlasTarget(kind = \"s\", domain = \"phys\", chooseType = true)" in builder)
        assertTrue("AtlasTypePickerDialog(" in builder)
        assertTrue("target.copy(chooseType = false, presetType = typeId)" in builder)
        // پنجرهٔ ویرایش دیگر انتخاب نوع ندارد (LazyRow انواع حذف شد؛ typeId ثابت).
        assertFalse("LazyRow" in atlasEditor)
        assertTrue("val typeId by remember { mutableStateOf(defaultType) }" in atlasEditor)
    }

    @Test
    fun `mark number sits at the arrow end in all renderers`() {
        assertTrue("drawCircle(color, radius, end)" in atlasEditor)
        assertTrue("drawCircle(Color(0xFFE4572E), radius, end)" in atlasView)
        assertTrue("canvas.drawCircle(x2, y2, radius, fillAccent)" in bitmapRenderer)
        assertFalse("drawCircle(Color(0xFFE4572E), radius, start)" in atlasView)
        assertFalse("canvas.drawCircle(x1, y1, radius, fillAccent)" in bitmapRenderer)
    }

    @Test
    fun `editor hides label boxes but keeps per-mark delete`() {
        assertFalse("برچسب/پاسخ نشانه" in atlasEditor)
        assertTrue("حذف نشانه" in atlasEditor)
        assertTrue("پاک‌کردن همه نشانه‌ها" in atlasEditor)
    }

    @Test
    fun `graph type picker is a two-column grid without the edit hint`() {
        val picker = figurePicker.substringAfter("fun FigureTypePickerDialog")
            .substringBefore("fun FigurePickerDialog")
        assertTrue("GridCells.Fixed(2)" in picker)
        assertFalse("برای ویرایش انتخاب کنید" in figurePicker)
    }
}
