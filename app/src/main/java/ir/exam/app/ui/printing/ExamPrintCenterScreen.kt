package ir.exam.app.ui.printing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    // V107 — سربرگ فقط از تنظیماتِ ذخیره‌شدهٔ دستگاه خوانده می‌شود و در
    // آزمون‌سازِ چاپی (پیش‌نمایش/چاپ) به کار می‌رود؛ خودِ این صفحه چاپ نمی‌کند.
    @Suppress("UNUSED_VARIABLE")
    val headerStore = remember(context.applicationContext) {
        ir.exam.app.data.local.PrintHeaderStore(context.applicationContext)
    }
    // V86.8 — آزمون‌های چاپیِ ذخیره‌شده روی دستگاه، کنارِ آزمون‌های سرور.
    // V163 — آزمون‌های چاپی روی سرور (print_exams) تا اپ و سایت یکی باشند.
    val printRepo = remember(context.applicationContext) {
        ir.exam.app.data.repository.SupabasePrintExamRepository(context.applicationContext)
    }
    var localExams by remember { mutableStateOf<List<ir.exam.app.data.repository.SupabasePrintExamRepository.Summary>>(emptyList()) }
    var localLoading by remember { mutableStateOf(true) }
    var localError by remember { mutableStateOf<String?>(null) }
    // V199 — وضعیت پرداخت چاپ هر آزمون چاپی برای سربرگ فعلی دستگاه (چاپگر قرمز = بدهی دارد، سبز = پرداخت‌شده)
    var payStatus by remember { mutableStateOf<Map<String, ir.exam.app.data.repository.SupabasePrintExamRepository.PayStatus>>(emptyMap()) }
    suspend fun reloadPayStatus() {
        payStatus = runCatching { printRepo.payStatus(PrintPayFingerprint.current(context)) }.getOrDefault(emptyMap())
    }
    suspend fun reloadLocal() {
        localLoading = true
        runCatching { printRepo.list() }
            .onSuccess { localExams = it; localError = null }
            .onFailure { localError = sanitizePrintError(it) }
        localLoading = false
        reloadPayStatus()
    }
    // V199 — پنجرهٔ پرداخت هزینهٔ چاپ (آیکون کیف پول روی کارت): برآورد سرور → تأیید → کسر
    var payQuote by remember { mutableStateOf<ir.exam.app.data.repository.SupabasePrintExamRepository.PayQuote?>(null) }
    var payTitle by remember { mutableStateOf("") }
    var payBusy by remember { mutableStateOf(false) }
    // V129 — آزمونی که کاربر روی سطلش زده و منتظر تأیید حذف است (id، عنوان).
    var pendingDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var printStatus by remember { mutableStateOf<String?>(null) }
    var printStatusIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // V107 — چاپِ مستقیم از کارت‌ها حذف شد؛ چاپ فقط از داخلِ آزمون‌ساز
    // (ExamHtmlPrintDialog) انجام می‌شود؛ چاپگرِ بدون‌صفحه دیگر اینجا نیست.
    // V101 — برای ساختِ «نسخهٔ چاپی» از آزمونِ آنلاین، آزمون کامل (با کلید)
    // با همان مسیرِ آزمون‌ساز بارگذاری می‌شود.
    val builderRepo = remember(context.applicationContext) {
        ir.exam.app.data.repository.SupabaseExamBuilderRepository(context.applicationContext)
    }
    var copyLoading by remember { mutableStateOf(false) }
    // V113 — پنجرهٔ «آزمون‌های آنلاین»
    var onlineOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
        // بازگشت از آزمون‌ساز ممکن است آزمونِ چاپیِ تازه‌ای ساخته باشد
        reloadLocal()
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
                // V163 — روی سرور؛ تصاویر آزمون آنلاین قبلاً URL هستند → هزینهٔ تازه‌ای ندارد
                val saved = runCatching { printRepo.save(rec) }.getOrElse {
                    printStatusIsError = true
                    printStatus = sanitizePrintError(it)
                    return@launch
                }
                reloadLocal()
                onOpenLocalPrintExam(saved.id)
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
        // V113 — کنار «آزمون جدید»، «آزمون‌های آنلاین»: پنجرهٔ کارت‌های آزمون
        // آنلاین؛ انتخاب هر کدام نسخهٔ چاپی می‌سازد/باز می‌کند (ویرایش و چاپ).
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = onNewNativeExam) {
                Text("آزمون جدید")
            }
            OutlinedButton(onClick = { onlineOpen = true }, enabled = !copyLoading) {
                Text("آزمون‌های آنلاین")
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
        if (state.loading || copyLoading || localLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        // V113 — فهرست اصلی فقط آزمون‌های چاپی است (آنلاین‌ها در پنجرهٔ خودشان).
        if (localExams.isEmpty() && !state.loading && !localLoading) {
            Text("هنوز آزمون چاپی‌ای نیست. «آزمون جدید» بزنید یا از «آزمون‌های آنلاین» نسخهٔ چاپی بسازید.")
        }
        payQuote?.let { q ->
            PrintPayDialog(
                quote = q,
                title = payTitle,
                busy = payBusy,
                onCancel = { if (!payBusy) payQuote = null },
                onPay = {
                    payBusy = true
                    scope.launch {
                        runCatching { printRepo.pay(q.id, PrintPayFingerprint.current(context)) }
                            .onSuccess { r ->
                                printStatusIsError = false
                                printStatus = "کسر " + "%,d".format(r.costToman) + " تومان از کیف پول با موفقیت انجام شد" +
                                    (r.balanceToman?.let { " (موجودی: " + "%,d".format(it) + " تومان)" } ?: "")
                                payQuote = null
                                reloadPayStatus()
                            }
                            .onFailure { printStatusIsError = true; printStatus = sanitizePrintError(it); payQuote = null }
                        payBusy = false
                    }
                }
            )
        }
        pendingDelete?.let { (delId, delTitle) ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("حذف آزمون چاپی") },
                text = { Text("آزمون «${delTitle.ifBlank { "آزمون چاپی" }}» برای همیشه حذف شود؟ این کار برگشت‌پذیر نیست.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDelete = null
                        scope.launch {
                            runCatching { printRepo.delete(delId) }
                                .onFailure { printStatusIsError = true; printStatus = sanitizePrintError(it) }
                            reloadLocal()
                        }
                    }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("انصراف") } }
            )
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
                        Text("درس: ${rec.subject.ifBlank { "—" }} · ${rec.questionCount} سؤال")
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
                            // V199 — کیف پول: پرداخت هزینهٔ چاپ همین آزمون (سؤال/تصویر، یک‌بار برای هر دو نسخه)
                            IconButton(onClick = {
                                scope.launch {
                                    runCatching { printRepo.quote(rec.id, PrintPayFingerprint.current(context)) }
                                        .onSuccess { q ->
                                            if (q.paid) { printStatusIsError = false; printStatus = "هزینهٔ چاپ این آزمون قبلاً پرداخت شده است."; reloadPayStatus() }
                                            else { payTitle = rec.title; payQuote = q }
                                        }
                                        .onFailure { printStatusIsError = true; printStatus = sanitizePrintError(it) }
                                }
                            }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = "پرداخت هزینهٔ چاپ این آزمون",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // V199 — چاپگر: قرمز تا پرداخت نشده، سبز پس از پرداخت؛ لمس → آزمون‌ساز چاپی (چاپ از پیش‌نمایش همان‌جا)
                            val st = payStatus[rec.id]
                            IconButton(onClick = { onOpenLocalPrintExam(rec.id) }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Print,
                                    contentDescription = when {
                                        st == null -> "پیش‌نمایش و چاپ"
                                        st.paid -> "پرداخت‌شده — پیش‌نمایش و چاپ"
                                        else -> "پرداخت‌نشده — پیش‌نمایش و چاپ"
                                    },
                                    tint = when {
                                        st == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                        st.paid -> androidx.compose.ui.graphics.Color(0xFF16A34A)
                                        else -> androidx.compose.ui.graphics.Color(0xFFDC2626)
                                    }
                                )
                            }
                            // V129 — حذف فقط بعد از تأیید کاربر (پنجرهٔ پرسش).
                            IconButton(onClick = { pendingDelete = rec.id to rec.title }) {
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
        }
    }
    if (onlineOpen) {
        AlertDialog(
            onDismissRequest = { onlineOpen = false },
            title = { Text("آزمون‌های آنلاین") },
            text = {
                if (state.exams.isEmpty()) Text(if (state.loading) "در حال دریافت آزمون‌ها…" else "آزمون آنلاینی وجود ندارد.")
                else LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.exams, key = { it.id }) { exam ->
                        val hasCopy = localExams.any { it.sourceExamId == exam.id }
                        Card(Modifier.fillMaxWidth().clickable(enabled = !copyLoading) { onlineOpen = false; openPrintCopy(exam) }) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        exam.title.ifBlank { "آزمون" },
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (hasCopy) AssistChip(onClick = {}, label = { Text("نسخهٔ چاپی دارد") })
                                }
                                Text("درس: ${exam.subject.orEmpty().ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onlineOpen = false }) { Text("بستن") } }
        )
    }
}

