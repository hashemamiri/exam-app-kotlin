package ir.exam.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.data.repository.ManagerApprovalItem
import ir.exam.app.data.repository.SupabaseTeacherDashboardRepository
import ir.exam.app.ui.app.NeumorphicPanel
import kotlinx.coroutines.launch

@Composable
fun TeacherManagerRequestsScreen() {
    val repository = remember { SupabaseTeacherDashboardRepository() }
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<ManagerApprovalItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() = scope.launch {
        loading = true
        repository.managerRequests()
            .onSuccess { requests = it; error = null }
            .onFailure { error = it.message }
        loading = false
    }
    fun decide(id: String, approve: Boolean) = scope.launch {
        repository.decideManagerRequest(id, approve)
            .onSuccess { load() }
            .onFailure { error = it.message }
    }

    LaunchedEffect(Unit) { load() }
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("درخواست‌های مدیر", style = MaterialTheme.typography.headlineSmall) }
        if (loading) item { CircularProgressIndicator() }
        error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        if (!loading && requests.none { it.status == "pending" }) {
            item { Text("درخواست در انتظار تأیید ندارید.") }
        }
        items(requests, key = { it.id }) { request ->
            NeumorphicPanel(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(request.managerName.ifBlank { "مدیر مدرسه" }, style = MaterialTheme.typography.titleMedium)
                    Text("${if (request.targetType == "class") "کلاس" else "حساب دانش‌آموز"} · ${if (request.action == "delete") "حذف" else "ویرایش"}")
                    Text("وضعیت: ${request.status.toPersianRequestStatus()}")
                    if (request.status == "pending") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { decide(request.id, true) }) { Text("تأیید") }
                            OutlinedButton(onClick = { decide(request.id, false) }) { Text("رد") }
                        }
                    }
                }
            }
        }
    }
}

private fun String.toPersianRequestStatus() = when (this) {
    "pending" -> "در انتظار"
    "approved" -> "تأیید شده"
    "rejected" -> "رد شده"
    "expired" -> "منقضی شده"
    "executed" -> "اجرا شده"
    else -> this
}
