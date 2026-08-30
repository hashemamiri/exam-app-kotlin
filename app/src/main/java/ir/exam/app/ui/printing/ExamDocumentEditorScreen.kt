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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import ir.exam.app.ui.builder.StyleSpanOps
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
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import ir.exam.app.R
import ir.exam.app.core.printing.UnifiedDocumentEngine

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
    // V68 — بازهٔ انتخاب‌شدهٔ متن (شروع/پایان انحصاری در متن کامل سؤالِ در ویرایش).
    var textRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
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
            // V68 — مثل ورد: با انتخاب بازه‌ای، بولد فقط همان تکهٔ متن را
            // می‌پوشاند (استایل تکه‌ای؛ فقط چیدمان چاپ).
            onBold = {
                val range = textRange
                if (editing != null && range != null && range.second > range.first) {
                    builder.setQuestionSpans(
                        editing!!.id,
                        StyleSpanOps.toggle(editing!!.textSpans, range.first, range.second, bold = true)
                    )
                } else {
                    val element = selectedElement
                    if (element != null) applyElementStyle(builder, element) { it.copy(bold = !it.bold) }
                    else editing?.let { builder.setQuestionBold(it.id, !it.bold) }
                }
            },
            onItalic = {
                val range = textRange
                if (editing != null && range != null && range.second > range.first) {
                    builder.setQuestionSpans(
                        editing!!.id,
                        StyleSpanOps.toggle(editing!!.textSpans, range.first, range.second, italic = true)
                    )
                } else {
                    val element = selectedElement
                    if (element != null) applyElementStyle(builder, element) { it.copy(italic = !it.italic) }
                    else editing?.let { builder.setQuestionItalic(it.id, !it.italic) }
                }
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
                // V68.9 — سطر «درس/مدت/بارم» بالای سند از اطلاعات خود آزمون.
                subject = state.subject,
                durationMinutes = state.durationMinutes.toIntOrNull() ?: 0,
                zoom = zoom,
                // V68 — زوم دو-انگشتی + دوبار-لمس = ۱۰۰٪ (مثل Ctrl+چرخ ورد).
                onZoom = { factor -> zoom = (zoom * factor).coerceIn(0.6f, 3f) },
                onResetZoom = { zoom = 1f },
                onTextRangeChange = { s, e ->
                    textRange = if (s != null && e != null && e > s) s to e else null
                },
                editingQuestionId = editingQuestionId,
                onPageCount = { measuredPageCount = it },
                onSelectQuestion = { id ->
                    editingQuestionId = if (editingQuestionId == id) null else id
                    textRange = null
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
                },
                // V68.4 — جابه‌جایی آزاد شکل/نمودار/جدول: موقعیت مطلق نسبت به
                // بالا-چپ بلوک همان سؤال در X.fx/X.fy توکن %%FIG%% ذخیره می‌شود.
                onMoveFigure = { questionId, occurrenceIndex, xMm, yMm ->
                    val question = state.questions.firstOrNull { it.id == questionId }
                    val occ = question?.let { FigureCodec.occurrences(it.text).getOrNull(occurrenceIndex) }
                    if (occ != null) builder.updateFigure(
                        questionId, occurrenceIndex,
                        WordPageLayout.withFigurePosMm(occ.spec, xMm, yMm)
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
    // V68.9 — سطر «درس/مدت/بارم» بالای سند از اطلاعات خود آزمون.
    subject: String,
    durationMinutes: Int,
    zoom: Float,
    onZoom: (Float) -> Unit,
    onResetZoom: () -> Unit,
    onTextRangeChange: (Int?, Int?) -> Unit,
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
    onResizeFigure: (String, Int, Float) -> Unit,
    onMoveFigure: (String, Int, Float, Float) -> Unit
) {
    val scroll = rememberScrollState()
    // V68 — زوم دو-انگشتی (pinch) بدون شکستن اسکرول تک‌انگشتی.
    val zoomState = rememberTransformableState { zoomChange, _, _ -> onZoom(zoomChange) }
    // V68.8 — درگ آزاد واقعی (گزارش کاربر: «حرکت تصویر گالری آزادانه نیست»):
    // کل سند داخل verticalScroll است و ژست عمودیِ درگِ شیء توسط اسکرول صفحه
    // دزدیده می‌شد؛ وقتی یک تصویر/شکل انتخاب است اسکرول موقتاً غیرفعال می‌شود
    // تا کشیدن در هر دو محور به خودِ شیء برسد. با لغو انتخاب، اسکرول برمی‌گردد.
    val scrollEnabled = selectedImageId == null && selectedFigure == null
    // V68.9 — موتور واحد: همان چیدمان/فونت/شکست خط/جای اشیایی که چاپ رسمی
    // استفاده می‌کند، اینجا کاغذهای ویرایشگر را می‌کشد؛ فقط سؤالِ در حال
    // ویرایش به‌صورت Compose (قابل تایپ/درگ) روی همان نقطه می‌نشیند.
    val context = LocalContext.current
    val engine = remember(context) { UnifiedDocumentEngine(context.applicationContext) }
    // V68.9 — امضای تصاویر فقط با تغییر id/uri عوض می‌شود (نه با هر تایپ) تا
    // cache بیت‌مایپ‌ها بی‌دلیل خالی و دوباره decode نشود.
    val imageSignature = remember(questions) {
        questions.flatMap { q -> q.images.map { it.id + "@" + it.uri } }.joinToString("|")
    }
    val imageBits = remember(imageSignature) { mutableStateMapOf<String, Bitmap>() }
    var imageBitsVersion by remember { mutableStateOf(0) }
    LaunchedEffect(imageSignature) {
        questions.forEach { q -> q.images.forEach { media ->
            if (!imageBits.containsKey(media.id)) {
                decodeGalleryImage(context, media.uri)?.let {
                    imageBits[media.id] = it; imageBitsVersion += 1
                }
            }
        } }
    }
    val printable = remember(questions, subject, durationMinutes) {
        engine.printableFromDrafts(questions, subject, durationMinutes)
    }
    val document = remember(printable, imageBitsVersion) {
        engine.layoutExamForEditor(printable, imageBits.toMap())
    }
    val editingIndex = remember(questions, editingQuestionId) {
        questions.indexOfFirst { it.id == editingQuestionId }.takeIf { it >= 0 }
    }
    // V68.9 — sp مثل pt چاپ: بزرگ‌نمایی فونتِ سیستم، موتور واحد را به‌هم
    // نمی‌زند (fontScale=1 فقط برای خود سند؛ dp/زوم بی‌تغییر).
    val screenDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(screenDensity.density, fontScale = 1f)
    ) {
    Column(
        // V68 — imePadding: مکان‌نا هنگام باز بودن کیبورد زیر آن گم نمی‌شود.
        Modifier.fillMaxSize().verticalScroll(scroll, enabled = scrollEnabled).imePadding().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeLayout(
            Modifier
                .transformable(zoomState)
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onResetZoom() }) }
        ) { constraints ->
            val pageWidthPx = WordPageLayout.mmToDp(WordPageLayout.PAGE_WIDTH_MM, zoom).dp.roundToPx()
            val pageHeightPx = WordPageLayout.mmToDp(WordPageLayout.PAGE_HEIGHT_MM, zoom).dp.roundToPx()
            val marginPx = WordPageLayout.mmToDp(WordPageLayout.MARGIN_MM, zoom).dp.roundToPx()

            val contentWidth = pageWidthPx - marginPx * 2
            // V68.9 — هر pt سند چاپ = همین نسبت روی صفحه.
            val pxPerPt = pageWidthPx / UnifiedDocumentEngine.PAGE_WIDTH.toFloat()

            // V68.9 — شمار صفحه از خود موتور می‌آید: همان برش‌هایی که چاپ
            // می‌شوند (بدون رزرو سربرگ در صفحهٔ ۱ — تصمیم کاربر).
            val pageCount = document.pageCount
            onPageCount(pageCount)

            val chrome = (0 until pageCount).map { pageIndex ->
                subcompose("page-$pageIndex") { WordPaperChrome() }
                    .first().measure(Constraints.fixed(pageWidthPx, pageHeightPx))
            }
            // V68.9 — کاغذِ محتوا با خود موتور چاپ رسم می‌شود؛ لمس کاغذ هم
            // از hitTest موتور به سؤال/تصویر/شکل نگاشت می‌شود.
            val papers = (0 until pageCount).map { pageIndex ->
                subcompose("engine-page-$pageIndex") {
                    EnginePageView(
                        engine = engine,
                        document = document,
                        pageIndex = pageIndex,
                        pxPerPt = pxPerPt,
                        skipQuestion = editingIndex,
                        onTap = { xPt, yPt ->
                            val hit = engine.hitTest(document, pageIndex, xPt, yPt)
                                ?: return@EnginePageView
                            val hitQuestion = questions.getOrNull(hit.questionIndex)
                                ?: return@EnginePageView
                            onSelectQuestion(hitQuestion.id)
                            if (hit.galleryImageKey != null) {
                                onSelectImage(hitQuestion.id, hit.galleryImageKey!!)
                            }
                            hit.figureOccurrence?.let { occ -> onSelectFigure(hitQuestion.id, occ) }
                        }
                    )
                }.first().measure(Constraints.fixed(pageWidthPx, pageHeightPx))
            }
            // V68.9.2 — لایهٔ اشیای تصویری هر صفحه: شکل/جدول/آناتومی/تصویر با
            // Compose روی همان مختصات موتور (راستی‌آزمایی‌شده با چاپ).
            val objectLayers = (0 until pageCount).map { pageIndex ->
                subcompose("objects-$pageIndex") {
                    EngineObjectsLayer(
                        engine = engine,
                        document = document,
                        pageIndex = pageIndex,
                        pxPerPt = pxPerPt,
                        skipQuestion = editingIndex
                    )
                }.first().measure(Constraints.fixed(pageWidthPx, pageHeightPx))
            }
            // V68.9 — فقط سؤالِ در حال ویرایش، Compose است (تایپ/درگ/انتخاب
            // درجا) و دقیقاً روی جای خودش در سند موتور می‌نشیند؛ بقیهٔ سند
            // از موتور واحد می‌آید → آنچه می‌بینی همان است که چاپ می‌شود.
            val overlay = editingIndex?.let { index ->
                subcompose("q-${questions[index].id}") {
                    val question = questions[index]
                    WordQuestionBlock(
                        row = index + 1,
                        question = question,
                        zoom = zoom,
                        highlighted = editingQuestionId == question.id,
                        onSelect = { onSelectQuestion(question.id) },
                        editable = editingQuestionId == question.id,
                        onTextChange = { onTextChange(question.id, it) },
                        onTextRangeChange = onTextRangeChange,
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
                        onResizeFigure = { occ, w -> onResizeFigure(question.id, occ, w) },
                        onMoveFigure = { occ, x, y -> onMoveFigure(question.id, occ, x, y) }
                    )
                }.first().measure(Constraints(maxWidth = contentWidth))
            }
            val totalHeight = pageCount * pageHeightPx
            layout(pageWidthPx, totalHeight) {
                chrome.forEachIndexed { pageIndex, paper -> paper.place(0, pageIndex * pageHeightPx) }
                papers.forEachIndexed { pageIndex, paper -> paper.place(0, pageIndex * pageHeightPx) }
                // V68.9.2 — اشیای تصویری (شکل/جدول/آناتومی/تصویر گالری) با
                // Compose روی همان مستطیل موتور (WYSIWYG) رسم می‌شوند.
                objectLayers.forEachIndexed { pageIndex, layer ->
                    layer.place(0, pageIndex * pageHeightPx)
                }
                val overlayIndex = editingIndex
                if (overlay != null && overlayIndex != null) {
                    // V68.9.2 — فیکس ناپدیدشدن سؤال در حال ویرایش: جای سؤال
                    // باید «داخل صفحهٔ خودش» حساب شود (قبلاً y = origin*pxPerPt
                    // بدون firstTop و شمارهٔ صفحه بود؛ برای سؤال‌های صفحهٔ ۲+
                    // overlay بیرون از کاغذ می‌افتاد و چون موتور هم همان سؤال
                    // را skip می‌کرد، کل محتوایش ناپدید می‌شد).
                    val originPt = document.questionOriginPt(overlayIndex)
                    val sliceIndex = document.slices.indexOfFirst { s ->
                        originPt >= s.first && originPt < s.second
                    }.let { if (it >= 0) it else 0 }
                    val slice = document.slices[sliceIndex]
                    val dstTopPt = if (sliceIndex == 0) document.firstTop
                    else UnifiedDocumentEngine.LATER_CONTENT_TOP
                    val yPx = sliceIndex * pageHeightPx +
                        ((dstTopPt + (originPt - slice.first)) * pxPerPt).roundToInt()
                    overlay.place(marginPx, yPx)
                }
            }
        }
    }
    }
}

