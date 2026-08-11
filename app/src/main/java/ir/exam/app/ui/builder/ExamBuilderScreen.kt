package ir.exam.app.ui.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.ui.image.QuestionMediaEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamBuilderScreen(
    viewModel: ExamBuilderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var typeMenu by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(if (state.examId == null) "ساخت آزمون" else "ویرایش آزمون") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.loading) Box {
                FloatingActionButton(onClick = { typeMenu = true }) { Text("+") }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    QuestionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.faLabel()) },
                            onClick = { viewModel.addQuestion(type); typeMenu = false }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ExamSettingsCard(state, viewModel) }
            item { AudienceCard(state, viewModel) }
            item { QuestionBankCard(state, viewModel) }
            items(state.questions, key = { it.id }) { question ->
                QuestionEditor(question, viewModel)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.examId == null) "ذخیره آزمون" else "ذخیره تغییرات")
                    }
                    if (state.saving) CircularProgressIndicator()
                    state.uploadProgress?.let { Text(it) }
                    state.savedCode?.let { code ->
                        Text("ذخیره شد. کد آزمون: $code", color = MaterialTheme.colorScheme.primary)
                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("بازگشت به آزمون‌ها") }
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun ExamSettingsCard(state: ExamBuilderState, viewModel: ExamBuilderViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("مشخصات آزمون", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(state.title, viewModel::setTitle, label = { Text("عنوان آزمون") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.subject, viewModel::setSubject, label = { Text("درس") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.durationMinutes, viewModel::setDuration, label = { Text("مدت (دقیقه)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.negativeMarking, viewModel::setNegativeMarking, label = { Text("نمره منفی") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.teacherMessage, viewModel::setTeacherMessage, label = { Text("پیام معلم") }, modifier = Modifier.fillMaxWidth())
            ToggleRow("تصادفی‌سازی سؤال‌ها", state.shuffleQuestions, viewModel::setShuffleQuestions)
            ToggleRow("تصادفی‌سازی گزینه‌ها", state.shuffleOptions, viewModel::setShuffleOptions)
            ToggleRow("اتمام تلاش در پایان زمان", state.attemptOnTimeout, viewModel::setAttemptOnTimeout)
            Text("تعداد تلاش مجاز")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { count ->
                    FilterChip(selected = state.attemptsAllowed == count, onClick = { viewModel.setAttempts(count) }, label = { Text(count.toString()) })
                }
            }
            Text("سیاست نمره")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("last" to "آخرین", "best" to "بهترین", "all" to "همه").forEach { (value, label) ->
                    FilterChip(selected = state.gradePolicy == value, onClick = { viewModel.setGradePolicy(value) }, label = { Text(label) })
                }
            }
            OutlinedTextField(state.attemptCooldown, viewModel::setAttemptCooldown, label = { Text("فاصله تلاش‌ها (دقیقه)") })
        }
    }
}

@Composable
private fun AudienceCard(state: ExamBuilderState, viewModel: ExamBuilderViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("مخاطبان آزمون", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all" to "همه", "classes" to "کلاس‌ها", "students" to "دانش‌آموزان").forEach { (value, label) ->
                    FilterChip(selected = state.audienceMode == value, onClick = { viewModel.setAudienceMode(value) }, label = { Text(label) })
                }
            }
            if (state.audienceMode == "classes") {
                state.availableClasses.forEach { item ->
                    SelectionRow(item.name, item.id in state.audienceClasses) { viewModel.toggleAudienceClass(item.id) }
                }
            }
            if (state.audienceMode == "students") {
                state.availableStudents.forEach { item ->
                    SelectionRow(
                        label = item.name + (item.classNames?.takeIf(String::isNotBlank)?.let { " · $it" } ?: ""),
                        selected = item.id in state.audienceStudents
                    ) { viewModel.toggleAudienceStudent(item.id) }
                }
            }
        }
    }
}

