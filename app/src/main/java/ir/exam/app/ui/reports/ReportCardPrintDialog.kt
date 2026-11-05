package ir.exam.app.ui.reports

import android.annotation.SuppressLint
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.ui.printing.findActivityContext
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.serialization.json.JsonObject

/**
 * V339 — انتخاب قالب کارنامه، پیش‌نمایش A4 و چاپ.
 *
 * پوستهٔ انتخابگر/پیش‌نمایش داخل همان موتور مشترک سایت است
 * (`assets/reportcard/reportcard.html`)؛ این فایل فقط میزبان WebView و پل چاپ است.
 * هیچ پرداختی در این مسیر نیست: کارنامه مثل قبل رایگان است.
 */
internal const val REPORT_CARD_URL = "https://exam-print.local/reportcard/reportcard.html"

@Composable
fun ReportCardPrintDialog(payload: JsonObject, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val webView = remember(payload) { createReportCardWebView(context, payload, title, onDismiss) }
    DisposableEffect(webView) {
        onDispose {
            runCatching {
                webView.removeJavascriptInterface("ReportCardBridge")
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { webView })
    }
}

@SuppressLint("SetJavaScriptEnabled")
internal fun createReportCardWebView(
    context: Context,
    payload: JsonObject,
    title: String,
    onDismiss: () -> Unit
): WebView = WebView(context).apply {
    setBackgroundColor(android.graphics.Color.WHITE)
    settings.javaScriptEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.setSupportZoom(false)
    // V339.2 — بزرگ‌نمایی سیستمی اندروید نباید هدر را درشت کند؛ زوم فقط داخل موتور و روی برگه است.
    settings.textZoom = 100
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = false
    addJavascriptInterface(ReportCardBridge(this, title, onDismiss), "ReportCardBridge")
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            return request.url.host != "exam-print.local"
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val path = request.url.path ?: return emptyResponse()
            if (path.startsWith("/fonts/")) {
                val name = path.removePrefix("/fonts/").removeSuffix(".ttf")
                if (name.isBlank() || !name.matches(Regex("[a-z0-9_]+"))) return emptyResponse()
                val res = view.context.resources
                val id = res.getIdentifier(name, "font", view.context.packageName)
                return try {
                    val stream = if (id != 0) res.openRawResource(id) else view.context.assets.open("fonts/$name.ttf")
                    WebResourceResponse("font/ttf", null, stream)
                } catch (_: Exception) { emptyResponse() }
            }
            if (!path.startsWith("/reportcard/")) return emptyResponse()
            val assetPath = "reportcard/" + path.removePrefix("/reportcard/")
            if (assetPath.contains("..")) return emptyResponse()
            return try {
                val mime = when {
                    assetPath.endsWith(".html") -> "text/html"
                    assetPath.endsWith(".css") -> "text/css"
                    assetPath.endsWith(".js") -> "application/javascript"
                    else -> "application/octet-stream"
                }
                WebResourceResponse(mime, "UTF-8", view.context.assets.open(assetPath))
            } catch (_: IOException) { emptyResponse() }
        }

        private fun emptyResponse(): WebResourceResponse =
            WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))

        override fun onPageFinished(view: WebView, url: String) {
            if (url != REPORT_CARD_URL) return
            // جداکننده‌های خط JS (U+2028/2029) نباید داخل نام دانش‌آموز رشتهٔ تزریق را بشکنند
            val json = payload.toString().replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
            view.evaluateJavascript("(function(){try{window.ReportCard.open($json,{skin:'app'});return 'ok'}catch(e){return 'err:'+e}})()", null)
        }
    }
    loadUrl(REPORT_CARD_URL)
}

private class ReportCardBridge(
    private val webView: WebView,
    private val title: String,
    private val onDismiss: () -> Unit
) {
    @JavascriptInterface
    fun print(templateId: String?) {
        webView.post {
            runCatching {
                val ctx = webView.context
                val printContext = ctx.findActivityContext() ?: ctx
                val manager = printContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (manager == null) {
                    Toast.makeText(ctx, "سرویس چاپ این دستگاه در دسترس نیست.", Toast.LENGTH_LONG).show()
                    return@post
                }
                val jobName = (title.ifBlank { "کارنامه" } + "-" + (templateId ?: "1")).take(80)
                manager.print(
                    jobName,
                    webView.createPrintDocumentAdapter(jobName),
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                )
            }.onFailure {
                Toast.makeText(webView.context, "چاپ انجام نشد؛ دوباره تلاش کنید.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @JavascriptInterface
    fun close() {
        webView.post { onDismiss() }
    }

    @JavascriptInterface
    fun toast(message: String?) {
        if (message.isNullOrBlank()) return
        webView.post { Toast.makeText(webView.context, message, Toast.LENGTH_LONG).show() }
    }
}
