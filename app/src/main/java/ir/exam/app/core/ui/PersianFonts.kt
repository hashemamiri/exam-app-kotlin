package ir.exam.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import ir.exam.app.R

@Composable
@ReadOnlyComposable
fun persianFontFamily(key: String): FontFamily = when (key.lowercase()) {
    "vazir", "vazirmatn" -> FontFamily(Font(R.font.vazirmatn_regular))
    "shabnam" -> FontFamily(Font(R.font.shabnam_regular))
    "sahel" -> FontFamily(Font(R.font.sahel_regular))
    else -> FontFamily.Default
}
