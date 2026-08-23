package ir.exam.app.ui.math

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.IOException
import org.json.JSONObject

/**
 * V53.1 — کادر متن سؤال WebView (استثنای تأییدشدهٔ کاربر در کنار ویرایشگر فرمول).
 *
 * این WebView فقط asset محلی `question_editor/question_editor.html` را بارگیری
 * می‌کند؛ ناوبری خارجی مسدود است و هیچ token یا Secretی به آن داده نمی‌شود.
 * آیکن‌های درج همگی Native هستند و از طریق [QuestionEditorFieldController]
 * فرمان می‌دهند؛ متن نهایی همیشه از رویداد `onTextChanged` به ViewModel برمی‌گردد.
 */
class QuestionEditorFieldController {
    internal var webView: WebView? = null

    /** آخرین متنی که خود WebView گزارش کرده یا برایش push شده؛ برای جلوگیری از echo. */
    var lastJsValue: String = ""
        internal set

    /** بازکردن ابزارهای مرجع داخل WebView: formula / anatomy / periodic / physics / chemistry. */
    fun openTool(name: String): Boolean {
        val view = webView ?: return false
        val quoted = JSONObject.quote(name)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.openTool($quoted);", null)
        return true
    }

    /** درج توکن `%%FIG:{json}%%` ساخته‌شده در ویرایشگرهای Native در محل مکان‌نما. */
    fun insertFigureJson(specJson: String): Boolean {
        val view = webView ?: return false
        val quoted = JSONObject.quote(specJson)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.insertToken($quoted);", null)
        return true
    }

    /** V53.3 — جایگزینی توکن در حال ویرایش (dblclick) با خروجی ویرایشگر Native. */
    fun applyEditedFigureJson(specJson: String): Boolean {
        val view = webView ?: return false
        val quoted = JSONObject.quote(specJson)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.applyEditedToken($quoted);", null)
        return true
    }

    /** V53.3 — انصراف از ویرایش توکن dblclick. */
    fun cancelEditFigure() {
        webView?.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.cancelEditToken();", null)
    }

    /** همگام‌سازی متن از سمت Native (مثلاً افزودن سؤال از بانک) به کادر WebView. */
    fun setValue(text: String) {
        val view = webView ?: return
        lastJsValue = text
        val quoted = JSONObject.quote(text)
        view.evaluateJavascript("window.ExamEditor && ExamEditor.setValue($quoted);", null)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QuestionTextFieldWebView(
    controller: QuestionEditorFieldController,
    initialValue: String,
    onValueChanged: (String) -> Unit,
    onOverlayChanged: (Boolean) -> Unit = {},
    onEditFigureToken: (String) -> Unit = {},
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    DisposableEffect(controller) {
        onDispose { controller.webView = null }
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.setSupportZoom(false)
                addJavascriptInterface(
                    FieldBridge(controller, onValueChanged, onOverlayChanged, onEditFigureToken, onError),
                    "ExamEditorNative"
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val path = request.url.path ?: return null
                        val assetPath = path.removePrefix("/question-editor/")
                        if (assetPath.isBlank() || assetPath.contains("..")) return null
                        return try {
                            val stream = view.context.assets.open("question_editor/$assetPath")
                            val mime = when {
                                assetPath.endsWith(".html") -> "text/html"
                                assetPath.endsWith(".css") -> "text/css"
                                assetPath.endsWith(".js") -> "application/javascript"
                                assetPath.endsWith(".json") -> "application/json"
                                else -> "application/octet-stream"
                            }
                            WebResourceResponse(mime, "UTF-8", stream)
                        } catch (_: IOException) { null }
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                        onError("EDITOR_LOAD_FAILED")
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        controller.lastJsValue = initialValue
                        val value = JSONObject.quote(initialValue)
                        view.evaluateJavascript("window.ExamEditor && window.ExamEditor.setValue($value);", null)
                    }
                }
                controller.webView = this
                // پارامتر nativeTools=1 نوار ابزار داخلی HTML را مخفی می‌کند؛
                // دیالوگ فرمول گزینه‌ها همان صفحه را بدون این پارامتر باز می‌کند.
                loadUrl("https://exam-editor.local/question-editor/question_editor.html?nativeTools=1")
            }
        }
    )
}

private class FieldBridge(
    private val controller: QuestionEditorFieldController,
    private val onValueChanged: (String) -> Unit,
    private val onOverlayChanged: (Boolean) -> Unit,
    private val onEditFigureToken: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onTextChanged(value: String?) {
        val text = value.orEmpty()
        controller.lastJsValue = text
        onValueChanged(text)
    }

    @JavascriptInterface
    fun onOverlayChanged(open: Boolean) { onOverlayChanged.invoke(open) }

    /** V53.3 — دوبار-کلیک روی توکن جدول/تناوبی/آناتومی/علوم داخل WebView. */
    @JavascriptInterface
    fun onEditFigure(specJson: String?) {
        specJson?.takeIf { it.isNotBlank() }?.let(onEditFigureToken)
    }

    @JavascriptInterface
    fun onReady() = Unit

    @JavascriptInterface
    fun onError(code: String?) { onError.invoke(code ?: "EDITOR_ERROR") }
}
