package ir.exam.app.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.data.repository.ManagerClassRoster
import ir.exam.app.data.repository.ManagerStudentItem
import ir.exam.app.data.repository.SupabaseManagerRepository
import ir.exam.app.domain.model.SchoolClass
import kotlinx.coroutines.launch

/**
 * V337 — اعضای کلاسِ انتخاب‌شده از «مدارس» مدیر.
 * برخلاف مسیر معلم، این صفحه فقط از RPCهای مدیر استفاده می‌کند:
 * native_manager_class_roster_v40c / native_manager_school_students_v40c /
 * native_manager_set_class_student_v40c. حذف، فقط عضویت کلاس را برمی‌دارد و
 * حساب دانش‌آموز را حذف نمی‌کند.
 */
@Composable
internal fun ManagerSchoolClassRosterScreen(
    schoolClass: SchoolClass,
    schoolName: String,
    onBack: () -> Unit
) {
    val repository = remember { SupabaseManagerRepository() }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var roster by remember { mutableStateOf<ManagerClassRoster?>(null) }
    var schoolStudents by remember { mutableStateOf<List<ManagerStudentItem>>(emptyList()) }
    var pickExistingOpen by remember { mutableStateOf(false) }
    var pickQuery by remember { mutableStateOf("") }
    var removing by remember { mutableStateOf<ManagerStudentItem?>(null) }

    suspend fun reload() {
        loading = true
        error = null
        repository.classRoster(schoolClass.id)
            .onSuccess { roster = it }
            .onFailure { error = safeManagerError(it) }
        repository.schoolStudents()
            .onSuccess { schoolStudents = it }
            .onFailure { if (error == null) error = safeManagerError(it) }
        loading = false
    }

    LaunchedEffect(schoolClass.id) { reload() }

    fun setMembership(studentId: String, add: Boolean) {
        if (busy) return
        busy = true
        error = null
        message = null
        scope.launch {
            repository.setClassStudent(schoolClass.id, studentId, add)
                .onSuccess {
                    message = if (add) "دانش‌آموز به کلاس اضافه شد." else "دانش‌آموز از کلاس حذف شد."
                    reload()
                }
                .onFailure { error = safeManagerError(it) }
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "بازگشت به کلاس‌های مدرسه")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    roster?.className?.ifBlank { schoolClass.name } ?: schoolClass.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "کلاس‌های ${schoolName.ifBlank { "مدرسه" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (loading || busy) CircularProgressIndicator()

        Button(
            onClick = { pickQuery = ""; pickExistingOpen = true },
            enabled = !loading && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("افزودن دانش‌آموز موجود به کلاس")
        }
        Text(
            "فهرست، دانش‌آموزان قابل مشاهدهٔ مدیر است؛ سرور فقط اجازهٔ افزودن دانش‌آموزِ همان مدرسهٔ کلاس را می‌دهد.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("اعضای کلاس", style = MaterialTheme.typography.titleMedium)
        val members = roster?.items.orEmpty()
        if (!loading && members.isEmpty()) {
            Text("این کلاس هنوز عضوی ندارد؛ با دکمهٔ بالا دانش‌آموز اضافه کنید.")
        }
        members.forEach { student ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(student.fullName.ifBlank { student.username }, style = MaterialTheme.typography.titleSmall)
                        if (student.username.isNotBlank()) {
                            Text(student.username, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { removing = student }, enabled = !busy) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "حذف از کلاس",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (pickExistingOpen) {
        val memberIds = roster?.items.orEmpty().mapTo(hashSetOf()) { it.id }
        val candidates = schoolStudents
            .filter { it.id !in memberIds }
            .filter {
                pickQuery.isBlank() ||
                    it.fullName.contains(pickQuery, ignoreCase = true) ||
                    it.username.contains(pickQuery, ignoreCase = true)
            }
        AlertDialog(
            onDismissRequest = { pickExistingOpen = false },
            title = { Text("افزودن دانش‌آموز موجود") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pickQuery,
                        onValueChange = { pickQuery = it.take(80) },
                        label = { Text("جست‌وجوی نام یا نام کاربری") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (candidates.isEmpty()) {
                        Text("دانش‌آموزی مطابق جست‌وجو پیدا نشد.")
                    }
                    LazyColumn(
                        Modifier.heightIn(max = 380.dp).imePadding(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(candidates, key = { it.id }) { student ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(student.fullName.ifBlank { student.username })
                                        if (student.username.isNotBlank()) {
                                            Text(student.username, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    TextButton(
                                        onClick = { setMembership(student.id, add = true) },
                                        enabled = !busy
                                    ) { Text("افزودن") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickExistingOpen = false }) { Text("بستن") } }
        )
    }

    removing?.let { student ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("حذف از کلاس") },
            text = { Text("«${student.fullName.ifBlank { student.username }}» از این کلاس حذف شود؟ حساب دانش‌آموز حذف نمی‌شود.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        removing = null
                        setMembership(student.id, add = false)
                    },
                    enabled = !busy
                ) { Text("حذف از کلاس") }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("انصراف") } }
        )
    }
}
