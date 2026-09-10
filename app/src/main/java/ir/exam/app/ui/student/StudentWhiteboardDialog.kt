package ir.exam.app.ui.student

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** ابزارهای تخته (V137). */
internal enum class BoardTool(val label: String) {
    PEN("قلم"), ERASER("پاک‌کن"), LINE("خط"), ARROW("پیکان"), RECT("مستطیل"), CIRCLE("دایره"), TEXT("متن")
}

/** پس‌زمینهٔ تخته (V137). */
internal enum class BoardBackground(val label: String) { PLAIN("ساده"), GRID("شطرنجی"), LINED("خط‌دار") }

/** یک عنصر کشیده‌شده روی تخته. */
internal data class BoardStroke(
    val tool: BoardTool,
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val text: String = "",
    val textSizeSp: Float = 18f
) {
    val eraser: Boolean get() = tool == BoardTool.ERASER
}

/**
 * V136 — «تخته وایت‌برد» پاسخ دانش‌آموز (جایگزین «نمودار پاسخ دانش‌آموز» V58).
 * V137 — ابزارهای بیشتر: قلم/پاک‌کن/خط/پیکان/مستطیل/دایره/متن، ۸ رنگ، ۴ ضخامت،
 * پس‌زمینهٔ ساده/شطرنجی/خط‌دار، برگشت/جلو، پاک‌کردن همه؛ خروجی همان تصویر پاسخ
 * (PNG در cache → مسیر تصاویر پاسخ `answers/<student>/<exam>/<question>`).
 */
