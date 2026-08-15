package ir.exam.app.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.launch

private data class TeacherActivity(
    val name: String, val exams: Int, val classes: Int, val students: Int, val walletBalance: Long
)
private data class ManagerSummary(
    val schoolName: String,
    val province: String,
    val city: String,
    val teachers: Int,
    val students: Int,
    val classes: Int,
    val exams: Int,
    val answers: Int = 0,
    val averagePercent: Double = 0.0,
    val distributedToman: Long = 0,
    val teacherActivity: List<TeacherActivity> = emptyList()
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
    var transferTarget by remember { mutableStateOf<ir.exam.app.data.repository.SchoolTeacherItem?>(null) }
    var transferAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loadingTeachers = true
            repository.teachers().onSuccess { teachers = it; error = null }
                .onFailure { error = safeManagerError(it) }
            loadingTeachers = false
        }
    }
    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(newTeacherRequested) { if (newTeacherRequested > 0) inviteOpen = true }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                                    .onFailure { error = safeManagerError(it) }
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
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
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
                        Text("کیف پول: ${"%,d".format(java.util.Locale.US, teacher.walletBalanceToman)} تومان")
                    }
                    androidx.compose.material3.TextButton(onClick = { transferTarget = teacher; transferAmount = "" }) { Text("شارژ") }
                    androidx.compose.material3.TextButton(onClick = { removeTarget = teacher }) { Text("حذف") }
                }
            }
        }
    }
    transferTarget?.let { teacher ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { transferTarget = null },
            title = { Text("شارژ کیف پول ${teacher.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مبلغ باید مضرب ۱٬۰۰۰ تومان باشد؛ مانند ۲۷٬۰۰۰ تومان.")
                    androidx.compose.material3.OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it.filter(Char::isDigit).take(9) },
                        label = { Text("مبلغ انتقال (تومان)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                val amount = transferAmount.toLongOrNull()
                androidx.compose.material3.Button(
                    enabled = ir.exam.app.domain.model.ManagerWalletRules.isValidTransfer(amount),
                    onClick = {
                        scope.launch {
                            repository.transferWallet(teacher.id, amount ?: 0).onSuccess { result ->
                                message = "${"%,d".format(java.util.Locale.US, result.amountToman)} تومان منتقل شد."
                                transferTarget = null
                                reload()
                            }.onFailure { error = safeManagerError(it); transferTarget = null }
                        }
                    }
                ) { Text("انتقال") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { transferTarget = null }) { Text("انصراف") } }
        )
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
                            .onFailure { error = safeManagerError(it); removeTarget = null }
                    }
                }) { Text("قطع عضویت") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { removeTarget = null }) { Text("انصراف") } }
        )
    }
}

@Composable
fun ManagerStatsScreen() {
    val state = rememberManagerSummary()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text("پاسخ‌ها"); Text(summary.answers.toString(), style = MaterialTheme.typography.titleLarge) } }
                    Card(Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text("میانگین نمره"); Text("${summary.averagePercent}٪", style = MaterialTheme.typography.titleLarge) } }
                }
                Text("مجموع اعتبار توزیع‌شده: ${"%,d".format(java.util.Locale.US, summary.distributedToman)} تومان")
                Text("فعالیت معلم‌ها", style = MaterialTheme.typography.titleMedium)
                summary.teacherActivity.forEach { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text("آزمون ${item.exams} · کلاس ${item.classes} · دانش‌آموز ${item.students}")
                            Text("کیف پول ${"%,d".format(java.util.Locale.US, item.walletBalance)} تومان")
                        }
                    }
                }
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
                exams = raw["exams"]?.jsonPrimitive?.intOrNull ?: 0,
                answers = raw["answers"]?.jsonPrimitive?.intOrNull ?: 0,
                averagePercent = raw["average_percent"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                distributedToman = raw["distributed_toman"]?.jsonPrimitive?.longOrNull ?: 0,
                teacherActivity = (raw["teacher_activity"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { element ->
                    val item = element as? JsonObject ?: return@mapNotNull null
                    TeacherActivity(
                        name = item["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        exams = item["exams"]?.jsonPrimitive?.intOrNull ?: 0,
                        classes = item["classes"]?.jsonPrimitive?.intOrNull ?: 0,
                        students = item["students"]?.jsonPrimitive?.intOrNull ?: 0,
                        walletBalance = item["wallet_balance"]?.jsonPrimitive?.longOrNull ?: 0
                    )
                }
            )
        }.onSuccess { state = ManagerSummaryState(loading = false, summary = it) }
            .onFailure { state = ManagerSummaryState(loading = false, error = safeManagerError(it)) }
    }
    return state
}

private fun safeManagerError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .replace(Regex("(?i)bearer\\s+[A-Za-z0-9._-]+"), "")
    .take(240)
    .ifBlank { "عملیات مدیریت مدرسه ناموفق بود." }
