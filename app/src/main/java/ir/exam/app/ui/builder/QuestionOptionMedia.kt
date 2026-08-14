package ir.exam.app.ui.builder

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.ui.image.InteractiveImageEditorDialog
import ir.exam.app.ui.math.ExistingFormulaEditor
import ir.exam.app.ui.math.NativeMathText
import kotlin.math.roundToInt

@Composable
fun SingleImagePicker(
    value: String?,
    label: String,
    modifier: Modifier = Modifier,
    onChange: (String?) -> Unit
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            editing = uri
        }
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = if (value.isNullOrBlank()) label else "تعویض $label")
        }
        if (!value.isNullOrBlank()) {
            AsyncImage(
                model = value,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(30.dp)
            )
            Surface(
                modifier = Modifier.size(17.dp).clickable { onChange(null) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "حذف $label",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
    editing?.let { uri ->
        InteractiveImageEditorDialog(
            source = uri,
            onDismiss = { editing = null },
            onDone = {
                onChange(it.toString())
                editing = null
            }
        )
    }
}

@Composable
fun ReorderDragButton(
    description: String,
    onMove: (Int) -> Unit
) {
    var accumulated by remember { mutableFloatStateOf(0f) }
    var active by remember { mutableStateOf(false) }
    val stepPx = with(LocalDensity.current) { 46.dp.toPx() }
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) {
        IconButton(
            onClick = {},
            modifier = Modifier.pointerInput(description) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { accumulated = 0f; active = true },
                    onDragCancel = { accumulated = 0f; active = false },
                    onDragEnd = {
                        val steps = (accumulated / stepPx).roundToInt()
                        if (steps != 0) onMove(steps)
                        accumulated = 0f
                        active = false
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        accumulated += amount.y
                    }
                )
            }
        ) {
            Icon(Icons.Outlined.DragIndicator, contentDescription = description)
        }
    }
}

fun persianOptionLetter(index: Int): String = listOf(
    "الف", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د",
    "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط", "ظ",
    "ع", "غ", "ف", "ق", "ک", "گ", "ل", "م", "ن", "و"
).getOrElse(index) { PersianDigits.convert(index + 1) }

@Composable
private fun MatchingItemTools(
    label: String,
    image: String?,
    imageLabel: String,
    onFormula: () -> Unit,
    onImage: (String?) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onFormula) {
            Icon(Icons.Outlined.Functions, contentDescription = "درج فرمول $label")
        }
        SingleImagePicker(image, imageLabel, onChange = onImage)
        ReorderDragButton("نگه‌دارید و $label را جابه‌جا کنید", onMove)
        TextButton(onClick = onDelete, enabled = deleteEnabled) { Text("حذف") }
    }
}

@Composable
fun MatchingQuestionEditor(
    question: QuestionDraft,
    viewModel: ExamBuilderViewModel,
    onFormulaEdit: (side: String, index: Int, occurrence: Int?, tex: String) -> Unit = { _, _, _, _ -> },
    onFormulaDelete: (side: String, index: Int, occurrence: Int) -> Unit = { _, _, _ -> }
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ابتدا موارد ستون راست و سپس موارد ستون چپ را تنظیم کنید.")

        Text("ستون راست", style = MaterialTheme.typography.titleSmall)
        question.matchingRight.forEachIndexed { index, value ->
            val label = persianOptionLetter(index)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    MatchingItemTools(
                        label = label,
                        image = question.matchingRightImages.getOrNull(index),
                        imageLabel = "تصویر $label",
                        onFormula = { onFormulaEdit("right", index, null, "") },
                        onImage = { viewModel.setMatchingImage(question.id, "right", index, it) },
                        onMove = { viewModel.moveMatchingItem(question.id, "right", index, it) },
                        onDelete = { viewModel.removeMatchingSide(question.id, "right", index) },
                        deleteEnabled = question.matchingRight.size > 2
                    )
                    OutlinedTextField(
                        value,
                        { viewModel.updateMatchingText(question.id, "right", index, it) },
                        placeholder = { Text("متن $label") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if ('$' in value) NativeMathText(value)
                    ExistingFormulaEditor(
                        source = value,
                        onEdit = { occurrence, tex -> onFormulaEdit("right", index, occurrence, tex) },
                        onDelete = { occurrence -> onFormulaDelete("right", index, occurrence) }
                    )
                }
            }
        }
        TextButton(
            onClick = { viewModel.addMatchingSide(question.id, "right") },
            enabled = question.matchingRight.size < 30
        ) { Text("افزودن مورد راست") }

        Text("ستون چپ", style = MaterialTheme.typography.titleSmall)
        question.matchingLeft.forEachIndexed { index, value ->
            val label = PersianDigits.convert(index + 1)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    MatchingItemTools(
                        label = label,
                        image = question.matchingLeftImages.getOrNull(index),
                        imageLabel = "تصویر مورد $label",
                        onFormula = { onFormulaEdit("left", index, null, "") },
                        onImage = { viewModel.setMatchingImage(question.id, "left", index, it) },
                        onMove = { viewModel.moveMatchingItem(question.id, "left", index, it) },
                        onDelete = { viewModel.removeMatchingSide(question.id, "left", index) },
                        deleteEnabled = question.matchingLeft.size > 2
                    )
                    OutlinedTextField(
                        value,
                        { viewModel.updateMatchingText(question.id, "left", index, it) },
                        placeholder = { Text("متن مورد $label") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if ('$' in value) NativeMathText(value)
                    ExistingFormulaEditor(
                        source = value,
                        onEdit = { occurrence, tex -> onFormulaEdit("left", index, occurrence, tex) },
                        onDelete = { occurrence -> onFormulaDelete("left", index, occurrence) }
                    )
                    Text("پاسخ صحیح این مورد")
                    question.matchingRight.indices.chunked(6).forEach { choices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            choices.forEach { right ->
                                FilterChip(
                                    selected = question.matchingPairs[index] == right,
                                    onClick = { viewModel.setMatchingPair(question.id, index, right) },
                                    label = { Text(persianOptionLetter(right)) }
                                )
                            }
                        }
                    }
                }
            }
        }
        TextButton(
            onClick = { viewModel.addMatchingSide(question.id, "left") },
            enabled = question.matchingLeft.size < 30
        ) { Text("افزودن مورد چپ") }
    }
}
