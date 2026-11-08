package ir.exam.app.ui.grading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import ir.exam.app.domain.model.GradeHistoryRow

/**
 * V354 — تب‌های «اعتراض‌ها» و «تاریخچهٔ نمره» در صفحهٔ تصحیح.
 *
 * قرارداد مهم: پذیرش اعتراض نمره را خودکار تغییر نمی‌دهد. معلم باید
 * آگاهانه در تب تصحیح نمره را اصلاح کند تا آن تغییر در تاریخچه ثبت شود.
 * این رفتار با پایگاه‌داده و سایت یکسان است.
 */

@Composable
internal fun AppealsContent(state: GradingUiState, viewModel: GradingViewModel) {
    var resolving by remember { mutableStateOf<Pair<GradeAppeal, String>?>(null) }
    val rows = state.appeals
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "اعتراض‌های این آزمون. با «مصحح دوم» می‌توانید نمرهٔ بازبینی را ثبت کنید؛ " +
                "پذیرش اعتراض نمره را خودکار عوض نمی‌کند و باید در تب تصحیح اصلاح شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = viewModel::loadAppeals, enabled = !state.actionLoading) { Text("تازه‌سازی") }
        if (rows.isEmpty()) {
            Text("اعتراضی برای این آزمون ثبت نشده است.")
            return@Column
        }
        val openCount = rows.count { it.status.isOpen }
        Text(
            "${PersianDigits.convert(rows.size.toString())} اعتراض · " +
                "${PersianDigits.convert(openCount.toString())} باز",
            style = MaterialTheme.typography.labelLarge
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.id }) { appeal ->
                AppealCard(appeal, enabled = !state.actionLoading) { status -> resolving = appeal to status }
            }
        }
    }
    val pending = resolving
    if (pending != null) {
        AppealResolveDialog(
            appeal = pending.first,
            status = pending.second,
            onDismiss = { resolving = null },
            onConfirm = { note, score ->
                viewModel.resolveAppeal(pending.first.id, pending.second, note, score)
                resolving = null
            }
        )
    }
}

@Composable
private fun AppealCard(appeal: GradeAppeal, enabled: Boolean, onResolve: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    appeal.studentName ?: "دانش‌آموز",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(appeal.targetLabel, style = MaterialTheme.typography.labelMedium)
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
            Text(appeal.reason, style = MaterialTheme.typography.bodyMedium)
            appeal.secondScore?.let {
                Text(
                    "نمرهٔ مصحح دوم: ${PersianDigits.convert(it.toString())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            appeal.secondNote?.let { Text("یادداشت بازبینی: $it", style = MaterialTheme.typography.bodySmall) }
            appeal.resolutionNote?.let { Text("نتیجه: $it", style = MaterialTheme.typography.bodySmall) }
            if (appeal.status.isOpen) {
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = false, enabled = enabled, onClick = { onResolve("second") }, label = { Text("مصحح دوم") })
                    FilterChip(selected = false, enabled = enabled, onClick = { onResolve("accepted") }, label = { Text("پذیرش") })
                    FilterChip(selected = false, enabled = enabled, onClick = { onResolve("rejected") }, label = { Text("رد") })
                }
            }
        }
    }
}

@Composable
private fun AppealResolveDialog(
    appeal: GradeAppeal,
    status: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double?) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    val isSecond = status == "second"
    val parsed = PersianDigits.latin(score.trim()).replace('٫', '.').toDoubleOrNull()
    val noteOk = if (isSecond) true else note.trim().length >= 3
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (status) {
                    "second" -> "ارجاع به مصحح دوم"
                    "accepted" -> "پذیرش اعتراض"
                    else -> "رد اعتراض"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appeal.reason, style = MaterialTheme.typography.bodySmall)
                if (isSecond) {
                    OutlinedTextField(
                        value = score,
                        onValueChange = { score = it },
                        singleLine = true,
                        label = { Text("نمرهٔ بازبینی (اختیاری)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isSecond) "یادداشت بازبینی" else "توضیح نتیجه برای دانش‌آموز") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!noteOk) Text(
                    "توضیح نتیجه دست‌کم ۳ نویسه باشد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                if (status == "accepted") Text(
                    "توجه: نمره خودکار تغییر نمی‌کند. پس از پذیرش، در تب تصحیح نمره را اصلاح کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(enabled = noteOk, onClick = { onConfirm(note, if (isSecond) parsed else null) }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
internal fun GradeHistoryContent(state: GradingUiState, viewModel: GradingViewModel) {
    val rows = state.gradeHistory
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "هر تغییر نمره به‌طور خودکار ثبت می‌شود و قابل پاک‌کردن یا ویرایش نیست.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = viewModel::loadGradeHistory, enabled = !state.actionLoading) { Text("تازه‌سازی") }
        if (rows.isEmpty()) {
            Text("هنوز تغییری در نمره‌های این آزمون ثبت نشده است.")
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.id }) { row -> HistoryCard(row) }
        }
    }
}

@Composable
private fun HistoryCard(row: GradeHistoryRow) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(row.studentName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("توسط ${row.changedByName}", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                "${scoreText(row.beforeScore)} ← ${scoreText(row.afterScore)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.scoreChanged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (row.beforeGraded != row.afterGraded) Text(
                if (row.afterGraded == true) "وضعیت: تصحیح نهایی شد" else "وضعیت: تصحیح به حالت بازنگری برگشت",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun scoreText(value: Double?): String =
    if (value == null) "—" else PersianDigits.convert(value.toString())
