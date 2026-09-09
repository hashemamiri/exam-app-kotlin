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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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

/**
 * V125 — سندِ اصلی اکنون میزبانِ موتورِ پیش‌نمایش/چاپِ «آزمون‌ساز v20» است
 * (print/web/*.css و *.js عیناً از نسخهٔ وب + print/web/webhost.js). رندررِ
 * قبلی (V105–V124) کنار گذاشته شده: print/exam_print_renderer_legacy.html.
 * بینندهٔ وب (PGS) نوارِ کاملِ خودش را دارد؛ پس هدرِ بومیِ پیش‌نمایش پنهان است.
 */
internal const val WEB_ENGINE_PREVIEW = true

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
    // V121 — پنجرهٔ تنظیماتِ کادر/جدول‌بندیِ سراسریِ جدولِ سؤال‌ها.
    var showBoxSettings by remember { mutableStateOf(false) }
    // V121 — پنجرهٔ «تنظیمات صفحه» (📐 موتور PGS): تغییرات همان لحظه در رندرر اعمال می‌شوند.
    var pageSetupOpen by remember { mutableStateOf(false) }
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
                // V116 — هدرِ بومی (Compose) بالای WebView: واقعاً ثابت است، چون
                // اصلاً داخل صفحهٔ اسکرول‌شونده نیست. ✕ قرمز، کل صفحه/اندازهٔ واقعی
                // و نوارِ قالب‌بندیِ اسکرول‌شونده که به JS رندرر فرمان می‌دهد.
                // V125 — موتورِ وبِ آزمون‌ساز v20 نوارِ خودش را دارد (بستن/چاپ/زوم/تنظیمات
                // صفحه/بندانگشتی)؛ هدرِ بومی فقط اگر موتورِ وب بارگذاری نشود نمایش داده می‌شود.
                if (!loading && initialPrintMode == null && !WEB_ENGINE_PREVIEW) {
                    PrintPreviewHeader(
                        onClose = { requestDismiss() },
                        onOpenBoxSettings = { showBoxSettings = true },
                        onPageSetup = { pageSetupOpen = true },
                        onFormat = { kind, value ->
                            // V120 — قبلاً فقط `\` و `'` حذف می‌شدند؛ `"`، خطِ
                            // جدید و `</script>` دست‌نخورده می‌ماندند. اگر یک‌روز
                            // این مسیر برای مقداری غیر از هگز رنگ/کلید فونت/عدد
                            // به کار می‌رفت، همان یک نقطهٔ تزریقِ کد در WebView
                            // می‌شد. حالا با toJsStringLiteral هر دو آرگومان با
                            // JSON.encode-مانند کاملاً امن اسکیپ می‌شوند.
                            runJs(
                                "(function(){try{return window.ExamPrintRenderer&&window.ExamPrintRenderer.applyFormat?" +
                                    "window.ExamPrintRenderer.applyFormat(${kind.toJsStringLiteral()},${value.toJsStringLiteral()}):''}" +
                                    "catch(e){return ''}})()",
                                null
                            )
                        }
                    )
                }
                if (pageSetupOpen) {
                    PrintPageSetupDialog(
                        initial = remember { ir.exam.app.data.local.PrintPageSetupStore(context).read() },
                        onDismiss = { pageSetupOpen = false },
                        onApply = { setup ->
                            ir.exam.app.data.local.PrintPageSetupStore(context).write(setup)
                            pageSetupOpen = false
                            val json = setup.toJson()
                            runJs("(function(){try{return window.ExamPrintRenderer&&window.ExamPrintRenderer.setPageSetup?window.ExamPrintRenderer.setPageSetup($json):'missing'}catch(e){return 'err'}})()", null)
                        }
                    )
                }
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

                // V121 — تنظیماتِ کادر/جدول‌بندیِ سراسری؛ همان ذخیره‌سازِ سراسریِ
                // دستگاه (PrintBoxStyleStore) که مثلِ PrintHeaderStore کار می‌کند.
                if (showBoxSettings) {
                    val boxStore = remember { ir.exam.app.data.local.PrintBoxStyleStore(context) }
                    PrintBoxSettingsDialog(
                        initial = remember { boxStore.read() },
                        onApply = { style ->
                            boxStore.write(style)
                            showBoxSettings = false
                            val json = ir.exam.app.data.local.printBoxStyleToJson(style)
                            runJs(
                                "(function(){try{return window.ExamPrintRenderer&&window.ExamPrintRenderer.applyBoxStyle?" +
                                    "window.ExamPrintRenderer.applyBoxStyle(${json.toJsStringLiteral()}):''}" +
                                    "catch(e){return ''}})()",
                                null
                            )
                        },
                        onDismiss = { showBoxSettings = false }
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
    private val onPreviewClosed: () -> Unit
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
    // V117 — viewport ثابت (۸۳۰px = برگهٔ A4 + حاشیه) و overviewMode روشن: پیش‌فرض
    // «اندازهٔ واقعی با کمترین زوم» (کلِ برگه در پهنای صفحه)؛ کاربر با دو انگشت
    // زوم می‌کند. پنجره‌های بومی دیگر داخلِ HTML نیستند، پس ریزشدنِ V76.2 منتفی است.
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
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
            onPreviewClosed = onPreviewClosed
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
                // V121 — تنظیماتِ سراسریِ کادر/جدول‌بندیِ چاپ، ذخیره‌شده روی دستگاه
                ir.exam.app.data.local.PrintBoxStyleStore(context).read(),
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

/** V116 — هدرِ ثابتِ پیش‌نمایش: عنوان، کل صفحه/اندازهٔ واقعی، ✕ قرمز و نوارِ قالب‌بندی. */
@Composable
private fun PrintPreviewHeader(
    onClose: () -> Unit,
    onOpenBoxSettings: () -> Unit,
    onPageSetup: () -> Unit,
    onFormat: (kind: String, value: String) -> Unit
) {
    var colorMenu by remember { mutableStateOf(false) }
    var sizeMenu by remember { mutableStateOf(false) }
    var fontMenu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("پیش‌نمایش برگه", style = MaterialTheme.typography.titleSmall, color = Color(0xFF0F172A))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onPageSetup,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) { Text("تنظیمات صفحه", color = Color(0xFF0F172A)) }
                androidx.compose.material3.FilledIconButton(
                    onClick = onClose,
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                    modifier = Modifier.size(36.dp)
                ) { Icon(Icons.Outlined.Close, contentDescription = "بستن") }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatChip("B", bold = true) { onFormat("bold", "") }
            FormatChip("I", italic = true) { onFormat("italic", "") }
            FormatChip("U", underline = true) { onFormat("underline", "") }
            // V121 — تراز پاراگرافیِ تکه‌ای: روی بندِ حاویِ بخشِ انتخاب‌شده اعمال
            // می‌شود (applyFormat("align", ...) در exam_print_renderer.html).
            FormatChip("راست") { onFormat("align", "right") }
            FormatChip("وسط") { onFormat("align", "center") }
            FormatChip("چپ") { onFormat("align", "left") }
            FormatChip("بلوک") { onFormat("align", "justify") }
            Box {
                FormatChip("رنگ") { colorMenu = true }
                androidx.compose.material3.DropdownMenu(expanded = colorMenu, onDismissRequest = { colorMenu = false }) {
                    listOf(
                        "#000000" to "مشکی", "#dc2626" to "قرمز", "#2563eb" to "آبی", "#16a34a" to "سبز",
                        "#7c3aed" to "بنفش", "#ea580c" to "نارنجی", "#6b7280" to "خاکستری"
                    ).forEach { (hex, name) ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(android.graphics.Color.parseColor(hex))))
                                    Text(name)
                                }
                            },
                            onClick = { colorMenu = false; onFormat("color", hex) }
                        )
                    }
                }
            }
            Box {
                FormatChip("اندازه") { sizeMenu = true }
                androidx.compose.material3.DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }) {
                    listOf(10, 11, 12, 13, 14, 16, 18, 20, 24, 28).forEach { px ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text("$px") }, onClick = { sizeMenu = false; onFormat("size", px.toString()) })
                    }
                }
            }
            Box {
                FormatChip("فونت") { fontMenu = true }
                androidx.compose.material3.DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                    listOf(
                        "default" to "پیش‌فرض", "Vazirmatn" to "وزیرمتن", "Shabnam" to "شبنم", "Sahel" to "ساحل",
                        "BNazanin" to "ب نازنین", "Tahoma" to "تاهوما", "serif" to "سریف"
                    ).forEach { (key, name) ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(name) }, onClick = { fontMenu = false; onFormat("font", key) })
                    }
                }
            }
            FormatChip("پاک") { onFormat("clear", "") }
            // V121 — کادر/جدول‌بندیِ سراسریِ جدولِ سؤال‌ها (خط دور/ستون/فاصلهٔ داخلی).
            FormatChip("کادر") { onOpenBoxSettings() }
        }
    }
}

