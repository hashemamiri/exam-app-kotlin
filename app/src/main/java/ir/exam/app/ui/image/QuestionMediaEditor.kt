package ir.exam.app.ui.image

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.data.repository.LocalImageRepository
import ir.exam.app.domain.model.ImageEditRequest
import ir.exam.app.ui.builder.MediaDraft
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** مدیریت رسانهٔ متن سؤال: دوربین و thumbnailهای آیکنی در یک سطر، بدون کارت جداگانه. */
@Composable
fun QuestionMediaEditor(
    images: List<MediaDraft>,
    freePlacement: Boolean,
    onAdd: (List<String>) -> Unit,
    onMove: (String, Float, Float) -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { LocalImageRepository(context) }
    val scope = rememberCoroutineScope()
    var processing by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
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
        if (uris.isNotEmpty()) {
            processing = true
            imageError = null
            scope.launch {
                val safeUris = mutableListOf<String>()
                var firstError: String? = null
                uris.forEach { uri ->
                    repository.prepare(ImageEditRequest(uri))
                        .onSuccess { safeUris += it.uri.toString() }
                        .onFailure { if (firstError == null) firstError = it.message }
                }
                if (safeUris.isNotEmpty()) onAdd(safeUris)
                imageError = firstError
                processing = false
            }
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(
            onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        ) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = "افزودن تصویر متن سؤال")
        }
        if (processing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        if (images.isEmpty()) {
            Text("تصویر", style = MaterialTheme.typography.labelSmall)
        } else {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(images, key = MediaDraft::id) { image ->
                    CompactImageThumbnail(
                        uri = image.uri,
                        description = "تصویر متن سؤال",
                        onRemove = { onRemove(image.id) }
                    )
                }
            }
        }
    }

    imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    if (images.isNotEmpty() && freePlacement) {
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
                                onMove(image.id, image.xMm + drag.x / 2f, image.yMm + drag.y / 2f)
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

@Composable
private fun CompactImageThumbnail(
    uri: String,
    description: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(30.dp)
        )
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).size(17.dp).clickable(onClick = onRemove),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "حذف تصویر",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
