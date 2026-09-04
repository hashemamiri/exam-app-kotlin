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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * ابزارهای پیشرفتهٔ باقی‌مانده (تفکیک چندسؤاله، لاک‌گیر، برچسب/فلش، صفحه‌ای/۴گوشه،
 * تشخیص خودکار زاویه) در پچ‌های بعدی بومی می‌شوند؛ تا آن‌موقع دکمهٔ «ابزار کامل»
 * همان استودیوی HTML را باز می‌کند (__qmfOpenLegacyStudio) تا هیچ امکانی از دست نرود.
 */
@Composable
fun ExamImageStudioDialog(
    questionId: String?,
    onInsert: (dataUrl: String, heightPx: Int) -> Unit,
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
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeBounded(context, uri, 2560)
            if (decoded != null) {
                original = decoded
                rotation = 0; flip = false; crop = Rect(0f, 0f, 1f, 1f)
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
                                            src, rotation, deskewAngle, flip, crop, scanOn, threshold, outSize, quality
                                        )
                                    }
                                    processing = false
                                    result?.let { (dataUrl, h) -> onInsert(dataUrl, h) }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("تایید و درج")
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
                            Image(
                                bitmap = imgBitmap,
                                contentDescription = "پیش‌نمایش تصویر",
                                modifier = Modifier.fillMaxSize(),
                                colorFilter = scanPreview,
                                contentScale = ContentScale.Fit
                            )
                            // قاب برش با دو دستگیره / یا چهار گوشهٔ صفحه‌ای (V76.5)
                            Canvas(
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(aspect, perspMode, boxSize.width, boxSize.height) {
                                        detectDragGestures(
                                            onDragStart = { pos ->
                                                if (perspMode) {
                                                    dragTarget = Corner.PERSP
                                                    dragPerspIndex = nearestPerspIndex(pos, offX, offY, drawW, drawH, perspPts)
                                                    dragOffset = Offset.Zero
                                                    return@detectDragGestures
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
                                                if (perspMode) {
                                                    val x = (change.position.x - offX).coerceIn(0f, drawW) / drawW
                                                    val y = (change.position.y - offY).coerceIn(0f, drawH) / drawH
                                                    if (dragPerspIndex >= 0) {
                                                        perspPts = perspPts.toMutableList().also { it[dragPerspIndex] = Offset(x, y) }
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
                                if (perspMode) {
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
            }
        }
    }
}

private enum class Corner { START, END, MOVE, PERSP }
private var dragTarget by androidx.compose.runtime.mutableStateOf(Corner.MOVE)
private var dragOffset by androidx.compose.runtime.mutableStateOf(Offset.Zero)
private var dragPerspIndex by androidx.compose.runtime.mutableStateOf(-1)

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
    quality: Int
): Pair<String, Int>? = runCatching {
    val m = Matrix().apply {
        postRotate(rotation.toFloat() + deskew)
        if (flip) postScale(-1f, 1f)
    }
    var bmp = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    val cx = (crop.left * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
    val cy = (crop.top * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
    val cw = max(1, ((crop.width) * bmp.width).roundToInt().coerceAtMost(bmp.width - cx))
    val ch = max(1, ((crop.height) * bmp.height).roundToInt().coerceAtMost(bmp.height - cy))
    bmp = Bitmap.createBitmap(bmp, cx, cy, cw, ch)

    if (scanOn) {
        val w = bmp.width; val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            val v = if (lum >= threshold) 255 else 0
            pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    if (outSize > 0) {
        val largest = max(bmp.width, bmp.height)
        if (largest > outSize) {
            val scale = outSize.toFloat() / largest
            bmp = Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * scale).roundToInt().coerceAtLeast(1),
                (bmp.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        }
    }

    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), out)
    val dataUrl = "data:image/jpeg;base64," +
        android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
    dataUrl to bmp.height
}.getOrNull()

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
