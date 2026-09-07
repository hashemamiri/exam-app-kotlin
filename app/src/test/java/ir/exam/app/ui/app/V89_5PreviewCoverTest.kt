package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V89.5 — دکمه‌های جابه‌جاییِ داخلِ کادرِ متن اضافی بودند (جابه‌جایی در
 * پنجرهٔ پیش‌نمایش انجام می‌شود) و بستنِ پیش‌نمایش پنجرهٔ کل را مدیریت می‌کند.
 *
 * V100 — دو تستِ «فهرستِ کارت‌ها کنار می‌رود» و «پیش‌نمایشِ زنده ماژول‌ها را
 * فعال می‌کند» (__qmfRichPreview) با حذفِ کاملِ «آزمون‌ساز چاپی» حذف شدند.
 */
class V89_5PreviewCoverTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }

    @Test
    fun `closing the preview brings the cards back`() {
        assertTrue("window.ExamPrintNative.previewClosed()" in asset)
        assertTrue("typeof window.ExamPrintNative.previewClosed === 'function'" in asset)
        assertTrue("fun previewClosed()" in dialog)
        // V99.2 — پیش از بستن، چیدمانِ اشیاء اسنپ‌شات می‌شود و سپس
        // کارت‌ها برمی‌گردند.
        assertTrue("fetchFigLayoutsSnapshot()" in dialog)
        assertTrue("post { previewOpen = false }" in dialog)
    }

    @Test
    fun `the in-text move controls are gone`() {
        // جابه‌جایی در پنجرهٔ پیش‌نمایش انجام می‌شود، نه در کادرِ متن
        assertTrue("جابجایی: داخل کادر بکشید" !in asset)
        assertTrue("title=\"کنار هم / روی هم\"" !in asset)
        assertTrue("title=\"بیاور جلو\"" !in asset)
        assertTrue("title=\"ببر عقب\"" !in asset)
    }

    @Test
    fun `but their functions survive so nothing is lost`() {
        assertTrue("qmfToggleFigFree" in asset)
        assertTrue("qmfFigLayer" in asset)
    }

    @Test
    fun `dragging inside the preview still works`() {
        assertTrue("initPreviewFigureEditing" in asset)
        assertTrue("function previewScale()" in asset)
    }
}
