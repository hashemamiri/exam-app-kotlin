package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V91 — ایرادهای پنجرهٔ پیش‌نمایش و بازیابیِ آزمون‌سازِ چاپی:
 * ۱) جابه‌جاییِ اشیاء محدود به همان سؤال می‌شود.
 * ۲) کادرِ سفیدِ پس‌زمینهٔ هر شیء در پیش‌نمایش حذف شد (رفع همپوشانی و محوشدنِ ستون بارم).
 * ۳) دکمهٔ بازیابی واقعاً فهرستِ بومی را تازه می‌کند و پیامِ بومی می‌دهد.
 * ۴) حالتِ خالیِ بومی به‌جای صفحهٔ خالیِ WebView.
 */
class V91_0PreviewRestoreFixTest {

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
    fun `a dragged figure stays inside its own question cell`() {
        // ارتفاعِ سلولِ سؤال اندازه‌گیری می‌شود …
        assertTrue("parentH: Math.max(1, pr.height || parent.clientHeight || 0)" in asset)
        // … و شیءِ شناور بینِ کف و سقفِ سلول گیره می‌خورد.
        assertTrue("var maxY = Math.max(0, m.parentH - h - borderSafe);" in asset)
        assertTrue("y = Math.max(0, Math.min(maxY, y));" in asset)
    }

    @Test
    fun `the white background box is gone from the preview`() {
        assertTrue("#previewArea .interactive-figure .qmf-fig {" in asset)
        assertTrue("background: transparent !important;" in asset)
        assertTrue("box-shadow: none !important;" in asset)
        assertTrue("padding: 0 !important;" in asset)
    }

    @Test
    fun `restore reports success so the host can refresh the list`() {
        assertTrue("return 'ok';" in asset)
        assertTrue("return window.restoreAutosave();" in dialog)
    }

    @Test
    fun `the restored questions appear immediately in the native list`() {
        // فهرستِ بومی همان لحظه تازه می‌شود؛ پیش‌تر فقط پس از افزودنِ سؤال ظاهر می‌شد.
        assertTrue("cardsRefresh++" in dialog)
        assertTrue("\"آزمون بازیابی شد ✓\"" in dialog)
        // در میزبانِ بومی، اعلانِ دوبارهٔ JS نمی‌آید (پیامِ بومی یک‌بار است).
        assertTrue(
            "typeof window.ExamPrintNative.toast === 'function')) qmfToast('آزمون ذخیره‌شده بازیابی شد.')"
                in asset
        )
    }

    @Test
    fun `the print builder opens natively with an empty state`() {
        assertTrue("cardDetails.isEmpty() && !previewOpen && !loading" in dialog)
        assertTrue("\"هنوز سؤالی ساخته نشده است.\"" in dialog)
    }
}
