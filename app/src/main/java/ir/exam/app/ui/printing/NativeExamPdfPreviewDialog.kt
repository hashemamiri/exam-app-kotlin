package ir.exam.app.ui.printing

import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.printing.NativePrintFigureLayout
import ir.exam.app.core.printing.OfficialExamImageLoader
import ir.exam.app.core.printing.OfficialPrintLayoutEngine
import ir.exam.app.core.printing.PrintPreviewLayoutCodec
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * پیش‌نمایش A4 کاملاً Native. تصویر هر صفحه فقط با [PdfRenderer] از همان فایل
 * PDF که Print Framework می‌گیرد ساخته می‌شود. Canvas شفاف بالای آن صرفاً
 * overlay gesture است و هیچ متن/شکل/صفحه‌بندی دوباره رسم نمی‌کند.
 */
@Composable
internal fun NativeExamPdfPreviewDialog(
    printable: OfficialExamPrintable,
    headerFields: Map<String, String>,
    onDismiss: () -> Unit,
    onFigLayouts: ((String) -> Unit)? = null,
    onQuestionTextChanged: ((questionIndex: Int, text: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var workingPrintable by remember(printable) { mutableStateOf<OfficialExamPrintable?>(null) }
    var document by remember { mutableStateOf<NativeExamPdfDocument?>(null) }
    var pageIndex by remember { mutableStateOf(0) }
    var loadingImages by remember { mutableStateOf(true) }
    var renderingPdf by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var figureTool by remember { mutableStateOf<FigureToolRequest?>(null) }

    // یک‌بار تصویرهای private/local را به Bitmap تبدیل کن. پس از هر drag فقط
    // PDF بازتولید می‌شود و شبکه/Coil دوباره اجرا نمی‌شود.
    LaunchedEffect(printable) {
        loadingImages = true
        error = null
        document = null
        workingPrintable = try {
            withContext(Dispatchers.IO) {
                OfficialExamImageLoader.load(appContext, printable)
            }
        } catch (_: Throwable) {
            error = "تصویرهای آزمون آماده نشدند. سایر بخش‌های PDF نمایش داده می‌شوند."
            printable
        }
        loadingImages = false
    }

    // این تنها نقطهٔ تولید layout است. PdfRenderer و adapter چاپ به فایل خروجی
    // همین factory متصل‌اند؛ بنابراین هیچ preview-layout موازی وجود ندارد.
    LaunchedEffect(workingPrintable, headerFields) {
        val source = workingPrintable ?: return@LaunchedEffect
        renderingPdf = true
        try {
            val produced = withContext(Dispatchers.Default) {
                NativeExamPdfDocumentFactory.create(appContext, source, headerFields)
            }
            document = produced
            pageIndex = pageIndex.coerceIn(0, produced.pageCount.coerceAtLeast(1) - 1)
        } catch (_: Throwable) {
            error = "ساخت PDF آزمون ناموفق بود."
        } finally {
            renderingPdf = false
        }
    }

    fun publishLayouts(source: OfficialExamPrintable) {
        onFigLayouts?.invoke(PrintPreviewLayoutCodec.snapshot(source))
    }

    fun updateQuestion(index: Int, transform: (OfficialPrintQuestion) -> OfficialPrintQuestion) {
        val source = workingPrintable ?: return
        val oldQuestion = source.questions.getOrNull(index) ?: return
        val newQuestion = transform(oldQuestion)
        if (newQuestion == oldQuestion) return
        val updated = source.copy(
            questions = source.questions.mapIndexed { questionIndex, question ->
                if (questionIndex == index) newQuestion else question
            }
        )
        workingPrintable = updated
        publishLayouts(updated)
    }

    fun commitFigure(
        target: NativePdfInteraction.Figure,
        mode: PreviewDragMode,
        bounds: RectF
    ) {
        updateQuestion(target.questionIndex) { question ->
            val existing = PrintPreviewLayoutCodec.decode(question.figLayoutsJson)[target.figureIndex]
            // target.bounds فقط قطعهٔ دیده‌شدهٔ شکل در این صفحه است. در صورتی که
            // شکل از page break عبور کرده باشد، اندازه/جای واقعی باید از
            // flowBounds کامل شروع شود، نه از ارتفاع crop همین صفحه.
            val logical = RectF(target.flowBounds)
            when (mode) {
                PreviewDragMode.MOVE -> {
                    logical.offset(
                        bounds.left - target.bounds.left,
                        bounds.top - target.bounds.top
                    )
                }

                PreviewDragMode.RESIZE -> {
                    logical.right = (logical.right + bounds.right - target.bounds.right)
                        .coerceAtLeast(logical.left + 12f * OfficialPrintLayoutEngine.MM_TO_PT)
                    logical.bottom = (logical.bottom + bounds.bottom - target.bounds.bottom)
                        .coerceAtLeast(logical.top + 8f * OfficialPrintLayoutEngine.MM_TO_PT)
                }

                PreviewDragMode.SEPARATOR -> Unit
            }
            val widthMm = (logical.width() / OfficialPrintLayoutEngine.MM_TO_PT).coerceIn(12f, 176f)
            val heightMm = (logical.height() / OfficialPrintLayoutEngine.MM_TO_PT).coerceIn(8f, 900f)
            val staysFree = mode == PreviewDragMode.MOVE || existing?.free == true || target.isFree
            val layout = NativePrintFigureLayout(
                xMm = if (staysFree) {
                    ((logical.left - OfficialPrintLayoutEngine.MARGIN) / OfficialPrintLayoutEngine.MM_TO_PT)
                        .coerceIn(0f, 181.76f - widthMm)
                } else {
                    existing?.xMm ?: 0f
                },
                yMm = if (staysFree) {
                    ((logical.top - target.questionFlowTop) / OfficialPrintLayoutEngine.MM_TO_PT)
                        .coerceIn(0f, 2_000f)
                } else {
                    existing?.yMm ?: 0f
                },
                widthMm = widthMm,
                heightMm = heightMm,
                free = staysFree
            )
            PrintPreviewLayoutCodec.updateFigure(question, target.figureIndex, layout)
        }
        status = if (mode == PreviewDragMode.MOVE) "جای شکل ذخیره شد ✓" else "اندازهٔ شکل ذخیره شد ✓"
    }

    fun commitSeparator(target: NativePdfInteraction.Separator, bounds: RectF) {
        val deltaPoints = bounds.centerY() - target.bounds.centerY()
        updateQuestion(target.questionIndex) { question ->
            question.copy(
                sepExtraPx = (question.sepExtraPx + PrintPreviewLayoutCodec.separatorPxDeltaFromPdfPoints(deltaPoints))
                    .coerceIn(0, 1_500)
            )
        }
        status = "فاصلهٔ سؤال ذخیره شد ✓"
    }

    fun startPrint(documentToPrint: NativeExamPdfDocument) {
        try {
            val manager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
                ?: throw IllegalStateException("سرویس چاپ در این دستگاه در دسترس نیست.")
            manager.print(
                printable.documentTitle.ifBlank { "آزمون" }.take(80),
                // عمداً همان pdfFile (نه PDF بازتولیدشده) استفاده می‌شود.
                NativeExamPdfPrintAdapter(documentToPrint, deleteWhenFinished = false),
                android.print.PrintAttributes.Builder()
                    .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(android.print.PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                    .build()
            )
        } catch (_: Throwable) {
            status = "بازکردن پنجرهٔ چاپ ناموفق بود."
        }
    }

    fun requestFigureEdit(target: NativePdfInteraction.Figure) {
        val question = workingPrintable?.questions?.getOrNull(target.questionIndex) ?: return
        val occurrence = FigureCodec.occurrences(question.text).getOrNull(target.figureIndex)
        val tool = occurrence?.let { toolOfSpec(it.rawJson) }
        if (occurrence == null || tool == null) {
            status = "این شکل با ابزار بومی قابل‌ویرایش نیست."
            return
        }
        figureTool = FigureToolRequest(
            questionId = target.questionIndex.toString(),
            tool = tool,
            initialSpecJson = occurrence.rawJson,
            tokenStart = occurrence.start,
            tokenEnd = occurrence.endExclusive
        )
    }

    fun closeDialog() {
        workingPrintable?.let(::publishLayouts)
        onDismiss()
    }

    Dialog(
        onDismissRequest = ::closeDialog,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF344155)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "پیش‌نمایش PDF A4",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            "کشیدن شکل = جابه‌جایی · گوشهٔ پایینِ آخرین بخش = تغییر اندازه · دو ضربه = ویرایش · خط بین سؤال‌ها = فاصله",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD7E3EF)
                        )
                    }
                    TextButton(onClick = { document?.let(::startPrint) }, enabled = document != null && !renderingPdf) {
                        Text("چاپ")
                    }
                    TextButton(onClick = ::closeDialog) { Text("بستن") }
                }

                val current = document
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        current != null -> NativePdfPage(
                            file = current.pdfFile,
                            pageIndex = pageIndex,
                            interactions = current.interactions.filter { it.pageIndex == pageIndex },
                            editable = onFigLayouts != null,
                            onEditFigure = ::requestFigureEdit,
                            onCommitFigure = ::commitFigure,
                            onCommitSeparator = ::commitSeparator
                        )

                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Text(
                                if (loadingImages) "در حال آماده‌سازی تصویرها..." else "در حال ساخت PDF...",
                                color = Color.White,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                    if (renderingPdf && current != null) {
                        Text(
                            "به‌روزرسانی PDF…",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .background(Color(0xCC111827))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                current?.let { generated ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }, enabled = pageIndex > 0) {
                            Text("صفحهٔ قبل")
                        }
                        Text("صفحه ${pageIndex + 1} از ${generated.pageCount}", color = Color.White)
                        TextButton(
                            onClick = { pageIndex = (pageIndex + 1).coerceAtMost(generated.pageCount - 1) },
                            enabled = pageIndex < generated.pageCount - 1
                        ) { Text("صفحهٔ بعد") }
                    }
                }
                error?.let {
                    Text(it, color = Color(0xFFFFD8D5), style = MaterialTheme.typography.labelSmall)
                }
                status?.let {
                    Text(it, color = Color(0xFFD0F4DD), style = MaterialTheme.typography.labelSmall)
                }
            }

            figureTool?.takeIf { it.isNative }?.let { request ->
                ExamFigureToolHost(
                    request = request,
                    onInsert = { token ->
                        val questionIndex = request.questionId.toIntOrNull() ?: -1
                        val source = workingPrintable
                        val question = source?.questions?.getOrNull(questionIndex)
                        if (source == null || question == null || request.tokenStart !in 0..question.text.length ||
                            request.tokenEnd !in request.tokenStart..question.text.length
                        ) {
                            status = "ویرایش شکل ذخیره نشد."
                        } else {
                            val newText = question.text.replaceRange(request.tokenStart, request.tokenEnd, token)
                            updateQuestion(questionIndex) { it.copy(text = newText) }
                            onQuestionTextChanged?.invoke(questionIndex, newText)
                            status = "ویرایش شکل ذخیره شد ✓"
                        }
                        figureTool = null
                    },
                    onDismiss = { figureTool = null }
                )
            }
        }
    }
}

