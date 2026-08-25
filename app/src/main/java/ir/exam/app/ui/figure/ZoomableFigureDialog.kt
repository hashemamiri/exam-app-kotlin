package ir.exam.app.ui.figure

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * V57.0 — پنجرهٔ زوم تمام‌صفحه برای شکل/نمودار/تصویر سمت دانش‌آموز:
 * بزرگ‌نمایی با دو انگشت (۱ تا ۶ برابر) + جابه‌جایی؛ دوضربه = بازنشانی.
 * برای جدول تناوبی (kind='p') گزینهٔ «نمایش افقی» محتوا را ۹۰ درجه
 * می‌چرخاند تا تمام‌عرض صفحه باز شود؛ پیش‌فرض همان حالت افقی است.
 *
 * V58.0 — گزارش دستگاه: نوار بالایی روی جدول چرخیده می‌افتاد. ساختار به
 * Column تبدیل شد: نوار بالا جدا و محتوا در BoxWithConstraints زیر آن؛
 * اندازهٔ چرخش از محدودهٔ«زیر نوار» محاسبه می‌شود و دیگر همپوشانی ندارد.
 */
@Composable
fun ZoomableFigureDialog(
    onDismiss: () -> Unit,
    title: String = "",
    rotatable: Boolean = false,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var landscape by remember { mutableStateOf(rotatable) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title.ifBlank { "بزرگ‌نمایی" },
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (rotatable) {
                        FilterChip(
                            selected = landscape,
                            onClick = {
                                landscape = !landscape
                                scale = 1f
                                offset = Offset.Zero
                            },
                            label = { Text("نمایش افقی") }
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("بستن ✕") }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val maxW = maxWidth
                    val maxH = maxHeight
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 6f)
                                    offset = if (scale <= 1f) Offset.Zero else offset + pan
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = {
                                    scale = 1f
                                    offset = Offset.Zero
                                })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .then(
                                    // چرخش ۹۰ درجه در محدودهٔ زیر نوار بالا؛ جای پهنا و
                                    // ارتفاع عوض می‌شود تا جدول تمام‌قد باز شود.
                                    if (landscape) Modifier
                                        .requiredSize(width = maxH, height = maxW)
                                        .graphicsLayer { rotationZ = 90f }
                                    else Modifier.fillMaxSize()
                                )
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}
