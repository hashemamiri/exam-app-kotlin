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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.ui.image.InteractiveImageEditorDialog
import ir.exam.app.ui.math.ExistingFormulaEditor
import ir.exam.app.ui.math.NativeMathText

@Composable
fun SingleImagePicker(
    value: String?,
    label: String,
    onChange: (String?) -> Unit
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            editing=uri
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!value.isNullOrBlank()) AsyncImage(value, label, Modifier.size(72.dp))
        OutlinedButton(onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text(if (value.isNullOrBlank()) "تصویر" else "تعویض") }
        if (!value.isNullOrBlank()) OutlinedButton(onClick = { onChange(null) }) { Text("حذف") }
    }
    editing?.let { uri -> InteractiveImageEditorDialog(
        source=uri,onDismiss={editing=null},onDone={onChange(it.toString());editing=null}
    ) }
}

@Composable
fun MatchingQuestionEditor(
    question: QuestionDraft,
    viewModel: ExamBuilderViewModel,
    onFormulaEdit: (side: String, index: Int, occurrence: Int?, tex: String) -> Unit = { _, _, _, _ -> },
    onFormulaDelete: (side: String, index: Int, occurrence: Int) -> Unit = { _, _, _ -> }
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ستون‌های جورکردنی می‌توانند تعداد متفاوت داشته باشند؛ موارد اضافی ستون چپ نقش حواس‌پرت‌کن دارند.")
        Text("ستون چپ", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        question.matchingLeft.forEachIndexed { index, value ->
            Card(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(value,{viewModel.updateMatchingText(question.id,"left",index,it)},label={Text("چپ ${index+1}")},modifier=Modifier.fillMaxWidth())
                    OutlinedButton(onClick={onFormulaEdit("left",index,null,"")}){Text("درج فرمول")}
                    if ('$' in value) NativeMathText(value)
                    ExistingFormulaEditor(
                        source=value,
                        onEdit={occurrence,tex->onFormulaEdit("left",index,occurrence,tex)},
                        onDelete={occurrence->onFormulaDelete("left",index,occurrence)}
                    )
                    SingleImagePicker(question.matchingLeftImages.getOrNull(index),"تصویر چپ ${index+1}"){viewModel.setMatchingImage(question.id,"left",index,it)}
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick={viewModel.moveMatchingItem(question.id,"left",index,-1)},enabled=index>0){Text("↑")}
                        OutlinedButton(onClick={viewModel.moveMatchingItem(question.id,"left",index,1)},enabled=index<question.matchingLeft.lastIndex){Text("↓")}
                        TextButton(onClick={viewModel.removeMatchingSide(question.id,"left",index)},enabled=question.matchingLeft.size>2){Text("حذف")}
                    }
                    Text("پاسخ صحیح این مورد")
                    question.matchingRight.indices.chunked(6).forEach { row ->
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { row.forEach { right ->
                            FilterChip(selected=question.matchingPairs[index]==right,onClick={viewModel.setMatchingPair(question.id,index,right)},label={Text((right+1).toString())})
                        } }
                    }
                }
            }
        }
        Button(onClick={viewModel.addMatchingSide(question.id,"left")},enabled=question.matchingLeft.size<30){Text("افزودن مورد چپ")}
        Text("ستون راست", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        question.matchingRight.forEachIndexed { index, value ->
            Card(Modifier.fillMaxWidth()) {
                Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(value,{viewModel.updateMatchingText(question.id,"right",index,it)},label={Text("راست ${index+1}")},modifier=Modifier.fillMaxWidth())
                    OutlinedButton(onClick={onFormulaEdit("right",index,null,"")}){Text("درج فرمول")}
                    if ('$' in value) NativeMathText(value)
                    ExistingFormulaEditor(
                        source=value,
                        onEdit={occurrence,tex->onFormulaEdit("right",index,occurrence,tex)},
                        onDelete={occurrence->onFormulaDelete("right",index,occurrence)}
                    )
                    SingleImagePicker(question.matchingRightImages.getOrNull(index),"تصویر راست ${index+1}"){viewModel.setMatchingImage(question.id,"right",index,it)}
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick={viewModel.moveMatchingItem(question.id,"right",index,-1)},enabled=index>0){Text("↑")}
                        OutlinedButton(onClick={viewModel.moveMatchingItem(question.id,"right",index,1)},enabled=index<question.matchingRight.lastIndex){Text("↓")}
                        TextButton(onClick={viewModel.removeMatchingSide(question.id,"right",index)},enabled=question.matchingRight.size>2){Text("حذف")}
                    }
                }
            }
        }
        Button(onClick={viewModel.addMatchingSide(question.id,"right")},enabled=question.matchingRight.size<30){Text("افزودن مورد راست")}
    }
}
