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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * V349 — انتخاب زبان محیط کدنویسی (۳۲ زبان).
 * V351 — کادر جست‌وجو حذف شد (درخواست کاربر): همهٔ زبان‌ها یک‌جا دیده
 * می‌شوند و با یک لمس انتخاب می‌شوند.
 */
@Composable
fun CodeLanguagePickerDialog(
    title: String = "زبان برنامه‌نویسی را انتخاب کنید",
    onDismiss: () -> Unit,
    onPick: (CodeLanguage) -> Unit
) {
    val shown = CodeLanguages.all
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "محیط نوشتن کد با رنگی‌شدن دستورها، شمارهٔ خط، علائم و قطعه‌های آماده باز می‌شود. کد اجرا نمی‌شود.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                OutlinedButton(onClick = onDismiss) { Text("بستن") }
            }
        }
    }
}
