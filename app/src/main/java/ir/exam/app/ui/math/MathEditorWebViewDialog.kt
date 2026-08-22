package ir.exam.app.ui.math

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
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
 * این دیالوگ تمام صفحهٔ ویرایشگر فرمول مستقل (استخراج کد به کد از نسخهٔ وب
 * 66.html — asset/math_editor_standalone.html، بایت‌به‌بایت بدون تغییر) را
 * در یک WebView ایزوله اجرا می‌کند. صفحهٔ وب با پروتکل زیر هدایت می‌شود:
 *
 *  - متن `$…$` (فرمول فعلی یا جفت خالی `$$` برای درج جدید) در textarea پنهان
 *    `qTxt_1` قرار داده می‌شود و کل آن انتخاب می‌شود؛
 *  - `openMath('qTxt_1')` ویرایشگر را باز می‌کند؛
 *  - `mfApply` (دکمهٔ ثبت) کل انتخاب را با `$فرمول جدید$` جایگزین می‌کند و
 *    سپس خودش `closeMath()` را صدا می‌زند؛
 *  - Bridge اندروید مقدار نهایی textarea را از `mfApply` می‌گیرد، `$…$`
 *    را باز می‌کند و نتیجه را به `onInsert` می‌دهد؛ بستن بدون ثبت به
 *    `onDismiss` می‌رسد.
 *
 * هیچ تغییری در محتوای صفحهٔ ویرایشگر داده نمی‌شود؛ فقط تابع‌های `mfApply` و
 * `closeMath` در لحظهٔ انتها wrap می‌شوند (همان الگوی bridge میزبان در 66.html).
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

    val settledBridge = remember(settled) {
        MathEditorJsBridge(
            settled = settled,
            onResult = { raw ->
                val tex = unwrapFormula(raw)
                if (tex.isBlank()) onDismiss() else onInsert(tex)
            },
            onClosed = { onDismiss() }
        )
    }

    val bootstrap = remember(initialTex) { bootstrapScript(initialTex) }

    Dialog(
        onDismissRequest = {
            val wb = webViewRef.value
            if (wb != null && ready) {
                wb.evaluateJavascript(CLOSE_MATH_JS, null)
            } else {
                onDismiss()
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

    // پس از ساخت WebView، منتظر آماده‌شدن صفحهٔ ویرایشگر می‌شویم.
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
        onDispose { poller.cancel() }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef.value?.let { wb ->
                runCatching { wb.removeJavascriptInterface("AndroidMathBridge") }
                runCatching { wb.destroy() }
            }
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
    wb.setBackgroundColor(0xFFEEF2F7.toInt())
    wb.addJavascriptInterface(bridge, "AndroidMathBridge")
    wb.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean = true
    }
    wb.loadUrl(ASSET_URL)
}

/** چرخهٔ انتظار برای آماده‌شدن توابع ویرایشگر در صفحهٔ وب (الگوی poll میزبان). */
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
    private val onClosed: () -> Unit
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
                Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                    Text("تلاش مجدد")
                }
            }
        }
    }
}

/** فرمول فعلی (یا `$$` برای درج جدید) را در متن ویرایشگر قرار می‌دهد و باز می‌کند. */
private fun bootstrapScript(initialTex: String): String {
    val wrapped = "\$" + initialTex + "\$"
    val valueLiteral = JSONObject.quote(wrapped)
    val selEnd = wrapped.length
    return """
      (function(){
        try{
          if (window.__mbAndroidInstalled) return;
          var m = document.getElementById('qTxt_1');
          if (!m) { AndroidMathBridge.onClosed(); return; }
          m.value = $valueLiteral;
          try { m.setSelectionRange(0, $selEnd); } catch (e) {}
          var ia = window.mfApply;
          window.mfApply = function(){
            window.__mbApplyInFlight = true;
            try { ia.apply(window, arguments); } catch (e1) {}
            var mm = document.getElementById('qTxt_1');
            var v = (mm && mm.value != null) ? String(mm.value) : '';
            AndroidMathBridge.onApplyResult(v);
          };
          var ic = window.closeMath;
          window.closeMath = function(){
            try { ic.apply(window, arguments); } catch (e2) {}
            if (window.__mbApplyInFlight) { window.__mbApplyInFlight = false; return; }
            AndroidMathBridge.onClosed();
          };
          window.__mbAndroidInstalled = true;
          window.openMath('qTxt_1');
        } catch (e) {
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
