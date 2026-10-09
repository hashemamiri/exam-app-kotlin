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

/**
 * V235 — جست‌وجو و فیلتر فهرست آزمون‌ها (آزمون‌های آنلاین و چاپی)، آینهٔ فیلد جست‌وجوی دانش‌آموزان:
 * آیکن ذره‌بین → فیلد باز می‌شود؛ یک سمت فیلد «✕» (بستن + پاک کردن) و سمت دیگر آیکن فیلتر (فعال = قرمز).
 */
data class ExamListFilter(val subject: String? = null, val status: String? = null) { // status: open | closed | null
    val isActive: Boolean get() = subject != null || status != null
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
                        Text("درس", modifier = Modifier.padding(top = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = draft.subject == null, onClick = { draft = draft.copy(subject = null) }, label = { Text("همه") })
                        }
                        subjects.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                pair.forEach { s ->
                                    FilterChip(selected = draft.subject == s, onClick = { draft = draft.copy(subject = s) }, label = { Text(s, maxLines = 1) })
                                }
                            }
                        }
                    }
                    if (!showStatus && subjects.isEmpty()) Text("فعلاً درسی برای فیلتر وجود ندارد.")
                }
            },
            confirmButton = { TextButton(onClick = { onFilter(draft); filterOpen = false }) { Text("اعمال") } },
            dismissButton = { TextButton(onClick = { onFilter(ExamListFilter()); filterOpen = false }) { Text("پاک کردن") } }
        )
    }
}

/** تطبیق متن جست‌وجو با عنوان/درس/کد (بدون حساسیت به بزرگی حروف و فاصله‌های اضافی). */
fun examMatches(query: String, vararg fields: String?): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return fields.any { it?.lowercase()?.contains(q) == true }
}