@Composable
private fun NativePdfPage(
    file: java.io.File,
    pageIndex: Int,
    interactions: List<NativePdfInteraction>,
    editable: Boolean,
    onEditFigure: (NativePdfInteraction.Figure) -> Unit,
    onCommitFigure: (NativePdfInteraction.Figure, PreviewDragMode, RectF) -> Unit,
    onCommitSeparator: (NativePdfInteraction.Separator, RectF) -> Unit
) {
    var bitmap by remember(file, pageIndex) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file, pageIndex) {
        bitmap = withContext(Dispatchers.Default) { renderPdfPage(file, pageIndex) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(
                OfficialPrintLayoutEngine.PAGE_WIDTH.toFloat() / OfficialPrintLayoutEngine.PAGE_HEIGHT.toFloat()
            )
            .background(Color.White)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            androidx.compose.foundation.Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "صفحهٔ PDF ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        } ?: CircularProgressIndicator()
        if (editable && bitmap != null) {
            NativePdfInteractionOverlay(
                interactions = interactions,
                onEditFigure = onEditFigure,
                onCommitFigure = onCommitFigure,
                onCommitSeparator = onCommitSeparator
            )
        }
    }
}

@Composable
private fun NativePdfInteractionOverlay(
    interactions: List<NativePdfInteraction>,
    onEditFigure: (NativePdfInteraction.Figure) -> Unit,
    onCommitFigure: (NativePdfInteraction.Figure, PreviewDragMode, RectF) -> Unit,
    onCommitSeparator: (NativePdfInteraction.Separator, RectF) -> Unit
) {
    val figures = interactions.filterIsInstance<NativePdfInteraction.Figure>()
    val separators = interactions.filterIsInstance<NativePdfInteraction.Separator>()
    var activeDrag by remember(interactions) { mutableStateOf<PreviewDrag?>(null) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(interactions) {
                detectTapGestures(onDoubleTap = { point ->
                    val hit = figures.firstOrNull { pdfToDisplay(it.bounds, size).expanded(10f).contains(point) }
                    if (hit != null) onEditFigure(hit)
                })
            }
            .pointerInput(interactions) {
                detectDragGestures(
                    onDragStart = { point ->
                        val figure = figures.firstOrNull {
                            pdfToDisplay(it.bounds, size).expanded(10f).contains(point)
                        }
                        if (figure != null) {
                            val bounds = pdfToDisplay(figure.bounds, size)
                            activeDrag = PreviewDrag(
                                target = figure,
                                // انتهای یک شکلِ split‌شده فقط در آخرین page crop
                                // resize-handle دارد؛ cropهای میانی/اول فقط move
                                // می‌شوند تا ارتفاع کامل با قطعهٔ صفحه اشتباه نشود.
                                mode = if (figure.canResize && point.x >= bounds.right - 28f && point.y >= bounds.bottom - 28f) {
                                    PreviewDragMode.RESIZE
                                } else {
                                    PreviewDragMode.MOVE
                                },
                                startBounds = bounds
                            )
                        } else {
                            val separator = separators.firstOrNull {
                                pdfToDisplay(it.bounds, size).expanded(8f).contains(point)
                            }
                            if (separator != null) {
                                activeDrag = PreviewDrag(
                                    target = separator,
                                    mode = PreviewDragMode.SEPARATOR,
                                    startBounds = pdfToDisplay(separator.bounds, size)
                                )
                            }
                        }
                    },
                    onDrag = { change, amount ->
                        val active = activeDrag ?: return@detectDragGestures
                        change.consume()
                        activeDrag = active.copy(delta = active.delta + amount)
                    },
                    onDragEnd = {
                        val active = activeDrag ?: return@detectDragGestures
                        val displayBounds = active.displayBounds(size)
                        val pdfBounds = displayToPdf(displayBounds, size)
                        when (val target = active.target) {
                            is NativePdfInteraction.Figure -> onCommitFigure(target, active.mode, pdfBounds)
                            is NativePdfInteraction.Separator -> onCommitSeparator(target, pdfBounds)
                        }
                        activeDrag = null
                    },
                    onDragCancel = { activeDrag = null }
                )
            }
    ) {
        figures.forEach { figure ->
            val bounds = pdfToDisplay(figure.bounds, size)
            drawRect(
                color = Color(0xB8008A78),
                topLeft = Offset(bounds.left, bounds.top),
                size = Size(bounds.width, bounds.height),
                style = Stroke(width = 1.25.dp.toPx())
            )
            if (figure.canResize) {
                drawCircle(
                    color = Color(0xCC008A78),
                    radius = 6.dp.toPx(),
                    center = Offset(bounds.right, bounds.bottom)
                )
            }
        }
        separators.forEach { separator ->
            val bounds = pdfToDisplay(separator.bounds, size)
            val y = bounds.center.y
            drawLine(
                color = Color(0xBB2563EB),
                start = Offset(bounds.left, y),
                end = Offset(bounds.right, y),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(Color(0xCC2563EB), radius = 5.dp.toPx(), center = Offset(bounds.center.x, y))
        }
        activeDrag?.let { active ->
            val bounds = active.displayBounds(size)
            val color = if (active.mode == PreviewDragMode.SEPARATOR) Color(0xFF1D4ED8) else Color(0xFF0F766E)
            if (active.mode == PreviewDragMode.SEPARATOR) {
                drawLine(color, Offset(bounds.left, bounds.center.y), Offset(bounds.right, bounds.center.y), 2.dp.toPx())
            } else {
                drawRect(
                    color = color,
                    topLeft = Offset(bounds.left, bounds.top),
                    size = Size(bounds.width, bounds.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

private enum class PreviewDragMode { MOVE, RESIZE, SEPARATOR }

private data class PreviewDrag(
    val target: NativePdfInteraction,
    val mode: PreviewDragMode,
    val startBounds: Rect,
    val delta: Offset = Offset.Zero
) {
    fun displayBounds(pageSize: IntSize): Rect = when (mode) {
        // جریان PDF پیوسته است: move عمودی نباید در مرز یک crop/page گیر کند.
        // Canvas بیرون صفحه را clip می‌کند، اما delta کامل به flowBounds می‌رسد.
        PreviewDragMode.MOVE -> startBounds.translated(delta).boundedHorizontally(pageSize)
        PreviewDragMode.RESIZE -> Rect(
            startBounds.left,
            startBounds.top,
            (startBounds.right + delta.x).coerceIn(startBounds.left + 24f, pageSize.width.toFloat()),
            // شکل می‌تواند با بزرگ‌شدن وارد صفحهٔ بعد شود؛ فقط حداقل ارتفاع
            // روی همین gesture نگه داشته می‌شود و cap نهایی در codec اعمال است.
            (startBounds.bottom + delta.y).coerceAtLeast(startBounds.top + 20f)
        )
        PreviewDragMode.SEPARATOR -> {
            val dy = delta.y.coerceIn(-startBounds.center.y + 5f, pageSize.height - startBounds.center.y - 5f)
            Rect(startBounds.left, startBounds.top + dy, startBounds.right, startBounds.bottom + dy)
        }
    }

    fun displayBounds(pageSize: Size): Rect = displayBounds(
        IntSize(
            pageSize.width.toInt().coerceAtLeast(1),
            pageSize.height.toInt().coerceAtLeast(1)
        )
    )
}

private fun renderPdfPage(file: java.io.File, pageIndex: Int): Bitmap? = try {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    try {
        val renderer = PdfRenderer(descriptor)
        try {
            if (pageIndex !in 0 until renderer.pageCount) {
                null
            } else {
                val page = renderer.openPage(pageIndex)
                try {
                    val width = max(1, page.width * 2)
                    val height = max(1, page.height * 2)
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                } finally {
                    page.close()
                }
            }
        } finally {
            renderer.close()
        }
    } finally {
        descriptor.close()
    }
} catch (_: Throwable) {
    null
}

private fun pdfToDisplay(rect: RectF, pageSize: IntSize): Rect = Rect(
    rect.left / OfficialPrintLayoutEngine.PAGE_WIDTH * pageSize.width,
    rect.top / OfficialPrintLayoutEngine.PAGE_HEIGHT * pageSize.height,
    rect.right / OfficialPrintLayoutEngine.PAGE_WIDTH * pageSize.width,
    rect.bottom / OfficialPrintLayoutEngine.PAGE_HEIGHT * pageSize.height
)

private fun pdfToDisplay(rect: RectF, pageSize: Size): Rect = pdfToDisplay(
    rect,
    IntSize(pageSize.width.toInt().coerceAtLeast(1), pageSize.height.toInt().coerceAtLeast(1))
)

private fun displayToPdf(rect: Rect, pageSize: IntSize): RectF = RectF(
    rect.left / pageSize.width * OfficialPrintLayoutEngine.PAGE_WIDTH,
    rect.top / pageSize.height * OfficialPrintLayoutEngine.PAGE_HEIGHT,
    rect.right / pageSize.width * OfficialPrintLayoutEngine.PAGE_WIDTH,
    rect.bottom / pageSize.height * OfficialPrintLayoutEngine.PAGE_HEIGHT
)

private fun Rect.expanded(amount: Float): Rect = Rect(
    left - amount,
    top - amount,
    right + amount,
    bottom + amount
)

private fun Rect.translated(delta: Offset): Rect = Rect(
    left + delta.x,
    top + delta.y,
    right + delta.x,
    bottom + delta.y
)

private fun Rect.boundedHorizontally(pageSize: IntSize): Rect {
    val dx = when {
        left < 0f -> -left
        right > pageSize.width -> pageSize.width - right
        else -> 0f
    }
    return Rect(left + dx, top, right + dx, bottom)
}