/**
 * V68.9 — یک کاغذ A4 که «خود موتور چاپ» آن را رسم می‌کند (مقیاس pxPerPt):
 * فونت، شکست خط، کادرها، خطوط پاسخ و جای تصاویر/شکل‌ها دقیقاً همان چاپ.
 * لمس کاغذ هم از طریق hitTest موتور به سؤال/تصویر/شکل نگاشت می‌شود.
 */
@Composable
private fun EnginePageView(
    engine: UnifiedDocumentEngine,
    document: UnifiedDocumentEngine.EngineDocument,
    pageIndex: Int,
    pxPerPt: Float,
    skipQuestion: Int?,
    onTap: (Float, Float) -> Unit
) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(document, pageIndex) {
                detectTapGestures { offset -> onTap(offset.x / pxPerPt, offset.y / pxPerPt) }
            }
    ) {
        // V68.9.1 — تابع کمکی مخصوص draw-into-canvas در نسخهٔ Compose پروژه
        // نیست؛ مسیر استاندارد DrawScope: drawContext.canvas + nativeCanvas.
        val native = drawContext.canvas.nativeCanvas
        native.save()
        native.scale(pxPerPt, pxPerPt)
        engine.drawEditorPage(native, document, pageIndex, skipQuestion)
        native.restore()
    }
}

