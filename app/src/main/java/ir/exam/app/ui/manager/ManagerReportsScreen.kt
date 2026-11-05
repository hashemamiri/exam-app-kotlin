package ir.exam.app.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.data.repository.ManagerClassReport
import ir.exam.app.data.repository.ManagerClassStat
import ir.exam.app.data.repository.ManagerClassStats
import ir.exam.app.data.repository.SupabaseManagerRepository
import ir.exam.app.ui.reports.ReportCardLauncher
import ir.exam.app.ui.reports.ReportCardPrintDialog
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * V340 — بخش «کارنامه» برای مدیر، همانند معلم:
 * کلاس → کارنامهٔ کل کلاس یا یک دانش‌آموز → انتخاب یکی از ۱۰ قالب → پیش‌نمایش → چاپ/PDF.
 * دفترچهٔ مدرسه هم از همین‌جا ساخته می‌شود: نمای کلی + خلاصهٔ مقایسه + کارنامهٔ کامل هر کلاس.
 *
 * هیچ SQL تازه‌ای لازم نیست: فهرست کلاس‌ها از native_manager_class_stats_v1 (V230)
 * و نمره‌ها از native_manager_class_report_v1 (V260.8) می‌آید.
 */
private const val MAX_BOOKLET_CLASSES = 40

