package ir.exam.app.ui.student

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.AutoFixNormal
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.LocalImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import ir.exam.app.core.figure.AtlasBitmapRenderer
import ir.exam.app.core.figure.FigureDigits
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer
import ir.exam.app.core.math.FormulaTextCodec
import ir.exam.app.core.math.NativeMathSvgRenderer
import ir.exam.app.ui.math.FormulaHostDialog
import ir.exam.app.ui.printing.ExamFigureToolHost
import ir.exam.app.ui.printing.FigureToolRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** ابزارهای تخته (V137 + V137.1: انتخاب، هایلایتر، پاک‌کن شیء، مثلث، تصویر). */
internal enum class BoardTool(val label: String, val pickable: Boolean = true) {
    PEN("قلم"), HIGHLIGHT("هایلایتر"), ERASER("پاک‌کن"), OBJ_ERASER("پاک‌کن شیء"), SELECT("انتخاب/جابه‌جایی"),
    LINE("خط"), ARROW("پیکان"), RECT("مستطیل"), CIRCLE("دایره"), TRIANGLE("مثلث"), TEXT("متن"),
    IMAGE("تصویر", pickable = false);

    /** V137.5 — آیکون ابزار (نوار ابزار آیکونی). */
    val icon: androidx.compose.ui.graphics.vector.ImageVector
        get() = when (this) {
            PEN -> Icons.Outlined.Draw
            HIGHLIGHT -> Icons.Outlined.Highlight
            ERASER -> Icons.Outlined.AutoFixNormal
            OBJ_ERASER -> Icons.Outlined.LayersClear
            SELECT -> Icons.Outlined.OpenWith
            LINE -> Icons.Outlined.HorizontalRule
            ARROW -> Icons.AutoMirrored.Outlined.ArrowRightAlt
            RECT -> Icons.Outlined.CropSquare
            CIRCLE -> Icons.Outlined.RadioButtonUnchecked
            TRIANGLE -> Icons.Outlined.ChangeHistory
            TEXT -> Icons.Outlined.TextFields
            IMAGE -> Icons.Outlined.AddBox
        }
}

/** V137.5 — چیپِ آیکونیِ نوار ابزار تخته (دایرهٔ ۴۰dp؛ انتخاب‌شده = پس‌زمینهٔ رنگ اصلی). */
@Composable
internal fun BoardIconChip(selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp)) }
}

/** V137.6 — یک سطرِ راهنما: آیکون + نام + توضیح. */
@Composable
private fun HelpRow(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, desc: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun k_hint(kind: BoardInstrument): String = when (kind) {
    BoardInstrument.RULER -> "خط‌کش: قلم نزدیک لبه می‌چسبد؛ آبی = چرخش/اندازه؛ خاکستری = جابه‌جایی"
    BoardInstrument.SETSQUARE -> "گونیا: قلم نزدیک لبه می‌چسبد؛ آبی = چرخش/اندازه؛ خاکستری = جابه‌جایی"
    else -> "قلم نزدیک لبه می‌چسبد؛ آبی = چرخش/اندازه؛ خاکستری = جابه‌جایی"
}

/** پس‌زمینهٔ تخته (V137؛ V137.1: نقطه‌ای و محور مختصات). */
internal enum class BoardBackground(val label: String) { PLAIN("ساده"), GRID("شطرنجی"), LINED("خط‌دار"), DOTS("نقطه‌ای"), AXES("محور مختصات") }

/**
 * V137.2 — ابزارهای هندسیِ تعاملی روی تخته (خط‌کش، گونیا، نقاله، پرگار). این‌ها «شیء» نیستند و در تصویر
 * خروجی نمی‌آیند؛ فقط راهنمای کشیدن‌اند: قلم/خط نزدیک لبهٔ خط‌کش/گونیا روی همان لبه می‌چسبد، نزدیک کمان
 * نقاله روی کمان می‌رود، و با پرگار هر کشیدن یک کمان به مرکز/شعاع پرگار است. جابه‌جایی: کشیدن بدنه؛
 * چرخش/اندازه: کشیدن دستگیرهٔ آبی.
 */
internal enum class BoardInstrument(val label: String) {
    NONE("بدون ابزار"), RULER("خط‌کش"), SETSQUARE("گونیا"), PROTRACTOR("نقاله"), COMPASS("پرگار");

    /** V137.5 — آیکون ابزار هندسی. */
    val icon: androidx.compose.ui.graphics.vector.ImageVector
        get() = when (this) {
            NONE -> Icons.Outlined.Block
            RULER -> Icons.Outlined.Straighten
            SETSQUARE -> Icons.Outlined.SquareFoot
            PROTRACTOR -> Icons.Outlined.Speed
            COMPASS -> Icons.Outlined.Architecture
        }
}

internal data class InstrumentState(
    val kind: BoardInstrument = BoardInstrument.NONE,
    val center: Offset = Offset.Zero,
    val angle: Float = 0f,
    val size: Float = 0f
) {
    val dir: Offset get() = Offset(cos(angle), sin(angle))
    val nrm: Offset get() = Offset(-sin(angle), cos(angle))
    /** دستگیرهٔ آبی چرخش/اندازه. */
    val handle: Offset get() = center + dir * size
}

/**
 * یک عنصر روی تخته. برای IMAGE، `ref` منبع است: `tex:<فرمول>`، `fig:<json شکل>`، `img:<نشانی تصویر سؤال>`
 * و points = [بالا‑راست، پایین‑چپ] قاب تصویر.
 */
internal data class BoardStroke(
    val id: Long,
    val tool: BoardTool,
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val text: String = "",
    val textSizeSp: Float = 18f,
    val ref: String = "",
    val page: Int = 0
) {
    val eraser: Boolean get() = tool == BoardTool.ERASER
    fun moved(d: Offset) = copy(points = points.map { it + d })
}

private val BOARD_PALETTE = listOf(
    Color(0xFF1F2937), Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFF2E7D32),
    Color(0xFFF57C00), Color(0xFF7B1FA2), Color(0xFF00838F), Color(0xFF6D4C41), Color(0xFFFFFFFF)
)
private val BOARD_WIDTHS = listOf(1.5f to "مویی", 3f to "نازک", 5f to "معمولی", 8f to "متوسط", 12f to "ضخیم", 18f to "خیلی‌ضخیم")
private const val MAX_PAGES = 6

/**
 * V136 — «تخته وایت‌برد» پاسخ دانش‌آموز.
 * V137 — ابزارهای بیشتر، رنگ/ضخامت، زمینه، برگشت/جلو.
 * V137.1 — متن و هر شیء قابل انتخاب/جابه‌جایی/تغییر رنگ/ویرایش/حذف/بزرگ‌وکوچک؛ هایلایتر؛ پاک‌کن دقیق
 * (لایهٔ شفاف، زمینه سالم می‌ماند) و پاک‌کن شیء؛ مثلث؛ درج فرمول (ویرایشگر فرمول)، نمودار/محور/شکل/جدول/
 * فیزیک/شیمی (همان ابزارهای سازنده) و تصویر سؤال روی تخته؛ چند صفحه؛ ذخیرهٔ خودکار پیش‌نویس در filesDir؛
 * تأیید پیش از ثبت؛ شمارهٔ سؤال و زمان باقی‌مانده در سربرگ؛ میان‌بر Ctrl+Z / Ctrl+Y / Delete؛
 * خروجی هر صفحه یک PNG کدر (بدون شفافیت) در filesDir (نه cache) تا تا زمان ارسال پاک نشود.
 */
