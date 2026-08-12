package ir.exam.app.ui.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.JalaliDate
import ir.exam.app.core.calendar.PersianDigits
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun JalaliDateTimeField(label: String, iso: String?, onChange: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Text("$label: ${iso?.let(::jalaliDisplay).orEmpty().ifBlank { "تعیین نشده" }}")
        }
        if (iso != null) TextButton(onClick = { onChange(null) }) { Text("پاک") }
    }
    if (open) JalaliDateTimeDialog(
        initialIso = iso,
        onDismiss = { open = false },
        onConfirm = { onChange(it); open = false }
    )
}

@Composable
private fun JalaliDateTimeDialog(initialIso: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val initial = parseInitial(initialIso)
    var year by remember(initialIso) { mutableStateOf(initial.first.year.toString()) }
    var month by remember(initialIso) { mutableStateOf(initial.first.month.toString()) }
    var day by remember(initialIso) { mutableStateOf(initial.first.day.toString()) }
    var hour by remember(initialIso) { mutableStateOf(initial.second.toString()) }
    var minute by remember(initialIso) { mutableStateOf(initial.third.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ و ساعت شمسی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("سال ۱۴۰۰ تا ۱۵۰۰؛ ساعت بر اساس منطقه زمانی گوشی ذخیره می‌شود.")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(year, { year = PersianDigits.latin(it).filter(Char::isDigit).take(4) }, label={Text("سال")}, modifier=Modifier.weight(1f))
                    OutlinedTextField(month, { month = PersianDigits.latin(it).filter(Char::isDigit).take(2) }, label={Text("ماه")}, modifier=Modifier.weight(1f))
                    OutlinedTextField(day, { day = PersianDigits.latin(it).filter(Char::isDigit).take(2) }, label={Text("روز")}, modifier=Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(hour, { hour = PersianDigits.latin(it).filter(Char::isDigit).take(2) }, label={Text("ساعت")}, modifier=Modifier.weight(1f))
                    OutlinedTextField(minute, { minute = PersianDigits.latin(it).filter(Char::isDigit).take(2) }, label={Text("دقیقه")}, modifier=Modifier.weight(1f))
                }
                error?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(onClick = {
                runCatching {
                    val date = JalaliDate(year.toInt(),month.toInt(),day.toInt())
                    val h=hour.toInt(); val m=minute.toInt()
                    require(h in 0..23 && m in 0..59) { "ساعت یا دقیقه نامعتبر است." }
                    JalaliCalendar.toGregorian(date).atTime(h,m).atZone(ZoneId.systemDefault()).toInstant().toString()
                }.onSuccess(onConfirm).onFailure { error=it.message ?: "تاریخ نامعتبر است." }
            }) { Text("تأیید") }
        },
        dismissButton = { TextButton(onClick=onDismiss) { Text("انصراف") } }
    )
}

private fun parseInitial(iso: String?): Triple<JalaliDate,Int,Int> = runCatching {
    val local = Instant.parse(iso ?: error("empty")).atZone(ZoneId.systemDefault()).toLocalDateTime()
    Triple(JalaliCalendar.fromGregorian(local.toLocalDate()),local.hour,local.minute)
}.getOrElse {
    val now=LocalDateTime.now()
    Triple(JalaliCalendar.fromGregorian(now.toLocalDate()),now.hour,now.minute)
}

fun jalaliDisplay(iso: String): String = runCatching {
    val local=Instant.parse(iso).atZone(ZoneId.systemDefault())
    val j=JalaliCalendar.fromGregorian(local.toLocalDate())
    "${j.display()} ${PersianDigits.convert(local.toLocalTime().toString().take(5))}"
}.getOrDefault("")
