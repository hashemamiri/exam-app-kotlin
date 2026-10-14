package ir.exam.app.ui.math

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
@Composable
fun FormulaHostDialog(
    initialText: String,
    selectionStart: Int,
    selectionEnd: Int,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    var latestText by remember { mutableStateOf(initialText) }
    var loading by remember { mutableStateOf(true) }
    // V54.5 — خطای واقعی JS (پاک‌سازی‌شده در asset؛ بدون URL/Token) برای نمایش امن.
    var jsError by remember { mutableStateOf<String?>(null) }
    // V217 — حالت تیرهٔ برنامه ⇒ ویرایشگر فرمول هم با تم تیرهٔ خودش باز می‌شود (پوستهٔ روشن میزبان خاموش).
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shellColor = if (darkTheme) ComposeColor(0xFF0F0C29) else ComposeColor(0xFFE9EEF5)

    Dialog(
        onDismissRequest = { onResult(latestText); onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        // پس‌زمینه همان رنگ صفحهٔ مرجع تا هیچ فریم سفید/ناهماهنگی دیده نشود.
        Surface(Modifier.fillMaxSize(), color = shellColor) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        // V216 — WebView «گرم» (از قبل ساخته و parse شده) یا در نبودش، نمونهٔ تازه.
                        val view = FormulaEditorPool.acquire(context)
                        view.setBackgroundColor(shellColor.toArgb())
                        FormulaEditorPool.session = FormulaEditorPool.Session(
                            onText = { latestText = it },
                            onJsError = { message -> view.post { jsError = message; loading = false } },
                            // V55 — فایل مستقل رویداد صریح onEditorClosed دارد؛
                            // بستن (✕ یا درج فرمول) متن نهایی را برمی‌گرداند.
                            onClosed = { view.post { onResult(latestText); onDismiss() } }
                        )
                        // V55.1/V216 — تا تعریف‌شدن پل (صفحهٔ گرم: همان لحظه)، begin هر 150ms تکرار می‌شود.
                        val text = JSONObject.quote(initialText)
                        var attempts = 0
                        var recreated = false
                        var evalSent = 0L // V288 — زمان آخرین evaluateJavascript بی‌پاسخ
                        var active: WebView = view
                        var tryBeginRef: (() -> Unit)? = null
                        fun recreate(reason: String) {
                            if (recreated) { jsError = reason; loading = false; return }
                            recreated = true
                            attempts = 0
                            val host = active.parent as? ViewGroup
                            val fresh = FormulaEditorPool.replace(active, context)
                            fresh.setBackgroundColor(shellColor.toArgb())
                            host?.addView(fresh, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                            active = fresh
                            fresh.postDelayed({ tryBeginRef?.invoke() }, 300)
                        }
                        FormulaEditorPool.onRenderGone = { gone -> if (gone === active) active.post { recreate("RENDER_PROCESS_GONE") } }
                        val tryBegin = fun() {
                            val view = active
                            if (FormulaEditorPool.current !== view) return
                            attempts++
                            if (!FormulaEditorPool.isReady(view)) {
                                if (attempts < 200) view.postDelayed({ tryBeginRef?.invoke() }, 150) else recreate("PAGE_NOT_LOADED after ${attempts} tries")
                                return
                            }
                            evalSent = android.os.SystemClock.uptimeMillis()
                            view.evaluateJavascript(
                                "(function(){window.__mfPersianDigits=" + ir.exam.app.core.figure.FigureDigits.persian + ";if(window.ExamFormulaHost){" + // V287 — اعداد فارسی صفحه‌کلید فرمول از تنظیمات
                                    (if (darkTheme) "if(window.__mathHostTheme){__mathHostTheme.off();}" else "") +
                                    "ExamFormulaHost.begin($text, $selectionStart, $selectionEnd);return 'ok';}return 'wait';})();"
                            ) { result ->
                                evalSent = 0L
                                when {
                                    result?.contains("ok") == true -> loading = false
                                    attempts < 200 -> view.postDelayed({ tryBeginRef?.invoke() }, 150) // V288 — تا ۳۰ ثانیه
                                    // V55.2/V288 — پل هرگز تعریف نشد: یک‌بار نمونهٔ تازه، سپس خطای صریح.
                                    else -> recreate("BRIDGE_NOT_READY after $attempts tries")
                                }
                            }
                        }
                        // V288 — نگهبان: اگر evaluateJavascript هرگز پاسخ نداد (رندر مرده)، پس از ۱۲ ثانیه نمونهٔ تازه
                        var watchdogRef: Runnable? = null
                        val watchdog = Runnable {
                            if (!loading || jsError != null || FormulaEditorPool.current !== active) return@Runnable
                            if (evalSent != 0L && android.os.SystemClock.uptimeMillis() - evalSent > 8000) recreate("NO_JS_RESPONSE")
                            else active.postDelayed(watchdogRef, 3000)
                        }
                        watchdogRef = watchdog
                        tryBeginRef = tryBegin
                        view.postDelayed(watchdog, 3000)
                        tryBegin()
                        // V288 — ظرف میزبان تا بتوان نمونهٔ تازه را جایگزین کرد
                        android.widget.FrameLayout(context).apply {
                            addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        }
                    },
                    onRelease = { host ->
                        FormulaEditorPool.onRenderGone = null
                        // V216 — به‌جای destroy، به استخر برمی‌گردد و در پس‌زمینه برای دفعهٔ بعد بازنشانی می‌شود.
                        val view = (0 until host.childCount).map { host.getChildAt(it) }.filterIsInstance<WebView>().firstOrNull()
                        if (view != null) FormulaEditorPool.recycle(view)
                    }
                )
                if (loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                jsError?.let { message ->
                    Text(
                        "خطای ویرایشگر: $message",
                        color = ComposeColor(0xFFB3261E),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * V216 — استخر WebView ویرایشگر فرمول (سرعت: کاربر «ویرایشگر فرمول خیلی کند است، مخصوصاً در اپ»).
 * اندازه‌گیری در Chromium: تعامل داخل ویرایشگر long task ندارد؛ هزینهٔ اصلی هر باز شدن، ساخت WebView
 * و parse فایل ۱٫۸MB `formula.html` بود (~۱ ثانیه با CPU ۴× کندتر؛ روی گوشی بیشتر). راه‌حل: یک WebView
 * از قبل ساخته و بارگذاری شده (پس از شروع برنامه، در بیکاری) که هر بار فقط به Dialog وصل می‌شود؛ پس از
 * بستن، همان نمونه در پس‌زمینه دوباره بارگذاری می‌شود تا برای دفعهٔ بعد تازه و آماده باشد.
 * قاعدهٔ WebView فقط در این فایل (Neumorphic69IntegrationTest) رعایت شده است.
 */
@SuppressLint("SetJavaScriptEnabled")
object FormulaEditorPool {
    class Session(
        val onText: (String) -> Unit,
        val onJsError: (String) -> Unit,
        val onClosed: () -> Unit
    )

    private const val EDITOR_URL = "https://exam-editor.local/formula-editor/formula.html"

    /** جلسهٔ فعال Dialog؛ رویدادهای JS فقط به آن می‌رسند (در حالت گرم/پارک‌شده null). */
    @Volatile
    var session: Session? = null

    /** WebView در اختیار Dialog فعلی (برای توقف پولینگ begin پس از بستن). */
    @Volatile
    var current: WebView? = null
        private set

    private var parked: WebView? = null

    /* V288 — علت‌های «گاهی ویرایشگر فرمول باز نمی‌شود» در اپ:
       ۱) WebView بازیافت‌شده هنوز در حال بارگذاری مجدد صفحه بود؛ begin روی سند قبلی «ok» می‌داد و سپس سند نو جای آن را می‌گرفت
          (پنجرهٔ خالی) → پرچم آماده‌بودن سند و انتظار تا onPageFinished.
       ۲) اندروید فرایند رندر WebView پارک‌شده را در کمبود حافظه می‌کشت؛ evaluateJavascript هرگز پاسخ نمی‌داد (دایرهٔ انتظار بی‌پایان)
          → onRenderProcessGone: نمونه دور ریخته می‌شود و Dialog نمونهٔ تازه می‌سازد.
       ۳) روی گوشی کند، parse فایل ۲MB از ۱۰ ثانیه می‌گذشت و «BRIDGE_NOT_READY» می‌داد → مهلت ۳۰ ثانیه و یک‌بار ساخت نمونهٔ تازه. */
    private val ready = java.util.WeakHashMap<WebView, Boolean>()

    /** آیا سندِ فعلیِ این WebView کاملاً بارگذاری شده است؟ */
    fun isReady(view: WebView): Boolean = ready[view] == true

    /** رویداد مرگ فرایند رندر؛ Dialog با آن نمونهٔ تازه می‌سازد. */
    @Volatile
    var onRenderGone: ((WebView) -> Unit)? = null

    /** از MainActivity پس از شروع (در بیکاری) صدا زده می‌شود؛ ساخت WebView، Chromium را هم گرم می‌کند. */
    fun prepare(context: Context) {
        if (parked != null || current != null) return
        parked = create(context)
    }

    fun acquire(context: Context): WebView {
        val view = parked?.takeIf { it.context === context || it.context.applicationContext === context.applicationContext }
            ?: create(context)
        parked = null
        (view.parent as? ViewGroup)?.removeView(view)
        view.onResume()
        current = view
        return view
    }

    fun recycle(view: WebView) {
        session = null
        if (current === view) current = null
        (view.parent as? ViewGroup)?.removeView(view)
        if (parked != null) { destroy(view); return }
        // بازنشانی وضعیت صفحه برای جلسهٔ بعد (parse در پس‌زمینه، نه هنگام باز شدن)
        view.stopLoading()
        ready[view] = false
        view.loadUrl(EDITOR_URL)
        parked = view
    }

    /** V288 — نمونهٔ خراب (رندر مرده/پل آماده نشده) را دور می‌ریزد و نمونهٔ تازه برمی‌گرداند. */
    fun replace(view: WebView, context: Context): WebView {
        if (current === view) current = null
        if (parked === view) parked = null
        (view.parent as? ViewGroup)?.removeView(view)
        destroy(view)
        val fresh = create(context)
        current = fresh
        return fresh
    }

    /** آزادسازی کامل (کمبود حافظه یا پایان Activity). */
    fun release() {
        parked?.let { destroy(it) }
        parked = null
    }

    private fun destroy(view: WebView) {
        runCatching {
            view.stopLoading()
            view.loadUrl("about:blank")
            view.removeAllViews()
            view.destroy()
        }
    }

    private fun create(context: Context): WebView = WebView(context).apply {
        // V55.3 — پس‌زمینهٔ «مات» به‌جای شفاف: WebView شفاف + backdrop-filter
        // فایل مرجع روی برخی دستگاه‌ها لایهٔ کامپوزیت خالی می‌سازد (مودال باز
        // ولی محتوا paint نمی‌شود — همان «صفحهٔ سفید» N55.2). رنگ همان --bg1 است.
        setBackgroundColor(Color.parseColor("#E9EEF5"))
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // این صفحات کاملاً از asset محلی می‌آیند؛ cache دیسک WebView
        // فقط IO و نگهداری دادهٔ تکراری ایجاد می‌کند.
        settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = false
        settings.setSupportZoom(false)
        addJavascriptInterface(
            FormulaHostBridge(
                onText = { text -> session?.onText?.invoke(text) },
                onJsError = { message -> session?.onJsError?.invoke(message) },
                onClosed = { session?.onClosed?.invoke() }
            ),
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
                // V54.4/V55 — فقط asset محلی ویرایشگر فرمول؛ بقیه پاسخ خالی امن.
                if (!path.startsWith("/formula-editor/")) return emptyResponse()
                val assetPath = path.removePrefix("/formula-editor/")
                if (assetPath.isBlank() || assetPath.contains("..")) return emptyResponse()
                return try {
                    val stream = view.context.assets.open("formula_editor/$assetPath")
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
                ready[view] = true // V288
                // V216 — نمونهٔ پارک‌شده پس از پایان بارگذاری، تایمرها/انیمیشن‌هایش متوقف می‌شود (بدون مصرف CPU در پس‌زمینه).
                if (parked === view) view.onPause()
            }
            override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                // V288 — فرایند رندر کشته شد: نمونه بی‌مصرف است؛ از استخر خارج و (اگر در Dialog است) جایگزین می‌شود.
                ready[view] = false
                if (parked === view) { parked = null; destroy(view) }
                else onRenderGone?.invoke(view)
                return true
            }
        }
        // V54.5 — WebChromeClient خطاهای console را امن گزارش می‌کند؛
        // نبودن آن، خطاهای boot ویرایشگر را بی‌صدا گم می‌کرد.
        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                if (message.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                    val safe = message.message().replace(Regex("https?://\\S+"), "[url]").take(300)
                    session?.onJsError?.invoke("CONSOLE: $safe")
                }
                return true
            }
        }
        // V55 — پنجرهٔ فرمول فایل مستقل formula.html است (پاک‌سازی V74.0:
        // asset قدیمی question_editor حذف شد). auto-open مرجع خودش پنجره را باز می‌کند.
        loadUrl(EDITOR_URL)
    }
}

private class FormulaHostBridge(
    private val onText: (String) -> Unit,
    private val onJsError: (String) -> Unit,
    private val onClosed: () -> Unit
) {
    @JavascriptInterface
    fun onTextChanged(value: String?) { onText(value.orEmpty()) }

    /** V55 — فایل مستقل فرمول پس از هر بستن (✕/درج) این رویداد را می‌فرستد. */
    @JavascriptInterface
    fun onEditorClosed() { onClosed() }

    @JavascriptInterface
    fun onOverlayChanged(open: Boolean) = Unit

    @JavascriptInterface
    fun onReady() = Unit

    @JavascriptInterface
    fun onError(code: String?) {
        code?.takeIf { it.isNotBlank() }?.let(onJsError)
    }
}