@Composable
fun StudentWhiteboardDialog(
    questionId: String,
    onDismiss: () -> Unit,
    onDone: (fileUris: List<String>) -> Unit,
    questionNumber: Int = 0,
    remainingSeconds: Long = UNLIMITED_TIME,
    questionImages: List<String> = emptyList(),
    /** V137.5 — حالت معلم (سازندهٔ آزمون): دکمهٔ پایانی «افزودن تصویر تخته به سؤال» است، نه «ثبت به‌عنوان پاسخ». */
    teacherMode: Boolean = false
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val imageLoader = LocalImageLoader.current
    val scope = rememberCoroutineScope()
    val strokes = remember(questionId) { mutableStateListOf<BoardStroke>() }
    val redo = remember(questionId) { mutableStateListOf<BoardStroke>() }
    val bitmaps = remember(questionId) { mutableStateMapOf<String, Bitmap>() }
    var draft by remember { mutableStateOf<BoardStroke?>(null) }
    var tool by remember { mutableStateOf(BoardTool.PEN) }
    var color by remember { mutableStateOf(BOARD_PALETTE.first()) }
    var width by remember { mutableStateOf(5f) }
    var background by remember { mutableStateOf(BoardBackground.PLAIN) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var page by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var textAt by remember { mutableStateOf<Offset?>(null) }
    var editTextId by remember { mutableStateOf<Long?>(null) }
    var textInput by remember { mutableStateOf("") }
    var formulaOpen by remember { mutableStateOf(false) }
    var figureTool by remember { mutableStateOf<String?>(null) }
    var confirmDone by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) } // V137.6 — راهنمای آیکون‌ها
    var confirmClearAll by remember { mutableStateOf(false) }
    var insertMenu by remember { mutableStateOf(false) }
    // V137.2 — ابزار هندسی فعال (خط‌کش/گونیا/نقاله/پرگار).
    var instrument by remember { mutableStateOf(InstrumentState()) }
    var restored by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var nextId by remember { mutableStateOf(System.currentTimeMillis()) }
    val focus = remember { FocusRequester() }
    val draftFile = remember(questionId) { boardDraftFile(context.filesDir, questionId) }

    fun newId(): Long = ++nextId
    fun bump() { revision++ }
    fun commit(s: BoardStroke) { strokes.add(s); redo.clear(); bump() }
    fun replace(s: BoardStroke) {
        val i = strokes.indexOfFirst { it.id == s.id }
        if (i >= 0) { strokes[i] = s; bump() }
    }
    fun remove(id: Long) {
        val i = strokes.indexOfFirst { it.id == id }
        if (i >= 0) { redo.add(strokes.removeAt(i)); if (selectedId == id) selectedId = null; bump() }
    }
    fun undo() { if (strokes.isNotEmpty()) { redo.add(strokes.removeAt(strokes.lastIndex)); selectedId = null; bump() } }
    fun redoLast() { if (redo.isNotEmpty()) { strokes.add(redo.removeAt(redo.lastIndex)); bump() } }
    val pageItems = strokes.filter { it.page == page }
    val selected = selectedId?.let { id -> strokes.firstOrNull { it.id == id } }

    suspend fun ensureBitmap(ref: String) {
        if (ref.isBlank() || bitmaps.containsKey(ref)) return
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                when {
                    ref.startsWith("tex:") -> texBitmap(ref.removePrefix("tex:"), density)
                    ref.startsWith("fig:") -> FigureSpec.parse(ref.removePrefix("fig:"))?.let { figureBitmap(context, it) }
                    ref.startsWith("img:") -> {
                        val res = imageLoader.execute(ImageRequest.Builder(context).data(ref.removePrefix("img:")).allowHardware(false).build())
                        (res as? SuccessResult)?.drawable?.toBitmap()
                    }
                    else -> null
                }
            }.getOrNull()
        }
        if (bmp != null) bitmaps[ref] = bmp
    }

    fun insertImage(ref: String) {
        scope.launch {
            ensureBitmap(ref)
            val bmp = bitmaps[ref] ?: return@launch
            val maxW = boardSize.width * 0.6f
            val maxH = boardSize.height * 0.6f
            val scale = min(1f, min(maxW / bmp.width, maxH / bmp.height))
            val w = bmp.width * scale; val h = bmp.height * scale
            val left = (boardSize.width - w) / 2f; val top = (boardSize.height - h) / 2f
            val item = BoardStroke(newId(), BoardTool.IMAGE, listOf(Offset(left, top), Offset(left + w, top + h)), color, width, ref = ref, page = page)
            commit(item); selectedId = item.id; tool = BoardTool.SELECT
        }
    }

    // بازیابی پیش‌نویس ذخیره‌شده (ذخیرهٔ خودکار)
    LaunchedEffect(questionId) {
        val saved = withContext(Dispatchers.IO) { runCatching { readBoardDraft(draftFile) }.getOrNull() }
        if (saved != null && saved.items.isNotEmpty()) {
            strokes.clear(); strokes.addAll(saved.items)
            background = saved.background; pageCount = saved.pageCount.coerceIn(1, MAX_PAGES)
            nextId = (saved.items.maxOfOrNull { it.id } ?: nextId) + 1
            saved.items.map { it.ref }.filter { it.isNotBlank() }.distinct().forEach { ensureBitmap(it) }
        }
        restored = true
        runCatching { focus.requestFocus() }
    }
    // ذخیرهٔ خودکار (با تأخیر کوتاه پس از آخرین تغییر)
    LaunchedEffect(revision, background, pageCount) {
        if (!restored) return@LaunchedEffect
        delay(700)
        val snapshot = strokes.toList()
        withContext(Dispatchers.IO) { runCatching { writeBoardDraft(draftFile, snapshot, background, pageCount) } }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(
            Modifier
                .fillMaxSize()
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        e.isCtrlPressed && e.key == Key.Z -> { undo(); true }
                        e.isCtrlPressed && e.key == Key.Y -> { redoLast(); true }
                        (e.key == Key.Delete || e.key == Key.Backspace) && selectedId != null -> { selectedId?.let { remove(it) }; true }
                        else -> false
                    }
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // سربرگ: شمارهٔ سؤال + زمان + برگشت/جلو + انصراف
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (questionNumber > 0) "تخته — سؤال $questionNumber" else "تخته وایت‌برد پاسخ", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (remainingSeconds == UNLIMITED_TIME) "زمان: بدون محدودیت" else "زمان باقی‌مانده: ${formatBoardTime(remainingSeconds)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (remainingSeconds != UNLIMITED_TIME && remainingSeconds < 120) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { helpOpen = true }) { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "راهنما", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { undo() }, enabled = strokes.isNotEmpty()) { Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "برگشت") }
                    IconButton(onClick = { redoLast() }, enabled = redo.isNotEmpty()) { Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "جلو") }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.error) }
                }
                // ابزارها — V137.5: آیکون به‌جای متن (نام ابزار در contentDescription/کنار ابزار انتخاب‌شده)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    BoardTool.values().filter { it.pickable }.forEach { t ->
                        BoardIconChip(selected = tool == t, icon = t.icon, label = t.label, onClick = { tool = t; if (t != BoardTool.SELECT) selectedId = null })
                    }
                    BoardIconChip(selected = insertMenu, icon = Icons.Outlined.AddBox, label = "درج…", onClick = { insertMenu = !insertMenu })
                    Text(tool.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                // V137.2 — ابزارهای هندسی (V137.5: آیکون)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    BoardInstrument.values().forEach { k ->
                        BoardIconChip(
                            selected = instrument.kind == k,
                            icon = k.icon,
                            label = k.label,
                            onClick = {
                                instrument = if (k == BoardInstrument.NONE) InstrumentState() else defaultInstrument(k, boardSize, density)
                                if (k != BoardInstrument.NONE && tool != BoardTool.PEN && tool != BoardTool.LINE) tool = BoardTool.PEN
                            }
                        )
                    }
                    if (instrument.kind != BoardInstrument.NONE) {
                        Text(
                            when (instrument.kind) {
                                BoardInstrument.COMPASS -> "پرگار: بکشید تا کمان رسم شود؛ آبی = شعاع؛ خاکستری = جابه‌جایی"
                                BoardInstrument.PROTRACTOR -> "نقاله: قلم نزدیک کمان/خط پایه می‌چسبد؛ خاکستری = جابه‌جایی"
                                else -> k_hint(instrument.kind)
                            },
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (insertMenu) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = false, onClick = { insertMenu = false; formulaOpen = true }, label = { Text("فرمول") })
                        listOf("graph" to "نمودار تابع", "axis" to "محور", "figure" to "شکل هندسی", "table" to "جدول", "physics" to "فیزیک", "chemistry" to "شیمی", "periodic" to "جدول تناوبی").forEach { (id, label) ->
                            FilterChip(selected = false, onClick = { insertMenu = false; figureTool = id }, label = { Text(label) })
                        }
                        questionImages.forEachIndexed { i, url ->
                            FilterChip(selected = false, onClick = { insertMenu = false; insertImage("img:$url") }, label = { Text("تصویر سؤال ${i + 1}") })
                        }
                    }
                }
                // رنگ‌ها + ضخامت (روی شیء انتخاب‌شده هم اعمال می‌شود)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BOARD_PALETTE.forEach { c ->
                        Box(
                            Modifier
                                .size(if (c == color) 30.dp else 24.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (c == color) 2.dp else 1.dp, Color(0xFF9CA3AF), CircleShape)
                                .clickable {
                                    color = c
                                    if (tool == BoardTool.ERASER || tool == BoardTool.OBJ_ERASER) tool = BoardTool.PEN
                                    selected?.let { replace(it.copy(color = c)) }
                                }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    BOARD_WIDTHS.forEach { (w, label) ->
                        FilterChip(selected = width == w, onClick = { width = w; selected?.let { s -> if (s.tool != BoardTool.IMAGE) replace(s.copy(width = w)) } }, label = { Text(label) })
                    }
                }
                // شیء انتخاب‌شده
                if (selected != null) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("انتخاب: ${selected.tool.label}", style = MaterialTheme.typography.labelMedium)
                        if (selected.tool == BoardTool.TEXT) {
                            TextButton(onClick = { editTextId = selected.id; textInput = selected.text }) { Text("ویرایش متن") }
                        }
                        if (selected.tool == BoardTool.TEXT || selected.tool == BoardTool.IMAGE) {
                            TextButton(onClick = { replace(scaleItem(selected, 1.2f)) }) { Text("بزرگ‌تر") }
                            TextButton(onClick = { replace(scaleItem(selected, 1f / 1.2f)) }) { Text("کوچک‌تر") }
                        }
                        if (selected.tool == BoardTool.IMAGE && selected.ref.startsWith("tex:")) {
                            TextButton(onClick = { formulaOpen = true }) { Text("ویرایش فرمول") }
                        }
                        TextButton(onClick = { remove(selected.id) }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = { selectedId = null }) { Text("لغو انتخاب") }
                    }
                }
                // زمینه + صفحه‌ها + پاک‌کردن همه
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("زمینه:", style = MaterialTheme.typography.labelMedium)
                    BoardBackground.values().forEach { b ->
                        FilterChip(selected = background == b, onClick = { background = b }, label = { Text(b.label) })
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("صفحه ${page + 1}/$pageCount", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { page -= 1; selectedId = null }, enabled = page > 0) { Text("قبلی") }
                    TextButton(onClick = { page += 1; selectedId = null }, enabled = page < pageCount - 1) { Text("بعدی") }
                    TextButton(onClick = { pageCount += 1; page = pageCount - 1; selectedId = null }, enabled = pageCount < MAX_PAGES) { Text("+ صفحه") }
                    TextButton(onClick = { confirmClearAll = true }, enabled = pageItems.isNotEmpty()) { Text("پاک‌کردن صفحه") }
                }
                // بوم
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White)
                        .onSizeChanged { boardSize = it }
                        .pointerInput(tool, page) {
                            detectTapGestures(
                                onTap = { p ->
                                    when (tool) {
                                        BoardTool.TEXT -> { textAt = p; textInput = "" }
                                        BoardTool.SELECT -> selectedId = hitTest(strokes.filter { it.page == page }, p, density, bitmaps)?.id
                                        BoardTool.OBJ_ERASER -> hitTest(strokes.filter { it.page == page }, p, density, bitmaps)?.let { remove(it.id) }
                                        else -> Unit
                                    }
                                },
                                onDoubleTap = { p ->
                                    if (tool == BoardTool.SELECT) {
                                        val hit = hitTest(strokes.filter { it.page == page }, p, density, bitmaps)
                                        if (hit?.tool == BoardTool.TEXT) { selectedId = hit.id; editTextId = hit.id; textInput = hit.text }
                                    }
                                }
                            )
                        }
                        .pointerInput(tool, color, width, page, instrument.kind) {
                            if (tool == BoardTool.TEXT) return@pointerInput
                            var moving: Long? = null
                            // V137.2 — حالت کشیدنِ ابزار هندسی: "move" (بدنه) / "handle" (چرخش/اندازه) / null؛
                            // snapLock: لبه/کمانی که ضربهٔ فعلی به آن چسبیده (تا پایان کشیدن ثابت می‌ماند).
                            var instMode: String? = null
                            var snapLock: SnapTarget? = null
                            detectDragGestures(
                                onDragStart = { p ->
                                    val inst = instrument
                                    val instActive = inst.kind != BoardInstrument.NONE && tool != BoardTool.SELECT && tool != BoardTool.OBJ_ERASER
                                    // V137.5 — جابه‌جایی فقط از دستگیرهٔ خاکستریِ مخصوص (grip)؛ روی بدنه می‌شود کشید/نوشت.
                                    instMode = when {
                                        !instActive -> null
                                        (p - inst.handle).getDistance() < 30f * density -> "handle"
                                        (p - instrumentGrip(inst, density)).getDistance() < 30f * density -> "move"
                                        else -> null
                                    }
                                    if (instMode == null) when (tool) {
                                        BoardTool.SELECT -> {
                                            val hit = hitTest(strokes.filter { it.page == page }, p, density, bitmaps)
                                            selectedId = hit?.id; moving = hit?.id
                                        }
                                        BoardTool.OBJ_ERASER -> hitTest(strokes.filter { it.page == page }, p, density, bitmaps)?.let { remove(it.id) }
                                        else -> {
                                            snapLock = if (instActive) snapTargetOf(inst, p, density) else null
                                            val sp = snapLock?.let { snapApply(inst, it, p, density) } ?: p
                                            draft = BoardStroke(newId(), tool, listOf(sp, sp), color, width, page = page)
                                        }
                                    }
                                },
                                onDragEnd = { draft?.let { commit(it) }; draft = null; moving = null; instMode = null; snapLock = null },
                                onDragCancel = { draft = null; moving = null; instMode = null; snapLock = null },
                                onDrag = { change, delta ->
                                    change.consume()
                                    when {
                                        instMode == "move" -> instrument = instrument.copy(center = instrument.center + delta)
                                        instMode == "handle" -> {
                                            val v = change.position - instrument.center
                                            instrument = instrument.copy(angle = atan2(v.y, v.x), size = v.getDistance().coerceIn(40f * density, 4000f))
                                        }
                                        tool == BoardTool.SELECT -> moving?.let { id -> strokes.firstOrNull { it.id == id }?.let { replace(it.moved(delta)) } }
                                        tool == BoardTool.OBJ_ERASER -> hitTest(strokes.filter { it.page == page }, change.position, density, bitmaps)?.let { remove(it.id) }
                                        else -> {
                                            val d = draft ?: return@detectDragGestures
                                            val pos = snapLock?.let { snapApply(instrument, it, change.position, density) } ?: change.position
                                            draft = if (d.tool == BoardTool.PEN || d.tool == BoardTool.ERASER || d.tool == BoardTool.HIGHLIGHT) {
                                                d.copy(points = d.points + pos)
                                            } else {
                                                d.copy(points = listOf(d.points.first(), pos))
                                            }
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val c = drawContext.canvas.nativeCanvas
                        renderBoard(c, size.width, size.height, background, pageItems + listOfNotNull(draft), density, bitmaps, selectedId)
                        // V137.2 — ابزار هندسی فقط روی صفحه (در خروجی نیست).
                        if (instrument.kind != BoardInstrument.NONE) drawInstrument(c, instrument, density)
                    }
                    if (pageItems.isEmpty() && draft == null) {
                        Text(
                            when (tool) {
                                BoardTool.TEXT -> "برای نوشتن متن، روی تخته بزنید"
                                BoardTool.SELECT -> "روی شیء بزنید تا انتخاب شود؛ بکشید تا جابه‌جا شود"
                                else -> "اینجا بنویسید یا بکشید…"
                            },
                            color = Color(0xFF9CA3AF), modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Button(
                    onClick = { confirmDone = true },
                    enabled = strokes.isNotEmpty() && boardSize.width > 0,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text((if (teacherMode) "افزودن تصویر تخته به سؤال" else "ثبت به‌عنوان تصویر پاسخ") + if (pageCount > 1) " ($pageCount صفحه)" else "") }
            }
        }
    }
    // افزودن/ویرایش متن
    val textDialogOpen = textAt != null || editTextId != null
    if (textDialogOpen) {
        AlertDialog(
            onDismissRequest = { textAt = null; editTextId = null },
            title = { Text(if (editTextId != null) "ویرایش متن" else "متن روی تخته") },
            text = {
                OutlinedTextField(value = textInput, onValueChange = { textInput = it.take(300) }, label = { Text("متن (فارسی/انگلیسی)") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(enabled = textInput.isNotBlank(), onClick = {
                    val editing = editTextId?.let { id -> strokes.firstOrNull { it.id == id } }
                    if (editing != null) {
                        replace(editing.copy(text = textInput.trim()))
                    } else textAt?.let { at ->
                        val item = BoardStroke(newId(), BoardTool.TEXT, listOf(at), color, width, text = textInput.trim(), textSizeSp = 14f + width * 1.2f, page = page)
                        commit(item); selectedId = item.id; tool = BoardTool.SELECT
                    }
                    textAt = null; editTextId = null
                }) { Text(if (editTextId != null) "ذخیره" else "افزودن") }
            },
            dismissButton = { TextButton(onClick = { textAt = null; editTextId = null }) { Text("انصراف") } }
        )
    }
    // فرمول (همان ویرایشگر فرمول سازنده)
    if (formulaOpen) {
        val editing = selected?.takeIf { it.tool == BoardTool.IMAGE && it.ref.startsWith("tex:") }
        val initial = editing?.ref?.removePrefix("tex:")?.let { "${'$'}$it${'$'}" }.orEmpty()
        FormulaHostDialog(
            initialText = initial,
            selectionStart = 0,
            selectionEnd = initial.length,
            onDismiss = { formulaOpen = false },
            onResult = { raw ->
                val tex = FormulaTextCodec.occurrences(raw).firstOrNull()?.tex ?: raw.trim().trim('$').trim()
                if (tex.isNotBlank()) {
                    if (editing != null) {
                        val ref = "tex:$tex"
                        scope.launch { ensureBitmap(ref); replace(editing.copy(ref = ref)) }
                    } else insertImage("tex:$tex")
                }
            }
        )
    }
    // نمودار/محور/شکل/جدول/فیزیک/شیمی/تناوبی (همان ابزارهای بومی سازنده)
    figureTool?.let { t ->
        ExamFigureToolHost(
            request = FigureToolRequest(questionId = questionId, tool = t, initialSpecJson = "", tokenStart = 0, tokenEnd = 0),
            onInsert = { token ->
                val json = token.removePrefix("%%FIG:").removeSuffix("%%")
                if (FigureSpec.parse(json) != null) insertImage("fig:$json")
                figureTool = null
            },
            onDismiss = { figureTool = null }
        )
    }
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("پاک‌کردن این صفحه؟") },
            text = { Text("همهٔ عناصر صفحهٔ ${page + 1} حذف می‌شود (با «برگشت» قابل بازگردانی نیست).") },
            confirmButton = { Button(onClick = { strokes.removeAll { it.page == page }; redo.clear(); selectedId = null; bump(); confirmClearAll = false }) { Text("پاک کن") } },
            dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("انصراف") } }
        )
    }
    if (helpOpen) {
        // V137.6 — راهنمای همهٔ آیکون‌های تخته
        AlertDialog(
            onDismissRequest = { helpOpen = false },
            title = { Text("راهنمای تخته") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("سربرگ", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    HelpRow(Icons.AutoMirrored.Outlined.Undo, "برگشت", "آخرین کار را لغو می‌کند (Ctrl+Z).")
                    HelpRow(Icons.AutoMirrored.Outlined.Redo, "جلو", "کارِ لغوشده را برمی‌گرداند (Ctrl+Y).")
                    HelpRow(Icons.Outlined.Close, "بستن", "تخته بسته می‌شود؛ پیش‌نویس می‌ماند.")
                    Text("ابزارها", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    HelpRow(BoardTool.PEN.icon, "قلم", "کشیدن آزاد با رنگ و ضخامت انتخابی.")
                    HelpRow(BoardTool.HIGHLIGHT.icon, "هایلایتر", "خط پهنِ نیمه‌شفاف برای برجسته‌کردن.")
                    HelpRow(BoardTool.ERASER.icon, "پاک‌کن", "هر جا بکشید پاک می‌شود.")
                    HelpRow(BoardTool.OBJ_ERASER.icon, "پاک‌کن شیء", "روی هر خط/شکل/متن بزنید تا کامل حذف شود.")
                    HelpRow(BoardTool.SELECT.icon, "انتخاب/جابه‌جایی", "روی شیء بزنید تا انتخاب شود؛ بکشید تا جابه‌جا شود. رنگ/ضخامت روی شیء انتخاب‌شده اعمال می‌شود.")
                    HelpRow(BoardTool.LINE.icon, "خط", "خط راست بین نقطهٔ شروع و پایان.")
                    HelpRow(BoardTool.ARROW.icon, "پیکان", "خط راست با نوک پیکان.")
                    HelpRow(BoardTool.RECT.icon, "مستطیل", "کشیدن از یک گوشه تا گوشهٔ مقابل.")
                    HelpRow(BoardTool.CIRCLE.icon, "دایره/بیضی", "کشیدن داخل کادر.")
                    HelpRow(BoardTool.TRIANGLE.icon, "مثلث", "کشیدن داخل کادر.")
                    HelpRow(BoardTool.TEXT.icon, "متن", "روی تخته بزنید و متن را بنویسید؛ با انتخاب می‌توان ویرایش/بزرگ/کوچک کرد.")
                    HelpRow(Icons.Outlined.AddBox, "درج…", "فرمول، نمودار، محور، شکل هندسی، جدول، فیزیک/شیمی، جدول تناوبی یا تصویر سؤال.")
                    Text("ابزارهای هندسی", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    HelpRow(BoardInstrument.NONE.icon, "بدون ابزار", "ابزار هندسی برداشته می‌شود.")
                    HelpRow(BoardInstrument.RULER.icon, "خط‌کش", "قلم/خط نزدیک لبه به لبه می‌چسبد.")
                    HelpRow(BoardInstrument.SETSQUARE.icon, "گونیا", "سه لبهٔ شماره‌دار؛ قلم به لبه می‌چسبد.")
                    HelpRow(BoardInstrument.PROTRACTOR.icon, "نقاله", "قلم نزدیک کمان روی کمان و نزدیک خط پایه روی خط می‌رود.")
                    HelpRow(BoardInstrument.COMPASS.icon, "پرگار", "هر کشیدن یک کمان به مرکز و شعاع پرگار است.")
                    Text("دستگیره‌ها: دایرهٔ خاکستری (چهارپیکان) = جابه‌جایی ابزار؛ دایرهٔ آبی = چرخش و تغییر اندازه.", style = MaterialTheme.typography.bodySmall)
                    Text("پایین نوار: رنگ‌ها و ضخامت، زمینه (ساده/شطرنجی/خط‌دار/نقطه‌ای/محور)، صفحه‌های قبلی/بعدی، + صفحه، پاک‌کردن صفحه.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { helpOpen = false }) { Text("فهمیدم") } }
        )
    }
    if (confirmDone) {
        val filled = (0 until pageCount).count { p -> strokes.any { it.page == p } }
        AlertDialog(
            onDismissRequest = { confirmDone = false },
            title = { Text(if (teacherMode) "افزودن تصویر تخته به سؤال؟" else "ثبت پاسخ تخته؟") },
            text = {
                Text(
                    when {
                        teacherMode && pageCount > 1 -> "$filled صفحهٔ دارای محتوا به‌عنوان تصویر به سؤال افزوده می‌شود. پیش‌نویس تخته برای ویرایش دوباره می‌ماند."
                        teacherMode -> "تصویر تخته به سؤال افزوده می‌شود. پیش‌نویس تخته برای ویرایش دوباره می‌ماند."
                        pageCount > 1 -> "$filled صفحهٔ دارای محتوا به‌عنوان تصویر پاسخ ثبت می‌شود. پیش‌نویس تخته برای ویرایش دوباره می‌ماند."
                        else -> "تصویر تخته به‌عنوان پاسخ این سؤال ثبت می‌شود. پیش‌نویس تخته برای ویرایش دوباره می‌ماند."
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    confirmDone = false
                    val uris = runCatching {
                        (0 until pageCount).filter { p -> strokes.any { it.page == p } }.map { p ->
                            exportBoard(context.filesDir, questionId, p, boardSize, strokes.filter { it.page == p }, background, density, bitmaps)
                        }
                    }.getOrDefault(emptyList())
                    if (uris.isNotEmpty()) onDone(uris)
                }) { Text(if (teacherMode) "افزودن" else "ثبت") }
            },
            dismissButton = { TextButton(onClick = { confirmDone = false }) { Text("انصراف") } }
        )
    }
}

// ---------------------------------------------------------------- رندر مشترک (صفحه و خروجی)

// ============================================================ V137.2 — ابزارهای هندسی

/** هدف چسبیدن ضربه: لبهٔ شماره‌دار (index) یا کمان (arc) یا کمانِ پرگار (compass). */
internal data class SnapTarget(val edge: Int = -1, val arc: Boolean = false, val compass: Boolean = false)

internal fun defaultInstrument(kind: BoardInstrument, board: IntSize, density: Float): InstrumentState {
    val w = board.width.coerceAtLeast(1).toFloat(); val h = board.height.coerceAtLeast(1).toFloat()
    val m = min(w, h)
    val size = when (kind) {
        BoardInstrument.RULER -> w * 0.32f
        BoardInstrument.SETSQUARE -> m * 0.38f
        BoardInstrument.PROTRACTOR -> m * 0.30f
        BoardInstrument.COMPASS -> m * 0.20f
        BoardInstrument.NONE -> 0f
    }
    return InstrumentState(kind, Offset(w / 2f, h / 2f), 0f, size)
}

/** لبه‌های مستقیمِ قابل‌چسبیدن هر ابزار (مختصات تخته). */
internal fun instrumentEdges(st: InstrumentState, density: Float): List<Pair<Offset, Offset>> {
    val d = st.dir; val n = st.nrm; val c = st.center; val s = st.size
    val half = RULER_THICK_DP * density / 2f
    return when (st.kind) {
        BoardInstrument.RULER -> listOf(
            (c - n * half - d * s) to (c - n * half + d * s),
            (c + n * half - d * s) to (c + n * half + d * s)
        )
        BoardInstrument.SETSQUARE -> {
            val a = c; val b = c + d * s; val e = c - n * s
            listOf(a to b, a to e, b to e)
        }
        BoardInstrument.PROTRACTOR -> listOf((c - d * s) to (c + d * s))
        else -> emptyList()
    }
}

private fun projectOnSegment(p: Offset, a: Offset, b: Offset): Offset {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 <= 0f) return a
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return Offset(a.x + ab.x * t, a.y + ab.y * t)
}

/** آیا نقطه روی بدنهٔ ابزار است (برای جابه‌جایی)؟ */
internal fun instrumentBodyHit(st: InstrumentState, p: Offset, density: Float): Boolean {
    val v = p - st.center
    val lx = v.x * st.dir.x + v.y * st.dir.y
    val ly = v.x * st.nrm.x + v.y * st.nrm.y
    val pad = 6f * density
    return when (st.kind) {
        BoardInstrument.RULER -> abs(lx) <= st.size + pad && abs(ly) <= RULER_THICK_DP * density / 2f + pad
        BoardInstrument.SETSQUARE -> lx >= -pad && ly <= pad && (lx - ly) <= st.size + pad // مثلث (0,0),(s,0),(0,-s)
        BoardInstrument.PROTRACTOR -> ly <= pad && ly >= -st.size - pad && v.getDistance() <= st.size + pad
        BoardInstrument.COMPASS -> v.getDistance() <= 34f * density
        BoardInstrument.NONE -> false
    }
}

/**
 * V137.5 — دستگیرهٔ خاکستریِ «جابه‌جایی» هر ابزار (مختصات تخته). خط‌کش: وسط بدنه؛ گونیا: داخل سوراخ میانی؛
 * نقاله: وسط نیم‌دایره؛ پرگار: لولای بالایی. فقط کشیدنِ این دستگیره ابزار را جابه‌جا می‌کند.
 */
internal fun instrumentGrip(st: InstrumentState, density: Float): Offset {
    val d = st.dir; val n = st.nrm; val c = st.center; val s = st.size
    return when (st.kind) {
        BoardInstrument.RULER -> c
        BoardInstrument.SETSQUARE -> c + d * (s * 0.28f) - n * (s * 0.28f)
        BoardInstrument.PROTRACTOR -> c - n * (s * 0.45f)
        BoardInstrument.COMPASS -> c + d * (s / 2f) - n * (s * 0.85f + 26f * density)
        BoardInstrument.NONE -> c
    }
}

/** اگر نقطهٔ شروع نزدیک لبه/کمان باشد، هدف چسبیدن را برمی‌گرداند. */
internal fun snapTargetOf(st: InstrumentState, p: Offset, density: Float): SnapTarget? {
    val snapPx = 28f * density
    if (st.kind == BoardInstrument.COMPASS) return SnapTarget(compass = true)
    var best: SnapTarget? = null; var bestD = snapPx
    instrumentEdges(st, density).forEachIndexed { i, (a, b) ->
        val d = (p - projectOnSegment(p, a, b)).getDistance()
        if (d < bestD) { bestD = d; best = SnapTarget(edge = i) }
    }
    if (st.kind == BoardInstrument.PROTRACTOR) {
        val v = p - st.center
        val ly = v.x * st.nrm.x + v.y * st.nrm.y
        val d = abs(v.getDistance() - st.size)
        if (ly <= 0f && d < bestD) { bestD = d; best = SnapTarget(arc = true) }
    }
    return best
}

/** نقطهٔ چسبیده روی هدف. */
internal fun snapApply(st: InstrumentState, target: SnapTarget, p: Offset, density: Float): Offset {
    val v = p - st.center
    val r = v.getDistance()
    return when {
        target.compass || target.arc -> if (r <= 0f) st.center + st.dir * st.size else st.center + v * (st.size / r)
        target.edge >= 0 -> instrumentEdges(st, density).getOrNull(target.edge)?.let { (a, b) -> projectOnSegment(p, a, b) } ?: p
        else -> p
    }
}

private const val RULER_THICK_DP = 44f

/** رسم ابزار روی بوم (نیمه‌شفاف، با دستگیرهٔ آبی). */
internal fun drawInstrument(canvas: Canvas, st: InstrumentState, density: Float) {
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0x55FDE68A }
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.4f * density; color = 0xCC374151.toInt() }
    val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * density; color = 0xCC374151.toInt() }
    val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF374151.toInt(); textSize = 9f * density; textAlign = Paint.Align.CENTER }
    val handle = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFF2563EB.toInt() }
    val handleRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density; color = 0xFFFFFFFF.toInt() }
    val deg = Math.toDegrees(st.angle.toDouble()).toFloat()
    val cmPx = 10f * density * 1.6f // «سانتی‌متر» نمایشی (۱۶dp)
    val save = canvas.save()
    canvas.translate(st.center.x, st.center.y)
    canvas.rotate(deg)
    when (st.kind) {
        BoardInstrument.RULER -> {
            val t = RULER_THICK_DP * density / 2f
            val r = RectF(-st.size, -t, st.size, t)
            canvas.drawRoundRect(r, 4f * density, 4f * density, fill)
            canvas.drawRoundRect(r, 4f * density, 4f * density, line)
            var i = 0; var x = -st.size
            while (x <= st.size) {
                val len = when { i % 10 == 0 -> 12f; i % 5 == 0 -> 8f; else -> 5f } * density
                canvas.drawLine(x, -t, x, -t + len, tick)
                if (i % 10 == 0) canvas.drawText(FigureDigits.apply((i / 10).toString()), x, -t + len + 9f * density, txt)
                x += cmPx / 10f; i++
            }
        }
        BoardInstrument.SETSQUARE -> {
            val path = Path().apply { moveTo(0f, 0f); lineTo(st.size, 0f); lineTo(0f, -st.size); close() }
            canvas.drawPath(path, fill); canvas.drawPath(path, line)
            // سوراخ میانی و علامت ۹۰°
            canvas.drawCircle(st.size * 0.28f, -st.size * 0.28f, st.size * 0.1f, line)
            canvas.drawRect(0f, -8f * density, 8f * density, 0f, tick)
            // V137.6 — شماره‌گذاری گونیا: هر میلی‌متر تیک، هر ۵ بلندتر، هر سانتی‌متر عدد (روی هر دو لبهٔ عمود).
            var x = 0f; var i = 0
            while (x <= st.size) {
                val len = when { i % 10 == 0 -> 11f; i % 5 == 0 -> 7f; else -> 4f } * density
                canvas.drawLine(x, 0f, x, -len, tick)
                if (i % 10 == 0 && i > 0) canvas.drawText(FigureDigits.apply((i / 10).toString()), x, -len - 3f * density, txt)
                x += cmPx / 10f; i++
            }
            var y = 0f; i = 0
            val txtL = Paint(txt).apply { textAlign = Paint.Align.LEFT }
            while (y <= st.size) {
                val len = when { i % 10 == 0 -> 11f; i % 5 == 0 -> 7f; else -> 4f } * density
                canvas.drawLine(0f, -y, len, -y, tick)
                if (i % 10 == 0 && i > 0) canvas.drawText(FigureDigits.apply((i / 10).toString()), len + 2f * density, -y + 3f * density, txtL)
                y += cmPx / 10f; i++
            }
        }
        BoardInstrument.PROTRACTOR -> {
            val r = st.size
            val rect = RectF(-r, -r, r, r)
            canvas.drawArc(rect, 180f, 180f, true, fill)
            canvas.drawArc(rect, 180f, 180f, true, line)
            canvas.drawLine(-r, 0f, r, 0f, line)
            canvas.drawCircle(0f, 0f, 3f * density, line)
            for (a in 0..180) {
                val rad = Math.toRadians(a.toDouble())
                val len = when { a % 30 == 0 -> 14f; a % 10 == 0 -> 10f; a % 5 == 0 -> 7f; else -> 4f } * density
                val cx = cos(rad).toFloat(); val sy = -sin(rad).toFloat()
                canvas.drawLine(cx * r, sy * r, cx * (r - len), sy * (r - len), tick)
                if (a % 30 == 0) {
                    val tr = r - len - 8f * density
                    // V137.5 — برچسب‌های ۰ و ۱۸۰ روی خطِ پایه می‌افتادند؛ حالا بالای خط و کمی داخل‌تر رسم می‌شوند.
                    val onBase = a == 0 || a == 180
                    val lx = if (onBase) cx * (tr - 5f * density) else cx * tr
                    val ly = if (onBase) -5f * density else sy * tr + 3f * density
                    canvas.drawText(FigureDigits.apply(a.toString()), lx, ly, txt)
                }
            }
        }
        BoardInstrument.COMPASS -> {
            // V137.3 — پرگار واقعی (درخواست کاربر): دو پایهٔ فلزیِ باریک‌شونده که در لولای بالایی
            // به هم می‌رسند، دستگیرهٔ شیاردار روی لولا، پایهٔ سوزن روی مرکز (نقطهٔ قرمز) و پایهٔ
            // مداد (چوب زرد + نوک گرافیتی) روی محیط دایره؛ دایرهٔ راهنما خط‌چین آبی.
            val dash = Paint(line).apply { pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f * density, 6f * density), 0f); color = 0x882563EB.toInt() }
            canvas.drawCircle(0f, 0f, st.size, dash)
            val r = st.size
            val apexX = r / 2f
            val apexY = -(r * 0.85f + 26f * density)
            val metal = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFF9CA3AF.toInt() }
            val metalDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * density; color = 0xFF4B5563.toInt() }
            fun leg(tipX: Float, tipY: Float, topHalf: Float, tipHalf: Float) {
                val dx = tipX - apexX; val dy = tipY - apexY
                val len = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
                val nx = -dy / len; val ny = dx / len
                val path = android.graphics.Path().apply {
                    moveTo(apexX + nx * topHalf, apexY + ny * topHalf)
                    lineTo(tipX + nx * tipHalf, tipY + ny * tipHalf)
                    lineTo(tipX - nx * tipHalf, tipY - ny * tipHalf)
                    lineTo(apexX - nx * topHalf, apexY - ny * topHalf)
                    close()
                }
                canvas.drawPath(path, metal)
                canvas.drawPath(path, metalDark)
            }
            // پایهٔ سوزن: تا ۱۰dp بالای مرکز فلز، بعد سوزن نازک تا خود مرکز.
            val needleTop = 12f * density
            val d1 = kotlin.math.hypot(apexX, apexY).coerceAtLeast(1f)
            val n1x = -apexX / d1 * needleTop; val n1y = -apexY / d1 * needleTop
            leg(n1x, n1y, 4.5f * density, 1.8f * density)
            canvas.drawLine(n1x, n1y, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f * density; color = 0xFF374151.toInt() })
            // پایهٔ مداد: فلز تا نیمهٔ راه، سپس بست فلزی و بدنهٔ زرد مداد تا نزدیک محیط و نوک گرافیتی روی محیط.
            val pdx = r - apexX; val pdy = -apexY
            val pl = kotlin.math.hypot(pdx, pdy).coerceAtLeast(1f)
            val ux = pdx / pl; val uy = pdy / pl
            val metalEnd = 0.55f * pl
            leg(apexX + ux * metalEnd, apexY + uy * metalEnd, 4.5f * density, 3.2f * density)
            val pencil = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f * density; color = 0xFFFACC15.toInt(); strokeCap = Paint.Cap.BUTT }
            val pencilStart = 0.45f * pl; val pencilEnd = pl - 9f * density
            canvas.drawLine(apexX + ux * pencilStart, apexY + uy * pencilStart, apexX + ux * pencilEnd, apexY + uy * pencilEnd, pencil)
            val clamp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 7f * density; color = 0xFF6B7280.toInt() }
            canvas.drawLine(apexX + ux * (metalEnd - 6f * density), apexY + uy * (metalEnd - 6f * density), apexX + ux * (metalEnd + 6f * density), apexY + uy * (metalEnd + 6f * density), clamp)
            val wood = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f * density; color = 0xFFE7C99A.toInt(); strokeCap = Paint.Cap.BUTT }
            canvas.drawLine(apexX + ux * pencilEnd, apexY + uy * pencilEnd, apexX + ux * (pl - 3f * density), apexY + uy * (pl - 3f * density), wood)
            canvas.drawLine(apexX + ux * (pl - 3.5f * density), apexY + uy * (pl - 3.5f * density), r, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f * density; color = 0xFF111827.toInt(); strokeCap = Paint.Cap.ROUND })
            // لولا و دستگیرهٔ شیاردار.
            canvas.drawCircle(apexX, apexY, 7f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B7280.toInt() })
            canvas.drawCircle(apexX, apexY, 2.2f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE5E7EB.toInt() })
            val knobW = 4f * density; val knobH = 16f * density
            canvas.drawRoundRect(apexX - knobW / 2f, apexY - 7f * density - knobH, apexX + knobW / 2f, apexY - 6f * density, 2f * density, 2f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4B5563.toInt() })
            var gy = apexY - 8f * density - knobH + 3f * density
            while (gy < apexY - 8f * density) {
                canvas.drawLine(apexX - knobW / 2f, gy, apexX + knobW / 2f, gy, Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f; color = 0xFF9CA3AF.toInt() })
                gy += 2.5f * density
            }
            canvas.drawCircle(0f, 0f, 3.5f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFDC2626.toInt() })
        }
        BoardInstrument.NONE -> Unit
    }
    canvas.restoreToCount(save)
    val h = st.handle
    canvas.drawCircle(h.x, h.y, 11f * density, handle)
    canvas.drawCircle(h.x, h.y, 11f * density, handleRing)
    // V137.5 — دستگیرهٔ خاکستریِ جابه‌جایی (چهارپیکان)
    val g = instrumentGrip(st, density)
    val gripFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xEE6B7280.toInt() }
    val gripLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.8f * density; color = 0xFFFFFFFF.toInt(); strokeCap = Paint.Cap.ROUND }
    val gr = 13f * density
    canvas.drawCircle(g.x, g.y, gr, gripFill)
    canvas.drawCircle(g.x, g.y, gr, handleRing)
    val a = gr * 0.62f; val hd = 3f * density
    canvas.drawLine(g.x - a, g.y, g.x + a, g.y, gripLine)
    canvas.drawLine(g.x, g.y - a, g.x, g.y + a, gripLine)
    listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach { u ->
        val tip = Offset(g.x + u.x * a, g.y + u.y * a)
        val side = Offset(-u.y, u.x)
        canvas.drawLine(tip.x, tip.y, tip.x - u.x * hd + side.x * hd, tip.y - u.y * hd + side.y * hd, gripLine)
        canvas.drawLine(tip.x, tip.y, tip.x - u.x * hd - side.x * hd, tip.y - u.y * hd - side.y * hd, gripLine)
    }
}

