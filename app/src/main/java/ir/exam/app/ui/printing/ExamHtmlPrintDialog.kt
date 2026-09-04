package ir.exam.app.ui.printing

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * V76.0 — پنجرهٔ تمام‌صفحهٔ «نسخهٔ 30» (آزمون‌ساز/چاپ تعاملی HTML):
 * فایل print/exam_print.html را در WebView بارگذاری می‌کند؛ با پل
 * window.setExamData سؤالات و سربرگ آزمون خودکار تزریق می‌شوند و کاربر همان‌جا
 * ویرایش/چاپ می‌کند (فقط چاپ؛ آزمون سرور تغییر نمی‌کند).
 * printable == null یعنی «آزمون جدید» — فایل با payload ریست خالی باز می‌شود.
 * V76.1 — viewport خود فایل اعمال می‌شود (رابط موبایل در اندازهٔ واقعی) و انتخاب
 * تصویر با دکمهٔ دوربینِ فایل از طریق onShowFileChooser پشتیبانی می‌شود.
 * V76.3 — هفت کنترل اصلی (تنظیمات سربرگ، ذخیره، بازکردن، چاپ دانشجو/استاد،
 * سوال جدید، پیش‌نمایش) به نوار فرمان بومی این پنجره منتقل شده‌اند؛ نوار HTML
 * فایل مخفی است و فرمان‌ها از طریق evaluateJavascript به صفحه اعمال می‌شوند.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExamHtmlPrintDialog(
    printable: OfficialExamPrintable?,
    onDismiss: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var jsError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    // V76.3 — ارجاع WebView برای فرمان‌های نوار بومی + پیام وضعیت
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var barStatus by remember { mutableStateOf<String?>(null) }
    val runJs: (String, ((String?) -> Unit)?) -> Unit = { script, cb ->
        webViewRef?.evaluateJavascript(script, cb)
    }
    // V76.1 — دکمهٔ دوربینِ فایل (📷) یک input[type=file] داینامیک را کلیک می‌کند؛
    // WebView اندروید بدون onShowFileChooser آن را بی‌صدا نادیده می‌گیرد.
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val callback = fileChooserCallback
        fileChooserCallback = null
        callback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
    }
    // V76.3 — «بازکردن آزمون»: فایل JSON با انتخاب‌گر بومی خوانده و با پل setExamData وارد صفحه می‌شود.
    val openExamPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        if (!text.trimStart().startsWith("{")) {
            barStatus = "فایل انتخاب‌شده آزمون نیست (فایل JSON لازم است)."
            return@rememberLauncherForActivityResult
        }
        val b64 = android.util.Base64.encodeToString(text.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        runJs("(function(){try{window.setExamData(atob('" + b64 + "'));return 'ok'}catch(e){return 'err'}})()") { r ->
            barStatus = if (r?.contains("ok") == true) "آزمون باز شد ✓" else "باز کردن آزمون ناموفق بود."
        }
    }

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
                        text = if (printable == null) "آزمون جدید — آزمون‌ساز"
                        else "چاپ آزمون: " + printable.documentTitle.ifBlank { printable.subject }.ifBlank { "آزمون" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // V76.3 — نوار فرمان بومی: هفت کنترل اصلی (نوار HTML فایل مخفی شده است).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    NativeBarButton("⚙ تنظیمات سربرگ") { runJs("if (typeof toggleSettings==='function') toggleSettings();", null) }
                    NativeBarButton("💾 ذخیره") {
                        runJs(
                            "(function(){try{return window.__qmfSaveNow?window.__qmfSaveNow():'missing'}catch(e){return 'err'}})()"
                        ) { r -> barStatus = if (r?.contains("ok") == true) "ذخیره شد ✓" else "ذخیره نشد!" }
                    }
                    NativeBarButton("📂 بازکردن") { openExamPicker.launch("*/*") }
                    NativeBarButton("🖨 چاپ دانشجو") { runJs("if (typeof printStudent==='function') printStudent();", null) }
                    NativeBarButton("✅ چاپ استاد") { runJs("if (typeof printTeacher==='function') printTeacher();", null) }
                    NativeBarButton("➕ سوال جدید") { runJs("if (typeof openQuestionTypePicker==='function') openQuestionTypePicker();", null) }
                    NativeBarButton("👁 پیش‌نمایش") { runJs("if (typeof togglePreviewWindow==='function') togglePreviewWindow();", null) }
                }

                Box(Modifier.fillMaxSize().weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).also { webViewRef = it }.apply {
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
                                // V76.2 — useWideViewPort متاوویوپورتِ فایل (width=device-width)
                                // را اعمال می‌کند؛ اما overviewMode باید خاموش بماند وگرنه WebView
                                // برای محتوای عریضِ A4 (۷۳۳px) کل صفحه را zoom-out می‌کند و همه
                                // پنجره‌ها/دکمه‌ها ریز می‌شوند (ریشهٔ «پنجره‌ها کوچک است»).
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = false

                                addJavascriptInterface(
                                    ExamPrintBridge(
                                        onPrint = { mode ->
                                            post {
                                                runCatching {
                                                    val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                                    val jobName = (printable?.documentTitle ?: "آزمون").ifBlank { "exam" } + "-$mode"
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

                                    // V76.1 — انتخاب تصویر برای دکمهٔ دوربین (📷) سؤال
                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallback: ValueCallback<Array<Uri>>,
                                        fileChooserParams: android.webkit.WebChromeClient.FileChooserParams
                                    ): Boolean {
                                        fileChooserCallback = filePathCallback
                                        imagePicker.launch("image/*")
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

                    barStatus?.let { message ->
                        Text(
                            message,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color(0xCC1E3A8A))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeBarButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
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
