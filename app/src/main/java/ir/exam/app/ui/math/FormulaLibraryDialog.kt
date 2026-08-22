package ir.exam.app.ui.math

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** کتابخانهٔ تمام‌صفحه؛ بازشدن دسته دیگر به scroll مخفی پایین صفحه وابسته نیست. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormulaLibraryDialog(
    title: String,
    entries: List<FormulaReferenceEntry>,
    isFavorite: (FormulaReferenceEntry) -> Boolean,
    onUse: (FormulaReferenceEntry) -> Unit,
    onToggleFavorite: (FormulaReferenceEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember(title, entries) { mutableStateOf("") }
    val filtered = remember(entries, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) entries else entries.filter {
            it.label.lowercase().contains(normalized) || it.tex.lowercase().contains(normalized)
        }
    }
    val haptic = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text("${filtered.size} از ${entries.size} مورد", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onDismiss) { Text("✕ بستن") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(120) },
                    label = { Text("جست‌وجوی نام یا نماد…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (filtered.isEmpty()) {
                    Text(
                        if (entries.isEmpty()) "این کتابخانه هنوز موردی ندارد." else "نتیجه‌ای پیدا نشد.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(118.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.label + "¦" + it.tex }) { entry ->
                            Card(
                                Modifier.combinedClickable(
                                    onClick = { onUse(entry) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleFavorite(entry)
                                    }
                                )
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    NativeFormulaIcon(
                                        tex = entry.tex,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        fontSize = 20.sp,
                                        contentDescription = entry.label
                                    )
                                    Text(
                                        entry.label.take(45),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { onUse(entry) }) { Text("درج") }
                                        TextButton(onClick = { onToggleFavorite(entry) }) {
                                            Text(if (isFavorite(entry)) "★" else "☆")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
