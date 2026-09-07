package ir.exam.app.ui.printing

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.core.figure.AtlasCatalog
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.GRAPH_FIGURES
import ir.exam.app.domain.model.OfficialExamPrintable
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * V76.0 — پنجرهٔ تمام‌صفحهٔ «نسخهٔ 30» (آزمون‌ساز/چاپ تعاملی HTML):
 * فایل print/exam_print.html را در WebView بارگذاری می‌کند؛ با پل
 * window.setExamData سؤالات و سربرگ آزمون خودکار تزریق می‌شوند و کاربر همان‌جا
 * ویرایش/چاپ می‌کند (فقط چاپ؛ آزمون سرور تغییر نمی‌کند).
 * V76.1 — viewport خود فایل اعمال می‌شود (رابط موبایل در اندازهٔ واقعی).
 * V99.2 — `onFigLayouts` چیدمانِ اشیاء/جداکنندهٔ ساخته‌شده در پیش‌نمایش را به
 * میزبانِ بومی برمی‌گرداند تا در بازِ بعدی (و چاپ) ریست نشود.
 *
 * V100 — «آزمون‌ساز چاپی» کامل حذف شد. این پنجره حالا فقط دو حالت دارد:
 *  - `initialPreview = true` — پیش‌نمایشِ بومی از آزمون‌ساز (دکمهٔ چشم):
 *    برگهٔ A4 با ویرایشِ شکل‌ها با لمسِ دوباره؛ بستنِ پنجرهٔ پیش‌نمایش
 *    پنجرهٔ کل را می‌بندد و چیدمان به بیلدر برمی‌گردد.
 *  - `initialPrintMode != null` — چاپِ مستقیم (student/teacher): برگهٔ خالصِ
 *    A4 روی پس‌زمینهٔ خاکستری و سپس پنجرهٔ چاپِ اندروید.
 * همهٔ امکاناتِ حالتِ ویرایش (کارت‌های بومی، منوی رادیال، ذخیره/بازکردن
 * JSON، بازیابی، مدیریت سؤال، ویرایشگر فرمول، استودیوی تصویر، هدر «سربرگ»،
 * تشخیص‌های فنی) با حذفِ آزمون‌سازِ چاپی از این پنجره برداشته شدند؛ ویرایش
 * آزمون در بیلدرِ بومی انجام می‌شود.
 * V101 — راه‌اندازیِ WebView به تابعِ مشترکِ `createExamPrintWebView` منتقل
 * شد تا «چاپِ مستقیمِ بدون‌صفحه» (HeadlessExamPrinter) هم از همان موتور
 * استفاده کند؛ در آن مسیر هیچ پنجرهٔ پیش‌نمایشی روی صفحه ظاهر نمی‌شود.
 */

/**
 * V80.0 — نشانیِ سندِ اصلیِ آزمون‌ساز. onPageFinished برای هر فریم (از جمله
 * iframe ویرایشگر فرمول) صدا زده می‌شود، پس باید بتوانیم فریمِ اصلی را تشخیص دهیم.
 */
