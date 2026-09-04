package ir.exam.app.ui.printing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.tan
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * V76.4 — هستهٔ بومیِ استودیوی تصویر سؤال (پچ اول از مسیر «بومیِ کامل»):
 * دوربین/گالری ← چرخش ۹۰° چپ/راست، قرینه، برش آزاد و نسبت‌ها (بدون برش/آزاد/۱:۱/۴:۳/۱۶:۹)،
 * سفیدسازی اسکن (سیاه‌سفید با آستانه — پیش‌فرض ۱۸۵ مطابق استودیو)، اندازهٔ خروجی
 * S/M/L/∞ (۲۴۰/۴۲۰/۶۴۰/اصلی) و کیفیت (۴۰..۱۰۰ — پیش‌فرض ۹۲). خروجی با همان قرارداد
 * استودیو (dataUrl + ارتفاع پیکسلی) به سؤال درج می‌شود.
 * V76.5 — ابزارهای صفحه/صاف‌سازی: برشِ صفحه‌ای با انتخاب ۴ گوشه (وارپِ
 * پرسپکتیو با setPolyToPoly — همان «📍 انتخاب ۴ گوشه صفحه / ✓ اعمال صاف‌سازی»)،
 * صاف‌سازیِ دقیقِ ±۱۵° (اسلایدر + شبکهٔ راهنما) و «🎯 تشخیص خودکار زاویه»
 * با پروفایلِ تصویر (بهینه‌سازی واریانسِ ردیف‌های تاریک).
 * V76.6 — تفکیک چندسؤاله (کادرهای ۲/۳/۴تایی + کادرِ دستی؛ «همه بخش‌ها به همین
 * سؤال» یا «هر بخش → سؤال جداگانه» عین رفتار استودیو: کپیِ ساختارِ سؤالِ مبدا،
 * متن خالی، یک تصویر برای هر بخش) + مدیریتِ تصویرهای موجودِ سؤال
 * (فهرست/ویرایشِ دوباره/حذف — پل‌های __qmfQuestionImages/__qmfRemoveQuestionImage/
 * __qmfReplaceQuestionImage).
 * V76.7 — ابزارهای رسم (لاک‌گیر/برچسب/فلش): فلش و فلش دوسر، خط، کادر، بیضی،
 * خط آزاد، هایلایتر نیمه‌شفاف، سانسورِ پیکسلی، برچسب متنی؛ ۴ رنگ، انتخاب/جابه‌جایی،
 * بازگردانی/انجام مجدد، حذف انتخاب/پاک کردن همه، مقایسهٔ قبل/بعد؛ همه در لحظهٔ
 * «تایید» روی خروجی پخته می‌شوند (bakeShapes در زنجیرهٔ encodeCropped).
 */
/** V76.6 — تصویرِ موجودِ سؤال برای مدیریت (فهرست/ویرایشِ دوباره/حذف). */
data class StudioImageRef(val dataUrl: String, val w: Int, val h: Int)

/**
 * V76.7 — شکلِ رسم‌شده روی تصویر؛ مختصات نرمال‌شده (۰..۱) نسبت به تصویرِ
 * نمایش‌داده‌شده تا در پیش‌نمایش و پخت (ابعاد واقعی) یکسان بمانند.
 */
data class StudioShape(
    val type: String,                                  // arrow/arrow2/line/rect/ellipse/free/highlighter/censor/text
    val points: List<Offset> = emptyList(),
    val color: Int = 0xFFDC2626.toInt(),
    val text: String = ""
)

