package ir.exam.app.ui.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamBuilderScreen(viewModel: ExamBuilderViewModel) {
    val state by viewModel.state.collectAsState()
    var typeMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("ساخت آزمون") }) },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { typeMenu = true }) { Text("+") }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    QuestionType.entries.forEach { type -> DropdownMenuItem(text = { Text(type.name) }, onClick = { viewModel.addQuestion(type); typeMenu = false }) }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(state.title, viewModel::setTitle, label = { Text("عنوان آزمون") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(state.subject, viewModel::setSubject, label = { Text("درس") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(state.durationMinutes, viewModel::setDuration, label = { Text("مدت (دقیقه)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) { Text("ذخیره آزمون") }
                    if (state.saving) CircularProgressIndicator()
                    state.savedCode?.let { Text("آزمون ذخیره شد. کد آزمون: $it") }
                    state.error?.let { Text(it) }
                }
            }
            items(state.questions, key = { it.id }) { question -> QuestionEditor(question, viewModel) }
        }
    }
}

@Composable
private fun QuestionEditor(question: QuestionDraft, viewModel: ExamBuilderViewModel) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("نوع: ${question.type}")
            OutlinedTextField(question.text, { viewModel.updateText(question.id, it) }, label = { Text("متن سؤال") }, modifier = Modifier.fillMaxWidth())
            when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> question.options.forEachIndexed { index, option ->
                    Row { RadioButton(question.correctIndex == index, onClick = { viewModel.setCorrect(question.id, index) }); OutlinedTextField(option, { viewModel.updateOption(question.id, index, it) }, label = { Text("گزینه ${index + 1}") }) }
                }
                QuestionType.FILL_BLANK -> OutlinedTextField(question.expectedText, { viewModel.updateExpectedText(question.id, it) }, label = { Text("پاسخ قابل قبول") })
                QuestionType.NUMERIC -> {
                    OutlinedTextField(question.expectedNumber, { viewModel.updateExpectedNumber(question.id, it) }, label = { Text("پاسخ عددی") })
                    OutlinedTextField(question.tolerance, { viewModel.updateTolerance(question.id, it) }, label = { Text("تلورانس") })
                }
                else -> Unit
            }
            TextButton(onClick = { viewModel.remove(question.id) }) { Text("حذف سؤال") }
        }
    }
}
