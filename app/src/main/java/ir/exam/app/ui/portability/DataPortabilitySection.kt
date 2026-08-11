package ir.exam.app.ui.portability

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

@Composable
fun DataPortabilitySection() {
    val context = LocalContext.current
    val viewModel = remember { DataPortabilityViewModel() }
    val state by viewModel.state.collectAsState()
    var confirmCleanup by remember { mutableStateOf(false) }
    val createFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val file = state.exportFile
        if (uri != null && file != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(file.content) }
        }
        viewModel.consumeExport()
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { readUtf8Limited(it, 20 * 1024 * 1024) }
                    ?: error("فایل پشتیبان خوانده نشد.")
            }.onSuccess(viewModel::parseBackup)
                .onFailure(viewModel::reportError)
        }
    }
    LaunchedEffect(state.exportFile) {
        state.exportFile?.let { createFile.launch(it.fileName) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("پشتیبان کامل Native", style = MaterialTheme.typography.titleMedium)
                Text("آزمون‌ها و کلیدها، کلاس‌ها، عضویت‌ها و سربرگ در JSON نسخه‌دار ذخیره می‌شوند.")
                Text(
                    "رمز دانش‌آموز، token، کلید API و plain_password عمداً وارد فایل نمی‌شوند.",
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::exportBackup, enabled = !state.loading) { Text("ساخت پشتیبان") }
                    OutlinedButton(
                        enabled = !state.loading,
                        onClick = { openFile.launch(arrayOf("application/json", "text/plain")) }
                    ) { Text("بازیابی") }
                }
                if (state.loading) CircularProgressIndicator()
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("قانون بازیابی", style = MaterialTheme.typography.titleMedium)
                Text("داده موجود پاک نمی‌شود؛ آزمون‌ها با شناسه و کد تازه ساخته می‌شوند.")
                Text("ساخت آزمون‌های بازیابی‌شده طبق کیف پول V9 و به‌صورت اتمیک محاسبه می‌شود.")
                Text("عضویت فقط برای دانش‌آموزانی بازیابی می‌شود که اکنون با همان نام کاربری زیر حساب شما وجود دارند.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نگهداری امن Storage", style = MaterialTheme.typography.titleMedium)
                Text("ابتدا فقط بررسی کنید. حذف واقعی نیازمند دو Secret مدیریتی و شناسه حساب مجاز در Edge Function است.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::checkStorage, enabled = !state.loading) { Text("بررسی بدون حذف") }
                    Button(onClick = { confirmCleanup = true }, enabled = !state.loading) { Text("پاک‌سازی مجاز") }
                }
                state.maintenance?.let { result ->
                    Text(
                        "فایل آزمون اسکن‌شده: ${result.scannedExamObjects} · orphan: ${result.orphanCandidates} · " +
                            "APK اسکن‌شده: ${result.scannedApks} · قدیمی: ${result.apkCandidates} · " +
                            "حذف‌شده: ${result.deletedObjects + result.deletedApks}"
                    )
                }
            }
        }
    }

    state.preview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPreview,
            title = { Text("پیش‌نمایش پشتیبان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("معلم: ${preview.teacherName ?: "—"}")
                    Text("${preview.examCount} آزمون · ${preview.totalQuestionCount} سؤال · ${preview.classCount} کلاس · ${preview.membershipCount} عضویت")
                    if (state.options.exams) {
                        Text("هزینه حداکثر بازیابی آزمون‌ها: ${preview.totalQuestionCount * 1_000L} تومان")
                    }
                    OptionRow("آزمون‌ها و کلید پاسخ", state.options.exams, viewModel::setExams)
                    OptionRow("کلاس‌ها", state.options.classes, viewModel::setClasses)
                    OptionRow("عضویت کلاس‌ها", state.options.memberships, viewModel::setMemberships)
                    OptionRow("اطلاعات سربرگ", state.options.header, viewModel::setHeader)
                    Text("عملیات بازیابی تراکنشی است؛ در خطا هیچ بخش یا هزینه‌ای نیمه‌کاره نمی‌ماند.")
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::restore, enabled = !state.loading) { Text("تأیید بازیابی") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissPreview) { Text("انصراف") } }
        )
    }

    if (confirmCleanup) {
        AlertDialog(
            onDismissRequest = { confirmCleanup = false },
            title = { Text("تأیید پاک‌سازی واقعی") },
            text = { Text("فقط فایل‌های بدون مرجع و قدیمی‌تر از ۷ روز و APKهای خارج از retention حذف می‌شوند. این عملیات برگشت‌پذیر نیست.") },
            confirmButton = {
                Button(onClick = { confirmCleanup = false; viewModel.cleanStorage() }) { Text("تأیید حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmCleanup = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun OptionRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun readUtf8Limited(input: java.io.InputStream, maxBytes: Int): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "حجم فایل پشتیبان بیش از ۲۰ مگابایت است." }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
