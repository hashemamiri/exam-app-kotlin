package ir.exam.app.ui.image

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import ir.exam.app.data.repository.LocalImageRepository
import ir.exam.app.domain.model.ImageEditRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** چرخش و برش مربعی با جابه‌جایی مستقیم چهار ضلع؛ بدون اسلایدر و حالت‌های اضافی. */
@Composable
fun InteractiveImageEditorDialog(
    source: Uri,
    forceSquare: Boolean = false,
    onDismiss: () -> Unit,
    onDone: (Uri) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { LocalImageRepository(context) }
    val scope = rememberCoroutineScope()
    var rotation by remember(source) { mutableIntStateOf(0) }
    var cropActive by remember(source, forceSquare) { mutableStateOf(forceSquare) }
    var cropCenterX by remember(source) { mutableFloatStateOf(.5f) }
    var cropCenterY by remember(source) { mutableFloatStateOf(.5f) }
    var cropSide by remember(source) { mutableFloatStateOf(.78f) }
    var sourcePixels by remember(source) { mutableStateOf(Size.Unspecified) }
    var safeSource by remember(source) { mutableStateOf<Uri?>(null) }
    var preparing by remember(source) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sourceBytes by remember(source) { mutableStateOf<Long?>(null) }
    val focusManager = LocalFocusManager.current
    // با بازشدن ویرایشگر، صفحه‌کلید بسته می‌شود تا دکمه‌های پایین همیشه در دسترس باشند.
    LaunchedEffect(Unit) { focusManager.clearFocus() }
    LaunchedEffect(source) {
        preparing = true
        error = null
        repository.prepare(ImageEditRequest(source))
            .onSuccess { prepared ->
                safeSource = prepared.uri
                sourceBytes = withContext(Dispatchers.IO) { imageByteSize(context, prepared.uri) }
                preparing = false
            }
            .onFailure {
                safeSource = null
                error = it.message ?: "آماده‌سازی امن تصویر انجام نشد."
                preparing = false
            }
    }

    // Size.Unspecified اجازهٔ خواندن width/height نمی‌دهد و پیش از اتمام بارگذاری
    // تصویر IllegalStateException ایجاد می‌کند. اندازهٔ امن در تمام محاسبات preview،
    // برش و حجم استفاده می‌شود تا اولین composition ویرایشگر هرگز crash نکند.
    val safePixels = safeImagePixelSize(sourcePixels)

    // ارتفاع پنجره از ۹۲٪ صفحه بیشتر نمی‌شود و محتوا در صورت نیاز اسکرول می‌شود؛
    // به این ترتیب دکمه‌های تأیید/انصراف هرگز زیر صفحه یا زیر صفحه‌کلید نمی‌مانند.
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * .92f).toInt()

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().imePadding().heightIn(max = maxDialogHeight.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 12.dp
        ) {
            Column(
                Modifier.padding(14.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ویرایش تصویر", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally)
                ) {
                    ImageToolButton(
                        "چرخش به چپ",
                        Icons.Outlined.RotateLeft,
                        enabled = safeSource != null && !preparing
                    ) {
                        rotation = (rotation + 270) % 360
                    }
                    ImageToolButton(
                        if (forceSquare) "برش دایره‌ای پروفایل" else "برش مربعی",
                        Icons.Outlined.Crop,
                        selected = cropActive,
                        enabled = safeSource != null && !preparing
                    ) {
                        cropActive = if (forceSquare) true else !cropActive
                        if (cropActive) {
                            cropCenterX = .5f
                            cropCenterY = .5f
                            cropSide = .78f
                        }
                    }
                    ImageToolButton(
                        "چرخش به راست",
                        Icons.Outlined.RotateRight,
                        enabled = safeSource != null && !preparing
                    ) {
                        rotation = (rotation + 90) % 360
                    }
                }

                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFF17191D), RoundedCornerShape(18.dp))
                        .clipToBounds()
                ) {
                    val rawWidth = safePixels.width
                    val rawHeight = safePixels.height
                    val (rotatedWidth, rotatedHeight) =
                        CropGeometry.rotatedSize(rawWidth, rawHeight, rotation)
                    val quarterTurn = rotatedWidth != rawWidth
                    val targetAspect = rotatedWidth / rotatedHeight
                    val boxAspect = maxWidth.value / maxHeight.value
                    val displayWidth: Dp
                    val displayHeight: Dp
                    if (targetAspect >= boxAspect) {
                        displayWidth = maxWidth
                        displayHeight = maxWidth / targetAspect
                    } else {
                        displayHeight = maxHeight
                        displayWidth = maxHeight * targetAspect
                    }
                    val displayLeft = (maxWidth - displayWidth) / 2
                    val displayTop = (maxHeight - displayHeight) / 2
                    val layerWidth = if (quarterTurn) displayHeight else displayWidth
                    val layerHeight = if (quarterTurn) displayWidth else displayHeight
                    val layerLeft = (maxWidth - layerWidth) / 2
                    val layerTop = (maxHeight - layerHeight) / 2

                    AsyncImage(
                        model = safeSource,
                        contentDescription = "پیش‌نمایش تصویر امن",
                        contentScale = ContentScale.FillBounds,
                        onSuccess = { sourcePixels = it.painter.intrinsicSize },
                        modifier = Modifier
                            .offset(layerLeft, layerTop)
                            .size(layerWidth, layerHeight)
                            .graphicsLayer(rotationZ = rotation.toFloat())
                    )
                    if (preparing) {
                        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    if (cropActive && safeSource != null) {
                        val minDimension = minOf(displayWidth, displayHeight)
                        val side = minDimension * cropSide
                        val sideXFraction = side.value / displayWidth.value
                        val sideYFraction = side.value / displayHeight.value
                        val safeCenterX = CropGeometry.clampCenter(cropCenterX, sideXFraction)
                        val safeCenterY = CropGeometry.clampCenter(cropCenterY, sideYFraction)
                        val frameLeft = displayLeft + displayWidth * safeCenterX - side / 2
                        val frameTop = displayTop + displayHeight * safeCenterY - side / 2
                        val density = LocalDensity.current
                        val displayWidthPx = with(density) { displayWidth.toPx() }
                        val displayHeightPx = with(density) { displayHeight.toPx() }
                        val minDimensionPx = with(density) { minDimension.toPx() }

                        CropFrame(
                            modifier = Modifier.offset(frameLeft, frameTop).size(side),
                            circular = forceSquare,
                            onMove = { dx, dy ->
                                val (movedX, movedY) = CropGeometry.moveCenter(
                                    safeCenterX,
                                    safeCenterY,
                                    dx,
                                    dy,
                                    displayWidthPx,
                                    displayHeightPx,
                                    sideXFraction,
                                    sideYFraction
                                )
                                cropCenterX = movedX
                                cropCenterY = movedY
                            },
                            onResize = { edge, dx, dy ->
                                val oldSide = cropSide
                                // V55.14 — ضلع: مؤلفهٔ عمود بر لبه؛ گوشه: بردار قطری.
                                val signed = when (edge) {
                                    CropEdgeKind.LEFT, CropEdgeKind.RIGHT ->
                                        CropGeometry.resizeDeltaForEdge(edge, dx)
                                    CropEdgeKind.TOP, CropEdgeKind.BOTTOM ->
                                        CropGeometry.resizeDeltaForEdge(edge, dy)
                                    else -> CropGeometry.resizeDeltaForCorner(edge, dx, dy)
                                }
                                cropSide = CropGeometry.resizeSide(cropSide, signed, minDimensionPx)
                                val pixelChange = (cropSide - oldSide) * minDimensionPx
                                val (newCenterX, newCenterY) = CropGeometry.recenterAfterResize(
                                    edge,
                                    pixelChange,
                                    displayWidthPx,
                                    displayHeightPx,
                                    cropCenterX,
                                    cropCenterY
                                )
                                cropCenterX = newCenterX
                                cropCenterY = newCenterY
                            }
                        )
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy) CircularProgressIndicator()

                val estimatedBytes = sourceBytes?.let { bytes ->
                    if (!cropActive) bytes else {
                        val areaFraction = CropGeometry.areaFraction(
                            cropSide,
                            safePixels.width,
                            safePixels.height,
                            rotation
                        )
                        (bytes * areaFraction).toLong().coerceAtLeast(1L)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color(0xFF19945B), shape = RoundedCornerShape(16.dp)) {
                        IconButton(
                            enabled = !busy && !preparing && safeSource != null,
                            onClick = {
                                val preparedSource = safeSource ?: return@IconButton
                                busy = true
                                error = null
                                scope.launch {
                                    val crop = if (cropActive) {
                                        CropGeometry.cropRect(
                                            cropCenterX,
                                            cropCenterY,
                                            cropSide,
                                            safePixels.width,
                                            safePixels.height,
                                            rotation
                                        )
                                    } else null
                                    repository.prepare(
                                        ImageEditRequest(preparedSource, crop, rotation, forceSquare)
                                    ).onSuccess { onDone(it.uri) }
                                        .onFailure {
                                            error = it.message
                                            busy = false
                                        }
                                }
                            }
                        ) { Icon(Icons.Outlined.Check, contentDescription = "اعمال و ذخیره", tint = Color.White) }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            estimatedBytes?.let(::readableImageBytes)?.let { "حجم تقریبی: $it" }
                                ?: "در حال محاسبه حجم",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Surface(color = Color(0xFFD63B49), shape = RoundedCornerShape(16.dp)) {
                        IconButton(enabled = !busy, onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "انصراف", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageToolButton(
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = .32f)
            )
        }
    }
}

@Composable
private fun CropFrame(
    modifier: Modifier,
    circular: Boolean,
    onMove: (Float, Float) -> Unit,
    onResize: (CropEdgeKind, Float, Float) -> Unit
) {
    val frameShape = if (circular) CircleShape else RoundedCornerShape(2.dp)
    // V55.14 — گزارش دستگاه: «اضلاع قابل جابه‌جایی نیست». دستگیره‌ها نامرئی
    // بودند و ناحیهٔ ۱۸dp کنارِ درست همان لبه، عملاً پیدا/لمس نمی‌شد. اکنون:
    // میله‌های سفید مرئی وسط اضلاع + مربع‌های سفید مرئی گوشه‌ها با سطح لمس
    // بزرگ (۳۲dp)؛ حرکت آزاد کل کادر از ناحیهٔ داخلی حفظ شده است.
    Box(modifier.border(2.dp, Color.White, frameShape)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(22.dp)
                .pointerInput(circular) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        onMove(drag.x, drag.y)
                    }
                }
        )
        // اضلاع
        CropHandle(CropEdgeKind.TOP, Modifier.align(Alignment.TopCenter), onResize, bar = true, horizontal = true)
        CropHandle(CropEdgeKind.BOTTOM, Modifier.align(Alignment.BottomCenter), onResize, bar = true, horizontal = true)
        CropHandle(CropEdgeKind.LEFT, Modifier.align(Alignment.CenterStart), onResize, bar = true, horizontal = false)
        CropHandle(CropEdgeKind.RIGHT, Modifier.align(Alignment.CenterEnd), onResize, bar = true, horizontal = false)
        // گوشه‌ها (درخواست کاربر: تغییر سایز از گوشه‌ها)
        CropHandle(CropEdgeKind.TOP_LEFT, Modifier.align(Alignment.TopStart), onResize)
        CropHandle(CropEdgeKind.TOP_RIGHT, Modifier.align(Alignment.TopEnd), onResize)
        CropHandle(CropEdgeKind.BOTTOM_LEFT, Modifier.align(Alignment.BottomStart), onResize)
        CropHandle(CropEdgeKind.BOTTOM_RIGHT, Modifier.align(Alignment.BottomEnd), onResize)
    }
}

