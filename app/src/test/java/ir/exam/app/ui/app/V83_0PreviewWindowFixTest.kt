package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V83.0 — آیکن 👁 پیش‌نمایش فقط یک کادر خاکستریِ کم‌رنگ نشان می‌داد.
 *
 * علت: در نمایشگرهای زیر ۷۰۰px این قانون فعال بود
 *     .pwo-body #printContent{zoom:.44}
 * و `zoom` یک ویژگیِ **غیراستاندارد** است. در WebView اندروید روی این عنصر
 * (فرزندِ یک flex container با ارتفاعِ محدود) نتیجه نمی‌داد، برگهٔ A4 عملاً
 * نامرئی می‌شد و کاربر فقط پس‌زمینهٔ `#eef2f7` پنجره را می‌دید.
 *
 * رفع: `transform: scale(.44)` که پشتیبانیِ سراسری دارد. چون transform فضای
 * چیدمان را آزاد نمی‌کند، ارتفاع با `qmfFitPreviewScale()` جبران می‌شود.
 */
class V83_0PreviewWindowFixTest {
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
    fun `the unsupported zoom rule is gone from the preview window`() {
        assertFalse("zoom نباید برگردد", ".pwo-body #printContent{zoom:.44}" in assetText)
        // V84.0 — ضریب دیگر در CSS ثابت نیست؛ در JS از روی اندازهٔ واقعیِ
        // کادر محاسبه می‌شود. پس رفتار پین می‌شود نه عدد.
        assertFalse("ضریب ثابت نباید برگردد", "transform:scale(.44)" in assetText)
        assertTrue("'transform', 'scale(' + k.toFixed(3) + ')'" in assetText)
        assertTrue("'transform-origin', 'top center'" in assetText)
    }

    @Test
    fun `print zoom rules are untouched`() {
        // مسیر چاپ خارج از دامنه است و نباید دست بخورد
        assertTrue("zoom:.42 !important" in assetText)
        assertTrue("zoom:1 !important" in assetText)
    }

    @Test
    fun `the height left behind by transform is compensated`() {
        assertTrue("window.qmfFitPreviewScale" in assetText)
        assertTrue("qmfFitPreviewScale()" in assetText)
        assertTrue("marginBottom" in assetText)
        // با تغییر اندازه هم دوباره محاسبه شود
        assertTrue("ResizeObserver" in assetText)
    }

    @Test
    fun `the compensation is cleared when the window closes`() {
        val close = assetText.substringAfter("closePreviewWindow = function").substringBefore("\n  }")
        assertTrue("pc.style.removeProperty('margin-bottom')" in close)
        assertTrue("__qmfRO" in close)
    }

    @Test
    fun `the preview renders before the window is shown`() {
        val open = assetText.substringAfter("function openPreviewWindow()").substringBefore("\n  }")
        assertTrue("renderPreview()" in open)
    }

    @Test
    fun `the window still moves the real print content`() {
        // رفتار اصلی نباید عوض شود: همان گرهٔ printContent جابه‌جا می‌شود
        assertTrue("prevInfo = { parent: pc.parentNode, next: pc.nextSibling }" in assetText)
        assertTrue("ov.querySelector('.pwo-body').appendChild(pc)" in assetText)
        assertTrue("pc.classList.add('in-window')" in assetText)
        assertTrue("prevInfo.parent.insertBefore(pc, prevInfo.next)" in assetText)
    }

    @Test
    fun `the css that reveals the sheet inside the overlay is intact`() {
        assertTrue("#previewWinOverlay #printContent.live-preview{display:block !important}" in assetText)
    }

    @Test
    fun `there is a preview diagnostic`() {
        assertTrue("window.__qmfPreviewDiag" in assetText)
        listOf("contentBytes", "inOverlay", "display", "transform", "rectW")
            .forEach { assertTrue("کلید $it نیست", it in assetText) }
        assertTrue("__qmfPreviewDiag" in dialog)
    }

    @Test
    fun `the preview button still reaches the page`() {
        assertTrue("togglePreviewWindow" in dialog)
        assertTrue("window.togglePreviewWindow = function" in assetText)
    }

    @Test
    fun `earlier work still holds`() {
        assertFalse("activeExactTool !== 'formula'" in assetText)        // V82.0
        assertTrue("ExamPrintNative.editFigureTool" in assetText)        // V82.0
        assertTrue("getComputedStyle(f).display !== 'none'" in assetText) // V81.0
        assertTrue("if (url != MAIN_PAGE_URL) return" in dialog)         // V80.0
        assertTrue("f.src = MATH_EDITOR_URL" in assetText)               // V79.0
    }
}
