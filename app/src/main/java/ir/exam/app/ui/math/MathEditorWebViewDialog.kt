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
import androidx.compose.ui.platform.LocalContext
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
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // اطمینان از اینکه onDismiss دقیقاً یک‌بار و حتماً اجرا می‌شود، حتی اگر
    // JS پل به هر دلیلی (استثنا، خطای صفحه، WebView قدیمی) فراخوانی نشود.
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
            onClosed = { dismissOnce.fire() }
        )
    }

    val appContext = LocalContext.current.applicationContext
    val v34Source = remember { FormulaV34Library.load(appContext) }
    val bootstrap = remember(initialTex, v34Source) {
        bootstrapScript(initialTex, v34Source)
    }

    Dialog(
        onDismissRequest = {
            val wb = webViewRef.value
            if (wb != null && ready) {
                // به JS فرصت می‌دهیم closeMath را کامل کند و پل onClosed
                // را صدا بزند؛ اگر تا DISMISS_FALLBACK_MS پل پاسخ نداد
                // (مثلاً JS یک استثنا در مسیر بستن انداخت یا closeMath
                // در WebView قدیمی آن‌طور که انتظار می‌رود عمل نکرد)،
                // خودمان دیالوگ را می‌بندیم تا پنجرهٔ ایجاد آزمون
                // هرگز در حالت «بهم‌ریخته» گیر نکند.
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
    // رنگ پس‌زمینه تیره با تم ویرایشگر (--bg1) هماهنگ است تا اگر لحظه‌ای
    // بین لود شدن HTML و آماده شدن، دیالوگ دیده شد، فلاش سفید نزند.
    wb.setBackgroundColor(0xFF0F0C29.toInt())
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

/**
 * تزریق CSS در لحظه برای رفع مشکلات رندر در WebViewهای قدیمی.
 *
 *  - `100dvh` در Chrome/WebView قبل از نسخهٔ ۱۰۸ ناشناخته است و چون تنها
 *    مقدار ارتفاع جعبهٔ تمام‌صفحه است، جعبه به ارتفاع صفر می‌افتد و
 *    «صفحهٔ خالی» دیده می‌شود. راه‌حل استاندارد «مقدار قدیمی قبل از
 *    مقدار مدرن» است: اول `100vh` و بعد `100dvh` با `!important`.
 *    مرورگر جدید مقدار دوم را می‌پذیرد، قدیمی مقدار اول را نگه می‌دارد.
 *  - والد `.modal` نیز به‌جای اتکا به `inset:0` (که در WebViewهای
 *    قدیمی هم ممکن است پشتیبانی نشود) با top/right/bottom/left صفر
 *    پین می‌شود و ارتفاعش به 100vh/100dvh می‌رود تا کل صفحه را بپوشاند.
 *  - بدنهٔ دموی صفحه (`.demo-wrap`) هنگام باز بودن مدال مخفی می‌شود تا
 *    اگر جعبه‌به‌دلیلی کوتاه رندر شد، محتوای دموی پشت‌صحنه دیده نشود.
 *  - چند قانون max-height با تابع `min(84dvh, …)` نیز fallback می‌گیرند.
 *
 * این CSS به <head> تزریق می‌شود و asset اصلی دست‌نخورده باقی می‌ماند.
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
 * فرمول فعلی (یا `$$` برای درج جدید) را seed می‌کند، کتابخانهٔ V34 را
 * پیش از openMath تزریق می‌نماید، و mfApply/closeMath را به پل اندروید وصل
 * می‌کند. [v34Source] محتوای خام فایل `formula/install_lib_v34.js` است که
 * در سطح @Composable از [FormulaV34Library.load] گرفته شده.
 */
private fun bootstrapScript(initialTex: String, v34Source: String): String {
    val wrapped = "\$" + initialTex + "\$"
    val valueLiteral = JSONObject.quote(wrapped)
    val selEnd = wrapped.length
    return VIEWPORT_FALLBACK_JS.trimIndent() + "\n" + """
      (function(){
        try{
          if (window.__mbAndroidInstalled) return;
          /* ---- V34 library (school/type/bio + curricular extensions) ----
             Byte-for-byte body of installLibV34 from 66.html, served from
             app/src/main/assets/formula/install_lib_v34.js. The asset function
             is idempotent (guards on w.__libV34). In the Kotlin WebView the
             asset is evaluated once per page load via indirect eval so the
             function declaration lands in the global lexical scope where the
             editor's top-level MB_PAD/MB_GROUPS live. We then reach it through
             window.installLibV34 (the asset assigns nothing on window by
             itself, so we bind it ourselves). If the asset is missing, the
             editor falls back to the base library. */
          if (!window.__mbV34Installed) {
            $v34Source
            try {
              if (typeof installLibV34 === 'function') {
                window.installLibV34 = installLibV34;
                installLibV34(window);
              }
            } catch (eLib) {}
            window.__mbV34Installed = true;
          }
          /* -------------------------------------------------------------- */
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

/**
 * اگر پل JS در این مدت پس از فشردن دکمهٔ بازگشت onClosed را صدا نزند،
 * خودمان دیالوگ را می‌بندیم. این عدد به‌قدری بزرگ است که اجرای طبیعی
 * closeMath چندین بار فرصت اتمام داشته باشد، و به‌قدری کوچک که کاربر
 * مکث محسوسی نبیند.
 */
private const val DISMISS_FALLBACK_MS = 250L
