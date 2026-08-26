package ir.exam.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.JalaliDate
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.domain.model.CalendarAudience
import ir.exam.app.domain.model.CalendarAudienceOption
import ir.exam.app.domain.model.CalendarDay
import ir.exam.app.domain.model.CalendarNote
import ir.exam.app.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(role: UserRole) {
    val viewModel = remember(role) { CalendarViewModel(role) }
    val state by viewModel.state.collectAsState()
    var deleteCandidate by remember { mutableStateOf<CalendarNote?>(null) }
    val selectedDay = state.monthData?.days?.firstOrNull { it.jalaliDate == state.selectedDate }

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CalendarHeader(
                year = state.year,
                month = state.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onToday = viewModel::goToday
            )
        }
        item { WeekdayHeader() }
        item {
            val monthData = state.monthData
            if (monthData != null) {
                CalendarGrid(
                    days = monthData.days,
                    year = monthData.year,
                    month = monthData.month,
                    selected = state.selectedDate,
                    onSelect = viewModel::select
                )
            } else if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text("تقویم دریافت نشد.")
            }
        }
        if (state.monthData?.holidayDataAvailable == false) {
            item { WarningCard("تعطیلات رسمی از سرور دریافت نشد؛ جمعه‌ها همچنان مشخص هستند.") }
        } else if (state.monthData != null && !state.monthData!!.officialYearIsExact) {
            item { WarningCard("دادهٔ رسمی کامل این سال تأیید نشده است؛ فقط مناسبت‌های موجود و جمعه‌ها نمایش داده می‌شوند.") }
        }
        state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
        state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            SelectedDayCard(
                day = selectedDay,
                isTeacher = role == UserRole.TEACHER,
                busy = state.saving,
                onNew = { selectedDay?.jalaliDate?.let(viewModel::newNote) },
                onEdit = viewModel::edit,
                onDelete = { deleteCandidate = it }
            )
        }
        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
    }

    state.editor?.let {
        CalendarEditorDialog(
            state = state,
            onDismiss = viewModel::dismissEditor,
            onTitle = viewModel::setEditorTitle,
            onBody = viewModel::setEditorBody,
            onAudience = viewModel::setAudience,
            onToggleClass = viewModel::toggleClass,
            onToggleStudent = viewModel::toggleStudent,
            onToggleSchool = viewModel::toggleSchool,
            onSave = viewModel::saveEditor
        )
    }

    deleteCandidate?.let { note ->
        AlertDialog(
            onDismissRequest = { if (!state.saving) deleteCandidate = null },
            title = { Text("حذف پیام") },
            text = { Text("پیام «${note.title}» حذف شود؟") },
            confirmButton = {
                Button(
                    enabled = !state.saving,
                    onClick = { viewModel.delete(note); deleteCandidate = null }
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun CalendarHeader(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, enabled = year > JalaliCalendar.MIN_YEAR || month > 1) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "ماه قبل")
                }
                Text(
                    text = PersianDigits.convert("${JalaliCalendar.MONTH_NAMES[month - 1]} $year"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNext, enabled = year < JalaliCalendar.MAX_YEAR || month < 12) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "ماه بعد")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToday) { Text("امروز") }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        JalaliCalendar.WEEKDAY_NAMES.forEachIndexed { index, label ->
            Text(
                text = label,
                textAlign = TextAlign.Center,
                color = if (index == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    days: List<CalendarDay>,
    year: Int,
    month: Int,
    selected: JalaliDate,
    onSelect: (JalaliDate) -> Unit
) {
    val lead = JalaliCalendar.firstDayOffset(year, month)
    val cells: List<CalendarDay?> = List(lead) { null } + days
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { partialRow ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (partialRow + List(7 - partialRow.size) { null }).forEach { day ->
                    if (day == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                    else DayCell(
                        day = day,
                        selected = selected == day.jalaliDate,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    selected: Boolean,
    onSelect: (JalaliDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = JalaliCalendar.today()
    val background = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        day.jalaliDate == today -> MaterialTheme.colorScheme.tertiaryContainer
        day.isHoliday -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface
    }
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val description = buildString {
        append(day.jalaliDate.longDisplay())
        if (day.isFriday) append("، جمعه")
        day.officialHolidays.forEach { append("، ").append(it.title) }
        if (day.notes.isNotEmpty()) append("، ${PersianDigits.convert(day.notes.size)} پیام")
    }
    Surface(
        color = background,
        border = border,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .semantics { contentDescription = description }
            .clickable { onSelect(day.jalaliDate) }
    ) {
        Box(Modifier.fillMaxSize().padding(4.dp)) {
            Text(
                PersianDigits.convert(day.jalaliDate.day),
                color = if (day.isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected || day.jalaliDate == today) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.align(Alignment.Center)
            )
            if (day.notes.isNotEmpty()) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(17.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(PersianDigits.convert(day.notes.size), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (day.officialHolidays.isNotEmpty()) {
                Box(Modifier.align(Alignment.BottomStart).size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
            }
        }
    }
}

@Composable
private fun SelectedDayCard(
    day: CalendarDay?,
    isTeacher: Boolean,
    busy: Boolean,
    onNew: () -> Unit,
    onEdit: (CalendarNote) -> Unit,
    onDelete: (CalendarNote) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // V59.2 — برای روزهای گذشته ساخت/ویرایش پیام ممکن نیست (فقط حذف).
            val dayIsPast = day != null &&
                day.gregorianDate.isBefore(java.time.LocalDate.now())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    day?.jalaliDate?.longDisplay() ?: "یک روز را انتخاب کنید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isTeacher && day != null && !dayIsPast) {
                    Button(onClick = onNew, enabled = !busy) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("پیام")
                    }
                }
            }
            day?.let { selected ->
                // جمعه فقط با رنگ قرمز تقویم مشخص می‌شود؛ متن فقط برای مناسبت واقعی است.
                selected.officialHolidays.forEach { HolidayRow(it.title) }
                if (selected.notes.isEmpty()) {
                    Text(if (isTeacher) "برای این روز پیامی ثبت نشده است." else "پیامی برای این روز نیست.")
                }
                selected.notes.forEach { note ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(note.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (isTeacher) {
                                    // V59.2 — ویرایش فقط برای امروز/آینده؛ حذف همیشه هست.
                                    if (!dayIsPast) IconButton(onClick = { onEdit(note) }, enabled = !busy) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "ویرایش پیام")
                                    }
                                    IconButton(onClick = { onDelete(note) }, enabled = !busy) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "حذف پیام", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (note.body.isNotBlank()) Text(note.body)
                            if (isTeacher) Text(note.audience.faLabel(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HolidayRow(title: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
        Text("تعطیل: $title", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.fillMaxWidth().padding(9.dp))
    }
}

@Composable
private fun WarningCard(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(10.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun CalendarEditorDialog(
    state: CalendarState,
    onDismiss: () -> Unit,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onAudience: (CalendarAudience) -> Unit,
    onToggleClass: (String) -> Unit,
    onToggleStudent: (String) -> Unit,
    onToggleSchool: (String) -> Unit,
    onSave: () -> Unit
) {
    val editor = state.editor ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text((if (editor.id == null) "پیام جدید" else "ویرایش پیام") + " — " + editor.date.longDisplay()) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.editorLoading) item { CircularProgressIndicator() }
                item {
                    OutlinedTextField(
                        value = editor.title,
                        onValueChange = onTitle,
                        label = { Text("عنوان") },
                        supportingText = { Text("${PersianDigits.convert(editor.title.length)}/۱۲۰") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = editor.body,
                        onValueChange = onBody,
                        label = { Text("توضیحات") },
                        supportingText = { Text("${PersianDigits.convert(editor.body.length)}/۲۰۰۰") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    // V61.1 — مخاطبان و دکمه‌ها وسط‌چین؛ دکمهٔ «دانش‌آموزان» حذف شد
                    // (پیام‌های قدیمی دانش‌آموزی همچنان نمایش/ویرایش می‌شوند).
                    Text(
                        "مخاطبان",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // V61.6 — سه دکمه در «یک سطر» وسط‌چین.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(
                            CalendarAudience.ALL,
                            CalendarAudience.SCHOOLS,
                            CalendarAudience.CLASSES
                        ).forEach { audience ->
                            FilterChip(
                                selected = editor.audience == audience,
                                onClick = { onAudience(audience) },
                                label = { Text(audience.faLabel()) }
                            )
                        }
                    }
                }
                if (editor.audience == CalendarAudience.SCHOOLS) {
                    item { Text("مدارس", fontWeight = FontWeight.Bold) }
                    if (state.schools.isEmpty()) item { Text("عضو مدرسه‌ای نیستید.") }
                    items(state.schools, key = { "school-${it.id}" }) { option ->
                        AudienceCheck(option, option.id in editor.schoolIds) { onToggleSchool(option.id) }
                    }
                }
                if (editor.audience == CalendarAudience.CLASSES) {
                    item { Text("کلاس‌ها", fontWeight = FontWeight.Bold) }
                    if (state.classes.isEmpty()) item { Text("هنوز کلاسی وجود ندارد.") }
                    items(state.classes, key = { "class-${it.id}" }) { option ->
                        AudienceCheck(option, option.id in editor.classIds) { onToggleClass(option.id) }
                    }
                }
                if (editor.audience == CalendarAudience.STUDENTS) {
                    item { Text("دانش‌آموزان", fontWeight = FontWeight.Bold) }
                    if (state.students.isEmpty()) item { Text("دانش‌آموزی وجود ندارد.") }
                    items(state.students, key = { "student-${it.id}" }) { option ->
                        AudienceCheck(option, option.id in editor.studentIds) { onToggleStudent(option.id) }
                    }
                }
                state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !state.saving && !state.editorLoading && editor.title.isNotBlank()) {
                if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("ذخیره")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.saving) { Text("انصراف") } }
    )
}

@Composable
private fun AudienceCheck(option: CalendarAudienceOption, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column {
            Text(option.label)
            option.subtitle?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun CalendarAudience.faLabel(): String = when (this) {
    CalendarAudience.ALL -> "همه"
    CalendarAudience.SCHOOLS -> "مدارس"
    CalendarAudience.CLASSES -> "کلاس‌ها"
    CalendarAudience.STUDENTS -> "دانش‌آموزان"
}
