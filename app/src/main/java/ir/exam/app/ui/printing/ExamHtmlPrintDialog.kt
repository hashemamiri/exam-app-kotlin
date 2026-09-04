package ir.exam.app.ui.printing

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.core.figure.AtlasCatalog
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.GRAPH_FIGURES
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.ui.math.FormulaHostDialog
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * V76.0 — پنجرهٔ تمام‌صفحهٔ «نسخهٔ 30» (آزمون‌ساز/چاپ تعاملی HTML):
 * فایل print/exam_print.html را در WebView بارگذاری می‌کند؛ با پل
 * window.setExamData سؤالات و سربرگ آزمون خودکار تزریق می‌شوند و کاربر همان‌جا
 * ویرایش/چاپ می‌کند (فقط چاپ؛ آزمون سرور تغییر نمی‌کند).
 * printable == null یعنی «آزمون جدید» — فایل با payload ریست خالی باز می‌شود.
 * V76.1 — viewport خود فایل اعمال می‌شود (رابط موبایل در اندازهٔ واقعی) و انتخاب
 * تصویر با دکمهٔ دوربینِ فایل از طریق onShowFileChooser پشتیبانی می‌شود.
 * V76.3 — هفت کنترل اصلی (تنظیمات سربرگ، ذخیره، بازکردن، چاپ دانشجو/استاد،
 * سوال جدید، پیش‌نمایش) به نوار فرمان بومی این پنجره منتقل شده‌اند؛ نوار HTML
 * فایل مخفی است و فرمان‌ها از طریق evaluateJavascript به صفحه اعمال می‌شوند.
 * V76.4 — پنجره‌های «تنظیمات سربرگ/ذخیره/بازکردن/سوال جدید» هم بومی شدند
 * (شِمای سربرگ از print/header_settings_schema.json — استخراج‌شده از خود فایل)
 * و دکمهٔ دوربینِ سؤال، استودیوی تصویر بومی را باز می‌کند
 * (ExamPrintNative.openImageStudio با پشتیبانِ استودیوی کامل HTML).
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
    onDismiss: () -> Unit
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
    // V82.0 — ویرایشگر بومیِ فرمول: (questionId, متنِ کاملِ سؤال)
    var formulaTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    // V78.1 — نوارِ بومیِ مدیریت سؤال
    var questionRows by remember { mutableStateOf<List<QuestionRow>>(emptyList()) }
    var questionTotal by remember { mutableStateOf("") }
    var showQuestionManager by remember { mutableStateOf(false) }
    var barStatus by remember { mutableStateOf<String?>(null) }
    // V81.0 — نتیجهٔ بررسی خواندنِ asset ویرایشگر و متنِ تشخیص.
    var mathAssetProbe by remember { mutableStateOf("(not-checked)") }
    var formulaDiag by remember { mutableStateOf<String?>(null) }
    // V76.4 — پنجره‌های بومی
    val headerSchema = remember { loadHeaderSchema(context) }
    var showHeaderSettings by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showNewQuestion by remember { mutableStateOf(false) }
    var studioQuestionId by remember { mutableStateOf<String?>(null) }
    var studioImagesJson by remember { mutableStateOf<String?>(null) }
    var pageSnapshotJson by remember { mutableStateOf<String?>(null) }
    var pendingOpenText by remember { mutableStateOf<String?>(null) }
    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = unwrapJsString(pageSnapshotJson).ifBlank { return@rememberLauncherForActivityResult }
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)
        barStatus = if (ok) "فایل JSON ذخیره شد ✓" else "ذخیرهٔ فایل ناموفق بود."
    }
    val runJs: (String, ((String?) -> Unit)?) -> Unit = { script, cb ->
        webViewRef?.evaluateJavascript(script, cb)
    }

    // V78.2 — گرفتنِ عکسِ فوریِ پیش‌نویس و نوشتنش در آینهٔ بومی
    fun mirrorDraft() {
        webViewRef?.evaluateJavascript(
            "(function(){try{return window.__qmfDraftSnapshot?window.__qmfDraftSnapshot():''}catch(e){return ''}})()"
        ) { raw ->
            val json = unwrapJsString(raw)
            if (json.isNotBlank()) ExamDraftMirror.save(context, json)
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

    // V82.0 — «فرمول» هم مثل بقیه از پل می‌آید؛ متنِ کاملِ سؤال را می‌گیریم و
    // به FormulaHostDialog می‌دهیم (همان ویرایشگرِ آزمون‌سازِ بومی).
    LaunchedEffect(figureTool) {
        val req = figureTool ?: return@LaunchedEffect
        if (req.tool != FigureToolRequest.FORMULA) return@LaunchedEffect
        runJs(
            "(function(){try{return window.__qmfQuestionText?" +
                "window.__qmfQuestionText('" + req.questionId + "'):''}catch(e){return ''}})()"
        ) { raw ->
            formulaTarget = req.questionId to unwrapJsString(raw)
            figureTool = null
        }
    }

    // V76.6 — فهرست تصویرهای موجود سؤال، بعد از آماده‌شدن runJs
    LaunchedEffect(studioQuestionId) {
        val qid = studioQuestionId ?: return@LaunchedEffect
        runJs("(function(){try{return window.__qmfQuestionImages?window.__qmfQuestionImages('" + qid + "'):'missing'}catch(e){return 'err'}})()") { r ->
            studioImagesJson = unwrapJsString(r)
        }
    }    // V76.1 — دکمهٔ دوربینِ فایل (📷) یک input[type=file] داینامیک را کلیک می‌کند؛
    // WebView اندروید بدون onShowFileChooser آن را بی‌صدا نادیده می‌گیرد.
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val callback = fileChooserCallback
        fileChooserCallback = null
        callback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
    }
    // V76.3 — «بازکردن آزمون»: فایل JSON با انتخاب‌گر بومی خوانده و با پل setExamData وارد صفحه می‌شود.
    val openExamPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        if (!text.trimStart().startsWith("{")) {
            barStatus = "فایل انتخاب‌شده آزمون نیست (فایل JSON لازم است)."
            return@rememberLauncherForActivityResult
        }
        pendingOpenText = text
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF1E3A8A)) {
            Column(Modifier.fillMaxSize()) {
                // نوار بالای پنجره با دکمه بستن و عنوان آزمون
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = if (printable == null) "آزمون جدید — آزمون‌ساز"
                        else "چاپ آزمون: " + printable.documentTitle.ifBlank { printable.subject }.ifBlank { "آزمون" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // V76.3 — نوار فرمان بومی: هفت کنترل اصلی (نوار HTML فایل مخفی شده است).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    NativeBarButton("⚙ تنظیمات سربرگ") {
                        runJs("(function(){try{return window.__qmfExportJson?window.__qmfExportJson():'{}'}catch(e){return '{}'}})()") { r ->
                            pageSnapshotJson = r
                            showHeaderSettings = true
                        }
                    }
                    NativeBarButton("💾 ذخیره") { showSaveDialog = true }
                    NativeBarButton("📂 بازکردن") { openExamPicker.launch("*/*") }
                    NativeBarButton("🖨 چاپ دانشجو") { runJs("if (typeof printStudent==='function') printStudent();", null) }
                    NativeBarButton("✅ چاپ استاد") { runJs("if (typeof printTeacher==='function') printTeacher();", null) }
                    NativeBarButton("➕ سوال جدید") { showNewQuestion = true }
                    NativeBarButton("👁 پیش‌نمایش") { runJs("if (typeof togglePreviewWindow==='function') togglePreviewWindow();", null) }
                    // V81.0 — اگر ویرایشگر فرمول باز نشد، این دکمه دقیقاً می‌گوید کجا گیر است.
                    NativeBarButton("🩺 بررسی فرمول") {
                        runJs("(function(){try{return window.__qmfFormulaDiag?window.__qmfFormulaDiag():'{}'}catch(e){return '{\"err\":\"'+e.message+'\"}'}})()") { raw ->
                            val formula = unwrapJsString(raw)
                            // V83.0 — وضعیت پیش‌نمایش را هم کنارش نشان بده.
                            runJs("(function(){try{return window.__qmfPreviewDiag?window.__qmfPreviewDiag():'{}'}catch(e){return '{}'}})()") { raw2 ->
                                formulaDiag = "asset=" + mathAssetProbe +
                                    "\n\n[فرمول]\n" + formula +
                                    "\n\n[پیش‌نمایش]\n" + unwrapJsString(raw2)
                            }
                        }
                    }
                    NativeBarButton("🗂 مدیریت سؤال") {
                        runJs("(function(){try{return window.__qmfQuestionList?window.__qmfQuestionList():'[]'}catch(e){return '[]'}})()") { list ->
                            questionRows = parseQuestionRows(list)
                            runJs("(function(){try{return window.__qmfTotalScore?window.__qmfTotalScore():''}catch(e){return ''}})()") { total ->
                                questionTotal = unwrapJsString(total)
                                showQuestionManager = true
                            }
                        }
                    }
                }

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
                                        onClose = { post { onDismiss() } },
                                        onError = { message -> post { jsError = message; loading = false } },
                                        onOpenImageStudio = { qid ->
                                            post { studioQuestionId = qid.ifBlank { null } }
                                        },
                                        onOpenFigureTool = { qid, tool ->
                                            post { figureTool = FigureToolRequest(qid, tool) }
                                        },
                                        // V82.0 — ویرایشِ ابزارِ درج‌شده با دابل‌کلیک
                                        onEditFigureTool = { qid, index ->
                                            post { figureEditRequest = qid to index }
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
                                        if (!path.startsWith("/print/")) return emptyResponse()
                                        val assetPath = path.removePrefix("/print/")
                                        if (assetPath.isBlank() || assetPath.contains("..")) return emptyResponse()
                                        return try {
                                            val stream = view.context.assets.open("print/$assetPath")
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
                                        // V81.0 — یک‌بار بررسی کن که فایل ویرایشگر
                                        // واقعاً از assets خوانده می‌شود. اگر نه، همان
                                        // ابتدا معلوم شود، نه بعد از کلیک کاربر.
                                        runCatching {
                                            view.context.assets.open("print/math_editor.html").use { st ->
                                                val head = ByteArray(256)
                                                val n = st.read(head)
                                                mathAssetProbe = if (n > 100) "ok:$n" else "short:$n"
                                            }
                                        }.onFailure { mathAssetProbe = "missing:${it.javaClass.simpleName}" }
                                        // V78.2 — اگر ذخیرهٔ خودکارِ صفحه خالی بود ولی آینهٔ بومی
                                        // پیش‌نویس داشت، آن را برگردان (کش WebView پاک شده بوده).
                                        if (printable == null) {
                                            view.evaluateJavascript(
                                                "(function(){try{return window.__qmfHasLocalDraft?window.__qmfHasLocalDraft():'no'}catch(e){return 'no'}})()"
                                            ) { has ->
                                                if (!unwrapJsString(has).contains("yes")) {
                                                    ExamDraftMirror.load(view.context)?.let { mirrored ->
                                                        view.evaluateJavascript(
                                                            "(function(){try{if(window.setExamData){window.setExamData($mirrored);return 'ok'}return 'missing'}catch(e){return 'err'}})()"
                                                        ) { r ->
                                                            if (r?.contains("ok") == true) {
                                                                post { barStatus = "پیش‌نویس آزمون بازیابی شد ✓" }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        val payload = ExamHtmlPrintPayloadBuilder.build(printable).toString()
                                        var attempts = 0
                                        fun tryInject() {
                                            attempts++
                                            view.evaluateJavascript(
                                                "(function(){if(window.setExamData){window.setExamData($payload);return 'ok';}return 'wait';})();"
                                            ) { result ->
                                                when {
                                                    result?.contains("ok") == true -> post { loading = false }
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
                                            val safe = message.message().replace(Regex("https?://\\S+"), "[url]").take(300)
                                            post { jsError = "CONSOLE: $safe"; loading = false }
                                        }
                                        return true
                                    }

                                    // V76.1 — انتخاب تصویر برای دکمهٔ دوربین (📷) سؤال
                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallback: ValueCallback<Array<Uri>>,
                                        fileChooserParams: android.webkit.WebChromeClient.FileChooserParams
                                    ): Boolean {
                                        fileChooserCallback = filePathCallback
                                        imagePicker.launch("image/*")
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
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }

                    // V81.0 — نتیجهٔ «بررسی فرمول»: متنِ قابل خواندن و قابل انتخاب،
                    // تا در صورت باقی‌ماندنِ مشکل عیناً برای بررسی فرستاده شود.
                    formulaDiag?.let { diag ->
                        AlertDialog(
                            onDismissRequest = { formulaDiag = null },
                            confirmButton = {
                                TextButton(onClick = { formulaDiag = null }) { Text("بستن") }
                            },
                            title = { Text("وضعیت ویرایشگر فرمول") },
                            text = {
                                SelectionContainer {
                                    Text(diag, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        )
                    }

                    jsError?.let { message ->
                        Text(
                            "خطای صفحه چاپ: $message",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                        )
                    }

                    barStatus?.let { message ->
                        Text(
                            message,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color(0xCC1E3A8A))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // V76.4 — پنجره‌های بومی آزمون‌ساز
            if (showHeaderSettings && headerSchema != null) {
                HeaderSettingsDialog(
                    schema = headerSchema,
                    currentValues = parsePageFields(pageSnapshotJson),
                    onApply = { payload ->
                        showHeaderSettings = false
                        val b64 = android.util.Base64.encodeToString(
                            kotlinx.serialization.json.Json.encodeToString(
                                kotlinx.serialization.serializer<Map<String, String>>(),
                                payload
                            ).toByteArray(Charsets.UTF_8),
                            android.util.Base64.NO_WRAP
                        )
                        runJs("(function(){try{return window.__qmfSetFields?window.__qmfSetFields(atob('" + b64 + "')):'missing'}catch(e){return 'err'}})()") { r ->
                            barStatus = if (r?.contains("ok") == true) "سربرگ اعمال شد ✓" else "اعمال سربرگ ناموفق بود."
                        }
                    },
                    onDismiss = { showHeaderSettings = false }
                )
            }
            if (showSaveDialog) {
                SaveExamDialog(
                    onSaveSession = {
                        showSaveDialog = false
                        runJs("(function(){try{return window.__qmfSaveNow?window.__qmfSaveNow():'missing'}catch(e){return 'err'}})()") { r ->
                            barStatus = if (r?.contains("ok") == true) "ذخیره شد ✓" else "ذخیره نشد!"
                        }
                    },
                    onSaveFile = {
                        showSaveDialog = false
                        runJs("(function(){try{return window.__qmfExportJson?window.__qmfExportJson():'{}'}catch(e){return '{}'}})()") { r ->
                            pageSnapshotJson = r ?: "{}"
                            saveFileLauncher.launch(safeExamFileName(null))
                        }
                    },
                    onDismiss = { showSaveDialog = false }
                )
            }
            pendingOpenText?.let { text ->
                val parsed = runCatching {
                    kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
                }.getOrNull()
                if (parsed == null) {
                    pendingOpenText = null
                    barStatus = "فایل JSON معتبر نیست."
                } else {
                    val fields = parsed["fields"]?.jsonObject
                    val count = parsed["questions"]?.jsonArray?.size ?: 0
                    OpenExamSummaryDialog(
                        course = fields?.get("f_course")?.jsonPrimitive?.content,
                        school = fields?.get("f_branch")?.jsonPrimitive?.content,
                        questionCount = count,
                        onApply = {
                            pendingOpenText = null
                            val b64 = android.util.Base64.encodeToString(text.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                            runJs("(function(){try{window.setExamData(atob('" + b64 + "'));return 'ok'}catch(e){return 'err'}})()") { r ->
                                barStatus = if (r?.contains("ok") == true) "آزمون باز شد ✓" else "باز کردن آزمون ناموفق بود."
                            }
                        },
                        onDismiss = { pendingOpenText = null }
                    )
                }
            }
            if (showNewQuestion) {
                NewQuestionTypeDialog(
                    onPick = { type ->
                        showNewQuestion = false
                        runJs("(function(){try{if(typeof pickQuestionType==='function'){pickQuestionType('" + type + "');return 'ok'}return 'missing'}catch(e){return 'err'}})()") { r ->
                            barStatus = if (r?.contains("ok") == true) null else "سوال جدید اضافه نشد."
                        }
                    },
                    onDismiss = { showNewQuestion = false }
                )
            }
            studioQuestionId?.let { qid ->
                ExamImageStudioDialog(
                    questionId = qid,
                    existingImages = parseExistingImages(studioImagesJson),
                    onInsert = { dataUrl, h ->
                        studioQuestionId = null
                        // dataUrl همیشه base64 است (بدون نقل‌قول/بک‌اسلش) ⇒ درج مستقیم امن است
                        runJs(
                            "(function(){try{return window.__qmfAddQuestionImage?window.__qmfAddQuestionImage('" + qid + "','" + dataUrl + "'," + h + "):'missing'}catch(e){return 'err'}})()"
                        ) { r ->
                            barStatus = if (r?.contains("ok") == true) "تصویر درج شد ✓" else "درج تصویر ناموفق بود."
                        }
                    },
                    onDeleteExisting = { idx ->
                        runJs(
                            "(function(){try{return window.__qmfRemoveQuestionImage?window.__qmfRemoveQuestionImage('" + qid + "'," + idx + "):'missing'}catch(e){return 'err'}})()"
                        ) { r ->
                            if (r?.contains("ok") == true) {
                                barStatus = "تصویر حذف شد."
                                runJs("(function(){try{return window.__qmfQuestionImages?window.__qmfQuestionImages('" + qid + "'):'missing'}catch(e){return 'err'}})()") { r2 ->
                                    studioImagesJson = unwrapJsString(r2)
                                }
                            } else barStatus = "حذف تصویر ناموفق بود."
                        }
                    },
                    onReplaceExisting = { idx, dataUrl, h ->
                        studioQuestionId = null
                        runJs(
                            "(function(){try{return window.__qmfReplaceQuestionImage?window.__qmfReplaceQuestionImage('" + qid + "'," + idx + ",'" + dataUrl + "'," + h + "):'missing'}catch(e){return 'err'}})()"
                        ) { r ->
                            barStatus = if (r?.contains("ok") == true) "تصویر جایگزین شد ✓" else "جایگزینی تصویر ناموفق بود."
                        }
                    },
                    onSplitToSame = { items ->
                        studioQuestionId = null
                        // چند فراخوانیِ پشت‌سرهم؛ dataUrlها base64 امن‌اند
                        items.forEach { (dataUrl, h) ->
                            runJs(
                                "(function(){try{return window.__qmfAddQuestionImage?window.__qmfAddQuestionImage('" + qid + "','" + dataUrl + "'," + h + "):'missing'}catch(e){return 'err'}})()"
                            ) { }
                        }
                        barStatus = items.size.toString() + " بخش به همین سؤال اضافه شد ✓"
                    },
                    onSplitToQuestions = { items ->
                        studioQuestionId = null
                        val payload = "[" + items.joinToString(",") { (d, h) -> "{\"d\":\"" + d + "\",\"h\":" + h + "}" } + "]"
                        val b64 = android.util.Base64.encodeToString(payload.toByteArray(), android.util.Base64.NO_WRAP)
                        runJs(
                            "(function(){try{return window.__qmfSplitQuestion?window.__qmfSplitQuestion('" + qid + "','" + b64 + "'):'missing'}catch(e){return 'err'}})()"
                        ) { r ->
                            barStatus = if (r?.toString()?.contains("ok") == true) "سؤال‌های جدید ساخته شدند ✓" else "ساخت سؤال‌های جداگانه ناموفق بود."
                        }
                    },
                    onOcrText = { text ->
                        // V76.9 — متنِ OCR به انتهای متنِ همان سؤال اضافه می‌شود
                        // (base64 تا فارسی و خط جدید سالم بمانند).
                        val b64 = android.util.Base64.encodeToString(
                            text.toByteArray(Charsets.UTF_8),
                            android.util.Base64.NO_WRAP
                        )
                        runJs(
                            "(function(){try{return window.__qmfAppendQuestionText?" +
                                "window.__qmfAppendQuestionText('" + qid + "','" + b64 + "'):'missing'}" +
                                "catch(e){return 'err'}})()"
                        ) { r ->
                            barStatus = if (r?.contains("ok") == true) {
                                "متنِ استخراج‌شده در سؤال درج شد ✓"
                            } else {
                                "درجِ متنِ استخراج‌شده ناموفق بود."
                            }
                        }
                    },
                    onDismiss = { studioQuestionId = null }
                )
            }

            // V78.0 — ابزارهای درجِ بومی (جدول، تناوبی، شکل، نمودار، آناتومی،
            // فیزیک، شیمی). V82.0 — همین میزبان حالتِ «ویرایش» را هم دارد:
            // اگر req.isEdit باشد، نتیجه جایگزینِ همان توکن می‌شود نه درجِ تازه.
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
                                mirrorDraft()
                                if (req.isEdit) "ویرایش شد ✓" else "در سؤال درج شد ✓"
                            } else {
                                if (req.isEdit) "ویرایش ناموفق بود." else "درج در سؤال ناموفق بود."
                            }
                        }
                    },
                    onDismiss = { figureTool = null }
                )
            }

            // V82.0 — ویرایشگرِ بومیِ فرمول. همان FormulaHostDialog که
            // آزمون‌سازِ بومی استفاده می‌کند: متنِ کاملِ سؤال را می‌گیرد و
            // متنِ کامل برمی‌گرداند، پس هم درج و هم ویرایشِ فرمول را پوشش می‌دهد.
            formulaTarget?.let { (qid, text) ->
                FormulaHostDialog(
                    initialText = text,
                    selectionStart = text.length,
                    selectionEnd = text.length,
                    onDismiss = { formulaTarget = null },
                    onResult = { newText ->
                        formulaTarget = null
                        if (newText != text) {
                            val b64 = android.util.Base64.encodeToString(
                                newText.toByteArray(Charsets.UTF_8),
                                android.util.Base64.NO_WRAP
                            )
                            runJs(
                                "(function(){try{return window.__qmfSetQuestionText?" +
                                    "window.__qmfSetQuestionText('" + qid + "','" + b64 + "'):'missing'}" +
                                    "catch(e){return 'err'}})()"
                            ) { r ->
                                barStatus = if (r?.contains("ok") == true) {
                                    mirrorDraft()
                                    "فرمول در سؤال درج شد ✓"
                                } else {
                                    "درج فرمول ناموفق بود."
                                }
                            }
                        }
                    }
                )
            }

            if (showQuestionManager) {
                ExamQuestionManagerSheet(
                    rows = questionRows,
                    totalScore = questionTotal,
                    onAction = { qid, action, arg ->
                        val argJs = if (arg == null) "null" else "'" + arg.replace("'", "") + "'"
                        runJs(
                            "(function(){try{return window.__qmfQuestionAction?" +
                                "window.__qmfQuestionAction('" + qid + "','" + action + "'," + argJs + "):'missing'}" +
                                "catch(e){return 'err'}})()"
                        ) { r ->
                            if (r?.contains("ok") == true) {
                                // فهرست را از خودِ صفحه دوباره بخوان تا منبعِ حقیقت یکی بماند
                                runJs("(function(){try{return window.__qmfQuestionList?window.__qmfQuestionList():'[]'}catch(e){return '[]'}})()") { list ->
                                    questionRows = parseQuestionRows(list)
                                }
                                runJs("(function(){try{return window.__qmfTotalScore?window.__qmfTotalScore():''}catch(e){return ''}})()") { total ->
                                    questionTotal = unwrapJsString(total)
                                }
                                mirrorDraft()
                            } else {
                                barStatus = "این کار روی سؤال انجام نشد."
                            }
                        }
                    },
                    onJumpTo = { qid ->
                        showQuestionManager = false
                        runJs("(function(){try{var e=document.getElementById('q_text_'+'" + qid + "');if(e){e.scrollIntoView({block:'center'});e.focus();return 'ok'}return 'missing'}catch(e){return 'err'}})()", null)
                    },
                    onDismiss = { showQuestionManager = false }
                )
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

/** استخراج فیلدهای ف_دار از JSON صفحه برای پیش‌پرکردن پنجرهٔ سربرگ. */
internal fun parsePageFields(snapshotJson: String?): Map<String, String> {
    val obj = runCatching {
        kotlinx.serialization.json.Json.parseToJsonElement(unwrapJsString(snapshotJson)).jsonObject
    }.getOrNull() ?: return emptyMap()
    val fields = obj["fields"]?.jsonObject ?: return emptyMap()
    return fields.entries.associate { (k, v) -> k to (v as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty() }
}

/** V76.6 — فهرست تصویرهای موجودِ سؤال از JSON پل (__qmfQuestionImages). */
internal fun parseExistingImages(raw: String?): List<StudioImageRef> {
    if (raw.isNullOrBlank() || !raw.startsWith("[")) return emptyList()
    return runCatching {
        kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonArray.map { el ->
            val o = el.jsonObject
            StudioImageRef(
                dataUrl = (o["src"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                w = (o["w"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                h = (o["h"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 0
            )
        }.filter { it.dataUrl.isNotBlank() }
    }.getOrDefault(emptyList())
}

@Composable
private fun NativeBarButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

private class ExamPrintBridge(
    private val onPrint: (String) -> Unit,
    private val onClose: () -> Unit,
    private val onError: (String) -> Unit,
    private val onOpenImageStudio: (String) -> Unit,
    private val onOpenFigureTool: (String, String) -> Unit,
    // V82.0 — دابل‌کلیک روی ابزارِ درج‌شده: ویرایشِ همان توکن
    private val onEditFigureTool: (String, Int) -> Unit
) {
    @JavascriptInterface
    fun print(mode: String?) {
        onPrint(mode ?: "student")
    }

    /** V76.4 — دکمهٔ دوربینِ سؤال: باز کردن استودیوی تصویر بومی برای این سؤال. */
    @JavascriptInterface
    fun openImageStudio(questionId: String?) {
        onOpenImageStudio(questionId.orEmpty())
    }

    /**
     * V78.0 — ابزارهای درج به پنجرهٔ بومیِ همان ابزار می‌روند؛ همان
     * ویرایشگرهایی که آزمون‌سازِ آنلاین استفاده می‌کند.
     * V82.0 — «formula» هم اضافه شد (FormulaHostDialog).
     * tool: figure | graph | table | anatomy | periodic | physics | chemistry | formula
     */
    @JavascriptInterface
    fun openFigureTool(questionId: String?, tool: String?) {
        onOpenFigureTool(questionId.orEmpty(), tool.orEmpty())
    }

    /**
     * V82.0 — دابل‌کلیک روی یک ابزارِ درج‌شده. `index` شمارهٔ ترتیبیِ توکن در
     * متنِ همان سؤال است؛ میزبانِ بومی spec را می‌خواند و همان پنجره را در
     * حالتِ ویرایش باز می‌کند، سپس نتیجه جایگزینِ همان توکن می‌شود.
     */
    @JavascriptInterface
    fun editFigureTool(questionId: String?, index: Int) {
        onEditFigureTool(questionId.orEmpty(), index)
    }

    @JavascriptInterface
    fun close() {
        onClose()
    }

    @JavascriptInterface
    fun onError(code: String?) {
        code?.takeIf { it.isNotBlank() }?.let(onError)
    }
}
