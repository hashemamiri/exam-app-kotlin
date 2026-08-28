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
import androidx.compose.runtime.key
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

    /** V65.0 — مسیر Native Compose برای درج/جایگزینی/فرمول بدون WebView. */
    internal var nativeInsert: ((String) -> Boolean)? = null
    internal var nativeReplace: ((String) -> Boolean)? = null
    internal var nativeOpenFormula: (() -> Boolean)? = null
    var pendingEditOccurrence: Int? = null

    /** آخرین متنی که خود WebView گزارش کرده یا برایش push شده؛ برای جلوگیری از echo. */
    var lastJsValue: String = ""
        internal set

    /** V55.10 — آیا محتوای کادر داخل خودش اسکرول دارد؟ (گزارش از HTML). */
    @Volatile
    var innerScrollable: Boolean = false
        internal set

    /** بازکردن ابزارهای مرجع داخل WebView: formula / anatomy / periodic / physics / chemistry. */
    fun openTool(name: String): Boolean {
        if (name == "formula") {
            nativeOpenFormula?.invoke()?.let { return it }
        }
        val view = webView ?: return false
        val quoted = JSONObject.quote(name)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.openTool($quoted);", null)
        return true
    }

    /** درج توکن `%%FIG:{json}%%` ساخته‌شده در ویرایشگرهای Native در محل مکان‌نما. */
    fun insertFigureJson(specJson: String): Boolean {
        nativeInsert?.invoke(specJson)?.let { return it }
        val view = webView ?: return false
        val quoted = JSONObject.quote(specJson)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.insertToken($quoted);", null)
        return true
    }

    /** V53.3 — جایگزینی توکن در حال ویرایش (dblclick) با خروجی ویرایشگر Native. */
    fun applyEditedFigureJson(specJson: String): Boolean {
        nativeReplace?.invoke(specJson)?.let { return it }
        val view = webView ?: return false
        val quoted = JSONObject.quote(specJson)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.applyEditedToken($quoted);", null)
        return true
    }

    /** V53.3 — انصراف از ویرایش توکن dblclick. */
    fun cancelEditFigure() {
        webView?.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.cancelEditToken();", null)
    }

    /** V54.4 — بستن لایه‌های تمام‌صفحهٔ مرجع (ابزارها) با دکمهٔ بازگشت سیستم. */
    fun closeOverlays() {
        webView?.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.closeOverlays();", null)
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
    onOpenFormula: (text: String, selStart: Int, selEnd: Int) -> Unit = { _, _, _ -> },
    onError: (String) -> Unit = {},
    // V55.7 — ارتفاع واقعی محتوا (px CSS) از HTML گزارش می‌شود تا کادر با درج
    // فرمول/شکل کشیده شود و اسکرول به صفحهٔ اصلی برنامه منتقل شود.
    onContentHeight: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    DisposableEffect(controller) {
        onDispose { controller.webView = null }
    }
    // V55.9 — گزارش دستگاه: «متن سؤال ۱ در کادر همهٔ سؤال‌ها ظاهر می‌شود».
    // در LazyColumn، بازیافت composition می‌تواند AndroidView را با closureهای
    // factory قدیمی (bridge/initialValue سؤال قبلی) برای سؤال دیگری نگه دارد.
    // key(controller) تضمین می‌کند با تغییر سؤال (controller هر سؤال یکتاست)
    // WebView قبلی کاملاً دور انداخته و factory از نو با bindings همان سؤال
    // اجرا شود؛ هیچ WebViewی بین سؤال‌ها مشترک نمی‌ماند.
    key(controller) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // V55.10 — گزارش دستگاه: «کادر متن سؤال اسکرول‌پذیر نیست». WebView داخل
            // LazyColumn است و لیست، ژست عمودی را می‌قاپد؛ وقتی محتوای کادر واقعاً
            // اسکرول دارد (پرچم از HTML)، هنگام لمس از والد می‌خواهیم دخالت نکند.
            object : WebView(context) {
                override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
                    if (controller.innerScrollable) {
                        when (event.actionMasked) {
                            android.view.MotionEvent.ACTION_DOWN ->
                                parent?.requestDisallowInterceptTouchEvent(true)
                            android.view.MotionEvent.ACTION_UP,
                            android.view.MotionEvent.ACTION_CANCEL ->
                                parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    return super.onTouchEvent(event)
                }
            }.apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                            // این صفحات کاملاً از asset محلی می‌آیند؛ cache دیسک WebView
                            // فقط IO و نگهداری دادهٔ تکراری ایجاد می‌کند.
                            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.setSupportZoom(false)
                addJavascriptInterface(
                    FieldBridge(controller, onValueChanged, onOverlayChanged, onEditFigureToken, onOpenFormula, onError, onContentHeight),
                    "ExamEditorNative"
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        // V54.5 — فقط ناوبری خارجی «صفحهٔ اصلی» مسدود می‌شود. WebView برخلاف
                        // مرورگر دسکتاپ، ناوبری داخلی iframe ویرایشگر فرمول (about:blank /
                        // document.open) را هم از این مسیر عبور می‌دهد؛ true برگرداندن برای آن،
                        // boot ویرایشگر مرجع را بی‌صدا می‌شکست.
                        if (!request.isForMainFrame) return false
                        val url = request.url
                        val isLocal = url.host == "exam-editor.local" || url.scheme == "about"
                        return !isLocal
                    }
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val path = request.url.path ?: return emptyResponse()
                        // V54.4 — هر مسیر خارج از asset محلی (مثل favicon خودکار)
                        // پاسخ خالی امن می‌گیرد تا WebView سراغ شبکهٔ ناموجود نرود.
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
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                        // V54.4 — فقط شکست «صفحهٔ اصلی» خطای واقعی است؛ خطای
                        // subresourceهای فرعی (مثل favicon خودکار مرورگر روی دامنهٔ
                        // محلی بدون DNS) پیام کاذب «بارگیری نشد» می‌ساخت.
                        if (request.isForMainFrame) onError("EDITOR_LOAD_FAILED")
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
        },
        onRelease = { view ->
            controller.webView = null
            view.stopLoading()
            view.loadUrl("about:blank")
            view.removeAllViews()
            view.destroy()
        }
    )
    }
}

