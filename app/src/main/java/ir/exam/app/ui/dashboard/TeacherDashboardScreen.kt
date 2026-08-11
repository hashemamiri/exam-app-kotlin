package ir.exam.app.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.core.printing.OfficialPrintController
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.ui.builder.ExamImportDraft
import java.io.ByteArrayOutputStream

@Composable
fun TeacherDashboardScreen(
    onCreateExam: () -> Unit,
    onEditExam: (String) -> Unit,
    onImportExam: (ExamImportDraft) -> Unit
) {
    val context = LocalContext.current
    val printController = remember(context.applicationContext) { OfficialPrintController(context.applicationContext) }
    val viewModel = remember { TeacherDashboardViewModel() }
    val state by viewModel.state.collectAsState()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val file = state.exportFile
        if (uri != null && file != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(file.content) }
        }
        viewModel.consumeExport()
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { readExamFileLimited(it) }
                    ?: error("فایل آزمون خوانده نشد.")
            }.onSuccess(viewModel::importExam)
                .onFailure(viewModel::reportError)
        }
    }
    var deleteCandidate by remember { mutableStateOf<ExamDashboardDto?>(null) }
    var duplicateCandidate by remember { mutableStateOf<ExamDashboardDto?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.exportFile) {
        state.exportFile?.let { exportLauncher.launch(it.fileName) }
    }
    LaunchedEffect(state.importDraft) {
        state.importDraft?.let {
            onImportExam(it)
            viewModel.consumeImport()
        }
    }
    LaunchedEffect(state.printExam) {
        state.printExam?.let { printable ->
            runCatching { printController.printExam(context, printable) }
                .onFailure(viewModel::reportError)
            viewModel.consumePrint()
        }
    }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داشبورد معلم", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = viewModel::load) { Text("به‌روزرسانی") }
            Button(onClick = onCreateExam) { Text("ساخت آزمون جدید") }
            OutlinedButton(
                enabled = !state.portabilityLoading,
                onClick = { importLauncher.launch(arrayOf("application/octet-stream", "application/json", "text/plain")) }
            ) { Text("واردکردن آزمون") }
        }
        if (state.actionLoading || state.portabilityLoading) CircularProgressIndicator()
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            state.loading -> CircularProgressIndicator()
            state.exams.isEmpty() -> Text("هنوز آزمونی برای نمایش وجود ندارد.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.exams, key = { it.id }) { exam ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(exam.title.ifBlank { "بدون عنوان" }, style = MaterialTheme.typography.titleMedium)
                            Text("درس: ${exam.subject ?: "بدون درس"}")
                            Text("کد آزمون: ${exam.code ?: "—"}")
                            Text("مدت: ${exam.duration ?: 0} دقیقه · بارم: ${exam.totalScore}")
                            Text(if (exam.isOpen) "وضعیت: باز" else "وضعیت: بسته")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { onEditExam(exam.id) }) { Text("ویرایش") }
                                OutlinedButton(onClick = { viewModel.setOpen(exam) }) {
                                    Text(if (exam.isOpen) "بستن" else "بازکردن")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { duplicateCandidate = exam }) { Text("تکثیر با کسر هزینه") }
                                OutlinedButton(onClick = { viewModel.exportExam(exam.id) }) { Text("صادرکردن") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { viewModel.preparePrint(exam.id, false) }) { Text("چاپ برگه") }
                                OutlinedButton(onClick = { viewModel.preparePrint(exam.id, true) }) { Text("چاپ با کلید") }
                                TextButton(onClick = { deleteCandidate = exam }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }

    duplicateCandidate?.let { exam ->
        AlertDialog(
            onDismissRequest = { duplicateCandidate = null },
            title = { Text("تکثیر آزمون") },
            text = { Text("کپی آزمون «${exam.title}» مثل یک آزمون جدید است و هزینه همه سؤال‌های آن با نرخ هر سؤال ۱٬۰۰۰ تومان به‌صورت اتمیک کسر می‌شود. ادامه می‌دهید؟") },
            confirmButton = {
                Button(onClick = { viewModel.duplicate(exam); duplicateCandidate = null }) { Text("تأیید و تکثیر") }
            },
            dismissButton = { TextButton(onClick = { duplicateCandidate = null }) { Text("انصراف") } }
        )
    }

    deleteCandidate?.let { exam ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("حذف آزمون") },
            text = { Text("آزمون «${exam.title}» و پاسخ‌ها، تلاش‌ها و مخاطبان وابسته حذف شوند؟ این کار برگشت‌پذیر نیست.") },
            confirmButton = {
                Button(onClick = { viewModel.delete(exam); deleteCandidate = null }) { Text("حذف کامل") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("انصراف") } }
        )
    }
}

private fun readExamFileLimited(input: java.io.InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= 8 * 1024 * 1024) { "حجم فایل آزمون بیش از ۸ مگابایت است." }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
