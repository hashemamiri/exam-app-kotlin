package ir.exam.app.ui.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import ir.exam.app.data.local.PrintLayoutStore
import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.printing.WordPageLayout
import ir.exam.app.ui.figure.InlineFigureView
import kotlin.math.roundToInt
import ir.exam.app.ui.builder.ExamBuilderViewModel
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import ir.exam.app.ui.math.NativeMathText

/**
 * V63.0 — ویرایشگر سند آزمون (Word-مانند)؛ پچ ۱ از ۳.
 *
 * این صفحه عمداً صفحهٔ «ایجاد آزمون» نیست: همهٔ سؤال‌های آزمون پشت‌سرهم روی
 * برگه‌های A4 با اندازهٔ واقعی و صفحه‌بندی خودکار چیده می‌شوند (موتور
 * `WordPageLayout`) و روی کارت هر سؤال یک آیکن مداد است که ویرایش همان سؤال را
 * باز می‌کند.
 *
 * پچ ۱: مداد + صفحه‌بندی/نمایش واقعی + ویرایش متن و بارم.
 * پچ ۲ (بعدی): جابه‌جایی و تغییر اندازهٔ تصویر/شکل/نمودار/جدول با دستگیره.
 * پچ ۳ (بعدی): ویرایش و اندازهٔ متن به سبک ورد (انتخاب بخشی از متن).
 */
