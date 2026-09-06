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
import ir.exam.app.data.local.PrintLayoutStore
import ir.exam.app.data.repository.SupabasePortabilityRepository
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.ui.dashboard.TeacherDashboardViewModel
import kotlinx.coroutines.launch

/**
 * صفحهٔ «چاپ آزمون» — نسخهٔ 30 تعاملی:
 * - V79.1 — «آزمون جدید» آزمون‌سازِ بومی را باز می‌کند (همان صفحهٔ «ایجاد آزمون»).
 *   V97 — دکمهٔ «آزمون‌ساز چاپی» حذف شد و تنها مسیر «آزمون جدید» بومی فعال است.
 * - V76.0 — کارت هر آزمون فقط دو آیکن دارد: مداد (ویرایش در نسخهٔ 30) و پرینتر.
 *   V99.1 — پرینتر دیگر پنجرهٔ آزمون‌ساز چاپی را باز نمی‌کند: نسخهٔ
 *   دانش‌آموز/پاسخ‌نامه انتخاب می‌شود و چاپ مستقیم انجام می‌گردد (برگهٔ خالصِ
 *   A4 + پنجرهٔ چاپِ اندروید). مداد همچنان آزمون‌ساز را برای ویرایش باز می‌کند.
 *   سؤالات با پل window.setExamData و تصاویر با توکن نشست (data-URL) منتقل
 *   می‌شوند؛ هر ویرایشی فقط روی خروجی چاپ همان جلسه اثر دارد و آزمون سرور را
 *   عوض نمی‌کند.
 * - V63.0 — پارامتر مداد ویرایشگر سند حفظ شده؛ مسیر DOC_EDITOR دست‌نخورده است.
 */
