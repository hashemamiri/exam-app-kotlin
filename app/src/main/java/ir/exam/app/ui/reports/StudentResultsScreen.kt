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
import androidx.compose.material3.MaterialTheme
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
fun StudentResultsScreen(viewModel: StudentResultsViewModel = remember { StudentResultsViewModel() }) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val rows = mutableListOf(listOf("آزمون", "درس", "نمره", "از", "درصد", "بازخورد"))
            state.grades.forEach { rows += listOf(it.title, it.subject, it.grade.toString(), it.total.toString(), "%.2f".format(it.percent), it.feedback) }
            val csv = "\uFEFF" + rows.joinToString("\n") { row -> row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } }
            context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
        }
    }
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("نتایج و پاسخ‌های من", style = MaterialTheme.typography.headlineSmall) }
        if (state.loading) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.grades.isNotEmpty()) {
            item {
                val avg = state.grades.map { it.percent }.average()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("میانگین کل: %.1f%%".format(avg))
                    Button(onClick = { exporter.launch("my-grades.csv") }) { Text("خروجی Excel/CSV") }
                }
            }
            items(state.grades, key = { it.examId + it.submittedAt }) { grade ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(grade.title, style = MaterialTheme.typography.titleMedium)
                        Text("${grade.subject} · ${grade.grade} از ${grade.total} · %.1f%%".format(grade.percent))
                        if (grade.feedback.isNotBlank()) Text("بازخورد: ${grade.feedback}")
                    }
                }
            }
        } else if (!state.loading) item { Text("هنوز نمره‌ای ثبت نشده است.") }
        item { Text("پاسخ‌های ثبت‌شده: ${state.answers.size}") }
    }
}
