package ir.exam.app.ui.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.ui.builder.MediaDraft
import kotlin.math.roundToInt

/** انتخاب چند تصویر، حذف و حرکت مستقل؛ URI محلی هنگام ذخیره واقعاً در Storage آپلود می‌شود. */
@Composable
fun QuestionMediaEditor(
    images: List<MediaDraft>,
    onAdd: (List<String>) -> Unit,
    onMove: (String, Float, Float) -> Unit,
    onRemove: (String) -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris -> onAdd(uris.map(Uri::toString)) }

    Button(onClick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }) { Text("افزودن تصویر") }

    if (images.isEmpty()) return

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
                    .offset { IntOffset((image.xMm * 2).roundToInt(), (image.yMm * 2).roundToInt()) }
                    .size((image.widthMm * 2).dp)
                    .pointerInput(image.id, image.xMm, image.yMm) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onMove(image.id, image.xMm + drag.x / 2f, image.yMm + drag.y / 2f)
                        }
                    }
            ) {
                AsyncImage(
                    model = image.uri,
                    contentDescription = "تصویر سؤال",
                    modifier = Modifier.fillMaxSize()
                )
                TextButton(
                    onClick = { onRemove(image.id) },
                    modifier = Modifier.align(Alignment.TopEnd).background(Color.White.copy(alpha = 0.8f))
                ) { Text("✕") }
            }
        }
    }
}