/** V199 — پنجرهٔ پرداخت هزینهٔ چاپ آزمون چاپی (همان متن سایت: printDueText). */
@Composable
internal fun PrintPayDialog(
    quote: ir.exam.app.data.repository.SupabasePrintExamRepository.PayQuote,
    title: String,
    busy: Boolean,
    onCancel: () -> Unit,
    onPay: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("پرداخت هزینهٔ چاپ" + (if (title.isNotBlank()) " — $title" else "")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (quote.headerChanged) Text("سربرگ تغییر کرده → کل آزمون دوباره محاسبه می‌شود")
                else if (quote.neverPaid) Text("این آزمون هنوز پرداخت نشده است")
                Text("سؤال: ${quote.questionsDue} × 1,000 تومان")
                Text("تصویر: ${quote.imagesDue} × 1,000 تومان")
                Text("مبلغ قابل کسر از کیف پول: " + "%,d".format(quote.dueToman) + " تومان", style = MaterialTheme.typography.titleMedium)
                if (busy) Text("در حال پرداخت…")
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onPay) { Text("پرداخت") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onCancel) { Text("انصراف") } }
    )
}

/** پاک‌سازی خطاها پیش از نمایش (بدون درز کلید/URL سرور). */
private fun sanitizePrintError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\\n]*"), "")
    .replace(Regex("(?i)apikey[^,\\n]*"), "")
    .take(240)
    .ifBlank { "چاپ ناموفق بود." }