internal const val MAIN_PAGE_URL = "https://exam-print.local/print/exam_print.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExamHtmlPrintDialog(
    printable: OfficialExamPrintable?,
    initialPreview: Boolean = false,
    initialPrintMode: String? = null,
    onDismiss: () -> Unit,
    /**
     * V99.2 — چیدمانِ اشیاء/جداکننده که کاربر در پنجرهٔ پیش‌نمایش ساخته
     * (JSON: شمارهٔ سؤال → {figLayouts, sepExtraPx}). بیلدرِ بومی آن را به
     * وضعیتِ خود می‌نویسد تا موقعیت‌ها در بازِ بعدی ریست نشوند.
     */
    onFigLayouts: ((String) -> Unit)? = null
) {
    var loading by remember { mutableStateOf(true) }
    var jsError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    // V76.3 — ارجاع WebView برای فرمان‌های نوار بومی + پیام وضعیت
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    // V78.0 — درخواستِ بازکردنِ یک ابزار درجِ بومی از داخل صفحه
    var figureTool by remember { mutableStateOf<FigureToolRequest?>(null) }
    // V82.0 — دابل‌کلیک: (questionId, tokenIndex) تا spec از صفحه خوانده شود.
    var figureEditRequest by remember { mutableStateOf<Pair<String, Int>?>(null) }
    // V87.7 — پیام پس از چند ثانیه خودش محو می‌شود
    var barStatus by remember { mutableStateOf<String?>(null) }
    /* V89.5 — فهرستِ کارت‌ها `fillMaxSize` است و روی WebView می‌نشیند، پس
       پنجرهٔ پیش‌نمایش (که داخلِ WebView باز می‌شود) زیرش پنهان می‌ماند.
       بدونِ سؤال، فهرست خالی بود و مشکل دیده نمی‌شد؛ با سؤال، چشم «کار
       نمی‌کرد». هنگامِ باز بودنِ پیش‌نمایش کارت‌ها کنار می‌روند. */
    var previewOpen by remember { mutableStateOf(initialPreview) }
    LaunchedEffect(barStatus) {
        if (barStatus != null) {
            kotlinx.coroutines.delay(2600)
            barStatus = null
        }
    }

    val runJs: (String, ((String?) -> Unit)?) -> Unit = { script, cb ->
        webViewRef?.evaluateJavascript(script, cb)
    }

    /* V99.2 — چیدمانِ اشیاء (موقعیت/شناور/slot + جداکننده) را از صفحه بخوان
       و به میزبانِ بومی بده تا در بازِ بعدیِ پنجره ریست نشود. */
    var lastFigLayoutsJson by remember { mutableStateOf<String?>(null) }

    fun fetchFigLayoutsSnapshot() {
        webViewRef?.evaluateJavascript(
            "(function(){try{return window.__qmfFigLayoutsSnapshot?window.__qmfFigLayoutsSnapshot():'{}'}catch(e){return '{}'}})()"
        ) { raw ->
            val json = unwrapJsString(raw).ifBlank { "{}" }
            if (json != "{}" && json != lastFigLayoutsJson) {
                lastFigLayoutsJson = json
                onFigLayouts?.invoke(json)
            }
        }
    }

    fun requestDismiss() {
        fetchFigLayoutsSnapshot()
        onDismiss()
    }

    LaunchedEffect(previewOpen, loading) {
        if (!loading && !previewOpen && initialPreview) {
            requestDismiss()
        }
    }

    // V82.0 — دابل‌کلیک روی ابزارِ درج‌شده: spec و محدودهٔ توکن را از صفحه
    // بخوان و همان پنجرهٔ بومی را در حالتِ ویرایش باز کن.
    LaunchedEffect(figureEditRequest) {
        val (qid, index) = figureEditRequest ?: return@LaunchedEffect
        runJs(
            "(function(){try{return window.__qmfEditFigAt?" +
                "window.__qmfEditFigAt('" + qid + "'," + index + "):''}catch(e){return ''}})()"
        ) { raw ->
            figureEditRequest = null
            val payload = unwrapJsString(raw)
            if (payload.isBlank()) {
                barStatus = "ویرایش این مورد ممکن نبود."
                return@runJs
            }
            val parsed = runCatching {
                val o = kotlinx.serialization.json.Json.parseToJsonElement(payload).jsonObject
                Triple(
                    o["spec"]?.jsonPrimitive?.content.orEmpty(),
                    o["start"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1,
                    o["end"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
                )
            }.getOrNull()
            val specJson = parsed?.first
            if (specJson.isNullOrBlank() || parsed.second < 0 || parsed.third <= parsed.second) {
                barStatus = "ویرایش این مورد ممکن نبود."
                return@runJs
            }
            val tool = toolOfSpec(specJson)
            if (tool == null) {
                barStatus = "این ابزار پنجرهٔ بومی ندارد."
                return@runJs
            }
            figureTool = FigureToolRequest(
                questionId = qid,
                tool = tool,
                editIndex = index,
                initialSpecJson = specJson,
                tokenStart = parsed.second,
                tokenEnd = parsed.third
            )
        }
    }

    Dialog(
        onDismissRequest = { requestDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF334155)) {
            Column(Modifier.fillMaxSize()) {
                // V102 — هدرِ پنجره (عنوان + بستن): پنجرهٔ پیش‌نمایش دیگر
                // «بدون هدر» نیست؛ کاربر عنوان برگه را می‌بیند و مستقیم
                // می‌تواند پنجره را ببندد (با ذخیرهٔ چیدمان اشیاء).
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        printable?.documentTitle ?: "پیش‌نمایش برگه",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { requestDismiss() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "بستن",
                            tint = Color.White
                        )
                    }
                }
                Box(Modifier.fillMaxSize().weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            // V101 — راه‌اندازیِ کامل در تابعِ مشترک (همان موتور
                            // چاپِ مستقیمِ بدون‌صفحه از اینجا استفاده می‌کند).
                            createExamPrintWebView(
                                context = ctx,
                                printable = printable,
                                printMode = initialPrintMode,
                                // تزریقِ داده کامل شد: فقط در حالتِ پیش‌نمایش
                                // پنجرهٔ پیش‌نمایش باز می‌شود؛ در چاپِ مستقیم
                                // خودِ تابع پیش از این فراخوانی، چاپ را شلیک کرده است.
                                onPageReady = {
                                    loading = false
                                    if (initialPreview) {
                                        previewOpen = true
                                        webViewRef?.evaluateJavascript(
                                            "(function(){try{return window.__qmfShowPreview?window.__qmfShowPreview():'missing'}catch(e){return 'err'}})()",
                                            null
                                        )
                                    }
                                },
                                onPrint = { mode ->
                                    webViewRef?.let { view ->
                                        view.post {
                                            runCatching {
                                                val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                                val jobName = (printable?.documentTitle ?: "آزمون").ifBlank { "exam" } + "-" + mode
                                                val printAdapter = view.createPrintDocumentAdapter(jobName)
                                                printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
                                            }
                                        }
                                    }
                                },
                                onError = { message ->
                                    // V101 — callback از تَرهٔ JS می‌آید؛ وضعیتِ Compose
                                    // باید روی تَرهٔ اصلی نوشته شود.
                                    webViewRef?.post { jsError = message; loading = false }
                                },
                                onToast = { message ->
                                    webViewRef?.post { if (message.isNotBlank()) barStatus = message }
                                },
                                onEditFigureTool = { qid, index -> webViewRef?.post { figureEditRequest = qid to index } },
                                onPreviewClosed = {
                                    fetchFigLayoutsSnapshot()
                                    webViewRef?.post { previewOpen = false }
                                }
                            ).also { webViewRef = it }
                        },
                        onRelease = { view ->
                            view.stopLoading()
                            view.loadUrl("about:blank")
                            view.removeAllViews()
                            view.destroy()
                        }
                    )

                    if (loading) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = Color.White)
                                Text(
                                    // V99.1 — در چاپِ مستقیم متنِ «آماده‌سازی چاپ»؛
                                    // برگهٔ A4 را پنجرهٔ چاپِ اندروید نمایش می‌دهد.
                                    if (initialPrintMode != null) "در حال آماده‌سازی چاپ..."
                                    else "در حال آماده‌سازی پیش‌نمایش برگه...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    jsError?.let { message ->
                        Text(
                            "خطای صفحه چاپ: $message",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                        )
                    }

                    /* V87.7 — پیام‌ها پایینِ صفحه می‌ماندند تا پیامِ بعدی
                       جایشان را بگیرد. حالا وسط ظاهر و پس از چند ثانیه محو
                       می‌شوند، مثلِ یک اعلانِ بومی.
                       V100e — با فرمِ کامل‌نام (همانندِ نسخهٔ قبل): فراخوانیِ
                       ساده AnimatedVisibility بارِ ColumnScope را برمی‌گزیند و
                       در این نقطه کامپایل نمی‌شود. */
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = barStatus != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                barStatus.orEmpty(),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xE6111827))
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            )
                        }
                    }
                }

                // V78.0 — ابزارهای درجِ بومی (جدول، تناوبی، شکل، نمودار، آناتومی،
                // فیزیک، شیمی). V82.0 — همین میزبان حالتِ «ویرایش» را هم دارد:
                // اگر req.isEdit باشد، نتیجه جایگزینِ همان توکن می‌شود نه درجِ تازه.
                // V100 — در حالتِ پیش‌نمایش، این همان مسیرِ «دابل‌کلیک روی
                // شکلِ برگه» است؛ درجِ تازه از این پنجره دیگر امکان‌پذیر نیست.
                figureTool?.takeIf { it.isNative }?.let { req ->
                    ExamFigureToolHost(
                        request = req,
                        onInsert = { token ->
                            figureTool = null
                            val b64 = android.util.Base64.encodeToString(
                                token.toByteArray(Charsets.UTF_8),
                                android.util.Base64.NO_WRAP
                            )
                            val script = if (req.isEdit) {
                                "(function(){try{return window.__qmfReplaceFigToken?" +
                                    "window.__qmfReplaceFigToken('" + req.questionId + "'," +
                                    req.tokenStart + "," + req.tokenEnd + ",'" + b64 + "'):'missing'}" +
                                    "catch(e){return 'err'}})()"
                            } else {
                                "(function(){try{return window.__qmfInsertFigToken?" +
                                    "window.__qmfInsertFigToken('" + req.questionId + "','" + b64 + "'):'missing'}" +
                                    "catch(e){return 'err'}})()"
                            }
                            runJs(script) { r ->
                                barStatus = if (r?.contains("ok") == true) {
                                    if (req.isEdit) "ویرایش شد ✓" else "در سؤال درج شد ✓"
                                } else {
                                    if (req.isEdit) "ویرایش ناموفق بود." else "درج در سؤال ناموفق بود."
                                }
                            }
                        },
                        onDismiss = { figureTool = null }
                    )
                }
            }
        }
    }
}

