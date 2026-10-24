package ir.exam.app.ui.student

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.AutoFixNormal
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
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
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import ir.exam.app.core.figure.AtlasBitmapRenderer
import ir.exam.app.core.figure.FigureDigits
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.io.DocumentIo
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

/**
 * V335.18 — آیکن‌های برداری تازهٔ تخته (پیکان دوسر، مثلث قائم، لوزی و محور سریع)؛
 * مستقل از آیکن‌های آماده تا هیچ نام آیکنی به نسخهٔ کتابخانه وابسته نباشد.
 */
internal object BoardVectorIcons {
    private val ink = SolidColor(Color(0xFF1F2937))

    val DoubleArrow: ImageVector by lazy {
        ImageVector.Builder("Board.DoubleArrow", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(4f, 12f); lineTo(20f, 12f)
            }
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(8f, 8.4f); lineTo(4f, 12f); lineTo(8f, 15.6f)
            }
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(16f, 8.4f); lineTo(20f, 12f); lineTo(16f, 15.6f)
            }
        }.build()
    }

    val RightTriangle: ImageVector by lazy {
        ImageVector.Builder("Board.RightTriangle", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(5f, 5f); lineTo(5f, 19f); lineTo(19f, 19f); close()
            }
            path(stroke = ink, strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(5f, 14f); lineTo(10f, 14f); lineTo(10f, 19f)
            }
        }.build()
    }

    val Diamond: ImageVector by lazy {
        ImageVector.Builder("Board.Diamond", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(12f, 3.5f); lineTo(20.5f, 12f); lineTo(12f, 20.5f); lineTo(3.5f, 12f); close()
            }
        }.build()
    }

    val Axes: ImageVector by lazy {
        ImageVector.Builder("Board.Axes", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(5f, 20f); lineTo(5f, 4f)
            }
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(2f, 7.5f); lineTo(5f, 4f); lineTo(8f, 7.5f)
            }
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(4f, 20f); lineTo(20f, 20f)
            }
            path(stroke = ink, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = null) {
                moveTo(16.5f, 17f); lineTo(20f, 20f); lineTo(16.5f, 23f)
            }
        }.build()
    }
}

/** ابزارهای تخته (V137 + V137.1: انتخاب، هایلایتر، پاک‌کن شیء، مثلث، تصویر؛ V335.18: پیکان دوسر، مثلث قائم، لوزی، محور سریع). */
internal enum class BoardTool(val label: String, val pickable: Boolean = true) {
    PEN("قلم"), HIGHLIGHT("هایلایتر"), ERASER("پاک‌کن"), OBJ_ERASER("پاک‌کن شیء"), SELECT("انتخاب/جابه‌جایی"),
    LINE("خط"), ARROW("پیکان"), ARROW2("پیکان دوسر"), RECT("مستطیل"), CIRCLE("دایره"), TRIANGLE("مثلث"),
    RTRIANGLE("مثلث قائم"), DIAMOND("لوزی"), AXES("محور سریع"), TEXT("متن"),
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
            ARROW2 -> BoardVectorIcons.DoubleArrow
            RECT -> Icons.Outlined.CropSquare
            CIRCLE -> Icons.Outlined.RadioButtonUnchecked
            TRIANGLE -> Icons.Outlined.ChangeHistory
            RTRIANGLE -> BoardVectorIcons.RightTriangle
            DIAMOND -> BoardVectorIcons.Diamond
            AXES -> BoardVectorIcons.Axes
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
    val page: Int = 0,
    /** V335.18 — برچسب وسط شکل‌های بسته (قابل افزودن/ویرایش/حذف). */
    val label: String = "",
    /** V335.18 — سبک خط همانند انتخابگر سه‌حالتهٔ سایت. */
    val dash: BoardDash = BoardDash.SOLID,
    /** V335.18 — توپرِ ملایم برای شکل‌های بسته. */
    val fill: Boolean = false
) {
    val eraser: Boolean get() = tool == BoardTool.ERASER
    /** شکل بسته‌ای که برچسب و توپرِ ملایم دارد. */
    val closedShape: Boolean
        get() = tool == BoardTool.RECT || tool == BoardTool.CIRCLE || tool == BoardTool.TRIANGLE ||
            tool == BoardTool.RTRIANGLE || tool == BoardTool.DIAMOND
    /** خط‌های دو سرِ قابل تغییر (پیکان‌ها و خط). */
    val segmented: Boolean
        get() = tool == BoardTool.LINE || tool == BoardTool.ARROW || tool == BoardTool.ARROW2
    fun moved(d: Offset) = copy(points = points.map { it + d })
}

/**
 * V335.18 — سبک خط (ممتد/خط‌چین/نقطه‌چین)، هم‌راستا با انتخابگر سه‌حالتهٔ سایت.
 */
internal enum class BoardDash(val label: String) { SOLID("ممتد"), DASHED("خط‌چین"), DOTTED("نقطه‌چین") }

/**
 * V335.18 — جعبهٔ محدودکنندهٔ مستقل از اندروید (چپ/بالا/راست/پایین) تا محاسبات انتخاب،
 * تغییر اندازه و قرینه در Kotlin خالص بمانند و با ماوس/لمس یکسان کار کنند.
 */
internal data class BoardBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val normal: BoardBox get() = BoardBox(min(left, right), min(top, bottom), max(left, right), max(top, bottom))
    fun union(o: BoardBox): BoardBox = BoardBox(min(left, o.left), min(top, o.top), max(right, o.right), max(bottom, o.bottom))
    fun contains(x: Float, y: Float, slop: Float = 0f): Boolean =
        x >= left - slop && x <= right + slop && y >= top - slop && y <= bottom + slop
    fun intersects(o: BoardBox): Boolean {
        val a = normal; val b = o.normal
        return a.left <= b.right && b.left <= a.right && a.top <= b.bottom && b.top <= a.bottom
    }
}

/** V335.18 — دستگیره‌های تغییر اندازه: هشت گوشه/ضلع + دو سرِ خط. */
internal enum class BoardHandle(val hx: Float, val hy: Float) {
    NW(0f, 0f), N(0.5f, 0f), NE(1f, 0f), E(1f, 0.5f), SE(1f, 1f), S(0.5f, 1f), SW(0f, 1f), W(0f, 0.5f),
    P1(-1f, -1f), P2(-1f, -1f);

    val isEndpoint: Boolean get() = this == P1 || this == P2
}

/** گوشه‌های قابل‌کشیدن تغییر اندازه (برای رسم و برخورد). */
internal val BOARD_RESIZE_HANDLES = listOf(
    BoardHandle.NW, BoardHandle.N, BoardHandle.NE, BoardHandle.E, BoardHandle.SE, BoardHandle.S, BoardHandle.SW, BoardHandle.W
)

/** مختصات دستگیرهٔ گوشه/ضلع روی جعبه. */
internal fun boardHandlePoint(box: BoardBox, h: BoardHandle): Offset {
    val b = box.normal
    return Offset(b.left + b.width * h.hx, b.top + b.height * h.hy)
}

