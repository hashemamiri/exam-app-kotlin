package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V89.6 — جابه‌جاییِ شکل در پنجرهٔ پیش‌نمایش: لمسِ نخست شیء را آزاد می‌کند،
 * شناور فقط کفِ صفر دارد، و تغییرِ اندازه مقیاسِ پیش‌نمایش را لحاظ می‌کند.
 *
 * V100 — سه تستِ CSSِ «پیش‌نمایشِ زندهٔ کارت» (__qmfPreviewCss/extraCss) با
 * حذفِ کاملِ «آزمون‌ساز چاپی» (و کارت‌های بومی) حذف شدند؛ مکانیکِ شکل‌ها در
 * پنجرهٔ پیش‌نمایش می‌ماند.
 */
class V89_6LivePreviewCssTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `the first touch makes a figure free so it can move anywhere`() {
        assertTrue("!drag.fig.classList.contains('fig-free')" in asset || "!fig.classList.contains('fig-free')" in asset)
        assertTrue("drag.fig.classList.add('fig-free');" in asset)
    }

    @Test
    fun `a free figure may rise above its natural position`() {
        // V93 — شناور فقط کفِ صفر دارد: سقفِ سلولِ اندازه‌گیری‌شده حذف شد
        // (سقفِ منفی شیء را به بالا می‌پراند و قفل می‌کرد).
        // V99.1 — سلول با minHeight رشد نمی‌کند (همان رشد، خطوطِ کادرِ سؤال را
        // جابه‌جا می‌کرد)؛ ارتفاعِ کادر را slotِ شیء در جریانِ متن نگه می‌دارد.
        assertTrue("Math.min(maxY, y)" !in asset)
        assertTrue("y = Math.max(0, y);" in asset)
        assertTrue("parent.style.minHeight = need + 'px';" !in asset)
        assertTrue("function syncFigFlowSlot(qId, figIndex, makingFree) {" in asset)
    }

    @Test
    fun `resizing converts pointer travel out of the scaled space`() {
        assertTrue("const dx = (e.clientX - drag.sx) / ks;" in asset)
        assertTrue("const dy = (e.clientY - drag.sy) / ks;" in asset)
    }
}
