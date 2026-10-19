package ir.exam.app.ui.builder

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.core.calendar.PersianDigits

/** V306 / ۱۲ — بانک آمادهٔ ابتدایی: فیلتر پایه/درس و افزودن سؤال به آزمون (هر بار یک سؤال). */
@Composable
fun PrimaryBankDialog(viewModel: ExamBuilderViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bankItems = remember {
        runCatching {
            PrimaryBank.parseAll(context.assets.open(PrimaryBank.ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() })
        }.getOrDefault(emptyList())
    }
    val subjects = remember(bankItems) { bankItems.map { it.subject }.distinct() }
    var grade by remember { mutableIntStateOf(0) }
    var subject by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val visible = bankItems.filter {
        (grade == 0 || it.grade == grade) &&
            (subject.isEmpty() || it.subject == subject) &&
            (query.isBlank() || it.text.contains(query.trim(), ignoreCase = true) || it.subject.contains(query.trim(), ignoreCase = true))
    }
    val added = remember { mutableStateListOf<String>() }
    val letters = listOf("الف", "ب", "ج", "د", "ه", "و")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("بستن") }
                    Text("بانک آمادهٔ ابتدایی", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (visible.isNotEmpty()) {
                        OutlinedButton(onClick = {
                            val drafts = visible.mapNotNull { PrimaryBank.toDraft(it) }
                            if (drafts.isNotEmpty()) {
                                viewModel.addPrimaryBankDrafts(drafts)
                                visible.forEach { if (it.id !in added) added.add(it.id) }
                            }
                        }) { Text("➕ افزودن همه (${PersianDigits.convert(visible.size)})") }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجو در متن سؤال یا مبحث...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = grade == 0, onClick = { grade = 0 }, label = { Text("همه پایه‌ها") })
                    (1..6).forEach { g ->
                        FilterChip(selected = grade == g, onClick = { grade = g }, label = { Text("پایهٔ ${PersianDigits.convert(g)}") })
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = subject.isEmpty(), onClick = { subject = "" }, label = { Text("همه درس‌ها") })
                    subjects.forEach { s ->
                        FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text(s) })
                    }
                }
                Text("${PersianDigits.convert(visible.size)} سؤال · افزوده‌شده در این نشست: ${PersianDigits.convert(added.size)}", style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visible, key = { it.id }) { bankItem ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${bankItem.subject} · پایهٔ ${PersianDigits.convert(bankItem.grade)}", style = MaterialTheme.typography.labelMedium)
                                Text(bankItem.text, style = MaterialTheme.typography.bodyLarge)
                                if (bankItem.options.isNotEmpty()) {
                                    Text(
                                        bankItem.options.mapIndexed { i, o -> "${letters.getOrElse(i) { (i + 1).toString() }}) $o" }.joinToString("   "),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(onClick = {
                                    PrimaryBank.toDraft(bankItem)?.let {
                                        viewModel.addPrimaryBankDrafts(listOf(it))
                                        added.add(bankItem.id)
                                    }
                                }) { Text(if (bankItem.id in added) "➕ دوباره افزودن" else "➕ افزودن به آزمون") }
                            }
                        }
                    }
                }
            }
        }
    }
}
