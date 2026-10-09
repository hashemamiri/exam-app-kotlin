package ir.exam.app.ui.reports

import ir.exam.app.core.io.DocumentIo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(if (gradesOnly) "کارنامه و لیست نمرات" else "آمار و تحلیل آزمون‌ها", style = MaterialTheme.typography.headlineSmall) }
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
        if (gradesOnly) item {
            Text("انتخاب کلاس", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.classes.take(6).forEach { item ->
                    FilterChip(
                        selected = state.selectedClass?.id == item.id,
                        onClick = { viewModel.selectClass(item) },
                        label = { Text(item.name) }
                    )
                }
            }
        }
        if (gradesOnly && state.selectedClass != null) {
            item {
                Text("آزمون‌های گزارش", style = MaterialTheme.typography.titleMedium)
                Column {
                    state.exams.forEach { exam ->
                        FilterChip(
                            selected = exam.id in state.selectedExamIds,
                            onClick = { viewModel.toggleExam(exam.id) },
                            label = { Text(exam.title) }
                        )
                    }
                }
            }
            item {
                val selectedExams = state.exams.filter { it.id in state.selectedExamIds }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = state.rows.isNotEmpty(),
                        onClick = { xlsxLauncher.launch("grade-list-${state.selectedClass?.name.orEmpty()}.xlsx") }
                    ) { Text("خروجی Excel واقعی") }
                    OutlinedButton(
                        enabled = state.rows.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                runCatching {
                                    ReportPrintHelper.print(
                                        context,
                                        "لیست نمرات ${state.selectedClass?.name.orEmpty()}",
                                        selectedExams,
                                        state.rows
                                    )
                                }.onFailure(viewModel::reportError)
                            }
                        }
                    ) { Text("چاپ / PDF") }
                }
            }
            item {
                val selectedExams = state.exams.filter { it.id in state.selectedExamIds }
                // V208 — جدول مرتب: ستون نام (و ریز نمرات زیر آن) + ستون ثابتِ میانگین، با خط جداکننده
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("دانش‌آموز", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text("میانگین", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        if (state.rows.isEmpty()) Text("دانش‌آموزی در این کلاس نیست.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.rows.forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(row.studentName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        selectedExams.joinToString("   ") { exam -> "${exam.title}: ${row.scores[exam.id]?.toString() ?: "غایب"}" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val avg = row.averagePercent
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = when { avg == null -> MaterialTheme.colorScheme.surfaceVariant; avg >= 50.0 -> Color(0xFFE3F5E8); else -> Color(0xFFFDE7E7) },
                                    modifier = Modifier.width(72.dp)
                                ) {
                                    Text(
                                        avg?.let { "%.1f٪".format(it) } ?: "—",
                                        style = MaterialTheme.typography.labelLarge,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
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