@Composable
fun ExamDocumentEditorScreen(
    builder: ExamBuilderViewModel,
    examId: String,
    onBack: () -> Unit
) {
    val state by builder.state.collectAsState()
    // V63.5 — چیدمان چاپی جدا از خود آزمون: بارگیری در ورود، ذخیرهٔ محلی.
    val context = LocalContext.current
    val layoutStore = remember(context.applicationContext) { PrintLayoutStore(context.applicationContext) }
    var layoutLoaded by remember(examId) { mutableStateOf(false) }
    // V64.6 — پایهٔ canonical آزمون جدا نگه داشته می‌شود تا ذخیرهٔ ویرایشگر
    // فقط تفاوت‌های مخصوص چاپ را بنویسد، نه نسخهٔ چاپی را داخل آزمون ببرد.
    var canonicalQuestions by remember(examId) { mutableStateOf<List<QuestionDraft>?>(null) }
    LaunchedEffect(examId, state.loading) {
        if (!state.loading && !layoutLoaded) {
            val latest = state.questions
            canonicalQuestions = latest
            layoutStore.readForLatest(examId, latest)?.let(builder::overridePrintLayout)
            layoutLoaded = true
        }
    }
    var savedTick by remember(examId) { mutableStateOf(false) }
    // V63.9 — پیام ذخیره روی صفحه ظاهر و پس از ۲.۵ ثانیه محو می‌شود.
    LaunchedEffect(savedTick) {
        if (savedTick) {
            kotlinx.coroutines.delay(2500)
            savedTick = false
        }
    }
    // V63.5 — دکمهٔ برگشت گوشی: خروج از ویرایشگر به «چاپ آزمون»، نه از برنامه.
    BackHandler(onBack = onBack)
    var zoom by remember { mutableStateOf(1.6f) }
    var editingQuestionId by remember { mutableStateOf<String?>(null) }
    // V63.3 — شیء انتخاب‌شده برای +/− و آیکن جابجایی نوار ابزار.
    var selectedImage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedFigure by remember { mutableStateOf<Pair<String, Int>?>(null) }
    // V64.0 — مدل Word-مانند: هر عنصر (گزینه/سمت جورکردنی) بلوک مستقل
    // انتخاب/ویرایش است: (questionId, kind, index) — kind: opt | mL | mR
    var selectedElement by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    // V64.3 — عنصرِ «در حال ویرایش» جدا از «انتخاب» و کنترل‌شده از بالا
    // (پیشنهاد بازبینی کاربر): لمس دوم روشنش می‌کند و Enter آن را به عنصر
    // تازه‌ساخته می‌برد — برای هر عنصری، نه فقط خالی.
    var editingElement by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    // V63.9 — قفل جابجایی: با لمس قفل، شیء همان‌جا ثابت می‌شود.
    var objectsLocked by remember { mutableStateOf(false) }

    // V63.6 — تعداد صفحه از صفحه‌بندی با ارتفاع «واقعی رندر» (مثل ورد).
    var measuredPageCount by remember { mutableStateOf(1) }
    val editing = state.questions.firstOrNull { it.id == editingQuestionId }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Color(0xFFECEFF3))) {
        DocumentEditorTopBar(
            title = state.title.ifBlank { "آزمون" },
            questionCount = state.questions.size,
            pageCount = measuredPageCount,
            saving = state.saving,
            onSave = {
                layoutStore.write(
                    examId,
                    canonicalQuestions ?: state.questions,
                    state.questions
                )
                savedTick = true
            },
            onBack = onBack
        )

        // V63.3 — نوار ابزار واحد همیشگی با اسکرول افقی: +/− شیء، جابجایی،
        // آ+/آ−، تراز، بولد/ایتالیک، ذره‌بین +/− صفحه. ابزارهای متن روی
        // سؤال انتخابی و +/−/جابجایی روی شیء انتخابی اثر می‌کنند.
        DocumentToolbar(
            question = editing,
            hasObject = selectedImage != null || selectedFigure != null,
            hasElement = selectedElement != null,
            hasDeletable = selectedImage != null || selectedFigure != null || selectedElement != null,
            locked = objectsLocked,
            onToggleLock = { objectsLocked = !objectsLocked },
            // V64.1 — حذف انتخاب‌شده (شیء یا عنصر) مثل Delete ورد.
            onDeleteSelected = {
                selectedImage?.let { (questionId, imageId) ->
                    builder.removeImage(questionId, imageId); selectedImage = null
                }
                selectedFigure?.let { (questionId, occurrenceIndex) ->
                    builder.deleteFigure(questionId, occurrenceIndex); selectedFigure = null
                }
                selectedElement?.let { (questionId, kind, index) ->
                    when (kind) {
                        "opt" -> builder.removeOptionAt(questionId, index)
                        "mL" -> builder.removeMatchingSide(questionId, "left", index)
                        "mR" -> builder.removeMatchingSide(questionId, "right", index)
                    }
                    selectedElement = null; editingElement = null
                }
            },
            onObjectGrow = {
                selectedImage?.let { (questionId, imageId) ->
                    val media = state.questions.firstOrNull { it.id == questionId }
                        ?.images?.firstOrNull { it.id == imageId }
                    if (media != null) builder.resizeImage(questionId, imageId, media.widthMm + 10f)
                }
                selectedFigure?.let { (questionId, occurrenceIndex) ->
                    resizeFigureBy(builder, state.questions, questionId, occurrenceIndex, +10f)
                }
            },
            onObjectShrink = {
                selectedImage?.let { (questionId, imageId) ->
                    val media = state.questions.firstOrNull { it.id == questionId }
                        ?.images?.firstOrNull { it.id == imageId }
                    if (media != null) builder.resizeImage(questionId, imageId, media.widthMm - 10f)
                }
                selectedFigure?.let { (questionId, occurrenceIndex) ->
                    resizeFigureBy(builder, state.questions, questionId, occurrenceIndex, -10f)
                }
            },
            // V64.4 — Word-مانند: اگر «عنصری» انتخاب است قالب روی همان عنصر
            // اعمال می‌شود (استایل per-option)؛ وگرنه مثل قبل روی کل سؤال.
            // V64.5 — قالب عنصر برای هر سه نوع (گزینه و دو سمت جورکردنی).
            onFontSize = { delta ->
                val element = selectedElement
                if (element != null) {
                    val question = state.questions.firstOrNull { it.id == element.first }
                    val styles = when (element.second) {
                        "opt" -> question?.optionStyles
                        "mL" -> question?.matchingLeftStyles
                        else -> question?.matchingRightStyles
                    }
                    val base = styles?.getOrNull(element.third)?.fontSizeSp
                        ?: question?.fontSizeSp ?: 16f
                    applyElementStyle(builder, element) {
                        it.copy(fontSizeSp = (base + delta).coerceIn(8f, 40f))
                    }
                } else editing?.let { builder.setQuestionFontSize(it.id, it.fontSizeSp + delta) }
            },
            onBold = {
                val element = selectedElement
                if (element != null) applyElementStyle(builder, element) { it.copy(bold = !it.bold) }
                else editing?.let { builder.setQuestionBold(it.id, !it.bold) }
            },
            onItalic = {
                val element = selectedElement
                if (element != null) applyElementStyle(builder, element) { it.copy(italic = !it.italic) }
                else editing?.let { builder.setQuestionItalic(it.id, !it.italic) }
            },
            onAlign = { value -> editing?.let { builder.setQuestionAlign(it.id, value) } },
            onMoveQuestion = { delta -> editing?.let { builder.moveQuestion(it.id, delta) } },
            onZoomOut = { zoom = (zoom - 0.2f).coerceIn(0.6f, 3f) },
            onZoomIn = { zoom = (zoom + 0.2f).coerceIn(0.6f, 3f) },
            zoom = zoom
        )

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        state.uploadProgress?.let {
            Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }


        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.questions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("این آزمون هنوز سؤالی ندارد.")
            }
            else -> WordFlowDocument(
                questions = state.questions,
                zoom = zoom,
                editingQuestionId = editingQuestionId,
                onPageCount = { measuredPageCount = it },
                onSelectQuestion = { id ->
                    editingQuestionId = if (editingQuestionId == id) null else id
                    selectedImage = null; selectedFigure = null
                    selectedElement = null; editingElement = null
                },
                onTextChange = builder::updateText,
                onScoreChange = builder::updateScore,
                selectedImageId = selectedImage?.second,
                selectedFigure = selectedFigure,
                onSelectImage = { questionId, imageId ->
                    selectedImage = if (selectedImage == questionId to imageId) null
                        else questionId to imageId
                    selectedFigure = null
                },
                onSelectFigure = { questionId, occurrenceIndex ->
                    selectedFigure = if (selectedFigure == questionId to occurrenceIndex) null
                        else questionId to occurrenceIndex
                    selectedImage = null
                },
                objectsLocked = objectsLocked,
                selectedElement = selectedElement,
                onSelectElement = { questionId, kind, index ->
                    val target = Triple(questionId, kind, index)
                    selectedElement = if (selectedElement == target) null else target
                    selectedImage = null; selectedFigure = null; editingElement = null
                },
                editingElement = editingElement,
                onStartEditElement = { questionId, kind, index ->
                    editingElement = Triple(questionId, kind, index)
                },
                onElementText = { questionId, kind, index, text ->
                    when (kind) {
                        "opt" -> builder.updateOption(questionId, index, text)
                        "mL" -> builder.updateMatchingText(questionId, "left", index, text)
                        "mR" -> builder.updateMatchingText(questionId, "right", index, text)
                    }
                },
                // V64.1 — Enter در عنصر = عنصر جدید بعد از همان (مثل پاراگراف ورد).
                onElementEnter = { questionId, kind, index ->
                    when (kind) {
                        "opt" -> {
                            builder.insertOptionAfter(questionId, index)
                            selectedElement = Triple(questionId, "opt", index + 1)
                            editingElement = Triple(questionId, "opt", index + 1)
                        }
                        "mL", "mR" -> {
                            builder.addMatchingRow(questionId)
                            selectedElement = null; editingElement = null
                        }
                    }
                },
                onImageFreeMove = { questionId -> builder.setImagePosition(questionId, "free") },
                onMoveImage = builder::moveImage,
                onResizeImage = builder::resizeImage,
                onResizeFigure = { questionId, occurrenceIndex, widthMm ->
                    val question = state.questions.firstOrNull { it.id == questionId }
                    val occ = question?.let { FigureCodec.occurrences(it.text).getOrNull(occurrenceIndex) }
                    if (occ != null) builder.updateFigure(
                        questionId, occurrenceIndex,
                        WordPageLayout.withFigureWidthMm(occ.spec, widthMm)
                    )
                }
            )
        }
    }
    // V63.9 — پیام شناور ذخیره وسط‌پایین صفحه.
    if (savedTick) Text(
        "چیدمان چاپ ذخیره شد؛ فقط در چاپ همین آزمون اعمال می‌شود.",
        color = Color.White,
        fontSize = 13.sp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 28.dp)
            .background(Color(0xCC222D36), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
    }


}

