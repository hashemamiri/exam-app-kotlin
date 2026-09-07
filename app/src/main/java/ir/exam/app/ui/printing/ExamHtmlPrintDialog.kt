package ir.exam.app.ui.printing

import android.annotation.SuppressLint
import android.content.Context
import android.print.PrintAttributes
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
        // V100 — هدرِ قدیمی (بازگشت/عنوان/«سربرگ») با حذفِ آزمون‌سازِ چاپی
        // کاربری نداشت: پیش‌نمایش و چاپِ مستقیم هر دو بدونِ هدر می‌شوند.
        Surface(Modifier.fillMaxSize(), color = Color(0xFF334155)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).also { webViewRef = it }.apply {
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
                                // V76.2 — useWideViewPort متاوویوپورتِ فایل (width=device-width)
                                // را اعمال می‌کند؛ اما overviewMode باید خاموش بماند وگرنه WebView
                                // برای محتوای عریضِ A4 (۷۳۳px) کل صفحه را zoom-out می‌کند و همه
                                // پنجره‌ها/دکمه‌ها ریز می‌شوند (ریشهٔ «پنجره‌ها کوچک است»).
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = false

                                addJavascriptInterface(
                                    ExamPrintBridge(
                                        onPrint = { mode ->
                                            post {
                                                runCatching {
                                                    val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                                    val jobName = (printable?.documentTitle ?: "آزمون").ifBlank { "exam" } + "-$mode"
                                                    val printAdapter = createPrintDocumentAdapter(jobName)
                                                    printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
                                                }
                                            }
                                        },
                                        onError = { message -> post { jsError = message; loading = false } },
                                        // V82.0 — ویرایشِ ابزارِ درج‌شده با دابل‌کلیک
                                        onEditFigureTool = { qid, index ->
                                            post { figureEditRequest = qid to index }
                                        },
                                        // V87.8 — همان اعلانِ وسط‌چینِ محوشونده
                                        onToast = { message ->
                                            post { if (message.isNotBlank()) barStatus = message }
                                        },
                                        // V99.2 — پیش از بستنِ پیش‌نمایش، چیدمانِ
                                        // اشیاء را اسنپ‌شات بگیر تا به بیلدر برسد.
                                        onPreviewClosed = {
                                            fetchFigLayoutsSnapshot()
                                            post { previewOpen = false }
                                        }
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
                                        // مسیرِ نسبیِ `../figure_atlas/x.jpg` را WebView پیش از
                                        // ارسال ساده می‌کند، پس اینجا به `/figure_atlas/` می‌رسد.
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
                                        // فریمِ اصلی. از V79.0 که ویرایشگر فرمول با src لود می‌شود،
                                        // پایانِ لودِ همان iframe این متد را دوباره شلیک می‌کرد و
                                        // setExamData({reset:true}) کلِ سؤالات را پاک و صفحه را
                                        // باز-رندر می‌کرد — دقیقاً وقتی ویرایشگر می‌خواست باز شود.
                                        // نتیجه: کلیک روی آیکن فرمول هیچ پنجره‌ای باز نمی‌کرد.
                                        // فقط به پایانِ لودِ سندِ اصلی واکنش نشان بده.
                                        if (url != MAIN_PAGE_URL) return
                                        // V100 — جریانِ بازیابی (بررسی asset ویرایشگر فرمول +
                                        // بازیابیِ autosave/آینهٔ پیش‌نویس) با حذفِ حالتِ
                                        // «آزمون جدید»ِ آزمون‌سازِ چاپی حذف شد: پنجره همیشه با
                                        // دادهٔ مشخص (پیش‌نمایش یا چاپ) باز می‌شود.
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
                                                    result?.contains("ok") == true -> post {
                                                        loading = false
                                                        // V99.1 — در چاپِ مستقیم (دانش‌آموز/پاسخ‌نامه) پنجرهٔ
                                                        // پیش‌نمایشِ HTML باز نمی‌شود: آن پنجرهٔ overlay هنگامِ
                                                        // چاپ پنهان می‌شود و چون محتوای چاپ داخلش منتقل شده،
                                                        // خروجی چاپ خالی می‌ماند. برگهٔ خالصِ A4 را خودِ
                                                        // پنجرهٔ چاپِ اندروید نمایش می‌دهد؛ پنجرهٔ کارت‌ها هم
                                                        // (با شرطِ initialPrintMode) هرگز دیده نمی‌شود.
                                                        if (initialPreview) {
                                                            previewOpen = true
                                                            view.evaluateJavascript("(function(){try{return window.__qmfShowPreview?window.__qmfShowPreview():'missing'}catch(e){return 'err'}})()", null)
                                                        }
                                                        // V99.2 — در چاپِ مستقیم برگهٔ A4 در جایِ اصلی‌اش (خارجِ
                                                        // overlay) روی پس‌زمینهٔ خاکستری دیده می‌شود؛ وگرنه با
                                                        // پنهانِ بودنِ کارت‌ها و نداشتنِ پنجرهٔ پیش‌نمایش، WebView
                                                        // یک صفحهٔ سفیدِ خالی پشتِ پنجرهٔ چاپ نشان می‌داد.
                                                        if (initialPrintMode != null) {
                                                            view.evaluateJavascript(
                                                                "try{document.body.classList.add('qmf-print-mode');}catch(e){}",
                                                                null
                                                            )
                                                        }
                                                        if (initialPrintMode == "student") {
                                                            view.evaluateJavascript("if (typeof printStudent==='function') printStudent();", null)
                                                        } else if (initialPrintMode == "teacher") {
                                                            view.evaluateJavascript("if (typeof printTeacher==='function') printTeacher();", null)
                                                        }
                                                    }
                                                    attempts < 50 -> view.postDelayed({ tryInject() }, 100)
                                                    else -> post { loading = false }
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
                                            // هنگامِ اسکرول می‌آید و خطای واقعی نیست؛ نوارِ قرمزِ
                                            // «خطای صفحه چاپ» کاربر را گیج می‌کرد.
                                            if (text.contains("Ignored attempt to cancel")) return true
                                            val safe = text.replace(Regex("https?://\\S+"), "[url]").take(300)
                                            post { jsError = "CONSOLE: $safe"; loading = false }
                                        }
                                        return true
                                    }
                                }

                                loadUrl(MAIN_PAGE_URL)
                            }
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
                       می‌شوند، مثلِ یک اعلانِ بومی. */
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
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
