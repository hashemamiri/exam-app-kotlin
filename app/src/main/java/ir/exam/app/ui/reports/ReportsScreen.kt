package ir.exam.app.ui.reports

import ir.exam.app.core.io.DocumentIo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.domain.model.ClassGradeRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.core.export.XlsxSheet
import ir.exam.app.core.export.XlsxWorkbook
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(
    // V131 — "stats": آمار/نمودار/تحلیل سؤال (کارت «آمار»)؛ "grades": کارنامه و لیست نمرات (کارت «کارنامه»).
    section: String = "stats",
    viewModel: ReportsViewModel = remember { ReportsViewModel() }
) {
    val statsOnly = section != "grades"
    val gradesOnly = section == "grades"
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val xlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        /* V252 — ساخت و نوشتن اکسل روی نخ IO با مدیریت خطا */
        if (uri != null) scope.launch { runCatching { viewModel.xlsx() }.fold({ bytes -> DocumentIo.writeBytes(context, uri, bytes) }, { Result.failure<Unit>(it) }).onFailure(viewModel::reportError) }
    }
    LaunchedEffect(Unit) { viewModel.load() }

    // V277 — کارنامهٔ تکی باز
    var reportCardRow by remember { mutableStateOf<ClassGradeRow?>(null) }
    val selectedExams = remember(state.exams, state.selectedExamIds) { state.exams.filter { it.id in state.selectedExamIds } }
    reportCardRow?.let { row ->
        StudentReportCardDialog(
            row = row,
            exams = selectedExams,
            onDismiss = { reportCardRow = null },
            onPrint = {
                scope.launch {
                    runCatching { ReportPrintHelper.print(context, "کارنامهٔ ${row.studentName}", selectedExams, listOf(row)) }
                        .onFailure(viewModel::reportError)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // V277 — عنوان داخل صفحه حذف شد؛ هدر برنامه («کارنامه و لیست نمرات»/«آمار») همان را نشان می‌دهد (دو هدر نباشد)
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.loading) item { CircularProgressIndicator() }
        if (statsOnly) state.analytics?.let { analytics ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("آزمون", analytics.examCount.toString(), Modifier.weight(1f))
                    StatCard("پاسخ", analytics.answerCount.toString(), Modifier.weight(1f))
                    StatCard("تصحیح‌شده", analytics.gradedCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("مانده", analytics.pendingCount.toString(), Modifier.weight(1f))
                    StatCard("میانگین", "%.1f%%".format(analytics.averagePercent), Modifier.weight(1f))
                }
            }
            // V208 — نمودار خوانا: نوار انباشتهٔ تصحیح‌شده/مانده با راهنما، عدد و درصد هر بخش
            item { AnswerStatusCard(total = analytics.answerCount, graded = analytics.gradedCount, pending = analytics.pendingCount) }
        }
        // V208 — «تحلیل پیشرفته کیفیت سؤال» به درخواست کاربر از بخش آمار حذف شد.
        if (gradesOnly) {
            // V277 — فیلتر پلکانی: ۱ مدرسه (اگر هست) → ۲ کلاس → ۳ دانش‌آموزان → ۴ آزمون‌ها → نمایش
            var step = 1
            if (state.schools.isNotEmpty()) {
                item {
                    FilterStepCard("${PersianDigits.convert(step)}. مدرسه") {
                        FilterChip(selected = state.selectedSchoolId == null, onClick = { viewModel.selectSchool(null) }, label = { Text("همهٔ کلاس‌های من") })
                        state.schools.forEach { sch ->
                            FilterChip(selected = state.selectedSchoolId == sch.id, onClick = { viewModel.selectSchool(sch.id) }, label = { Text(sch.name) })
                        }
                    }
                }
                step++
            }
            item {
                FilterStepCard("${PersianDigits.convert(step)}. کلاس") {
                    if (state.classOptions.isEmpty()) Text("کلاسی یافت نشد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.classOptions.forEach { item ->
                        FilterChip(selected = state.selectedClass?.id == item.id, onClick = { viewModel.selectClass(item) }, label = { Text(item.name) })
                    }
                }
            }
            step++
            if (state.selectedClass != null) {
                item {
                    val allStudents = state.roster.isNotEmpty() && state.selectedStudentIds.size == state.roster.size
                    FilterStepCard(
                        "${PersianDigits.convert(step)}. دانش‌آموزان (${PersianDigits.convert(state.selectedStudentIds.size)} از ${PersianDigits.convert(state.roster.size)})",
                        trailing = { TextButton(onClick = { viewModel.setAllStudents(!allStudents) }) { Text(if (allStudents) "هیچ‌کدام" else "همه") } }
                    ) {
                        if (state.rosterLoading) CircularProgressIndicator(Modifier.size(20.dp))
                        else if (state.roster.isEmpty()) Text("دانش‌آموزی در این کلاس نیست.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.roster.forEach { st ->
                            FilterChip(selected = st.id in state.selectedStudentIds, onClick = { viewModel.toggleStudent(st.id) }, label = { Text(st.fullName) })
                        }
                    }
                }
                item {
                    val allExams = state.exams.isNotEmpty() && state.selectedExamIds.size == state.exams.size
                    FilterStepCard(
                        "${PersianDigits.convert(step + 1)}. آزمون‌ها (${PersianDigits.convert(state.selectedExamIds.size)} از ${PersianDigits.convert(state.exams.size)})",
                        trailing = { TextButton(onClick = { viewModel.setAllExams(!allExams) }) { Text(if (allExams) "هیچ‌کدام" else "همه") } }
                    ) {
                        state.exams.forEach { exam ->
                            FilterChip(selected = exam.id in state.selectedExamIds, onClick = { viewModel.toggleExam(exam.id) }, label = { Text(exam.title) })
                        }
                    }
                }
                item {
                    Button(
                        enabled = state.selectedStudentIds.isNotEmpty() && state.selectedExamIds.isNotEmpty() && !state.loading,
                        onClick = viewModel::showReports,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("نمایش کارنامه‌ها") }
                }
            }
            if (state.rowsShown) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = state.rows.isNotEmpty(),
                            onClick = { xlsxLauncher.launch("grade-list-${state.selectedClass?.name.orEmpty()}.xlsx") }
                        ) { Text("خروجی Excel") }
                        OutlinedButton(
                            enabled = state.rows.isNotEmpty(),
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        ReportPrintHelper.print(context, "لیست نمرات ${state.selectedClass?.name.orEmpty()}", selectedExams, state.rows)
                                    }.onFailure(viewModel::reportError)
                                }
                            }
                        ) { Text("چاپ / PDF") }
                    }
                }
                if (state.rows.isEmpty()) item { Text("نتیجه‌ای برای این انتخاب نیست.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                // V277 — کارت تمیز هر دانش‌آموز: نام + میانگین + آیکن کارنامه (ریز نمرات فقط داخل کارنامهٔ تکی)
                items(state.rows, key = { it.studentId }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(row.studentName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            AverageBadge(row.averagePercent)
                            IconButton(onClick = { reportCardRow = row }) {
                                Icon(Icons.Outlined.Description, contentDescription = "کارنامهٔ ${row.studentName}", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** V277 — کارت یک مرحلهٔ فیلتر: عنوان + چیپ‌ها در FlowRow (در گوشی باریک به سطر بعد می‌روند). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterStepCard(title: String, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) { content() }
        }
    }
}

@Composable
private fun AverageBadge(avg: Double?) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when { avg == null -> MaterialTheme.colorScheme.surfaceVariant; avg >= 50.0 -> Color(0xFFE3F5E8); else -> Color(0xFFFDE7E7) },
        modifier = Modifier.width(72.dp)
    ) {
        Text(
            avg?.let { PersianDigits.convert("%.1f".format(it)) + "٪" } ?: "—",
            style = MaterialTheme.typography.labelLarge,
            color = if (avg == null) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF1F2937),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

/** V277 — کارنامهٔ تکی دانش‌آموز: هر آزمون یک سطر (نمره از سقف · درصد · وضعیت) + میانگین + چاپ. */
@Composable
private fun StudentReportCardDialog(row: ClassGradeRow, exams: List<ExamDashboardDto>, onDismiss: () -> Unit, onPrint: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کارنامهٔ ${row.studentName}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("آزمون", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("نمره", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(84.dp), textAlign = TextAlign.Center)
                    Text("درصد", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                }
                HorizontalDivider()
                exams.forEach { exam ->
                    val score = row.scores[exam.id]
                    val max = row.maxScores[exam.id] ?: exam.totalScore
                    val pct = if (score != null && max > 0) score * 100.0 / max else null
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(exam.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            exam.subject?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Text(
                            if (score == null) "غایب" else PersianDigits.convert(trimNum(score)) + " از " + PersianDigits.convert(trimNum(max)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (score == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(84.dp), textAlign = TextAlign.Center
                        )
                        Text(
                            pct?.let { PersianDigits.convert("%.0f".format(it)) + "٪" } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when { pct == null -> MaterialTheme.colorScheme.onSurfaceVariant; pct >= 50.0 -> Color(0xFF2E9E6B); else -> Color(0xFFC62828) },
                            modifier = Modifier.width(56.dp), textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("میانگین درصد", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    AverageBadge(row.averagePercent)
                }
            }
        },
        confirmButton = { Button(onClick = onPrint) { Text("چاپ / PDF") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

private fun trimNum(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else "%.2f".format(v).trimEnd('0').trimEnd('.')

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** V208 — وضعیت پاسخ‌ها: نوار انباشته + راهنما (تصحیح‌شده / مانده) با تعداد و درصد. */
@Composable
private fun AnswerStatusCard(total: Int, graded: Int, pending: Int) {
    val gradedColor = Color(0xFF2E9E6B)
    val pendingColor = Color(0xFFE39A2D)
    val denom = total.coerceAtLeast(1)
    val gradedFrac = (graded.toFloat() / denom).coerceIn(0f, 1f)
    val pendingFrac = (pending.toFloat() / denom).coerceIn(0f, 1f - gradedFrac)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("وضعیت پاسخ‌ها", style = MaterialTheme.typography.titleMedium)
            Text("از مجموع ${PersianDigits.convert(total)} پاسخ دریافتی", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (gradedFrac > 0f) Box(Modifier.fillMaxHeight().weight(gradedFrac).background(gradedColor))
                if (pendingFrac > 0f) Box(Modifier.fillMaxHeight().weight(pendingFrac).background(pendingColor))
                val rest = 1f - gradedFrac - pendingFrac
                if (rest > 0.0001f) Spacer(Modifier.weight(rest))
            }
            StatusLegendRow("تصحیح‌شده", graded, total, gradedColor)
            StatusLegendRow("در انتظار تصحیح", pending, total, pendingColor)
        }
    }
}

@Composable
private fun StatusLegendRow(label: String, count: Int, total: Int, color: Color) {
    val percent = if (total > 0) count * 100.0 / total else 0.0
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, modifier = Modifier.weight(1f))
        Text(
            PersianDigits.convert(count) + " پاسخ · " + PersianDigits.convert("%.0f".format(percent)) + "٪",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
