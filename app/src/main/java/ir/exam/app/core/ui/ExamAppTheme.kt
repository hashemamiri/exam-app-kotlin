package ir.exam.app.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** پوستهٔ واقعی برنامه با تم و اندازه متن ماندگار. */
@Composable
fun ExamAppTheme(
    appearance: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = when (appearance.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        appearance.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        appearance.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val density = LocalDensity.current
    val scaledDensity = Density(
        density = density.density,
        fontScale = density.fontScale * appearance.fontScale.coerceIn(
            AppearancePreferences.MIN_FONT_SCALE,
            AppearancePreferences.MAX_FONT_SCALE
        )
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
