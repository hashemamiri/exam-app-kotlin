package ir.exam.app.ui.math

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.IOException
import org.json.JSONObject

/**
 * V53.4 — پنجرهٔ «تمام‌صفحهٔ» ویرایشگر فرمول WebView (درخواست صریح کاربر:
 * پنجرهٔ فرمول همه‌جا کاملاً WebView و تمام‌صفحه باشد).
 *
 * به‌جای بازشدن iframe فرمول داخل WebView کوچک کادر متن (که فقط صفحه را تاریک
 * می‌کرد)، همین Dialog تمام‌صفحه همان asset محلی را با `?formulaHost=1` بارگیری
 * می‌کند؛ پوستهٔ صفحه مخفی است و مستقیماً ویرایشگر فرمول مرجع با متن و محدودهٔ
 * انتخاب دریافتی باز می‌شود. خروجی، متن کامل به‌روزشده است که پس از بسته‌شدن
 * ویرایشگر (تأیید یا انصراف مرجع) به Native برمی‌گردد.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FormulaHostDialog(
    initialText: String,
    selectionStart: Int,
    selectionEnd: Int,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    var latestText by remember { mutableStateOf(initialText) }
    var editorOpened by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = { onResult(latestText); onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        // پس‌زمینه همان رنگ صفحهٔ مرجع تا هیچ فریم سفید/ناهماهنگی دیده نشود.
        Surface(Modifier.fillMaxSize(), color = ComposeColor(0xFFE9EEF5)) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(Color.TRANSPARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            @Suppress("DEPRECATION")
                            settings.allowFileAccessFromFileURLs = false
                            @Suppress("DEPRECATION")
                            settings.allowUniversalAccessFromFileURLs = false
                            settings.setSupportZoom(false)
                            addJavascriptInterface(
                                FormulaHostBridge(
                                    onText = { latestText = it },
                                    onOverlay = { open ->
                                        if (open) {
                                            editorOpened = true
                                            loading = false
                                        } else if (editorOpened) {
                                            // بسته‌شدن ویرایشگر مرجع (تأیید/انصراف خودش) = پایان کار.
                                            post { onResult(latestText); onDismiss() }
                                        }
                                    }
                                ),
                                "ExamEditorNative"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                    val path = request.url.path ?: return emptyResponse()
                                    // V54.4 — مسیرهای خارج از asset محلی پاسخ خالی امن می‌گیرند.
                                    if (!path.startsWith("/question-editor/")) return emptyResponse()
                                    val assetPath = path.removePrefix("/question-editor/")
                                    if (assetPath.isBlank() || assetPath.contains("..")) return emptyResponse()
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
                                    } catch (_: IOException) { emptyResponse() }
                                }

                                private fun emptyResponse(): WebResourceResponse =
                                    WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                                override fun onPageFinished(view: WebView, url: String) {
                                    val text = JSONObject.quote(initialText)
                                    view.evaluateJavascript(
                                        "window.ExamEditorFormula && ExamEditorFormula.begin($text, $selectionStart, $selectionEnd);",
                                        null
                                    )
                                }
                            }
                            loadUrl("https://exam-editor.local/question-editor/question_editor.html?formulaHost=1")
                        }
                    }
                )
                if (loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

private class FormulaHostBridge(
    private val onText: (String) -> Unit,
    private val onOverlay: (Boolean) -> Unit
) {
    @JavascriptInterface
    fun onTextChanged(value: String?) { onText(value.orEmpty()) }

    @JavascriptInterface
    fun onOverlayChanged(open: Boolean) { onOverlay(open) }

    @JavascriptInterface
    fun onReady() = Unit

    @JavascriptInterface
    fun onError(code: String?) = Unit
}
