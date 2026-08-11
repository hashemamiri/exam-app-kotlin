package ir.exam.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import ir.exam.app.data.dto.ExamDashboardDto

@Composable
fun TeacherDashboardScreen(
    onCreateExam: () -> Unit,
    onEditExam: (String) -> Unit
) {
    val viewModel = remember { TeacherDashboardViewModel() }
    val state by viewModel.state.collectAsState()
    var deleteCandidate by remember { mutableStateOf<ExamDashboardDto?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داشبورد معلم", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = viewModel::load) { Text("به‌روزرسانی") }
            Button(onClick = onCreateExam) { Text("ساخت آزمون جدید") }
        }
        if (state.actionLoading) CircularProgressIndicator()
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            state.loading -> CircularProgressIndicator()
            state.exams.isEmpty() -> Text("هنوز آزمونی برای نمایش وجود ندارد.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.exams, key = { it.id }) { exam ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(exam.title.ifBlank { "بدون عنوان" }, style = MaterialTheme.typography.titleMedium)
                            Text("درس: ${exam.subject ?: "بدون درس"}")
                            Text("کد آزمون: ${exam.code ?: "—"}")
                            Text("مدت: ${exam.duration ?: 0} دقیقه · بارم: ${exam.totalScore}")
                            Text(if (exam.isOpen) "وضعیت: باز" else "وضعیت: بسته")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { onEditExam(exam.id) }) { Text("ویرایش") }
                                OutlinedButton(onClick = { viewModel.setOpen(exam) }) {
                                    Text(if (exam.isOpen) "بستن" else "بازکردن")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { viewModel.duplicate(exam) }) { Text("تکثیر") }
                                TextButton(onClick = { deleteCandidate = exam }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteCandidate?.let { exam ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("حذف آزمون") },
            text = { Text("آزمون «${exam.title}» و پاسخ‌ها، تلاش‌ها و مخاطبان وابسته حذف شوند؟ این کار برگشت‌پذیر نیست.") },
            confirmButton = {
                Button(onClick = { viewModel.delete(exam); deleteCandidate = null }) { Text("حذف کامل") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("انصراف") } }
        )
    }
}