/**
 * V68.9.2 — لایهٔ اشیای تصویری یک کاغذ ویرایشگر: هر شکل/جدول/آناتومی/تصویرِ
 * سند با «همان بیت‌مایپ و همان مستطیل موتور چاپ» ولی با Compose رسم می‌شود
 * (گزارش کاربر روی V68.9.1: «جدول و آناتومی و شکل در ویرایشگر نیست»).
 * لمس به Canvas زیر می‌رسد (انتخاب از hitTest موتور)؛ درگ روی سؤالِ در حال
 * ویرایش (overlay) انجام می‌شود.
 */
@Composable
private fun EngineObjectsLayer(
    engine: UnifiedDocumentEngine,
    document: UnifiedDocumentEngine.EngineDocument,
    pageIndex: Int,
    pxPerPt: Float,
    skipQuestion: Int?
) {
    val slice = document.slices.getOrNull(pageIndex) ?: return
    val dstTopPt = if (pageIndex == 0) document.firstTop
    else UnifiedDocumentEngine.LATER_CONTENT_TOP
    val objects = remember(document, pageIndex) { engine.editorObjects(document) }
    // V68.9.3 — px خام نمی‌شود مستقیم به Modifier.size داد (Dp می‌خواهد)؛
    // با چگالی محلی (fontScale=1 سند) تبدیل می‌شود؛ round-trip پیکسل‌دقیق.
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize().clipToBounds()) {
        objects.forEach { obj ->
            // سؤالِ در حال ویرایش: اشیایش از overlay تعاملی (Compose) می‌آیند؛
            // اینجا دوبار رسم نشوند.
            if (obj.questionIndex == skipQuestion) return@forEach
            if (obj.rect.top >= slice.second || obj.rect.bottom <= slice.first) return@forEach
            val leftPx = (obj.rect.left * pxPerPt).roundToInt()
            val topPx = ((dstTopPt + (obj.rect.top - slice.first)) * pxPerPt).roundToInt()
            val widthPx = (obj.rect.width() * pxPerPt).roundToInt().coerceAtLeast(1)
            val heightPx = (obj.rect.height() * pxPerPt).roundToInt().coerceAtLeast(1)
            Image(
                bitmap = obj.bitmap.asImageBitmap(),
                contentDescription = "شکل",
                modifier = Modifier
                    .offset { IntOffset(leftPx, topPx) }
                    .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
            )
        }
    }
}

/** V68.9 — فونت سؤال در ویرایشگر همان فونت چاپ: از نام خانواده به FontFamily. */
private fun draftFontFamily(family: String?): FontFamily? = when (family?.lowercase()) {
    "vazir", "vazirmatn" -> FontFamily(
        Font(R.font.vazirmatn_regular, FontWeight.Normal),
        Font(R.font.vazirmatn_bold, FontWeight.Bold)
    )
    "shabnam" -> FontFamily(Font(R.font.shabnam_regular))
    "sahel" -> FontFamily(Font(R.font.sahel_regular))
    else -> null
}

