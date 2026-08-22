package ir.exam.app.ui.math

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

/**
 * ویرایشگر فرمول ریاضی داخل WebView.
 *
 * فایل asset `math_editor_standalone.html` نسخهٔ تک‌فایلی ویرایشگر است
 * (فرمول، V34، و لایهٔ میزبان همگی در همان فایل جاسازی شده‌اند) و در یک
 * WebView ایزوله اجرا می‌شود. این Composable فقط کارهای زیر را انجام
 * می‌دهد:
 *
 *  - متن `$…$` (فرمول فعلی یا جفت خالی `$$` برای درج جدید) را در
 *    textarea پنهان `qTxt_1` قرار داده و کل آن را انتخاب می‌کند؛
 *  - `openMath('qTxt_1')` را صدا می‌زند تا ویرایشگر باز شود؛
 *  - `mfApply` و `closeMath` را wrap می‌کند تا پل اندروید در لحظهٔ
 *    ثبت/بسته‌شدن مقدار نهایی را دریافت کند؛
 *  - بستن دیالوگ Compose را با یک timeout تضمین می‌کند تا اگر JS به
 *    هر دلیلی onClosed را صدا نزد، پنجرهٔ ایجاد آزمون در حالت نیمه‌باز
 *    گیر نکند.
 *
 * خود فایل HTML (از V45.8 به بعد، با نام «formula-editor-window»)
 * بایت‌به‌بایت به‌عنوان asset بسته‌بندی می‌شود و این پل تنها نقطهٔ
 * تماس اندروید با آن است.
 */
@Composable
fun MathEditorWebViewDialog(
    initialTex: String,
    onDismiss: () -> Unit,
    onInsert: (String) -> Unit,
) {
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val settled = remember { AtomicBoolean(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val dismissOnce = remember {
        object {
            fun fire() {
                if (settled.compareAndSet(false, true)) {
                    onDismiss()
                }
            }
        }
    }

    val settledBridge = remember(settled) {
        MathEditorJsBridge(
            settled = settled,
            onResult = { raw ->
                val tex = unwrapFormula(raw)
                if (tex.isBlank()) dismissOnce.fire() else onInsert(tex)
            },
            onClosed = { dismissOnce.fire() },
            onDiagnostic = { msg -> android.util.Log.i("MathEditorWebView", msg) }
        )
    }

    val bootstrap = remember(initialTex) { bootstrapScript(initialTex) }

    Dialog(
        onDismissRequest = {
            val wb = webViewRef.value
            if (wb != null && ready) {
                runCatching { wb.evaluateJavascript(CLOSE_MATH_JS, null) }
                mainHandler.postDelayed({ dismissOnce.fire() }, DISMISS_FALLBACK_MS)
            } else {
                dismissOnce.fire()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val wb = WebView(ctx)
                        webViewRef.value = wb
                        configureWebView(wb, settledBridge)
                        wb
                    },
                    update = {}
                )
                if (!ready && !failed) {
                    LoadingOverlay()
                } else if (failed) {
                    FailedOverlay(
                        onRetry = {
                            failed = false
                            ready = false
                            attempt++
                            webViewRef.value?.reload()
                        }
                    )
                }
            }
        }
    }

    DisposableEffect(webViewRef.value, attempt) {
        val wb = webViewRef.value ?: return@DisposableEffect onDispose {}
        val poller = MathEditorPoller(
            webView = wb,
            onReady = {
                ready = true
                wb.evaluateJavascript(bootstrap, null)
            },
            onFailed = { failed = true }
        )
        poller.start()
        onDispose {
            poller.cancel()
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
            webViewRef.value?.let { wb ->
                runCatching { wb.removeJavascriptInterface("AndroidMathBridge") }
                runCatching { wb.destroy() }
            }
            webViewRef.value = null
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(wb: WebView, bridge: MathEditorJsBridge) {
    with(wb.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        allowFileAccess = true
        allowContentAccess = false
        javaScriptCanOpenWindowsAutomatically = false
        setSupportZoom(false)
        loadWithOverviewMode = true
        useWideViewPort = true
    }
    wb.setBackgroundColor(0xFF0F0C29.toInt())
    wb.addJavascriptInterface(bridge, "AndroidMathBridge")
    wb.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean = true
    }
    wb.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
            val m = consoleMessage ?: return false
            android.util.Log.i(
                "MathEditorWebView",
                "JS[${m.messageLevel()}] ${m.sourceId()}:${m.lineNumber()} - ${m.message()}"
            )
            return true
        }
    }
    wb.loadUrl(ASSET_URL)
}