@Composable
fun StudentWhiteboardDialog(
    questionId: String,
    onDismiss: () -> Unit,
    onDone: (fileUri: String) -> Unit
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val strokes = remember(questionId) { mutableStateListOf<BoardStroke>() }
    val redo = remember(questionId) { mutableStateListOf<BoardStroke>() }
    var draft by remember { mutableStateOf<BoardStroke?>(null) }
    var tool by remember { mutableStateOf(BoardTool.PEN) }
    var color by remember { mutableStateOf(Color(0xFF1F2937)) }
    var width by remember { mutableStateOf(5f) }
    var background by remember { mutableStateOf(BoardBackground.PLAIN) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var textAt by remember { mutableStateOf<Offset?>(null) }
    var textInput by remember { mutableStateOf("") }
    val palette = listOf(
        Color(0xFF1F2937), Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFF2E7D32),
        Color(0xFFF57C00), Color(0xFF7B1FA2), Color(0xFF00838F), Color(0xFF6D4C41)
    )

    fun commit(s: BoardStroke) { strokes.add(s); redo.clear() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("تخته وایت‌برد پاسخ", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (strokes.isNotEmpty()) redo.add(strokes.removeAt(strokes.lastIndex)) }, enabled = strokes.isNotEmpty()) { Text("برگشت") }
                    TextButton(onClick = { if (redo.isNotEmpty()) strokes.add(redo.removeAt(redo.lastIndex)) }, enabled = redo.isNotEmpty()) { Text("جلو") }
                    TextButton(onClick = onDismiss) { Text("انصراف", color = MaterialTheme.colorScheme.error) }
                }
                // ابزارها
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BoardTool.values().forEach { t ->
                        FilterChip(selected = tool == t, onClick = { tool = t }, label = { Text(t.label) })
                    }
                }
                // رنگ‌ها + ضخامت
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    palette.forEach { c ->
                        Box(
                            Modifier
                                .size(if (c == color && tool != BoardTool.ERASER) 30.dp else 24.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(if (c == color) Modifier.border(2.dp, Color(0xFF9CA3AF), CircleShape) else Modifier)
                                .clickable { color = c; if (tool == BoardTool.ERASER) tool = BoardTool.PEN }
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    listOf(2f to "خیلی‌نازک", 4f to "نازک", 7f to "متوسط", 12f to "ضخیم").forEach { (w, label) ->
                        FilterChip(selected = width == w, onClick = { width = w }, label = { Text(label) })
                    }
                }
                // پس‌زمینه + پاک‌کردن همه
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("زمینه:", style = MaterialTheme.typography.labelMedium)
                    BoardBackground.values().forEach { b ->
                        FilterChip(selected = background == b, onClick = { background = b }, label = { Text(b.label) })
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { strokes.clear(); redo.clear() }, enabled = strokes.isNotEmpty()) { Text("پاک‌کردن همه") }
                }
                // بوم
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White)
                        .onSizeChanged { boardSize = it }
                        .pointerInput(tool) {
                            if (tool == BoardTool.TEXT) {
                                detectTapGestures { p -> textAt = p; textInput = "" }
                            }
                        }
                        .pointerInput(tool, color, width) {
                            if (tool == BoardTool.TEXT) return@pointerInput
                            detectDragGestures(
                                onDragStart = { p ->
                                    draft = BoardStroke(tool, listOf(p, p), color, width)
                                },
                                onDragEnd = { draft?.let { commit(it) }; draft = null },
                                onDragCancel = { draft = null },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val d = draft ?: return@detectDragGestures
                                    draft = if (d.tool == BoardTool.PEN || d.tool == BoardTool.ERASER) {
                                        d.copy(points = d.points + change.position)
                                    } else {
                                        // شکل‌ها: نقطهٔ شروع ثابت، نقطهٔ پایان دنبال انگشت
                                        d.copy(points = listOf(d.points.first(), change.position))
                                    }
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawBoardBackground(background, density)
                        strokes.forEach { drawStroke(it, density) }
                        draft?.let { drawStroke(it, density) }
                    }
                    if (strokes.isEmpty() && draft == null) {
                        Text(
                            if (tool == BoardTool.TEXT) "برای نوشتن متن، روی تخته بزنید" else "اینجا بنویسید یا بکشید…",
                            color = Color(0xFF9CA3AF), modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Button(
                    onClick = {
                        val uri = runCatching { exportBoard(context.cacheDir, questionId, boardSize, strokes, background, density) }.getOrNull()
                        if (uri != null) onDone(uri)
                    },
                    enabled = strokes.isNotEmpty() && boardSize.width > 0,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("ثبت به‌عنوان تصویر پاسخ") }
            }
        }
    }
    textAt?.let { at ->
        AlertDialog(
            onDismissRequest = { textAt = null },
            title = { Text("متن روی تخته") },
            text = {
                OutlinedTextField(value = textInput, onValueChange = { textInput = it.take(200) }, label = { Text("متن") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(enabled = textInput.isNotBlank(), onClick = {
                    commit(BoardStroke(BoardTool.TEXT, listOf(at), color, width, text = textInput.trim(), textSizeSp = 14f + width * 1.5f))
                    textAt = null
                }) { Text("افزودن") }
            },
            dismissButton = { TextButton(onClick = { textAt = null }) { Text("انصراف") } }
        )
    }
}

private fun DrawScope.drawBoardBackground(bg: BoardBackground, density: Float) {
    if (bg == BoardBackground.PLAIN) return
    val step = 24f * density
    val line = Color(0xFFE5E7EB)
    var y = step
    while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y), 1f); y += step }
    if (bg == BoardBackground.GRID) {
        var x = step
        while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
    }
}

private fun DrawScope.drawStroke(s: BoardStroke, density: Float) {
    val c = if (s.eraser) Color.White else s.color
    val w = (if (s.eraser) s.width * 3f else s.width) * density
    when (s.tool) {
        BoardTool.PEN, BoardTool.ERASER -> {
            if (s.points.size < 2 || s.points.all { it == s.points.first() }) {
                drawCircle(c, radius = w / 2f, center = s.points.first())
            } else {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(s.points[0].x, s.points[0].y)
                    for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                }
                drawPath(path, c, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        BoardTool.LINE -> drawLine(c, s.points.first(), s.points.last(), w, StrokeCap.Round)
        BoardTool.ARROW -> {
            val a = s.points.first(); val b = s.points.last()
            drawLine(c, a, b, w, StrokeCap.Round)
            arrowHead(a, b, w).forEach { (p, q) -> drawLine(c, p, q, w, StrokeCap.Round) }
        }
        BoardTool.RECT -> {
            val r = rectOf(s.points.first(), s.points.last())
            drawRect(c, topLeft = Offset(r.left, r.top), size = androidx.compose.ui.geometry.Size(r.width, r.height), style = Stroke(width = w, join = StrokeJoin.Round))
        }
        BoardTool.CIRCLE -> {
            val r = rectOf(s.points.first(), s.points.last())
            drawOval(c, topLeft = Offset(r.left, r.top), size = androidx.compose.ui.geometry.Size(r.width, r.height), style = Stroke(width = w))
        }
        BoardTool.TEXT -> {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = c.toArgbInt(); textSize = s.textSizeSp * density; textAlign = android.graphics.Paint.Align.RIGHT
            }
            drawContext.canvas.nativeCanvas.drawText(s.text, s.points.first().x, s.points.first().y, paint)
        }
    }
}

private fun rectOf(a: Offset, b: Offset) = Rect(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))

