package ir.exam.app.ui.student

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.MatchingQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.NumericQuestion
import ir.exam.app.domain.model.StudentAnswer
import ir.exam.app.domain.model.TextAnswer
import ir.exam.app.domain.model.TrueFalseQuestion
import ir.exam.app.ui.math.NativeMathText

@Composable
fun StudentExamContent(
    state: StudentExamUiState,
    onAnswer: (StudentAnswer) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAddImages: (String, List<String>) -> Unit,
    onRemoveImage: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit
) {
    val exam = state.exam ?: return
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, state.finished) {
        if (!state.finished) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    if (state.finished) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (state.queued) "پاسخ شما در صف امن ارسال است." else "پاسخ شما با موفقیت ثبت شد.",
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.queued) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Text("کد آزمون: ${exam.code}")
            state.submissionMessage?.let { Text(it) }
            if (state.queued) {
                Text("تا ارسال موفق، پیش‌نویس و تصاویر از دستگاه حذف نمی‌شوند.")
            }
            Button(onClick = onDone) { Text("بازگشت به داشبورد") }
        }
        return
    }
    val question = exam.questions.getOrNull(state.questionIndex) ?: return

    Scaffold(
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("زمان: ${state.remainingSeconds / 60}:${(state.remainingSeconds % 60).toString().padStart(2, '0')}")
                    Button(onClick = onSubmit, enabled = !state.submitting) {
                        Text(if (state.submitting) "در حال ارسال..." else "ارسال نهایی")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onPrevious, enabled = state.questionIndex > 0) { Text("قبلی") }
                    Button(onClick = onNext, enabled = state.questionIndex < exam.questions.lastIndex) { Text("بعدی") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(exam.title, style = MaterialTheme.typography.titleLarge) }
            item { Text("سؤال ${state.questionIndex + 1} از ${exam.questions.size} · بارم ${question.score}") }
            item {
                NativeMathText(
                    question.text,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                )
            }
            items(question.images) { url -> AsyncImage(url, "تصویر سؤال", Modifier.fillMaxWidth()) }
            item {
                when (question) {
                    is EssayQuestion -> OutlinedTextField(
                        value = (state.answers[question.id] as? TextAnswer)?.value.orEmpty(),
                        onValueChange = { onAnswer(TextAnswer(question.id, it)) },
                        label = { Text("پاسخ شما") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    is FillBlankQuestion -> OutlinedTextField(
                        value = (state.answers[question.id] as? TextAnswer)?.value.orEmpty(),
                        onValueChange = { onAnswer(TextAnswer(question.id, it)) },
                        label = { Text("پاسخ جای خالی") }
                    )
                    is NumericQuestion -> OutlinedTextField(
                        value = (state.answers[question.id] as? TextAnswer)?.value.orEmpty(),
                        onValueChange = { value -> onAnswer(TextAnswer(question.id, value.filter { it.isDigit() || it in ".-" })) },
                        label = { Text("پاسخ عددی") }
                    )
                    is MultipleChoiceQuestion -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEachIndexed { index, option ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = (state.answers[question.id] as? ChoiceAnswer)?.selectedIndex == index,
                                        onClick = { onAnswer(ChoiceAnswer(question.id, index)) }
                                    )
                                    Column {
                                        NativeMathText(option)
                                        question.optionImages.getOrNull(index)?.let { AsyncImage(it, "تصویر گزینه", Modifier.size(120.dp)) }
                                    }
                                }
                            }
                        }
                    }
                    is TrueFalseQuestion -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val current = (state.answers[question.id] as? BooleanAnswer)?.value
                        FilterChip(selected = current == true, onClick = { onAnswer(BooleanAnswer(question.id, true)) }, label = { Text("صحیح") })
                        FilterChip(selected = current == false, onClick = { onAnswer(BooleanAnswer(question.id, false)) }, label = { Text("غلط") })
                    }
                    is MatchingQuestion -> MatchingStudentAnswer(
                        question,
                        state.answers[question.id] as? MatchingAnswer,
                        onAnswer
                    )
                }
            }
            if (question.maxAnswerImages > 0) {
                item {
                    ResponseImages(
                        questionId = question.id,
                        max = question.maxAnswerImages,
                        values = state.responseImages[question.id].orEmpty(),
                        onAdd = onAddImages,
                        onRemove = onRemoveImage
                    )
                }
            }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
}

@Composable
private fun MatchingStudentAnswer(
    question: MatchingQuestion,
    answer: MatchingAnswer?,
    onAnswer: (StudentAnswer) -> Unit
) {
    val current = answer?.pairs.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.leftItems.forEachIndexed { leftIndex, left ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NativeMathText("${leftIndex + 1}. $left")
                    question.leftImages.getOrNull(leftIndex)?.let { AsyncImage(it, "تصویر جورکردنی", Modifier.size(100.dp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        question.rightItems.indices.forEach { rightIndex ->
                            FilterChip(
                                selected = current[leftIndex] == rightIndex,
                                onClick = { onAnswer(MatchingAnswer(question.id, current + (leftIndex to rightIndex))) },
                                label = { Text((rightIndex + 1).toString()) }
                            )
                        }
                    }
                }
            }
        }
        Text("ستون راست:")
        question.rightItems.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                NativeMathText("${index + 1}. $value")
                question.rightImages.getOrNull(index)?.let { AsyncImage(it, "تصویر ستون راست", Modifier.size(80.dp)) }
            }
        }
    }
}

@Composable
private fun ResponseImages(
    questionId: String,
    max: Int,
    values: List<String>,
    onAdd: (String, List<String>) -> Unit,
    onRemove: (String, String) -> Unit
) {
    val context = LocalContext.current
    fun persist(uri: Uri) {
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            persist(uri)
            onAdd(questionId, listOf(uri.toString()))
        }
    }
    val multiplePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(max.coerceAtLeast(2))
    ) { uris ->
        uris.forEach(::persist)
        onAdd(questionId, uris.map(Uri::toString))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("تصاویر پاسخ (${values.size} از $max)")
        Button(
            enabled = values.size < max,
            onClick = {
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                if (max - values.size <= 1) singlePicker.launch(request) else multiplePicker.launch(request)
            }
        ) { Text("افزودن تصویر پاسخ") }
        values.forEach { uri ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(uri, "تصویر پاسخ", Modifier.size(100.dp))
                OutlinedButton(onClick = { onRemove(questionId, uri) }) { Text("حذف") }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
