package ir.exam.app.ui.code

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * V349 — انتخاب زبان محیط کدنویسی (۳۲ زبان) با جست‌وجو.
 * نام فارسی، نام انگلیسی و پسوند فایل هر زبان جست‌وجو می‌شود.
 */
@Composable
fun CodeLanguagePickerDialog(
    title: String = "زبان برنامه‌نویسی را انتخاب کنید",
    onDismiss: () -> Unit,
    onPick: (CodeLanguage) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) CodeLanguages.all
        else CodeLanguages.all.filter {
            it.name.lowercase().contains(q) || it.id.contains(q) ||
                it.short.lowercase().contains(q) || it.ext.contains(q)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "محیط نوشتن کد با رنگی‌شدن دستورها، شمارهٔ خط، علائم و قطعه‌های آماده باز می‌شود. کد اجرا نمی‌شود.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("جست‌وجوی زبان") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (shown.isEmpty()) {
                    Text("زبانی با این نام پیدا نشد.", fontSize = 13.sp)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 132.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shown) { lang ->
                            OutlinedButton(onClick = { onPick(lang) }, modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(lang.colorArgb)) {
                                            Text(
                                                lang.short,
                                                Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                        Text(lang.name, fontSize = 12.sp)
                                    }
                                    Text("." + lang.ext, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onDismiss) { Text("بستن") }
            }
        }
    }
}
