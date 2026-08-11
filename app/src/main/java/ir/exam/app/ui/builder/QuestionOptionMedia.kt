package ir.exam.app.ui.builder

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun SingleImagePicker(
    value: String?,
    label: String,
    onChange: (String?) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onChange(uri.toString())
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!value.isNullOrBlank()) AsyncImage(value, label, Modifier.size(72.dp))
        OutlinedButton(onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text(if (value.isNullOrBlank()) "تصویر" else "تعویض") }
        if (!value.isNullOrBlank()) OutlinedButton(onClick = { onChange(null) }) { Text("حذف") }
    }
}

@Composable
fun MatchingQuestionEditor(question: QuestionDraft, viewModel: ExamBuilderViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("هر مورد ستون چپ را به شماره درست ستون راست متصل کنید.")
        question.matchingLeft.indices.forEach { index ->
            Card(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = question.matchingLeft.getOrElse(index) { "" },
                            onValueChange = { viewModel.updateMatchingText(question.id, "left", index, it) },
                            label = { Text("چپ ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = question.matchingRight.getOrElse(index) { "" },
                            onValueChange = { viewModel.updateMatchingText(question.id, "right", index, it) },
                            label = { Text("راست ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SingleImagePicker(
                            question.matchingLeftImages.getOrNull(index),
                            "تصویر چپ"
                        ) { viewModel.setMatchingImage(question.id, "left", index, it) }
                        SingleImagePicker(
                            question.matchingRightImages.getOrNull(index),
                            "تصویر راست"
                        ) { viewModel.setMatchingImage(question.id, "right", index, it) }
                    }
                    Text("پاسخ صحیح برای مورد چپ ${index + 1}:")
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        question.matchingRight.indices.forEach { rightIndex ->
                            FilterChip(
                                selected = question.matchingPairs[index] == rightIndex,
                                onClick = { viewModel.setMatchingPair(question.id, index, rightIndex) },
                                label = { Text((rightIndex + 1).toString()) }
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.addMatchingRow(question.id) }) { Text("افزودن ردیف") }
            OutlinedButton(onClick = { viewModel.removeMatchingRow(question.id) }) { Text("حذف ردیف آخر") }
        }
    }
}