/** چرخهٔ انتظار برای آماده‌شدن توابع ویرایشگر در صفحهٔ وب. */
private class MathEditorPoller(
    private val webView: WebView,
    private val onReady: () -> Unit,
    private val onFailed: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var count = 0
    private var cancelled = false

    fun start() = tick()

    fun cancel() {
        cancelled = true
        handler.removeCallbacksAndMessages(null)
    }

    private fun tick() {
        if (cancelled) return
        count++
        webView.evaluateJavascript(READY_CHECK_JS) { value ->
            if (cancelled) return@evaluateJavascript
            if (value == "true") {
                onReady()
            } else if (count >= MAX_TRIES) {
                onFailed()
            } else {
                handler.postDelayed({ tick() }, POLL_MS)
            }
        }
    }

    private companion object {
        const val POLL_MS = 80L
        const val MAX_TRIES = 100
    }
}

/**
 * مقابل اندرویدِ صفحهٔ ویرایشگر. همهٔ فراخوانی‌ها از رشتهٔ غیر UI می‌آیند و به
 * رشتهٔ اصلی منتقل می‌شوند؛ فقط اولین رویداد (نتیجه یا بستن) اعمال می‌شود.
 */
private class MathEditorJsBridge(
    private val settled: AtomicBoolean,
    private val onResult: (String) -> Unit,
    private val onClosed: () -> Unit,
    private val onDiagnostic: (String) -> Unit
) {
    @JavascriptInterface
    fun onApplyResult(value: String?) {
        val raw = value ?: ""
        Handler(Looper.getMainLooper()).post {
            if (!settled.compareAndSet(false, true)) return@post
            onResult(raw)
        }
    }

    @JavascriptInterface
    fun onClosed() {
        Handler(Looper.getMainLooper()).post {
            if (!settled.compareAndSet(false, true)) return@post
            onClosed()
        }
    }

    /** لاگ تشخیصی از درون صفحه برای مشاهده در logcat. */
    @JavascriptInterface
    fun log(msg: String?) {
        val text = msg ?: "(null)"
        Handler(Looper.getMainLooper()).post { onDiagnostic(text) }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FailedOverlay(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ویرایشگر فرمول بارگیری نشد.")
                Button(onClick = onRetry, modifier = Modifier.padding(top: 12.dp)) {
                    Text("تلاش مجدد")
                }
            }
        }
    }
}

/**
 * CSS تزریقی برای رفع مشکلات رندر در WebViewهای قدیمی.
 *
 * فایل ویرایشگر از `100dvh` برای ارتفاع جعبهٔ تمام‌صفحه استفاده می‌کند که
 * در Chrome/WebView قبل از نسخهٔ ۱۰۸ ناشناخته است. راه‌حل استاندارد
 * «مقدار قدیمی قبل از مقدار مدرن»: اول `100vh` و بعد `100dvh` با
 * `!important`. مرورگر جدید دومی را می‌پذیرد، قدیمی اولی را نگه می‌دارد.
 *
 * همچنین والد مدال به‌جای `inset:0` با چهار ضلع صفر پین می‌شود و بدنهٔ
 * دموی پشت جعبه هنگام باز بودن مدال پنهان می‌شود.
 */
private const val VIEWPORT_FALLBACK_JS = """
(function(){
  try{
    var css = '' +
      '#mfModal{top:0 !important;right:0 !important;bottom:0 !important;left:0 !important;height:100vh !important;height:100dvh !important;}' +
      '#mfModal.box-fullscreen .mf-box{height:100vh !important;max-height:none !important;height:100dvh !important;}' +
      '#mfModal.box-fullscreen #mfP_box{height:100% !important;}' +
      '#mfModal.box-fullscreen #mfPad.mb-library-open .mb-library-panel{max-height:84vh !important;max-height:min(84dvh,720px) !important;}' +
      '#mfModal.box-fullscreen #mfPad.mb-smart-hub .mb-smart-shell{max-height:84vh !important;max-height:min(84dvh,720px) !important;}' +
      'body.math-open .demo-wrap{display:none !important;}';
    var s = document.createElement('style');
    s.id = 'mbAndroidViewportFallback';
    s.textContent = css;
    (document.head || document.documentElement).appendChild(s);
  }catch(e){}
})();
"""

