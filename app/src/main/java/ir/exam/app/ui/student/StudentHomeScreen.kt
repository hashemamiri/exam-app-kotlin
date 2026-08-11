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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Room
import ir.exam.app.data.local.AppDatabase
import ir.exam.app.data.repository.RoomAnswerDraftRepository
import ir.exam.app.data.repository.SupabaseStudentExamRepository

@Composable
fun StudentHomeScreen() {
    val appContext = LocalContext.current.applicationContext
    val database = remember(appContext) {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "exam-native.db").build()
    }
    val viewModel = remember(appContext, database) {
        StudentExamViewModel(
            SupabaseStudentExamRepository(appContext),
            RoomAnswerDraftRepository(database.answerDraftDao())
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
            onSubmit = viewModel::submit
        )
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
