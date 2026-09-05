package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V89.5 — فهرستِ کارت‌ها روی WebView می‌نشست و پنجرهٔ پیش‌نمایش را می‌پوشاند،
 * دکمه‌های جابه‌جاییِ داخلِ کادرِ متن اضافی بودند، و شیءِ درج‌شده «زنده»
 * نمی‌شد چون ماژول‌های شکل معوق بودند.
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
    fun `the card list steps aside while the preview is open`() {
        // بدونِ سؤال فهرست خالی بود و مشکل دیده نمی‌شد؛ با سؤال چشم بی‌اثر بود
        assertTrue("if (cardDetails.isNotEmpty() && !previewOpen) {" in dialog)
        assertTrue("if (r?.contains(\"ok\") == true) previewOpen = true" in dialog)
    }

    @Test
    fun `closing the preview brings the cards back`() {
        assertTrue("window.ExamPrintNative.previewClosed()" in asset)
        assertTrue("typeof window.ExamPrintNative.previewClosed === 'function'" in asset)
        assertTrue("fun previewClosed()" in dialog)
        assertTrue("onPreviewClosed = { post { previewOpen = false } }" in dialog)
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

    @Test
    fun `the live preview activates the deferred figure modules first`() {
        // V87.3 ماژول‌ها را معوق کرد؛ بدونِ فعال‌سازی، renderFigToken جای خالی می‌دهد
        val at = asset.indexOf("window.__qmfRichPreview = function")
        assertTrue(at > 0)
        val head = asset.substring(at, at + 700)
        assertTrue("__qmfEnsureFigTools()" in head)
        assertTrue(head.indexOf("__qmfEnsureFigTools()") < head.indexOf("renderRichText("))
    }
}