/**
 * فرمول را در textarea پنهان seed می‌کند، `mfApply`/`closeMath` را به پل
 * اندروید وصل می‌کند، و سپس `openMath('qTxt_1')` را اجرا می‌نماید.
 * کتابخانهٔ V34 و همهٔ لایه‌های میزبان از پیش داخل asset تعبیه شده‌اند و
 * نیازی به تزریق جاوااسکریپت اضافی نیست.
 */
private fun bootstrapScript(initialTex: String): String {
    val wrapped = "\$" + initialTex + "\$"
    val valueLiteral = JSONObject.quote(wrapped)
    val selEnd = wrapped.length
    return VIEWPORT_FALLBACK_JS.trimIndent() + "\n" + """
      (function(){
        function log(m){ try{ AndroidMathBridge.log(String(m)); }catch(_e){} }
        try{
          if (window.__mbAndroidInstalled) return;

          var m = document.getElementById('qTxt_1');
          if (!m) { log('qTxt_1 missing'); AndroidMathBridge.onClosed(); return; }
          m.value = $valueLiteral;
          try { m.setSelectionRange(0, $selEnd); } catch (eSel) {}

          // لایهٔ میزبان داخل asset، window.mfApply و window.closeMath را
          // پیش از ما wrap کرده است (برای ذخیرهٔ فیلد، history، keypad و…).
          // ما فقط آن‌ها را یک‌لایهٔ نازک دیگر می‌پیچیم تا مقدار را به
          // اندروید برسانیم؛ زنجیرهٔ wrapها به‌درستی کار می‌کند چون هر
          // یک تابع قبلی را ذخیره می‌کند.
          var ia = window.mfApply;
          window.mfApply = function(){
            window.__mbApplyInFlight = true;
            try { ia.apply(window, arguments); } catch (e1) { log('mfApply wrap: ' + e1); }
            var mm = document.getElementById('qTxt_1');
            var v = (mm && mm.value != null) ? String(mm.value) : '';
            AndroidMathBridge.onApplyResult(v);
          };
          var ic = window.closeMath;
          window.closeMath = function(){
            try { ic.apply(window, arguments); } catch (e2) { log('closeMath wrap: ' + e2); }
            if (window.__mbApplyInFlight) { window.__mbApplyInFlight = false; return; }
            AndroidMathBridge.onClosed();
          };
          window.__mbAndroidInstalled = true;

          try { window.openMath('qTxt_1'); }
          catch (eOpen) { log('openMath threw: ' + eOpen); }

          // تضمین رندر: اگر openMath در WebView خاصی کلاس‌ها را نگذاشت
          // (مثلاً استثنا در initMathEdit)، خودمان می‌گذاریم. اگر درست
          // کار کرده باشد این فراخوانی‌ها بی‌اثرند.
          try {
            var modal = document.getElementById('mfModal');
            if (modal) {
              modal.classList.add('modal', 'open', 'box-fullscreen');
              modal.style.display = 'flex';
            }
            document.body.classList.add('math-open');
          } catch (eForce) { log('force open: ' + eForce); }

          log('bootstrap done; modal classes=' + (modal && modal.className));
        } catch (e) {
          log('bootstrap fatal: ' + e);
          try { AndroidMathBridge.onClosed(); } catch (e3) {}
        }
      })();
    """.trimIndent()
}

/** `$…$` را باز می‌کند؛ بدون پوشش، همان متن برمی‌گردد. */
private fun unwrapFormula(raw: String): String {
    val v = raw.trim()
    if (v.length >= 2 && v.startsWith('\$') && v.endsWith('\$')) {
        return v.substring(1, v.length - 1)
    }
    return v
}

private const val ASSET_URL = "file:///android_asset/math_editor_standalone.html"

private const val READY_CHECK_JS =
    "(function(){try{return !!window.openMath && !!window.mfApply && !!window.closeMath && !!document.getElementById('qTxt_1');}catch(e){return false;}})()"

private const val CLOSE_MATH_JS =
    "try{if(window.closeMath)window.closeMath();}catch(e){}"

/**
 * اگر پل JS پس از این مدت onClosed را صدا نزند، خودمان دیالوگ را می‌بندیم.
 */
private const val DISMISS_FALLBACK_MS = 250L
