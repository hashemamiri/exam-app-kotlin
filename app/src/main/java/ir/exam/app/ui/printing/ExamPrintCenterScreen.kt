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
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import ir.exam.app.data.local.PrintLayoutStore
import ir.exam.app.data.repository.SupabasePortabilityRepository
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.ui.dashboard.TeacherDashboardViewModel
import kotlinx.coroutines.launch

/**
 * صفحهٔ «چاپ آزمون» — نسخهٔ 30 تعاملی:
 * - V79.1 — «آزمون جدید» آزمون‌سازِ بومی را باز می‌کند (همان صفحهٔ «ایجاد آزمون»).
 *   دکمهٔ دوم «آزمون‌ساز چاپی» نسخهٔ ۳۰ را مثل قبل باز می‌کند، پس هیچ امکانی
 *   از دسترس خارج نمی‌شود.
 * - V76.0 — کارت هر آزمون فقط دو آیکن دارد: مداد (ویرایش در نسخهٔ 30) و پرینتر
 *   (ورود خودکار سؤالات به نسخهٔ 30 و چاپ از همان‌جا). سؤالات با پل
 *   window.setExamData و تصاویر با توکن نشست (data-URL) منتقل می‌شوند؛
 *   هر ویرایشی فقط روی خروجی چاپ همان جلسه اثر دارد و آزمون سرور را عوض نمی‌کند.
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
    val header = remember {
        val f = headerStore.read()
        OfficialPrintHeader(
            school = f["f_branch"].orEmpty(),
            subject = f["f_course"].orEmpty(),
            examDate = f["f_examDate"].orEmpty(),
            examDuration = f["f_duration"].orEmpty()
        )
    }
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

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // V76.0 — دکمهٔ وسط‌چین «آزمون جدید»: نسخهٔ 30 را بدون داده (ریست) باز می‌کند.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            // V79.1 — مسیرِ اصلی: آزمون‌سازِ کاملاً بومی (همان «ایجاد آزمون»).
            Button(onClick = onNewNativeExam) {
                Text("آزمون جدید")
            }
            // نسخهٔ ۳۰ همچنان در دسترس است تا هیچ امکانی از دست نرود.
            OutlinedButton(onClick = { htmlPrintExam = null; htmlPrintOpen = true }) {
                Text("آزمون‌ساز چاپی")
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
                            // پرینتر: ورود خودکار سؤالات به نسخهٔ 30؛ چاپ نسخهٔ دانشجو/استاد همان‌جا.
                            IconButton(onClick = { openBuilder30(exam.id) }, enabled = !htmlPrintLoading) {
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
    if (htmlPrintOpen) {
        ExamHtmlPrintDialog(
            printable = htmlPrintExam,
            onDismiss = { htmlPrintOpen = false }
        )
    }
}

/** پاک‌سازی خطاها پیش از نمایش (بدون درز کلید/URL سرور). */
private fun sanitizePrintError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\\n]*"), "")
    .replace(Regex("(?i)apikey[^,\\n]*"), "")
    .take(240)
    .ifBlank { "چاپ ناموفق بود." }