/** خروجی evaluateJavascript برای رشته‌ها JSON-کوت است؛ رشتهٔ واقعی را برمی‌گرداند. */
/**
 * V82.0 — از روی `k` و `t` داخلِ spec، ابزارِ متناظر را تشخیص می‌دهد تا
 * دابل‌کلیک همان پنجره‌ای را باز کند که موقع درج باز شده بود.
 * نگاشت مرجع: خالی=هندسه/نمودار، t=جدول، a=آناتومی، p=تناوبی، s=فیزیک/شیمی.
 */
internal fun toolOfSpec(specJson: String): String? {
    val spec = FigureSpec.parse(specJson) ?: return null
    return when (spec.kind) {
        "t" -> "table"
        "p" -> "periodic"
        "a" -> "anatomy"
        "s" -> if (AtlasCatalog.scienceDomain(spec.type) == "chem") "chemistry" else "physics"
        "g" -> "graph"
        "" -> if (GRAPH_FIGURES.any { it.id == spec.type }) "graph" else "figure"
        else -> null
    }
}

internal fun unwrapJsString(value: String?): String {
    val raw = value ?: return ""
    if (!raw.startsWith("\"")) return raw
    return runCatching {
        kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonPrimitive.content
    }.getOrDefault(raw)
}

/* V100 — jsArg (سازندهٔ لیترالِ امنِ JS) فقط در کارت‌های بومی مصرف داشت و
   با حذفِ «آزمون‌ساز چاپی» حذف شد. */