@Composable
private fun QuestionBankCard(state: ExamBuilderState, viewModel: ExamBuilderViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("بانک سؤال", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "بستن" else "نمایش (${state.bankQuestions.size})")
                }
            }
            if (state.bankLoading) CircularProgressIndicator()
            if (expanded) {
                if (state.bankQuestions.isEmpty()) Text("بانک سؤال خالی است. از دکمه «ذخیره در بانک» هر سؤال استفاده کنید.")
                state.bankQuestions.forEach { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.question.text.ifBlank { "بدون متن" })
                            Text("${item.subject ?: "بدون درس"} · ${item.question.type.faLabel()}")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { viewModel.addFromBank(item.id) }) { Text("افزودن") }
                                TextButton(onClick = { viewModel.deleteFromBank(item.id) }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionEditor(question: QuestionDraft, viewModel: ExamBuilderViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("نوع: ${question.type.faLabel()}")
            OutlinedTextField(question.text, { viewModel.updateText(question.id, it) }, label = { Text("متن سؤال") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(question.score.toString(), { viewModel.updateScore(question.id, it) }, label = { Text("بارم") })
            QuestionMediaEditor(
                images = question.images,
                onAdd = { uris -> viewModel.addImages(question.id, uris) },
                onMove = { imageId, x, y -> viewModel.moveImage(question.id, imageId, x, y) },
                onRemove = { imageId -> viewModel.removeImage(question.id, imageId) }
            )
            Text("تصویر پاسخ دانش‌آموز")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("no" to "غیرفعال", "optional" to "اختیاری", "required" to "اجباری").forEach { (mode, label) ->
                    FilterChip(
                        selected = question.answerImageMode == mode,
                        onClick = { viewModel.setAnswerImageMode(question.id, mode) },
                        label = { Text(label) }
                    )
                }
            }
            if (question.answerImageMode != "no") {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (1..5).forEach { count ->
                        FilterChip(
                            selected = question.maxAnswerImages == count,
                            onClick = { viewModel.setMaxAnswerImages(question.id, count) },
                            label = { Text(count.toString()) }
                        )
                    }
                }
            }
            when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> question.options.forEachIndexed { index, option ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(question.correctIndex == index, onClick = { viewModel.setCorrect(question.id, index) })
                            OutlinedTextField(
                                option,
                                { viewModel.updateOption(question.id, index, it) },
                                label = { Text("گزینه ${index + 1}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        SingleImagePicker(
                            value = question.optionImages.getOrNull(index),
                            label = "تصویر گزینه ${index + 1}"
                        ) { uri -> viewModel.setOptionImage(question.id, index, uri) }
                    }
                }
                QuestionType.TRUE_FALSE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = question.expectedText == "true", onClick = { viewModel.setTrueFalse(question.id, true) }, label = { Text("صحیح") })
                    FilterChip(selected = question.expectedText == "false", onClick = { viewModel.setTrueFalse(question.id, false) }, label = { Text("غلط") })
                }
                QuestionType.FILL_BLANK -> OutlinedTextField(question.expectedText, { viewModel.updateExpectedText(question.id, it) }, label = { Text("پاسخ‌های قابل قبول با |") })
                QuestionType.NUMERIC -> {
                    OutlinedTextField(question.expectedNumber, { viewModel.updateExpectedNumber(question.id, it) }, label = { Text("پاسخ عددی") })
                    OutlinedTextField(question.tolerance, { viewModel.updateTolerance(question.id, it) }, label = { Text("تلورانس") })
                }
                QuestionType.MATCHING -> MatchingQuestionEditor(question, viewModel)
                QuestionType.ESSAY -> Unit
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.saveToBank(question.id) }) { Text("ذخیره در بانک") }
                TextButton(onClick = { viewModel.remove(question.id) }) { Text("حذف سؤال") }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SelectionRow(label: String, selected: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Text(label)
    }
}

private fun QuestionType.faLabel(): String = when (this) {
    QuestionType.ESSAY -> "تشریحی"
    QuestionType.MULTIPLE_CHOICE -> "چهارگزینه‌ای"
    QuestionType.TRUE_FALSE -> "صحیح/غلط"
    QuestionType.FILL_BLANK -> "جای خالی"
    QuestionType.NUMERIC -> "عددی"
    QuestionType.MATCHING -> "جورکردنی"
}
