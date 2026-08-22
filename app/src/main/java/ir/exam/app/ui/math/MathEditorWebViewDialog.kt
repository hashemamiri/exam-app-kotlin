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
      '#mfModal{position:fixed !important;top:0 !important;right:0 !important;bottom:0 !important;left:0 !important;width:100% !important;height:100% !important;display:flex !important;}' +
      '#mfModal.box-fullscreen{padding:0 !important;align-items:stretch !important;background:#0f0c29 !important;transform:none !important;will-change:auto !important;}' +
      '#mfModal.box-fullscreen .mf-box{position:absolute !important;top:0 !important;right:0 !important;bottom:0 !important;left:0 !important;width:100% !important;height:100% !important;max-width:none !important;max-height:none !important;margin:0 !important;padding:0 !important;border:0 !important;border-radius:0 !important;background:#0f0c29 !important;box-shadow:none !important;overflow:hidden !important;transform:none !important;will-change:auto !important;contain:none !important;}' +
      '#mfModal.box-fullscreen #mfP_box{display:flex !important;flex-direction:column !important;position:absolute !important;top:0 !important;right:0 !important;bottom:0 !important;left:0 !important;width:100% !important;height:100% !important;min-height:0 !important;overflow:hidden !important;}' +
      '#mfModal.box-fullscreen .mb-wrap{flex:1 1 auto !important;min-height:0 !important;margin:0 !important;padding:14px !important;display:flex !important;overflow:hidden !important;background:#0f0c29 !important;}' +
      '#mfModal.box-fullscreen .mb-canvas{flex:1 1 auto !important;width:100% !important;min-height:0 !important;height:auto !important;display:flex !important;align-items:flex-start !important;justify-content:flex-start !important;overflow:auto !important;}' +
      '#mfModal.box-fullscreen .mb-chip-scroll{flex:0 0 auto !important;display:grid !important;grid-template-rows:repeat(2,54px) !important;grid-auto-flow:column !important;grid-auto-columns:max-content !important;gap:10px 12px !important;overflow-x:auto !important;overflow-y:hidden !important;padding:12px 16px !important;background:#24243e !important;}' +
      '#mfModal.box-fullscreen .mb-key-section{flex:0 0 auto !important;display:flex !important;flex-direction:column !important;background:#302b63 !important;}' +
      '#mfModal.box-fullscreen .mb-fixed-keypad{display:grid !important;grid-template-columns:repeat(6,1fr) !important;gap:6px !important;padding:6px 8px 8px !important;background:transparent !important;}' +
      '#mfModal.box-fullscreen .card-header,#mfModal.box-fullscreen .mf-modes,#mfModal.box-fullscreen .mf-help,#mfModal.box-fullscreen .mb-tools,#mfModal.box-fullscreen .mb-quick,#mfModal.box-fullscreen .mb-symbol-search,#mfModal.box-fullscreen .mf-code,#mfModal.box-fullscreen .mf-act{display:none !important;}' +
      '#mfModal.box-fullscreen #mfPad:not(.mb-library-open):not(.mb-smart-hub){display:none !important;}' +
      '#mfModal.box-fullscreen #mfPad.mb-library-open,#mfModal.box-fullscreen #mfPad.mb-smart-hub{display:flex !important;position:fixed !important;top:0 !important;right:0 !important;bottom:0 !important;left:0 !important;width:100vw !important;height:100vh !important;max-height:none !important;z-index:12040 !important;overflow:hidden !important;padding:16px !important;background:rgba(0,0,0,.55) !important;align-items:center !important;justify-content:center !important;}' +
      '#mfModal.box-fullscreen #mfPad.mb-library-open .mb-library-panel,#mfModal.box-fullscreen #mfPad.mb-smart-hub .mb-library-panel{max-height:84vh !important;max-height:min(84dvh,720px) !important;}' +
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

        // فیکس چیدمان با ابعاد واقعی viewport — به‌صورت گلوبال تعریف
        // می‌شود تا هم همین‌جا و هم در صورت نیاز در زمان‌های دیگر
        // قابل فراخوانی باشد.
        window.__mbForceLayout = function(){
          try{
            var h = (window.innerHeight || document.documentElement.clientHeight || 0) + 'px';
            var w = (window.innerWidth || document.documentElement.clientWidth || 0) + 'px';
            function set(id, vals){
              var el = document.getElementById(id);
              if (!el) return;
              for (var k in vals) { try { el.style[k] = vals[k]; } catch(_e){} }
            }
            set('mfModal', {position:'fixed',top:'0',left:'0',right:'0',bottom:'0',width:w,height:h,display:'flex',padding:'0',margin:'0',zIndex:'2147483646'});
            var box = document.querySelector('#mfModal .mf-box');
            if (box) {
              box.style.position='absolute'; box.style.top='0'; box.style.left='0';
              box.style.right='0'; box.style.bottom='0';
              box.style.width='100%'; box.style.height=h;
              box.style.maxWidth='none'; box.style.maxHeight='none';
              box.style.margin='0'; box.style.padding='0';
              box.style.borderRadius='0'; box.style.border='0'; box.style.overflow='hidden';
            }
            set('mfP_box', {position:'absolute',top:'0',left:'0',right:'0',bottom:'0',width:'100%',height:h,display:'flex',flexDirection:'column',minHeight:'0',overflow:'hidden',margin:'0',padding:'0'});
            if (typeof window.mbDraw === 'function') {
              try { window.mbDraw(); } catch (eD) {}
            }
          }catch(e){ log('force layout: ' + e); }
        };

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
            // اگر سرکوب فعال است (مثلاً در لحظهٔ فشردن بازگشت، قبل از
            // آن که Compose دیالوگ را کاملاً ببندد)، فقط پل را صدا می‌زنیم
            // ولی کلاس‌های مدال را حذف نمی‌کنیم تا UI میانی موبایلی
            // نمایش داده نشود.
            if (window.__mbSuppressClose) {
              AndroidMathBridge.onClosed();
              return;
            }
            try { ic.apply(window, arguments); } catch (e2) { log('closeMath wrap: ' + e2); }
            if (window.__mbApplyInFlight) { window.__mbApplyInFlight = false; return; }
            AndroidMathBridge.onClosed();
          };
          window.__mbAndroidInstalled = true;

          // قبل از هر چیز، اطمینان حاصل کنیم که مدال در حالت تمام‌صفحه
          // باز می‌شود. در برخی وب‌ویوها بین زمان فراخوانی openMath و
          // اعمال CSS یک حالت میانی موبایلی (پایین‌صفحه) دیده می‌شد.
          try {
            var modalPre = document.getElementById('mfModal');
            if (modalPre) {
              modalPre.classList.add('modal', 'open', 'box-fullscreen');
              modalPre.style.display = 'flex';
            }
            document.body.classList.add('math-open');
          } catch(ePre){}

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
              // جلوگیری از حالت میانی: تا زمانی که دیالوگ Compose باز
              // است، هیچ‌کس نتواند کلاس box-fullscreen را از مدال بردارد.
              if (window.MutationObserver && !modal.__mbFsLock) {
                modal.__mbFsLock = true;
                new MutationObserver(function(){
                  if (!modal.classList.contains('box-fullscreen')) {
                    modal.classList.add('box-fullscreen');
                    modal.style.display = 'flex';
                  }
                  if (!document.body.classList.contains('math-open')) {
                    document.body.classList.add('math-open');
                  }
                }).observe(modal, {attributes:true, attributeFilter:['class','style']});
              }
            }
            document.body.classList.add('math-open');
            // wrap داخلی closeMath نباید کلاس‌ها را تا زمان بسته شدن
            // واقعی حذف کند (در غیر این صورت همان لحظهٔ بین فشردن
            // بازگشت و بسته شدن Compose، UI میانی موبایلی دیده می‌شود).
            // تابع فعلی window.closeMath از قبل wrap ماست؛ ما در همان
            // ابتدا ic (نسخهٔ داخلی) را یادداشت کرده‌ایم؛ اینجا فقط
            // اطمینان می‌دهیم که حذف کلاس به‌تعویق بیفتد.
            window.__mbSuppressClose = true;
            window.__mbForceLayout();
            setTimeout(window.__mbForceLayout, 100);
            setTimeout(window.__mbForceLayout, 400);
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
