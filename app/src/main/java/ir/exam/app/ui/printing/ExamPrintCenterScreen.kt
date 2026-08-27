package ir.exam.app.ui.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.core.printing.OfficialPrintController
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.ui.builder.JalaliDateTimeField
import ir.exam.app.ui.builder.jalaliDisplay
import ir.exam.app.ui.classes.TeacherSchoolItem
import ir.exam.app.ui.common.FieldOfStudyPicker
import ir.exam.app.ui.common.GradeOdometerPicker
import ir.exam.app.ui.dashboard.TeacherDashboardViewModel

/**
 * V62.7 — صفحهٔ «چاپ آزمون» (جایگزین کارت سربرگ منوی معلم): لیست آزمون‌ها با
 * دکمه‌های «چاپ برگه» و «چاپ با کلید» + دکمهٔ وسط‌چین «سربرگ» بالای لیست
 * (مثل «مشخصات آزمون» سازنده) که پنجرهٔ سربرگ رسمی را باز می‌کند:
 * استان، شهر/شهرستان، منطقه/ناحیه، نام مدرسه (از مدارس عضو یا سایر)،
 * پایه/رشته (همان چرخ‌های فرم دانش‌آموز)، نام درس، تاریخ امتحان (تقویم
 * شمسی) و مدت امتحان؛ پس از تأیید، پیش‌نمایش سربرگ کامل نشان داده می‌شود.
 */
@Composable
fun ExamPrintCenterScreen(
    // V63.0 — مداد روی کارت هر آزمون: ویرایشگر سند Word-مانند را باز می‌کند.
    onEditExamDocument: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val printController = remember(context.applicationContext) { OfficialPrintController(context.applicationContext) }
    val viewModel = remember { TeacherDashboardViewModel() }
    val state by viewModel.state.collectAsState()
    var headerOpen by remember { mutableStateOf(false) }
    var header by remember { mutableStateOf(OfficialPrintHeader()) }
    // مدارس عضو معلم برای انتخاب «نام مدرسه» در سربرگ (همان RPC نمای مدارس).
    var schools by remember { mutableStateOf<List<TeacherSchoolItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        viewModel.load()
        runCatching {
            val raw = ir.exam.app.data.remote.SupabaseProvider.client.postgrest
                .rpc("native_teacher_schools_v61")
                .decodeAs<kotlinx.serialization.json.JsonObject>()
            ((raw["items"] as? kotlinx.serialization.json.JsonArray) ?: kotlinx.serialization.json.JsonArray(emptyList()))
                .mapNotNull { element ->
                    val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                    val id = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
                    TeacherSchoolItem(
                        id = id,
                        name = (obj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                    )
                }
        }.onSuccess { schools = it }
    }
    LaunchedEffect(state.printExam) {
        state.printExam?.let { printable ->
            runCatching { printController.printExam(context, printable) }
                .onFailure(viewModel::reportError)
            viewModel.consumePrint()
        }
    }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // دکمهٔ وسط‌چین «سربرگ» مثل «مشخصات آزمون».
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = { headerOpen = true }) {
                Text(if (headerOpen) "بستن سربرگ" else "سربرگ")
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.loading || state.portabilityLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        if (state.exams.isEmpty() && !state.loading) Text("آزمونی برای چاپ نیست.")
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        // V62.7 — چاپ برگه/چاپ با کلید فقط اینجا هستند.
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(onClick = { viewModel.preparePrint(exam.id, false, header) }) {
                                Text("چاپ برگه")
                            }
                            OutlinedButton(onClick = { viewModel.preparePrint(exam.id, true, header) }) {
                                Text("چاپ با کلید")
                            }
                            // V63.0 — مداد ویرایش آزمون: سند Word-مانند (همهٔ سؤال‌ها
                            // پشت‌سرهم، اندازهٔ واقعی A4 و صفحه‌بندی) را باز می‌کند.
                            OutlinedButton(onClick = { onEditExamDocument(exam.id) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "ویرایش آزمون")
                                Text("ویرایش سند")
                            }
                        }
                    }
                }
            }
        }
    }
    if (headerOpen) {
        PrintHeaderDialog(
            initial = header,
            schools = schools,
            onDismiss = { headerOpen = false },
            onConfirm = { header = it; headerOpen = false }
        )
    }
}

