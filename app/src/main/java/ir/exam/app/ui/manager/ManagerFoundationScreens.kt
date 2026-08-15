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
    val state = rememberManagerSummary()
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("معلم‌ها", style = MaterialTheme.typography.headlineSmall)
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
            else -> {
                Text(
                    listOf(state.summary?.schoolName, state.summary?.province, state.summary?.city)
                        .filterNotNull().filter(String::isNotBlank).joinToString(" · ")
                )
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("مدیریت معلم‌های مدرسه", style = MaterialTheme.typography.titleMedium)
                        Text("تعداد معلم فعال: ${state.summary?.teachers ?: 0}")
                        Text("دعوت امن، حذف عضویت و مدیریت کلاس معلم در V37 فعال می‌شود.")
                    }
                }
                if (newTeacherRequested > 0) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("معلم جدید", style = MaterialTheme.typography.titleMedium)
                            Text("درخواست دکمه + دریافت شد؛ فرم دعوت امن در مرحله V37 تکمیل می‌شود.")
                        }
                    }
                }
            }
        }
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
