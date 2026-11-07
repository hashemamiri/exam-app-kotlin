package ir.exam.app.ui.code

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * V349 — انتخاب زبان محیط کدنویسی (۳۲ زبان).
 * V351 — کادر جست‌وجو حذف شد.
 * V351.1 — نام‌های بلند دیگر بریده نمی‌شوند.
 * V351.2 — خواسته‌های کاربر:
 *   • «بستن» به ضربدر قرمز تبدیل شد و در همان سطر عنوان نشست.
 *   • جملهٔ توضیحی حذف شد تا فضای بیشتری برای زبان‌ها بماند.
 *   • پنجره بلندتر شد: شبکهٔ زبان‌ها همهٔ ارتفاع باقی‌مانده را می‌گیرد.
 *   • برای هر زبان آیکن SVG در کاشی رنگ خودش نشست.
 *   • نام انگلیسی در پرانتز جلوی نام فارسی آمد و هر دو وسط‌چین شدند.
 */
@Composable
fun CodeLanguagePickerDialog(
    title: String = "زبان برنامه‌نویسی را انتخاب کنید",
    onDismiss: () -> Unit,
    onPick: (CodeLanguage) -> Unit
) {
    val shown = CodeLanguages.all
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) WindowCompat.setDecorFitsSystemWindows(window, false)
            onDispose { }
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val availableHeight = maxHeight
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp).heightIn(max = availableHeight),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ضربدر قرمز و عنوان، در یک سطر و وسط‌چین
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color(0xFFE5484D), shape = MaterialTheme.shapes.medium) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "بستن",
                                    tint = Color.White
                                )
                            }
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 148.dp),
                        // همهٔ ارتفاع باقی‌مانده؛ پنجره بلندتر دیده می‌شود
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shown) { lang ->
                            val tile = Color(lang.colorArgb)
                            // روی رنگ‌های روشن، نشان تیره تا خوانا بماند
                            val mark = if (tile.luminance() > 0.6f) Color(0xFF111111) else Color.White
                            OutlinedButton(
                                onClick = { onPick(lang) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = tile,
                                        shape = RoundedCornerShape(9.dp),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            CodeLanguageIcons.of(lang.id),
                                            contentDescription = lang.english,
                                            tint = mark,
                                            modifier = Modifier.padding(6.dp).size(22.dp)
                                        )
                                    }
                                    Text(
                                        lang.name + " (" + lang.english + ")",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
