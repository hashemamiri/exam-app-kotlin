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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
 * پنجرهٔ تمام‌صفحهٔ پیش‌نمایش و چاپ آزمون.
 *
 * asset مستقلِ `exam_print_renderer.html` فقط برگهٔ A4 را می‌سازد. ورود داده
 * با `window.setExamData` انجام می‌شود و فرمول/شکل با رندررهای بومی به تصویر
 * امن تبدیل می‌شوند. بنابراین این سطح هیچ فرم ساخت آزمون، ذخیرهٔ محلی یا
 * ویرایشگر متنی ندارد.
 *
 * - `initialPreview` پیش‌نمایش A4 و تنظیم چیدمان شکل‌ها را نشان می‌دهد.
 * - `initialPrintMode` چاپ مستقیم دانش‌آموز یا پاسخ‌نامه را آغاز می‌کند.
 */

/** نشانی ثابت سند اصلی تا callbackهای WebView فقط یک‌بار داده را تزریق کنند. */
internal const val MAIN_PAGE_URL = "https://exam-print.local/print/exam_print_renderer.html"


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
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    // V119 — تصاویرِ سؤال (data-URL استودیو یا نشانیِ Supabase) پیش از ساختِ WebView
    // به توکنِ %%FIG:img%% تبدیل می‌شوند؛ از V107 این مرحله در مسیرِ آزمون‌ساز
    // فراخوانی نمی‌شد و تصویرِ آپلودشده در پیش‌نمایش/چاپ نبود.
    var inlinedPrintable by remember(printable) { mutableStateOf<OfficialExamPrintable?>(null) }
    LaunchedEffect(printable) {
        inlinedPrintable = if (printable == null) null else ExamHtmlImageInliner.inline(context.applicationContext, printable)
    }
    // دابل‌کلیک روی شکل در پیش‌نمایش، ویرایشگر بومیِ همان شکل را باز می‌کند.
    var figureTool by remember { mutableStateOf<FigureToolRequest?>(null) }
    var figureEditRequest by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var barStatus by remember { mutableStateOf<String?>(null) }
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

    // چیدمان شکل‌ها و فاصلهٔ جداکننده باید پیش از بستن پیش‌نمایش حفظ شود.
    var lastFigLayoutsJson by remember { mutableStateOf<String?>(null) }
    var dismissing by remember { mutableStateOf(false) }

    fun publishFigLayouts(raw: String?) {
        val json = unwrapJsString(raw).ifBlank { "{}" }
        if (json != "{}" && json != lastFigLayoutsJson) {
            lastFigLayoutsJson = json
            onFigLayouts?.invoke(json)
        }
    }

    fun fetchFigLayoutsSnapshot(after: (() -> Unit)? = null) {
        val view = webViewRef
        if (view == null) {
            after?.invoke()
            return
        }
        view.evaluateJavascript(
            "(function(){try{return window.ExamPrintRenderer&&window.ExamPrintRenderer.layoutSnapshot?window.ExamPrintRenderer.layoutSnapshot():'{}'}catch(e){return '{}'}})()"
        ) { raw ->
            publishFigLayouts(raw)
            after?.invoke()
        }
    }

    fun requestDismiss() {
        if (dismissing) return
        dismissing = true
        fetchFigLayoutsSnapshot {
            val view = webViewRef
            if (view != null) view.post { onDismiss() } else onDismiss()
        }
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
            "(function(){try{return window.ExamPrintRenderer&&window.ExamPrintRenderer.figureAt?" +
                "window.ExamPrintRenderer.figureAt('" + qid + "'," + index + "):''}catch(e){return ''}})()"
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
        // V100 — هدرِ قدیمی (بازگشت/عنوان/«سربرگ») با حذفِ آزمون‌سازِ چاپی
        // کاربری نداشت: پیش‌نمایش و چاپِ مستقیم هر دو بدونِ هدر می‌شوند.
        Surface(Modifier.fillMaxSize(), color = Color(0xFF334155)) {
            Column(Modifier.fillMaxSize()) {
                // V126 — هیچ هدر/نوارِ بومی روی پیش‌نمایش نیست: بینندهٔ موتورِ وب (PGS)
                // نوارِ کاملِ خودش را دارد (بستن، چاپ، زوم، تنظیمات صفحه 📐، بندانگشتی).
                Box(Modifier.fillMaxSize().weight(1f)) {
                    val readyPrintable = inlinedPrintable
                    if (readyPrintable != null || printable == null) AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            // V101 — راه‌اندازیِ کامل در تابعِ مشترک (همان موتور
                            // چاپِ مستقیمِ بدون‌صفحه از اینجا استفاده می‌کند).
                            createExamPrintWebView(
                                context = ctx,
                                printable = readyPrintable,
                                printMode = initialPrintMode,
                                // تزریقِ داده کامل شد: فقط در حالتِ پیش‌نمایش
                                // پنجرهٔ پیش‌نمایش باز می‌شود؛ در چاپِ مستقیم
                                // خودِ تابع پیش از این فراخوانی، چاپ را شلیک کرده است.
                                onPageReady = {
                                    loading = false
                                    if (initialPreview) {
                                        previewOpen = true
                                        webViewRef?.evaluateJavascript(
                                            "(function(){try{return window.ExamPrintRenderer&&window.ExamPrintRenderer.showPreview?window.ExamPrintRenderer.showPreview():'missing'}catch(e){return 'err'}})()",
                                            null
                                        )
                                    }
                                },
                                onPrint = { mode ->
                                    webViewRef?.let { view ->
                                        view.post {
                                            // V106 — پس از بسته‌شدنِ پنلِ چاپ، برگه به حالتِ
                                            // پیش‌نمایش برمی‌گردد (کلاسِ چاپ پاک، رندرِ دوباره).
                                            val restore = {
                                                // چاپِ مستقیم از بیلدر: پنجره پس از پنلِ چاپ بسته می‌شود
                                                // (پیش‌تر صفحهٔ خالیِ حالتِ چاپ می‌ماند).
                                                if (initialPrintMode != null) view.post { requestDismiss() }
                                                else view.post {
                                                    view.evaluateJavascript(
                                                        "(function(){try{return window.ExamPrintRenderer&&window.ExamPrintRenderer.restorePreview?window.ExamPrintRenderer.restorePreview():'missing'}catch(e){return 'err'}})()",
                                                        null
                                                    )
                                                }
                                            }
                                            runCatching {
                                                val printContext = ctx.findActivityContext() ?: ctx
                                                val printManager = printContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                                val jobName = (printable?.documentTitle ?: "آزمون").ifBlank { "exam" } + "-" + mode
                                                val printAdapter = view.createPrintDocumentAdapter(jobName)
                                                if (printManager == null) {
                                                    restore()
                                                } else {
                                                    printManager.print(jobName, OneShotPrintAdapter(printAdapter) { restore() }, ir.exam.app.data.local.PrintPageSetupStore(ctx).read().printAttributes())
                                                }
                                            }.onFailure { restore() }
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
                                // یک snapshot پیش از dismiss کافی است؛ requestDismiss با
                                // پرچم `dismissing` از فراخوانی تکراری جلوگیری می‌کند.
                                onPreviewClosed = {
                                    val view = webViewRef
                                    if (view != null) view.post { requestDismiss() } else requestDismiss()
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

                // فقط ویرایشِ شکلِ انتخاب‌شده از پیش‌نمایش با ابزارهای بومی.
                figureTool?.takeIf { it.isNative }?.let { req ->
                    ExamFigureToolHost(
                        request = req,
                        onInsert = { token ->
                            figureTool = null
                            val b64 = android.util.Base64.encodeToString(
                                token.toByteArray(Charsets.UTF_8),
                                android.util.Base64.NO_WRAP
                            )
                            // V120 — questionId (و b64، هرچند فقط ارقام/حروفِ base64
                            // دارد) اکنون با toJsStringLiteral اسکیپ می‌شوند؛ قبلاً
                            // مستقیم داخلِ رشتهٔ تک‌کوت الحاق می‌شدند.
                            val script = "(function(){try{return window.ExamPrintRenderer&&window.ExamPrintRenderer.replaceFigure?" +
                                "window.ExamPrintRenderer.replaceFigure(${req.questionId.toJsStringLiteral()}," +
                                req.tokenStart + "," + req.tokenEnd + ",${b64.toJsStringLiteral()}):'missing'}" +
                                "catch(e){return 'err'}})()"
                            runJs(script) { result ->
                                barStatus = if (result?.contains("ok") == true) "ویرایش شد ✓" else "ویرایش ناموفق بود."
                            }
                        },
                        onDismiss = { figureTool = null }
                    )
                }

            }
        }
    }
}

/**
 * V120 — رشته را به یک لیترالِ رشته‌ایِ امنِ جاوااسکریپت تبدیل می‌کند
 * (`kotlinx.serialization` همان قوانینِ اسکیپِ JSON را دارد که برای رشتهٔ
 * جاوااسکریپت هم کافی است: `\`، `"`، کنترل‌کاراکترها و `</script>` را
 * می‌پوشاند). قبلاً `onFormat` فقط `\` و `'` را دستی حذف می‌کرد که `"`،
 * خطِ جدید و `</script>` را باز می‌گذاشت؛ همهٔ نقاطی که مقدارِ آزاد را به
 * `runJs` می‌دهند باید از این تابع استفاده کنند، نه الحاقِ مستقیمِ رشته.
 */
private fun String.toJsStringLiteral(): String =
    kotlinx.serialization.json.JsonPrimitive(this).toString().replace("</", "<\\/")

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

private class ExamPrintBridge(
    private val renderer: ExamPrintAssetRenderer,
    private val onPrint: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onEditFigureTool: (String, Int) -> Unit,
    private val onToast: (String) -> Unit,
    private val onPreviewClosed: () -> Unit,
    private val pageSetupStore: ir.exam.app.data.local.PrintPageSetupStore
) {
    @JavascriptInterface
    fun renderFormula(source: String?): String = renderer.formulaDataUrl(source)

    @JavascriptInterface
    fun renderFigure(rawSpec: String?): String = renderer.figureDataUrl(rawSpec)

    @JavascriptInterface
    fun toast(message: String?) {
        onToast(message.orEmpty())
    }

    @JavascriptInterface
    fun previewClosed() {
        onPreviewClosed()
    }

    @JavascriptInterface
    fun print(mode: String?) {
        onPrint(if (mode == "teacher") "teacher" else "student")
    }

    @JavascriptInterface
    fun editFigureTool(questionId: String?, index: Int) {
        onEditFigureTool(questionId.orEmpty(), index)
    }

    /**
     * V126 — پنلِ «تنظیمات صفحه»ی خودِ موتورِ وب (📐) تغییرش را برمی‌گرداند تا مثلِ قبل روی
     * دستگاه بماند و پنلِ چاپِ اندروید هم‌اندازهٔ همان کاغذ باز شود (وب localStorage داشت).
     */
    @JavascriptInterface
    fun pageSetupChanged(json: String?) {
        // روی رشتهٔ JS صدا زده می‌شود؛ SharedPreferences.apply ایمن است.
        runCatching { pageSetupStore.write(ir.exam.app.data.local.PrintPageSetup.fromJson(json)) }
    }

    @JavascriptInterface
    fun onError(code: String?) {
        code?.takeIf { it.isNotBlank() }?.let(onError)
    }
}

/**
 * کارخانهٔ مشترک WebView برای پیش‌نمایش و چاپ مستقیم. فقط assetهای محلی
 * را سرو می‌کند و payload را پس از آماده‌شدن صفحه تزریق می‌کند.
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
    // V125 — موتورِ وب چند مگابایت اسکریپت دارد (اطلس‌های شکل)؛ همهٔ منابع محلی و بدونِ شبکه‌اند.
    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    @Suppress("DEPRECATION")
    settings.allowFileAccessFromFileURLs = false
    @Suppress("DEPRECATION")
    settings.allowUniversalAccessFromFileURLs = false
    // V125 — بینندهٔ PGS خودش برگه را در پهنای صفحه جا می‌دهد (pgsFit) و زوم/لمسِ
    // دوانگشتیِ خودش را دارد؛ زومِ WebView نوارِ ثابتِ آن را از دست می‌داد.
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    // V76.2 — useWideViewPort متاوویوپورتِ فایل (width=device-width) را اعمال
    // می‌کند؛ اما overviewMode باید خاموش بماند وگرنه WebView برای محتوای عریضِ
    // A4 (۷۳px) کل صفحه را zoom-out می‌کند و همه پنجره‌ها/دکمه‌ها ریز می‌شوند
    // (ریشهٔ «پنجره‌ها کوچک است»).
    // V117 — viewport ثابت ۸۳۰px + overviewMode برای رندررِ قبلی بود.
    // V127 — موتورِ وب (PGS) خودش برگه را با transform در پهنای صفحه جا می‌دهد (pgsFit) و
    // پینچِ خودش را دارد؛ اما پنل‌های position:fixed آن (📐 تنظیمات صفحه، دیالوگ چاپ) با
    // viewportِ ۸۳۰px نسبت به پنجرهٔ مجازیِ بزرگ‌تر چیده می‌شدند و روی گوشی بریده/بیرونِ
    // صفحه می‌افتادند و media queryِ موبایلِ آن‌ها (≤820px) هم فعال نمی‌شد. پس viewport
    // باید پهنای واقعیِ دستگاه باشد (متاتگِ width=device-width در سند).
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = false
    // V118 — WebView «اندازهٔ قلمِ سیستم» (دسترس‌پذیری) را روی متنِ HTML اعمال می‌کند
    // و سربرگ/جدول بلندتر از حالتِ چاپ می‌شد؛ برگهٔ A4 باید مستقل از تنظیمِ گوشی باشد.
    settings.textZoom = 100

    addJavascriptInterface(
        ExamPrintBridge(
            renderer = ExamPrintAssetRenderer(context),
            onPrint = onPrint,
            onError = onError,
            // V82.0 — ویرایشِ ابزارِ درج‌شده با دابل‌کلیک
            onEditFigureTool = onEditFigureTool,
            // V87.8 — همان اعلانِ وسط‌چینِ محوشونده
            onToast = onToast,
            // V89.5 — بستنِ پنجرهٔ پیش‌نمایش
            onPreviewClosed = onPreviewClosed,
            // V126 — تنظیمات صفحه از پنلِ 📐 خودِ موتورِ وب روی دستگاه ذخیره می‌شود
            pageSetupStore = ir.exam.app.data.local.PrintPageSetupStore(context)
        ),
        "ExamPrintBridge"
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
            // V114 — فونت‌های نوارِ قالب‌بندی: از res/font (وزیرمتن/شبنم/ساحل) یا assets/fonts (ب نازنین)
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
                ir.exam.app.data.local.PrintHeaderStore(context).read(),
                // V121 — تنظیمات صفحهٔ موتور چاپ (کاغذ/جهت/حاشیه/…)
                ir.exam.app.data.local.PrintPageSetupStore(context).read().toJson()
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
                                    "try{document.body.classList.add('exam-print-mode');}catch(e){}",
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
    // V106 — PrintManager فقط با Context فعالیت پنلِ چاپ را نشان می‌دهد؛ با
    // applicationContext هیچ خطایی نمی‌دهد ولی پنل هرگز باز نمی‌شود (ریشهٔ
    // «آیکن پرینتر کارت‌ها کار نمی‌کند»). WebView با appContext ساخته می‌شود
    // (بدون نشت)، ولی چاپ با Context فعالیت انجام می‌شود.
    private val printContext: Context = context.findActivityContext() ?: context
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
        lateinit var configuredWebView: WebView
        configuredWebView = createExamPrintWebView(
            context = appContext,
            printable = printable,
            printMode = mode,
            onPageReady = { },
            onPrint = { selectedMode -> startPrintJob(configuredWebView, selectedMode) },
            onError = { message -> onStatus("چاپ ناموفق بود: $message") },
            onToast = { message -> if (message.isNotBlank()) onStatus(message) },
            onEditFigureTool = { _, _ -> },
            onPreviewClosed = { }
        )
        webView = configuredWebView
        handler.postDelayed(timeoutRunnable, 180_000L)
    }

    private fun status(message: String) {
        onStatusCb?.invoke(message)
    }

    private fun startPrintJob(web: WebView, mode: String) {
        web.post {
            runCatching {
                val printManager = printContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager == null) {
                    status("امکان چاپ روی این دستگاه در دسترس نیست.")
                    finish()
                    return@post
                }
                val base = web.createPrintDocumentAdapter(jobName)
                // V101 — الگوی رسمیِ AOSP (PrintHtmlOffScreen): آزادسازی در
                // onFinishِ adapter؛ timeoutِ 180 ثانیه‌ای هم پشتوانه‌ی اضافی
                // برای انصراف/اتصالِ معلق است (API چاپ callbackِ job ندارد).
                printManager.print(jobName, OneShotPrintAdapter(base) { finish() }, ir.exam.app.data.local.PrintPageSetupStore(appContext).read().printAttributes())
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

/** V106 — Context فعالیتِ میزبان (لازم برای نمایشِ پنلِ چاپ). */
internal tailrec fun Context.findActivityContext(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivityContext()
    else -> null
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
