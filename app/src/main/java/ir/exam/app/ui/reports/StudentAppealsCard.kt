package ir.exam.app.ui.reports

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
import ir.exam.app.domain.model.AppealStatus
import ir.exam.app.domain.model.GradeAppeal
import ir.exam.app.domain.model.StudentAnswerSummary

/**
 * V354 — کارت اعتراض به نمره در صفحهٔ نتایج دانش‌آموز.
 *
 * دانش‌آموز برای هر برگهٔ تصحیح‌شده می‌تواند یک اعتراض باز ثبت کند و
 * وضعیت آن (در انتظار / نزد مصحح دوم / پذیرفته / رد) را پیگیری کند.
 * اعتراض فقط پس از نهایی‌شدن تصحیح ممکن است — همان قید پایگاه‌داده.
 */
@Composable
internal fun StudentAppealsCard(
    answers: List<StudentAnswerSummary>,
    appeals: List<GradeAppeal>,
    busy: Boolean,
    message: String?,
    onSubmit: (String, String) -> Unit,
    onClearMessage: () -> Unit
) {
    var target by remember { mutableStateOf<StudentAnswerSummary?>(null) }
    val graded = answers.filter { it.graded }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("اعتراض به نمره", style = MaterialTheme.typography.titleMedium)
            if (graded.isEmpty()) {
                Text(
                    "پس از نهایی‌شدن تصحیح، می‌توانید برای هر آزمون اعتراض ثبت کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            Text(
                "برای هر آزمون یک اعتراض باز می‌توانید داشته باشید. دلیل را روشن بنویسید تا معلم بتواند بررسی کند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            graded.forEach { answer ->
                val open = appeals.any { it.answerId == answer.id && it.status.isOpen }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(answer.title, modifier = Modifier.weight(1f), maxLines = 1)
                    if (open) {
                        Text("اعتراض باز دارید", style = MaterialTheme.typography.labelMedium)
                    } else {
                        OutlinedButton(enabled = !busy, onClick = { target = answer }) { Text("ثبت اعتراض") }
                    }
                }
            }
            if (appeals.isNotEmpty()) {
                HorizontalDivider()
                Text("پیگیری اعتراض‌ها", style = MaterialTheme.typography.titleSmall)
                appeals.forEach { appeal -> AppealStatusRow(appeal) }
            }
        }
    }

    val picked = target
    if (picked != null) {
        AppealReasonDialog(
            title = picked.title,
            busy = busy,
            onDismiss = { target = null },
            onConfirm = { reason ->
                onSubmit(picked.id, reason)
                target = null
            }
        )
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = onClearMessage,
            title = { Text("اعتراض به نمره") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onClearMessage) { Text("باشه") } }
        )
    }
}

@Composable
private fun AppealStatusRow(appeal: GradeAppeal) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                appeal.examTitle ?: "آزمون",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                appeal.status.label,
                style = MaterialTheme.typography.labelMedium,
                color = when (appeal.status) {
                    AppealStatus.ACCEPTED -> MaterialTheme.colorScheme.primary
                    AppealStatus.REJECTED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Text(appeal.reason, style = MaterialTheme.typography.bodySmall)
        appeal.secondScore?.let {
            Text(
                "نمرهٔ مصحح دوم: ${PersianDigits.convert(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        appeal.resolutionNote?.let { Text("پاسخ معلم: $it", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AppealReasonDialog(
    title: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val error = GradeAppeal.reasonError(reason)
    val touched = reason.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اعتراض به نمره") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("دلیل اعتراض") },
                    isError = touched && error != null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                if (touched && error != null) Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "${PersianDigits.convert(reason.trim().length)} نویسه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && error == null, onClick = { onConfirm(reason) }) { Text("ثبت اعتراض") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