private class ExamPrintBridge(
    private val onPrint: (String) -> Unit,
    private val onError: (String) -> Unit,
    // V82.0 — دابل‌کلیک روی ابزارِ درج‌شده: ویرایشِ همان توکن
    private val onEditFigureTool: (String, Int) -> Unit,
    // V87.8 — پیام‌های صفحه به‌جای alert مرورگر، اعلانِ بومی می‌شوند
    private val onToast: (String) -> Unit,
    // V89.5 — بستنِ پنجرهٔ پیش‌نمایش
    private val onPreviewClosed: () -> Unit
) {
    /** V87.8 — `alert()` پنجرهٔ خام با نشانیِ exam-print.local نشان می‌داد. */
    @JavascriptInterface
    fun toast(message: String?) {
        onToast(message.orEmpty())
    }

    /** V89.5 — پیش‌نمایش بسته شد. */
    @JavascriptInterface
    fun previewClosed() {
        onPreviewClosed()
    }

    @JavascriptInterface
    fun print(mode: String?) {
        onPrint(mode ?: "student")
    }

    /**
     * V82.0 — دابل‌کلیک روی یک ابزارِ درج‌شده. `index` شمارهٔ ترتیبیِ توکن در
     * متنِ همان سؤال است؛ میزبانِ بومی spec را می‌خواند و همان پنجره را در
     * حالتِ ویرایش باز می‌کند، سپس نتیجه جایگزینِ همان توکن می‌شود.
     * (V100 — openFigureTool/openImageStudio/close با حذفِ «آزمون‌ساز چاپی»
     * برداشته شدند: این پل حالا فقط مسیرِ ویرایشِ دابل‌کلیکِ پیش‌نمایش
     * و چاپ/اعلان را دارد.)
     */
    @JavascriptInterface
    fun editFigureTool(questionId: String?, index: Int) {
        onEditFigureTool(questionId.orEmpty(), index)
    }

    @JavascriptInterface
    fun onError(code: String?) {
        code?.takeIf { it.isNotBlank() }?.let(onError)
    }
}


