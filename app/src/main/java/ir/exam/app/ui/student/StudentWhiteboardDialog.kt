package ir.exam.app.ui.student

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

/** یک خطِ کشیده‌شده روی تخته. */
private data class BoardStroke(val points: MutableList<Offset>, val color: Color, val width: Float, val eraser: Boolean)

/**
 * V136 — «تخته وایت‌برد» پاسخ دانش‌آموز (جایگزین «نمودار پاسخ دانش‌آموز» V58).
 * معلم در کارت سؤال آن را فعال می‌کند؛ دانش‌آموز رسم آزاد می‌کند و نتیجه به‌عنوان
 * یک **تصویر پاسخ** (PNG در cache) به همان جریان تصاویر پاسخ می‌رود، پس در تصحیح و
 * ارسال به سرور همان مسیر تصاویر (`answers/<student>/<exam>/<question>`) استفاده می‌شود.
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
    var current by remember { mutableStateOf<BoardStroke?>(null) }
    var color by remember { mutableStateOf(Color(0xFF1F2937)) }
    var width by remember { mutableStateOf(5f) }
    var eraser by remember { mutableStateOf(false) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val palette = listOf(Color(0xFF1F2937), Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFF2E7D32), Color(0xFFF57C00), Color(0xFF7B1FA2))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("تخته وایت‌برد پاسخ", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("انصراف", color = MaterialTheme.colorScheme.error) }
                }
                // ابزارها: رنگ‌ها، پاک‌کن، ضخامت، برگشت، پاک‌کردن همه
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    palette.forEach { c ->
                        Box(
                            Modifier
                                .size(if (c == color && !eraser) 30.dp else 24.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { color = c; eraser = false }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    FilterChip(selected = eraser, onClick = { eraser = !eraser }, label = { Text("پاک‌کن") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(3f to "نازک", 6f to "متوسط", 12f to "ضخیم").forEach { (w, label) ->
                        FilterChip(selected = width == w, onClick = { width = w }, label = { Text(label) })
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }, enabled = strokes.isNotEmpty()) { Text("برگشت") }
                    TextButton(onClick = { strokes.clear() }, enabled = strokes.isNotEmpty()) { Text("پاک‌کردن همه") }
                }
                // بوم سفید
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White)
                        .onSizeChanged { boardSize = it }
                        .pointerInput(color, width, eraser) {
                            detectDragGestures(
                                onDragStart = { p ->
                                    current = BoardStroke(mutableListOf(p), color, width, eraser).also { strokes.add(it) }
                                },
                                onDragEnd = { current = null },
                                onDragCancel = { current = null },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val s = current ?: return@detectDragGestures
                                    // کپی جدید تا Compose تغییر را ببیند
                                    val idx = strokes.lastIndex
                                    if (idx >= 0) strokes[idx] = s.copy(points = (s.points + change.position).toMutableList()).also { current = it }
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        strokes.forEach { s ->
                            if (s.points.size < 2) {
                                drawCircle(if (s.eraser) Color.White else s.color, radius = s.width.dp.toPx() / 2f, center = s.points.first())
                            } else {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(s.points[0].x, s.points[0].y)
                                    for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                                }
                                drawPath(
                                    path,
                                    color = if (s.eraser) Color.White else s.color,
                                    style = Stroke(width = (if (s.eraser) s.width * 3f else s.width).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                    if (strokes.isEmpty()) {
                        Text("اینجا بنویسید یا بکشید…", color = Color(0xFF9CA3AF), modifier = Modifier.align(Alignment.Center))
                    }
                }
                Button(
                    onClick = {
                        val uri = runCatching { exportBoard(context.cacheDir, questionId, boardSize, strokes, density) }.getOrNull()
                        if (uri != null) onDone(uri)
                    },
                    enabled = strokes.isNotEmpty() && boardSize.width > 0,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("ثبت به‌عنوان تصویر پاسخ") }
            }
        }
    }
}

/** رندر خط‌ها روی Bitmap سفید و ذخیره به PNG در cache؛ خروجی `file://`. */
private fun exportBoard(cacheDir: File, questionId: String, size: IntSize, strokes: List<BoardStroke>, density: Float): String {
    val w = size.width.coerceAtLeast(1); val h = size.height.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    // ضخامت‌ها در Compose با dp.toPx بودند؛ همان چگالی واقعی صفحه.
    val dpToPx = density
    strokes.forEach { s ->
        paint.color = if (s.eraser) android.graphics.Color.WHITE else android.graphics.Color.argb(
            (s.color.alpha * 255).toInt(), (s.color.red * 255).toInt(), (s.color.green * 255).toInt(), (s.color.blue * 255).toInt()
        )
        paint.strokeWidth = (if (s.eraser) s.width * 3f else s.width) * dpToPx
        if (s.points.size < 2) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(s.points[0].x, s.points[0].y, paint.strokeWidth / 2f, paint)
            paint.style = Paint.Style.STROKE
        } else {
            val p = Path().apply {
                moveTo(s.points[0].x, s.points[0].y)
                for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
            }
            canvas.drawPath(p, paint)
        }
    }
    val dir = File(cacheDir, "whiteboard").apply { mkdirs() }
    val out = File(dir, "wb-${questionId.take(8)}-${System.currentTimeMillis()}.png")
    FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bmp.recycle()
    return out.toURI().toString()
}