@Composable
private fun DocumentEditorTopBar(
    title: String,
    questionCount: Int,
    pageCount: Int,
    saving: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.Close, contentDescription = "بستن ویرایشگر سند")
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "$questionCount سؤال · $pageCount صفحهٔ A4",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (saving) CircularProgressIndicator(Modifier.height(18.dp).width(18.dp))
            Button(onClick = onSave) { Text("ذخیره") }
        }
    }
}

/**
 * V63.6 — سند پیوستهٔ Word-واقعی: هر سؤال یک‌بار با عرض واقعی صفحه
 * اندازه‌گیری می‌شود (SubcomposeLayout) و سپس مثل ورد پشت‌سرهم در صفحه‌های
 * A4 چیده می‌شود؛ صفحهٔ بعدی فقط وقتی صفحهٔ قبلی «واقعاً» پر شد ساخته
 * می‌شود. سؤالی که از صفحه بلندتر است تنها در صفحهٔ خودش می‌آید.
 */
@Composable
private fun WordFlowDocument(
    questions: List<QuestionDraft>,
    zoom: Float,
    editingQuestionId: String?,
    onPageCount: (Int) -> Unit,
    onSelectQuestion: (String) -> Unit,
    onTextChange: (String, String) -> Unit,
    onScoreChange: (String, String) -> Unit,
    selectedImageId: String?,
    selectedFigure: Pair<String, Int>?,
    onSelectImage: (String, String) -> Unit,
    onSelectFigure: (String, Int) -> Unit,
    objectsLocked: Boolean,
    selectedElement: Triple<String, String, Int>?,
    editingElement: Triple<String, String, Int>?,
    onSelectElement: (String, String, Int) -> Unit,
    onStartEditElement: (String, String, Int) -> Unit,
    onElementText: (String, String, Int, String) -> Unit,
    onElementEnter: (String, String, Int) -> Unit,
    onImageFreeMove: (String) -> Unit,
    onMoveImage: (String, String, Float, Float) -> Unit,
    onResizeImage: (String, String, Float) -> Unit,
    onResizeFigure: (String, Int, Float) -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeLayout(Modifier) { constraints ->
            val pageWidthPx = WordPageLayout.mmToDp(WordPageLayout.PAGE_WIDTH_MM, zoom).dp.roundToPx()
            val pageHeightPx = WordPageLayout.mmToDp(WordPageLayout.PAGE_HEIGHT_MM, zoom).dp.roundToPx()
            val marginPx = WordPageLayout.mmToDp(WordPageLayout.MARGIN_MM, zoom).dp.roundToPx()

            val gapPx = WordPageLayout.mmToDp(WordPageLayout.BLOCK_GAP_MM, zoom).dp.roundToPx()
            val contentWidth = pageWidthPx - marginPx * 2
            val blockConstraints = Constraints(maxWidth = contentWidth)

            // ۱) اندازه‌گیری واقعی هر سؤال با عرض محتوا
            val placeables = questions.mapIndexed { index, question ->
                subcompose("q-${question.id}") {
                    WordQuestionBlock(
                        row = index + 1,
                        question = question,
                        zoom = zoom,
                        highlighted = editingQuestionId == question.id,
                        onSelect = { onSelectQuestion(question.id) },
                        editable = editingQuestionId == question.id,
                        onTextChange = { onTextChange(question.id, it) },
                        onScoreChange = { onScoreChange(question.id, it) },
                        selectedImageId = selectedImageId,
                        selectedFigureIndex = selectedFigure?.takeIf { it.first == question.id }?.second,
                        onSelectImage = { imageId -> onSelectImage(question.id, imageId) },
                        onSelectFigure = { occ -> onSelectFigure(question.id, occ) },
                        objectsLocked = objectsLocked,
                        selectedElement = selectedElement?.takeIf { it.first == question.id }
                            ?.let { it.second to it.third },
                        editingElement = editingElement?.takeIf { it.first == question.id }
                            ?.let { it.second to it.third },
                        onSelectElement = { kind, index -> onSelectElement(question.id, kind, index) },
                        onStartEditElement = { kind, index -> onStartEditElement(question.id, kind, index) },
                        onElementText = { kind, index, text -> onElementText(question.id, kind, index, text) },
                        onElementEnter = { kind, index -> onElementEnter(question.id, kind, index) },
                        onImageFreeMove = { onImageFreeMove(question.id) },
                        onMoveImage = { imageId, x, y -> onMoveImage(question.id, imageId, x, y) },
                        onResizeImage = { imageId, w -> onResizeImage(question.id, imageId, w) },
                        onResizeFigure = { occ, w -> onResizeFigure(question.id, occ, w) }
                    )
                }.first().measure(blockConstraints)
            }

            // V63.9 — جریان پیوسته (درخواست کاربر): هر سؤال دقیقاً جایی که
            // قبلی تمام شد شروع می‌شود؛ هیچ فضای خالی تا انتهای صفحه نمی‌ماند.
            // کاغذهای A4 پشت‌سرهم و بدون فاصله فقط «پس‌زمینه»اند و محتوا مثل
            // ورد از مرز صفحه عبور می‌کند.
            val totalContent = placeables.sumOf { it.height } +
                (placeables.size - 1).coerceAtLeast(0) * gapPx
            val pageCount = (((totalContent + marginPx * 2).toFloat() / pageHeightPx) + 0.999f)
                .toInt().coerceAtLeast(1)
            onPageCount(pageCount)

            val chrome = (0 until pageCount).map { pageIndex ->
                subcompose("page-$pageIndex") { WordPaperChrome() }
                    .first().measure(Constraints.fixed(pageWidthPx, pageHeightPx))
            }
            val totalHeight = pageCount * pageHeightPx
            layout(pageWidthPx, totalHeight) {
                chrome.forEachIndexed { pageIndex, paper -> paper.place(0, pageIndex * pageHeightPx) }
                var y = marginPx
                placeables.forEachIndexed { index, placeable ->
                    if (index > 0) y += gapPx
                    placeable.place(marginPx, y)
                    y += placeable.height
                }
            }
        }
    }
}

