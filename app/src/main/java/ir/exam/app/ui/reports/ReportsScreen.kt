package ir.exam.app.ui.reports

import ir.exam.app.core.io.DocumentIo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
    // V279 — کدام پنجرهٔ انتخاب باز است: school / class / students / exams
    var picker by remember { mutableStateOf<String?>(null) }
    when (picker) {
        "school" -> PickerDialog(
            title = "انتخاب مدرسه",
            items = listOf(PickItem("", "همهٔ کلاس‌های من")) + state.schools.map { PickItem(it.id, it.name, it.city.takeIf(String::isNotBlank)) },
            selected = setOf(state.selectedSchoolId ?: ""), multi = false,
            onDismiss = { picker = null },
            onConfirm = { ids -> viewModel.selectSchool(ids.firstOrNull()?.takeIf(String::isNotBlank)); picker = null }
        )
        "class" -> PickerDialog(
            title = "انتخاب کلاس",
            items = state.classOptions.map { PickItem(it.id, it.name, listOfNotNull(it.grade, it.fieldOfStudy).filter(String::isNotBlank).joinToString(" · ").ifBlank { null }) },
            selected = state.selectedClass?.id?.let(::setOf) ?: emptySet(), multi = false,
            onDismiss = { picker = null },
            onConfirm = { ids -> state.classOptions.firstOrNull { it.id == ids.firstOrNull() }?.let(viewModel::selectClass); picker = null }
        )
        "students" -> PickerDialog(
            title = "انتخاب دانش‌آموزان",
            items = state.roster.map { PickItem(it.id, it.fullName, it.username) },
            selected = state.selectedStudentIds, multi = true,
            onDismiss = { picker = null },
            onConfirm = { ids -> viewModel.setStudents(ids); picker = null }
        )
        "exams" -> PickerDialog(
            title = "انتخاب آزمون‌ها",
            items = state.exams.map { PickItem(it.id, it.title, it.subject?.takeIf(String::isNotBlank)) },
            selected = state.selectedExamIds, multi = true,
            onDismiss = { picker = null },
            onConfirm = { ids -> viewModel.setExams(ids); picker = null }
        )
    }
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
            // V279 — انتخاب از پنجره: هر مرحله یک ردیف «عنوان: مقدار ▾» که پنجرهٔ انتخاب (با جست‌وجو) باز می‌کند
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                        var n = 1
                        if (state.schools.isNotEmpty()) {
                            PickerRow("${PersianDigits.convert(n++)}. مدرسه", state.schools.firstOrNull { it.id == state.selectedSchoolId }?.name ?: "همهٔ کلاس‌های من") { picker = "school" }
                            HorizontalDivider()
                        }
                        PickerRow("${PersianDigits.convert(n++)}. کلاس", state.selectedClass?.name ?: "انتخاب کنید", enabled = state.classOptions.isNotEmpty() || state.selectedSchoolId != null) { picker = "class" }
                        if (state.selectedClass != null) {
                            HorizontalDivider()
                            PickerRow("${PersianDigits.convert(n)}. دانش‌آموزان", if (state.rosterLoading) "در حال دریافت…" else "${PersianDigits.convert(state.selectedStudentIds.size)} از ${PersianDigits.convert(state.roster.size)}", enabled = !state.rosterLoading && state.roster.isNotEmpty()) { picker = "students" }
                            HorizontalDivider()
                            PickerRow("${PersianDigits.convert(n + 1)}. آزمون‌ها", "${PersianDigits.convert(state.selectedExamIds.size)} از ${PersianDigits.convert(state.exams.size)}", enabled = state.exams.isNotEmpty()) { picker = "exams" }
                        }
                    }
                }
            }
            if (state.selectedClass != null) {
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

/** V279 — ردیف انتخاب: عنوان سمت راست، مقدار فعلی + ▾ سمت چپ؛ لمس = پنجرهٔ انتخاب. */
@Composable
private fun PickerRow(label: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class PickItem(val id: String, val title: String, val subtitle: String? = null)

/** V279 — پنجرهٔ انتخاب: جست‌وجو، فهرست با تیک (چندانتخابی) یا دایره (تک‌انتخابی)، «همه/هیچ‌کدام»، تأیید. */
@Composable
private fun PickerDialog(title: String, items: List<PickItem>, selected: Set<String>, multi: Boolean, onDismiss: () -> Unit, onConfirm: (Set<String>) -> Unit) {
    var query by remember { mutableStateOf("") }
    var picked by remember(selected) { mutableStateOf(selected) }
    val shown = remember(items, query) { if (query.isBlank()) items else items.filter { (it.title + " " + (it.subtitle ?: "")).contains(query.trim(), ignoreCase = true) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (items.size > 6) OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("جست‌وجو") }, modifier = Modifier.fillMaxWidth())
                if (multi) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { picked = picked + shown.map { it.id } }) { Text("همه") }
                    TextButton(onClick = { picked = picked - shown.map { it.id }.toSet() }) { Text("هیچ‌کدام") }
                    Text("${PersianDigits.convert(picked.size)} انتخاب", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
                }
                if (shown.isEmpty()) Text("موردی یافت نشد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it.id }) { row ->
                        val on = row.id in picked
                        Row(
                            Modifier.fillMaxWidth().clickable { picked = if (multi) (if (on) picked - row.id else picked + row.id) else setOf(row.id) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (multi) Checkbox(checked = on, onCheckedChange = null) else RadioButton(selected = on, onClick = null)
                            Column(Modifier.weight(1f)) {
                                Text(row.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                row.subtitle?.let { sub -> Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(picked) }, enabled = multi || picked.isNotEmpty()) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
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
