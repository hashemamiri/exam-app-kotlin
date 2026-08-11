package ir.exam.app.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.core.network.NetworkMonitor
import ir.exam.app.data.local.NativeDatabaseProvider
import ir.exam.app.data.repository.PendingActionRepository
import ir.exam.app.data.repository.QueuedExamRepository
import ir.exam.app.data.repository.RoomAnswerDraftRepository
import ir.exam.app.data.repository.SupabaseStudentExamRepository

@Composable
fun StudentHomeScreen(userId: String) {
    val appContext = LocalContext.current.applicationContext
    val database = remember(appContext) { NativeDatabaseProvider.get(appContext) }
    val pending = remember(appContext, database) {
        PendingActionRepository(appContext, database.pendingActionDao())
    }
    val viewModel = remember(appContext, database, pending, userId) {
        val remote = SupabaseStudentExamRepository(appContext)
        StudentExamViewModel(
            exams = QueuedExamRepository(remote, NetworkMonitor(appContext), pending),
            drafts = RoomAnswerDraftRepository(database.answerDraftDao()),
            pending = pending,
            ownerUserId = userId
        )
    }
    val state by viewModel.state.collectAsState()
    if (state.exam != null) {
        StudentExamContent(
            state = state,
            onAnswer = viewModel::answer,
            onPrevious = { viewModel.goTo(state.questionIndex - 1) },
            onNext = { viewModel.goTo(state.questionIndex + 1) },
            onAddImages = viewModel::addResponseImages,
            onRemoveImage = viewModel::removeResponseImage,
            onSubmit = viewModel::submit,
            onDone = viewModel::leaveFinishedExam
        )
        return
    }
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داشبورد دانش‌آموز", style = MaterialTheme.typography.headlineMedium)
        if (state.pendingSubmissions.isNotEmpty()) {
            val blocked = state.pendingSubmissions.count { it.state == "blocked_auth" || it.state == "failed" }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        "${state.pendingSubmissions.size} پاسخ در صف ارسال",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        if (blocked > 0) "$blocked مورد نیازمند تلاش دوباره است."
                        else "با اتصال معتبر اینترنت، WorkManager خودکار ارسال می‌کند."
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::retryPending) { Text("تلاش دوباره") }
                        if (blocked > 0) {
                            OutlinedButton(onClick = viewModel::clearFailedPending) { Text("حذف خطاها") }
                        }
                    }
                }
            }
        }
        Text("کد آزمون را وارد کنید")
        OutlinedTextField(state.code, viewModel::setCode, label = { Text("کد آزمون") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = viewModel::join, enabled = !state.loading && state.code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("ورود به آزمون") }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
