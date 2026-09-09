package ir.exam.app.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.ui.app.NeumorphicPanel
import ir.exam.app.ui.builder.ExamImportDraft
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    refreshKey: Int = 0,
    onCreateExam: () -> Unit,
    onEditExam: (String) -> Unit,
    onImportExam: (ExamImportDraft) -> Unit,
    // V113.2 — انتخاب آزمونِ چاپیِ محلی از پنجرهٔ «آزمون‌های چاپی» → ساخت آزمون آنلاین از روی آن
    onOpenPrintExam: (String) -> Unit = {}
) {
    val context = LocalContext.current
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
    // V75.4 — پیش از صدور، معلم باید روشن کند که پاسخنامه همراه فایل باشد یا نه.
    var exportCandidate by remember { mutableStateOf<ExamDashboardDto?>(null) }
    var expandedExamId by remember { mutableStateOf<String?>(null) }
    // V113 — پنجرهٔ کارت‌های آزمون‌های چاپی (ذخیره‌شده روی دستگاه)
    var printExamsOpen by remember { mutableStateOf(false) }
    val printExamStore = remember(context.applicationContext) {
        ir.exam.app.data.local.PrintExamStore(context.applicationContext)
    }

    LaunchedEffect(refreshKey) { viewModel.load() }
    LaunchedEffect(state.exportFile) {
        state.exportFile?.let { exportLauncher.launch(it.fileName) }
    }
    LaunchedEffect(state.importDraft) {
        state.importDraft?.let {
            onImportExam(it)
            viewModel.consumeImport()
        }
    }
    // V120 — LaunchedEffect(state.printExam) حذف شد: هدفش صدا زدنِ
    // OfficialPrintController.printExam (موتورِ PDF بومیِ جداگانه) بود اما
    // state.printExam هیچ‌وقت مقداردهی نمی‌شد (preparePrint حذف شد، رجوع
    // کنید به TeacherDashboardViewModel) — کدِ کاملاً بدونِ اثر بود.
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::load,
        modifier = Modifier.fillMaxSize()
    ) {
        // V56.1 — تبلت: فهرست آزمون‌ها وسط صفحه با سقف پهنا.
        val tabletDash = ir.exam.app.core.ui.LocalTabletLayout.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (tabletDash) Modifier.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 760.dp)
                    else Modifier
                ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // V113 — راست: «آزمون‌های چاپی»؛ وسط: + (ساخت آزمون آنلاین)؛ چپ: واردکردن.
                // این صفحه به‌صورت پیش‌فرض فقط کارت‌های آزمون آنلاین را نشان می‌دهد.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { printExamsOpen = true }) { Text("آزمون‌های چاپی") }
                    FilledIconButton(onClick = onCreateExam) {
                        Icon(Icons.Outlined.Add, contentDescription = "ساخت آزمون جدید")
                    }
                    OutlinedButton(
                        enabled = !state.portabilityLoading,
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/octet-stream", "application/json", "text/plain")
                            )
                        }
                    ) { Text("واردکردن") }
                }
            }
            if (state.actionLoading || state.portabilityLoading) {
                item { CircularProgressIndicator() }
            }
            state.message?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.primary) }
            }
            state.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            when {
                state.loading && state.exams.isEmpty() -> {
                    item { Text("در حال دریافت آزمون‌ها…") }
                }
                state.exams.isEmpty() -> {
                    item { Text("هنوز آزمونی برای نمایش وجود ندارد.") }
                }
                else -> items(state.exams, key = { it.id }) { exam ->
                    NeumorphicPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedExamId = if (expandedExamId == exam.id) null else exam.id
                            },
                        radius = 18.dp,
                        depth = 8.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    exam.title.ifBlank { "بدون عنوان" },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    if (exam.isOpen) "باز" else "بسته",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                "${exam.subject ?: "بدون درس"} · ${exam.code ?: "—"} · ${exam.duration ?: 0} دقیقه · بارم ${exam.totalScore}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            AnimatedVisibility(
                                visible = expandedExamId == exam.id,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                // V132 — همهٔ عملیاتِ کارت به‌صورتِ آیکن در یک سطر:
                                // ویرایش، بازکردن/بستن، تکثیر (با کسر هزینه)، صادرکردن، حذف.
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ExamCardAction(Icons.Outlined.Edit, "ویرایش") { onEditExam(exam.id) }
                                    ExamCardAction(
                                        if (exam.isOpen) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                        if (exam.isOpen) "بستن" else "بازکردن"
                                    ) { viewModel.setOpen(exam) }
                                    ExamCardAction(Icons.Outlined.ContentCopy, "تکثیر") { duplicateCandidate = exam }
                                    ExamCardAction(Icons.Outlined.IosShare, "صادرکردن") { exportCandidate = exam }
                                    ExamCardAction(Icons.Outlined.Delete, "حذف", tint = MaterialTheme.colorScheme.error) { deleteCandidate = exam }
                                }
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

    exportCandidate?.let { exam ->
        AlertDialog(
            onDismissRequest = { exportCandidate = null },
            title = { Text("صدور فایل آزمون") },
            text = {
                Text(
                    "فایل «${exam.title}» در حالت عادی همراه پاسخنامه است. " +
                        "اگر این فایل را برای کسی می‌فرستید «بدون پاسخنامه» را انتخاب کنید؛ " +
                        "برای آرشیو و بازگردانی روی حساب خودتان «همراه پاسخنامه» را انتخاب کنید."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.exportExam(exam.id, false); exportCandidate = null }) {
                    Text("بدون پاسخنامه")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.exportExam(exam.id, true); exportCandidate = null }) {
                    Text("همراه پاسخنامه")
                }
            }
        )
    }

    // V113.2 — پنجرهٔ «آزمون‌های چاپی»: کارت‌های ذخیره‌شده روی دستگاه؛ لمس هر
    // کارت، ساختِ آزمونِ آنلاین با همان سؤال‌ها را باز می‌کند.
    if (printExamsOpen) {
        val printExams = remember(printExamsOpen) { printExamStore.list() }
        AlertDialog(
            onDismissRequest = { printExamsOpen = false },
            title = { Text("آزمون‌های چاپی") },
            text = {
                if (printExams.isEmpty()) Text("هنوز آزمون چاپی‌ای ذخیره نشده است. از بخش «چاپ آزمون» بسازید.")
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("با انتخاب هر آزمون، آزمون آنلاین جدیدی با همان سؤال‌ها ساخته می‌شود.", style = MaterialTheme.typography.bodySmall)
                    PrintExamCards(printExams) { printExamsOpen = false; onOpenPrintExam(it) }
                }
            },
            confirmButton = { TextButton(onClick = { printExamsOpen = false }) { Text("بستن") } }
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

/** V132 — یک عملِ کارتِ آزمون: آیکن + برچسبِ کوچک زیرش (همه در یک سطر). */
@Composable
private fun ExamCardAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = label, tint = tint) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** V113.2 — کارت‌های آزمون‌های چاپیِ دستگاه (پنجرهٔ «آزمون‌های چاپی»). */
@Composable
private fun PrintExamCards(
    printExams: List<ir.exam.app.data.local.PrintExamRecord>,
    onPick: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxWidth().heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(printExams, key = { it.id }) { rec ->
            Card(Modifier.fillMaxWidth().clickable { onPick(rec.id) }) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(rec.title.ifBlank { "آزمون چاپی" }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${rec.subject.ifBlank { "بدون درس" }} · ${rec.questions.size} سؤال", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
