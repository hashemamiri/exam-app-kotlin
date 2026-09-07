package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V91 — ایرادهای پنجرهٔ پیش‌نمایش:
 * ۱) جابه‌جاییِ اشیاء محدود به همان سؤال می‌شود.
 * ۲) کادرِ سفیدِ پس‌زمینهٔ هر شیء در پیش‌نمایش حذف شد (رفع همپوشانی و محوشدنِ ستون بارم).
 *
 * V100 — دو تستِ «بازیابی فهرستِ بومی» و «حالتِ خالیِ بومی» با حذفِ کاملِ
 * «آزمون‌ساز چاپی» (کارت‌های بومی + جریانِ بازیابی) حذف شدند؛ دو تستِ
 * مکانیکِ پیش‌نمایش (سلولِ سؤال + پس‌زمینه) می‌مانند.
 */
class V91_0PreviewRestoreFixTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `a dragged figure stays inside its own question cell`() {
        // V93/V99.1 — شیءِ شناور با کفِ صفر داخلِ سلولِ سؤال می‌ماند (بالای
        // سؤال بیرون نمی‌رود). V99.1 — سلول دیگر با minHeight رشد نمی‌کند؛
        // ارتفاعِ کادر را slotِ شیء در جریانِ متن نگه می‌دارد تا خطوطِ
        // کادر با جابه‌جاییِ شیء جابه‌جا نشوند.
        assertTrue("Math.min(maxY, y)" !in asset)
        assertTrue("y = Math.max(0, y);" in asset)
        assertTrue("parent.style.minHeight = need + 'px';" !in asset)
        assertTrue("function syncFigFlowSlot(qId, figIndex, makingFree) {" in asset)
    }

    @Test
    fun `the white background box is gone from the preview`() {
        assertTrue("#previewArea .interactive-figure .qmf-fig {" in asset)
        assertTrue("background: transparent !important;" in asset)
        assertTrue("box-shadow: none !important;" in asset)
        assertTrue("padding: 0 !important;" in asset)
    }
}
