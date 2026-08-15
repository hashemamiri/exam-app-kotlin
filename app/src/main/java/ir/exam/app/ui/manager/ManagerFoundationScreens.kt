package ir.exam.app.ui.manager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
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
fun ManagerTeachersScreen(
    newTeacherRequested: Int = 0,
    onManageTeacher: (String) -> Unit = {}
) {
    val summaryState = rememberManagerSummary()
    val repository = remember { ir.exam.app.data.repository.SupabaseManagerRepository() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var teachers by remember { mutableStateOf<List<ir.exam.app.data.repository.SchoolTeacherItem>>(emptyList()) }
    var invites by remember { mutableStateOf<List<ir.exam.app.data.repository.ManagerInviteItem>>(emptyList()) }
    var expandedTeacher by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var inviteMode by remember { mutableStateOf(false) }
    var inviteCountDialog by remember { mutableStateOf(false) }
    var inviteCount by remember { mutableStateOf(1) }
    var transferTarget by remember { mutableStateOf<ir.exam.app.data.repository.SchoolTeacherItem?>(null) }
    var transferAmount by remember { mutableStateOf("") }
    var removeTarget by remember { mutableStateOf<ir.exam.app.data.repository.SchoolTeacherItem?>(null) }

    fun reloadTeachers() {
        scope.launch {
            loading = true
            repository.teachers().onSuccess { teachers = it; error = null }
                .onFailure { error = safeManagerError(it) }
            loading = false
        }
    }
    fun reloadInvites() {
        scope.launch {
            loading = true
            repository.invites().onSuccess { invites = it; error = null }
                .onFailure { error = safeManagerError(it) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reloadTeachers() }
    LaunchedEffect(newTeacherRequested) {
        if (newTeacherRequested > 0) { inviteMode = true; reloadInvites() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (inviteMode) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = { inviteMode = false; reloadTeachers() }) {
                    androidx.compose.material3.Icon(Icons.Outlined.ArrowBack, "بازگشت")
                }
                Text("کدهای دعوت معلم", style = MaterialTheme.typography.headlineSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                androidx.compose.material3.Button(onClick = { inviteCountDialog = true }) { Text("ساخت کد دعوت") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            invites.forEach { invite ->
                val remaining = inviteRemainingText(invite.expiresAt, invite.used, invite.revoked)
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(invite.code, style = MaterialTheme.typography.titleLarge)
                            Text(remaining)
                            Text(if (invite.used) "استفاده شده" else "استفاده نشده")
                        }
                        if (!invite.used && !invite.revoked) {
                            androidx.compose.material3.IconButton(onClick = {
                                scope.launch {
                                    repository.revokeInvite(invite.id).onSuccess { reloadInvites() }
                                        .onFailure { error = safeManagerError(it) }
                                }
                            }) {
                                androidx.compose.material3.Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "حذف کد دعوت",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text("معلم‌ها", style = MaterialTheme.typography.headlineSmall)
            summaryState.summary?.let { Text(it.schoolName) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            teachers.forEach { teacher ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        expandedTeacher = if (expandedTeacher == teacher.id) null else teacher.id
                    }
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(teacher.fullName.ifBlank { "بدون نام" }, style = MaterialTheme.typography.titleMedium)
                        Text("کد پرسنلی: ${teacher.employeeCode.ifBlank { "—" }}")
                        Text("شماره تلفن: ${teacher.phone.ifBlank { "—" }}")
                        androidx.compose.animation.AnimatedVisibility(expandedTeacher == teacher.id) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                androidx.compose.material3.IconButton(onClick = {
                                    scope.launch {
                                        repository.setTeacherActive(teacher.id, !teacher.active)
                                            .onSuccess { reloadTeachers() }
                                            .onFailure { error = safeManagerError(it) }
                                    }
                                }) {
                                    androidx.compose.material3.Icon(
                                        if (teacher.active) Icons.Outlined.ToggleOn
                                        else Icons.Outlined.ToggleOff,
                                        contentDescription = if (teacher.active) "غیرفعال‌کردن" else "فعال‌کردن"
                                    )
                                }
                                androidx.compose.material3.IconButton(onClick = { onManageTeacher(teacher.id); message = "مدیریت کلاس معلم در V40C تکمیل می‌شود." }) {
                                    androidx.compose.material3.Icon(Icons.Outlined.Login, "ورود به مدیریت معلم")
                                }
                                androidx.compose.material3.IconButton(onClick = { transferTarget = teacher; transferAmount = "" }) {
                                    androidx.compose.material3.Icon(Icons.Outlined.AccountBalanceWallet, "شارژ کیف پول")
                                }
                                androidx.compose.material3.IconButton(onClick = { removeTarget = teacher }) {
                                    androidx.compose.material3.Icon(Icons.Outlined.Delete, "حذف معلم", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (inviteCountDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { inviteCountDialog = false },
            title = { Text("تعداد کد دعوت") },
            text = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { count ->
                        androidx.compose.material3.FilterChip(
                            selected = inviteCount == count,
                            onClick = { inviteCount = count },
                            label = { Text(count.toString()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    inviteCountDialog = false
                    scope.launch {
                        repository.createInvites(inviteCount).onSuccess { reloadInvites() }
                            .onFailure { error = safeManagerError(it) }
                    }
                }) { Text("تأیید") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { inviteCountDialog = false }) { Text("انصراف") } }
        )
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
                        label = { Text("مبلغ انتقال (تومان)") }, singleLine = true
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
                                transferTarget = null; reloadTeachers()
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
            title = { Text("حذف معلم از مدرسه") },
            text = { Text("عضویت ${teacher.fullName} حذف شود؟ حساب Auth و آزمون‌های شخصی او باقی می‌مانند.") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    scope.launch {
                        repository.removeTeacher(teacher.id).onSuccess { removeTarget = null; reloadTeachers() }
                            .onFailure { error = safeManagerError(it); removeTarget = null }
                    }
                }) { Text("حذف عضویت") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { removeTarget = null }) { Text("انصراف") } }
        )
    }
}

private fun inviteRemainingText(expiresAt: String, used: Boolean, revoked: Boolean): String {
    if (used) return "مصرف شده"
    if (revoked) return "حذف شده"
    val millis = runCatching { java.time.Instant.parse(expiresAt).toEpochMilli() - System.currentTimeMillis() }.getOrDefault(0)
    if (millis <= 0) return "منقضی شده"
    val minutes = millis / 60_000
    return "زمان باقی‌مانده: ${minutes / 60} ساعت و ${minutes % 60} دقیقه"
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
