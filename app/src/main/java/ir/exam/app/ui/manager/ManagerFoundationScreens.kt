package ir.exam.app.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.launch

private data class ManagerSummary(
    val schoolName: String,
    val province: String,
    val city: String,
    val teachers: Int,
    val students: Int,
    val classes: Int,
    val exams: Int
)

@Composable
fun ManagerTeachersScreen(newTeacherRequested: Int = 0) {
    val summaryState = rememberManagerSummary()
    val repository = remember { ir.exam.app.data.repository.SupabaseManagerRepository() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var teachers by remember { mutableStateOf<List<ir.exam.app.data.repository.SchoolTeacherItem>>(emptyList()) }
    var email by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf<ir.exam.app.data.repository.TeacherInviteResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadingTeachers by remember { mutableStateOf(true) }
    var inviteOpen by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<ir.exam.app.data.repository.SchoolTeacherItem?>(null) }

    fun reload() {
        scope.launch {
            loadingTeachers = true
            repository.teachers().onSuccess { teachers = it; error = null }
                .onFailure { error = it.message }
            loadingTeachers = false
        }
    }
    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(newTeacherRequested) { if (newTeacherRequested > 0) inviteOpen = true }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("معلم‌ها", style = MaterialTheme.typography.headlineSmall)
        summaryState.summary?.let { Text(it.schoolName) }
        androidx.compose.material3.Button(onClick = { inviteOpen = !inviteOpen }) { Text("دعوت معلم جدید") }
        if (inviteOpen) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = email, onValueChange = { email = it.trim().take(254) },
                        label = { Text("ایمیل معلم") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    androidx.compose.material3.Button(
                        enabled = '@' in email,
                        onClick = {
                            scope.launch {
                                repository.createInvite(email).onSuccess { invite = it; error = null }
                                    .onFailure { error = it.message }
                            }
                        }
                    ) { Text("ساخت کد دعوت") }
                    invite?.let {
                        Text("کد دعوت ۷ روز اعتبار دارد و فقط برای ${it.email} قابل استفاده است.")
                        androidx.compose.foundation.text.selection.SelectionContainer { Text(it.code) }
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loadingTeachers) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        teachers.forEach { teacher ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(teacher.fullName.ifBlank { teacher.username }, style = MaterialTheme.typography.titleMedium)
                        Text(teacher.email, style = MaterialTheme.typography.bodySmall)
                    }
                    androidx.compose.material3.TextButton(onClick = { removeTarget = teacher }) { Text("حذف") }
                }
            }
        }
    }
    removeTarget?.let { teacher ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("قطع عضویت معلم") },
            text = { Text("عضویت ${teacher.fullName} غیرفعال شود؟ حساب و آزمون‌های او حذف نمی‌شوند.") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    scope.launch {
                        repository.disableTeacher(teacher.id).onSuccess { removeTarget = null; reload() }
                            .onFailure { error = it.message; removeTarget = null }
                    }
                }) { Text("قطع عضویت") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { removeTarget = null }) { Text("انصراف") } }
        )
    }
}

@Composable
fun ManagerWalletFoundationScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("کیف پول مدیر/معاون", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("زیرساخت کیف پول مدرسه آماده است.", style = MaterialTheme.typography.titleMedium)
                Text("شارژ مدیر و انتقال اتمیک تومان به معلم در V38 فعال می‌شود.")
            }
        }
    }
}

@Composable
fun ManagerStatsScreen() {
    val state = rememberManagerSummary()
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("آمار مدرسه", style = MaterialTheme.typography.headlineSmall)
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
            else -> {
                val summary = checkNotNull(state.summary)
                listOf(
                    "معلم" to summary.teachers,
                    "دانش‌آموز" to summary.students,
                    "کلاس" to summary.classes,
                    "آزمون" to summary.exams
                ).chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (label, value) ->
                            Card(Modifier.weight(1f)) {
                                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
                                    Text(label)
                                }
                            }
                        }
                    }
                }
                Text("آمار آزمون‌های برگزارشده، پاسخ‌ها، میانگین نمره و فعالیت معلم‌ها در V38 فعال می‌شود.")
            }
        }
    }
}

private data class ManagerSummaryState(
    val loading: Boolean = true,
    val summary: ManagerSummary? = null,
    val error: String? = null
)

@Composable
private fun rememberManagerSummary(): ManagerSummaryState {
    var state by remember { mutableStateOf(ManagerSummaryState()) }
    LaunchedEffect(Unit) {
        runCatching {
            val raw = SupabaseProvider.client.postgrest.rpc("native_manager_school_summary_v36")
                .decodeAs<JsonObject>()
            raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
            ManagerSummary(
                schoolName = raw["school_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                province = raw["province"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                city = raw["city"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                teachers = raw["teachers"]?.jsonPrimitive?.intOrNull ?: 0,
                students = raw["students"]?.jsonPrimitive?.intOrNull ?: 0,
                classes = raw["classes"]?.jsonPrimitive?.intOrNull ?: 0,
                exams = raw["exams"]?.jsonPrimitive?.intOrNull ?: 0
            )
        }.onSuccess { state = ManagerSummaryState(loading = false, summary = it) }
            .onFailure { state = ManagerSummaryState(loading = false, error = it.message ?: "دریافت آمار ناموفق بود.") }
    }
    return state
}