private fun renderBoard(
    canvas: Canvas, w: Float, h: Float, bg: BoardBackground, items: List<BoardStroke>, density: Float,
    bitmaps: Map<String, Bitmap>, selectedId: Long?
) {
    drawBackground(canvas, w, h, bg, density)
    // لایهٔ شفاف: پاک‌کن با CLEAR فقط خطوط را پاک می‌کند و زمینه سالم می‌ماند.
    val layer = canvas.saveLayer(0f, 0f, w, h, null)
    items.forEach { drawItem(canvas, it, density, bitmaps) }
    canvas.restoreToCount(layer)
    selectedId?.let { id -> items.firstOrNull { it.id == id } }?.let { s ->
        val r = boundsOf(s, density, bitmaps)
        r.inset(-6f * density, -6f * density)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.5f * density; color = 0xFF2563EB.toInt()
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f * density, 5f * density), 0f)
        }
        canvas.drawRoundRect(r, 6f * density, 6f * density, p)
    }
}

private fun drawBackground(canvas: Canvas, w: Float, h: Float, bg: BoardBackground, density: Float) {
    canvas.drawColor(android.graphics.Color.WHITE)
    if (bg == BoardBackground.PLAIN) return
    val step = 24f * density
    val grid = Paint().apply { color = 0xFFE5E7EB.toInt(); strokeWidth = 1f }
    when (bg) {
        BoardBackground.LINED -> { var y = step; while (y < h) { canvas.drawLine(0f, y, w, y, grid); y += step } }
        BoardBackground.GRID -> {
            var y = step; while (y < h) { canvas.drawLine(0f, y, w, y, grid); y += step }
            var x = step; while (x < w) { canvas.drawLine(x, 0f, x, h, grid); x += step }
        }
        BoardBackground.DOTS -> {
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCBD5E1.toInt() }
            var y = step; while (y < h) { var x = step; while (x < w) { canvas.drawCircle(x, y, 1.5f * density, dot); x += step }; y += step }
        }
        BoardBackground.AXES -> {
            var y = step; while (y < h) { canvas.drawLine(0f, y, w, y, grid); y += step }
            var x = step; while (x < w) { canvas.drawLine(x, 0f, x, h, grid); x += step }
            val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF374151.toInt(); strokeWidth = 2f * density }
            val cx = (w / 2f / step).toInt() * step; val cy = (h / 2f / step).toInt() * step
            canvas.drawLine(0f, cy, w, cy, axis); canvas.drawLine(cx, 0f, cx, h, axis)
            arrowHead(Offset(0f, cy), Offset(w, cy), axis.strokeWidth).forEach { (p, q) -> canvas.drawLine(p.x, p.y, q.x, q.y, axis) }
            arrowHead(Offset(cx, h), Offset(cx, 0f), axis.strokeWidth).forEach { (p, q) -> canvas.drawLine(p.x, p.y, q.x, q.y, axis) }
            val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B7280.toInt(); textSize = 10f * density; textAlign = Paint.Align.CENTER }
            var i = 1
            while (cx + i * step < w || cx - i * step > 0) {
                val pos = FigureDigits.apply("$i"); val neg = FigureDigits.apply("-$i")
                if (cx + i * step < w) canvas.drawText(pos, cx + i * step, cy + 14f * density, tick)
                if (cx - i * step > 0) canvas.drawText(neg, cx - i * step, cy + 14f * density, tick)
                if (cy - i * step > 0) canvas.drawText(pos, cx - 10f * density, cy - i * step + 4f * density, tick)
                if (cy + i * step < h) canvas.drawText(neg, cx - 10f * density, cy + i * step + 4f * density, tick)
                i++
            }
        }
        BoardBackground.PLAIN -> Unit
    }
}