/** پنجرهٔ سربرگ رسمی: فرم + پیش‌نمایش زندهٔ سربرگ کامل پس از تأیید اطلاعات. */
@Composable
private fun PrintHeaderDialog(
    initial: OfficialPrintHeader,
    schools: List<TeacherSchoolItem>,
    onDismiss: () -> Unit,
    onConfirm: (OfficialPrintHeader) -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    var otherSchool by remember {
        mutableStateOf(initial.school.isNotBlank() && schools.none { it.name == initial.school })
    }
    var examDateIso by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سربرگ رسمی آزمون") },
        text = {
            Column(
                // V62.8 — با باز شدن کیبورد پنجره بالا کشیده و اسکرول‌پذیر می‌ماند.
                Modifier.fillMaxWidth().heightIn(max = 560.dp).imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    draft.province, { draft = draft.copy(province = it.take(80)) },
                    label = { Text("استان") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    draft.city, { draft = draft.copy(city = it.take(80)) },
                    label = { Text("شهر/شهرستان") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    draft.district, { draft = draft.copy(district = it.take(80)) },
                    label = { Text("منطقه/ناحیه") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                // نام مدرسه: از مدارس عضو یا «سایر».
                Text("نام مدرسه", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    schools.take(3).forEach { school ->
                        FilterChip(
                            selected = !otherSchool && draft.school == school.name,
                            onClick = { otherSchool = false; draft = draft.copy(school = school.name) },
                            label = { Text(school.name.ifBlank { "مدرسه" }, maxLines = 1) }
                        )
                    }
                    FilterChip(
                        selected = otherSchool,
                        onClick = { otherSchool = true; draft = draft.copy(school = "") },
                        label = { Text("سایر") }
                    )
                }
                if (otherSchool || schools.isEmpty()) {
                    OutlinedTextField(
                        draft.school, { draft = draft.copy(school = it.take(120)) },
                        label = { Text("نام مدرسه") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                // پایه و رشته: همان چرخ‌های پنجرهٔ ایجاد دانش‌آموز.
                GradeOdometerPicker(
                    value = draft.grade,
                    onValueChange = { draft = draft.copy(grade = it) },
                    modifier = Modifier.fillMaxWidth()
                )
                FieldOfStudyPicker(
                    value = draft.fieldOfStudy,
                    onValueChange = { draft = draft.copy(fieldOfStudy = it) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    draft.subject, { draft = draft.copy(subject = it.take(80)) },
                    label = { Text("نام درس") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                // تاریخ امتحان از تقویم شمسی.
                JalaliDateTimeField(
                    label = "تاریخ امتحان",
                    iso = examDateIso,
                    onChange = { iso ->
                        examDateIso = iso
                        // V62.8 — فقط تاریخ؛ ساعت و دقیقه حذف می‌شود.
                        draft = draft.copy(examDate = iso?.let { jalaliDisplay(it).substringBefore(" ") }.orEmpty())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                // V62.8 — فقط عدد دقیقه؛ «دقیقه» خودکار به سربرگ اضافه می‌شود.
                OutlinedTextField(
                    draft.examDuration, { draft = draft.copy(examDuration = it.filter(Char::isDigit).take(4)) },
                    label = { Text("مدت امتحان") },
                    supportingText = { Text("عدد دقیقه؛ مثال: 120") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                // پیش‌نمایش سربرگ کامل‌شده (همان چیدمان چاپ).
                Text("پیش‌نمایش سربرگ", style = MaterialTheme.typography.labelLarge)
                HeaderPreview(draft)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(draft) }) { Text("تأیید سربرگ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/**
 * پیش‌نمایش سربرگ رسمی — دقیقاً همان ۵ سطر چاپ:
 * ۱) آرم وسط. ۲) نام | وزارت... | تاریخ. ۳) نام خانوادگی | اداره کل... | مدت.
 * ۴) نام پدر | مدیریت... | پایه. ۵) نام درس | مدرسه | رشته.
 */
@Composable
fun HeaderPreview(header: OfficialPrintHeader) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AsyncImage(
                "file:///android_asset/print/emblem.png",
                contentDescription = "آرم",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit
            )
        }
        HeaderPreviewRow("نام:", "وزارت آموزش و پرورش جمهوری اسلامی ایران", "تاریخ آزمون: ${header.examDate}")
        // V62.8 — مدت با پسوند «دقیقه» (مثل: مدت آزمون: 120 دقیقه).
        HeaderPreviewRow(
            "نام خانوادگی:",
            "اداره کل آموزش و پرورش استان ${header.province}",
            "مدت آزمون: " + header.examDuration.takeIf(String::isNotBlank)?.let { "$it دقیقه" }.orEmpty()
        )
        HeaderPreviewRow(
            "نام پدر:",
            "مدیریت آموزش و پرورش شهر/شهرستان ${header.city}" +
                header.district.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty(),
            "پایه: ${header.grade}"
        )
        HeaderPreviewRow("نام درس: ${header.subject}", header.school, "رشته: ${header.fieldOfStudy}")
    }
}

@Composable
private fun HeaderPreviewRow(right: String, center: String, left: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            right, fontSize = 9.sp, color = Color.Black, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        Text(
            center, fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.8f)
        )
        Text(
            left, fontSize = 9.sp, color = Color.Black, textAlign = TextAlign.Left,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.height(0.dp))
    }
}
