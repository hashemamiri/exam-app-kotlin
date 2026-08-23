package ir.exam.app.ui.math

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.IOException
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/** Phase 1 POC: local-only editor surface. Not wired into Builder yet. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QuestionEditorWebViewPoc(
    modifier: Modifier = Modifier,
    initialValue: String = "",
    onValueChanged: (String) -> Unit = {},
    onReady: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
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
                addJavascriptInterface(Bridge(onValueChanged, onReady, onError), "ExamEditorNative")
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
                        val value = JSONObject.quote(initialValue)
                        view.evaluateJavascript("window.ExamEditor && window.ExamEditor.setValue($value);", null)
                        view.evaluateJavascript("window.ExamEditorNative && ExamEditorNative.onReady && ExamEditorNative.onReady();", null)
                    }
                }
                loadUrl("https://exam-editor.local/question-editor/question_editor.html")
            }
        }
        // POC intentionally has no update-side effect: native owns the value.
    )
}

private class Bridge(
    private val onValueChanged: (String) -> Unit,
    private val onReady: () -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface fun onTextChanged(value: String?) { onValueChanged(value.orEmpty()) }
    @JavascriptInterface fun onReady() { onReady() }
    @JavascriptInterface fun onError(code: String?) { onError(code ?: "EDITOR_ERROR") }
}
