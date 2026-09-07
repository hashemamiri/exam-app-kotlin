package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V89.3 — توکنِ خام در کادرِ متن، آب‌رفتنِ شیء با هر لمس، درگِ کند، و
 * چشمی که گاهی می‌بست به‌جای بازکردن.
 */
class V89_3PreviewAndTokensTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val webview by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt").readText()
    }

    // V100 — wrapPrintPreviewHtml (پوششِ نمایشِ زنده) با حذفِ نمایشگرِ
    // زندهٔ کارتِ بومی (آزمون‌ساز چاپی) از QuestionTextFieldWebView حذف شد.
    @Test
    fun `the live preview wrapper stays deleted`() {
        assertTrue("wrapPrintPreviewHtml" !in webview)
        assertTrue("PrintRichTextPreview" !in webview)
    }

    @Test
    fun `question text is shown readably instead of raw json`() {
        assertTrue("window.__qmfDisplayText = function" in asset)
        assertTrue("displayText: window.__qmfDisplayText(String(q.id))" in asset)
        assertTrue("hasTokens:" in asset)
        // متنِ واقعی باید دست‌نخورده بماند وگرنه چاپ خراب می‌شود
        assertTrue("text: String(q.text == null ? '' : q.text)," in asset)
    }

    // V100 — «کادرِ متنِ خوانا/واقعی» (QuestionTextWebSection) در کارتِ بومی
    // بود و با حذفِ «آزمون‌ساز چاپی» حذف شد.

    @Test
    fun `the drag accounts for the preview scale`() {
        // rect.width عرضِ دیده‌شده است؛ ذخیره‌اش شیء را با هر لمس آب می‌کرد
        assertTrue("function previewScale()" in asset)
        assertTrue("(rect.width / previewScale())" in asset)
        assertTrue("(rect.height / previewScale())" in asset)
        assertTrue("(e.clientX - drag.sx) / k" in asset)
        assertTrue("(e.clientY - drag.sy) / k" in asset)
    }

    @Test
    fun `the eye always opens the preview`() {
        assertTrue("window.__qmfShowPreview = function" in asset)
        assertTrue("if (!document.getElementById('previewWinOverlay')) openPreviewWindow();" in asset)
        assertTrue("window.__qmfShowPreview?window.__qmfShowPreview()" in dialog)
    }

    // V100 — `togglePreviewWindow` با حذفِ نوارِ HTML (آزمون‌ساز چاپی) از
    // صفحه حذف شد؛ باز شدنِ پیش‌نمایش فقط از تزریقِ بومی (آیکن چشم).
}
