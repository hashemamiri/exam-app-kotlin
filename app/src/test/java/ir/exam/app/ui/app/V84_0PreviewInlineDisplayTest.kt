package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V84.0 — پیش‌نمایش پس از V83.0 هم روی دستگاه باز نمی‌شد.
 *
 * تشخیصِ کاربر (`__qmfPreviewDiag`) نشان داد:
 *     printContentExists:true, contentBytes:2650, overlayOpen:false,
 *     inOverlay:false, display:"none", rectW:0, rectH:0
 * یعنی محتوا **هست** ولی برگه `display:none` مانده بود.
 *
 * ریشه: دیده‌شدنِ برگه به یک سلکتورِ نزولیِ CSS وابسته بود
 *     #previewWinOverlay #printContent.live-preview{display:block !important}
 * که باید بر `#printContent.live-preview{display:none !important}` غلبه کند.
 * در WebView این وابستگی شکننده است و اگر اعمال نشود، پنجره باز می‌شود ولی
 * برگه نامرئی می‌ماند — همان «کادر خاکستریِ کم‌رنگ».
 *
 * رفع: دیگر به هیچ قانونِ CSSِ صفحه تکیه نمی‌کنیم. هنگام باز شدن،
 * `display/visibility/opacity` به‌صورت **inline با اولویتِ important** روی
 * خودِ عنصر نوشته می‌شوند و هنگام بستن پاک می‌شوند. ضریبِ کوچک‌نمایی هم
 * به‌جای media queryِ ثابت، از روی عرضِ واقعیِ کادر اندازه‌گیری می‌شود.
 */
class V84_0PreviewInlineDisplayTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val assetText by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }

    @Test
    fun `opening forces visibility inline so no css rule can hide the sheet`() {
        val open = assetText.substringAfter("function openPreviewWindow()").substringBefore("\n  }")
        assertTrue("pc.style.setProperty('display', 'block', 'important')" in open)
        assertTrue("pc.style.setProperty('visibility', 'visible', 'important')" in open)
        assertTrue("pc.style.setProperty('opacity', '1', 'important')" in open)
    }

    @Test
    fun `closing removes the inline overrides`() {
        val close = assetText.substringAfter("closePreviewWindow = function").substringBefore("\n  }")
        listOf("display", "visibility", "opacity", "transform", "transform-origin", "width")
            .forEach { assertTrue("پاک‌سازی $it نیست", "pc.style.removeProperty('$it')" in close) }
    }

    @Test
    fun `the scale is measured from the real box not guessed`() {
        assertFalse("ضریب ثابت CSS نباید بماند", "transform:scale(.44)" in assetText)
        assertFalse("zoom نباید برگردد", "zoom:.44" in assetText)
        assertTrue("body.clientWidth" in assetText)
        assertTrue("'transform', 'scale(' + k.toFixed(3) + ')'" in assetText)
        // حدهای منطقی
        assertTrue("k < 0.2" in assetText)
        assertTrue("k >= 1" in assetText)
    }

    @Test
    fun `the transform height is still compensated`() {
        // V85.0 — لفافهٔ صریح جای حاشیهٔ منفی را گرفت.
        assertTrue("qmf-pv-wrap" in assetText)
        assertTrue("ResizeObserver" in assetText)
    }

    @Test
    fun `print zoom rules remain untouched`() {
        assertTrue("zoom:.42 !important" in assetText)
        assertTrue("zoom:1 !important" in assetText)
    }

    @Test
    fun `the css reveal rule is kept as a second line of defence`() {
        assertTrue(
            "#previewWinOverlay #printContent.live-preview{display:block !important}" in assetText
        )
    }

    @Test
    fun `the window still moves the real node and restores it`() {
        assertTrue("prevInfo = { parent: pc.parentNode, next: pc.nextSibling }" in assetText)
        assertTrue("ov.querySelector('.pwo-body').appendChild(pc)" in assetText)
        assertTrue("prevInfo.parent.insertBefore(pc, prevInfo.next)" in assetText)
    }

    @Test
    fun `the diagnostic reports why the preview failed`() {
        listOf("inlineDisplayPriority", "scrollW", "scrollH", "toggleFn", "fitFn", "uiInstalled")
            .forEach { assertTrue("کلید $it نیست", it in assetText) }
        assertTrue("__qmfPreviewDiag" in dialog)
    }

    @Test
    fun `preview still renders before showing`() {
        val open = assetText.substringAfter("function openPreviewWindow()").substringBefore("\n  }")
        assertTrue("renderPreview()" in open)
    }

    @Test
    fun `earlier work still holds`() {
        assertFalse("activeExactTool !== 'formula'" in assetText)         // V82.0
        assertTrue("ExamPrintNative.editFigureTool" in assetText)         // V82.0
        assertTrue("getComputedStyle(f).display !== 'none'" in assetText) // V81.0
        assertTrue("if (url != MAIN_PAGE_URL) return" in dialog)          // V80.0
        assertTrue("f.src = MATH_EDITOR_URL" in assetText)                // V79.0
    }
}
