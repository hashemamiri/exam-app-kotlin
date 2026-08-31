package ir.exam.app.ui.printing

import android.annotation.SuppressLint
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.domain.model.OfficialExamPrintable
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * V73.0 — پنجرهٔ تمام‌صفحهٔ چاپ تعاملی HTML با انتقال خودکار سؤالات آزمون:
 * فایل چاپ تعاملی را در WebView بارگذاری کرده و سؤالات و مشخصات سربرگ آزمون
 * را به‌صورت خودکار در بخش سؤالات و سربرگ فایل تزریق می‌کند.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExamHtmlPrintDialog(
    printable: OfficialExamPrintable,
    onDismiss: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var jsError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF1E3A8A)) {
            Column(Modifier.fillMaxSize()) {
                // نوار بالای پنجره با دکمه بستن و عنوان آزمون
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "چاپ آزمون: " + printable.documentTitle.ifBlank { printable.subject }.ifBlank { "آزمون" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(Modifier.fillMaxSize().weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.parseColor("#E8ECF1"))
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                @Suppress("DEPRECATION")
                                settings.allowFileAccessFromFileURLs = false
                                @Suppress("DEPRECATION")
                                settings.allowUniversalAccessFromFileURLs = false
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false

                                addJavascriptInterface(
                                    ExamPrintBridge(
                                        onPrint = { mode ->
                                            post {
                                                runCatching {
                                                    val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                                    val jobName = "${printable.documentTitle.ifBlank { "exam" }}-$mode"
                                                    val printAdapter = createPrintDocumentAdapter(jobName)
                                                    printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
                                                }
                                            }
                                        },
                                        onClose = { post { onDismiss() } },
                                        onError = { message -> post { jsError = message; loading = false } }
                                    ),
                                    "ExamPrintNative"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        if (!request.isForMainFrame) return false
                                        val url = request.url
                                        val isLocal = url.host == "exam-print.local" || url.scheme == "about"
                                        return !isLocal
                                    }

                                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                        val path = request.url.path ?: return emptyResponse()
                                        if (!path.startsWith("/print/")) return emptyResponse()
                                        val assetPath = path.removePrefix("/print/")
                                        if (assetPath.isBlank() || assetPath.contains("..")) return emptyResponse()
                                        return try {
                                            val stream = view.context.assets.open("print/$assetPath")
                                            val mime = when {
                                                assetPath.endsWith(".html") -> "text/html"
                                                assetPath.endsWith(".css") -> "text/css"
                                                assetPath.endsWith(".js") -> "application/javascript"
                                                assetPath.endsWith(".json") -> "application/json"
                                                assetPath.endsWith(".png") -> "image/png"
                                                assetPath.endsWith(".jpg") || assetPath.endsWith(".jpeg") -> "image/jpeg"
                                                else -> "application/octet-stream"
                                            }
                                            WebResourceResponse(mime, "UTF-8", stream)
                                        } catch (_: IOException) { emptyResponse() }
                                    }

                                    private fun emptyResponse(): WebResourceResponse =
                                        WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))

                                    override fun onPageFinished(view: WebView, url: String) {
                                        val payload = ExamHtmlPrintPayloadBuilder.build(printable).toString()
                                        var attempts = 0
                                        fun tryInject() {
                                            attempts++
                                            view.evaluateJavascript(
                                                "(function(){if(window.setExamData){window.setExamData($payload);return 'ok';}return 'wait';})();"
                                            ) { result ->
                                                when {
                                                    result?.contains("ok") == true -> post { loading = false }
                                                    attempts < 50 -> view.postDelayed({ tryInject() }, 100)
                                                    else -> post { loading = false }
                                                }
                                            }
                                        }
                                        tryInject()
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                                        if (message.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                                            val safe = message.message().replace(Regex("https?://\\S+"), "[url]").take(300)
                                            post { jsError = "CONSOLE: $safe"; loading = false }
                                        }
                                        return true
                                    }
                                }

                                loadUrl("https://exam-print.local/print/exam_print.html")
                            }
                        },
                        onRelease = { view ->
                            view.stopLoading()
                            view.loadUrl("about:blank")
                            view.removeAllViews()
                            view.destroy()
                        }
                    )

                    if (loading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }

                    jsError?.let { message ->
                        Text(
                            "خطای صفحه چاپ: $message",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

private class ExamPrintBridge(
    private val onPrint: (String) -> Unit,
    private val onClose: () -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun print(mode: String?) {
        onPrint(mode ?: "student")
    }

    @JavascriptInterface
    fun close() {
        onClose()
    }

    @JavascriptInterface
    fun onError(code: String?) {
        code?.takeIf { it.isNotBlank() }?.let(onError)
    }
}