/**
 * V101 — راه‌اندازیِ مشترکِ WebViewِ چاپ (پنجرهٔ پیش‌نمایش + چاپِ مستقیمِ
 * بدون‌صفحه). تنظیمات، پلِ ExamPrintNative، перехبِرِ assetها و تزریقِ داده
 * از طریق setExamData همین‌جا است. در حالتِ `printMode` (چاپِ مستقیم) صفحه
 * به حالتِ چاپ می‌رود و خودِ صفحه printStudent/printTeacher را صدا می‌زند
 * (پس از آن پلِ print به PrintManager می‌رسد)؛ سپس `onPageReady` در رشتهٔ
 * اصلی فراخوانی می‌شود.
 */
@SuppressLint("SetJavaScriptEnabled")
internal fun createExamPrintWebView(
    context: Context,
    printable: OfficialExamPrintable?,
    printMode: String?,
    onPageReady: () -> Unit,
    onPrint: (String) -> Unit,
    onError: (String) -> Unit,
    onToast: (String) -> Unit,
    onEditFigureTool: (String, Int) -> Unit,
    onPreviewClosed: () -> Unit
): WebView = WebView(context).apply {
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
    // V76.2 — useWideViewPort متاوویوپورتِ فایل (width=device-width) را اعمال
    // می‌کند؛ اما overviewMode باید خاموش بماند وگرنه WebView برای محتوای عریضِ
    // A4 (۷۳px) کل صفحه را zoom-out می‌کند و همه پنجره‌ها/دکمه‌ها ریز می‌شوند
    // (ریشهٔ «پنجره‌ها کوچک است»).
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = false

    addJavascriptInterface(
        ExamPrintBridge(
            onPrint = onPrint,
            onError = onError,
            // V82.0 — ویرایشِ ابزارِ درج‌شده با دابل‌کلیک
            onEditFigureTool = onEditFigureTool,
            // V87.8 — همان اعلانِ وسط‌چینِ محوشونده
            onToast = onToast,
            // V89.5 — بستنِ پنجرهٔ پیش‌نمایش
            onPreviewClosed = onPreviewClosed
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
            // V87.1 — تصاویرِ اطلس از `print/` بیرون‌اند
            // (`figure_atlas/`، همان‌هایی که پنجرهٔ بومی می‌خواند).
            val assetPath = when {
                path.startsWith("/print/") -> "print/" + path.removePrefix("/print/")
                path.startsWith("/figure_atlas/") -> path.removePrefix("/")
                else -> return emptyResponse()
            }
            if (assetPath.isBlank() || assetPath.contains("..")) return emptyResponse()
            return try {
                val stream = view.context.assets.open(assetPath)
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
            // V80.0 — onPageFinished برای «هر فریم» صدا زده می‌شود، نه فقط
            // فریمِ اصلی. فقط به پایانِ لودِ سندِ اصلی واکنش نشان بده.
            if (url != MAIN_PAGE_URL) return
            val payload = ExamHtmlPrintPayloadBuilder.build(
                printable,
                // V86.8 — میدان‌های سربرگِ ذخیره‌شده روی دستگاه
                ir.exam.app.data.local.PrintHeaderStore(context).read()
            ).toString()
            var attempts = 0
            fun tryInject() {
                attempts++
                view.evaluateJavascript(
                    "(function(){if(window.setExamData){window.setExamData($payload);return 'ok';}return 'wait';}());"
                ) { result ->
                    when {
                        result?.contains("ok") == true -> {
                            // V99.1 — در چاپِ مستقیم (دانش‌آموز/پاسخ‌نامه) پنجرهٔ
                            // پیش‌نمایشِ HTML باز نمی‌شود: آن پنجرهٔ overlay هنگامِ
                            // چاپ پنهان می‌شود و چون محتوای چاپ داخلش منتقل شده،
                            // خروجی چاپ خالی می‌ماند.
                            if (printMode != null) {
                                view.evaluateJavascript(
                                    "try{document.body.classList.add('qmf-print-mode');}catch(e){}",
                                    null
                                )
                            }
                            if (printMode == "student") {
                                view.evaluateJavascript("if (typeof printStudent==='function') printStudent();", null)
                            } else if (printMode == "teacher") {
                                view.evaluateJavascript("if (typeof printTeacher==='function') printTeacher();", null)
                            }
                            view.post { onPageReady() }
                        }
                        attempts < 50 -> view.postDelayed({ tryInject() }, 100)
                        else -> view.post { onError("برگهٔ چاپ بارگذاری نشد.") }
                    }
                }
            }
            tryInject()
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
            if (message.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                val text = message.message()
                // V99.2d — پیام‌های بی‌ضرر: WebView هشدار «Ignored
                // attempt to cancel a: touchmove ...» را با سطحِ ERROR
                // می‌فرستد، ولی این فقط از handler هایِ pinch-zoom صفحه
                // هنگامِ اسکرول می‌آید و خطای واقعی نیست.
                if (text.contains("Ignored attempt to cancel")) return true
                val safe = text.replace(Regex("https?://\\S+"), "[url]").take(300)
                onError("CONSOLE: $safe")
            }
            return true
        }
    }

    loadUrl(MAIN_PAGE_URL)
}

/**
 * V101 — چاپِ مستقیمِ بدون‌صفحه (headless): WebView نمایش داده نمی‌شود
 * (الگوی رسمیِ Android: PrintHtmlOffScreen). صفحهٔ آزمون بارگذاری و تزریق
 * می‌شود، پنلِ چاپِ اندروید مستقیم روی صفحهٔ مرکز چاپ ظاهر می‌شود و
 * WebView پس از پایانِ کارِ چاپ (onFinish/onCancel/onFailedِ adapter) آزاد
 * می‌شود؛ حدِ ۳ دقیقه هم برای حالتی است که کاربر پنلِ چاپ را باز نکند.
 */
internal class HeadlessExamPrinter(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var jobName = "exam"
    private var finished = false
    private var onStatusCb: ((String) -> Unit)? = null
    private var onFinishedCb: (() -> Unit)? = null

    private val timeoutRunnable = Runnable {
        if (!finished) finish()
    }

    fun print(
        printable: OfficialExamPrintable,
        mode: String,
        onStatus: (String) -> Unit,
        onFinished: () -> Unit
    ) {
        releaseWebView()
        finished = false
        jobName = printable.documentTitle.ifBlank { "exam" } + "-" + mode
        onStatusCb = onStatus
        onFinishedCb = onFinished
        val web = WebView(appContext)
        webView = web
        createExamPrintWebView(
            context = appContext,
            printable = printable,
            printMode = mode,
            onPageReady = { },
            onPrint = { m -> startPrintJob(web, m) },
            onError = { message -> onStatus("چاپ ناموفق بود: $message") },
            onToast = { message -> if (message.isNotBlank()) onStatus(message) },
            onEditFigureTool = { _, _ -> },
            onPreviewClosed = { }
        )
        handler.postDelayed(timeoutRunnable, 180_000L)
        web.loadUrl(MAIN_PAGE_URL)
    }

    private fun status(message: String) {
        onStatusCb?.invoke(message)
    }

    private fun startPrintJob(web: WebView, mode: String) {
        web.post {
            runCatching {
                val printManager = web.context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager == null) {
                    status("امکان چاپ روی این دستگاه در دسترس نیست.")
                    finish()
                    return@post
                }
                val base = web.createPrintDocumentAdapter(jobName)
                // V101 — الگوی رسمیِ AOSP (PrintHtmlOffScreen): آزادسازی در
                // onFinishِ adapter؛ timeoutِ 180 ثانیه‌ای هم پشتوانه‌ی اضافی
                // برای انصراف/اتصالِ معلق است (API چاپ callbackِ job ندارد).
                printManager.print(jobName, OneShotPrintAdapter(base) { finish() }, PrintAttributes.Builder().build())
            }.onFailure {
                status("چاپ ناموفق بود.")
                finish()
            }
        }
    }

    /** پایانِ منظم: آزادسازیِ WebView + اطلاع به میزبان (یک‌بار). */
    private fun finish() {
        if (finished) return
        finished = true
        handler.removeCallbacks(timeoutRunnable)
        releaseWebView()
        onFinishedCb?.invoke()
    }

    private fun releaseWebView() {
        handler.removeCallbacks(timeoutRunnable)
        webView?.let { view ->
            runCatching {
                view.stopLoading()
                view.loadUrl("about:blank")
                view.destroy()
            }
        }
        webView = null
    }
}

/**
 * V101 — wrapper رسمیِ Android (PrintHtmlOffScreen): پس از پایانِ کارِ
 * چاپ WebView آزاد می‌شود چون نگهداریِ آن پرهزینه است (adapter API فقط
 * onStart/onLayout/onWrite/onFinish دارد؛ انصراف/اتصالِ معلق هم با timeoutِ
 * 180 ثانیه‌ایِ HeadlessExamPrinter پوشش می‌شود).
 */
private class OneShotPrintAdapter(
    private val wrapped: PrintDocumentAdapter,
    private val onDone: () -> Unit
) : PrintDocumentAdapter() {
    override fun onStart() { wrapped.onStart() }

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        wrapped.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
    }

    override fun onWrite(
        pages: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        wrapped.onWrite(pages, destination, cancellationSignal, callback)
    }

    override fun onFinish() {
        runCatching { wrapped.onFinish() }
        handlerPost { onDone() }
    }

    private fun handlerPost(block: () -> Unit) {
        // adapter روی رشتهٔ چاپ صدا زده می‌شود؛ آزادسازیِ WebView باید اصلی باشد.
        Handler(Looper.getMainLooper()).post(block)
    }
}
