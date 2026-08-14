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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import ir.exam.app.data.repository.LocalImageRepository
import ir.exam.app.domain.model.CropRect
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

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 12.dp
        ) {
            Column(
                Modifier.padding(14.dp),
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
                        "برش مربعی",
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
                    val quarterTurn = rotation % 180 != 0
                    val rawWidth = sourcePixels.width.takeIf { it > 0f } ?: 1f
                    val rawHeight = sourcePixels.height.takeIf { it > 0f } ?: 1f
                    val rotatedWidth = if (quarterTurn) rawHeight else rawWidth
                    val rotatedHeight = if (quarterTurn) rawWidth else rawHeight
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
                        val safeCenterX = cropCenterX.coerceIn(
                            sideXFraction / 2f,
                            1f - sideXFraction / 2f
                        )
                        val safeCenterY = cropCenterY.coerceIn(
                            sideYFraction / 2f,
                            1f - sideYFraction / 2f
                        )
                        val frameLeft = displayLeft + displayWidth * safeCenterX - side / 2
                        val frameTop = displayTop + displayHeight * safeCenterY - side / 2
                        val density = LocalDensity.current
                        val displayWidthPx = with(density) { displayWidth.toPx() }
                        val displayHeightPx = with(density) { displayHeight.toPx() }
                        val minDimensionPx = with(density) { minDimension.toPx() }

                        CropFrame(
                            modifier = Modifier.offset(frameLeft, frameTop).size(side),
                            onMove = { dx, dy ->
                                cropCenterX = (safeCenterX + dx / displayWidthPx)
                                    .coerceIn(sideXFraction / 2f, 1f - sideXFraction / 2f)
                                cropCenterY = (safeCenterY + dy / displayHeightPx)
                                    .coerceIn(sideYFraction / 2f, 1f - sideYFraction / 2f)
                            },
                            onResize = { edge, delta ->
                                val oldSide = cropSide
                                val signed = when (edge) {
                                    CropEdge.LEFT, CropEdge.TOP -> -delta
                                    CropEdge.RIGHT, CropEdge.BOTTOM -> delta
                                }
                                cropSide = (cropSide + signed / minDimensionPx).coerceIn(.20f, .98f)
                                val pixelChange = (cropSide - oldSide) * minDimensionPx
                                when (edge) {
                                    CropEdge.LEFT -> cropCenterX -= pixelChange / 2f / displayWidthPx
                                    CropEdge.RIGHT -> cropCenterX += pixelChange / 2f / displayWidthPx
                                    CropEdge.TOP -> cropCenterY -= pixelChange / 2f / displayHeightPx
                                    CropEdge.BOTTOM -> cropCenterY += pixelChange / 2f / displayHeightPx
                                }
                            }
                        )
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy) CircularProgressIndicator()

                val estimatedBytes = sourceBytes?.let { bytes ->
                    if (!cropActive) bytes else {
                        val quarterTurn = rotation % 180 != 0
                        val rawWidth = sourcePixels.width.takeIf { it > 0f } ?: 1f
                        val rawHeight = sourcePixels.height.takeIf { it > 0f } ?: 1f
                        val imageWidth = if (quarterTurn) rawHeight else rawWidth
                        val imageHeight = if (quarterTurn) rawWidth else rawHeight
                        val shortSide = minOf(imageWidth, imageHeight) * cropSide
                        val areaFraction = (shortSide / imageWidth) * (shortSide / imageHeight)
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
                                        val quarterTurn = rotation % 180 != 0
                                        val rawWidth = sourcePixels.width.takeIf { it > 0f } ?: 1f
                                        val rawHeight = sourcePixels.height.takeIf { it > 0f } ?: 1f
                                        val imageWidth = if (quarterTurn) rawHeight else rawWidth
                                        val imageHeight = if (quarterTurn) rawWidth else rawHeight
                                        val shortSide = minOf(imageWidth, imageHeight) * cropSide
                                        val widthFraction = shortSide / imageWidth
                                        val heightFraction = shortSide / imageHeight
                                        CropRect(
                                            left = (cropCenterX - widthFraction / 2f).coerceIn(0f, 1f - widthFraction),
                                            top = (cropCenterY - heightFraction / 2f).coerceIn(0f, 1f - heightFraction),
                                            width = widthFraction,
                                            height = heightFraction
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

private enum class CropEdge { LEFT, RIGHT, TOP, BOTTOM }

@Composable
private fun CropFrame(
    modifier: Modifier,
    onMove: (Float, Float) -> Unit,
    onResize: (CropEdge, Float) -> Unit
) {
    Box(
        modifier
            .border(2.dp, Color.White, RoundedCornerShape(2.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onMove(drag.x, drag.y)
                }
            }
    ) {
        CropEdgeHandle(
            edge = CropEdge.TOP,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(26.dp),
            onResize = onResize
        )
        CropEdgeHandle(
            edge = CropEdge.BOTTOM,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(26.dp),
            onResize = onResize
        )
        CropEdgeHandle(
            edge = CropEdge.LEFT,
            modifier = Modifier.align(Alignment.CenterStart).width(26.dp).fillMaxHeight(),
            onResize = onResize
        )
        CropEdgeHandle(
            edge = CropEdge.RIGHT,
            modifier = Modifier.align(Alignment.CenterEnd).width(26.dp).fillMaxHeight(),
            onResize = onResize
        )
    }
}

@Composable
private fun CropEdgeHandle(
    edge: CropEdge,
    modifier: Modifier,
    onResize: (CropEdge, Float) -> Unit
) {
    Box(
        modifier.pointerInput(edge) {
            detectDragGestures { change, drag ->
                change.consume()
                onResize(edge, if (edge == CropEdge.LEFT || edge == CropEdge.RIGHT) drag.x else drag.y)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .then(
                    if (edge == CropEdge.TOP || edge == CropEdge.BOTTOM) {
                        Modifier.width(34.dp).height(5.dp)
                    } else {
                        Modifier.width(5.dp).height(34.dp)
                    }
                )
                .background(Color.White, RoundedCornerShape(4.dp))
        )
    }
}

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
