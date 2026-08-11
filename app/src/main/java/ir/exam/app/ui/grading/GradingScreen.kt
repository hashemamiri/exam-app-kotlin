package ir.exam.app.ui.grading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.domain.model.AttendanceRow
import ir.exam.app.domain.model.GradingQuestion
import ir.exam.app.domain.model.GradingSubmission
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
fun GradingScreen(viewModel: GradingViewModel = remember { GradingViewModel() }) {
    val state by viewModel.state.collectAsState()
    var showFeedbackDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.actionLoading) CircularProgressIndicator()

        val exam = state.selectedExam
        if (exam == null) {
            Text("تصحیح و نظارت", style = MaterialTheme.typography.headlineSmall)
            if (state.loading) CircularProgressIndicator()
            else if (state.exams.isEmpty()) Text("آزمونی برای تصحیح وجود ندارد.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.exams, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text("${item.subject ?: "بدون درس"} · کد ${item.code ?: "—"}")
                            Button(onClick = { viewModel.selectExam(item.id) }) { Text("ورود به تصحیح") }
                        }
                    }
                }
            }
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::back) { Text("بازگشت") }
            Text(exam.title, style = MaterialTheme.typography.titleLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.mode == "grading", onClick = { viewModel.setMode("grading") }, label = { Text("تصحیح") })
            FilterChip(selected = state.mode == "attendance", onClick = { viewModel.setMode("attendance") }, label = { Text("حضور و غیاب") })
            OutlinedButton(onClick = viewModel::approveAutoGrades) { Text("تأیید خودکار") }
            TextButton(onClick = { showFeedbackDialog = true }) { Text("بازخورد جدید") }
        }

        if (state.mode == "attendance") {
            AttendanceContent(state.attendance, viewModel)
        } else {
            if (state.submissions.isEmpty()) Text("هنوز پاسخی ثبت نشده است.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.submissions, key = { it.id }) { submission ->
                    SubmissionCard(exam.questions, submission, state.edits[submission.id], state.feedbackBank.map { it.text }, viewModel)
                }
            }
        }
    }

    if (showFeedbackDialog) {
        var phrase by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("عبارت بازخورد") },
            text = { OutlinedTextField(phrase, { phrase = it }, label = { Text("متن") }) },
            confirmButton = {
                Button(onClick = { viewModel.addFeedback(phrase); showFeedbackDialog = false }, enabled = phrase.isNotBlank()) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showFeedbackDialog = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun SubmissionCard(
    questions: List<GradingQuestion>,
    submission: GradingSubmission,
    edit: GradingEdit?,
    feedbackPhrases: List<String>,
    viewModel: GradingViewModel
) {
    if (edit == null) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(submission.studentName.ifBlank { "دانش‌آموز" }, style = MaterialTheme.typography.titleMedium)
            Text("تلاش ${submission.attemptNo} · ${if (submission.graded) "تصحیح‌شده" else "در انتظار"}")
            questions.forEachIndexed { index, question ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("سؤال ${index + 1}: ${question.text}")
                        Text("پاسخ: ${submission.responses.getOrNull(index).displayText()}")
                        submission.responseImages[question.id].orEmpty().forEach { url ->
                            AsyncImage(url, "تصویر پاسخ", Modifier.size(160.dp))
                        }
                        OutlinedTextField(
                            value = edit.grades.getOrElse(index) { 0.0 }.toString(),
                            onValueChange = { viewModel.setScore(submission.id, index, it) },
                            label = { Text("نمره از ${question.score}") }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = edit.feedback,
                onValueChange = { viewModel.setFeedback(submission.id, it) },
                label = { Text("بازخورد") },
                modifier = Modifier.fillMaxWidth()
            )
            if (feedbackPhrases.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    feedbackPhrases.take(4).forEach { phrase ->
                        FilterChip(selected = false, onClick = { viewModel.appendFeedback(submission.id, phrase) }, label = { Text(phrase.take(24)) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.save(submission.id) }) { Text("ذخیره نمره") }
                if (submission.graded) TextButton(onClick = { viewModel.unapprove(submission.id) }) { Text("لغو تأیید") }
            }
        }
    }
}

@Composable
private fun AttendanceContent(rows: List<AttendanceRow>, viewModel: GradingViewModel) {
    if (rows.isEmpty()) {
        Text("فهرست حضور خالی است یا مخاطبی برای آزمون تعیین نشده است.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows, key = { it.studentId }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(row.fullName, style = MaterialTheme.typography.titleMedium)
                    Text("وضعیت: ${row.status.faStatus()} · تلاش‌ها: ${row.attempts}/${row.attemptsAllowed}")
                    row.totalGrade?.let { Text("نمره: $it") }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { viewModel.extendTime(row.studentId, 10) }) { Text("۱۰ دقیقه تمدید") }
                        OutlinedButton(onClick = { viewModel.resetAttempt(row.studentId) }) { Text("تلاش مجدد") }
                    }
                }
            }
        }
    }
}

private fun JsonElement?.displayText(): String = when (this) {
    null -> "بدون پاسخ"
    is JsonPrimitive -> contentOrNull.orEmpty().ifBlank { "بدون پاسخ" }
    is JsonArray -> joinToString("، ") { it.displayText() }
    is JsonObject -> entries.joinToString("، ") { (key, value) -> "$key←${value.displayText()}" }
}

private fun String.faStatus(): String = when (lowercase()) {
    "submitted" -> "ارسال‌شده"
    "started", "active" -> "در حال پاسخ"
    "graded" -> "تصحیح‌شده"
    "absent" -> "غایب"
    else -> this
}
