package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V68.8 — چاپ WYSIWYG پیوسته + درگ آزاد بدون دزدیده‌شدن ژست توسط اسکرول.
 *
 * گزارش کاربر روی V68.7: «چاپ و ویرایشگر خیلی متفاوت هستند» و «حرکت تصویر
 * گالری آزادانه نیست». ریشه‌ها: (۱) موتور چاپ بلوک‌به‌بلوک صفحه‌بندی می‌کرد و
 * بلوک بلند (جدول تناوبی) را کامل به صفحهٔ بعد می‌برد و یک فضای خالی بزرگ جا
 * می‌گذاشت، در حالی که ویرایشگر پیوسته است؛ (۲) کل سند داخل verticalScroll بود
 * و ژست عمودیِ درگِ شیء توسط اسکرول صفحه دزدیده می‌شد.
 *
 * راه‌حل: چیدمان چاپ مثل ویرایشگر «پیوسته» می‌شود (y تجمعی، بدون صفحه‌بندی
 * بلوک‌به‌بلوک) و هر صفحهٔ A4 یک برش از همان سند پیوسته است (clip + translate)؛
 * و در ویرایشگر هنگام انتخاب یک تصویر/شکل، اسکرول صفحه موقتاً غیرفعال می‌شود تا
 * ژست عمودیِ درگ به خودِ شیء برسد. عمداً هیچ بیت‌مایپ بلندِ کل سند ساخته نمی‌شود
 * (در آزمون بلند = ریسک OutOfMemory) و متن به‌صورت برداری روی صفحه می‌نشیند.
 */
class V68_8ContinuousPrintFreeDragTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val pdf by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }

    @Test
    fun `print lays out one continuous document and draws each A4 page as a slice of it`() {
        assertTrue("private fun placeContinuous()" in pdf)
        assertTrue("private fun slicePages()" in pdf)
        assertTrue("private fun drawSlice(" in pdf)
        assertTrue("private fun drawBlockAt(" in pdf)
        // هر صفحه = همان سند پیوسته با جابه‌جایی به اندازهٔ برش + clip ناحیهٔ محتوا
        assertTrue("canvas.translate(0f, dstTop - slice.first)" in pdf)
        assertTrue("canvas.clipRect(MARGIN - 6f, dstTop, PAGE_WIDTH - MARGIN + 6f, dstTop + sliceH)" in pdf)
        // فقط بلوک‌های متقاطع با همین برش رسم می‌شوند
        assertTrue("if (p.y + p.height > slice.first && p.y < slice.second)" in pdf)
    }

    @Test
    fun `the whole document is never rasterized into one tall bitmap`() {
        // آزمون بلند: بیت‌مایپ کل سند ده‌ها مگابایت می‌شد و چاپ کرش می‌کرد
        assertFalse("docBitmap" in pdf)
        assertFalse("renderDocument()" in pdf)
        assertFalse("Bitmap.createBitmap(w, h" in pdf)
    }

    @Test
    fun `block pagination that caused page-jump and blank gap is gone`() {
        // الگوی صفحه‌بندی بلوک‌به‌بلوک (بلوک بلند → صفحهٔ بعد + فضای خالی)
        assertFalse("used + height > capacity" in pdf)
        assertFalse("PlannedPage(" in pdf)
        assertFalse("private val pages: List<PlannedPage>" in pdf)
        assertFalse("private fun planPages()" in pdf)
    }

    @Test
    fun `free image y in continuous doc is relative to its flow slot without page clamp`() {
        assertTrue("(top+block.imageYmm*MM_TO_PT).coerceAtLeast(0f)" in pdf)
        // clamp به «یک صفحه» دیگر در مسیر رسم تصویر آزاد نیست
        assertFalse("(top+block.imageYmm*MM_TO_PT).coerceIn(MARGIN, PAGE_HEIGHT-MARGIN-height)" in pdf)
    }

    @Test
    fun `editor suspends page scroll while an object is selected so drag is free`() {
        assertTrue("val scrollEnabled = selectedImageId == null && selectedFigure == null" in editor)
        assertTrue("verticalScroll(scroll, enabled = scrollEnabled)" in editor)
    }

    @Test
    fun `header still only on page one and signatures only on last page`() {
        assertTrue("if (pageNumber == 1) drawHeader(canvas, pageNumber, totalPages)" in pdf)
        assertTrue("if (pageNumber == totalPages) canvas.drawText(printable.footerNote" in pdf)
    }
}
