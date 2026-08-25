package ir.exam.app.ui.grading

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import ir.exam.app.ui.math.NativeMathText
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun GradingScreen(
    initialPendingOnly: Boolean = false,
    initialGradedOnly: Boolean = false,
    viewModel: GradingViewModel = remember { GradingViewModel() }
) {
    val state by viewModel.state.collectAsState()
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var editingFeedback by remember { mutableStateOf<ir.exam.app.domain.model.FeedbackPhrase?>(null) }
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(initialPendingOnly, initialGradedOnly) {
        viewModel.setPendingOnly(initialPendingOnly)
        viewModel.setGradedOnly(initialGradedOnly)
    }

    // V58.0 — پنجرهٔ گزارش‌های نظارتی آزمون (لیست دانش‌آموزان + رویدادها).
    if (state.monitorExamId != null) {
        MonitorReportsDialog(
            reports = state.monitorReports,
            onDismiss = viewModel::closeMonitorReports
        )
    }

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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.selectExam(item.id) }) { Text("ورود به تصحیح") }
                                // V58.0 — گزارش‌های نظارتی: رویدادهای امنیتی و زمان‌بندی هر دانش‌آموز.
                                OutlinedButton(onClick = { viewModel.openMonitorReports(item.id) }) { Text("گزارش‌ها") }
                            }
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
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = state.mode == "grading", onClick = { viewModel.setMode("grading") }, label = { Text("دانش‌آموزمحور") })
            FilterChip(selected = state.mode == "question", onClick = { viewModel.setMode("question") }, label = { Text("سؤال‌محور") })
            FilterChip(selected = state.mode == "attendance", onClick = { viewModel.setMode("attendance") }, label = { Text("حضور") })
            FilterChip(selected = state.pendingOnly, onClick = { viewModel.setPendingOnly(!state.pendingOnly) }, label = { Text("فقط مانده") })
            FilterChip(selected = state.gradedOnly, onClick = { viewModel.setGradedOnly(!state.gradedOnly) }, label = { Text("فقط پاسخ") })
            OutlinedButton(onClick = viewModel::approveAutoGrades) { Text("تأیید خودکار") }
            TextButton(onClick = { showFeedbackDialog = true }) { Text("بازخورد جدید") }
        }

        if(state.feedbackBank.isNotEmpty()) Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            state.feedbackBank.take(6).forEach { phrase -> FilterChip(selected=false,onClick={editingFeedback=phrase},label={Text(phrase.text.take(18))}) }
        }
        if (state.mode == "attendance") {
            AttendanceContent(state.attendance,state.liveStatus, viewModel)
        } else if (state.mode == "question") {
            QuestionCentricContent(state, viewModel)
        } else {
            val visibleSubmissions = when {
                state.pendingOnly -> state.submissions.filterNot { it.graded }
                state.gradedOnly -> state.submissions.filter { it.graded }
                else -> state.submissions
            }
            if (visibleSubmissions.isEmpty()) {
                Text(
                    when {
                        state.pendingOnly -> "پاسخ تصحیح‌نشده‌ای باقی نمانده است."
                        state.gradedOnly -> "پاسخ تصحیح‌شده‌ای ثبت نشده است."
                        else -> "هنوز پاسخی ثبت نشده است."
                    }
                )
            }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visibleSubmissions, key = { it.id }) { submission ->
                    SubmissionCard(
                        exam.questions,
                        submission,
                        state.edits[submission.id],
                        state.feedbackBank.map { it.text },
                        state.scoreInputs,
                        state.scoreErrors,
                        viewModel
                    )
                }
            }
        }
    }

    editingFeedback?.let { original ->
        var phrase by remember(original.id){mutableStateOf(original.text)}
        AlertDialog(onDismissRequest={editingFeedback=null},title={Text("ویرایش بازخورد")},text={OutlinedTextField(phrase,{phrase=it.take(1000)},label={Text("متن")})},
            confirmButton={Column{Button(onClick={viewModel.updateFeedbackPhrase(original.id,phrase);editingFeedback=null},enabled=phrase.isNotBlank()){Text("ذخیره")};TextButton(onClick={viewModel.deleteFeedbackPhrase(original.id);editingFeedback=null}){Text("حذف عبارت")}}},
            dismissButton={TextButton(onClick={editingFeedback=null}){Text("انصراف")}})
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
    scoreInputs: Map<String, String>,
    scoreErrors: Set<String>,
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
                        NativeMathText("سؤال ${index + 1}: ${question.text}")
                        Text("پاسخ: ${submission.responses.getOrNull(index).displayText()}")
                        submission.responseImages[question.id].orEmpty().forEach { url ->
                            AsyncImage(url, "تصویر پاسخ", Modifier.size(160.dp))
                        }
                        val scoreKey = "${submission.id}:$index"
                        OutlinedTextField(
                            value = scoreInputs[scoreKey] ?: edit.grades.getOrElse(index) { 0.0 }.toString(),
                            onValueChange = { viewModel.setScore(submission.id, index, it) },
                            isError = scoreKey in scoreErrors,
                            supportingText = if (scoreKey in scoreErrors) ({ Text("نمره باید بین صفر و ${question.score} باشد.") }) else null,
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
private fun QuestionCentricContent(state: GradingUiState, viewModel: GradingViewModel) {
    val exam = state.selectedExam ?: return
    val index = state.selectedQuestionIndex.coerceIn(0, exam.questions.lastIndex.coerceAtLeast(0))
    val question = exam.questions.getOrNull(index) ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.moveQuestion(-1) }, enabled = index > 0) { Text("سؤال قبل") }
                    Text("سؤال ${index + 1} از ${exam.questions.size} · بارم ${question.score}")
                    OutlinedButton(onClick = { viewModel.moveQuestion(1) }, enabled = index < exam.questions.lastIndex) { Text("سؤال بعد") }
                }
                NativeMathText(question.text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = state.submissions.isNotEmpty() && !state.actionLoading,
                        onClick = viewModel::saveCurrentQuestionForAll
                    ) { Text("ثبت اتمیک همه نمره‌ها") }
                    OutlinedButton(
                        enabled = state.submissions.isNotEmpty() && !state.actionLoading,
                        onClick = viewModel::finalizeAllGrades
                    ) { Text("نهایی‌سازی کل آزمون") }
                }
                Text("اگر حتی یک نمره نامعتبر باشد، هیچ‌کدام ذخیره نمی‌شود.")
            }
        }
        if (state.submissions.isEmpty()) Text("هنوز پاسخی ثبت نشده است.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.submissions, key = { it.id }) { submission ->
                val edit = state.edits[submission.id]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(submission.studentName, style = MaterialTheme.typography.titleMedium)
                        Text("پاسخ: ${submission.responses.getOrNull(index).displayText()}")
                        submission.responseImages[question.id].orEmpty().forEach { url ->
                            AsyncImage(url, "تصویر پاسخ", Modifier.size(150.dp))
                        }
                        val scoreKey = "${submission.id}:$index"
                        OutlinedTextField(
                            value = state.scoreInputs[scoreKey] ?: edit?.grades?.getOrNull(index)?.toString().orEmpty(),
                            onValueChange = { viewModel.setScore(submission.id, index, it) },
                            isError = scoreKey in state.scoreErrors,
                            supportingText = if (scoreKey in state.scoreErrors) ({ Text("نمره نامعتبر است.") }) else null,
                            label = { Text("نمره از ${question.score}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceContent(rows: List<AttendanceRow>, live:JsonObject?,viewModel: GradingViewModel) {
    val summary=live?.get("summary") as? JsonObject
    if(summary!=null) Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        listOf("in_progress" to "در حال آزمون","submitted" to "تحویل","not_started" to "شروع‌نشده").forEach { (key,label) ->
            Card(Modifier.weight(1f)){Column(Modifier.padding(7.dp)){Text(summary[key]?.jsonPrimitive?.contentOrNull?:"0");Text(label)}}
        }
    }
    Text("وضعیت زنده هر ۲۰ ثانیه خودکار تازه می‌شود.")
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

/**
 * V58.0 — پنجرهٔ گزارش‌های نظارتی آزمون: هر دانش‌آموز با رویدادهای امنیتی
 * (تلاش اسکرین‌شات/ضبط، خروج از برنامه و...)، زمان ورود/خروج، مدت پاسخ‌گویی
 * به هر سؤال و تعداد بازدید سؤال‌ها.
 */
@Composable
private fun MonitorReportsDialog(reports: JsonObject?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("گزارش‌های آزمون") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } },
        text = {
            val rows = (reports?.get("rows") as? JsonArray).orEmpty()
            if (rows.isEmpty()) {
                Text(
                    "هنوز گزارشی برای این آزمون ثبت نشده است.\n" +
                        "گزارش از زمان شرکت دانش‌آموز با نسخهٔ جدید برنامه ساخته می‌شود؛ " +
                        "آزمون‌هایی که پیش از این نسخه برگزار شده‌اند گزارش ندارند."
                )
            } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rows.size) { index ->
                    val row = rows[index] as? JsonObject ?: return@items
                    val report = row["report"] as? JsonObject
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                (row["student_name"] as? JsonPrimitive)?.contentOrNull ?: "دانش‌آموز",
                                style = MaterialTheme.typography.titleSmall
                            )
                            val events = report?.get("events") as? JsonObject
                            if (events.isNullOrEmpty()) {
                                Text("رویداد مشکوکی ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                            } else events.forEach { (kind, count) ->
                                Text(
                                    "${kind.faMonitorEvent()}: ${(count as? JsonPrimitive)?.contentOrNull ?: "?"} بار",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            (report?.get("entered_at_epoch_ms") as? JsonPrimitive)?.contentOrNull
                                ?.toLongOrNull()?.takeIf { it > 0 }?.let {
                                    Text("زمان ورود: ${it.faClock()}", style = MaterialTheme.typography.bodySmall)
                                }
                            (report?.get("left_at_epoch_ms") as? JsonPrimitive)?.contentOrNull
                                ?.toLongOrNull()?.takeIf { it > 0 }?.let {
                                    Text("زمان خروج: ${it.faClock()}", style = MaterialTheme.typography.bodySmall)
                                }
                            val times = report?.get("question_time_ms") as? JsonObject
                            val visits = report?.get("question_visits") as? JsonObject
                            if (!times.isNullOrEmpty()) {
                                Text("مدت پاسخ‌گویی هر سؤال:", style = MaterialTheme.typography.bodySmall)
                                times.entries.forEachIndexed { qIndex, (qid, ms) ->
                                    val seconds = ((ms as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L) / 1000L
                                    val visitCount = (visits?.get(qid) as? JsonPrimitive)?.contentOrNull ?: "1"
                                    Text(
                                        "سؤال ${qIndex + 1}: $seconds ثانیه · $visitCount بازدید",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

private fun String.faMonitorEvent(): String = when (this) {
    "screenshot_attempt" -> "تلاش برای اسکرین‌شات"
    "screen_record_attempt" -> "تلاش برای ضبط صفحه"
    "app_leave" -> "خارج شدن از برنامه"
    "app_close" -> "بستن برنامه"
    "exam_screen_leave" -> "خارج شدن از صفحه آزمون"
    else -> this
}

private fun Long.faClock(): String {
    val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
    return formatter.format(java.util.Date(this))
}