@Composable
fun ManagerReportsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { SupabaseManagerRepository() }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf<ManagerClassStats?>(null) }
    var reload by remember { mutableStateOf(0) }

    // مسیر: null = انتخاب گونه، "class" = کارنامهٔ کلاس، "student" = کارنامهٔ دانش‌آموز
    var kind by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<ManagerClassStat?>(null) }
    var report by remember { mutableStateOf<ManagerClassReport?>(null) }
    var busy by remember { mutableStateOf(false) }

    var cardPayload by remember { mutableStateOf<JsonObject?>(null) }
    var cardTitle by remember { mutableStateOf("") }

    LaunchedEffect(reload) {
        loading = true
        error = ""
        repo.classStats()
            .onSuccess { stats = it }
            .onFailure { error = it.message ?: "دریافت آمار کلاس‌ها ناموفق بود." }
        loading = false
    }

    cardPayload?.let { payload ->
        ReportCardPrintDialog(payload = payload, title = cardTitle, onDismiss = { cardPayload = null })
    }

    fun openClass(k: ManagerClassStat) {
        if (busy) return
        busy = true
        note = ""
        picked = k
        scope.launch {
            repo.classReport(k.id)
                .onSuccess { r ->
                    report = r
                    if (kind == "class") {
                        runCatching { ReportCardLauncher.forManagerClass(context, r, k.teacher, k.school) }
                            .onSuccess { cardTitle = "کارنامهٔ کلاس ${k.name}"; cardPayload = it }
                            .onFailure { error = it.message ?: "ساخت کارنامه ناموفق بود." }
                    }
                }
                .onFailure { error = it.message ?: "دریافت کارنامهٔ کلاس ناموفق بود." }
            busy = false
        }
    }

    fun openStudent(name: String) {
        val r = report ?: return
        val k = picked ?: return
        if (busy) return
        busy = true
        scope.launch {
            runCatching { ReportCardLauncher.forManagerStudent(context, r, name, k.school) }
                .onSuccess { cardTitle = "کارنامهٔ $name"; cardPayload = it }
                .onFailure { error = it.message ?: "ساخت کارنامه ناموفق بود." }
            busy = false
        }
    }

    fun openBooklet() {
        val s = stats ?: return
        if (busy) return
        busy = true
        note = "در حال آماده‌سازی دفترچهٔ مدرسه…"
        scope.launch {
            val classes = s.classes.take(MAX_BOOKLET_CLASSES)
            val reports = mutableListOf<JsonObject>()
            classes.forEach { k ->
                val r = repo.classReport(k.id).getOrNull() ?: return@forEach
                if (r.exams.isEmpty() || r.students.isEmpty()) return@forEach
                runCatching { ReportCardLauncher.forManagerClass(context, r, k.teacher, k.school) }
                    .onSuccess { reports.add(it) }
            }
            runCatching { ReportCardLauncher.forManagerSchool(context, s, reports) }
                .onSuccess {
                    cardTitle = "کارنامهٔ مدرسه"
                    cardPayload = it
                    note = if (reports.isEmpty()) "کلاسی با آزمونِ تصحیح‌شده پیدا نشد؛ فقط نمای کلی مدرسه ساخته شد." else ""
                }
                .onFailure { error = it.message ?: "ساخت دفترچه ناموفق بود." }
            busy = false
        }
    }

    fun back() {
        when {
            report != null && kind == "student" -> { report = null; picked = null }
            picked != null -> { picked = null; report = null }
            else -> kind = null
        }
        error = ""
        note = ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "کارنامهٔ کلاس، دانش‌آموز و مدرسه با ۱۰ قالب آماده؛ پس از انتخاب قالب، پیش‌نمایش و چاپ/PDF.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (loading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                    Text("در حال دریافت…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (error.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { reload++ }) { Text("تلاش دوباره") }
                    }
                }
            }
        }
        if (note.isNotBlank()) item { Text(note, style = MaterialTheme.typography.bodySmall) }

        val s = stats
        if (!loading && s != null) {
            if (kind == null) {
                item {
                    ReportKindCard(
                        "کارنامهٔ کلاس",
                        "نمرهٔ همهٔ دانش‌آموزان یک کلاس در همهٔ آزمون‌ها، با میانگین و رتبه.",
                        busy
                    ) { kind = "class" }
                }
                item {
                    ReportKindCard(
                        "کارنامهٔ دانش‌آموز",
                        "کارنامهٔ یک دانش‌آموز با رتبه در کلاس و تحلیل درس‌ها.",
                        busy
                    ) { kind = "student" }
                }
                item {
                    ReportKindCard(
                        "کارنامهٔ مدرسه (دفترچه)",
                        "نمای کلی مدرسه + خلاصهٔ مقایسهٔ کلاس‌ها + کارنامهٔ کامل هر کلاس در یک فایل.",
                        busy
                    ) { openBooklet() }
                }
                item {
                    Text(
                        "تعداد کلاس‌ها: " + PersianDigits.convert(s.classes.size.toString()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (picked == null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { back() }) { Text("بازگشت") }
                    }
                }
                if (s.classes.isEmpty()) {
                    item { Text("کلاسی برای این مدرسه ثبت نشده است.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(s.classes.size) { i ->
                    val k = s.classes[i]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(k.name.ifBlank { "کلاس" }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOf(
                                    k.teacher.ifBlank { "بدون معلم" },
                                    k.school,
                                    PersianDigits.convert(k.students.toString()) + " دانش‌آموز",
                                    PersianDigits.convert(k.exams.toString()) + " آزمون"
                                ).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(onClick = { openClass(k) }, enabled = !busy) {
                                Text(if (kind == "class") "ساخت کارنامهٔ کلاس" else "انتخاب دانش‌آموز")
                            }
                        }
                    }
                }
            } else if (kind == "student") {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { back() }, enabled = !busy) { Text("بازگشت") }
                    }
                }
                val r = report
                if (r == null) {
                    item { Text("در حال دریافت دانش‌آموزان…", style = MaterialTheme.typography.bodyMedium) }
                } else if (r.students.isEmpty()) {
                    item { Text("این کلاس دانش‌آموزی ندارد.", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(r.students.size) { i ->
                        val st = r.students[i]
                        val name = st.fullName.ifBlank { st.username }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(name.ifBlank { "دانش‌آموز" }, style = MaterialTheme.typography.titleSmall)
                                    if (st.username.isNotBlank()) Text(st.username, style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = { openStudent(name) }, enabled = !busy) { Text("کارنامه") }
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { back() }, enabled = !busy) { Text("بازگشت") }
                        val r = report
                        if (r != null) {
                            Button(onClick = { picked?.let { openClass(it) } }, enabled = !busy) { Text("ساخت دوباره") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportKindCard(title: String, subtitle: String, busy: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onClick, enabled = !busy) { Text("انتخاب") }
        }
    }
}