/** دستگیرهٔ زیر انگشت (۱۸dp تلورانس)؛ سرهای خط فقط وقتی یک خط تنها انتخاب شده باشد. */
internal fun boardHandleAt(box: BoardBox, p: Offset, density: Float, endpointItem: BoardStroke?): BoardHandle? {
    val reach = 18f * density
    if (endpointItem != null && endpointItem.points.size >= 2) {
        if ((p - endpointItem.points.first()).getDistance() <= reach) return BoardHandle.P1
        if ((p - endpointItem.points.last()).getDistance() <= reach) return BoardHandle.P2
    }
    var best: BoardHandle? = null
    var bestD = reach
    BOARD_RESIZE_HANDLES.forEach { h ->
        val d = (p - boardHandlePoint(box, h)).getDistance()
        if (d <= bestD) { bestD = d; best = h }
    }
    return best
}

/** جعبهٔ تازه پس از کشیدن دستگیره؛ با `keepRatio` (کلید Shift) نسبت ابعاد حفظ می‌شود. */
internal fun resizedBoardBox(box: BoardBox, h: BoardHandle, dx: Float, dy: Float, keepRatio: Boolean, minSize: Float): BoardBox {
    val ob = box.normal
    if (h.isEndpoint) return ob
    var l = ob.left; var t = ob.top; var r = ob.right; var b = ob.bottom
    if (h.hx <= 0f) l += dx
    if (h.hx >= 1f) r += dx
    if (h.hy <= 0f) t += dy
    if (h.hy >= 1f) b += dy
    var nb = BoardBox(min(l, r), min(t, b), max(l, r), max(t, b))
    if (keepRatio && ob.width > 1f && ob.height > 1f) {
        val k = max(nb.width / ob.width, nb.height / ob.height)
        val ax = if (h.hx <= 0.5f) ob.right else ob.left
        val ay = if (h.hy <= 0.5f) ob.bottom else ob.top
        val nw = ob.width * k; val nh = ob.height * k
        val left = if (h.hx <= 0.5f) ax - nw else ax
        val top = if (h.hy <= 0.5f) ay - nh else ay
        nb = BoardBox(left, top, left + nw, top + nh)
    }
    return if (nb.width < minSize || nb.height < minSize) ob else nb
}

/** نگاشت همهٔ نقاط شیء از جعبهٔ `from` به جعبهٔ `to` (بزرگ/کوچک کردن متناسب با خود شیء). */
internal fun scaleBoardStroke(s: BoardStroke, from: BoardBox, to: BoardBox): BoardStroke {
    val fb = from.normal; val tb = to.normal
    val sx = if (fb.width > 1f) tb.width / fb.width else 1f
    val sy = if (fb.height > 1f) tb.height / fb.height else 1f
    val k = (abs(sx) + abs(sy)) / 2f
    return s.copy(
        points = s.points.map { Offset(tb.left + (it.x - fb.left) * sx, tb.top + (it.y - fb.top) * sy) },
        textSizeSp = (s.textSizeSp * k).coerceIn(8f, 160f),
        width = (s.width * k).coerceIn(0.8f, 40f)
    )
}

/** قرینهٔ افقی/عمودی هر شیء حول جعبهٔ خودش (جهت پیکان برعکس می‌شود). */
internal fun flipBoardStroke(s: BoardStroke, horizontal: Boolean): BoardStroke {
    val xs = s.points.map { it.x }; val ys = s.points.map { it.y }
    val l = xs.minOrNull() ?: 0f; val r = xs.maxOrNull() ?: 0f
    val t = ys.minOrNull() ?: 0f; val b = ys.maxOrNull() ?: 0f
    return s.copy(points = s.points.map { if (horizontal) Offset(l + r - it.x, it.y) else Offset(it.x, t + b - it.y) })
}

/** جعبهٔ اجتماع همهٔ شیءهای داده‌شده. */
internal fun unionBoardBox(items: List<BoardStroke>, density: Float): BoardBox? {
    var box: BoardBox? = null
    items.forEach { s -> val b = boundsOf(s, density); box = box?.union(b) ?: b }
    return box
}

private val BOARD_PALETTE = listOf(
    Color(0xFF1F2937), Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFF2E7D32),
    Color(0xFFF57C00), Color(0xFF7B1FA2), Color(0xFF00838F), Color(0xFF6D4C41), Color(0xFFFFFFFF)
)
private val BOARD_WIDTHS = listOf(1.5f to "مویی", 3f to "نازک", 5f to "معمولی", 8f to "متوسط", 12f to "ضخیم", 18f to "خیلی‌ضخیم")
private const val MAX_PAGES = 6

/** V335.18 — سقف تاریخچهٔ برگشت/جلو (۴۰ مرحله، هم‌راستا با سایت). */
private const val UNDO_LIMIT = 40

