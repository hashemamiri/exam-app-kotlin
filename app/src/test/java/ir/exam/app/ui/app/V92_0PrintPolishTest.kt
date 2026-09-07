package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V92 — گزارش‌های کاربر از پنجرهٔ آزمون‌سازِ چاپی که با مکانیکِ صفحه حل
 * شدند: اندازهٔ اشیاء/فرمول‌ها، کوچک‌شدن با لمس، ستونِ بارم در چاپ، و
 * لایهٔ بالای شیءِ لمس‌شده.
 *
 * V100 — دو تستِ «بازهٔ انتخابِ فرمول به ویرایشگر» (کارت‌های بومی) و
 * «فهرستِ بومی اول باز می‌شود» با حذفِ کاملِ «آزمون‌ساز چاپی» حذف شدند.
 */
class V92_0PrintPolishTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `non-formula objects shrink to 30 percent and formulas stay at 70 percent`() {
        assertTrue("zoom:.30 !important" in asset)
        assertTrue("max-width:30% !important" in asset)
        // جبرانِ دکمه‌های × و ✎ با ۱/۰٫۳۰
        assertTrue("transform:scale(3.3333) !important" in asset)
        assertTrue("transform: scale(3.3333) !important" in asset)
        // فرمول (اتم) دست‌نخورده در ۷۰٪
        assertTrue(".qmf-surface .qmf-atom{position:relative;font-size:.7em;line-height:1.2;cursor:pointer;user-select:none}" in asset)
    }

    @Test
    fun `pure move no longer rewrites the height so touching cannot shrink the object`() {
        assertTrue("storedH: Number.isFinite(+layout.h)," in asset)
        assertTrue("if (d.resizing || d.storedH) {" in asset)
        assertTrue("d.fig.style.removeProperty('height');" in asset)
    }

    @Test
    fun `the score column is protected in print from covering white boxes`() {
        assertTrue("<style id=\"qmf-print-box-clean-v92\">" in asset)
        assertTrue("background:transparent !important;" in asset)
        assertTrue(".interactive-figure.fig-free{" in asset)
        assertTrue("max-width:100% !important;" in asset)
    }

    @Test
    fun `the touched object always rises to the top layer`() {
        assertTrue("z: z0" in asset)
        assertTrue("fig.style.zIndex = String(z0);" in asset)
        assertTrue("fig.style.zIndex = String(zz);" in asset)
        assertTrue("setFigLayout(qid, idx, { z: zz })" in asset)
    }
}