private fun textPaint(s: BoardStroke, density: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = s.color.toArgb(); textSize = s.textSizeSp * density; textAlign = Paint.Align.RIGHT
}

private fun drawItem(canvas: Canvas, s: BoardStroke, density: Float, bitmaps: Map<String, Bitmap>) {
    val a = s.points.first(); val b = s.points.last()
    if (s.tool == BoardTool.IMAGE) {
        val bmp = bitmaps[s.ref]
        val r = rectOf(a, b)
        if (bmp != null) canvas.drawBitmap(bmp, null, r, Paint(Paint.FILTER_BITMAP_FLAG))
        else {
            val ph = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFF9CA3AF.toInt(); strokeWidth = density }
            canvas.drawRect(r, ph)
        }
        return
    }
    if (s.tool == BoardTool.TEXT) {
        val tp = textPaint(s, density)
        var y = a.y
        s.text.split('\n').forEach { line -> canvas.drawText(line, a.x, y, tp); y += tp.textSize * 1.3f }
        return
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        color = s.color.toArgb()
        strokeWidth = s.width * density
        when (s.tool) {
            BoardTool.ERASER -> { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); strokeWidth = s.width * 3f * density }
            BoardTool.HIGHLIGHT -> { alpha = 90; strokeWidth = s.width * 3f * density; strokeCap = Paint.Cap.SQUARE }
            else -> Unit
        }
    }
    when (s.tool) {
        BoardTool.PEN, BoardTool.ERASER, BoardTool.HIGHLIGHT -> {
            if (s.points.size < 2 || s.points.all { it == a }) {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(a.x, a.y, paint.strokeWidth / 2f, paint)
            } else {
                val p = Path().apply {
                    moveTo(a.x, a.y)
                    for (i in 1 until s.points.size) {
                        val prev = s.points[i - 1]; val cur = s.points[i]
                        quadTo(prev.x, prev.y, (prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
                    }
                    lineTo(b.x, b.y)
                }
                canvas.drawPath(p, paint)
            }
        }
        BoardTool.LINE -> canvas.drawLine(a.x, a.y, b.x, b.y, paint)
        BoardTool.ARROW -> {
            canvas.drawLine(a.x, a.y, b.x, b.y, paint)
            arrowHead(a, b, paint.strokeWidth).forEach { (p, q) -> canvas.drawLine(p.x, p.y, q.x, q.y, paint) }
        }
        BoardTool.RECT -> canvas.drawRect(rectOf(a, b), paint)
        BoardTool.CIRCLE -> canvas.drawOval(rectOf(a, b), paint)
        BoardTool.TRIANGLE -> {
            val r = rectOf(a, b)
            val p = Path().apply { moveTo(r.centerX(), r.top); lineTo(r.right, r.bottom); lineTo(r.left, r.bottom); close() }
            canvas.drawPath(p, paint)
        }
        else -> Unit
    }
}

private fun boundsOf(s: BoardStroke, density: Float, bitmaps: Map<String, Bitmap>): RectF {
    val a = s.points.first()
    return when (s.tool) {
        BoardTool.TEXT -> {
            val tp = textPaint(s, density)
            val lines = s.text.split('\n')
            val w = lines.maxOf { tp.measureText(it) }
            RectF(a.x - w, a.y - tp.textSize, a.x, a.y + tp.textSize * 1.3f * (lines.size - 1) + tp.textSize * 0.3f)
        }
        BoardTool.IMAGE -> rectOf(a, s.points.last())
        else -> {
            val r = RectF(a.x, a.y, a.x, a.y)
            s.points.forEach { r.union(it.x, it.y) }
            val pad = s.width * density / 2f
            r.inset(-pad, -pad); r
        }
    }
}

/** بالاترین شیءِ زیر انگشت (آخرین کشیده‌شده اولویت دارد). */
private fun hitTest(items: List<BoardStroke>, p: Offset, density: Float, bitmaps: Map<String, Bitmap>): BoardStroke? {
    val slop = 14f * density
    return items.asReversed().firstOrNull { s ->
        if (s.tool == BoardTool.ERASER) return@firstOrNull false
        val r = boundsOf(s, density, bitmaps)
        r.inset(-slop, -slop)
        if (!r.contains(p.x, p.y)) return@firstOrNull false
        when (s.tool) {
            BoardTool.PEN, BoardTool.HIGHLIGHT, BoardTool.LINE, BoardTool.ARROW -> {
                val tol = slop + s.width * density
                if (s.points.size == 1) return@firstOrNull true
                (1 until s.points.size).any { i -> distToSegment(p, s.points[i - 1], s.points[i]) <= tol } ||
                    (s.tool == BoardTool.LINE || s.tool == BoardTool.ARROW) && distToSegment(p, s.points.first(), s.points.last()) <= tol
            }
            else -> true
        }
    }
}

private fun distToSegment(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x; val dy = b.y - a.y
    val len2 = dx * dx + dy * dy
    if (len2 < 1e-3f) return hypot(p.x - a.x, p.y - a.y)
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / len2).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

private fun scaleItem(s: BoardStroke, f: Float): BoardStroke = when (s.tool) {
    BoardTool.TEXT -> s.copy(textSizeSp = (s.textSizeSp * f).coerceIn(8f, 120f))
    BoardTool.IMAGE -> {
        val a = s.points.first(); val b = s.points.last()
        val w = (b.x - a.x) * f; val h = (b.y - a.y) * f
        if (abs(w) < 24f || abs(h) < 24f) s else s.copy(points = listOf(a, Offset(a.x + w, a.y + h)))
    }
    else -> s
}

private fun rectOf(a: Offset, b: Offset) = RectF(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))