/**
 * V136 — «تخته وایت‌برد» پاسخ دانش‌آموز.
 * V137 — ابزارهای بیشتر، رنگ/ضخامت، زمینه، برگشت/جلو.
 * V137.1 — متن و هر شیء قابل انتخاب/جابه‌جایی/تغییر رنگ/ویرایش/حذف/بزرگ‌وکوچک؛ هایلایتر؛ پاک‌کن دقیق
 * (لایهٔ شفاف، زمینه سالم می‌ماند) و پاک‌کن شیء؛ مثلث؛ درج فرمول (ویرایشگر فرمول)، نمودار/محور/شکل/جدول/
 * فیزیک/شیمی (همان ابزارهای سازنده) و تصویر سؤال روی تخته؛ چند صفحه؛ ذخیرهٔ خودکار پیش‌نویس در filesDir؛
 * تأیید پیش از ثبت؛ شمارهٔ سؤال و زمان باقی‌مانده در سربرگ؛ میان‌بر Ctrl+Z / Ctrl+Y / Delete؛
 * خروجی هر صفحه یک PNG کدر (بدون شفافیت) در filesDir (نه cache) تا تا زمان ارسال پاک نشود.
 * V335.18 — بخش کامل «انتخاب و ویرایش موارد درج‌شده»: انتخاب تکی/چندگانه و کمند، جابه‌جایی با کشیدن و
 * کلیدهای جهت، تغییر اندازه با ۸ دستگیره (Shift = حفظ تناسب) و ۲ سرِ خط، ویرایش متن/برچسب شکل،
 * تکثیر، قرینه، بزرگ/کوچک‌تر، حذف، انتخاب همه (Ctrl+A)، میان‌بر Ctrl+D، سبک خط (ممتد/خط‌چین/نقطه‌چین)،
 * توپرِ ملایم، ابزارهای تازه (پیکان دوسر، مثلث قائم، لوزی، محور سریع)، کپی صفحه، ذخیرهٔ PNG و
 * تاریخچهٔ ۴۰ مرحله‌ای برگشت/جلو برای همهٔ عملیات‌ها.
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
    val imageLoader = coil.Coil.imageLoader(context)
    val scope = rememberCoroutineScope()
    val strokes = remember(questionId) { mutableStateListOf<BoardStroke>() }
    // V335.18 — تاریخچهٔ عکس‌فوری (۴۰ مرحله) تا همهٔ عملیات‌های چندشیئی هم دقیق برگردند.
    val undoStack = remember(questionId) { mutableStateListOf<List<BoardStroke>>() }
    val redoStack = remember(questionId) { mutableStateListOf<List<BoardStroke>>() }
    val bitmaps = remember(questionId) { mutableStateMapOf<String, Bitmap>() }
    val selectedIds = remember(questionId) { mutableStateListOf<Long>() }
    var draft by remember { mutableStateOf<BoardStroke?>(null) }
    var tool by remember { mutableStateOf(BoardTool.PEN) }
    var autoShape by remember { mutableStateOf(false) } // V264
    var color by remember { mutableStateOf(BOARD_PALETTE.first()) }
    var width by remember { mutableStateOf(5f) }
    var dash by remember { mutableStateOf(BoardDash.SOLID) }
    var fillShape by remember { mutableStateOf(false) }
    var background by remember { mutableStateOf(BoardBackground.PLAIN) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var page by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var marquee by remember { mutableStateOf<BoardBox?>(null) }
    var multiPick by remember { mutableStateOf(false) }
    var shiftDown by remember { mutableStateOf(false) }
    var textAt by remember { mutableStateOf<Offset?>(null) }
    var editId by remember { mutableStateOf<Long?>(null) }
    var textInput by remember { mutableStateOf("") }
    var labelInput by remember { mutableStateOf("") }
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
    var savePage by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    val draftFile = remember(questionId) { boardDraftFile(context.filesDir, questionId) }
    // V335.18 — ذخیرهٔ مستقیم تصویر صفحه روی دستگاه (بدون اشتراک‌گذاری، با SAF امنِ DocumentIo).
    val pngSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) scope.launch {
            val bytes = exportBoardPng(boardSize, strokes.filter { it.page == savePage }, background, density, bitmaps)
            if (bytes == null) {
                Toast.makeText(context, "ساخت تصویر تخته ناموفق بود.", Toast.LENGTH_LONG).show()
                return@launch
            }
            val result = DocumentIo.writeBytes(context, uri, bytes)
            Toast.makeText(
                context,
                if (result.isSuccess) "تصویر تخته ذخیره شد." else DocumentIo.WRITE_FAILED,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun newId(): Long = ++nextId
    fun bump() { revision++ }
    fun snapshot() {
        undoStack.add(strokes.toList())
        while (undoStack.size > UNDO_LIMIT) undoStack.removeAt(0)
        redoStack.clear()
    }
    fun restoreSnapshot(items: List<BoardStroke>) {
        strokes.clear(); strokes.addAll(items); selectedIds.clear(); bump()
    }
    fun commit(s: BoardStroke) { snapshot(); strokes.add(s); bump() }
    fun replaceOne(s: BoardStroke) {
        val i = strokes.indexOfFirst { it.id == s.id }
        if (i >= 0) { strokes[i] = s; bump() }
    }
    fun pageItems(): List<BoardStroke> = strokes.filter { it.page == page }
    fun selectOnly(id: Long) { selectedIds.clear(); selectedIds.add(id) }
    fun selectAllOnPage() { selectedIds.clear(); selectedIds.addAll(pageItems().map { it.id }) }
    fun selection(): List<BoardStroke> = strokes.filter { it.id in selectedIds }
    fun selectionBox(): BoardBox? = unionBoardBox(selection(), density)
    fun deleteSelected() {
        if (selectedIds.isEmpty()) return
        snapshot()
        val ids = selectedIds.toSet()
        strokes.removeAll { it.id in ids }
        selectedIds.clear(); bump()
    }
    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(strokes.toList())
        restoreSnapshot(undoStack.removeAt(undoStack.lastIndex))
    }
    fun redoLast() {
        if (redoStack.isEmpty()) return
        undoStack.add(strokes.toList())
        restoreSnapshot(redoStack.removeAt(redoStack.lastIndex))
    }
    fun nudgeBy(dx: Float, dy: Float) {
        if (selectedIds.isEmpty()) return
        snapshot()
        val d = Offset(dx, dy)
        selection().forEach { replaceOne(it.moved(d)) }
    }
    fun applyToSelection(change: (BoardStroke) -> BoardStroke) {
        if (selectedIds.isEmpty()) return
        snapshot()
        selection().forEach { replaceOne(change(it)) }
    }
    fun duplicateSelected() {
        if (selectedIds.isEmpty()) return
        snapshot()
        val gap = Offset(14f * density, 14f * density)
        val copies = selection().map { it.copy(id = newId(), points = it.points.map { p -> p + gap }) }
        strokes.addAll(copies)
        selectedIds.clear(); selectedIds.addAll(copies.map { it.id })
        bump()
    }
    fun flipSelected(horizontal: Boolean) {
        if (selectedIds.isEmpty()) return
        snapshot()
        selection().forEach { replaceOne(flipBoardStroke(it, horizontal)) }
    }
    fun scaleSelected(factor: Float) {
        if (selectedIds.isEmpty()) return
        val from = selectionBox() ?: return
        val halfW = from.width * factor / 2f
        val halfH = from.height * factor / 2f
        val to = BoardBox(from.centerX - halfW, from.centerY - halfH, from.centerX + halfW, from.centerY + halfH)
        snapshot()
        selection().forEach { replaceOne(scaleBoardStroke(it, from, to)) }
    }
    fun openEditorForSelection() {
        val s = selection().singleOrNull() ?: return
        editId = s.id; textInput = s.text; labelInput = s.label
    }

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
            commit(item); selectOnly(item.id); tool = BoardTool.SELECT
        }
    }

    // V335.18 — درج تصویر از گالری دستگاه (هم‌راستا با ابزار «درج تصویر» سایت)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            insertImage("img:$uri")
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
        undoStack.clear(); redoStack.clear(); selectedIds.clear()
        restored = true
        runCatching { focus.requestFocus() }
    }
    // ذخیرهٔ خودکار (با تأخیر کوتاه پس از آخرین تغییر)
    LaunchedEffect(revision, background, pageCount) {
        if (!restored) return@LaunchedEffect
        delay(700)
        val snapshotItems = strokes.toList()
        withContext(Dispatchers.IO) { runCatching { writeBoardDraft(draftFile, snapshotItems, background, pageCount) } }
    }

    val selectionCount = selectedIds.size
    val selectedSingle = selection().singleOrNull()
    val selBadge = when {
        selectionCount == 0 -> "بدون انتخاب"
        selectionCount == 1 -> "انتخاب‌شده: " + (selectedSingle?.tool?.label ?: "مورد")
        else -> FigureDigits.apply(selectionCount.toString()) + " مورد انتخاب‌شده"
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(
            Modifier
                .fillMaxSize()
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { e ->
                    val down = e.type == KeyEventType.KeyDown
                    when {
                        e.key == Key.ShiftLeft || e.key == Key.ShiftRight -> { shiftDown = down; false }
                        !down -> false
                        e.isCtrlPressed && e.key == Key.Z -> { undo(); true }
                        e.isCtrlPressed && e.key == Key.Y -> { redoLast(); true }
                        e.isCtrlPressed && e.key == Key.A -> { selectAllOnPage(); true }
                        e.isCtrlPressed && e.key == Key.D -> { duplicateSelected(); true }
                        (e.key == Key.Delete || e.key == Key.Backspace) && selectedIds.isNotEmpty() -> { deleteSelected(); true }
                        selectedIds.isNotEmpty() && (
                            e.key == Key.DirectionLeft || e.key == Key.DirectionRight ||
                                e.key == Key.DirectionUp || e.key == Key.DirectionDown
                            ) -> {
                            val step = (if (e.isShiftPressed) 10f else 2f) * density
                            when (e.key) {
                                Key.DirectionLeft -> nudgeBy(-step, 0f)
                                Key.DirectionRight -> nudgeBy(step, 0f)
                                Key.DirectionUp -> nudgeBy(0f, -step)
                                else -> nudgeBy(0f, step)
                            }
                            true
                        }
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
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) { Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "برگشت") }
                    IconButton(onClick = { redoLast() }, enabled = redoStack.isNotEmpty()) { Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "جلو") }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.error) }
                }
                // ابزارها — V137.5: آیکون به‌جای متن (نام ابزار در contentDescription/کنار ابزار انتخاب‌شده)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    BoardTool.values().filter { it.pickable }.forEach { t ->
                        BoardIconChip(selected = tool == t, icon = t.icon, label = t.label, onClick = { tool = t; if (t != BoardTool.SELECT) selectedIds.clear() })
                    }
                    BoardIconChip(selected = insertMenu, icon = Icons.Outlined.AddBox, label = "درج…", onClick = { insertMenu = !insertMenu })
                    // V264 — تشخیص شکل: خط/دایره/مثلث/مستطیلِ دست‌آزاد با قلم، پس از رها کردن انگشت به شکل منظم تبدیل می‌شود
                    BoardIconChip(selected = autoShape, icon = Icons.Outlined.AutoFixHigh, label = "تشخیص شکل", onClick = { autoShape = !autoShape; if (autoShape) tool = BoardTool.PEN })
                    Text(if (autoShape && tool == BoardTool.PEN) "قلم + تشخیص شکل" else tool.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                        FilterChip(selected = false, onClick = { insertMenu = false; imagePicker.launch("image/*") }, label = { Text("تصویر از گالری") })
                        listOf("graph" to "نمودار تابع", "axis" to "محور", "figure" to "شکل هندسی", "table" to "جدول", "physics" to "فیزیک", "chemistry" to "شیمی", "periodic" to "جدول تناوبی").forEach { (id, label) ->
                            FilterChip(selected = false, onClick = { insertMenu = false; figureTool = id }, label = { Text(label) })
                        }
                        questionImages.forEachIndexed { i, url ->
                            FilterChip(selected = false, onClick = { insertMenu = false; insertImage("img:$url") }, label = { Text("تصویر سؤال ${i + 1}") })
                        }
                    }
                }
                // رنگ‌ها + ضخامت (روی همهٔ موارد انتخاب‌شده هم اعمال می‌شود)
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
                                    if (selectedIds.isNotEmpty()) applyToSelection { it.copy(color = c) }
                                }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    BOARD_WIDTHS.forEach { (w, label) ->
                        FilterChip(
                            selected = width == w,
                            onClick = {
                                width = w
                                if (selectedIds.isNotEmpty()) applyToSelection { if (it.tool == BoardTool.IMAGE) it else it.copy(width = w) }
                            },
                            label = { Text(label) }
                        )
                    }
                }
                // V335.18 — بخش اختصاصی «انتخاب و ویرایش موارد درج‌شده»
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "انتخاب و ویرایش موارد درج‌شده",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(selBadge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = tool == BoardTool.SELECT, onClick = { tool = BoardTool.SELECT }, label = { Text("↖ ابزار انتخاب") })
                        FilterChip(selected = multiPick, onClick = { multiPick = !multiPick }, label = { Text("⊕ انتخاب چندگانه") })
                        TextButton(onClick = { selectAllOnPage() }, enabled = pageItems().isNotEmpty()) { Text("⊞ انتخاب همه") }
                        TextButton(onClick = { openEditorForSelection() }, enabled = selectionCount == 1) { Text("✏️ ویرایش") }
                        TextButton(onClick = { duplicateSelected() }, enabled = selectionCount > 0) { Text("⧉ تکثیر مورد") }
                        TextButton(onClick = { scaleSelected(1.12f) }, enabled = selectionCount > 0) { Text("➕ بزرگ‌تر") }
                        TextButton(onClick = { scaleSelected(1f / 1.12f) }, enabled = selectionCount > 0) { Text("➖ کوچک‌تر") }
                        TextButton(onClick = { flipSelected(true) }, enabled = selectionCount > 0) { Text("⇄ قرینه") }
                        TextButton(onClick = { deleteSelected() }, enabled = selectionCount > 0) {
                            Text("🗑 حذف مورد", color = MaterialTheme.colorScheme.error)
                        }
                        if (selectionCount > 0) {
                            TextButton(onClick = { selectedIds.clear() }) { Text("لغو انتخاب") }
                        }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("سبک خط:", style = MaterialTheme.typography.labelMedium)
                        BoardDash.values().forEach { d ->
                            FilterChip(
                                selected = dash == d,
                                onClick = {
                                    dash = d
                                    if (selectedIds.isNotEmpty()) applyToSelection { it.copy(dash = d) }
                                },
                                label = { Text(d.label) }
                            )
                        }
                        FilterChip(
                            selected = fillShape,
                            onClick = {
                                fillShape = !fillShape
                                val on = fillShape
                                if (selectedIds.isNotEmpty()) applyToSelection { if (it.closedShape) it.copy(fill = on) else it }
                            },
                            label = { Text("▣ توپر ملایم") }
                        )
                        Text(
                            "با انتخاب هر مورد، رنگ و ضخامت و سبک خط فوراً روی همان مورد اعمال می‌شود.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    TextButton(onClick = { page -= 1; selectedIds.clear() }, enabled = page > 0) { Text("قبلی") }
                    TextButton(onClick = { page += 1; selectedIds.clear() }, enabled = page < pageCount - 1) { Text("بعدی") }
                    TextButton(onClick = { pageCount += 1; page = pageCount - 1; selectedIds.clear() }, enabled = pageCount < MAX_PAGES) { Text("+ صفحه") }
                    // V335.18 — کپی همهٔ موارد صفحهٔ فعلی در صفحهٔ تازه
                    TextButton(
                        onClick = {
                            if (pageCount < MAX_PAGES) {
                                snapshot()
                                val copies = pageItems().map { it.copy(id = newId(), page = pageCount) }
                                strokes.addAll(copies); page = pageCount; pageCount += 1
                                selectedIds.clear(); bump()
                            }
                        },
                        enabled = pageCount < MAX_PAGES && pageItems().isNotEmpty()
                    ) { Text("⧉ کپی صفحه") }
                    TextButton(onClick = { savePage = page; pngSaver.launch("whiteboard-p${page + 1}.png") }, enabled = pageItems().isNotEmpty() && boardSize.width > 0) { Text("⬇ PNG") }
                    TextButton(onClick = { confirmClearAll = true }, enabled = pageItems().isNotEmpty()) { Text("پاک‌کردن صفحه") }
                }
                // بوم
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White)
                        .onSizeChanged { boardSize = it }
                        .pointerInput(tool, page, multiPick) {
                            detectTapGestures(
                                onTap = { p ->
                                    when (tool) {
                                        BoardTool.TEXT -> { textAt = p; textInput = ""; labelInput = ""; editId = null }
                                        BoardTool.SELECT -> {
                                            val hit = hitTest(pageItems(), p, density, bitmaps)
                                            if (hit == null) {
                                                if (!multiPick && !shiftDown) selectedIds.clear()
                                            } else if (multiPick || shiftDown) {
                                                if (hit.id in selectedIds) selectedIds.remove(hit.id) else selectedIds.add(hit.id)
                                            } else {
                                                selectOnly(hit.id)
                                            }
                                        }
                                        BoardTool.OBJ_ERASER -> hitTest(pageItems(), p, density, bitmaps)?.let { hit ->
                                            snapshot(); strokes.removeAll { it.id == hit.id }; selectedIds.remove(hit.id); bump()
                                        }
                                        else -> Unit
                                    }
                                },
                                onDoubleTap = { p ->
                                    if (tool == BoardTool.SELECT) {
                                        val hit = hitTest(pageItems(), p, density, bitmaps)
                                        if (hit != null) {
                                            selectOnly(hit.id); editId = hit.id; textInput = hit.text; labelInput = hit.label
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(tool, color, width, page, instrument.kind, dash, fillShape) {
                            if (tool == BoardTool.TEXT) return@pointerInput
                            var mode: String? = null
                            var handle: BoardHandle? = null
                            var startBox: BoardBox? = null
                            var startItems: List<BoardStroke> = emptyList()
                            var dragStart = Offset.Zero
                            var snapshotDone = false
                            // V137.2 — حالت کشیدنِ ابزار هندسی: "move" (بدنه) / "handle" (چرخش/اندازه) / null؛
                            // snapLock: لبه/کمانی که ضربهٔ فعلی به آن چسبیده (تا پایان کشیدن ثابت می‌ماند).
                            var instMode: String? = null
                            var snapLock: SnapTarget? = null
                            fun takeSnapshot() { if (!snapshotDone) { snapshot(); snapshotDone = true } }
                            detectDragGestures(
                                onDragStart = { p ->
                                    dragStart = p; snapshotDone = false; mode = null; handle = null
                                    startBox = null; startItems = emptyList()
                                    val inst = instrument
                                    val instActive = inst.kind != BoardInstrument.NONE && tool != BoardTool.SELECT && tool != BoardTool.OBJ_ERASER
                                    // V137.5 — جابه‌جایی فقط از دستگیرهٔ خاکستریِ مخصوص (grip)؛ روی بدنه می‌شود کشید/نوشت.
                                    instMode = when {
                                        !instActive -> null
                                        (p - inst.handle).getDistance() < 30f * density -> "handle"
                                        (p - instrumentGrip(inst, density)).getDistance() < 30f * density -> "move"
                                        else -> null
                                    }
                                    if (instMode == null) {
                                        when (tool) {
                                            BoardTool.SELECT -> {
                                                val current = strokes.filter { it.id in selectedIds }
                                                val box = unionBoardBox(current, density)
                                                val endpointItem = current.singleOrNull()?.takeIf { it.segmented }
                                                val h = if (box != null) boardHandleAt(box, p, density, endpointItem) else null
                                                if (h != null && box != null) {
                                                    mode = "resize"; handle = h; startBox = box; startItems = current
                                                } else {
                                                    val hit = hitTest(pageItems(), p, density, bitmaps)
                                                    if (hit != null) {
                                                        if (hit.id !in selectedIds) {
                                                            if (multiPick || shiftDown) selectedIds.add(hit.id) else selectOnly(hit.id)
                                                        }
                                                        mode = "move"
                                                        startItems = strokes.filter { it.id in selectedIds }
                                                    } else {
                                                        if (!multiPick && !shiftDown) selectedIds.clear()
                                                        marquee = BoardBox(p.x, p.y, p.x, p.y)
                                                        mode = "marquee"
                                                    }
                                                }
                                            }
                                            BoardTool.OBJ_ERASER -> hitTest(pageItems(), p, density, bitmaps)?.let { hit ->
                                                takeSnapshot(); strokes.removeAll { it.id == hit.id }; selectedIds.remove(hit.id); bump()
                                            }
                                            else -> {
                                                snapLock = if (instActive) snapTargetOf(inst, p, density) else null
                                                val sp = snapLock?.let { snapApply(inst, it, p, density) } ?: p
                                                draft = BoardStroke(
                                                    newId(), tool, listOf(sp, sp), color, width,
                                                    dash = dash, fill = fillShape, page = page
                                                )
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (mode == "marquee") {
                                        val mb = marquee?.normal
                                        if (mb != null && mb.width > 4f && mb.height > 4f) {
                                            pageItems().filter { it.tool != BoardTool.ERASER && boundsOf(it, density).intersects(mb) }.forEach { s ->
                                                if (s.id !in selectedIds) selectedIds.add(s.id)
                                            }
                                        }
                                        marquee = null
                                    }
                                    draft?.let { d ->
                                        // V264 — تشخیص شکل روی ضربهٔ قلم
                                        val recognized = if (autoShape && d.tool == BoardTool.PEN) recognizeStroke(d) else null
                                        commit(recognized ?: d)
                                    }
                                    draft = null; mode = null; handle = null; startBox = null
                                    startItems = emptyList(); instMode = null; snapLock = null; snapshotDone = false
                                },
                                onDragCancel = {
                                    marquee = null; draft = null; mode = null; handle = null; startBox = null
                                    startItems = emptyList(); instMode = null; snapLock = null; snapshotDone = false
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    val acc = change.position - dragStart
                                    when {
                                        instMode == "move" -> instrument = instrument.copy(center = instrument.center + delta)
                                        instMode == "handle" -> {
                                            val v = change.position - instrument.center
                                            instrument = instrument.copy(angle = atan2(v.y, v.x), size = v.getDistance().coerceIn(40f * density, 4000f))
                                        }
                                        mode == "move" -> { takeSnapshot(); startItems.forEach { replaceOne(it.moved(acc)) } }
                                        mode == "resize" -> {
                                            val sb = startBox
                                            val hh = handle
                                            if (sb != null && hh != null) {
                                                takeSnapshot()
                                                if (hh.isEndpoint) {
                                                    val src = startItems.firstOrNull()
                                                    if (src != null && src.points.size >= 2) {
                                                        val moved = if (hh == BoardHandle.P1) {
                                                            listOf(src.points.first() + acc) + src.points.drop(1)
                                                        } else {
                                                            src.points.dropLast(1) + listOf(src.points.last() + acc)
                                                        }
                                                        replaceOne(src.copy(points = moved))
                                                    }
                                                } else {
                                                    val nb = resizedBoardBox(sb, hh, acc.x, acc.y, shiftDown, 16f * density)
                                                    startItems.forEach { replaceOne(scaleBoardStroke(it, sb, nb)) }
                                                }
                                            }
                                        }
                                        mode == "marquee" -> marquee = BoardBox(dragStart.x, dragStart.y, change.position.x, change.position.y)
                                        tool == BoardTool.OBJ_ERASER -> hitTest(pageItems(), change.position, density, bitmaps)?.let { hit ->
                                            takeSnapshot(); strokes.removeAll { it.id == hit.id }; selectedIds.remove(hit.id); bump()
                                        }
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
                        renderBoard(
                            c, size.width, size.height, background, pageItems() + listOfNotNull(draft), density,
                            bitmaps, selectedIds.toSet(), marquee
                        )
                        // V137.2 — ابزار هندسی فقط روی صفحه (در خروجی نیست).
                        if (instrument.kind != BoardInstrument.NONE) drawInstrument(c, instrument, density)
                    }
                    if (pageItems().isEmpty() && draft == null) {
                        Text(
                            when (tool) {
                                BoardTool.TEXT -> "برای نوشتن متن، روی تخته بزنید"
                                BoardTool.SELECT -> "روی هر مورد بزنید تا انتخاب شود؛ بکشید تا جابه‌جا یا با دستگیره‌ها تغییر اندازه دهید"
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
    // افزودن/ویرایش متن و برچسب وسط شکل
    val editTarget = editId?.let { id -> strokes.firstOrNull { it.id == id } }
    val textDialogOpen = textAt != null || editTarget != null
    if (textDialogOpen) {
        AlertDialog(
            onDismissRequest = { textAt = null; editId = null },
            title = { Text(if (editTarget != null) "ویرایش مورد درج‌شده" else "متن روی تخته") },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it.take(300) },
                        label = { Text(if (editTarget != null && editTarget.tool != BoardTool.TEXT) "متن (اختیاری)" else "متن (فارسی/انگلیسی)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it.take(80) },
                        label = { Text("برچسب داخل شکل (اختیاری)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = when {
                        editTarget == null -> textInput.isNotBlank() || labelInput.isNotBlank()
                        editTarget.tool == BoardTool.TEXT -> textInput.isNotBlank()
                        else -> true // شکل: می‌توان برچسب را کامل پاک کرد
                    },
                    onClick = {
                        val editing = editTarget
                        if (editing != null) {
                            snapshot()
                            replaceOne(editing.copy(text = textInput.trim(), label = labelInput.trim()))
                        } else textAt?.let { at ->
                            val item = BoardStroke(
                                newId(), BoardTool.TEXT, listOf(at), color, width,
                                text = textInput.trim(), textSizeSp = 14f + width * 1.2f, page = page
                            )
                            commit(item); selectOnly(item.id); tool = BoardTool.SELECT
                        }
                        textAt = null; editId = null
                    }
                ) { Text(if (editTarget != null) "ذخیره" else "افزودن") }
            },
            dismissButton = { TextButton(onClick = { textAt = null; editId = null }) { Text("انصراف") } }
        )
    }
    // فرمول (همان ویرایشگر فرمول سازنده)
    if (formulaOpen) {
        val editing = selectedSingle?.takeIf { it.tool == BoardTool.IMAGE && it.ref.startsWith("tex:") }
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
                        scope.launch { ensureBitmap(ref); replaceOne(editing.copy(ref = ref)) }
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
            text = { Text("همهٔ عناصر صفحهٔ ${page + 1} حذف می‌شود (با «برگشت» قابل بازگردانی است).") },
            confirmButton = {
                Button(onClick = {
                    snapshot(); strokes.removeAll { it.page == page }; selectedIds.clear(); bump(); confirmClearAll = false
                }) { Text("پاک کن") }
            },
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
                    HelpRow(Icons.Outlined.AutoFixHigh, "تشخیص شکل", "وقتی روشن است، خط، دایره، مثلث یا مستطیلی که با قلم می‌کشید پس از رها کردن انگشت به شکل منظم تبدیل می‌شود.")
                    HelpRow(BoardTool.HIGHLIGHT.icon, "هایلایتر", "خط پهنِ نیمه‌شفاف برای برجسته‌کردن.")
                    HelpRow(BoardTool.ERASER.icon, "پاک‌کن", "هر جا بکشید پاک می‌شود.")
                    HelpRow(BoardTool.OBJ_ERASER.icon, "پاک‌کن شیء", "روی هر خط/شکل/متن بزنید تا کامل حذف شود.")
                    HelpRow(BoardTool.SELECT.icon, "انتخاب/جابه‌جایی", "روی شیء بزنید تا انتخاب شود؛ بکشید تا جابه‌جا شود. رنگ/ضخامت روی شیء انتخاب‌شده اعمال می‌شود.")
                    HelpRow(BoardTool.LINE.icon, "خط", "خط راست بین نقطهٔ شروع و پایان.")
                    HelpRow(BoardTool.ARROW.icon, "پیکان", "خط راست با نوک پیکان.")
                    HelpRow(BoardTool.ARROW2.icon, "پیکان دوسر", "خط راست با پیکان در هر دو سر (برای بردار و محور).")
                    HelpRow(BoardTool.RECT.icon, "مستطیل", "کشیدن از یک گوشه تا گوشهٔ مقابل.")
                    HelpRow(BoardTool.CIRCLE.icon, "دایره/بیضی", "کشیدن داخل کادر.")
                    HelpRow(BoardTool.TRIANGLE.icon, "مثلث", "کشیدن داخل کادر.")
                    HelpRow(BoardTool.RTRIANGLE.icon, "مثلث قائم", "مثلث با زاویهٔ قائمه در گوشهٔ پایین-چپ.")
                    HelpRow(BoardTool.DIAMOND.icon, "لوزی", "چهارضلعی متساوی‌الاضلاع داخل کادر.")
                    HelpRow(BoardTool.AXES.icon, "محور سریع", "دو محور عمود بر هم با پیکان، داخل کادرِ کشیده‌شده.")
                    HelpRow(BoardTool.TEXT.icon, "متن", "روی تخته بزنید و متن را بنویسید؛ با انتخاب می‌توان ویرایش/بزرگ/کوچک کرد.")
                    HelpRow(Icons.Outlined.AddBox, "درج…", "فرمول، نمودار، محور، شکل هندسی، جدول، فیزیک/شیمی، جدول تناوبی، تصویر سؤال یا تصویر از گالری دستگاه.")
                    Text("انتخاب و ویرایش موارد درج‌شده", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    HelpRow(Icons.Outlined.OpenWith, "انتخاب", "با ابزار انتخاب روی هر مورد بزنید؛ با «انتخاب چندگانه» یا کلید Shift چند مورد را هم‌زمان انتخاب کنید و با کشیدن روی فضای خالی، کمند انتخاب بکشید.")
                    HelpRow(Icons.Outlined.CropSquare, "تغییر اندازه", "هشت دستگیرهٔ گوشه/ضلع کادر انتخاب؛ نگه‌داشتن Shift نسبت ابعاد را حفظ می‌کند. برای خط و پیکان دو دستگیرهٔ گرد سرِ خط هم هست.")
                    HelpRow(Icons.Outlined.Edit, "ویرایش", "متن یا برچسب وسط شکل را تغییر می‌دهد (دوبار کلیک روی مورد = همان ویرایش).")
                    HelpRow(Icons.Outlined.ContentCopy, "تکثیر مورد", "یک نسخهٔ جابه‌جاشده از موارد انتخاب‌شده می‌سازد (Ctrl+D).")
                    HelpRow(Icons.Outlined.OpenWith, "جابه‌جایی دقیق", "با کلیدهای جهت ۲dp و با Shift+جهت ۱۰dp جابه‌جا می‌شود.")
                    Text("پایین نوار: رنگ‌ها و ضخامت، سبک خط (ممتد/خط‌چین/نقطه‌چین)، توپرِ ملایم، زمینه، صفحه‌های قبلی/بعدی، + صفحه، کپی صفحه، ذخیرهٔ PNG، پاک‌کردن صفحه.", style = MaterialTheme.typography.bodySmall)
                    Text("ابزارهای هندسی", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    HelpRow(BoardInstrument.NONE.icon, "بدون ابزار", "ابزار هندسی برداشته می‌شود.")
                    HelpRow(BoardInstrument.RULER.icon, "خط‌کش", "قلم/خط نزدیک لبه به لبه می‌چسبد.")
                    HelpRow(BoardInstrument.SETSQUARE.icon, "گونیا", "سه لبهٔ شماره‌دار؛ قلم به لبه می‌چسبد.")
                    HelpRow(BoardInstrument.PROTRACTOR.icon, "نقاله", "قلم نزدیک کمان روی کمان و نزدیک خط پایه روی خط می‌رود.")
                    HelpRow(BoardInstrument.COMPASS.icon, "پرگار", "هر کشیدن یک کمان به مرکز و شعاع پرگار است.")
                    Text("دستگیره‌های ابزار هندسی: دایرهٔ خاکستری (چهارپیکان) = جابه‌جایی ابزار؛ دایرهٔ آبی = چرخش و تغییر اندازه.", style = MaterialTheme.typography.bodySmall)
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
                        (0 until pageCount).filter { p -> strokes.any { it.page == p } }.mapNotNull { p ->
                            val uri = exportBoard(context.filesDir, questionId, p, boardSize, strokes.filter { it.page == p }, background, density, bitmaps)
                            uri.ifBlank { null }
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
/** V264 — تبدیل ضربهٔ قلم به شیء هندسی (ShapeRecognizer؛ خالص و تست‌شده). */
internal fun recognizeStroke(d: BoardStroke): BoardStroke? {
    val pts = d.points.map { ir.exam.app.core.geometry.ShapeRecognizer.Pt(it.x, it.y) }
    val shape = ir.exam.app.core.geometry.ShapeRecognizer.recognize(pts) ?: return null
    val tool = when (shape.kind) {
        ir.exam.app.core.geometry.ShapeRecognizer.Kind.LINE -> BoardTool.LINE
        ir.exam.app.core.geometry.ShapeRecognizer.Kind.CIRCLE -> BoardTool.CIRCLE
        ir.exam.app.core.geometry.ShapeRecognizer.Kind.TRIANGLE -> BoardTool.TRIANGLE
        ir.exam.app.core.geometry.ShapeRecognizer.Kind.RECT -> BoardTool.RECT
    }
    return d.copy(tool = tool, points = listOf(Offset(shape.a.x, shape.a.y), Offset(shape.b.x, shape.b.y)))
}

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
    bitmaps: Map<String, Bitmap>, selectedIds: Set<Long>, marquee: BoardBox? = null
) {
    drawBackground(canvas, w, h, bg, density)
    // لایهٔ شفاف: پاک‌کن با CLEAR فقط خطوط را پاک می‌کند و زمینه سالم می‌ماند.
    val layer = canvas.saveLayer(0f, 0f, w, h, null)
    items.forEach { drawItem(canvas, it, density, bitmaps) }
    canvas.restoreToCount(layer)
    // V335.18 — نمای انتخاب: کمند، کادر هر مورد، کادر گروهی و دستگیره‌های تغییر اندازه.
    if (marquee != null) {
        val m = marquee.normal
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0x332563EB }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.2f * density; color = 0xFF2563EB.toInt()
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(7f * density, 5f * density), 0f)
        }
        canvas.drawRect(RectF(m.left, m.top, m.right, m.bottom), fill)
        canvas.drawRect(RectF(m.left, m.top, m.right, m.bottom), line)
        return
    }
    if (selectedIds.isEmpty()) return
    val chosen = items.filter { it.id in selectedIds }
    if (chosen.isEmpty()) return
    val itemOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.2f * density; color = 0x992563EB.toInt()
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f * density, 5f * density), 0f)
    }
    chosen.forEach { s ->
        val b = boundsOf(s, density)
        canvas.drawRoundRect(RectF(b.left, b.top, b.right, b.bottom), 4f * density, 4f * density, itemOutline)
    }
    val box = unionBoardBox(chosen, density)?.normal ?: return
    val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.6f * density; color = 0xFF2563EB.toInt()
    }
    canvas.drawRoundRect(RectF(box.left, box.top, box.right, box.bottom), 6f * density, 6f * density, frame)
    // ۸ دستگیرهٔ تغییر اندازه
    val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFFFFFFFF.toInt() }
    val handleRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density; color = 0xFF2563EB.toInt() }
    BOARD_RESIZE_HANDLES.forEach { h ->
        val p = boardHandlePoint(box, h)
        canvas.drawCircle(p.x, p.y, 7f * density, handleFill)
        canvas.drawCircle(p.x, p.y, 7f * density, handleRing)
    }
    // ۲ سرِ خط برای خط/پیکان تنها
    val single = chosen.singleOrNull()?.takeIf { it.segmented }
    if (single != null && single.points.size >= 2) {
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFF2563EB.toInt() }
        listOf(single.points.first(), single.points.last()).forEach { p ->
            canvas.drawCircle(p.x, p.y, 8f * density, handleFill)
            canvas.drawCircle(p.x, p.y, 8f * density, handleRing)
            canvas.drawCircle(p.x, p.y, 3f * density, dot)
        }
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

