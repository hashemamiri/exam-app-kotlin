package ir.exam.app.ui.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import ir.exam.app.ui.builder.MediaDraft
import kotlin.math.roundToInt

/** انتخاب چند تصویر و حرکت مستقل هرکدام روی بوم ثابت؛ مختصات بر حسب میلی‌متر ذخیره می‌شود. */
@Composable
fun QuestionMediaEditor(
    images: List<MediaDraft>,
    onAdd: (List<String>) -> Unit,
    onMove: (String, Float, Float) -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris -> onAdd(uris.map(Uri::toString)) }
    Button(onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("افزودن تصویر") }
    Box(
        Modifier.fillMaxWidth().height(360.dp).background(Color.White).border(1.dp, Color.Gray).clipToBounds()
    ) {
        images.forEach { image ->
            AsyncImage(
                model = image.uri,
                contentDescription = "تصویر سؤال",
                modifier = Modifier
                    .offset { IntOffset((image.xMm * 2).roundToInt(), (image.yMm * 2).roundToInt()) }
                    .size((image.widthMm * 2).dp)
                    .pointerInput(image.id) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onMove(image.id, image.xMm + drag.x / 2f, image.yMm + drag.y / 2f)
                        }
                    }
            )
        }
    }
}
