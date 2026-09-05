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
    private val cards by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintQuestionCards.kt").readText()
    }
    private val webview by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt").readText()
    }

    @Test
    fun `the preview wrapper interpolates its body`() {
        // `${'$'}body` یک `$body` لفظی می‌ساخت و همان زیرِ کادر دیده می‌شد
        assertTrue("<body>\${body}</body>" in webview)
        assertTrue("\${'\$'}body" !in webview)
    }

    @Test
    fun `question text is shown readably instead of raw json`() {
        assertTrue("window.__qmfDisplayText = function" in asset)
        assertTrue("displayText: window.__qmfDisplayText(String(q.id))" in asset)
        assertTrue("hasTokens:" in asset)
        // متنِ واقعی باید دست‌نخورده بماند وگرنه چاپ خراب می‌شود
        assertTrue("text: String(q.text == null ? '' : q.text)," in asset)
    }

    @Test
    fun `editing switches back to the real text so nothing is lost`() {
        assertTrue("if (editingText || !detail.hasTokens) text else detail.displayText" in cards)
        assertTrue("text = detail.text" in cards)
    }

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

    @Test
    fun `but the toggle stays for anything that relies on it`() {
        assertTrue("window.togglePreviewWindow = function" in asset)
    }
}