/** V335.18 — افکت خط‌چین/نقطه‌چین هر مورد. */
private fun dashEffectOf(s: BoardStroke, density: Float): android.graphics.PathEffect? = when (s.dash) {
    BoardDash.SOLID -> null
    BoardDash.DASHED -> android.graphics.DashPathEffect(floatArrayOf(12f * density, 8f * density), 0f)
    BoardDash.DOTTED -> android.graphics.DashPathEffect(floatArrayOf(2f * density, 7f * density), 0f)
}

/** مسیر شکل‌های بسته (برای پرکردن ملایم و خط‌کشی یکسان). */
private fun shapePathOf(tool: BoardTool, r: RectF): Path? = when (tool) {
    BoardTool.RECT -> Path().apply { addRect(r, Path.Direction.CW) }
    BoardTool.CIRCLE -> Path().apply { addOval(r, Path.Direction.CW) }
    BoardTool.TRIANGLE -> Path().apply { moveTo(r.centerX(), r.top); lineTo(r.right, r.bottom); lineTo(r.left, r.bottom); close() }
    BoardTool.RTRIANGLE -> Path().apply { moveTo(r.left, r.top); lineTo(r.left, r.bottom); lineTo(r.right, r.bottom); close() }
    BoardTool.DIAMOND -> Path().apply {
        moveTo(r.centerX(), r.top); lineTo(r.right, r.centerY()); lineTo(r.centerX(), r.bottom); lineTo(r.left, r.centerY()); close()
    }
    else -> null
}

