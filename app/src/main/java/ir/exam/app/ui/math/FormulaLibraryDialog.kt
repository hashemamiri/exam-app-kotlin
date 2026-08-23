package ir.exam.app.ui.math

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import kotlinx.coroutines.withTimeoutOrNull
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
                        Text("${entries.size} مورد", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onDismiss) { Text("✕ بستن") }
                }
                if (entries.isEmpty()) {
                    Text(
                        "این کتابخانه هنوز موردی ندارد.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(118.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(entries, key = { it.label + "¦" + it.tex }) { entry ->
                            Card(
                                Modifier.pointerInput(entry.label + "¦" + entry.tex) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        val releasedEarly = withTimeoutOrNull(2000L) {
                                            waitForUpOrCancellation()
                                        }
                                        if (releasedEarly == null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onToggleFavorite(entry)
                                        } else {
                                            onUse(entry)
                                        }
                                    }
                                }
                            ) {
                                NativeFormulaIcon(
                                    tex = entry.tex,
                                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(7.dp),
                                    fontSize = 20.sp,
                                    contentDescription = "فرمول"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
