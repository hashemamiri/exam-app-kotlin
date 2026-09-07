package ir.exam.app.ui.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.exam.app.data.repository.SupabasePortabilityRepository
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.ui.dashboard.TeacherDashboardViewModel
import kotlinx.coroutines.launch

/**
 * صفحهٔ «چاپ آزمون».
 *
 * آزمون‌های سرور یا نسخه‌های چاپیِ محلی را به چاپ مستقیم می‌فرستد. مدادِ
 * نسخهٔ محلی سازندهٔ بومی را باز می‌کند.
 */
@Composable
fun ExamPrintCenterScreen(
    // «آزمون جدید» سازندهٔ بومی را باز می‌کند.
    onNewNativeExam: () -> Unit = {},
    // V86.8 — ویرایشِ آزمونِ چاپیِ ذخیره‌شده روی دستگاه.
    onOpenLocalPrintExam: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { TeacherDashboardViewModel() }
    val state by viewModel.state.collectAsState()
    val portability = remember { SupabasePortabilityRepository() }
    // سربرگ پیش‌فرض از پروفایل ساخته می‌شود و مقدارهای تکمیلیِ تنظیمات سربرگ
    // از دستگاه خوانده می‌شوند.
    val headerStore = remember(context.applicationContext) {
        ir.exam.app.data.local.PrintHeaderStore(context.applicationContext)
    }
    // V86.8 — آزمون‌های چاپیِ ذخیره‌شده روی دستگاه، کنارِ آزمون‌های سرور.
    val printExamStore = remember(context.applicationContext) {
        ir.exam.app.data.local.PrintExamStore(context.applicationContext)
    }
    var localExams by remember { mutableStateOf(printExamStore.list()) }
    var pdfPrintLoading by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf<String?>(null) }
    var printStatusIsError by remember { mutableStateOf(false) }
    // هدف انتخاب‌شده برای چاپ مستقیم.
    var printTarget by remember { mutableStateOf<PrintTarget?>(null) }
    val scope = rememberCoroutineScope()
    // چاپ مستقیم نیز نخست یک PDF A4 بومی می‌سازد؛ پنل Android همان فایل
    // immutable را مستقیماً دریافت می‌کند.
    // V101 — برای ساختِ «نسخهٔ چاپی» از آزمونِ آنلاین، آزمون کامل (با کلید)
    // با همان مسیرِ آزمون‌ساز بارگذاری می‌شود.
    val builderRepo = remember(context.applicationContext) {
        ir.exam.app.data.repository.SupabaseExamBuilderRepository(context.applicationContext)
    }
    var copyLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
        // بازگشت از آزمون‌ساز ممکن است آزمونِ چاپیِ تازه‌ای ساخته باشد
        localExams = printExamStore.list()
    }

    // چاپ مستقیم: PDF immutable بومی یک‌بار ساخته و همان فایل به Print
    // Framework تحویل می‌شود. نسخهٔ استاد فقط در PDF پاسخ‌ها را فعال می‌کند.
    fun startPrint(target: PrintTarget, mode: String) {
        printTarget = null
        scope.launch {
            pdfPrintLoading = true
            try {
                printStatus = "در حال ساخت PDF A4..."
                printStatusIsError = false
                val headerFields = headerStore.read()
                val header = ir.exam.app.data.local.printHeaderOf(headerFields)
                val printable = when (target) {
                    is PrintTarget.ServerExam -> portability.printableExam(
                        target.examId,
                        includeAnswerKey = mode == "teacher",
                        headerOverride = header
                    ).getOrElse { throw it }

                    is PrintTarget.LocalExam -> ir.exam.app.domain.model.PrintableFromDrafts.build(
                        title = target.rec.title.ifBlank { "آزمون" },
                        subject = target.rec.subject,
                        header = header,
                        questions = target.rec.questions,
                        includeAnswerKey = mode == "teacher"
                    )
                }
                NativeExamPrintLauncher.print(context, printable, headerFields, mode)
                    .onSuccess {
                        printStatus = "پنجرهٔ چاپ باز شد."
                    }
                    .onFailure { error ->
                        printStatusIsError = true
                        printStatus = sanitizePrintError(error)
                    }
            } catch (error: Throwable) {
                printStatusIsError = true
                printStatus = sanitizePrintError(error)
            } finally {
                pdfPrintLoading = false
            }
        }
    }

    // V101 — مداد روی کارتِ آزمونِ آنلاین: «نسخهٔ چاپی» از آن آزمون می‌سازد
    // (سؤالات کامل، با کلیدِ جواب) و در آزمون‌سازِ بومی باز می‌کند. ویرایش و
    // ذخیرهٔ این نسخه فقط در بخش چاپ آزمون می‌ماند (PrintExamStore) و هرگز
    // به آزمونِ سرور/دانش‌آموز برنمی‌گردد. اگر نسخهٔ چاپی قبلاً ساخته شده
    // باشد، همان باز می‌شود.
    fun openPrintCopy(exam: ir.exam.app.data.dto.ExamDashboardDto) {
        scope.launch {
            val existing = localExams.firstOrNull { it.sourceExamId == exam.id }
            if (existing != null) {
                onOpenLocalPrintExam(existing.id)
                return@launch
            }
            copyLoading = true
            printStatus = "در حال آماده‌سازی نسخهٔ چاپی..."
            printStatusIsError = false
            try {
                val loaded = builderRepo.load(exam.id).getOrElse {
                    printStatusIsError = true
                    printStatus = sanitizePrintError(it)
                    return@launch
                }
                if (loaded.questions.isEmpty()) {
                    printStatusIsError = true
                    printStatus = "برای نسخهٔ چاپی سؤالی در این آزمون پیدا نشد."
                    return@launch
                }
                val rec = ir.exam.app.data.local.PrintExamRecord(
                    id = java.util.UUID.randomUUID().toString(),
                    title = loaded.title.ifBlank { exam.title },
                    subject = loaded.subject.ifBlank { exam.subject.orEmpty() },
                    questions = loaded.questions,
                    savedAt = System.currentTimeMillis(),
                    sourceExamId = exam.id
                )
                printExamStore.save(rec)
                localExams = printExamStore.list()
                onOpenLocalPrintExam(rec.id)
            } finally {
                copyLoading = false
                printStatus = null
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ساخت آزمون جدید از سازندهٔ بومی آغاز می‌شود.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = onNewNativeExam) {
                Text("آزمون جدید")
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        printStatus?.let {
            Text(
                it,
                color = if (printStatusIsError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
        if (state.loading || pdfPrintLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        if (state.exams.isEmpty() && localExams.isEmpty() && !state.loading) {
            Text("آزمونی برای چاپ نیست.")
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // V86.8 — آزمون‌های چاپیِ محلی، با نشانهٔ «چاپی» تا با آزمونِ سرور
            // اشتباه نشوند. حذف هم دارند، وگرنه راهی برای پاک‌کردنشان نیست.
            items(localExams, key = { "local-" + it.id }) { rec ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                rec.title.ifBlank { "آزمون چاپی" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(onClick = {}, label = { Text("چاپی") })
                        }
                        Text("درس: ${rec.subject.ifBlank { "—" }} · ${rec.questions.size} سؤال")
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            IconButton(onClick = { onOpenLocalPrintExam(rec.id) }) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "ویرایش آزمون چاپی",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // چاپ مستقیم: ابتدا نسخهٔ دانش‌آموز یا پاسخ‌نامه
                            // انتخاب می‌شود.
                            IconButton(onClick = { printTarget = PrintTarget.LocalExam(rec) }, enabled = !pdfPrintLoading) {
                                Icon(
                                    Icons.Outlined.Print,
                                    contentDescription = "چاپ آزمون چاپی",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                printExamStore.delete(rec.id)
                                localExams = printExamStore.list()
                            }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "حذف آزمون چاپی",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            items(state.exams, key = { it.id }) { exam ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            exam.title.ifBlank { "آزمون" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("درس: ${exam.subject.orEmpty().ifBlank { "—" }}")
                        // V101 — مداد: «نسخهٔ چاپی» ویرایش‌پذیر از این آزمون
                        // (فقط در بخش چاپ آزمون ذخیره می‌شود).
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            IconButton(onClick = { openPrintCopy(exam) }, enabled = !copyLoading) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "ویرایش نسخهٔ چاپی",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // V99.1 — پرینتر: چاپِ مستقیم (دانش‌آموز/پاسخ‌نامه).
                            IconButton(onClick = { printTarget = PrintTarget.ServerExam(exam.id) }, enabled = !pdfPrintLoading) {
                                Icon(
                                    Icons.Outlined.Print,
                                    contentDescription = "چاپ آزمون",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    // PDF بومیِ آماده‌شده مستقیماً به پنل چاپ اندروید می‌رود.
    // V99.1 — منوی چاپ از آیکن پرینتر: نسخهٔ دانش‌آموز یا پاسخ‌نامه.
    printTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { printTarget = null },
            title = { Text("چاپ آزمون") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .clickable { startPrint(target, "student") }
                            .padding(vertical = 14.dp, horizontal = 12.dp)
                    ) { Text("🖨 چاپ آزمون (دانش‌آموز)", style = MaterialTheme.typography.titleMedium) }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .clickable { startPrint(target, "teacher") }
                            .padding(vertical = 14.dp, horizontal = 12.dp)
                    ) { Text("✅ چاپ با کلید (پاسخ‌نامه)", style = MaterialTheme.typography.titleMedium) }
                }
            },
            confirmButton = {
                TextButton(onClick = { printTarget = null }) { Text("بستن") }
            }
        )
    }
}

/** V99.1 — هدفِ چاپِ آیکن پرینتر در کارت‌های آزمون. */
private sealed class PrintTarget {
    data class ServerExam(val examId: String) : PrintTarget()
    data class LocalExam(val rec: ir.exam.app.data.local.PrintExamRecord) : PrintTarget()
}

/** پاک‌سازی خطاها پیش از نمایش (بدون درز کلید/URL سرور). */
private fun sanitizePrintError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\\n]*"), "")
    .replace(Regex("(?i)apikey[^,\\n]*"), "")
    .take(240)
    .ifBlank { "چاپ ناموفق بود." }
