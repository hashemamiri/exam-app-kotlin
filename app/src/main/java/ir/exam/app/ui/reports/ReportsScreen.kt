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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = remember { ReportsViewModel() }) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
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
                            ReportPrintHelper.print(
                                context,
                                "لیست نمرات ${state.selectedClass?.name.orEmpty()}",
                                selectedExams,
                                state.rows
                            )
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