/** V335.18 — برچسب وسط شکل با هالهٔ سفید تا روی خطوط زمینه/شکل خوانا بماند. */
private fun drawShapeLabel(canvas: Canvas, s: BoardStroke, box: BoardBox, density: Float) {
    if (s.label.isBlank()) return
    val b = box.normal
    val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = s.color.toArgb(); textSize = s.textSizeSp * density; textAlign = Paint.Align.CENTER
    }
    val halo = Paint(txt).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f * density; strokeJoin = Paint.Join.ROUND }
    val lines = s.label.split('\n')
    val lineHeight = txt.textSize * 1.25f
    val first = b.centerY - lineHeight * (lines.size - 1) / 2f + txt.textSize * 0.35f
    lines.forEachIndexed { i, line ->
        val y = first + i * lineHeight
        canvas.drawText(line, b.centerX, y, halo)
        canvas.drawText(line, b.centerX, y, txt)
    }
}

/** V335.18 — محور سریع موضعی داخل کادر کشیده‌شده (پیکان‌دار، با نشانهٔ مبدأ). */
private fun drawQuickAxes(canvas: Canvas, r: RectF, s: BoardStroke, density: Float) {
    val box = BoardBox(r.left, r.top, r.right, r.bottom).normal
    if (box.width < 8f * density || box.height < 8f * density) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        color = s.color.toArgb(); strokeWidth = max(1.2f, s.width * 0.7f) * density; pathEffect = dashEffectOf(s, density)
    }
    val cy = box.centerY; val cx = box.centerX
    canvas.drawLine(box.left, cy, box.right, cy, paint)
    canvas.drawLine(cx, box.bottom, cx, box.top, paint)
    arrowHead(Offset(box.left, cy), Offset(box.right, cy), paint.strokeWidth).forEach { (p, q) -> canvas.drawLine(p.x, p.y, q.x, q.y, paint) }
    arrowHead(Offset(cx, box.bottom), Offset(cx, box.top), paint.strokeWidth).forEach { (p, q) -> canvas.drawLine(p.x, p.y, q.x, q.y, paint) }
    val origin = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = s.color.toArgb() }
    canvas.drawCircle(cx, cy, 3f * density, origin)
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
    if (s.tool == BoardTool.AXES) {
        drawQuickAxes(canvas, rectOf(a, b), s, density)
        return
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        color = s.color.toArgb()
        strokeWidth = s.width * density
        when (s.tool) {
            BoardTool.ERASER -> { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); strokeWidth = s.width * 3f * density }
            BoardTool.HIGHLIGHT -> { alpha = 90; strokeWidth = s.width * 3f * density; strokeCap = Paint.Cap.SQUARE }
            else -> pathEffect = dashEffectOf(s, density)
        }
    }
    val shape = shapePathOf(s.tool, rectOf(a, b))
    if (shape != null) {
        if (s.fill) {
            // V335.18 — پرکردن ملایم (شفافیت ~۲۰٪) تا متن و برچسب روی آن خوانا بماند.
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = s.color.toArgb(); alpha = 50 }
            canvas.drawPath(shape, fillPaint)
        }
        canvas.drawPath(shape, paint)
        drawShapeLabel(canvas, s, boundsOf(s, density), density)
        return
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
        BoardTool.ARROW2 -> {
            canvas.drawLine(a.x, a.y, b.x, b.y, paint)
            arrowHead(a, b, paint.strokeWidth).forEach { (p, q) -> canvas.drawLine(p.x, p.y, q.x, q.y, paint) }
            arrowHead(b, a, paint.strokeWidth).forEach { (p, q) -> canvas.drawLine(p.x, p.y, q.x, q.y, paint) }
        }
        else -> Unit
    }
}