@Composable
private fun CropHandle(
    edge: CropEdgeKind,
    modifier: Modifier,
    onResize: (CropEdgeKind, Float, Float) -> Unit,
    bar: Boolean = false,
    horizontal: Boolean = false
) {
    Box(
        modifier
            .size(32.dp)
            .pointerInput(edge) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onResize(edge, drag.x, drag.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // نشانگر مرئی: میله برای اضلاع، مربع کوچک برای گوشه‌ها.
        if (bar) {
            Box(
                Modifier
                    .size(width = if (horizontal) 26.dp else 5.dp, height = if (horizontal) 5.dp else 26.dp)
                    .background(Color.White, RoundedCornerShape(3.dp))
            )
        } else {
            Box(Modifier.size(13.dp).background(Color.White, RoundedCornerShape(3.dp)))
        }
    }
}

/**
 * [Size.Unspecified] در Compose یک Size عادی با width/height قابل خواندن نیست؛
 * getterهای آن عمداً IllegalStateException می‌اندازند. این مرز واحد جلوی دسترسی
 * زودهنگام در اولین composition و هنگام تعویض source را می‌گیرد.
 */
internal fun safeImagePixelSize(size: Size): Size =
    if (size == Size.Unspecified) Size(1f, 1f)
    else Size(size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))

private fun imageByteSize(context: android.content.Context, uri: Uri): Long? {
    if (uri.scheme.equals("file", true)) return uri.path?.let(::File)?.takeIf(File::isFile)?.length()
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0).takeIf { it >= 0L } else null
        }
    }.getOrNull()
}

private fun readableImageBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f مگابایت".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.0f کیلوبایت".format(bytes / 1024.0)
    else -> "$bytes بایت"
}