@Composable
private fun FormatChip(
    label: String,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    onClick: () -> Unit
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Text(
            label,
            color = Color(0xFF0F172A),
            fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Black else null,
            fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else null,
            textDecoration = if (underline) androidx.compose.ui.text.style.TextDecoration.Underline else null
        )
    }
}

/** V121 — پنجرهٔ «تنظیمات صفحه» موتور چاپ (معادلِ 📐 در آزمون‌ساز v20). */
@Composable
private fun PrintPageSetupDialog(
    initial: ir.exam.app.data.local.PrintPageSetup,
    onDismiss: () -> Unit,
    onApply: (ir.exam.app.data.local.PrintPageSetup) -> Unit
) {
    var s by remember { mutableStateOf(initial) }
    var mT by remember { mutableStateOf(initial.mT.toString()) }
    var mB by remember { mutableStateOf(initial.mB.toString()) }
    var mR by remember { mutableStateOf(initial.mR.toString()) }
    var mL by remember { mutableStateOf(initial.mL.toString()) }
    var customW by remember { mutableStateOf(initial.customW.toString()) }
    var customH by remember { mutableStateOf(initial.customH.toString()) }
    var font by remember { mutableStateOf(initial.font.toString()) }
    fun num(text: String, fallback: Int, lo: Int, hi: Int) = (text.trim().toIntOrNull() ?: fallback).coerceIn(lo, hi)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیمات صفحه") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("کاغذ", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ir.exam.app.data.local.PrintPageSetup.PAPER_NAMES.forEach { (key, name) ->
                        androidx.compose.material3.FilterChip(selected = s.paper == key, onClick = { s = s.copy(paper = key) }, label = { Text(name) })
                    }
                }
                if (s.paper == "custom") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedTextField(value = customW, onValueChange = { customW = it }, label = { Text("عرض (mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                        androidx.compose.material3.OutlinedTextField(value = customH, onValueChange = { customH = it }, label = { Text("ارتفاع (mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
                Text("جهت", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.FilterChip(selected = s.orient == "portrait", onClick = { s = s.copy(orient = "portrait") }, label = { Text("عمودی") })
                    androidx.compose.material3.FilterChip(selected = s.orient == "landscape", onClick = { s = s.copy(orient = "landscape") }, label = { Text("افقی") })
                }
                Text("حاشیه‌ها (mm)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.OutlinedTextField(value = mT, onValueChange = { mT = it }, label = { Text("بالا") }, singleLine = true, modifier = Modifier.weight(1f))
                    androidx.compose.material3.OutlinedTextField(value = mB, onValueChange = { mB = it }, label = { Text("پایین") }, singleLine = true, modifier = Modifier.weight(1f))
                    androidx.compose.material3.OutlinedTextField(value = mR, onValueChange = { mR = it }, label = { Text("راست") }, singleLine = true, modifier = Modifier.weight(1f))
                    androidx.compose.material3.OutlinedTextField(value = mL, onValueChange = { mL = it }, label = { Text("چپ") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                androidx.compose.material3.OutlinedTextField(value = font, onValueChange = { font = it }, label = { Text("اندازهٔ فونت پایه (pt)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("فاصلهٔ سؤال‌ها", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("compact" to "فشرده", "normal" to "معمولی", "open" to "باز").forEach { (key, name) ->
                        androidx.compose.material3.FilterChip(selected = s.spacing == key, onClick = { s = s.copy(spacing = key) }, label = { Text(name) })
                    }
                }
                SetupSwitch("کادرِ دورِ برگه", s.border) { s = s.copy(border = it) }
                SetupSwitch("شمارهٔ صفحه", s.pageNumbers) { s = s.copy(pageNumbers = it) }
                SetupSwitch("تکرارِ سرستونِ جدول در هر برگه", s.repeatHeader) { s = s.copy(repeatHeader = it) }
                SetupSwitch("نمایشِ بارم", s.showScores) { s = s.copy(showScores = it) }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                onApply(
                    s.copy(
                        mT = num(mT, s.mT, 0, 80), mB = num(mB, s.mB, 0, 80), mR = num(mR, s.mR, 0, 80), mL = num(mL, s.mL, 0, 80),
                        customW = num(customW, s.customW, 60, 600), customH = num(customH, s.customH, 60, 600),
                        font = num(font, s.font, 6, 20)
                    )
                )
            }) { Text("اعمال") }
        },
        dismissButton = {
            Row {
                androidx.compose.material3.TextButton(onClick = { onApply(ir.exam.app.data.local.PrintPageSetup()) }) { Text("پیش‌فرض") }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )
}

@Composable
private fun SetupSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}
