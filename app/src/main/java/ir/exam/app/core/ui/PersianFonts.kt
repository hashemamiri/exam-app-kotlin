package ir.exam.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.exam.app.R

@Composable
@ReadOnlyComposable
fun persianFontFamily(key: String): FontFamily = when (key.lowercase()) {
    "vazir", "vazirmatn" -> FontFamily(
        Font(R.font.vazirmatn_regular, FontWeight.Normal),
        Font(R.font.vazirmatn_medium, FontWeight.Medium),
        Font(R.font.vazirmatn_medium, FontWeight.SemiBold),
        Font(R.font.vazirmatn_bold, FontWeight.Bold)
    )
    "shabnam" -> FontFamily(Font(R.font.shabnam_regular))
    "sahel" -> FontFamily(Font(R.font.sahel_regular))
    else -> FontFamily.Default
}