/** V68.9 — decode تصویر گالری برای موتور واحد (content/file uri؛ حداکثر ~1400px). */
private fun decodeGalleryImage(context: android.content.Context, uri: String, maxPx: Int = 1400): Bitmap? = runCatching {
    val resolver = context.contentResolver
    val parsed = android.net.Uri.parse(uri)
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(parsed)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxPx) sample *= 2
    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
    resolver.openInputStream(parsed)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
}.getOrNull()

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
    onTextRangeChange: (Int?, Int?) -> Unit,
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
    onResizeFigure: (Int, Float) -> Unit,
    onMoveFigure: (Int, Float, Float) -> Unit
) {
    // V63.8 — هم‌مقیاسی دقیق با چاپ: چاپ متن را با textSize=fontSizeSp پوینت
    // روی عرض ۵۱۹pt می‌چیند (595-2×38). اینجا همان نسبت روی عرض واقعی صفحه
    // اعمال می‌شود تا «تعداد کلمات هر سطر» در ویرایش و چاپ یکی باشد.
    val printScale = WordPageLayout.mmToDp(WordPageLayout.PAGE_WIDTH_MM - 2f * WordPageLayout.MARGIN_MM, zoom) / 519f
    val fontSize = (question.fontSizeSp.coerceIn(8f, 30f) * printScale).sp
    // V68.9 — فونتِ چاپ همان فونتِ ویرایشگر: انتخاب «وزیر/شبام/سهل» سؤال در هر
    // دو یکسان اعمال می‌شود (قبلاً فقط چاپ اعمال می‌کرد).
    val family = remember(question.fontFamily) { draftFontFamily(question.fontFamily) }
    // V63.2 — بولد/ایتالیک/تراز سؤال روی برگه هم مثل چاپ دیده می‌شوند.
    val weight = if (question.bold) FontWeight.Bold else null
    val style = if (question.italic) FontStyle.Italic else null
    val align = when (question.textAlign) {
        "center" -> TextAlign.Center
        "left" -> TextAlign.Left
        else -> TextAlign.Right
    }
    // V68 — قطعه‌بندی و بازهٔ آفست هر قطعه (برای انتخاب بازه‌ای و استایل تکه‌ای).
    val formulas = remember(question.id, question.text) {
        ir.exam.app.core.math.FormulaTextCodec.occurrences(question.text)
    }
    val parts = remember(question.id, question.text) {
        ir.exam.app.core.text.RichTextSplitter.split(question.text)
    }
    val segRanges = remember(question.id, question.text) {
        ir.exam.app.core.text.RichTextSplitter.segmentSourceRanges(
            parts, formulas, FigureCodec.occurrences(question.text)
        )
    }
    // V68 — یک‌لمسی مثل ورد: لمس هر جای سؤال، نزدیک‌ترین تکهٔ متنیِ همان
    // نقطه را فوکوس می‌کند تا مکان‌نا همان‌جا بنشیند.
    val segmentBounds = remember(question.id) { mutableStateMapOf<Int, Rect>() }
    val segmentFocusers = remember(question.id) { mutableStateMapOf<Int, FocusRequester>() }
    var blockCoords by remember(question.id) { mutableStateOf<LayoutCoordinates?>(null) }
    // V68.4 — محدودیت حرکت آزاد هر شیء به «همان بلوک سؤال»:
    // ارتفاع بلوک (mm) + جای طبیعی (mm از بالا-چپ بلوک) هر شکل/تصویر.
    val pxPerMm = with(androidx.compose.ui.platform.LocalDensity.current) {
        WordPageLayout.mmToDp(1f, zoom).dp.toPx()
    }
    var blockHeightMm by remember(question.id) { mutableStateOf(0f) }
    val figureAnchors = remember(question.id) { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val imageSlotTops = remember(question.id) { mutableStateMapOf<String, Float>() }
    var pendingFocusPart by remember(question.id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(editable, pendingFocusPart) {
        if (editable) {
            val target = pendingFocusPart
                ?: parts.indexOfFirst { it is ir.exam.app.core.text.RichSegment.Text }.takeIf { it >= 0 }
            if (target != null) runCatching { segmentFocusers[target]?.requestFocus() }
        }
    }
    // V64.6 — سؤال در ویرایشگر مثل متن Word است؛ انتخاب/ویرایش نباید
    // دور کل سؤال یک کادر بسازد. کادر آبی فقط برای خودِ شیء تصویری باقی است.
    Column(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { blockCoords = it
                // V68.4 — سقف حرکت آزاد اشیا = ارتفاع خودِ بلوک سؤال.
                blockHeightMm = it.size.height / pxPerMm
            }
            .pointerInput(question.id) {
                detectTapGestures(onTap = { pos ->
                    onSelect()
                    val root = blockCoords?.localToRoot(pos) ?: return@detectTapGestures
                    pendingFocusPart = segmentBounds.entries
                        .filter { it.value.contains(root) }
                        .minByOrNull { it.key }?.key
                        ?: segmentBounds.keys.minOrNull()
                })
            }
    ) {
        // V63.4 — بدون مداد: انتخاب سؤال، بارم را هم درجا ویرایش‌پذیر می‌کند.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "سؤال $row     (",
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = family
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
                        fontFamily = family,
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
                fontWeight = FontWeight.Bold,
                fontFamily = family
            )
            Text(
                " نمره)",
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = family,
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
                            fontSize = fontSize,
                            fontFamily = family
                        )
                        is ir.exam.app.core.text.RichSegment.Figure -> {
                            val occIndex = figureCursor++
                            // V68.4 — لنگرِ جای طبیعی (بدون آفست) در جریان متن؛
                            // شکل اسلات خود را رزرو می‌کند و آفست بصری آزاد است.
                            Box(
                                Modifier.onGloballyPositioned { c ->
                                    val block = blockCoords
                                    if (block != null) figureAnchors[occIndex] =
                                        ((c.positionInRoot().x - block.positionInRoot().x) / pxPerMm) to
                                            ((c.positionInRoot().y - block.positionInRoot().y) / pxPerMm)
                                }
                            ) {
                                ResizableFigure(
                                    spec = part.spec,
                                    zoom = zoom,
                                    selected = selectedFigureIndex == occIndex,
                                    locked = objectsLocked,
                                    anchorPosMm = figureAnchors[occIndex] ?: (0f to 0f),
                                    boundsHeightMm = blockHeightMm,
                                    onMove = { xMm, yMm -> onMoveFigure(occIndex, xMm, yMm) },
                                    onSelect = { onSelectFigure(occIndex) },
                                    onResized = { widthMm -> onResizeFigure(occIndex, widthMm) }
                                )
                            }
                        }
                        is ir.exam.app.core.text.RichSegment.Text -> {
                            val segRange = segRanges.getOrNull(partIndex)
                            // V68 — مقدار TextFieldValue: انتخاب بازه‌ای هم در
                            // دسترس است (هایلایت خود BasicTextField مثل ورد).
                            var segmentValue by remember(question.id, partIndex, parts.size) {
                                mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(part.text))
                            }
                            LaunchedEffect(part.text) {
                                if (segmentValue.text != part.text) {
                                    segmentValue = androidx.compose.ui.text.input.TextFieldValue(part.text)
                                }
                            }
                            val requester = remember(partIndex, parts.size) { FocusRequester() }
                            segmentFocusers[partIndex] = requester
                            // V68 — بازه‌های استایل تکه‌ای محلی همین قطعه.
                            val (localBold, localItalic) = remember(
                                question.id, partIndex, question.textSpans, parts.size
                            ) {
                                val bolds = mutableListOf<Pair<Int, Int>>()
                                val italics = mutableListOf<Pair<Int, Int>>()
                                question.textSpans.forEach { sp ->
                                    if (segRange != null) {
                                        val s = (sp.start - segRange.first).coerceIn(0, part.text.length)
                                        val e = (sp.end - segRange.first).coerceIn(0, part.text.length)
                                        if (e > s) {
                                            if (sp.bold) bolds += s to e
                                            if (sp.italic) italics += s to e
                                        }
                                    }
                                }
                                bolds to italics
                            }
                            val fieldValue =
                                if (localBold.isEmpty() && localItalic.isEmpty()) segmentValue
                                else androidx.compose.ui.text.input.TextFieldValue(
                                    androidx.compose.ui.text.buildAnnotatedString {
                                        append(segmentValue.text)
                                        localBold.forEach { (s, e) ->
                                            addStyle(
                                                androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                                                s, e
                                            )
                                        }
                                        localItalic.forEach { (s, e) ->
                                            addStyle(
                                                androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic),
                                                s, e
                                            )
                                        }
                                    },
                                    segmentValue.selection
                                )
                            BasicTextField(
                                value = fieldValue,
                                onValueChange = { newValue ->
                                    segmentValue = newValue
                                    val value = newValue.text
                                    onTextChange(
                                        ir.exam.app.core.text.RichTextSplitter.reconstruct(parts, partIndex, value)
                                    )
                                    // V68 — گزارش بازهٔ انتخاب‌شده به نوار ابزار.
                                    val sel = newValue.selection
                                    val base = segRange?.first ?: 0
                                    if (!sel.collapsed) {
                                        onTextRangeChange(
                                            base + minOf(sel.min, sel.max),
                                            base + maxOf(sel.min, sel.max)
                                        )
                                    } else onTextRangeChange(null, null)
                                },
                                textStyle = TextStyle(
                                    fontSize = fontSize,
                                    fontWeight = weight ?: FontWeight.Normal,
                                    fontStyle = style ?: FontStyle.Normal,
                                    fontFamily = family,
                                    textAlign = align
                                ),
                                // V64.5 — مثل ورد: بدون جعبه/پس‌زمینه؛ فقط مکان‌نما.
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0B72B8)),
                                decorationBox = { innerField -> innerField() },
                                // عرض به اندازهٔ محتوا؛ تکهٔ خالی حداقل جا برای مکان‌نما.
                                modifier = Modifier.widthIn(min = 12.dp)
                                    .background(Color.Transparent)
                                    .focusRequester(requester)
                                    .onGloballyPositioned { c ->
                                        if (segRange != null) segmentBounds[partIndex] = c.boundsInRoot()
                                    }
                            )
                        }
                    }
                }
            }
        } else if (question.textSpans.isNotEmpty() && textOnly.isNotBlank()) {
            // V68 — نمایش استایل تکه‌ای در حالت غیر ویرایش: تکه‌های بولد/ایتالیک
            // مثل ورد دیده می‌شوند (همان ساختار جریان قطعه‌ای ویرایش).
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                parts.forEachIndexed { partIndex, part ->
                    when (part) {
                        is ir.exam.app.core.text.RichSegment.Math -> NativeMathText(
                            source = "$" + part.tex + "$",
                            fontSize = fontSize,
                            fontFamily = family
                        )
                        is ir.exam.app.core.text.RichSegment.Figure -> Unit
                        is ir.exam.app.core.text.RichSegment.Text -> {
                            val segRange = segRanges.getOrNull(partIndex)
                            if (segRange != null) {
                                StyleSpanOps.splitBySpans(part.text, segRange.first, question.textSpans)
                                    .forEach { piece ->
                                        if (piece.first.isEmpty()) return@forEach
                                        NativeMathText(
                                            source = piece.first,
                                            fontSize = fontSize,
                                            fontWeight = if (piece.second) FontWeight.Bold else weight,
                                            fontStyle = if (piece.third) FontStyle.Italic else style,
                                            fontFamily = family
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        } else if (textOnly.isNotBlank() || figureOccurrences.isEmpty()) NativeMathText(
            source = textOnly,
            fontSize = fontSize,
            fontWeight = weight,
            fontStyle = style,
            fontFamily = family,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
        // V64.2 — در حالت ویرایش، شکل‌ها داخل جریان قطعه‌ای درجا رندر شدند؛
        // این بلوک فقط برای حالت نمایش است وگرنه شکل دوبار دیده می‌شد.
        if (!editable) figureOccurrences.forEachIndexed { occurrenceIndex, occ ->
            // V68.4 — همان لنگر طبیعی برای شاخهٔ نمایش (بدون ویرایش متن).
            Box(
                Modifier.onGloballyPositioned { c ->
                    val block = blockCoords
                    if (block != null) figureAnchors[occurrenceIndex] =
                        ((c.positionInRoot().x - block.positionInRoot().x) / pxPerMm) to
                            ((c.positionInRoot().y - block.positionInRoot().y) / pxPerMm)
                }
            ) {
                ResizableFigure(
                    spec = occ.spec,
                    zoom = zoom,
                    selected = selectedFigureIndex == occurrenceIndex,
                    locked = objectsLocked,
                    anchorPosMm = figureAnchors[occurrenceIndex] ?: (0f to 0f),
                    boundsHeightMm = blockHeightMm,
                    onMove = { xMm, yMm -> onMoveFigure(occurrenceIndex, xMm, yMm) },
                    onSelect = { onSelectFigure(occurrenceIndex) },
                    onResized = { widthMm -> onResizeFigure(occurrenceIndex, widthMm) }
                )
            }
        }

        // V64.0 — مدل Word-مانند: هر گزینه/سمت جورکردنی «بلوک مستقل» است؛
        // لمس = انتخاب (کادر آبی) و لمس دوم = ویرایش درجای همان عنصر.
        when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> question.options.forEachIndexed { index, option ->
                Row(Modifier.fillMaxWidth().padding(top = (1 * zoom).dp)) {
                    Text("${index + 1}) ", fontSize = fontSize, fontWeight = FontWeight.Bold, fontFamily = family)
                    WordElement(
                        text = option,
                        // V64.4 — استایل مستقل گزینه؛ null = ارث از سؤال.
                        fontSize = question.optionStyles.getOrNull(index)?.fontSizeSp
                            ?.let { (it.coerceIn(8f, 30f) * printScale).sp } ?: fontSize,
                        weight = if (question.optionStyles.getOrNull(index)?.bold == true) FontWeight.Bold else weight,
                        style = if (question.optionStyles.getOrNull(index)?.italic == true) FontStyle.Italic else style,
                        align = align,
                        fontFamily = family,
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
                Text("○ صحیح", fontSize = fontSize, fontFamily = family)
                Text("○ غلط", fontSize = fontSize, fontFamily = family)
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
                            fontFamily = family,
                            selected = selectedElement == ("mR" to index),
                            editing = editingElement == ("mR" to index),
                            onSelect = { onSelectElement("mR", index) },
                            onStartEdit = { onStartEditElement("mR", index) },
                            onText = { onElementText("mR", index, it) },
                            onEnter = { onElementEnter("mR", index) },
                            modifier = Modifier.weight(1f)
                        )
                        Text("↔", fontSize = fontSize, fontFamily = family)
                        WordElement(
                            text = question.matchingLeft.getOrNull(index).orEmpty(),
                            fontSize = question.matchingLeftStyles.getOrNull(index)?.fontSizeSp
                                ?.let { (it.coerceIn(8f, 30f) * printScale).sp } ?: fontSize,
                            weight = if (question.matchingLeftStyles.getOrNull(index)?.bold == true) FontWeight.Bold else weight,
                            style = if (question.matchingLeftStyles.getOrNull(index)?.italic == true) FontStyle.Italic else style,
                            align = align,
                            fontFamily = family,
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
            // V68.4 — اسلات طبیعی تصویر (انتهای بلوک) با این wrapper اندازه‌گیری
            // می‌شود تا آفست ذخیره‌شده (yMm) به مختصات مطلق بلوک تبدیل و clamp شود.
            Box(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { c ->
                        val block = blockCoords
                        if (block != null) imageSlotTops[media.id] =
                            (c.positionInRoot().y - block.positionInRoot().y) / pxPerMm
                    }
            ) {
                DraggableQuestionImage(
                    media = media,
                    zoom = zoom,
                    freePlacement = question.imagePosition == "free",
                    selected = selectedImageId == media.id,
                    locked = objectsLocked,
                    anchorTopMm = imageSlotTops[media.id] ?: 0f,
                    boundsHeightMm = blockHeightMm,
                    onSelect = { onSelectImage(media.id) },
                    // V68 — دستگیرهٔ گوشه: اندازه با کشیدن همان‌جا عوض می‌شود.
                    onResize = { w -> onResizeImage(media.id, w) },
                    // V63.8 — با اولین کشیدن، تصویر خودکار «آزاد» می‌شود.
                    onFreeMove = onImageFreeMove,
                    onMoved = { xMm, yMm -> onMoveImage(media.id, xMm, yMm) }
                )
            }
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
    hasElement: Boolean,
    hasDeletable: Boolean,
    locked: Boolean,
    onToggleLock: () -> Unit,
    onDeleteSelected: () -> Unit,
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
    anchorTopMm: Float,
    boundsHeightMm: Float,
    onSelect: () -> Unit,
    onResize: (Float) -> Unit,
    onFreeMove: () -> Unit,
    onMoved: (Float, Float) -> Unit
) {
    val widthMm = media.widthMm.coerceIn(WordPageLayout.IMAGE_MIN_WIDTH_MM, WordPageLayout.IMAGE_MAX_WIDTH_MM)
    // V68 — عرض زندهٔ حین کشیدن دستگیرهٔ گوشه (میلی‌متر).
    var liveResizeMm by remember(media.id) { mutableStateOf<Float?>(null) }
    val heightMm = widthMm * 0.6f
    val pxPerMm = with(androidx.compose.ui.platform.LocalDensity.current) {
        WordPageLayout.mmToDp(1f, zoom).dp.toPx()
    }
    // آفست زندهٔ حین درگ (میلی‌متر)؛ با رها شدن انگشت commit می‌شود.
    var dragXmm by remember(media.id) { mutableStateOf(0f) }
    var dragYmm by remember(media.id) { mutableStateOf(0f) }
    // V68.4.1 — ارتفاع واقعیِ رندرشدهٔ تصویر (نسبت‌های واقعی به‌جای فرض ۰٫۶)؛
    // clamp پایین بلوک با همین ارتفاع است تا تصویر در سؤال بعدی نرود و اسنپ نخورد.
    var realHeightMm by remember(media.id) { mutableStateOf(0f) }
    val liveWidthMm = (liveResizeMm ?: widthMm).coerceIn(
        WordPageLayout.IMAGE_MIN_WIDTH_MM, WordPageLayout.IMAGE_MAX_WIDTH_MM
    )
    // V68.4 — سقف عمودی = محدودهٔ خودِ بلوک سؤال: بالا-چپ بلوک تا
    // (ارتفاع بلوک − ارتفاع تصویر). تصویر سؤال ۱ دیگر وارد سؤال ۲ نمی‌شود.
    val objHeightMm = if (realHeightMm > 0f) realHeightMm else heightMm
    val maxTopMm = if (boundsHeightMm > 0f) (boundsHeightMm - objHeightMm).coerceAtLeast(0f)
    else Float.MAX_VALUE
    // V63.9 — آفست زنده حتی قبل از free شدن هم دیده می‌شود تا درگ واقعاً کار کند.
    // V68.6 — تصویر غیرآزاد مثل چاپ «وسط» اسلات می‌نشیند (چاپ رسمی غیرآزاد را
    // center می‌کند؛ اینجا فضای LTR آن را چپ می‌چسباند — عارضهٔ V68.4.1). مبنای
    // x چپِ بلوک می‌ماند تا با imageXmm چاپ و clamp یکی باشد: درگِ اول از
    // وسط شروع و commit همچنان مطلق از چپ ذخیره می‌شود.
    // V68.7 — فیکس جابه‌جایی آزاد گالری (گزارش کاربر: «تصویر گالری جابجایی آزاد ندارد»):
    // قبلاً baseX هنگام !free صفر بود، در حالی که نمایش centeredXmm بود؛ اولین
    // درگ از وسط دیده می‌شد ولی commit با ۰+drag به چپ می‌پرید و تصویر گالری
    // بعد از آزاد شدن به چپ می‌چسبید. همچنین تصاویرِ دیگرِ همان سؤال که هنوز
    // xMm/yMm پیش‌فرض (۲۰/۳۰) دارند باید تا اولین درگِ خودشان وسط بمانند، نه
    // چپ؛ وگرنه با آزاد شدن یک تصویر، بقیهٔ گالری ناگهان چپ می‌شدند.
    val centeredXmm = ((WordPageLayout.USABLE_WIDTH_MM - liveWidthMm) / 2f).coerceAtLeast(0f)
    // V68.6 needle برای تست رگرسیون (verify_native_final.py):
    // (if (freePlacement) media.xMm else centeredXmm)
    val isDefaultPos = media.xMm == 20f && media.yMm == 30f
    val effectiveXmm = when {
        !freePlacement -> centeredXmm
        isDefaultPos -> centeredXmm
        else -> media.xMm
    }
    val effectiveYmm = when {
        !freePlacement -> 0f
        isDefaultPos -> 0f
        else -> media.yMm
    }
    val baseXmm = WordPageLayout.clampImageXmm(effectiveXmm + dragXmm, liveWidthMm)
    // V68.4 — yMm ذخیره‌شده = آفست از اسلات طبیعی (انتهای بلوک)؛ برای clamp
    // به مختصات مطلق بلوک تبدیل و بعد دوباره به آفست برمی‌گردیم.
    val visualTopMm =
        (anchorTopMm + effectiveYmm + dragYmm).coerceIn(0f, maxTopMm)
    val baseYmm = visualTopMm - anchorTopMm
    // V68.4 — مقدار تازهٔ لنگر/سقف برای onDragEnd (بدون restart ژست؛
    // کلید pointerInput همان media.id, zoom قبلی می‌ماند).
    val currentAnchorTopMm by rememberUpdatedState(anchorTopMm)
    val currentBoundsHeightMm by rememberUpdatedState(boundsHeightMm)
    // V68.6 — درگ تصویر گالری (گزارش کاربر: جابه‌جایی آزاد واقعی نمی‌شد):
    // لامبدای pointerInput مقادیر لحظهٔ ساختش را می‌بیند؛ چون کلید ژست
    // (media.id, zoom) بعد از commit عوض نمی‌شود، media.xMm/yMm و
    // freePlacement «کهنه» می‌ماندند و هر درگِ بعدی دوباره از اسلاتِ صفر
    // شروع و جای قبلی را بازنویسی می‌کرد. مثل لنگر/سقف، اینها هم باید از
    // rememberUpdatedState خوانده شوند.
    val currentFreePlacement by rememberUpdatedState(freePlacement)
    val currentXmm by rememberUpdatedState(media.xMm)
    val currentYmm by rememberUpdatedState(media.yMm)
    val currentObjHeightMm by rememberUpdatedState(objHeightMm)
    val currentLiveWidthMm by rememberUpdatedState(liveWidthMm)
    val currentCenteredXmm by rememberUpdatedState(centeredXmm)
    val currentIsDefaultPos by rememberUpdatedState(isDefaultPos)

    // V63.8 — بدون لکهٔ آبی: لمس = انتخاب؛ شیء انتخاب‌شده با کشیدن انگشت
    // آزادانه جابه‌جا می‌شود (+/− نوار ابزار اندازه را عوض می‌کند).
    // V68.4.1 — فضای شیء LTR: برنامهٔ فارسی RTL است و Modifier.offset/align
    // در RTL افقی را «آینه» می‌کنند (درگ و هندل‌های گوشه برعکس کار می‌کردند)؛
    // با LTR، آفست مثبت = راست، هندل‌ها در گوشهٔ واقعی و x از چپ بلوک = چاپ.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Box(
        Modifier
            .padding(top = WordPageLayout.mmToDp(WordPageLayout.MEDIA_GAP_MM / 2f, zoom).dp)
            .onGloballyPositioned { realHeightMm = it.size.height / pxPerMm }
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
                            // V68.4 — commit هم مثل رندر زنده به محدودهٔ خودِ
                            // بلوک clamp می‌شود؛ y به‌صورت آفست از اسلات طبیعی
                            // ذخیره می‌شود (می‌تواند منفی = بالاتر از اسلات).
                            // V68.6 — همهٔ مقادیر از rememberUpdatedState تا
                            // درگِ بعدی از جای واقعی فعلی ادامه یابد.
                            // V68.7 — فیکس پرش گالری: قبلاً baseX هنگام !free صفر بود
                            // در حالی که نمایش centered بود؛ اولین درگ به چپ می‌پرید.
                            // حالا مبنای X/Y مثل رندر زنده از centered و default
                            // پیروی می‌کند تا جابه‌جایی واقعاً آزاد و پیوسته باشد.
                            val anchor = currentAnchorTopMm
                            val dragMaxTopMm = if (currentBoundsHeightMm > 0f)
                                (currentBoundsHeightMm - currentObjHeightMm).coerceAtLeast(0f)
                            else Float.MAX_VALUE
                            val baseX = when {
                                !currentFreePlacement -> currentCenteredXmm
                                currentIsDefaultPos -> currentCenteredXmm
                                else -> currentXmm
                            }
                            val baseY = if (!currentFreePlacement || currentIsDefaultPos) 0f else currentYmm
                            val topMm = (anchor + baseY + dragYmm).coerceIn(0f, dragMaxTopMm)
                            onMoved(
                                WordPageLayout.clampImageXmm(baseX + dragXmm, currentLiveWidthMm),
                                topMm - anchor
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
        // V68 — دستگیره‌های گوشه مثل ورد: کشیدن = تغییر اندازهٔ زنده.
        if (selected && !locked) {
            ObjectCornerHandles(
                onLiveDeltaPx = { dxPx ->
                    liveResizeMm = (liveResizeMm ?: widthMm) + dxPx / pxPerMm
                },
                onCommit = {
                    val w = (liveResizeMm ?: widthMm).coerceIn(
                        WordPageLayout.IMAGE_MIN_WIDTH_MM, WordPageLayout.IMAGE_MAX_WIDTH_MM
                    )
                    liveResizeMm = null
                    onResize(w)
                }
            )
        }
    }
    }
}

/**
 * V63.1 — شکل/نمودار/جدول درون‌متنی با دستگیرهٔ تغییر اندازه؛ عرض جدید داخل
 * X.wmm همان توکن %%FIG%% ذخیره می‌شود و چاپ رسمی همان را می‌خواند.
 * V68.4 — کشیدن بدنه شکل هم آزادانه جابه‌جایش می‌کند؛ fx مطلق از چپ بلوک و
 * fy آفست از جای طبیعی (مثل قرارداد تصویر) در X.fx/X.fy ذخیره می‌شود و
 * رندر همیشه به محدودهٔ همان بلوک clamp می‌شود (شکل سؤال ۱ وارد سؤال ۲
 * نمی‌شود). اسلات درون‌متنی خودش را رزرو می‌کند تا ارتفاع بلوک ثابت بماند؛
 * آفست فقط بصری است.
 */
@Composable
private fun ResizableFigure(
    spec: ir.exam.app.core.figure.FigureSpec,
    zoom: Float,
    selected: Boolean,
    locked: Boolean,
    anchorPosMm: Pair<Float, Float>,
    boundsHeightMm: Float,
    onMove: (Float, Float) -> Unit,
    onSelect: () -> Unit,
    onResized: (Float) -> Unit
) {
    // V68 — اندازه با دستگیرهٔ گوشه مثل ورد (کشیدن = تغییر زنده).
    val widthMm = WordPageLayout.figureWidthMm(spec)
    val heightMm = WordPageLayout.figureHeightMm(spec)
    var liveResizeMm by remember(spec.raw) { mutableStateOf<Float?>(null) }
    // V68.4 — آفست زندهٔ حین درگ بدنه (mm)؛ با رها شدن commit می‌شود.
    var dragMm by remember(spec.raw) { mutableStateOf<Pair<Float, Float>?>(null) }
    // V68.4.1 — ارتفاع واقعیِ رندرشدهٔ شکل (به‌جای تخمین figureHeightMm)؛
    // clamp پایین بلوک با همین ارتفاع است تا نه سرریز کند و نه اسنپ بخورد.
    var realHeightMm by remember(spec.raw) { mutableStateOf(0f) }
    val shownWidthMm = (liveResizeMm ?: widthMm).coerceIn(
        WordPageLayout.FIGURE_MIN_WIDTH_MM, WordPageLayout.FIGURE_MAX_WIDTH_MM
    )
    val pxPerMm = with(androidx.compose.ui.platform.LocalDensity.current) {
        WordPageLayout.mmToDp(1f, zoom).dp.toPx()
    }
    // V68.4.1 — fx و fy هر دو «مطلق از بالا-چپ بلوک» ذخیره می‌شوند تا شکل با
    // رفتن/آمدن حالت ویرایش (جریان inline در FlowRow) یا حالت نمایش (زیر
    // متن) و در چاپ، در همان جایگاه بماند و نپرد.
    val pos = WordPageLayout.figurePosMm(spec)
    val baseLeftMm = pos?.first ?: anchorPosMm.first
    val baseTopMm = pos?.second ?: anchorPosMm.second
    // V68.4 — clamp به محدودهٔ خودِ بلوک سؤال: بالا ∈ [0, ارتفاع بلوک − ارتفاع شکل].
    val objHeightMm = if (realHeightMm > 0f) realHeightMm else heightMm
    val maxTopMm = if (boundsHeightMm > 0f) (boundsHeightMm - objHeightMm).coerceAtLeast(0f)
    else Float.MAX_VALUE
    val visualLeftMm = WordPageLayout.clampImageXmm(baseLeftMm + (dragMm?.first ?: 0f), shownWidthMm)
    val visualTopMm = (baseTopMm + (dragMm?.second ?: 0f)).coerceIn(0f, maxTopMm)
    // آفست بصری نسبت به اسلات رزروشدهٔ طبیعی (جریان متن تغییر نمی‌کند).
    val offXmm = visualLeftMm - anchorPosMm.first
    val offYmm = visualTopMm - anchorPosMm.second
    // V68.4.1 — فضای شیء LTR: برنامه RTL است و Modifier.offset/align در RTL
    // افقی را آینه می‌کنند؛ با LTR درگ به راست = حرکت به راست و هندل‌های
    // گوشه در گوشهٔ واقعی می‌نشینند (بزرگ‌کردن آینه‌ای نمی‌شود).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Box(
        Modifier
            .padding(top = WordPageLayout.mmToDp(1.5f, zoom).dp)
            .onGloballyPositioned { realHeightMm = it.size.height / pxPerMm }
            .offset { IntOffset((offXmm * pxPerMm).roundToInt(), (offYmm * pxPerMm).roundToInt()) }
            .width(WordPageLayout.mmToDp(shownWidthMm, zoom).dp)
            .then(
                if (selected) Modifier.border(2.dp, Color(0xFF0B72B8)) else Modifier
            )
            .pointerInput(spec.raw) { detectTapGestures(onTap = { onSelect() }) }
            .then(
                // V68.4 — درگ بدنه شکل مثل تصاویر: فقط انتخاب‌شده و باز (قفل = ثابت).
                if (selected && !locked) Modifier.pointerInput(spec.raw, anchorPosMm, boundsHeightMm, shownWidthMm) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            change.consume()
                            dragMm = ((dragMm?.first ?: 0f) + drag.x / pxPerMm) to
                                ((dragMm?.second ?: 0f) + drag.y / pxPerMm)
                        },
                        onDragEnd = {
                            // V68.4.1 — commit مطلق از بالا-چپ بلوک در X.fx/X.fy.
                            val x = WordPageLayout.clampImageXmm(
                                baseLeftMm + (dragMm?.first ?: 0f), shownWidthMm
                            )
                            val topAbs = (baseTopMm + (dragMm?.second ?: 0f)).coerceIn(0f, maxTopMm)
                            dragMm = null
                            onMove(x, topAbs)
                        },
                        onDragCancel = { dragMm = null }
                    )
                } else Modifier
            )
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
        // V68.4 — دستگیره‌ها مثل تصاویر فقط در حالت باز (قفل = ثابت).
        if (selected && !locked) {
            ObjectCornerHandles(
                onLiveDeltaPx = { dxPx ->
                    liveResizeMm = (liveResizeMm ?: widthMm) + dxPx / pxPerMm
                },
                onCommit = {
                    val w = (liveResizeMm ?: widthMm).coerceIn(
                        WordPageLayout.FIGURE_MIN_WIDTH_MM, WordPageLayout.FIGURE_MAX_WIDTH_MM
                    )
                    liveResizeMm = null
                    onResized(w)
                }
            )
        }
    }
    }
}

/**
 * V68 — چهار دستگیرهٔ گوشهٔ دایره‌ای مثل ورد روی شیء انتخاب‌شده؛ کشیدن هر
 * گوشه عرض را زنده تغییر می‌دهد و با رها شدن commit می‌شود (sign جهت گوشه).
 */
@Composable
private fun BoxScope.ObjectCornerHandles(
    onLiveDeltaPx: (Float) -> Unit,
    onCommit: () -> Unit
) {
    listOf(
        Triple(Alignment.TopStart, (-9).dp to (-9).dp, -1f),
        Triple(Alignment.TopEnd, 9.dp to (-9).dp, 1f),
        Triple(Alignment.BottomStart, (-9).dp to 9.dp, -1f),
        Triple(Alignment.BottomEnd, 9.dp to 9.dp, 1f)
    ).forEach { (corner, offsetDp, sign) ->
        Box(
            Modifier
                .align(corner)
                .offset(x = offsetDp.first, y = offsetDp.second)
                .size(18.dp)
                .background(Color.White, CircleShape)
                .border(1.5.dp, Color(0xFF0B72B8), CircleShape)
                .pointerInput(corner) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            change.consume()
                            onLiveDeltaPx(sign * drag.x)
                        },
                        onDragEnd = { onCommit() }
                    )
                }
        )
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
    fontFamily: FontFamily? = null,
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
                fontFamily = fontFamily,
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
                fontFamily = fontFamily,
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

