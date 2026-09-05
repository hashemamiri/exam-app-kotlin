package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V85.0 — پیش‌نمایش پس از V84.0 هم درست نبود.
 *
 * تشخیصِ کاربر مقصر را لو داد:
 *     display:"block", visibility:"visible", opacity:"1",
 *     inlineDisplayPriority:"important",      ← اجبارِ V84 کار کرده بود
 *     transform:"matrix(0.545,...)",           ← مقیاس هم درست بود
 *     rectW:181, rectH:1216,                   ← برگه ابعاد دارد
 *     bodyH:12                                 ← ولی کادرش ۱۲ پیکسل است!
 *
 * یعنی برگه ۱۲۱۶ پیکسل بلند بود ولی داخل نواری به ارتفاعِ ۱۲ پیکسل — عملاً
 * نامرئی. مقصر خودِ «جبرانِ ارتفاع» در V84 بود:
 *     body.style.height = 'auto'                 (flex:1 1 auto را شکست)
 *     pc.style.marginBottom = '-1014px'          (حاشیهٔ منفیِ عظیم)
 * این دو با هم کادر را جمع کردند.
 *
 * رفع: مقیاس روی یک **لفافه** با ارتفاعِ صریحِ `natH × k` اعمال می‌شود.
 * نه حاشیهٔ منفی، نه دستکاریِ ارتفاعِ کادر.
 */
class V85_0PreviewWrapperTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val assetText by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `the negative margin hack is gone`() {
        val fit = assetText.substringAfter("window.qmfFitPreviewScale = function")
            .substringBefore("\n  };")
        assertFalse("حاشیهٔ منفی نباید برگردد", "marginBottom" in fit)
        assertFalse("ارتفاع کادر نباید auto شود", "body.style.height" in fit)
    }

    @Test
    fun `scaling happens on an explicit wrapper`() {
        assertTrue("qmf-pv-wrap" in assetText)
        assertTrue("box.style.height" in assetText)
        assertTrue("box.style.width" in assetText)
        assertTrue("Math.ceil(natH * kz)" in assetText)
    }

    @Test
    fun `the wrapper css does not collapse`() {
        assertTrue(".qmf-pv-wrap{margin:0 auto;position:relative;overflow:visible;direction:rtl}" in assetText)
    }

    @Test
    fun `transform origin follows the page direction so nothing shifts sideways`() {
        // V86.6 — صفحه RTL است: مبدأ باید راست باشد وگرنه ستونِ بارم بیرون می‌افتد
        assertTrue("'transform-origin', 'top right'" in assetText)
    }

    @Test
    fun `the wrapper is removed when the window closes`() {
        val close = assetText.substringAfter("closePreviewWindow = function").substringBefore("\n  }")
        assertTrue("qmf-pv-wrap" in close)
        assertTrue("box.parentNode.removeChild(box)" in close)
    }

    @Test
    fun `the V84 inline forcing is still there`() {
        val open = assetText.substringAfter("function openPreviewWindow()").substringBefore("\n  }")
        assertTrue("pc.style.setProperty('display', 'block', 'important')" in open)
    }

    @Test
    fun `the diagnostic now reports the deciding numbers`() {
        listOf("wrapExists", "wrapH", "bodyScrollH", "bodyClientH", "marginBottom")
            .forEach { assertTrue("کلید $it نیست", it in assetText) }
    }

    @Test
    fun `earlier work still holds`() {
        assertFalse("activeExactTool !== 'formula'" in assetText)          // V82.0
        assertTrue("ExamPrintNative.editFigureTool" in assetText)          // V82.0
        assertTrue("getComputedStyle(f).display !== 'none'" in assetText)  // V81.0
        assertTrue("f.src = MATH_EDITOR_URL" in assetText)                 // V79.0
        assertTrue("zoom:.42 !important" in assetText)                     // چاپ دست‌نخورده
    }
}
