package ir.exam.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

/**
 * V235 — جست‌وجو و فیلتر فهرست آزمون‌ها (آزمون‌های آنلاین و چاپی)، آینهٔ فیلد جست‌وجوی دانش‌آموزان:
 * آیکن ذره‌بین → فیلد باز می‌شود؛ یک سمت فیلد «✕» (بستن + پاک کردن) و سمت دیگر آیکن فیلتر (فعال = قرمز).
 */
data class ExamListFilter(val subjects: Set<String> = emptySet(), val status: String? = null) { // status: open | closed | null
    val isActive: Boolean get() = subjects.isNotEmpty() || status != null
    /** V237 — خالی = همهٔ درس‌ها. */
    fun matchesSubject(subject: String?): Boolean = subjects.isEmpty() || (subject != null && subject in subjects)
}

@Composable
fun ExamSearchIcon(open: Boolean, onOpen: () -> Unit, contentDescription: String = "جست‌وجوی آزمون") {
    if (!open) IconButton(onClick = onOpen) { Icon(Icons.Outlined.Search, contentDescription = contentDescription) }
}

@Composable
fun ExamSearchField(
    open: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    filter: ExamListFilter,
    onFilter: (ExamListFilter) -> Unit,
    subjects: List<String>,
    showStatus: Boolean,
    label: String = "جست‌وجوی عنوان، درس یا کد آزمون"
) {
    var filterOpen by remember { mutableStateOf(false) }
    AnimatedVisibility(visible = open, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text(label) },
            leadingIcon = {
                IconButton(onClick = { filterOpen = true }) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = "فیلتر آزمون‌ها",
                        tint = if (filter.isActive) Color(0xFFD32F2F) else LocalContentColor.current
                    )
                }
            },
            trailingIcon = {
                IconButton(onClick = { onQuery(""); onFilter(ExamListFilter()); onClose() }) {
                    Icon(Icons.Outlined.Close, contentDescription = "بستن جست‌وجو")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (filterOpen) {
        var draft by remember(filter) { mutableStateOf(filter) }
        var subjectPickerOpen by remember { mutableStateOf(false) }
        if (subjectPickerOpen) {
            SubjectPickerDialog(
                subjects = subjects,
                selected = draft.subjects,
                onDismiss = { subjectPickerOpen = false },
                onApply = { draft = draft.copy(subjects = it); subjectPickerOpen = false }
            )
        }
        AlertDialog(
            onDismissRequest = { filterOpen = false },
            title = { Text("فیلتر آزمون‌ها") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showStatus) {
                        Text("وضعیت")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = draft.status == null, onClick = { draft = draft.copy(status = null) }, label = { Text("همه") })
                            FilterChip(selected = draft.status == "open", onClick = { draft = draft.copy(status = "open") }, label = { Text("باز") })
                            FilterChip(selected = draft.status == "closed", onClick = { draft = draft.copy(status = "closed") }, label = { Text("بسته") })
                        }
                    }
                    if (subjects.isNotEmpty()) {
                        // V237 — «همه» + دکمهٔ «انتخاب درس» → پنجرهٔ چیپ‌های وسط‌چین با انتخاب چندتایی
                        Text("درس", modifier = Modifier.padding(top = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = draft.subjects.isEmpty(), onClick = { draft = draft.copy(subjects = emptySet()) }, label = { Text("همه") })
                            FilterChip(
                                selected = draft.subjects.isNotEmpty(),
                                onClick = { subjectPickerOpen = true },
                                label = { Text(if (draft.subjects.isEmpty()) "انتخاب درس" else "انتخاب درس (${draft.subjects.size})", maxLines = 1) }
                            )
                        }
                        if (draft.subjects.isNotEmpty()) Text(draft.subjects.sorted().joinToString("، "), style = MaterialTheme.typography.bodySmall)
                    }
                    if (!showStatus && subjects.isEmpty()) Text("فعلاً درسی برای فیلتر وجود ندارد.")
                }
            },
            confirmButton = { TextButton(onClick = { onFilter(draft); filterOpen = false }) { Text("اعمال") } },
            dismissButton = { TextButton(onClick = { onFilter(ExamListFilter()); filterOpen = false }) { Text("پاک کردن") } }
        )
    }
}

/** V237 — پنجرهٔ انتخاب چند درس: چیپ‌های وسط‌چین؛ لمس = انتخاب/لغو. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectPickerDialog(
    subjects: List<String>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var picked by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب درس", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    subjects.forEach { s ->
                        FilterChip(
                            selected = s in picked,
                            onClick = { picked = if (s in picked) picked - s else picked + s },
                            label = { Text(s, maxLines = 1) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(picked) }) { Text(if (picked.isEmpty()) "همهٔ درس‌ها" else "تأیید (${picked.size})") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/** تطبیق متن جست‌وجو با عنوان/درس/کد (بدون حساسیت به بزرگی حروف و فاصله‌های اضافی). */
fun examMatches(query: String, vararg fields: String?): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return fields.any { it?.lowercase()?.contains(q) == true }
}
