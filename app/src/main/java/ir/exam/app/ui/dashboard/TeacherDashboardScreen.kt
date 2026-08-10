package ir.exam.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp

@Composable
fun TeacherDashboardScreen(onCreateExam: () -> Unit) {
    val viewModel = remember { TeacherDashboardViewModel() }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داشبورد معلم", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = viewModel::load) { Text("به‌روزرسانی") }
            Button(onClick = onCreateExam) { Text("ساخت آزمون جدید") }
        }
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.exams.isEmpty() -> Text("هنوز آزمونی برای نمایش وجود ندارد.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.exams, key = { it.id }) { exam ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(exam.title.ifBlank { "بدون عنوان" }, style = MaterialTheme.typography.titleMedium)
                            Text("درس: ${exam.subject ?: "بدون درس"}")
                            Text("کد آزمون: ${exam.code ?: "—"}")
                            Text("مدت: ${exam.duration ?: 0} دقیقه")
                            Text(if (exam.isOpen) "وضعیت: باز" else "وضعیت: بسته")
                        }
                    }
                }
            }
        }
    }
}
