package ir.exam.app.ui.image

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.ui.builder.MediaDraft
import kotlin.math.roundToInt

/** تصاویر برای مدیریت همیشه کنار هم‌اند؛ بوم جداگانه فقط در حالت جای‌گذاری آزاد دیده می‌شود. */
@Composable
fun QuestionMediaEditor(
    images: List<MediaDraft>,
    freePlacement: Boolean,
    onAdd: (List<String>) -> Unit,
    onMove: (String, Float, Float) -> Unit,
    onResize: (String, Float) -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    var editQueue by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        editQueue = uris
    }

    Button(onClick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }) { Text("افزودن تصویر") }

    editQueue.firstOrNull()?.let { uri ->
        InteractiveImageEditorDialog(
            source = uri,
            onDismiss = { editQueue = editQueue.drop(1) },
            onDone = { edited ->
                onAdd(listOf(edited.toString()))
                editQueue = editQueue.drop(1)
            }
        )
    }

    if (images.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(images, key = MediaDraft::id) { image ->
            Card(Modifier.width(156.dp)) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.fillMaxWidth().height(112.dp)) {
                        AsyncImage(
                            model = image.uri,
                            contentDescription = "تصویر سؤال",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { onRemove(image.id) },
                            modifier = Modifier.align(Alignment.TopEnd).background(Color.White.copy(alpha = .82f))
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "حذف تصویر")
                        }
                    }
                    Text(
                        "عرض ${image.widthMm.toInt()} میلی‌متر",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = image.widthMm,
                        onValueChange = { onResize(image.id, it) },
                        valueRange = 20f..190f
                    )
                }
            }
        }
    }

    if (freePlacement) {
        Text("چیدمان آزاد تصاویر", style = MaterialTheme.typography.titleSmall)
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Color.White)
                .border(1.dp, Color.Gray)
                .clipToBounds()
        ) {
            images.forEach { image ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (image.xMm * 2).roundToInt(),
                                (image.yMm * 2).roundToInt()
                            )
                        }
                        .size((image.widthMm * 2).dp)
                        .pointerInput(image.id, image.xMm, image.yMm) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                onMove(
                                    image.id,
                                    image.xMm + drag.x / 2f,
                                    image.yMm + drag.y / 2f
                                )
                            }
                        }
                ) {
                    AsyncImage(
                        model = image.uri,
                        contentDescription = "تصویر آزاد سؤال",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