private class FieldBridge(
    private val controller: QuestionEditorFieldController,
    private val onValueChanged: (String) -> Unit,
    private val onOverlayChanged: (Boolean) -> Unit,
    private val onEditFigureToken: (String) -> Unit,
    private val onOpenFormula: (String, Int, Int) -> Unit,
    private val onError: (String) -> Unit,
    private val onContentHeight: (Int) -> Unit
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

    /** V53.4 — درخواست بازکردن پنجرهٔ تمام‌صفحهٔ فرمول با متن و محدودهٔ انتخاب. */
    @JavascriptInterface
    fun onOpenFormula(text: String?, selStart: Int, selEnd: Int) {
        onOpenFormula.invoke(text.orEmpty(), selStart, selEnd)
    }

    /** V55.7 — ارتفاع واقعی محتوای صفحه (px CSS)؛ Compose ارتفاع کادر را هماهنگ می‌کند. */
    @JavascriptInterface
    fun onContentHeight(height: Int) {
        if (height in 41..20000) onContentHeight.invoke(height)
    }

    /** V55.10 — وضعیت اسکرول داخلی کادر؛ برای آزادسازی ژست لمس از لیست والد. */
    @JavascriptInterface
    fun onScrollableChanged(scrollable: Boolean) {
        controller.innerScrollable = scrollable
    }

    @JavascriptInterface
    fun onReady() = Unit

    @JavascriptInterface
    fun onError(code: String?) { onError.invoke(code ?: "EDITOR_ERROR") }
}
