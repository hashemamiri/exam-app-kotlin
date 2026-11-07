package ir.exam.app.ui.code

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * V349 — انتخاب زبان محیط کدنویسی (۳۲ زبان).
 * V351 — کادر جست‌وجو حذف شد (درخواست کاربر).
 * V351.1 — نام‌های بلند بریده می‌شدند. دو علت داشت:
 *   ۱) حاشیهٔ پیش‌فرض دکمه ۲۴dp در هر طرف بود و پهنای سلول را می‌خورد؛
 *   ۲) Text داخل Row بدون weight پهنای نامحدود می‌گیرد و به‌جای شکستن
 *      سطر، از لبهٔ دکمه بیرون می‌زند و بریده می‌شود.
 * حالا حاشیه کم شده، نام weight گرفته و تا دو سطر می‌شکند.
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
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shown) { lang ->
                        OutlinedButton(
                            onClick = { onPick(lang) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                                    Text(
                                        lang.name,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    "." + lang.ext,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onDismiss) { Text("بستن") }
            }
        }
    }
}
