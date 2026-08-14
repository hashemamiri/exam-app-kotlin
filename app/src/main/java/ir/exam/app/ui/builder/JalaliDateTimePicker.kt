package ir.exam.app.ui.builder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.JalaliDate
import ir.exam.app.core.calendar.PersianDigits
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun JalaliDateTimeField(
    label: String,
    iso: String?,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    minimumIso: String? = null
) {
    var open by remember { mutableStateOf(false) }
    val configured = iso != null
    Button(
        onClick = { open = true },
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (configured) Color(0xFF19945B) else Color(0xFFD63B49),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(17.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(
                iso?.let(::jalaliDisplay).orEmpty().ifBlank { "تعیین نشده" },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
    if (open) {
        JalaliDateTimeDialog(
            initialIso = iso,
            minimumIso = minimumIso,
            title = label,
            canClear = iso != null,
            onDismiss = { open = false },
            onClear = {
                onChange(null)
                open = false
            },
            onConfirm = {
                onChange(it)
                open = false
            }
        )
    }
}

@Composable
private fun JalaliDateTimeDialog(
    initialIso: String?,
    minimumIso: String?,
    title: String,
    canClear: Boolean,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initial = remember(initialIso) { parseInitial(initialIso) }
    val minimumInstant = remember(minimumIso) { minimumIso?.let { runCatching { Instant.parse(it) }.getOrNull() } }
    val minimumDate = remember(minimumInstant) {
        minimumInstant?.atZone(ZoneId.systemDefault())?.toLocalDate()?.let(JalaliCalendar::fromGregorian)
    }
    var selected by remember(initialIso) { mutableStateOf(initial.first) }
    var visibleYear by remember(initialIso) { mutableStateOf(initial.first.year) }
    var visibleMonth by remember(initialIso) { mutableStateOf(initial.first.month) }
    var hour by remember(initialIso) { mutableStateOf(initial.second.toString().padStart(2, '0')) }
    var minute by remember(initialIso) { mutableStateOf(initial.third.toString().padStart(2, '0')) }
    var error by remember { mutableStateOf<String?>(null) }

    fun shiftMonth(delta: Int) {
        runCatching { JalaliCalendar.shiftMonth(visibleYear, visibleMonth, delta) }
            .onSuccess { (year, month) -> visibleYear = year; visibleMonth = month }
    }

    fun confirmManual() {
        runCatching {
            val h = hour.toInt()
            val m = minute.toInt()
            require(h in 0..23 && m in 0..59) { "ساعت یا دقیقه نامعتبر است." }
            val candidate = JalaliCalendar.toGregorian(selected)
                .atTime(h, m)
                .atZone(ZoneId.systemDefault())
                .toInstant()
            require(minimumInstant == null || !candidate.isBefore(minimumInstant)) {
                "زمان پایان نمی‌تواند قبل از زمان شروع باشد."
            }
            candidate.toString()
        }.onSuccess(onConfirm)
            .onFailure { error = it.message ?: "تاریخ نامعتبر است." }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 10.dp
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("انتخاب $title", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { shiftMonth(-1) },
                        enabled = visibleYear > JalaliCalendar.MIN_YEAR || visibleMonth > 1
                    ) { Icon(Icons.Outlined.ChevronRight, contentDescription = "ماه قبل") }
                    Text(
                        PersianDigits.convert("${JalaliCalendar.MONTH_NAMES[visibleMonth - 1]} $visibleYear"),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { shiftMonth(1) },
                        enabled = visibleYear < JalaliCalendar.MAX_YEAR || visibleMonth < 12
                    ) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "ماه بعد") }
                }
                DateWeekHeader()
                DateMonthGrid(
                    year = visibleYear,
                    month = visibleMonth,
                    selected = selected,
                    minimumDate = minimumDate,
                    onSelect = { selected = it; error = null }
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = hour,
                            onValueChange = {
                                hour = PersianDigits.latin(it).filter(Char::isDigit).take(2)
                                error = null
                            },
                            label = { Text("ساعت") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minute,
                            onValueChange = {
                                minute = PersianDigits.latin(it).filter(Char::isDigit).take(2)
                                error = null
                            },
                            label = { Text("دقیقه") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color(0xFF19945B), shape = RoundedCornerShape(16.dp)) {
                        IconButton(onClick = ::confirmManual) {
                            Icon(Icons.Outlined.Check, contentDescription = "تأیید زمان", tint = Color.White)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val now = LocalDateTime.now()
                            val today = JalaliCalendar.fromGregorian(now.toLocalDate())
                            selected = today
                            visibleYear = today.year
                            visibleMonth = today.month
                            hour = now.hour.toString().padStart(2, '0')
                            minute = now.minute.toString().padStart(2, '0')
                            error = if (minimumInstant != null && Instant.now().isBefore(minimumInstant)) {
                                "زمان اکنون قبل از شروع است؛ پایان را بعد از شروع انتخاب کنید."
                            } else null
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("اکنون", fontWeight = FontWeight.Bold) }
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        IconButton(onClick = onClear, enabled = canClear) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "حذف زمان تعیین‌شده",
                                tint = if (canClear) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = .32f)
                            )
                        }
                    }
                    Surface(color = Color(0xFFD63B49), shape = RoundedCornerShape(16.dp)) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "انصراف", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateWeekHeader() {
    Row(Modifier.fillMaxWidth()) {
        JalaliCalendar.WEEKDAY_NAMES.forEachIndexed { index, name ->
            Text(
                name,
                modifier = Modifier.weight(1f).padding(vertical = 3.dp),
                textAlign = TextAlign.Center,
                color = if (index == 6) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DateMonthGrid(
    year: Int,
    month: Int,
    selected: JalaliDate,
    minimumDate: JalaliDate?,
    onSelect: (JalaliDate) -> Unit
) {
    val lead = JalaliCalendar.firstDayOffset(year, month)
    val days = (1..JalaliCalendar.monthLength(year, month)).map { JalaliDate(year, month, it) }
    val cells: List<JalaliDate?> = List(lead) { null } + days
    val today = JalaliCalendar.today()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        cells.chunked(7).forEach { partial ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (partial + List(7 - partial.size) { null }).forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val active = date == selected
                        val enabled = minimumDate == null ||
                            !JalaliCalendar.toGregorian(date).isBefore(JalaliCalendar.toGregorian(minimumDate))
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable(enabled = enabled) { onSelect(date) },
                            shape = CircleShape,
                            color = when {
                                active -> MaterialTheme.colorScheme.primary
                                date == today -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> Color.Transparent
                            },
                            border = if (active) null else BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    PersianDigits.convert(date.day),
                                    color = when {
                                        active -> MaterialTheme.colorScheme.onPrimary
                                        enabled -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = .28f)
                                    },
                                    fontWeight = if (active || date == today) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseInitial(iso: String?): Triple<JalaliDate, Int, Int> = runCatching {
    val local = Instant.parse(iso ?: error("empty"))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    Triple(JalaliCalendar.fromGregorian(local.toLocalDate()), local.hour, local.minute)
}.getOrElse {
    val now = LocalDateTime.now()
    Triple(JalaliCalendar.fromGregorian(now.toLocalDate()), now.hour, now.minute)
}

fun jalaliDisplay(iso: String): String = runCatching {
    val local = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val jalali = JalaliCalendar.fromGregorian(local.toLocalDate())
    "${jalali.display()} ${PersianDigits.convert(local.toLocalTime().toString().take(5))}"
}.getOrDefault("")
