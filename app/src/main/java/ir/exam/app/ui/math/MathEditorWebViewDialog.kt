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
            // مهم: نباید منتظر پل JS بمانیم. در برخی WebViewها closeMath یا
            // پل به‌دلیل استثنا/استاکینگ کانتکست اجرا نمی‌شد و دیالوگ Compose
            // باز می‌ماند. مستقیماً بستن Compose را صدا می‌زنیم؛ cleanup جاوا
            // اسکریپت به‌صورت best-effort پشت صحنه اجرا می‌شود و WebView
            // هنگام خروج از Composition به‌هرحال destroy خواهد شد.
            val wb = webViewRef.value
            if (wb != null) {
                runCatching { wb.evaluateJavascript(CLOSE_MATH_JS, null) }
            }
            dismissOnce.fire()
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
                Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                    Text("تلاش مجدد")
                }
            }
        }
    }
}

/**
 * CSS تزریقی برای رفع مشکلات رندر در WebViewهای قدیمی.
 *
 * به‌جای بازنویسی گستردهٔ چیدمان (که در نسخه‌های قبلی تداخل ایجاد
 * می‌کرد)، فقط حداقل‌های لازم را تزریق می‌کنیم:
 *  - برای `100dvh` یک `100vh` قبل از آن می‌گذاریم؛
 *  - برای `inset:0` چهارضلع صفر می‌گذاریم؛
 *  - والد .modal از `position:fixed` و چهارضلع صفر استفاده می‌کند تا
 *    کل ویوپورت را بپوشاند؛
 *  - وقتی کتابخانه یا اسمارت‌هاب باز می‌شود، #mfPad باید display:flex
 *    بگیرد (قانون پیش‌فرض asset این را دارد ولی با !important اضافه
 *    می‌کنیم تا هیچ قانون مخفی‌کننده دیگری نتواند آن را پنهان کند)؛
 *  - منوی پاپ‌آپ mbVar در مرکز صفحه پین می‌شود (در برخی WebViewها
 *    اندازه‌گیری اولیه صفر بود و منو به گوشه می‌رفت)؛
 *  - بدنهٔ دموی پشت مدال هنگام باز بودن فرمول پنهان می‌شود.
 */
private const val VIEWPORT_FALLBACK_JS = """
(function(){
  try{
    var css = '' +
      'html,body{margin:0 !important;padding:0 !important;height:100% !important;}' +
      '#mfModal{position:fixed !important;top:0 !important;right:0 !important;bottom:0 !important;left:0 !important;display:none !important;z-index:2147483645 !important;}' +
      '#mfModal.modal.open{display:flex !important;}' +
      '#mfModal.box-fullscreen{padding:0 !important;align-items:stretch !important;background:var(--bg1) !important;}' +
      '#mfModal.box-fullscreen .mf-box{height:100vh !important;height:100dvh !important;max-height:none !important;max-width:none !important;width:100% !important;margin:0 !important;border:0 !important;border-radius:0 !important;}' +
      '#mbVar.mb-var{display:none !important;position:fixed !important;top:50% !important;left:50% !important;right:auto !important;bottom:auto !important;transform:translate(-50%,-50%) !important;width:min(340px,92vw) !important;max-width:92vw !important;max-height:80vh !important;min-width:240px !important;z-index:2147483646 !important;overflow:auto !important;}' +
      '#mbVar.mb-var.open{display:block !important;}' +
      'body.math-open .demo-wrap{display:none !important;}';
    var s = document.getElementById('mbAndroidViewportFallback');
    if (!s) { s = document.createElement('style'); s.id = 'mbAndroidViewportFallback'; (document.head||document.documentElement).appendChild(s); }
    s.textContent = css;
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
          // پیش از ما wrap کرده است. ما فقط یک‌لایهٔ نازک اضافه می‌کنیم تا
          // مقدار را به اندروید برسانیم.
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

          // ---- تضمین باز شدن کتابخانه‌ها (بدون بازنویسی چیدمان) ----
          // host-bridge داخل asset چیپ‌های V34 را با w.eval بایند می‌کند
          // که در برخی WebViewهای اندرویدی به‌خاطر تفاوت محدودهٔ
          // const/let سطح‌بالا ممکن است بی‌اثر باشد. ما مستقیم و بدون
          // eval می‌بندیم تا کلیک همیشه روی mbGroupLibrary برسد. ضمناً
          // پاپ‌آپ mbVar را پس از هر باز شدن در مرکز صفحه می‌نشانیم
          // چون اندازه‌گیری اولیهٔ offsetWidth در برخی WebViewها صفر بود.
          function centerPop(){
            try {
              var p = document.getElementById('mbVar');
              if (!p || !p.classList.contains('open')) return;
              var vw = window.innerWidth || document.documentElement.clientWidth || 0;
              var vh = window.innerHeight || document.documentElement.clientHeight || 0;
              p.style.position = 'fixed';
              p.style.top = '50%';
              p.style.left = '50%';
              p.style.right = 'auto';
              p.style.bottom = 'auto';
              p.style.transform = 'translate(-50%,-50%)';
              p.style.zIndex = '2147483646';
              p.style.maxHeight = Math.round(vh*0.85) + 'px';
              p.style.width = Math.min(360, Math.round(vw*0.92)) + 'px';
              p.style.display = 'block';
            } catch(_e) {}
          }
          if (typeof window.mbVarOpen === 'function' && !window.__mbVarOpenWrapped) {
            window.__mbVarOpenWrapped = true;
            var __innerMbVarOpen = window.mbVarOpen;
            window.mbVarOpen = function(){
              try {
                var r = __innerMbVarOpen.apply(this, arguments);
                setTimeout(centerPop, 0);
                setTimeout(centerPop, 50);
                setTimeout(centerPop, 200);
                return r;
              } catch (eOpen) { log('mbVarOpen wrap: ' + eOpen); }
            };
          }
          function bindV34Chips(){
            try {
              var scroll = document.querySelector('.mb-chip-scroll');
              if (!scroll) return;
              var chips = scroll.querySelectorAll('.mb-chip[data-v34="1"]');
              for (var i=0;i<chips.length;i++) {(function(chip){
                if (chip.__mbBound) return; chip.__mbBound = true;
                chip.addEventListener('click', function(ev){
                  try { ev.stopPropagation(); ev.preventDefault(); } catch(_e){}
                  var txt = chip.textContent || '';
                  var key = txt.indexOf('کتاب') >= 0 ? 'school'
                          : txt.indexOf('تزئین') >= 0 ? 'type'
                          : txt.indexOf('زیست') >= 0 ? 'bio' : null;
                  if (key && typeof window.mbGroupLibrary === 'function') {
                    try { window.mbGroupLibrary(key); } catch (eGL){ log('mbGroupLibrary: '+eGL); }
                    setTimeout(centerPop, 0);
                    setTimeout(centerPop, 50);
                    setTimeout(centerPop, 200);
                  }
                }, true);
              })(chips[i]);}
            } catch (eBind) { log('bindV34Chips: ' + eBind); }
          }
          bindV34Chips();
          setTimeout(bindV34Chips, 200);
          setTimeout(bindV34Chips, 600);

          // openMath باید پس از نصب همه‌چیز صدا زده شود.
          try { window.openMath('qTxt_1'); }
          catch (eOpen) { log('openMath threw: ' + eOpen); }

          log('bootstrap done');
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