/** کاغذ سفید A4 با سایه + عنوان بالا و شمارهٔ صفحه پایین (پشت سؤال‌ها). */
@Composable
private fun WordPaperChrome() {
    // V63.8 — کاغذ سفید خالی: بدون سرصفحه/پاصفحه (درخواست کاربر؛ سربرگ و
    // امضا فقط در خروجی چاپ‌اند).
    Box(
        Modifier
            .fillMaxSize()
            .shadow(1.dp)
            .background(Color.White)
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WordQuestionBlock(
    row: Int,
    question: QuestionDraft,
    zoom: Float,
    highlighted: Boolean,
    onSelect: () -> Unit,
    editable: Boolean,
    onTextChange: (String) -> Unit,
    onScoreChange: (String) -> Unit,
    selectedImageId: String?,
    selectedFigureIndex: Int?,
    onSelectImage: (String) -> Unit,
    onSelectFigure: (Int) -> Unit,
    objectsLocked: Boolean,
    selectedElement: Pair<String, Int>?,
    editingElement: Pair<String, Int>?,
    onSelectElement: (String, Int) -> Unit,
    onStartEditElement: (String, Int) -> Unit,
    onElementText: (String, Int, String) -> Unit,
    onElementEnter: (String, Int) -> Unit,
    onImageFreeMove: () -> Unit,
    onMoveImage: (String, Float, Float) -> Unit,
    onResizeImage: (String, Float) -> Unit,
    onResizeFigure: (Int, Float) -> Unit
) {
    // V63.8 — هم‌مقیاسی دقیق با چاپ: چاپ متن را با textSize=fontSizeSp پوینت
    // روی عرض ۵۱۹pt می‌چیند (595-2×38). اینجا همان نسبت روی عرض واقعی صفحه
    // اعمال می‌شود تا «تعداد کلمات هر سطر» در ویرایش و چاپ یکی باشد.
    val printScale = WordPageLayout.mmToDp(WordPageLayout.PAGE_WIDTH_MM - 2f * WordPageLayout.MARGIN_MM, zoom) / 519f
    val fontSize = (question.fontSizeSp.coerceIn(8f, 30f) * printScale).sp
    // V63.2 — بولد/ایتالیک/تراز سؤال روی برگه هم مثل چاپ دیده می‌شوند.
    val weight = if (question.bold) FontWeight.Bold else null
    val style = if (question.italic) FontStyle.Italic else null
    val align = when (question.textAlign) {
        "center" -> TextAlign.Center
        "left" -> TextAlign.Left
        else -> TextAlign.Right
    }
    // V64.6 — سؤال در ویرایشگر مثل متن Word است؛ انتخاب/ویرایش نباید
    // دور کل سؤال یک کادر بسازد. کادر آبی فقط برای خودِ شیء تصویری باقی است.
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        // V63.4 — بدون مداد: انتخاب سؤال، بارم را هم درجا ویرایش‌پذیر می‌کند.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "سؤال $row     (",
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
            if (editable) {
                var scoreDraft by remember(question.id) { mutableStateOf(scoreText(question.score)) }
                BasicTextField(
                    value = scoreDraft,
                    onValueChange = { value ->
                        scoreDraft = value.filter { it.isDigit() || it == '.' }
                        onScoreChange(scoreDraft)
                    },
                    textStyle = TextStyle(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B72B8)
                    ),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0B72B8)),
                    decorationBox = { innerField -> innerField() },
                    modifier = Modifier
                        .width(WordPageLayout.mmToDp(14f, zoom).dp)
                        .background(Color.Transparent)
                )
            } else Text(
                scoreText(question.score),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
            Text(
                " نمره)",
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // V63.1 — شکل/نمودار/جدول درون‌متنی جدا رندر می‌شوند تا دستگیرهٔ
        // تغییر اندازه بگیرند؛ متن/فرمول باقی با NativeMathText قبلی.
        val figureOccurrences = remember(question.id, question.text) {
            FigureCodec.occurrences(question.text)
        }
        var textOnly = question.text
        figureOccurrences.asReversed().forEach { occ ->
            textOnly = textOnly.removeRange(occ.start, occ.endExclusive)
        }
        // V64.3 — ویرایش قطعه‌ای با ابزار core تست‌شده (پیشنهاد بازبینی
        // کاربر): RichTextSplitter.split کل متن را می‌شکند و reconstruct جای
        // فرمول/شکل را دقیقاً حفظ می‌کند — بدون منطق offset دست‌ساز.
        if (editable) {
            val parts = remember(question.id, question.text) {
                ir.exam.app.core.text.RichTextSplitter.split(question.text)
            }
            var figureCursor = 0
            // V64.5.1 — مثل ورد: تکه‌های متن و فرمول «در یک سطر جاری» کنار هم
            // می‌نشینند (FlowRow)؛ قبلاً هر تکه تمام‌عرض بود و متنِ فرمول‌دار
            // به چند جعبهٔ سطری جدا می‌شکست (گزارش کاربر).
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                parts.forEachIndexed { partIndex, part ->
                    when (part) {
                        is ir.exam.app.core.text.RichSegment.Math -> NativeMathText(
                            source = "$" + part.tex + "$",
                            fontSize = fontSize
                        )
                        is ir.exam.app.core.text.RichSegment.Figure -> {
                            val occIndex = figureCursor++
                            ResizableFigure(
                                spec = part.spec,
                                zoom = zoom,
                                selected = selectedFigureIndex == occIndex,
                                onSelect = { onSelectFigure(occIndex) },
                                onResized = { widthMm -> onResizeFigure(occIndex, widthMm) }
                            )
                        }
                        is ir.exam.app.core.text.RichSegment.Text -> {
                            var pieceDraft by remember(question.id, partIndex, parts.size) {
                                mutableStateOf(part.text)
                            }
                            BasicTextField(
                                value = pieceDraft,
                                onValueChange = { value ->
                                    pieceDraft = value
                                    onTextChange(
                                        ir.exam.app.core.text.RichTextSplitter.reconstruct(parts, partIndex, value)
                                    )
                                },
                                textStyle = TextStyle(
                                    fontSize = fontSize,
                                    fontWeight = weight ?: FontWeight.Normal,
                                    fontStyle = style ?: FontStyle.Normal,
                                    textAlign = align
                                ),
                                // V64.5 — مثل ورد: بدون جعبه/پس‌زمینه؛ فقط مکان‌نما.
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0B72B8)),
                                decorationBox = { innerField -> innerField() },
                                // عرض به اندازهٔ محتوا؛ تکهٔ خالی حداقل جا برای مکان‌نما.
                                modifier = Modifier.widthIn(min = 12.dp)
                                    .background(Color.Transparent)
                            )
                        }
                    }
                }
            }
        } else if (textOnly.isNotBlank() || figureOccurrences.isEmpty()) NativeMathText(
            source = textOnly,
            fontSize = fontSize,
            fontWeight = weight,
            fontStyle = style,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
        // V64.2 — در حالت ویرایش، شکل‌ها داخل جریان قطعه‌ای درجا رندر شدند؛
        // این بلوک فقط برای حالت نمایش است وگرنه شکل دوبار دیده می‌شد.
        if (!editable) figureOccurrences.forEachIndexed { occurrenceIndex, occ ->
            ResizableFigure(
                spec = occ.spec,
                zoom = zoom,
                selected = selectedFigureIndex == occurrenceIndex,
                onSelect = { onSelectFigure(occurrenceIndex) },
                onResized = { widthMm -> onResizeFigure(occurrenceIndex, widthMm) }
            )
        }

        // V64.0 — مدل Word-مانند: هر گزینه/سمت جورکردنی «بلوک مستقل» است؛
        // لمس = انتخاب (کادر آبی) و لمس دوم = ویرایش درجای همان عنصر.
        when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> question.options.forEachIndexed { index, option ->
                Row(Modifier.fillMaxWidth().padding(top = (1 * zoom).dp)) {
                    Text("${index + 1}) ", fontSize = fontSize, fontWeight = FontWeight.Bold)
                    WordElement(
                        text = option,
                        // V64.4 — استایل مستقل گزینه؛ null = ارث از سؤال.
                        fontSize = question.optionStyles.getOrNull(index)?.fontSizeSp
                            ?.let { (it.coerceIn(8f, 30f) * printScale).sp } ?: fontSize,
                        weight = if (question.optionStyles.getOrNull(index)?.bold == true) FontWeight.Bold else weight,
                        style = if (question.optionStyles.getOrNull(index)?.italic == true) FontStyle.Italic else style,
                        align = align,
                        selected = selectedElement == ("opt" to index),
                        editing = editingElement == ("opt" to index),
                        onSelect = { onSelectElement("opt", index) },
                        onStartEdit = { onStartEditElement("opt", index) },
                        onText = { onElementText("opt", index, it) },
                        onEnter = { onElementEnter("opt", index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            QuestionType.TRUE_FALSE -> Row(
                Modifier.fillMaxWidth().padding(top = (1 * zoom).dp),
                horizontalArrangement = Arrangement.spacedBy((10 * zoom).dp)
            ) {
                Text("○ صحیح", fontSize = fontSize)
                Text("○ غلط", fontSize = fontSize)
            }
            QuestionType.MATCHING -> {
                val rows = maxOf(question.matchingLeft.size, question.matchingRight.size)
                repeat(rows) { index ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = (1 * zoom).dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WordElement(
                            text = question.matchingRight.getOrNull(index).orEmpty(),
                            fontSize = question.matchingRightStyles.getOrNull(index)?.fontSizeSp
                                ?.let { (it.coerceIn(8f, 30f) * printScale).sp } ?: fontSize,
                            weight = if (question.matchingRightStyles.getOrNull(index)?.bold == true) FontWeight.Bold else weight,
                            style = if (question.matchingRightStyles.getOrNull(index)?.italic == true) FontStyle.Italic else style,
                            align = align,
                            selected = selectedElement == ("mR" to index),
                            editing = editingElement == ("mR" to index),
                            onSelect = { onSelectElement("mR", index) },
                            onStartEdit = { onStartEditElement("mR", index) },
                            onText = { onElementText("mR", index, it) },
                            onEnter = { onElementEnter("mR", index) },
                            modifier = Modifier.weight(1f)
                        )
                        Text("↔", fontSize = fontSize)
                        WordElement(
                            text = question.matchingLeft.getOrNull(index).orEmpty(),
                            fontSize = question.matchingLeftStyles.getOrNull(index)?.fontSizeSp
                                ?.let { (it.coerceIn(8f, 30f) * printScale).sp } ?: fontSize,
                            weight = if (question.matchingLeftStyles.getOrNull(index)?.bold == true) FontWeight.Bold else weight,
                            style = if (question.matchingLeftStyles.getOrNull(index)?.italic == true) FontStyle.Italic else style,
                            align = align,
                            selected = selectedElement == ("mL" to index),
                            editing = editingElement == ("mL" to index),
                            onSelect = { onSelectElement("mL", index) },
                            onStartEdit = { onStartEditElement("mL", index) },
                            onText = { onElementText("mL", index, it) },
                            onEnter = { onElementEnter("mL", index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            else -> Unit
        }

        question.images.forEach { media ->
            DraggableQuestionImage(
                media = media,
                zoom = zoom,
                freePlacement = question.imagePosition == "free",
                selected = selectedImageId == media.id,
                locked = objectsLocked,
                onSelect = { onSelectImage(media.id) },
                // V63.8 — با اولین کشیدن، تصویر خودکار «آزاد» می‌شود.
                onFreeMove = onImageFreeMove,
                onMoved = { xMm, yMm -> onMoveImage(media.id, xMm, yMm) }
            )
        }

        // V64.0 — نقطه‌چین/خط پاسخ از ویرایشگر حذف شد (درخواست کاربر):
        // فضای پاسخ را خود کاربر با اینتر در متن سؤال می‌سازد.
    }
}

/**
 * V63.3 — نوار ابزار واحد ویرایشگر سند (درخواست کاربر): همیشه دیده می‌شود و
 * به چپ/راست اسکرول می‌شود. ترتیب آیکن‌ها: + و − (بزرگ/کوچک‌کردن شیء
 * انتخابی)، جابجایی (راهنمای درگ شیء)، آ+/آ− (متن سؤال انتخابی)، تراز
 * راست/وسط/چپ، بولد/ایتالیک، فلش ترتیب سؤال و ذره‌بین +/− (زوم صفحه).
 * ابزار بدون هدفِ انتخاب‌شده خاموش (غیرفعال) است — همه نماد، بدون متن/کد.
 */
@Composable
private fun DocumentToolbar(
    question: QuestionDraft?,
    hasObject: Boolean,
    hasElement: Boolean,
    hasDeletable: Boolean,
    locked: Boolean,
    onToggleLock: () -> Unit,
    onDeleteSelected: () -> Unit,
    onObjectGrow: () -> Unit,
    onObjectShrink: () -> Unit,
    onFontSize: (Float) -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onAlign: (String) -> Unit,
    onMoveQuestion: (Int) -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    zoom: Float
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F6FA))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // + و − : اندازهٔ شیء انتخابی (تصویر/شکل/نمودار/جدول)
        IconButton(onClick = onObjectGrow, enabled = hasObject) {
            Icon(Icons.Outlined.Add, contentDescription = "بزرگ‌کردن شیء",
                tint = if (hasObject) Color(0xFF0B72B8) else Color(0xFFB2BDC6))
        }
        IconButton(onClick = onObjectShrink, enabled = hasObject) {
            Icon(Icons.Outlined.Remove, contentDescription = "کوچک‌کردن شیء",
                tint = if (hasObject) Color(0xFF0B72B8) else Color(0xFFB2BDC6))
        }
        // V63.9 — قفل جابجایی: بسته=قرمز (اشیا ثابت)، باز=سبز (جابجایی آزاد).
        IconButton(onClick = onToggleLock) {
            Icon(
                if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                contentDescription = if (locked) "بازکردن قفل جابجایی" else "قفل جابجایی",
                tint = if (locked) Color(0xFFC62828) else Color(0xFF25A86B)
            )
        }
        // V64.1 — حذف عنصر/شیء انتخاب‌شده (مثل Delete در ورد).
        IconButton(onClick = onDeleteSelected, enabled = hasDeletable) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "حذف انتخاب‌شده",
                tint = if (hasDeletable) Color(0xFFC62828) else Color(0xFFB2BDC6)
            )
        }
        // V64.4 — با انتخاب عنصر هم فعال (قالب روی عنصر اثر می‌کند).
        val hasQuestion = question != null || hasElement
        TextButton(onClick = { onFontSize(+2f) }, enabled = hasQuestion) { Text("آ+") }
        TextButton(onClick = { onFontSize(-2f) }, enabled = hasQuestion) { Text("آ-") }
        FormatToggle(
            selected = question?.textAlign == "right",
            icon = Icons.Outlined.FormatAlignRight,
            description = "تراز راست",
            onClick = { onAlign("right") }
        )
        FormatToggle(
            selected = question?.textAlign == "center",
            icon = Icons.Outlined.FormatAlignCenter,
            description = "تراز وسط",
            onClick = { onAlign("center") }
        )
        FormatToggle(
            selected = question?.textAlign == "left",
            icon = Icons.Outlined.FormatAlignLeft,
            description = "تراز چپ",
            onClick = { onAlign("left") }
        )
        FormatToggle(
            selected = question?.bold == true,
            icon = Icons.Outlined.FormatBold,
            description = "بولد",
            onClick = onBold
        )
        FormatToggle(
            selected = question?.italic == true,
            icon = Icons.Outlined.FormatItalic,
            description = "ایتالیک",
            onClick = onItalic
        )
        IconButton(onClick = { onMoveQuestion(-1) }, enabled = hasQuestion) {
            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "سؤال بالاتر",
                tint = if (hasQuestion) Color(0xFF44505A) else Color(0xFFB2BDC6))
        }
        IconButton(onClick = { onMoveQuestion(1) }, enabled = hasQuestion) {
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "سؤال پایین‌تر",
                tint = if (hasQuestion) Color(0xFF44505A) else Color(0xFFB2BDC6))
        }
        // ذره‌بین ±: زوم صفحهٔ سؤال‌ها
        IconButton(onClick = onZoomIn) {
            Icon(Icons.Outlined.ZoomIn, contentDescription = "بزرگ‌نمایی صفحه", tint = Color(0xFF44505A))
        }
        Text("${(zoom * 100).toInt()}٪", style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = onZoomOut) {
            Icon(Icons.Outlined.ZoomOut, contentDescription = "کوچک‌نمایی صفحه", tint = Color(0xFF44505A))
        }
    }
}

/** V63.3 — تغییر اندازهٔ شکل انتخابی با گام میلی‌متری از نوار ابزار. */
private fun resizeFigureBy(
    builder: ExamBuilderViewModel,
    questions: List<QuestionDraft>,
    questionId: String,
    occurrenceIndex: Int,
    deltaMm: Float
) {
    val question = questions.firstOrNull { it.id == questionId } ?: return
    val occ = FigureCodec.occurrences(question.text).getOrNull(occurrenceIndex) ?: return
    builder.updateFigure(
        questionId, occurrenceIndex,
        WordPageLayout.withFigureWidthMm(occ.spec, WordPageLayout.figureWidthMm(occ.spec) + deltaMm)
    )
}

/** دکمهٔ قالب با پس‌زمینهٔ آبی وقتی فعال است. */
@Composable
private fun FormatToggle(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = if (selected) Modifier.background(Color(0x3327A5F2), RoundedCornerShape(8.dp)) else Modifier
    ) {
        Icon(icon, contentDescription = description, tint = if (selected) Color(0xFF0B72B8) else Color(0xFF44505A))
    }
}

