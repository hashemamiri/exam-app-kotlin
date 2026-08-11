package ir.exam.app.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.ui.math.NativeMathText
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = remember { ReportsViewModel() }) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) context.contentResolver.openOutputStream(uri)?.use { it.write(viewModel.csv().toByteArray()) }
    }
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("آمار، کارنامه و لیست نمرات", style = MaterialTheme.typography.headlineSmall) }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.loading) item { CircularProgressIndicator() }
        state.analytics?.let { analytics ->
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
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تحلیل پیشرفته کیفیت سؤال", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        state.exams.take(5).forEach { exam ->
                            FilterChip(
                                selected = state.selectedAnalysisExamId == exam.id,
                                onClick = { viewModel.loadQuestionAnalysis(exam.id) },
                                label = { Text(exam.title.take(18)) }
                            )
                        }
                    }
                    if (state.analysisLoading) CircularProgressIndicator()
                    state.questionAnalysis?.let { analysis ->
                        Text(
                            "${analysis.answerCount} پاسخ تصحیح‌شده · آلفای کرونباخ: " +
                                (analysis.cronbachAlpha?.let { "%.3f".format(it) } ?: "داده ناکافی")
                        )
                        analysis.questions.forEach { row ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    NativeMathText("سؤال ${row.index + 1}: ${row.text}")
                                    Text(
                                        "میانگین ${"%.1f".format(row.averagePercent)}٪ · حذف ${"%.1f".format(row.omitPercent)}٪ · " +
                                            "تمیز ${row.discrimination?.let { "%.3f".format(it) } ?: "—"} · " +
                                            "همبستگی ${row.pointBiserial?.let { "%.3f".format(it) } ?: "—"}"
                                    )
                                    Text(row.level.faAnalysisLevel(), color = row.level.analysisColor())
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
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
        if (state.selectedClass != null) {
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
                        onClick = { csvLauncher.launch("grade-list-${state.selectedClass?.name.orEmpty()}.csv") }
                    ) { Text("خروجی Excel/CSV") }
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
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("دانش‌آموز")
                            Text("میانگین٪")
                        }
                        state.rows.forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(row.studentName)
                                    Text(selectedExams.joinToString(" · ") { exam ->
                                        "${exam.title}: ${row.scores[exam.id]?.toString() ?: "غایب"}"
                                    }, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(row.averagePercent?.let { "%.1f".format(it) } ?: "—")
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

private fun String.faAnalysisLevel(): String = when (this) {
    "very_easy" -> "بسیار آسان؛ بازبینی تمایز پیشنهاد می‌شود"
    "easy" -> "آسان"
    "hard" -> "دشوار"
    "very_hard" -> "بسیار دشوار یا احتمالاً مبهم"
    "weak_discrimination" -> "قدرت تمایز ضعیف؛ سؤال نیازمند بازبینی است"
    else -> "متعادل"
}

@Composable
private fun String.analysisColor(): Color = when (this) {
    "very_hard", "weak_discrimination" -> MaterialTheme.colorScheme.error
    "very_easy" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}