/** جعبهٔ محدودکنندهٔ هر مورد (برچسب شکل و متن هم لحاظ می‌شود). */
private fun boundsOf(s: BoardStroke, density: Float): BoardBox {
    val a = s.points.first()
    return when (s.tool) {
        BoardTool.TEXT -> {
            val tp = textPaint(s, density)
            val lines = s.text.split('\n')
            val w = lines.maxOf { tp.measureText(it) }
            BoardBox(a.x - w, a.y - tp.textSize, a.x, a.y + tp.textSize * 1.3f * (lines.size - 1) + tp.textSize * 0.3f)
        }
        BoardTool.IMAGE -> rectOf(a, s.points.last()).let { BoardBox(it.left, it.top, it.right, it.bottom) }
        else -> {
            var l = a.x; var t = a.y; var r = a.x; var bt = a.y
            s.points.forEach { l = min(l, it.x); t = min(t, it.y); r = max(r, it.x); bt = max(bt, it.y) }
            val pad = max(s.width * density / 2f, 2f * density)
            BoardBox(l - pad, t - pad, r + pad, bt + pad)
        }
    }
}

/** بالاترین شیءِ زیر انگشت (آخرین کشیده‌شده اولویت دارد). */
private fun hitTest(items: List<BoardStroke>, p: Offset, density: Float, bitmaps: Map<String, Bitmap>): BoardStroke? {
    val slop = 14f * density
    return items.asReversed().firstOrNull { s ->
        if (s.tool == BoardTool.ERASER) return@firstOrNull false
        val r = boundsOf(s, density)
        if (!r.contains(p.x, p.y, slop)) return@firstOrNull false
        when (s.tool) {
            BoardTool.PEN, BoardTool.HIGHLIGHT, BoardTool.LINE, BoardTool.ARROW, BoardTool.ARROW2 -> {
                val tol = slop + s.width * density
                if (s.points.size == 1) return@firstOrNull true
                (1 until s.points.size).any { i -> distToSegment(p, s.points[i - 1], s.points[i]) <= tol } ||
                    (s.segmented && distToSegment(p, s.points.first(), s.points.last()) <= tol)
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
                // V335.18 — برچسب شکل، سبک خط و توپرِ ملایم
                "lbl" to JsonPrimitive(s.label), "dash" to JsonPrimitive(s.dash.name), "fill" to JsonPrimitive(s.fill),
                "pts" to JsonArray(s.points.flatMap { listOf(JsonPrimitive(it.x), JsonPrimitive(it.y)) })
            )
        )
    })
    val root = JsonObject(mapOf("v" to JsonPrimitive(2), "bg" to JsonPrimitive(bg.name), "pages" to JsonPrimitive(pageCount), "items" to arr))
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
            page = o["page"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
            label = o["lbl"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            dash = runCatching { BoardDash.valueOf(o["dash"]?.jsonPrimitive?.contentOrNull ?: "SOLID") }.getOrDefault(BoardDash.SOLID),
            fill = o["fill"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        )
    }
    return BoardDraft(items, bg, pages)
}