/**
 * V63.1 — تصویر سؤال روی برگه: کشیدن بدنه جابه‌جا می‌کند (فقط حالت «آزاد»؛
 * حالت‌های سطری فقط اندازه)، کشیدن دستگیرهٔ گوشه اندازه را عوض می‌کند.
 * مقادیر همیشه میلی‌متر ذخیره می‌شوند و مستقیم به چاپ می‌روند.
 */
@Composable
private fun DraggableQuestionImage(
    media: ir.exam.app.ui.builder.MediaDraft,
    zoom: Float,
    freePlacement: Boolean,
    selected: Boolean,
    locked: Boolean,
    onSelect: () -> Unit,
    onFreeMove: () -> Unit,
    onMoved: (Float, Float) -> Unit
) {
    val widthMm = media.widthMm.coerceIn(WordPageLayout.IMAGE_MIN_WIDTH_MM, WordPageLayout.IMAGE_MAX_WIDTH_MM)
    val heightMm = widthMm * 0.6f
    val pxPerMm = with(androidx.compose.ui.platform.LocalDensity.current) {
        WordPageLayout.mmToDp(1f, zoom).dp.toPx()
    }
    // آفست زندهٔ حین درگ (میلی‌متر)؛ با رها شدن انگشت commit می‌شود.
    var dragXmm by remember(media.id) { mutableStateOf(0f) }
    var dragYmm by remember(media.id) { mutableStateOf(0f) }
    val liveWidthMm = widthMm
    // V63.9 — آفست زنده حتی قبل از free شدن هم دیده می‌شود تا درگ واقعاً کار کند.
    val baseXmm = WordPageLayout.clampImageXmm((if (freePlacement) media.xMm else 0f) + dragXmm, liveWidthMm)
    val baseYmm = (if (freePlacement) media.yMm.coerceIn(0f, 60f) else 0f) + dragYmm

    // V63.8 — بدون لکهٔ آبی: لمس = انتخاب؛ شیء انتخاب‌شده با کشیدن انگشت
    // آزادانه جابه‌جا می‌شود (+/− نوار ابزار اندازه را عوض می‌کند).
    Box(
        Modifier
            .padding(top = WordPageLayout.mmToDp(WordPageLayout.MEDIA_GAP_MM / 2f, zoom).dp)
            .offset { IntOffset((baseXmm * pxPerMm).roundToInt(), (baseYmm * pxPerMm).roundToInt()) }
            .width(WordPageLayout.mmToDp(liveWidthMm, zoom).dp)
            .then(
                if (selected) Modifier.border(2.dp, Color(0xFF0B72B8)) else Modifier
            )
            .pointerInput(media.id) { detectTapGestures(onTap = { onSelect() }) }
            .then(
                if (selected && !locked) Modifier.pointerInput(media.id, zoom) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            change.consume()
                            dragXmm += drag.x / pxPerMm
                            dragYmm += drag.y / pxPerMm
                        },
                        onDragEnd = {
                            onFreeMove()
                            onMoved(
                                (media.xMm + dragXmm).coerceIn(0f, 190f),
                                (media.yMm + dragYmm).coerceIn(0f, 270f)
                            )
                            dragXmm = 0f; dragYmm = 0f
                        }
                    )
                } else Modifier
            )
    ) {
        // V63.9 — ارتفاع از نسبت واقعی تصویر (آناتومی و... کامل دیده شوند).
        AsyncImage(
            model = media.uri,
            contentDescription = "تصویر سؤال",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * V63.1 — شکل/نمودار/جدول درون‌متنی با دستگیرهٔ تغییر اندازه؛ عرض جدید داخل
 * X.wmm همان توکن %%FIG%% ذخیره می‌شود و چاپ رسمی همان را می‌خواند.
 */
@Composable
private fun ResizableFigure(
    spec: ir.exam.app.core.figure.FigureSpec,
    zoom: Float,
    selected: Boolean,
    onSelect: () -> Unit,
    onResized: (Float) -> Unit
) {
    // V63.8 — بدون دستگیره/لکهٔ آبی: لمس = انتخاب (کادر آبی)؛ اندازه با
    // +/− نوار ابزار (onResized از آنجا صدا می‌خورد).
    val widthMm = WordPageLayout.figureWidthMm(spec)
    Box(
        Modifier
            .padding(top = WordPageLayout.mmToDp(1.5f, zoom).dp)
            .width(WordPageLayout.mmToDp(widthMm, zoom).dp)
            .then(
                if (selected) Modifier.border(2.dp, Color(0xFF0B72B8)) else Modifier
            )
            .pointerInput(spec.raw) { detectTapGestures(onTap = { onSelect() }) }
    ) {
        // V64.2 — آناتومی/فیزیک (kind a/s) تصویر واقعی می‌خواهند نه SVG برچسبی؛
        // همان مسیر AtlasFigureView که NativeMathText/چاپ استفاده می‌کنند.
        if (spec.kind in setOf("a", "s")) {
            ir.exam.app.ui.figure.AtlasFigureView(
                spec = spec,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = "شکل",
                showBlanks = false
            )
        } else {
            InlineFigureView(spec = spec, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * V64.0 — عنصر Word-مانند: کوچک‌ترین واحد قابل انتخاب سند (گزینهٔ سؤال،
 * سمت راست/چپ جفت جورکردنی). مثل Word: کلیک اول انتخاب (کادر آبی)، کلیک
 * روی عنصرِ انتخاب‌شده = ویرایش درجا؛ فرمول‌های $...$ رندر می‌مانند.
 */
@Composable
private fun WordElement(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight?,
    style: FontStyle?,
    align: TextAlign,
    selected: Boolean,
    editing: Boolean,
    onSelect: () -> Unit,
    onStartEdit: () -> Unit,
    onText: (String) -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    // V64.3 — کنترل‌شده از بالا (editingElement): هیچ state محلی‌ای که با
    // تایپ ریست شود وجود ندارد؛ لمس دوم = onStartEdit برای «هر» عنصری.
    if (editing) {
        var draft by remember(text) { mutableStateOf(text) }
        BasicTextField(
            value = draft,
            onValueChange = { value ->
                // V64.1 — Enter داخل عنصر = پایان این عنصر و ساخت عنصر بعدی
                // (هر گزینه مثل یک پاراگراف ورد تک‌خطی است).
                if ('\n' in value) {
                    val clean = value.replace("\n", "")
                    draft = clean
                    onText(clean)
                    onEnter()
                } else {
                    draft = value
                    onText(value)
                }
            },
            textStyle = TextStyle(
                fontSize = fontSize,
                fontWeight = weight ?: FontWeight.Normal,
                fontStyle = style ?: FontStyle.Normal,
                textAlign = align
            ),
            // V64.5/V64.6 — مثل ورد: کادر و پس‌زمینه حذف؛ فقط مکان‌نمای چشمک‌زن.
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0B72B8)),
            decorationBox = { innerField -> innerField() },
            modifier = modifier.background(Color.Transparent)
        )
    } else {
        Box(
            // V64.6 — متن انتخاب‌شده هم کادر ندارد؛ مکان‌نما/خود متن
            // بازخورد انتخاب است و ابزار نوار بالا روی همان عنصر کار می‌کند.
            modifier.pointerInput(selected) {
                    detectTapGestures(onTap = {
                        // V64.5 — مثل ورد: یک کلیک روی متن = مکان‌نما (انتخاب+ویرایش).
                        onSelect()
                        onStartEdit()
                    })
                }
        ) {
            NativeMathText(
                source = text.ifBlank { " " },
                fontSize = fontSize,
                fontWeight = weight,
                fontStyle = style,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** V64.5 — اعمال استایل روی عنصر انتخابی: گزینه یا سمت جورکردنی. */
private fun applyElementStyle(
    builder: ExamBuilderViewModel,
    element: Triple<String, String, Int>,
    change: (ir.exam.app.ui.builder.OptionStyle) -> ir.exam.app.ui.builder.OptionStyle
) {
    when (element.second) {
        "opt" -> builder.setOptionStyle(element.first, element.third, change)
        "mL" -> builder.setMatchingStyle(element.first, "left", element.third, change)
        "mR" -> builder.setMatchingStyle(element.first, "right", element.third, change)
    }
}

/** بارم بدون نقطهٔ اضافی: ۲ یا ۱٫۵ */
private fun scoreText(score: Double): String =
    if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()

