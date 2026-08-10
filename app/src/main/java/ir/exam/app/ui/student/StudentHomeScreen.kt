package ir.exam.app.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.data.repository.InMemoryAnswerDraftRepository
import ir.exam.app.data.repository.SupabaseStudentExamRepository

@Composable
fun StudentHomeScreen() {
    val viewModel = remember { StudentExamViewModel(SupabaseStudentExamRepository(), InMemoryAnswerDraftRepository()) }
    val state by viewModel.state.collectAsState()
    if (state.exam != null) {
        StudentExamContent(state, viewModel::answer, { viewModel.goTo(state.questionIndex + 1) }, viewModel::submit)
        return
    }
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داشبورد دانش‌آموز", style = MaterialTheme.typography.headlineMedium)
        Text("کد آزمون را وارد کنید")
        OutlinedTextField(state.code, viewModel::setCode, label = { Text("کد آزمون") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = viewModel::join, enabled = !state.loading && state.code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("ورود به آزمون") }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
