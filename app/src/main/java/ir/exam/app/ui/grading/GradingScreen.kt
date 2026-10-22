package ir.exam.app.ui.grading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import ir.exam.app.core.calendar.PersianDigits
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.ui.audio.AnswerAudioPlayer
import ir.exam.app.ui.audio.isAnswerAudioUrl
import ir.exam.app.domain.model.AttendanceRow
import ir.exam.app.domain.model.GradingQuestion
import ir.exam.app.domain.model.GradingSubmission
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    initialMode: String = "grading",
    viewModel: GradingViewModel = remember { GradingViewModel() }
) {
    val state by viewModel.state.collectAsState()
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var editingFeedback by remember { mutableStateOf<ir.exam.app.domain.model.FeedbackPhrase?>(null) }
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(initialPendingOnly, initialGradedOnly, initialMode) {
        viewModel.setPendingOnly(initialPendingOnly)
        viewModel.setGradedOnly(initialGradedOnly)
        if (initialMode in setOf("grading", "question", "attendance")) {
            viewModel.setMode(initialMode)
        }
    }

    // V58.0 — پنجرهٔ گزارش‌های نظارتی آزمون (لیست دانش‌آموزان + رویدادها).
    if (state.monitorExamId != null) {
        MonitorReportsDialog(
            reports = state.monitorReports,
            onDismiss = viewModel::closeMonitorReports
        )
    }

    // V280 — کدام کارت آزمون باز است (دکمه‌ها فقط روی کارت لمس‌شده)
    var expandedCardId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.actionLoading) CircularProgressIndicator()

        val exam = state.selectedExam
        if (exam == null) {
            if (initialMode == "feedback") {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("بانک بازخورد معلم", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { showFeedbackDialog = true }) { Text("بازخورد جدید") }
                        }
                        if (state.feedbackBank.isEmpty()) {
                            Text("هنوز عبارت بازخوردی ثبت نکرده‌اید.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.feedbackBank.forEach { phrase ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { editingFeedback = phrase },
                                        label = { Text(phrase.text) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // V280 — توضیح کوتاه به هدر برنامه منتقل شد (ExamApp › gradingHint)؛ داخل صفحه هیچ عنوانی نیست.
            val visibleExams = if (state.pendingOnly) {
                // «مانده» فقط آزمون‌هایی را نشان می‌دهد که پاسخِ تصحیح‌نشده دارند (تا آمار نرسیده، همه می‌مانند).
                state.exams.filter { ex -> state.cardStats[ex.id]?.let { it.pending > 0 } ?: true }
            } else state.exams
            if (state.loading) CircularProgressIndicator()
            else if (visibleExams.isEmpty()) Text(if (state.pendingOnly) "پاسخِ در انتظارِ تصحیحی نیست." else "آزمونی برای تصحیح وجود ندارد.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleExams, key = { it.id }) { item ->
                    val st = state.cardStats[item.id]
                    // V280 — کارت بدون دکمه؛ لمس کارت دکمه‌های «ورود به تصحیح/گزارش‌ها» را باز می‌کند
                    val expanded = expandedCardId == item.id
                    // رنگِ کادر: پاسخ → نسبتِ پاسخ‌داده‌ها به کلِ دانش‌آموزان؛ تصحیح → نسبتِ تصحیح‌شده‌ها به پاسخ‌ها
                    val level = when {
                        st == null -> null
                        state.gradedOnly -> cardLevel(st.answered, if (st.totalStudents > 0) st.totalStudents else st.answered)
                        state.pendingOnly -> cardLevel(st.graded, st.answered)
                        else -> cardLevel(st.graded, st.answered)
                    }
                    val border = when (level) {
                        CardLevel.FULL -> Color(0xFF16A34A)
                        CardLevel.HIGH -> Color(0xFFEAB308)
                        CardLevel.LOW -> Color(0xFFF97316)
                        CardLevel.NONE -> Color(0xFFDC2626)
                        null -> null
                    }
                    Card(
                        Modifier.fillMaxWidth().clickable { expandedCardId = if (expanded) null else item.id },
                        border = border?.let { BorderStroke(2.dp, it) }
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text("${item.subject ?: "بدون درس"} · کد ${item.code ?: "—"}")
                            if (st != null) {
                                Text(
                                    if (state.gradedOnly) "پاسخ داده: ${st.answered}" + (if (st.totalStudents > 0) " از ${st.totalStudents} دانش‌آموز" else "")
                                    else "تصحیح‌شده: ${st.graded} از ${st.answered} پاسخ · مانده: ${st.pending}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (expanded) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        state.partScoreErrors,
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
    partScoreErrors: Set<String>,
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
                        if (question.sectionTitle.isNotBlank()) Text(question.sectionTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        NativeMathText("سؤال ${index + 1}: ${question.text}")
                        val rawElement = submission.responses.getOrNull(index)
                        val rawJsonString = (rawElement as? JsonPrimitive)?.contentOrNull ?: rawElement?.toString().orEmpty()
                        val hasGeometrySpec = question.constructionTargets.isNotEmpty() || question.constructionConstraints.isNotEmpty()
                        val geometryEval = if (hasGeometrySpec) {
                            ir.exam.app.core.geometry.GeometryConstructionEvaluator.evaluate(
                                questionScore = question.score,
                                targets = question.constructionTargets.map { ir.exam.app.core.geometry.GeometryConstructionEvaluator.TargetSpec(it.label, it.x, it.y, it.tolerance, it.score) },
                                constraints = question.constructionConstraints.map { ir.exam.app.core.geometry.GeometryConstructionEvaluator.ConstraintSpec(it.id, it.type, it.points, it.value, it.tolerance, it.score) },
                                rawResponse = rawJsonString
                            )
                        } else null
                        if (hasGeometrySpec) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(question.constructionPrompt.ifBlank { "نقاط و شرط‌های هندسی" }, fontWeight = FontWeight.Bold)
                                question.constructionTargets.forEach { target -> Text("${target.label}: (${target.x}, ${target.y}) ± ${target.tolerance} · بارم ${target.score}", style = MaterialTheme.typography.bodySmall) }
                                question.constructionConstraints.forEach { c ->
                                    Text(ir.exam.app.core.geometry.GeometryConstructionEvaluator.formatConstraintSummary(
                                        ir.exam.app.core.geometry.GeometryConstructionEvaluator.ConstraintSpec(c.id, c.type, c.points, c.value, c.tolerance, c.score)
                                    ), style = MaterialTheme.typography.bodySmall)
                                }
                                geometryEval?.checks?.forEach { chk ->
                                    Text("${if (chk.passed) "✓" else "✗"} ${chk.label}: ${chk.detail}", style = MaterialTheme.typography.bodySmall, color = if (chk.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                }
                                if (geometryEval != null && question.assessmentParts.isEmpty()) {
                                    TextButton(onClick = { viewModel.setScore(submission.id, index, geometryEval.score.toString()) }) {
                                        Text("⚡ اعمال نمرهٔ پیشنهادی هندسی (${geometryEval.score})")
                                    }
                                }
                            }
                        }
                        if (question.rubric.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("راهنمای نمره‌دهی دبیر", fontWeight = FontWeight.Bold)
                                NativeMathText(question.rubric)
                            }
                        }
                        if (question.answerMode == "proof" && question.assessmentParts.isNotEmpty()) {
                            val responseText = submission.responses.getOrNull(index).displayText()
                            if (question.proofAssumptions.isNotBlank()) Text("فرض‌ها: ${question.proofAssumptions}")
                            if (question.proofGoal.isNotBlank()) Text("خواسته: ${question.proofGoal}")
                            question.assessmentParts.forEachIndexed { proofIndex, proofPart ->
                                Text("گام ${proofPart.label.ifBlank { (proofIndex + 1).toString() }} — ${proofPart.prompt}", fontWeight = FontWeight.SemiBold)
                                Text("نتیجه: ${proofPartAnswer(responseText, proofPart.id, "answer")}")
                                if (proofPart.requiresReason) Text("دلیل: ${proofPartAnswer(responseText, proofPart.id, "reason")}")
                            }
                        } else if (question.answerMode == "construct" || hasGeometrySpec) {
                            Text("پاسخ هندسی: ${ir.exam.app.core.geometry.GeometryConstructionEvaluator.formatResponseSummary(rawJsonString, question.constructionLabels)}")
                        } else Text("پاسخ: ${submission.responses.getOrNull(index).displayText()}")
                        AnswerImagesRow(submission.answerImagesFor(question.id, index))
                        val scoreKey = "${submission.id}:$index"
                        question.assessmentParts.forEachIndexed { partIndex, part ->
                            val partKey = "${submission.id}:$index:$partIndex"
                            Text("${part.label}: ${part.prompt} · سقف ${part.score}", style = MaterialTheme.typography.bodySmall)
                            if (part.rubric.isNotBlank()) Text("معیار این گام: ${part.rubric}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = scoreInputs[partKey] ?: edit.partGrades["$index:$partIndex"]?.toString().orEmpty(),
                                onValueChange = { viewModel.setPartScore(submission.id, index, partIndex, it) },
                                isError = partKey in partScoreErrors,
                                supportingText = if (partKey in partScoreErrors) ({ Text("بارم این زیربخش ${part.score} است؛ جمع زیربخش‌ها نباید از بارم سؤال بیشتر شود.") }) else null,
                                label = { Text("نمرهٔ ${part.label} از ${part.score}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = edit.partFeedback["$index:$partIndex"].orEmpty(),
                                onValueChange = { viewModel.setPartFeedback(submission.id, index, partIndex, it) },
                                label = { Text("بازخورد ${part.label}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (question.assessmentParts.isNotEmpty()) {
                            Text("جمع نمرهٔ زیربخش‌ها: ${edit.grades.getOrElse(index) { 0.0 }} از ${question.score}", fontWeight = FontWeight.Bold)
                        } else OutlinedTextField(
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
            // V314.0 — مهرها و برچسب‌های تشویقی تصویری ویژهٔ دورهٔ ابتدایی
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(
                    "🌟 آفرین قهرمان! خیلی خوب درخشیدی",
                    "🏆 ستارهٔ طلایی برای دقت و تلاش عالی",
                    "🎨 پاسخ و ترسیم بسیار تمیز و خلاقانه",
                    "🦋 تلاش بسیار خوب؛ همین مسیر زیبا را ادامه بده",
                    "📚 پیشرفت عالی نسبت به آزمون‌های قبل!",
                    "💪 با کمی تمرین بیشتر عالی می‌شوی"
                ).forEach { stamp ->
                    FilterChip(selected = false, onClick = { viewModel.appendFeedback(submission.id, stamp) }, label = { Text(stamp.take(22)) })
                }
            }
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
    // V277 — چیدمان مرتب: نوار پیمایش (قبلی | «سؤال ۳ از ۱۰ · بارم ۲» | بعدی) با آیکن جهت‌دار،
    // متن سؤال، دکمه‌های عملیات تمام‌عرض، و کارت هر دانش‌آموز با نام و نمره در یک سطر.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { viewModel.moveQuestion(-1) }, enabled = index > 0) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "سؤال قبل") // RTL: رو به راست
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سؤال ${PersianDigits.convert(index + 1)} از ${PersianDigits.convert(exam.questions.size)}", style = MaterialTheme.typography.titleMedium)
                        Text("بارم ${PersianDigits.convert(question.score.toString())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick = { viewModel.moveQuestion(1) }, enabled = index < exam.questions.lastIndex) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "سؤال بعد") // RTL: رو به چپ
                    }
                }
                HorizontalDivider()
                if (question.sectionTitle.isNotBlank()) Text(question.sectionTitle, style = MaterialTheme.typography.labelLarge)
                NativeMathText(question.text)
                if (question.rubric.isNotBlank()) Text("راهنمای دبیر: ${question.rubric}", color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                Button(
                    enabled = state.submissions.isNotEmpty() && !state.actionLoading,
                    onClick = viewModel::saveCurrentQuestionForAll,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ثبت نمره‌های این سؤال برای همه") }
                OutlinedButton(
                    enabled = state.submissions.isNotEmpty() && !state.actionLoading,
                    onClick = viewModel::finalizeAllGrades,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("نهایی‌سازی کل آزمون") }
                Text("اگر حتی یک نمره نامعتبر باشد، هیچ‌کدام ذخیره نمی‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.submissions.isEmpty()) Text("هنوز پاسخی ثبت نشده است.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.submissions, key = { it.id }) { submission ->
                val edit = state.edits[submission.id]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // V255 — پیک تصادفی: این سؤال برای این دانش‌آموز نمایش داده نشده است.
                        if (!submission.isShown(index)) {
                            Text(submission.studentName, style = MaterialTheme.typography.titleSmall)
                            Text("این سؤال برای این دانش‌آموز نمایش داده نشده است (پیک تصادفی).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            return@Column
                        }
                        val scoreKey = "${submission.id}:$index"
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(submission.studentName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            OutlinedTextField(
                                value = state.scoreInputs[scoreKey] ?: edit?.grades?.getOrNull(index)?.toString().orEmpty(),
                                onValueChange = { viewModel.setScore(submission.id, index, it) },
                                isError = scoreKey in state.scoreErrors,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                label = { Text("از ${PersianDigits.convert(question.score.toString())}") },
                                modifier = Modifier.width(112.dp)
                            )
                        }
                        if (scoreKey in state.scoreErrors) Text("نمره باید بین صفر و ${PersianDigits.convert(question.score.toString())} باشد.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Text("پاسخ: ${submission.responses.getOrNull(index).displayText()}", style = MaterialTheme.typography.bodyMedium)
                        AnswerImagesRow(submission.answerImagesFor(question.id, index))
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

private fun proofPartAnswer(raw: String, id: String, field: String): String = runCatching {
    Json.parseToJsonElement(raw).jsonObject["parts"]?.jsonObject?.get(id)?.jsonObject?.get(field)?.jsonPrimitive?.contentOrNull.orEmpty()
}.getOrDefault("")

private fun JsonElement?.displayText(): String = when (this) {
    null -> "بدون پاسخ"
    is JsonPrimitive -> contentOrNull.orEmpty().ifBlank { "بدون پاسخ" }
    is JsonArray -> joinToString("، ") { it.displayText() }
    is JsonObject -> entries.joinToString("، ") { (key, value) -> "$key←${value.displayText()}" }
}

private fun String.faStatus(): String = when (lowercase()) {
    "submitted" -> "ارسال‌شده"
    "started", "active", "in_progress" -> "در حال پاسخ"
    "graded" -> "تصحیح‌شده"
    "expired" -> "منقضی"
    "not_started" -> "شروع‌نشده"
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
    // V137 — پنجرهٔ گزارش‌ها تمام‌صفحه شد (فهرست دانش‌آموزان و گزارش هر دانش‌آموز).
    val rows = (reports?.get("rows") as? JsonArray).orEmpty()
    var selected by remember(reports) { mutableStateOf<Int?>(null) }
    val selectedRow = selected?.let { rows.getOrNull(it) as? JsonObject }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (selectedRow != null) selected = null else onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (selectedRow == null) "گزارش‌های آزمون"
                        else (selectedRow["student_name"] as? JsonPrimitive)?.contentOrNull ?: "دانش‌آموز",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedRow != null) {
                        TextButton(onClick = { selected = null }) { Text("→ بازگشت به لیست") }
                    }
                    TextButton(onClick = onDismiss) { Text("بستن", color = MaterialTheme.colorScheme.error) }
                }
                if (rows.isEmpty()) {
                    Text(
                        "هنوز گزارشی برای این آزمون ثبت نشده است.\n" +
                            "گزارش از زمان شرکت دانش‌آموز با نسخهٔ جدید برنامه ساخته می‌شود؛ " +
                            "آزمون‌هایی که پیش از این نسخه برگزار شده‌اند گزارش ندارند."
                    )
                } else if (selectedRow == null) {
                    // V59.0 — کارت‌های رنگی دانش‌آموزان (سبز→زرد→نارنجی→قرمز بر اساس شدت تخلف).
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rows.size) { index ->
                            val row = rows[index] as? JsonObject ?: return@items
                            val report = row["report"] as? JsonObject
                            val score = monitorViolationScore(report)
                            Card(
                                Modifier.fillMaxWidth().clickable { selected = index },
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = monitorSeverityColor(score)
                                )
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            (row["student_name"] as? JsonPrimitive)?.contentOrNull ?: "دانش‌آموز",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            if (score == 0) "بدون تخلف" else "$score رویداد مشکوک",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                    Text("مشاهده ‹", color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    val report = selectedRow["report"] as? JsonObject
                    val events = report?.get("events") as? JsonObject
                    val times = report?.get("question_time_ms") as? JsonObject
                    val visits = report?.get("question_visits") as? JsonObject
                    val labels = report?.get("question_labels") as? JsonObject
                    val submitted = (report?.get("submitted") as? JsonPrimitive)?.contentOrNull == "true"
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("رویدادها", style = MaterialTheme.typography.titleSmall)
                                    Text("بستر آزمون: ${monitorPlatformLabel(report)}", style = MaterialTheme.typography.bodySmall)
                                    if (events.isNullOrEmpty()) {
                                        Text("رویداد مشکوکی ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                                    } else events.forEach { (kind, count) ->
                                        Text(
                                            "${kind.faMonitorEvent()}: ${(count as? JsonPrimitive)?.contentOrNull ?: "?"} بار",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("زمان", style = MaterialTheme.typography.titleSmall)
                                    (report?.get("entered_at_epoch_ms") as? JsonPrimitive)?.contentOrNull
                                        ?.toLongOrNull()?.takeIf { it > 0 }?.let {
                                            Text("زمان شروع: ${it.faClock()}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    (report?.get("left_at_epoch_ms") as? JsonPrimitive)?.contentOrNull
                                        ?.toLongOrNull()?.takeIf { it > 0 }?.let {
                                            // V137 — تا پیش از ارسال نهایی، این فقط «آخرین فعالیت» است نه خروج.
                                            Text(
                                                (if (submitted) "زمان ارسال پاسخ: " else "آخرین فعالیت: ") + it.faClock(),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("مدت پاسخ‌گویی هر سؤال", style = MaterialTheme.typography.titleSmall)
                                    if (times.isNullOrEmpty()) {
                                        Text("زمانی ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        // V137 — مرتب بر اساس شمارهٔ سؤال معلم (question_labels)؛ زمان به دقیقه:ثانیه.
                                        val entries = times.entries.toList().sortedBy { (qid, _) ->
                                            (labels?.get(qid) as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: Int.MAX_VALUE
                                        }
                                        entries.forEachIndexed { order, (qid, ms) ->
                                            val label = (labels?.get(qid) as? JsonPrimitive)?.contentOrNull ?: (order + 1).toString()
                                            val seconds = ((ms as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L) / 1000L
                                            val visitCount = (visits?.get(qid) as? JsonPrimitive)?.contentOrNull ?: "1"
                                            Text(
                                                "سؤال $label: ${seconds.faDuration()} · $visitCount بازدید",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** V137 — نمایش ثانیه به شکل «۲ دقیقه و ۵ ثانیه». */
private fun Long.faDuration(): String {
    val m = this / 60; val s = this % 60
    return if (m > 0) "$m دقیقه و $s ثانیه" else "$s ثانیه"
}

/**
 * V137.3 — تصاویر پاسخ یک سؤال با کلیدهای جایگزین: شناسهٔ سؤال در سمت معلم (ExamQuestionCodec) و
 * سمت دانش‌آموز (StudentExamPayloadCodec) وقتی JSON سؤال «id» ندارد یکسان نیست (UUID تصادفی در
 * برابر «q-<index>»)؛ پس علاوه بر id، کلیدهای «q-<index>»، «<index>» و شمارهٔ یک‌مبنا هم بررسی می‌شود.
 */
internal fun GradingSubmission.answerImagesFor(questionId: String, index: Int): List<String> {
    val keys = listOf(questionId, "q-$index", index.toString(), (index + 1).toString())
    return keys.firstNotNullOfOrNull { key -> responseImages[key]?.takeIf { it.isNotEmpty() } }.orEmpty()
}

/**
 * V137 — تصاویر پاسخ دانش‌آموز (از جمله خروجی تختهٔ وایت‌برد) بزرگ‌تر و با لمس تمام‌صفحه/زوم.
 */
@Composable
private fun AnswerImagesRow(urls: List<String>) {
    if (urls.isEmpty()) return
    var viewing by remember { mutableStateOf<String?>(null) }
    Text("تصاویر پاسخ (${urls.size})", style = MaterialTheme.typography.labelMedium)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        urls.forEach { url ->
            if (isAnswerAudioUrl(url)) AnswerAudioPlayer(url) else AsyncImage(
                url, "تصویر پاسخ",
                Modifier.size(220.dp).clickable { viewing = url },
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
    }
    viewing?.let { url ->
        ir.exam.app.ui.image.FullScreenImageViewer(uri = url, onDismiss = { viewing = null })
    }
}

/** V59.0 — مجموع رویدادهای مشکوک برای رنگ‌بندی کارت. */
private fun monitorViolationScore(report: JsonObject?): Int {
    val events = report?.get("events") as? JsonObject ?: return 0
    return events.values.sumOf { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0 }
}

/** V59.0 — رنگ کارت: ۰=سبز، ۱-۲=زرد، ۳-۵=نارنجی، بیشتر=قرمز. */
private fun monitorSeverityColor(score: Int): Color = when {
    score == 0 -> Color(0xFF2E7D32)
    score <= 2 -> Color(0xFFF9A825)
    score <= 5 -> Color(0xFFEF6C00)
    else -> Color(0xFFC62828)
}

private fun String.faMonitorEvent(): String = when (this) {
    "screenshot_attempt" -> "تلاش برای اسکرین‌شات"
    "screen_record_attempt" -> "تلاش برای ضبط صفحه"
    "app_leave" -> "خارج شدن از برنامه"
    "app_close" -> "بستن برنامه"
    "exam_screen_leave" -> "خارج شدن از صفحه آزمون"
    "window_blur" -> "خروج از پنجره"
    "copy_attempt" -> "تلاش برای کپی متن"
    "print_attempt" -> "تلاش برای چاپ/ذخیرهٔ صفحه"
    "multi_window" -> "چندپنجره"
    else -> this
}

/** V269 — بستر آزمون از گزارش نظارتی. */
internal fun monitorPlatformLabel(report: JsonObject?): String = when ((report?.get("platform") as? JsonPrimitive)?.contentOrNull) {
    "app" -> "اپ اندروید"
    "desktop" -> "سایت دسکتاپ"
    "mobile" -> "سایت گوشی"
    else -> "نامشخص"
}

private fun Long.faClock(): String {
    val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
    return formatter.format(java.util.Date(this))
}