/**
 * V335.18 — رندر یک صفحه روی Bitmap کدرِ سفید و برگرداندن بایت‌های PNG (بدون کادر انتخاب).
 * V137.1 — کدر (setHasAlpha=false) تا در هیچ مسیر فشرده‌سازی/نمایشی سیاه نشود.
 */
private fun exportBoardPng(
    size: IntSize, strokes: List<BoardStroke>, background: BoardBackground, density: Float, bitmaps: Map<String, Bitmap>
): ByteArray? = runCatching {
    val w = size.width.coerceAtLeast(1); val h = size.height.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    renderBoard(canvas, w.toFloat(), h.toFloat(), background, strokes, density, bitmaps, emptySet())
    // هر پیکسل شفافِ باقی‌مانده (پس از پاک‌کن) روی سفید می‌نشیند.
    val flat = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(flat).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(bmp, 0f, 0f, null) }
    bmp.recycle()
    flat.setHasAlpha(false)
    val out = java.io.ByteArrayOutputStream()
    flat.compress(Bitmap.CompressFormat.PNG, 100, out)
    flat.recycle()
    out.toByteArray()
}.getOrNull()

/**
 * رندر یک صفحه روی Bitmap کدرِ سفید و ذخیره به PNG در filesDir/whiteboard؛ خروجی `file:`.
 * V137.1 — filesDir به‌جای cache تا تا زمان ارسال (که ممکن است ساعت‌ها بعد باشد) پاک نشود.
 */
private fun exportBoard(
    filesDir: File, questionId: String, page: Int, size: IntSize, strokes: List<BoardStroke>, background: BoardBackground,
    density: Float, bitmaps: Map<String, Bitmap>
): String {
    val bytes = exportBoardPng(size, strokes, background, density, bitmaps) ?: return ""
    val dir = File(filesDir, "whiteboard").apply { mkdirs() }
    val out = File(dir, "wb-${questionId.take(8)}-p$page-${System.currentTimeMillis()}.png")
    FileOutputStream(out).use { it.write(bytes) }
    return out.toURI().toString()
}