@Composable
fun ExamPrintCenterScreen(
    onEditExamDocument: (String) -> Unit = {},
    // V79.1 — «آزمون جدید» به آزمون‌سازِ بومی می‌رود، نه به WebView نسخهٔ ۳۰.
    onNewNativeExam: () -> Unit = {},
    // V86.8 — ویرایشِ آزمونِ چاپیِ ذخیره‌شده روی دستگاه.
    onOpenLocalPrintExam: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val layoutStore = remember(context.applicationContext) { PrintLayoutStore(context.applicationContext) }
    val viewModel = remember { TeacherDashboardViewModel() }
    val state by viewModel.state.collectAsState()
    val portability = remember { SupabasePortabilityRepository() }
    // سربرگ خالی: printableExam سربرگ را از پروفایل می‌سازد؛ جزئیات داخل نسخهٔ 30 ویرایش می‌شود.
    // V86.7 — مقادیرِ «تنظیمات سربرگ» که کاربر در آزمون‌سازِ چاپی وارد کرده
    // روی دستگاه می‌مانند و همین‌جا به سربرگِ چاپ می‌رسند. اگر چیزی ذخیره
    // نشده باشد، دقیقاً مثل قبل خالی است.
    val headerStore = remember(context.applicationContext) {
        ir.exam.app.data.local.PrintHeaderStore(context.applicationContext)
    }
    val header = remember { ir.exam.app.data.local.printHeaderOf(headerStore.read()) }
    // V86.8 — آزمون‌های چاپیِ ذخیره‌شده روی دستگاه، کنارِ آزمون‌های سرور.
    val printExamStore = remember(context.applicationContext) {
        ir.exam.app.data.local.PrintExamStore(context.applicationContext)
    }
    var localExams by remember { mutableStateOf(printExamStore.list()) }
    var htmlPrintOpen by remember { mutableStateOf(false) }
    var htmlPrintExam by remember { mutableStateOf<OfficialExamPrintable?>(null) }
    var htmlPrintLoading by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf<String?>(null) }
    var printStatusIsError by remember { mutableStateOf(false) }
    // V99.1 — آیکن پرینتر دیگر «پنجرهٔ آزمون‌ساز چاپی» را باز نمی‌کند:
    // printTarget = آزمونِ انتخاب‌شدهٔ چاپ، printModeFor = حالتِ پنجرهٔ
    // چاپ (null یعنی آزمون‌ساز برای ویرایش؛ student/teacher یعنی چاپِ مستقیم).
    var printTarget by remember { mutableStateOf<PrintTarget?>(null) }
    var printModeFor by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.load()
        // بازگشت از آزمون‌ساز ممکن است آزمونِ چاپیِ تازه‌ای ساخته باشد
        localExams = printExamStore.list()
    }

    // V76.0 — ورود خودکار سؤالات یک آزمون به نسخهٔ 30 و باز کردن آن تمام‌صفحه.
    fun openBuilder30(examId: String) {
        scope.launch {
            htmlPrintLoading = true
            try {
                // V99.1 — مداد (ویرایش) هنوز آزمون‌سازِ کامل را باز می‌کند؛
                // حالتِ پنجرهٔ چاپ باید null بماند تا کارت‌ها نمایش داده شوند.
                printModeFor = null
                printStatus = null
                printStatusIsError = false
                portability.printableExam(examId, false, header, layoutStore.read(examId))
                    .onSuccess { printable ->
                        htmlPrintExam = ExamHtmlImageInliner.inline(context.applicationContext, printable)
                        htmlPrintOpen = true
                    }
                    .onFailure { error ->
                        printStatusIsError = true
                        printStatus = sanitizePrintError(error)
                    }
            } finally {
                htmlPrintLoading = false
            }
        }
    }

    // V99.1 — چاپِ مستقیم از آیکن پرینتر: پنجرهٔ آزمون‌ساز چاپی باز نمی‌شود؛
    // در پنجرهٔ چاپ فقط برگهٔ خالصِ A4 دیده می‌شود و سپس پنجرهٔ چاپِ
    // اندروید اجرا می‌شود.
    fun startPrint(target: PrintTarget, mode: String) {
        printTarget = null
        scope.launch {
            htmlPrintLoading = true
            try {
                printStatus = null
                printStatusIsError = false
                when (target) {
                    is PrintTarget.ServerExam -> portability.printableExam(
                        target.examId, false, header, layoutStore.read(target.examId)
                    ).onSuccess { printable ->
                        printModeFor = mode
                        htmlPrintExam = ExamHtmlImageInliner.inline(context.applicationContext, printable)
                        htmlPrintOpen = true
                    }.onFailure { error ->
                        printStatusIsError = true
                        printStatus = sanitizePrintError(error)
                    }
                    is PrintTarget.LocalExam -> {
                        printModeFor = mode
                        htmlPrintExam = ir.exam.app.domain.model.PrintableFromDrafts.build(
                            title = target.rec.title.ifBlank { "آزمون" },
                            subject = target.rec.subject,
                            header = header,
                            questions = target.rec.questions
                        )
                        htmlPrintOpen = true
                    }
                }
            } finally {
                htmlPrintLoading = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // V76.0 — دکمهٔ وسط‌چین «آزمون جدید»: نسخهٔ 30 را بدون داده (ریست) باز می‌کند.
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
        if (state.loading || htmlPrintLoading) {
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
                            // V99.1 — پرینتر: چاپِ مستقیم (پنجرهٔ آزمون‌ساز
                            // چاپی باز نمی‌شود)؛ اول دانش‌آموز/پاسخ‌نامه را
                            // انتخاب می‌کنید.
                            IconButton(onClick = { printTarget = PrintTarget.LocalExam(rec) }, enabled = !htmlPrintLoading) {
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
                        // V76.0 — فقط مداد (ویرایش) و پرینتر (ورود سؤالات + چاپ)؛
                        // هر دو همان جریان نسخهٔ 30 را باز می‌کنند.
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            // مداد: ویرایش سؤالات/چیدمان در نسخهٔ 30 (فقط چاپ؛ بدون تغییر سرور).
                            IconButton(onClick = { openBuilder30(exam.id) }, enabled = !htmlPrintLoading) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "ویرایش آزمون",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // V99.1 — پرینتر: چاپِ مستقیم (دانش‌آموز/پاسخ‌نامه)؛
                            // پنجرهٔ آزمون‌ساز چاپی دیگر باز نمی‌شود.
                            IconButton(onClick = { printTarget = PrintTarget.ServerExam(exam.id) }, enabled = !htmlPrintLoading) {
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
    // V76.0 — پنجرهٔ تمام‌صفحهٔ نسخهٔ 30؛ null یعنی «آزمون جدید» (ریست).
    // V99.1 — وقتی initialPrintMode خالی نباشد = چاپِ مستقیم: پنجرهٔ
    // کارت‌ها و کنترل‌های آزمون‌ساز چاپی نمایش داده نمی‌شوند، فقط برگهٔ
    // خالصِ A4 و سپس پنجرهٔ چاپِ اندروید.
    if (htmlPrintOpen) {
        ExamHtmlPrintDialog(
            printable = htmlPrintExam,
            initialPrintMode = printModeFor,
            onDismiss = {
                htmlPrintOpen = false
                printModeFor = null
            }
        )
    }
    // V99.1 — منوی چاپ از آیکن پرینتر: نسخهٔ دانش‌آموز یا پاسخ‌نامه.
    printTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { printTarget = null },
            title = { Text("چاپ") },
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
