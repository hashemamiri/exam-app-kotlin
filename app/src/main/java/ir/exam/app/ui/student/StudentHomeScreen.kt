package ir.exam.app.ui.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.core.network.NetworkMonitor
import ir.exam.app.data.local.NativeDatabaseProvider
import ir.exam.app.data.repository.PendingActionRepository
import ir.exam.app.data.repository.QueuedExamRepository
import ir.exam.app.data.repository.RoomAnswerDraftRepository
import ir.exam.app.data.repository.SupabaseStudentExamRepository
import ir.exam.app.ui.app.NeumorphicPanel
import kotlinx.coroutines.launch

@Composable
fun StudentHomeScreen(
    userId: String,
    initialJoinCode: String? = null,
    joinRequestKey: Int = 0,
    // V58.0.2 — گزارش «داخل آزمون بودن» به پوسته تا هدر و منوی همبرگری
    // در حین آزمون حذف شوند.
    onExamActiveChanged: (Boolean) -> Unit = {}
) {
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
    // آزمون فعال (بعد از شروع و پیش از پایان) → هدر پوسته پنهان شود.
    val examActive = state.exam != null && !state.showPreview && !state.finished
    LaunchedEffect(examActive) { onExamActiveChanged(examActive) }
    LaunchedEffect(joinRequestKey, initialJoinCode) {
        if (joinRequestKey > 0 && !initialJoinCode.isNullOrBlank() && state.exam == null) {
            viewModel.setCode(initialJoinCode)
            viewModel.join()
        }
    }
    if (state.exam != null) {
        if (state.showPreview && !state.finished) {
            StudentExamPreview(state = state, onStart = viewModel::startExam)
        } else {
            StudentExamContent(
                state = state,
                onAnswer = viewModel::answer,
                onPrevious = { viewModel.goTo(state.questionIndex - 1) },
                onNext = { viewModel.goTo(state.questionIndex + 1) },
                onGoTo = viewModel::goTo,
                onToggleFlag = viewModel::toggleFlag,
                onAddImages = viewModel::addResponseImages,
                onRemoveImage = viewModel::removeResponseImage,
                onSubmit = viewModel::requestSubmitReview,
                onConfirmSubmit = viewModel::confirmSubmit,
                onDismissSubmit = viewModel::dismissSubmitReview,
                onDone = viewModel::leaveFinishedExam,
                onDismissExamChanges = viewModel::dismissExamChanges,
                onSecurityEvent = viewModel::recordSecurityEvent,
                onExitExam = viewModel::exitExamScreen
            )
        }
        return
    }
    // V59.2 — اعلان «پیام جدید دارید»: پیام‌های دیده‌نشدهٔ تقویم معلم.
    val calendarRepo = remember { ir.exam.app.data.repository.SupabaseCalendarRepository() }
    var unseenNotes by remember { mutableStateOf<List<ir.exam.app.domain.model.CalendarNote>>(emptyList()) }
    var openedNote by remember { mutableStateOf<ir.exam.app.domain.model.CalendarNote?>(null) }
    val noteScope = rememberCoroutineScope()
    LaunchedEffect(userId, state.exam) {
        if (state.exam == null) {
            unseenNotes = calendarRepo.unseenNotes().getOrDefault(emptyList())
        }
    }
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داشبورد دانش‌آموز", style = MaterialTheme.typography.headlineMedium)
        if (unseenNotes.isNotEmpty()) {
            NeumorphicPanel(
                modifier = Modifier.fillMaxWidth(),
                radius = 22.dp,
                depth = 10.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { openedNote = unseenNotes.firstOrNull() }
                ) {
                    Text(
                        "پیام جدید دارید",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (unseenNotes.size == 1) unseenNotes.first().title
                        else "${unseenNotes.size} پیام خوانده‌نشده — برای مشاهده لمس کنید"
                    )
                }
            }
        }
        if (state.restoringExam) {
            CircularProgressIndicator()
            Text("در حال بررسی آزمون نیمه‌تمام روی این دستگاه...")
        }
        if (state.resumableExamAvailable && !state.restoringExam) {
            // V59.2 — پیام آزمون نیمه‌کاره: پاسخ‌های قبلی ذخیره‌اند؛ با یک دکمه ادامه بده.
            NeumorphicPanel(
                modifier = Modifier.fillMaxWidth(),
                radius = 22.dp,
                depth = 10.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("آزمون نیمه‌تمام دارید", style = MaterialTheme.typography.titleMedium)
                    Text("پاسخ‌های قبلی شما ذخیره شده‌اند و زمان سرور ادامه دارد.")
                    Button(onClick = viewModel::rejoinActiveExam, enabled = !state.loading) {
                        Text("پیوستن به آزمون")
                    }
                }
            }
        }
        if (state.pendingSubmissions.isNotEmpty()) {
            val blocked = state.pendingSubmissions.count { it.state == "blocked_auth" || it.state == "failed" }
            NeumorphicPanel(
                modifier = Modifier.fillMaxWidth(),
                radius = 22.dp,
                depth = 10.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
        NeumorphicPanel(
            modifier = Modifier.fillMaxWidth(),
            radius = 26.dp,
            depth = 12.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("کد آزمون را وارد کنید", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    state.code,
                    viewModel::setCode,
                    label = { Text("کد آزمون") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = viewModel::join,
                    enabled = !state.loading && !state.restoringExam && state.code.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ورود به آزمون") }
                if (state.loading) CircularProgressIndicator()
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    openedNote?.let { note ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { openedNote = null },
            title = { Text(note.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (note.body.isNotBlank()) Text(note.body)
                    Text(
                        "تاریخ: ${note.date}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val id = note.id
                    openedNote = null
                    unseenNotes = unseenNotes.filterNot { it.id == id }
                    noteScope.launch { calendarRepo.markSeen(id) }
                }) { Text("خواندم") }
            }
        )
    }
}