private fun arrowHead(a: Offset, b: Offset, w: Float): List<Pair<Offset, Offset>> {
    val len = max(14f, w * 4f)
    if (hypot(b.x - a.x, b.y - a.y) < 1f) return emptyList()
    val ang = atan2(b.y - a.y, b.x - a.x)
    val l = Offset(b.x - len * cos(ang - 0.5f), b.y - len * sin(ang - 0.5f))
    val r = Offset(b.x - len * cos(ang + 0.5f), b.y - len * sin(ang + 0.5f))
    return listOf(b to l, b to r)
}

private fun formatBoardTime(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val h = safe / 3_600L; val m = (safe % 3_600L) / 60L; val s = safe % 60L
    return if (h > 0L) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// ---------------------------------------------------------------- بیت‌مپ فرمول/شکل

private fun texBitmap(tex: String, density: Float): Bitmap? {
    val doc = NativeMathSvgRenderer.render(tex, fontSizePx = 28f * density, color = "#111111")
    return svgBitmap(doc.xml, doc.widthPx, doc.heightPx, 1f)
}

private fun figureBitmap(context: android.content.Context, spec: FigureSpec): Bitmap? {
    if (spec.kind in setOf("a", "s")) return AtlasBitmapRenderer.render(context, spec)
    val doc = FigureSvgRenderer.render(spec)
    return svgBitmap(doc.xml, doc.widthPx, doc.heightPx, 2f)
}

private fun svgBitmap(xml: String, wPx: Float, hPx: Float, scale: Float): Bitmap? = runCatching {
    val svg = com.caverock.androidsvg.SVG.getFromString(xml)
    val w = (wPx * scale).toInt().coerceIn(1, 4096); val h = (hPx * scale).toInt().coerceIn(1, 4096)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    svg.documentWidth = w.toFloat(); svg.documentHeight = h.toFloat()
    svg.renderToCanvas(Canvas(bmp))
    bmp
}.getOrNull()

// ---------------------------------------------------------------- پیش‌نویس (ذخیرهٔ خودکار)

private class BoardDraft(val items: List<BoardStroke>, val background: BoardBackground, val pageCount: Int)

private fun boardDraftFile(filesDir: File, questionId: String): File =
    File(File(filesDir, "whiteboard").apply { mkdirs() }, "draft-${questionId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(60)}.json")

private fun writeBoardDraft(file: File, items: List<BoardStroke>, bg: BoardBackground, pageCount: Int) {
    val arr = JsonArray(items.map { s ->
        JsonObject(
            mapOf(
                "id" to JsonPrimitive(s.id), "tool" to JsonPrimitive(s.tool.name), "color" to JsonPrimitive(s.color.toArgb()),
                "width" to JsonPrimitive(s.width), "text" to JsonPrimitive(s.text), "size" to JsonPrimitive(s.textSizeSp),
                "ref" to JsonPrimitive(s.ref), "page" to JsonPrimitive(s.page),
                "pts" to JsonArray(s.points.flatMap { listOf(JsonPrimitive(it.x), JsonPrimitive(it.y)) })
            )
        )
    })
    val root = JsonObject(mapOf("v" to JsonPrimitive(1), "bg" to JsonPrimitive(bg.name), "pages" to JsonPrimitive(pageCount), "items" to arr))
    val tmp = File(file.parentFile, file.name + ".tmp")
    tmp.writeText(root.toString()); if (!tmp.renameTo(file)) { file.writeText(root.toString()); tmp.delete() }
}

private fun readBoardDraft(file: File): BoardDraft? {
    if (!file.isFile) return null
    val root = Json.parseToJsonElement(file.readText()).jsonObject
    val bg = runCatching { BoardBackground.valueOf(root["bg"]?.jsonPrimitive?.contentOrNull ?: "PLAIN") }.getOrDefault(BoardBackground.PLAIN)
    val pages = root["pages"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
    val items = (root["items"] as? JsonArray).orEmpty().mapNotNull { e ->
        val o = e as? JsonObject ?: return@mapNotNull null
        val tool = runCatching { BoardTool.valueOf(o["tool"]?.jsonPrimitive?.contentOrNull ?: "") }.getOrNull() ?: return@mapNotNull null
        val nums = o["pts"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull?.toFloatOrNull() }
        if (nums.size < 2) return@mapNotNull null
        val pts = nums.chunked(2).filter { it.size == 2 }.map { Offset(it[0], it[1]) }
        BoardStroke(
            id = o["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: System.nanoTime(),
            tool = tool, points = pts,
            color = Color(o["color"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0xFF1F2937.toInt()),
            width = o["width"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 5f,
            text = o["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            textSizeSp = o["size"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 18f,
            ref = o["ref"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            page = o["page"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        )
    }
    return BoardDraft(items, bg, pages)
}

/**
 * رندر یک صفحه روی Bitmap کدرِ سفید و ذخیره به PNG در filesDir/whiteboard؛ خروجی `file:`.
 * V137.1 — کدر (setHasAlpha=false) تا در هیچ مسیر فشرده‌سازی/نمایشی سیاه نشود؛ filesDir به‌جای cache
 * تا پیش از ارسال (که ممکن است ساعت‌ها بعد باشد) توسط سیستم پاک نشود.
 */
private fun exportBoard(
    filesDir: File, questionId: String, page: Int, size: IntSize, strokes: List<BoardStroke>, background: BoardBackground,
    density: Float, bitmaps: Map<String, Bitmap>
): String {
    val w = size.width.coerceAtLeast(1); val h = size.height.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    renderBoard(canvas, w.toFloat(), h.toFloat(), background, strokes, density, bitmaps, null)
    // هر پیکسل شفافِ باقی‌مانده (پس از پاک‌کن) روی سفید می‌نشیند.
    val flat = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(flat).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(bmp, 0f, 0f, null) }
    bmp.recycle()
    flat.setHasAlpha(false)
    val dir = File(filesDir, "whiteboard").apply { mkdirs() }
    val out = File(dir, "wb-${questionId.take(8)}-p$page-${System.currentTimeMillis()}.png")
    FileOutputStream(out).use { flat.compress(Bitmap.CompressFormat.PNG, 100, it) }
    flat.recycle()
    return out.toURI().toString()
}
