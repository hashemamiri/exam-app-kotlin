package ir.exam.app.ui.grading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.domain.exam.ExamTiming

/**
 * V357 — تب زمان‌بندی: زمان اضافهٔ فردی و محدودیت زمان هر سؤال.
 *
 * مهلت را **سرور** تعیین می‌کند. این صفحه فقط مجوز و قاعده را ثبت
 * می‌کند؛ زمان اضافه دلیل اجباری دارد تا تصمیم قابل حسابرسی باشد.
 */
@Composable
internal fun GradingTimingContent(
    state: GradingUiState,
    viewModel: GradingViewModel
) {
    var limitsOpen by remember { mutableStateOf(false) }
    var grantTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    val active = state.timeGrants.count { it.extraMinutes > 0 }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "محدودیت زمان سؤال‌ها",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (state.questionLimits.isEmpty()) "بدون محدودیت"
                        else "${PersianDigits.convert(state.questionLimits.size)} قاعده",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    "پس از پایان زمان هر سؤال، همان سؤال قفل می‌شود؛ بقیهٔ آزمون باز می‌ماند. " +
                        "قفل سمت سرور اعمال می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { limitsOpen = true }) { Text("ویرایش محدودیت‌ها") }
            }
        }

        Text(
            "زمان اضافهٔ فردی — ${PersianDigits.convert(active)} دانش‌آموز",
            style = MaterialTheme.typography.titleSmall
        )
        if (state.timeGrants.isEmpty()) {
            Text(
                "هنوز مجوزی ثبت نشده است. از فهرست دانش‌آموزان، زمان اضافه را با دلیل ثبت کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        state.timeGrants.forEach { g ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            g.studentId,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            if (g.extraMinutes > 0) "${PersianDigits.convert(g.extraMinutes)} دقیقه" else "لغو شده",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(g.reason, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    TextButton(onClick = { grantTarget = g.studentId to g.studentId }) { Text("ویرایش") }
                }
            }
        }
        OutlinedButton(onClick = { grantTarget = "" to "دانش‌آموز تازه" }) { Text("افزودن زمان اضافه") }
    }

    if (limitsOpen) {
        QuestionLimitsDialog(
            initial = ExamTiming.formatLimits(state.questionLimits),
            onDismiss = { limitsOpen = false },
            onSave = { text -> viewModel.saveQuestionLimits(text); limitsOpen = false }
        )
    }
    grantTarget?.let { (sid, name) ->
        TimeGrantDialog(
            studentName = name,
            initialMinutes = viewModel.grantFor(sid)?.extraMinutes ?: 15,
            initialReason = viewModel.grantFor(sid)?.reason.orEmpty(),
            onDismiss = { grantTarget = null },
            onSave = { id, minutes, reason ->
                viewModel.saveTimeGrant(id, minutes, reason)
                grantTarget = null
            }
        )
    }
}

@Composable
private fun QuestionLimitsDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("محدودیت زمان هر سؤال") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "هر خط: «شمارهٔ سؤال = ثانیه». «همه» پیش‌فرض همهٔ سؤال‌ها است. " +
                        "رقم فارسی هم پذیرفته می‌شود. ساعت با نخستین بازدید هر سؤال روشن " +
                        "می‌شود و بازگشت به سؤال آن را از نو شروع نمی‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("همه = 300") },
                    minLines = 5, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = ExamTiming.parseLimits(text)
                if (p.errors.isNotEmpty()) {
                    val e = p.errors.first()
                    error = "خط ${PersianDigits.convert(e.line)}: ${e.message}"
                    return@TextButton
                }
                error = null
                onSave(text)
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun TimeGrantDialog(
    studentName: String,
    initialMinutes: Int,
    initialReason: String,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit
) {
    var studentId by remember { mutableStateOf(if (studentName == "دانش‌آموز تازه") "" else studentName) }
    var minutes by remember { mutableStateOf(initialMinutes.toString()) }
    var reason by remember { mutableStateOf(initialReason) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("زمان اضافه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = studentId, onValueChange = { studentId = it },
                    label = { Text("شناسهٔ دانش‌آموز") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minutes, onValueChange = { minutes = it },
                    label = { Text("دقیقهٔ اضافه (۰ = لغو)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    label = { Text("دلیل (اجباری)") },
                    minLines = 2, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (studentId.isBlank()) { error = "شناسهٔ دانش‌آموز را بنویسید."; return@TextButton }
                val m = ExamTiming.latinDigits(minutes).trim().toIntOrNull()
                if (m == null) { error = "دقیقه باید عدد صحیح باشد."; return@TextButton }
                val err = ExamTiming.validateGrant(m, reason)
                if (err.isNotEmpty()) { error = err; return@TextButton }
                error = null
                onSave(studentId.trim(), m, reason.trim())
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