private fun arrowHead(a: Offset, b: Offset, w: Float): List<Pair<Offset, Offset>> {
    val len = max(14f, w * 4f)
    if (hypot(b.x - a.x, b.y - a.y) < 1f) return emptyList()
    val ang = atan2(b.y - a.y, b.x - a.x)
    val l = Offset(b.x - len * cos(ang - 0.5f), b.y - len * sin(ang - 0.5f))
    val r = Offset(b.x - len * cos(ang + 0.5f), b.y - len * sin(ang + 0.5f))
    return listOf(b to l, b to r)
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)

/** رندر عناصر روی Bitmap سفید و ذخیره به PNG در cache؛ خروجی `file://`. */
private fun exportBoard(
    cacheDir: File, questionId: String, size: IntSize, strokes: List<BoardStroke>, background: BoardBackground, density: Float
): String {
    val w = size.width.coerceAtLeast(1); val h = size.height.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    if (background != BoardBackground.PLAIN) {
        val grid = Paint().apply { color = 0xFFE5E7EB.toInt(); strokeWidth = 1f }
        val step = 24f * density
        var y = step
        while (y < h) { canvas.drawLine(0f, y, w.toFloat(), y, grid); y += step }
        if (background == BoardBackground.GRID) {
            var x = step
            while (x < w) { canvas.drawLine(x, 0f, x, h.toFloat(), grid); x += step }
        }
    }
    strokes.forEach { s ->
        paint.color = if (s.eraser) android.graphics.Color.WHITE else s.color.toArgbInt()
        paint.strokeWidth = (if (s.eraser) s.width * 3f else s.width) * density
        paint.style = Paint.Style.STROKE
        val a = s.points.first(); val b = s.points.last()
        when (s.tool) {
            BoardTool.PEN, BoardTool.ERASER -> {
                if (s.points.size < 2 || s.points.all { it == a }) {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(a.x, a.y, paint.strokeWidth / 2f, paint)
                } else {
                    val p = Path().apply {
                        moveTo(a.x, a.y)
                        for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                    }
                    canvas.drawPath(p, paint)
                }
            }
            BoardTool.LINE -> canvas.drawLine(a.x, a.y, b.x, b.y, paint)
            BoardTool.ARROW -> {
                canvas.drawLine(a.x, a.y, b.x, b.y, paint)
                arrowHead(a, b, paint.strokeWidth).forEach { (p, q) -> canvas.drawLine(p.x, p.y, q.x, q.y, paint) }
            }
            BoardTool.RECT -> { val r = rectOf(a, b); canvas.drawRect(r.left, r.top, r.right, r.bottom, paint) }
            BoardTool.CIRCLE -> { val r = rectOf(a, b); canvas.drawOval(android.graphics.RectF(r.left, r.top, r.right, r.bottom), paint) }
            BoardTool.TEXT -> {
                val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = s.color.toArgbInt(); textSize = s.textSizeSp * density; textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(s.text, a.x, a.y, tp)
            }
        }
    }
    val dir = File(cacheDir, "whiteboard").apply { mkdirs() }
    val out = File(dir, "wb-${questionId.take(8)}-${System.currentTimeMillis()}.png")
    FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bmp.recycle()
    return out.toURI().toString()
}