@Composable
fun ExamImageStudioDialog(
    questionId: String?,
    existingImages: List<StudioImageRef> = emptyList(),
    onInsert: (dataUrl: String, heightPx: Int) -> Unit,
    onDeleteExisting: (Int) -> Unit = {},
    onReplaceExisting: (Int, String, Int) -> Unit = { _, _, _ -> },
    onSplitToSame: (List<Pair<String, Int>>) -> Unit = {},
    onSplitToQuestions: (List<Pair<String, Int>>) -> Unit = {},
    onLegacyStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableStateOf(0) }
    var flip by remember { mutableStateOf(false) }
    // برش نرمال‌شده روی تصویرِ چرخیده
    var crop by remember { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }
    var scanOn by remember { mutableStateOf(false) }
    var threshold by remember { mutableStateOf(185) }
    var outSize by remember { mutableStateOf(420) }
    var quality by remember { mutableStateOf(92) }
    var processing by remember { mutableStateOf(false) }
    var aspect by remember { mutableStateOf("free") }
    // V76.5 — صاف‌سازیِ دقیق و صفحه‌ای (۴ گوشه)
    var deskewAngle by remember { mutableStateOf(0f) }
    var deskewGrid by remember { mutableStateOf(false) }
    var perspMode by remember { mutableStateOf(false) }
    var perspPts by remember { mutableStateOf(listOf(Offset(0.12f, 0.12f), Offset(0.88f, 0.12f), Offset(0.88f, 0.88f), Offset(0.12f, 0.88f))) }
    var note by remember { mutableStateOf<String?>(null) }
    // V76.6 — مدیریتِ تصویر موجود + تفکیک چندسؤاله
    var editIndex by remember { mutableStateOf(-1) }
    var splitMode by remember { mutableStateOf(false) }
    var splitBoxes by remember { mutableStateOf(listOf(Rect(0f, 0f, 1f, 0.5f), Rect(0f, 0.5f, 1f, 1f))) }
    var selectedBox by remember { mutableStateOf(0) }
    // V76.7 — ابزارهای رسم
    var drawMode by remember { mutableStateOf("none") } // none/arrow/arrow2/line/rect/ellipse/free/highlighter/censor/text
    var drawColor by remember { mutableStateOf(0xFFDC2626.toInt()) }
    var shapes by remember { mutableStateOf(listOf<StudioShape>()) }
    var redoStack by remember { mutableStateOf(listOf<StudioShape>()) }
    var selectedShape by remember { mutableStateOf(-1) }
    var activeShape by remember { mutableStateOf<StudioShape?>(null) }
    var textPromptPoint by remember { mutableStateOf<Offset?>(null) }
    var textPromptValue by remember { mutableStateOf("") }
    var showTextPrompt by remember { mutableStateOf(false) }
    var previewOriginal by remember { mutableStateOf(false) }
    var openedWith by remember { mutableStateOf<Bitmap?>(null) }

    fun makeCameraUri(): Pair<Uri, File> {
        val dir = File(context.cacheDir, "studio").apply { mkdirs() }
        val f = File.createTempFile("studio-", ".jpg", dir)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
        return uri to f
    }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingCameraFile
        pendingCameraFile = null
        if (ok && f != null) {
            val decoded = decodeBounded(f, 2560)
            f.delete()
            if (decoded != null) {
                original = decoded
                rotation = 0; flip = false; crop = Rect(0f, 0f, 1f, 1f)
                editIndex = -1
                openedWith = decoded
                shapes = emptyList(); redoStack = emptyList(); selectedShape = -1
                drawMode = "none"; previewOriginal = false
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeBounded(context, uri, 2560)
            if (decoded != null) {
                original = decoded
                rotation = 0; flip = false; crop = Rect(0f, 0f, 1f, 1f)
                editIndex = -1
                openedWith = decoded
                shapes = emptyList(); redoStack = emptyList(); selectedShape = -1
                drawMode = "none"; previewOriginal = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!processing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // نوار بالا
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (!processing) onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                    Text(
                        "تصویر سؤال" + (questionId?.let { " — سؤال $it" } ?: ""),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (original != null && questionId != null) {
                        Button(
                            onClick = {
                                val src = original ?: return@Button
                                processing = true
                                scope.launch {
                                    val result = withContext(Dispatchers.Default) {
                                        processAndEncode(
                                            src, rotation, deskewAngle, flip, crop, scanOn, threshold, outSize, quality, shapes
                                        )
                                    }
                                    processing = false
                                    result?.let { (dataUrl, h) ->
                                        if (editIndex >= 0) onReplaceExisting(editIndex, dataUrl, h) else onInsert(dataUrl, h)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (editIndex >= 0) "تایید و جایگزینی" else "تایید و درج")
                        }
                    }
                }

                if (original == null) {
                    // انتخاب منبع
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("تصویر از کجا بیاید؟", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = {
                                runCatching {
                                    val (uri, f) = makeCameraUri()
                                    pendingCameraFile = f
                                    cameraLauncher.launch(uri)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) { Text("📷 دوربین", style = MaterialTheme.typography.titleMedium) }
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) { Text("🖼️ گالری", style = MaterialTheme.typography.titleMedium) }
                        // V76.6 — تصویرهای موجودِ همین سؤال: ویرایشِ دوباره یا حذف
                        if (existingImages.isNotEmpty()) {
                            Text(
                                "تصویرهای فعلی این سؤال (" + existingImages.size + "):",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                existingImages.forEachIndexed { idx, ref ->
                                    var thumb by remember(ref.dataUrl) { mutableStateOf<ImageBitmap?>(null) }
                                    LaunchedEffect(ref.dataUrl) {
                                        thumb = withContext(Dispatchers.IO) {
                                            decodeDataUrlBounded(ref.dataUrl, 200)?.asImageBitmap()
                                        }
                                    }
                                    Card(Modifier.fillMaxWidth()) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier.size(56.dp).background(Color(0xFFF1F5F9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                thumb?.let {
                                                    Image(bitmap = it, contentDescription = "بند‌انگشتی تصویر " + (idx + 1), contentScale = ContentScale.Crop)
                                                }
                                            }
                                            Text("تصویر " + (idx + 1), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                                            TextButton(onClick = {
                                                scope.launch {
                                                    val bmp = withContext(Dispatchers.IO) { decodeDataUrlBounded(ref.dataUrl, 2560) }
                                                    if (bmp != null) {
                                                        editIndex = idx
                                                        original = bmp
                                                        rotation = 0; flip = false
                                                        crop = Rect(0f, 0f, 1f, 1f); aspect = "free"
                                                        deskewAngle = 0f; perspMode = false; splitMode = false
                                                        openedWith = bmp
                                                        shapes = emptyList(); redoStack = emptyList(); selectedShape = -1
                                                        drawMode = "none"; previewOriginal = false
                                                        note = null
                                                    } else {
                                                        note = "بازکردن تصویر ممکن نشد."
                                                    }
                                                }
                                            }) { Text("✏️ ویرایش") }
                                            TextButton(onClick = { onDeleteExisting(idx) }) {
                                                Text("🗑️ حذف", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (questionId != null) {
                            OutlinedButton(
                                onClick = onLegacyStudio,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🧰 ابزارهای کامل (استودیو — موقت تا پورت نهایی)") }
                        }
                    }
                } else {
                    // پیش‌نمایش + برش
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF17203A))
                    ) {
                        var boxSize by remember { mutableStateOf(IntSize.Zero) }
                        val shown = remember(original, rotation, flip, deskewAngle, perspMode) {
                            val m = Matrix().apply {
                                if (!perspMode) {
                                    postRotate(rotation.toFloat() + deskewAngle)
                                    if (flip) postScale(-1f, 1f)
                                }
                            }
                            Bitmap.createBitmap(original!!, 0, 0, original!!.width, original!!.height, m, true)
                        }
                        val imgBitmap: ImageBitmap = shown.asImageBitmap()
                        Box(
                            Modifier
                                .fillMaxSize()
                                .onSizeChanged { boxSize = it },
                            contentAlignment = Alignment.Center
                        ) {
                            val imgAspect = shown.width.toFloat() / shown.height.toFloat()
                            val boxW = boxSize.width.toFloat().coerceAtLeast(1f)
                            val boxH = boxSize.height.toFloat().coerceAtLeast(1f)
                            val boxAspect = boxW / boxH
                            val drawW: Float
                            val drawH: Float
                            if (boxAspect > imgAspect) {
                                drawH = boxH; drawW = boxH * imgAspect
                            } else {
                                drawW = boxW; drawH = boxW / imgAspect
                            }
                            val offX = (boxW - drawW) / 2f
                            val offY = (boxH - drawH) / 2f
                            // V76.4.1 — پیش‌نمایشِ کنتراستِ بالای تقریبی با colorFilter خود Image؛
                            // آستانهٔ دقیقِ سیاه‌سفید هنگام «تایید و درج» اعمال می‌شود.
                            val scanPreview: ColorFilter? = if (scanOn) {
                                val s = 4f
                                val off = 128f - 255f * (threshold / 255f) * s
                                ColorFilter.colorMatrix(
                                    ColorMatrix(
                                        floatArrayOf(
                                            s, 0f, 0f, 0f, off,
                                            0f, s, 0f, 0f, off,
                                            0f, 0f, s, 0f, off,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                    )
                                )
                            } else null
                            val displayBitmap: ImageBitmap =
                                if (previewOriginal && openedWith != null) openedWith!!.asImageBitmap() else imgBitmap
                            Image(
                                bitmap = displayBitmap,
                                contentDescription = "پیش‌نمایش تصویر",
                                modifier = Modifier.fillMaxSize(),
                                colorFilter = scanPreview,
                                contentScale = ContentScale.Fit
                            )
                            // قاب برش با دو دستگیره / یا چهار گوشهٔ صفحه‌ای (V76.5)
                            Canvas(
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(aspect, perspMode, splitMode, splitBoxes.size, drawMode, boxSize.width, boxSize.height) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                if (dragTarget == Corner.DRAW) {
                                                    val sp = activeShape
                                                    if (sp != null) {
                                                        val ok = if (sp.type == "free" || sp.type == "highlighter") {
                                                            sp.points.size > 2
                                                        } else {
                                                            sp.points.size >= 2 &&
                                                                dist(sp.points[0], sp.points[1]) > 0.02f
                                                        }
                                                        if (ok) {
                                                            shapes = shapes + sp
                                                            redoStack = emptyList()
                                                        }
                                                    }
                                                    activeShape = null
                                                    dragTarget = Corner.IDLE
                                                }
                                            },
                                            onDragStart = { pos ->
                                                if (splitMode) {
                                                    val nx = (pos.x - offX) / drawW
                                                    val ny = (pos.y - offY) / drawH
                                                    var hit = -1
                                                    splitBoxes.forEachIndexed { bi, b ->
                                                        if (nx >= b.left && nx <= b.right && ny >= b.top && ny <= b.bottom) hit = bi
                                                    }
                                                    selectedBox = if (hit >= 0) hit else selectedBox
                                                    dragSplitIndex = if (hit >= 0) hit else -1
                                                    dragTarget = if (hit >= 0 && dist(pos, Offset(offX + splitBoxes[hit].left * drawW, offY + splitBoxes[hit].bottom * drawH)) < 90f) Corner.SPLIT_RESIZE else Corner.SPLIT_MOVE
                                                    dragOffset = Offset.Zero
                                                    return@detectDragGestures
                                                }
                                                if (perspMode) {
                                                    dragTarget = Corner.PERSP
                                                    dragPerspIndex = nearestPerspIndex(pos, offX, offY, drawW, drawH, perspPts)
                                                    dragOffset = Offset.Zero
                                                    return@detectDragGestures
                                                }
                                                if (drawMode != "none") {
                                                    // V76.7 — کشیدن شکل جدید / یا لمس برای برچسب متنی
                                                    val nx = ((pos.x - offX) / drawW).coerceIn(0f, 1f)
                                                    val ny = ((pos.y - offY) / drawH).coerceIn(0f, 1f)
                                                    if (drawMode == "text") {
                                                        textPromptPoint = Offset(nx, ny)
                                                        textPromptValue = ""
                                                        showTextPrompt = true
                                                        return@detectDragGestures
                                                    }
                                                    selectedShape = -1
                                                    activeShape = StudioShape(
                                                        type = drawMode,
                                                        points = listOf(Offset(nx, ny), Offset(nx, ny)),
                                                        color = drawColor
                                                    )
                                                    dragTarget = Corner.DRAW
                                                    dragOffset = Offset.Zero
                                                    return@detectDragGestures
                                                }
                                                if (shapes.isNotEmpty()) {
                                                    // V76.7 — انتخاب/جابه‌جایی شکل موجود
                                                    val nx = (pos.x - offX) / drawW
                                                    val ny = (pos.y - offY) / drawH
                                                    var hit = -1
                                                    shapes.forEachIndexed { si, sp ->
                                                        if (sp.points.isEmpty()) return@forEachIndexed
                                                        val xs = sp.points.map { it.x }
                                                        val ys = sp.points.map { it.y }
                                                        val m = 0.035f
                                                        // min/max لیستی در kotlin.math نیست — minOrNull/maxOrNull
                                                        val xsMin = xs.minOrNull() ?: nx
                                                        val xsMax = xs.maxOrNull() ?: nx
                                                        val ysMin = ys.minOrNull() ?: ny
                                                        val ysMax = ys.maxOrNull() ?: ny
                                                        if (nx >= xsMin - m && nx <= xsMax + m &&
                                                            ny >= ysMin - m && ny <= ysMax + m
                                                        ) hit = si
                                                    }
                                                    selectedShape = hit
                                                    if (hit >= 0) {
                                                        dragShapeIndex = hit
                                                        dragTarget = Corner.SHAPE_MOVE
                                                        dragOffset = Offset.Zero
                                                        return@detectDragGestures
                                                    }
                                                }
                                                dragTarget = when {
                                                    dist(pos, Offset(offX + crop.left * drawW, offY + crop.top * drawH)) < 90f -> Corner.START
                                                    dist(pos, Offset(offX + crop.right * drawW, offY + crop.bottom * drawH)) < 90f -> Corner.END
                                                    else -> Corner.MOVE
                                                }
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, drag ->
                                                change.consume()
                                                if (splitMode) {
                                                    val dx = drag.x / drawW
                                                    val dy = drag.y / drawH
                                                    if (dragSplitIndex in splitBoxes.indices) {
                                                        val old = splitBoxes[dragSplitIndex]
                                                        val updated = if (dragTarget == Corner.SPLIT_RESIZE) {
                                                            old.copy(
                                                                left = (old.left + dx).coerceIn(0f, old.right - 0.05f),
                                                                bottom = (old.bottom + dy).coerceIn(old.top + 0.05f, 1f)
                                                            )
                                                        } else {
                                                            val w = old.width; val h = old.height
                                                            val nl = (old.left + dx).coerceIn(0f, 1f - w)
                                                            val nt = (old.top + dy).coerceIn(0f, 1f - h)
                                                            Rect(nl, nt, nl + w, nt + h)
                                                        }
                                                        splitBoxes = splitBoxes.toMutableList().also { it[dragSplitIndex] = updated }
                                                    }
                                                    return@detectDragGestures
                                                }
                                                if (perspMode) {
                                                    val x = (change.position.x - offX).coerceIn(0f, drawW) / drawW
                                                    val y = (change.position.y - offY).coerceIn(0f, drawH) / drawH
                                                    if (dragPerspIndex >= 0) {
                                                        perspPts = perspPts.toMutableList().also { it[dragPerspIndex] = Offset(x, y) }
                                                    }
                                                    return@detectDragGestures
                                                }
                                                if (dragTarget == Corner.DRAW) {
                                                    // V76.7 — شکلِ زنده
                                                    val sp = activeShape
                                                    if (sp != null) {
                                                        val nx = ((change.position.x - offX) / drawW).coerceIn(0f, 1f)
                                                        val ny = ((change.position.y - offY) / drawH).coerceIn(0f, 1f)
                                                        activeShape = if (sp.type == "free" || sp.type == "highlighter") {
                                                            sp.copy(points = sp.points + Offset(nx, ny))
                                                        } else {
                                                            sp.copy(points = listOf(sp.points.first(), Offset(nx, ny)))
                                                        }
                                                    }
                                                    return@detectDragGestures
                                                }
                                                if (dragTarget == Corner.SHAPE_MOVE) {
                                                    // V76.7 — جابه‌جایی شکلِ انتخاب‌شده
                                                    val dx = drag.x / drawW
                                                    val dy = drag.y / drawH
                                                    if (dragShapeIndex in shapes.indices) {
                                                        shapes = shapes.toMutableList().also { list ->
                                                            val old = list[dragShapeIndex]
                                                            list[dragShapeIndex] = old.copy(points = old.points.map { Offset(it.x + dx, it.y + dy) })
                                                        }
                                                    }
                                                    return@detectDragGestures
                                                }
                                                val x = (change.position.x - offX) / drawW
                                                val y = (change.position.y - offY) / drawH
                                                when (dragTarget) {
                                                    Corner.START -> crop = Rect(
                                                        x.coerceIn(0f, crop.right - 0.05f),
                                                        y.coerceIn(0f, crop.bottom - 0.05f),
                                                        crop.right, crop.bottom
                                                    )
                                                    Corner.END -> crop = Rect(
                                                        crop.left, crop.top,
                                                        x.coerceIn(crop.left + 0.05f, 1f),
                                                        y.coerceIn(crop.top + 0.05f, 1f)
                                                    )
                                                    else -> {
                                                        val w = crop.width; val h = crop.height
                                                        var l = crop.left + drag.x / drawW
                                                        var t = crop.top + drag.y / drawH
                                                        l = l.coerceIn(0f, 1f - w)
                                                        t = t.coerceIn(0f, 1f - h)
                                                        crop = Rect(l, t, l + w, t + h)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                if (previewOriginal) {
                                    // نمای «قبل»: تصویر خام بدون هیچ پوششی
                                } else if (splitMode) {
                                    // V76.6 — کادرهای تفکیک با شماره و انتخاب
                                    splitBoxes.forEachIndexed { bi, b ->
                                        val l = offX + b.left * drawW
                                        val t = offY + b.top * drawH
                                        val r = offX + b.right * drawW
                                        val btm = offY + b.bottom * drawH
                                        drawRect(
                                            color = if (bi == selectedBox) Color(0xFF16A34A) else Color(0xFFFFC107),
                                            topLeft = Offset(l, t),
                                            size = Size(r - l, btm - t),
                                            style = Stroke(width = if (bi == selectedBox) 4f else 3f)
                                        )
                                        drawIntoCanvas { c ->
                                            val bg = android.graphics.Paint().apply {
                                                color = if (bi == selectedBox) 0xFF16A34A.toInt() else 0xFFFFC107.toInt()
                                                isAntiAlias = true
                                            }
                                            val fg = android.graphics.Paint().apply {
                                                color = android.graphics.Color.WHITE; textSize = 28f
                                                textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                                            }
                                            c.nativeCanvas.drawCircle(l + 26f, t + 26f, 22f, bg)
                                            c.nativeCanvas.drawText((bi + 1).toString(), l + 26f, t + 36f, fg)
                                        }
                                        if (bi == selectedBox) {
                                            // دستگیرهٔ اندازه (پایین-چپ)
                                            drawCircle(Color(0xFF4F46E5), radius = 16f, center = Offset(l, btm))
                                        }
                                    }
                                } else if (perspMode) {
                                    // V76.5 — چهارگوشِ صفحه با نقطه‌های شماره‌دار ۱..۴
                                    val ptsPx = perspPts.map { Offset(offX + it.x * drawW, offY + it.y * drawH) }
                                    val quad = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(ptsPx[0].x, ptsPx[0].y)
                                        for (i in 1 until 4) lineTo(ptsPx[i].x, ptsPx[i].y)
                                        close()
                                    }
                                    drawPath(quad, Color(0xFFFFC107), style = Stroke(width = 3f))
                                    drawIntoCanvas { c ->
                                        val bg = android.graphics.Paint().apply { color = 0xFF4F46E5.toInt(); isAntiAlias = true }
                                        val fg = android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE; textSize = 30f
                                            textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                                        }
                                        ptsPx.forEachIndexed { i, p ->
                                            c.nativeCanvas.drawCircle(p.x, p.y, 24f, bg)
                                            c.nativeCanvas.drawText((i + 1).toString(), p.x, p.y + 10f, fg)
                                        }
                                    }
                                } else {
                                    if (deskewGrid) {
                                        // شبکهٔ راهنمای صاف‌سازی (کمکِ بصریِ هم‌ترازی خطوط)
                                        val step = drawH / 12f
                                        var gy = offY
                                        while (gy <= offY + drawH) {
                                            drawLine(Color(0x664F46E5), Offset(offX, gy), Offset(offX + drawW, gy), strokeWidth = 1.5f)
                                            gy += step
                                        }
                                    }
                                    val l = offX + crop.left * drawW
                                    val t = offY + crop.top * drawH
                                    val r = offX + crop.right * drawW
                                    val b = offY + crop.bottom * drawH
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(l, t),
                                        size = Size(r - l, b - t),
                                        style = Stroke(width = 3f)
                                    )
                                    drawCircle(Color(0xFF4F46E5), radius = 16f, center = Offset(l, t))
                                    drawCircle(Color(0xFF4F46E5), radius = 16f, center = Offset(r, b))
                                }
                                // V76.7 — شکل‌های رسم‌شده + شکلِ درحال کشیدن (پیش‌نمایش زنده)
                                if (!previewOriginal && (shapes.isNotEmpty() || activeShape != null)) {
                                    val baseW = max(3f, kotlin.math.min(drawW, drawH) * 0.008f)
                                    fun P(p: Offset) = Offset(offX + p.x * drawW, offY + p.y * drawH)
                                    fun head(a: Offset, tip: Offset, sw: Float, col: Color) {
                                        val ang = kotlin.math.atan2(tip.y - a.y, tip.x - a.x)
                                        val l = sw * 3.2f
                                        drawLine(col, tip, Offset(tip.x - l * kotlin.math.cos(ang + 0.45f), tip.y - l * kotlin.math.sin(ang + 0.45f)), strokeWidth = sw)
                                        drawLine(col, tip, Offset(tip.x - l * kotlin.math.cos(ang - 0.45f), tip.y - l * kotlin.math.sin(ang - 0.45f)), strokeWidth = sw)
                                    }
                                    (shapes + listOfNotNull(activeShape)).forEachIndexed { si, sp ->
                                        val col = Color(sp.color)
                                        val hl = sp.type == "highlighter"
                                        val sw = if (hl) baseW * 3.5f else baseW
                                        val st = Stroke(width = sw)
                                        if (sp.type == "arrow" || sp.type == "arrow2" || sp.type == "line") {
                                            if (sp.points.size >= 2) {
                                                val a = P(sp.points[0]); val b2 = P(sp.points[1])
                                                drawLine(col, a, b2, strokeWidth = sw)
                                                if (sp.type != "line") head(a, b2, sw, col)
                                                if (sp.type == "arrow2") head(b2, a, sw, col)
                                            }
                                        } else if (sp.type == "rect" || sp.type == "ellipse" || sp.type == "censor") {
                                            if (sp.points.size >= 2) {
                                                val a = P(sp.points[0]); val b2 = P(sp.points[1])
                                                val tl = Offset(kotlin.math.min(a.x, b2.x), kotlin.math.min(a.y, b2.y))
                                                val sz = Size(kotlin.math.abs(a.x - b2.x), kotlin.math.abs(a.y - b2.y))
                                                if (sp.type == "censor") {
                                                    drawRect(Color(0xFF1F2937), topLeft = tl, size = sz)
                                                    if (si == selectedShape) drawRect(Color(0xFF4F46E5), topLeft = tl, size = sz, style = Stroke(width = 2f))
                                                } else if (sp.type == "rect") {
                                                    drawRect(col, topLeft = tl, size = sz, style = st)
                                                } else {
                                                    drawOval(col, topLeft = tl, size = sz, style = st)
                                                }
                                            }
                                        } else if (sp.type == "free" || sp.type == "highlighter") {
                                            if (sp.points.size >= 2) {
                                                val path = androidx.compose.ui.graphics.Path().apply {
                                                    val f = P(sp.points[0])
                                                    moveTo(f.x, f.y)
                                                    for (i in 1 until sp.points.size) {
                                                        val q = P(sp.points[i])
                                                        lineTo(q.x, q.y)
                                                    }
                                                }
                                                drawPath(path, if (hl) col.copy(alpha = 0.42f) else col, style = st)
                                            }
                                        } else if (sp.type == "text") {
                                            if (sp.points.isNotEmpty()) {
                                                val at = P(sp.points[0])
                                                drawIntoCanvas { c ->
                                                    val tp = android.graphics.Paint().apply {
                                                        color = sp.color; isAntiAlias = true
                                                        textSize = max(24f, drawH * 0.045f)
                                                    }
                                                    c.nativeCanvas.drawText(sp.text, at.x, at.y, tp)
                                                }
                                            }
                                        }
                                        if (si == selectedShape && drawMode == "none" && sp.points.isNotEmpty()) {
                                            drawCircle(Color(0xFF4F46E5).copy(alpha = 0.7f), radius = sw * 1.6f, center = P(sp.points.first()))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ابزارها
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ToolChip("↺ ۹۰° چپ") { rotation = (rotation + 270) % 360 }
                            ToolChip("↻ ۹۰° راست") { rotation = (rotation + 90) % 360 }
                            ToolChip("⇄ قرینه") { flip = !flip }
                            ToolChip("🖼️ بدون برش") { crop = Rect(0f, 0f, 1f, 1f); aspect = "free" }
                            ToolChip("برش آزاد") { aspect = "free" }
                            ToolChip("مربع ۱:۱") { aspect = "r11"; crop = centeredAspect(crop, 1f) }
                            ToolChip("۴:۳") { aspect = "r43"; crop = centeredAspect(crop, 4f / 3f) }
                            ToolChip("۱۶:۹") { aspect = "r169"; crop = centeredAspect(crop, 16f / 9f) }
                            ToolChip("✂️ تفکیک چندسؤاله") {
                                splitMode = !splitMode
                                if (splitMode) { perspMode = false; selectedBox = 0 }
                            }
                        }
                        // V76.7 — لاک‌گیر/برچسب/فلش (عین مجموعهٔ استودیو)
                        fun setDraw(m: String) {
                            drawMode = m
                            if (m != "none") {
                                splitMode = false; perspMode = false
                                selectedShape = -1; activeShape = null
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = drawMode == "none",
                                onClick = { setDraw("none") },
                                label = { Text("👆 انتخاب/جابجایی") }
                            )
                            ToolChip("➡️ فلش") { setDraw("arrow") }
                            ToolChip("↔️ فلش دوسر") { setDraw("arrow2") }
                            ToolChip("📏 خط") { setDraw("line") }
                            ToolChip("⬜ کادر") { setDraw("rect") }
                            ToolChip("⭕ بیضی") { setDraw("ellipse") }
                            ToolChip("✏️ خط آزاد") { setDraw("free") }
                            ToolChip("🖍️ هایلایتر") { setDraw("highlighter") }
                            ToolChip("🚫 سانسور") { setDraw("censor") }
                            ToolChip("🔤 متن") { setDraw("text") }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolChip("🔴") { drawColor = 0xFFDC2626.toInt() }
                            ToolChip("🔵") { drawColor = 0xFF2563EB.toInt() }
                            ToolChip("⚫") { drawColor = 0xFF111827.toInt() }
                            ToolChip("🟢") { drawColor = 0xFF16A34A.toInt() }
                            ToolChip("↩️ بازگردانی") {
                                if (shapes.isNotEmpty()) {
                                    redoStack = redoStack + shapes.last()
                                    shapes = shapes.dropLast(1)
                                    selectedShape = -1
                                }
                            }
                            ToolChip("↪️ انجام مجدد") {
                                redoStack.lastOrNull()?.let { last ->
                                    shapes = shapes + last
                                    redoStack = redoStack.dropLast(1)
                                }
                            }
                            ToolChip("🗑️ حذف انتخاب") {
                                if (selectedShape in shapes.indices) {
                                    redoStack = redoStack + shapes[selectedShape]
                                    shapes = shapes.filterIndexed { i, _ -> i != selectedShape }
                                    selectedShape = -1
                                }
                            }
                            ToolChip("🧹 پاک کردن همه") {
                                redoStack = emptyList()
                                shapes = emptyList()
                                selectedShape = -1
                                activeShape = null
                            }
                            FilterChip(
                                selected = previewOriginal,
                                onClick = { previewOriginal = !previewOriginal },
                                label = { Text("👁 قبل/بعد") }
                            )
                        }
                        // V76.5 — صاف‌سازی: صفحه‌ای ۴گوشه + خودکار + دقیق ±۱۵°
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = perspMode,
                                onClick = { perspMode = !perspMode; note = null },
                                label = { Text("📐 صفحه‌ای (۴ گوشه)") }
                            )
                            ToolChip("🎯 تشخیص خودکار زاویه") {
                                val src = original ?: return@ToolChip
                                processing = true; note = null
                                scope.launch {
                                    val ang = withContext(Dispatchers.Default) { detectSkewAngle(src, threshold) }
                                    processing = false
                                    deskewAngle = ang
                                    note = "زاویهٔ تشخیص‌شده: ${"%.1f".format(ang)}° — در صورت نیاز با اسلایدر تنظیم کنید."
                                }
                            }
                            ToolChip("صفر کردن صاف‌سازی") { deskewAngle = 0f }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("↯ صاف‌سازی: " + (if (deskewAngle >= 0) "+" else "") + "%.1f".format(deskewAngle) + "°", style = MaterialTheme.typography.labelLarge)
                            Slider(
                                value = deskewAngle,
                                onValueChange = { deskewAngle = (it * 10).roundToInt() / 10f },
                                valueRange = -15f..15f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("شبکه", style = MaterialTheme.typography.labelMedium)
                            Switch(checked = deskewGrid, onCheckedChange = { deskewGrid = it })
                        }
                        if (splitMode) {
                            // V76.6 — پیش‌فرض‌های تفکیک + اعمال (عین استودیو)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ToolChip("✂️ ۲ سؤال (بالا / پایین)") {
                                    splitBoxes = listOf(Rect(0f, 0f, 1f, 0.5f), Rect(0f, 0.5f, 1f, 1f)); selectedBox = 0
                                }
                                ToolChip("✂️ ۳ سؤال ستونی") {
                                    splitBoxes = listOf(
                                        Rect(0f, 0f, 1f / 3f, 1f), Rect(1f / 3f, 0f, 2f / 3f, 1f), Rect(2f / 3f, 0f, 1f, 1f)
                                    ); selectedBox = 0
                                }
                                ToolChip("✂️ ۴ سؤال (۲×۲)") {
                                    splitBoxes = listOf(
                                        Rect(0f, 0f, 0.5f, 0.5f), Rect(0.5f, 0f, 1f, 0.5f),
                                        Rect(0f, 0.5f, 0.5f, 1f), Rect(0.5f, 0.5f, 1f, 1f)
                                    ); selectedBox = 0
                                }
                                ToolChip("➕ کادر جدید") {
                                    splitBoxes = splitBoxes + Rect(0.08f, 0.62f, 0.92f, 0.94f)
                                    selectedBox = splitBoxes.lastIndex
                                }
                                ToolChip("🗑️ حذف کادر انتخاب‌شده") {
                                    if (splitBoxes.size > 1) {
                                        splitBoxes = splitBoxes.filterIndexed { i, _ -> i != selectedBox }
                                        selectedBox = selectedBox.coerceAtMost(splitBoxes.lastIndex)
                                    }
                                }
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (questionId != null) {
                                    Button(onClick = {
                                        val src = original ?: return@Button
                                        processing = true; note = null
                                        scope.launch {
                                            val results = withContext(Dispatchers.Default) {
                                                val m = Matrix().apply {
                                                    postRotate(rotation.toFloat() + deskewAngle)
                                                    if (flip) postScale(-1f, 1f)
                                                }
                                                val base = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
                                                splitBoxes.mapNotNull { b -> encodeCropped(base, b, scanOn, threshold, outSize, quality, shapes) }
                                            }
                                            processing = false
                                            if (results.isEmpty()) {
                                                note = "هیچ بخشی آماده نشد."
                                            } else {
                                                onSplitToSame(results)
                                            }
                                        }
                                    }) { Text("💾 همه بخش‌ها به همین سؤال") }
                                    Button(onClick = {
                                        val src = original ?: return@Button
                                        processing = true; note = null
                                        scope.launch {
                                            val results = withContext(Dispatchers.Default) {
                                                val m = Matrix().apply {
                                                    postRotate(rotation.toFloat() + deskewAngle)
                                                    if (flip) postScale(-1f, 1f)
                                                }
                                                val base = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
                                                splitBoxes.mapNotNull { b -> encodeCropped(base, b, scanOn, threshold, outSize, quality, shapes) }
                                            }
                                            processing = false
                                            if (results.isEmpty()) {
                                                note = "هیچ بخشی آماده نشد."
                                            } else {
                                                onSplitToQuestions(results)
                                            }
                                        }
                                    }) { Text("🧩 هر بخش → سؤال جداگانه") }
                                }
                                ToolChip("انصراف") { splitMode = false }
                                Text(
                                    splitBoxes.size.toString() + " کادر — بدنه = جابه‌جایی، دستگیره = اندازه",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        if (perspMode) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(onClick = {
                                    val src = original ?: return@Button
                                    processing = true; note = null
                                    scope.launch {
                                        val warped = withContext(Dispatchers.Default) { applyPerspective(src, perspPts) }
                                        processing = false
                                        if (warped != null) {
                                            original = warped
                                            rotation = 0; flip = false; deskewAngle = 0f
                                            crop = Rect(0f, 0f, 1f, 1f); aspect = "free"
                                            perspMode = false
                                        } else {
                                            note = "چهار نقطهٔ شماره‌دار را روی گوشه‌های صفحه بکشید و دوباره اعمال کنید."
                                        }
                                    }
                                }) { Text("✓ اعمال صاف‌سازی") }
                                ToolChip("انصراف") { perspMode = false }
                                Text("نقطه‌های ۱..۴ را روی گوشه‌های صفحه بکشید.", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📄 سفیدسازی اسکن", style = MaterialTheme.typography.labelLarge)
                            Switch(checked = scanOn, onCheckedChange = { scanOn = it })
                            if (scanOn) {
                                Text("آستانه: $threshold", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (scanOn) {
                            Slider(
                                value = threshold.toFloat(),
                                onValueChange = { threshold = it.roundToInt() },
                                valueRange = 100f..240f,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("اندازه:", style = MaterialTheme.typography.labelLarge)
                            listOf(240 to "S", 420 to "M", 640 to "L", 0 to "∞").forEach { (v, lbl) ->
                                FilterChip(
                                    selected = outSize == v,
                                    onClick = { outSize = v },
                                    label = { Text(lbl) }
                                )
                            }
                            Text("کیفیت: $quality", style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = quality.toFloat(),
                            onValueChange = { quality = it.roundToInt() },
                            valueRange = 40f..100f,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                if (processing) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.height(22.dp))
                        Text("در حال آماده‌سازی تصویر…")
                    }
                }
                note?.let {
                    Text(
                        it,
                        Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (showTextPrompt) {
                    // V76.7 — برچسب متنی: متن را بگیر و در نقطهٔ لمس بنشین
                    AlertDialog(
                        onDismissRequest = { showTextPrompt = false },
                        title = { Text("متن برچسب") },
                        text = {
                            OutlinedTextField(
                                value = textPromptValue,
                                onValueChange = { textPromptValue = it },
                                placeholder = { Text("مثلاً «شکل ۱» یا «الف»") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                val p = textPromptPoint
                                if (p != null && textPromptValue.isNotBlank()) {
                                    shapes = shapes + StudioShape(
                                        type = "text",
                                        points = listOf(p),
                                        color = drawColor,
                                        text = textPromptValue
                                    )
                                    redoStack = emptyList()
                                }
                                showTextPrompt = false
                            }) { Text("افزودن") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTextPrompt = false }) { Text("انصراف") }
                        }
                    )
                }
            }
        }
    }
}

private enum class Corner { START, END, MOVE, PERSP, SPLIT_MOVE, SPLIT_RESIZE, DRAW, SHAPE_MOVE, IDLE }
private var dragTarget by androidx.compose.runtime.mutableStateOf(Corner.MOVE)
private var dragOffset by androidx.compose.runtime.mutableStateOf(Offset.Zero)
private var dragPerspIndex by androidx.compose.runtime.mutableStateOf(-1)
private var dragSplitIndex by androidx.compose.runtime.mutableStateOf(-1)
private var dragShapeIndex by androidx.compose.runtime.mutableStateOf(-1)

/** نزدیک‌ترین نقطهٔ صفحه‌ای به لمس (آستانهٔ ۲۲۰px برای انگشت). */
private fun nearestPerspIndex(pos: Offset, offX: Float, offY: Float, drawW: Float, drawH: Float, pts: List<Offset>): Int {
    var best = -1
    var bestD = 220f
    pts.forEachIndexed { i, p ->
        val d = dist(pos, Offset(offX + p.x * drawW, offY + p.y * drawH))
        if (d < bestD) {
            bestD = d
            best = i
        }
    }
    return best
}

private fun dist(a: Offset, b: Offset): Float = (a - b).getDistance()

/** قابِ برش را با نسبتِ خواسته و مرکزِ ثابت تنظیم می‌کند. */
private fun centeredAspect(current: Rect, aspect: Float): Rect {
    val w0 = current.width
    val h0 = current.height
    val target = if (w0 / h0 > aspect) {
        val w = h0 * aspect
        Rect(current.center.x - w / 2f, current.top, current.center.x + w / 2f, current.bottom)
    } else {
        val h = w0 / aspect
        Rect(current.left, current.center.y - h / 2f, current.right, current.center.y + h / 2f)
    }
    val clampedLeft = target.left.coerceIn(0f, 1f - target.width)
    val clampedTop = target.top.coerceIn(0f, 1f - target.height)
    return Rect(clampedLeft, clampedTop, clampedLeft + target.width, clampedTop + target.height)
}

@Composable
private fun ToolChip(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(label) }
}

/** دیکود با محدودیت ابعاد (inSampleSize) تا حافظه نترکد. */
private fun decodeBounded(f: File, maxEdge: Int): Bitmap? = runCatching {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.absolutePath, opts)
    var sample = 1
    var w = opts.outWidth; var h = opts.outHeight
    while (max(w, h) / sample > maxEdge) sample *= 2
    val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(f.absolutePath, o2)
}.getOrNull()

private fun decodeBounded(context: android.content.Context, uri: Uri, maxEdge: Int): Bitmap? = runCatching {
    val input = context.contentResolver.openInputStream(uri) ?: return null
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeStream(input, null, opts)
    input.close()
    var sample = 1
    var w = opts.outWidth; var h = opts.outHeight
    while (max(w, h) / sample > maxEdge) sample *= 2
    val input2 = context.contentResolver.openInputStream(uri) ?: return null
    val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp = BitmapFactory.decodeStream(input2, null, o2)
    input2.close()
    bmp
}.getOrNull()

/** چرخش/قرینه + برش + سفیدسازی آستانه‌ای + مقیاس خروجی + JPEG→dataURL. */
private fun processAndEncode(
    src: Bitmap,
    rotation: Int,
    deskew: Float,
    flip: Boolean,
    crop: Rect,
    scanOn: Boolean,
    threshold: Int,
    outSize: Int,
    quality: Int,
    shapes: List<StudioShape> = emptyList()
): Pair<String, Int>? = runCatching {
    val m = Matrix().apply {
        postRotate(rotation.toFloat() + deskew)
        if (flip) postScale(-1f, 1f)
    }
    val bmp = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    encodeCropped(bmp, crop, scanOn, threshold, outSize, quality, shapes)
}.getOrNull()

/**
 * V76.6 — برشِ پیکسلی + اسکن + اندازه + کدگذاری؛ مشترک بین درجِ تکی
 * (processAndEncode) و تفکیک چندسؤاله (هر کادر جداگانه).
 */
private fun encodeCropped(
    bmp: Bitmap,
    box: Rect,
    scanOn: Boolean,
    threshold: Int,
    outSize: Int,
    quality: Int,
    shapes: List<StudioShape> = emptyList()
): Pair<String, Int>? = runCatching {
    // V76.7 — شکل‌ها قبل از برش روی تصویر پخته می‌شوند (در تفکیک هم هر بخش شکل‌ها را دارد)
    val painted = if (shapes.isEmpty()) bmp else bakeShapes(bmp, shapes)
    val cx = (box.left * painted.width).roundToInt().coerceIn(0, painted.width - 1)
    val cy = (box.top * painted.height).roundToInt().coerceIn(0, painted.height - 1)
    val cw = max(1, (box.width * painted.width).roundToInt().coerceAtMost(painted.width - cx))
    val ch = max(1, (box.height * painted.height).roundToInt().coerceAtMost(painted.height - cy))
    var out = Bitmap.createBitmap(painted, cx, cy, cw, ch)

    if (scanOn) {
        val w = out.width; val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            val v = if (lum >= threshold) 255 else 0
            pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    if (outSize > 0) {
        val largest = max(out.width, out.height)
        if (largest > outSize) {
            val scale = outSize.toFloat() / largest
            out = Bitmap.createScaledBitmap(
                out,
                (out.width * scale).roundToInt().coerceAtLeast(1),
                (out.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        }
    }

    val bos = ByteArrayOutputStream()
    out.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), bos)
    val dataUrl = "data:image/jpeg;base64," +
        android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
    dataUrl to out.height
}.getOrNull()

/** V76.6 — دیکدِ dataURL (تصویرِ موجودِ سؤال) با سقفِ ابعاد، برای ویرایشِ دوباره/بندانگشتی. */
internal fun decodeDataUrlBounded(dataUrl: String, maxDim: Int): Bitmap? = runCatching {
    val b64 = dataUrl.substringAfter("base64,", "")
    if (b64.isBlank()) return@runCatching null
    val bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
    if (bytes.isEmpty()) return@runCatching null
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    var sample = 1
    var largest = max(opts.outWidth, opts.outHeight)
    while (largest / (sample * 2) >= maxDim) sample *= 2
    BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample }
    )
}.getOrNull()

/**
 * V76.7 — پختِ شکل‌ها روی بیت‌مپ با مختصات نرمال‌شده × ابعاد واقعی؛
 * هایلایتر نیمه‌شفاف، سانسورِ پیکسلی (میانگینِ بلوک‌ها)، فلش‌ها با سرِ
 * محاسبه‌شده از atan2، متن با اندازهٔ نسبیِ ارتفاع.
 */
internal fun bakeShapes(base: Bitmap, shapes: List<StudioShape>): Bitmap = runCatching {
    if (shapes.isEmpty()) return@runCatching base
    val out = base.copy(Bitmap.Config.ARGB_8888, true) ?: return@runCatching base
    val cv = android.graphics.Canvas(out)
    val w = out.width.toFloat()
    val h = out.height.toFloat()
    val X = { p: Offset -> p.x * w }
    val Y = { p: Offset -> p.y * h }
    val baseStroke = (max(w, h) * 0.005f).coerceIn(3f, 24f)
    shapes.forEach { sp ->
        val hl = sp.type == "highlighter"
        val paint = android.graphics.Paint().apply {
            color = sp.color
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeWidth = if (hl) baseStroke * 3.5f else baseStroke
            if (hl) alpha = 110
        }
        if (sp.type == "line" || sp.type == "arrow" || sp.type == "arrow2") {
            if (sp.points.size >= 2) {
                val x0 = X(sp.points[0]); val y0 = Y(sp.points[0])
                val x1 = X(sp.points[1]); val y1 = Y(sp.points[1])
                cv.drawLine(x0, y0, x1, y1, paint)
                val hl2 = paint.strokeWidth * 3.2f
                fun head(fx: Float, fy: Float, a: Float) {
                    val dx1 = hl2 * kotlin.math.cos(a + 0.45f)
                    val dy1 = hl2 * kotlin.math.sin(a + 0.45f)
                    val dx2 = hl2 * kotlin.math.cos(a - 0.45f)
                    val dy2 = hl2 * kotlin.math.sin(a - 0.45f)
                    cv.drawLine(fx, fy, fx - dx1, fy - dy1, paint)
                    cv.drawLine(fx, fy, fx - dx2, fy - dy2, paint)
                }
                val ang = kotlin.math.atan2(y1 - y0, x1 - x0)
                head(x1, y1, ang)
                if (sp.type == "arrow2") head(x0, y0, ang + Math.PI.toFloat())
            }
        } else if (sp.type == "rect" || sp.type == "ellipse" || sp.type == "censor") {
            if (sp.points.size >= 2) {
                val l = kotlin.math.min(X(sp.points[0]), X(sp.points[1]))
                val t = kotlin.math.min(Y(sp.points[0]), Y(sp.points[1]))
                val r = kotlin.math.max(X(sp.points[0]), X(sp.points[1]))
                val b = kotlin.math.max(Y(sp.points[0]), Y(sp.points[1]))
                if (sp.type == "censor") {
                    // سانسورِ پیکسلی: میانگینِ بلوک‌های ناحیه (عین حسِ موزاییک)
                    val x0i = l.roundToInt().coerceIn(0, out.width - 1)
                    val y0i = t.roundToInt().coerceIn(0, out.height - 1)
                    val x1i = r.roundToInt().coerceIn(x0i + 1, out.width)
                    val y1i = b.roundToInt().coerceIn(y0i + 1, out.height)
                    val rgnW = x1i - x0i
                    val rgnH = y1i - y0i
                    val px = IntArray(rgnW * rgnH)
                    base.getPixels(px, 0, rgnW, x0i, y0i, rgnW, rgnH)
                    val bs = max(6f, kotlin.math.min(rgnW, rgnH) / 8f).roundToInt()
                    val fill = android.graphics.Paint().apply {
                        style = android.graphics.Paint.Style.FILL
                        isAntiAlias = false
                    }
                    var by = y0i
                    while (by < y1i) {
                        var bx = x0i
                        while (bx < x1i) {
                            val ex = minOf(bx + bs, x1i)
                            val ey = minOf(by + bs, y1i)
                            var sr = 0; var sg = 0; var sb = 0; var n = 0
                            var yy = by
                            while (yy < ey) {
                                var xx = bx
                                while (xx < ex) {
                                    val c0 = px[(yy - y0i) * rgnW + (xx - x0i)]
                                    sr += (c0 shr 16) and 0xFF
                                    sg += (c0 shr 8) and 0xFF
                                    sb += c0 and 0xFF
                                    n++
                                    xx++
                                }
                                yy++
                            }
                            fill.color = (0xFF shl 24) or ((sr / n) shl 16) or ((sg / n) shl 8) or (sb / n)
                            cv.drawRect(bx.toFloat(), by.toFloat(), ex.toFloat(), ey.toFloat(), fill)
                            bx += bs
                        }
                        by += bs
                    }
                } else if (sp.type == "rect") {
                    cv.drawRect(l, t, r, b, paint)
                } else {
                    cv.drawOval(android.graphics.RectF(l, t, r, b), paint)
                }
            }
        } else if (sp.type == "free" || sp.type == "highlighter") {
            if (sp.points.size >= 2) {
                val path = android.graphics.Path().apply {
                    moveTo(X(sp.points[0]), Y(sp.points[0]))
                    for (i in 1 until sp.points.size) lineTo(X(sp.points[i]), Y(sp.points[i]))
                }
                cv.drawPath(path, paint)
            }
        } else if (sp.type == "text") {
            if (sp.points.isNotEmpty()) {
                val tp = android.graphics.Paint().apply {
                    color = sp.color
                    isAntiAlias = true
                    textSize = (h * 0.045f).coerceIn(28f, 220f)
                }
                cv.drawText(sp.text, X(sp.points[0]), Y(sp.points[0]), tp)
            }
        }
    }
    out
}.getOrNull() ?: base

/**
 * V76.5 — «🎯 تشخیص خودکار زاویه»: پروفایلِ تصویر — تصویر را نمونه‌برداری
 * (~۲۰۰px) سیاه‌سفید می‌کند و برای هر زاویهٔ ۰.۵±۱۵° شیبِ خطوط را با
 * جابه‌جایی عمودی approximates؛ زاویه‌ای که بیشترین واریانسِ تاریکیِ ردیف‌ها
 * را بدهد (خطوط متنی هم‌تراز) پاسخ است.
 */
private fun detectSkewAngle(src: Bitmap, threshold: Int): Float {
    val targetW = 200
    val largest = max(src.width, src.height).coerceAtLeast(1)
    if (largest <= targetW) return 0f
    val scale = targetW.toFloat() / largest
    val w = (src.width * scale).roundToInt().coerceAtLeast(1)
    val h = (src.height * scale).roundToInt().coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(src, w, h, true) ?: return 0f
    val px = IntArray(w * h)
    small.getPixels(px, 0, w, 0, 0, w, h)
    val dark = FloatArray(w * h)
    for (i in px.indices) {
        val p = px[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        val lum = (r * 299 + g * 587 + b * 114) / 1000
        dark[i] = if (lum < threshold) 1f else 0f
    }
    var best = 0f
    var bestScore = -1f
    var angle = -15f
    while (angle <= 15.01f) {
        val tan = tan(Math.toRadians(angle.toDouble()))
        val scores = FloatArray(h)
        for (x in 0 until w) {
            val shift = x * tan
            for (y in 0 until h) {
                val yi = (y + shift).roundToInt()
                if (yi in 0 until h) scores[yi] += dark[y * w + x]
            }
        }
        val mean = scores.sum() / h
        var v = 0f
        for (sv in scores) {
            val d = sv - mean
            v += d * d
        }
        if (v > bestScore) {
            bestScore = v
            best = angle
        }
        angle += 0.5f
    }
    return best
}

/**
 * V76.5 — «✓ اعمال صاف‌سازی» صفحه‌ای: چهار نقطهٔ نرمال‌شدهٔ ۱..۴ با
 * Matrix.setPolyToPoly به مستطیلِ محصورکننده وارپ می‌شود (خروجی = صفحهٔ صاف).
 */
private fun applyPerspective(src: Bitmap, pts: List<Offset>): Bitmap? = runCatching {
    if (pts.size != 4) return@runCatching null
    val srcPts = FloatArray(8)
    pts.forEachIndexed { i, p ->
        srcPts[i * 2] = p.x.coerceIn(0f, 1f) * src.width
        srcPts[i * 2 + 1] = p.y.coerceIn(0f, 1f) * src.height
    }
    val xs = (0 until 4).map { srcPts[it * 2] }
    val ys = (0 until 4).map { srcPts[it * 2 + 1] }
    val l = xs.minOrNull() ?: return@runCatching null
    val r = xs.maxOrNull() ?: return@runCatching null
    val t = ys.minOrNull() ?: return@runCatching null
    val b = ys.maxOrNull() ?: return@runCatching null
    if (r - l < 10f || b - t < 10f) return@runCatching null
    val dst = floatArrayOf(l, t, r, t, r, b, l, b)
    val m = Matrix()
    if (!m.setPolyToPoly(srcPts, 0, dst, 0, 4)) return@runCatching null
    Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
}.getOrNull()
