package ir.exam.app.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import ir.exam.app.R

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

    val fontFamily = when (appearance.appFont) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.VAZIRMATN -> FontFamily(Font(R.font.vazirmatn_regular))
        AppFont.SHABNAM -> FontFamily(Font(R.font.shabnam_regular))
        AppFont.SAHEL -> FontFamily(Font(R.font.sahel_regular))
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colors,
            typography = appTypography(fontFamily),
            content = content
        )
    }
}

private fun appTypography(font:FontFamily):Typography {
    val t=Typography()
    return t.copy(
        displayLarge=t.displayLarge.copy(fontFamily=font),displayMedium=t.displayMedium.copy(fontFamily=font),displaySmall=t.displaySmall.copy(fontFamily=font),
        headlineLarge=t.headlineLarge.copy(fontFamily=font),headlineMedium=t.headlineMedium.copy(fontFamily=font),headlineSmall=t.headlineSmall.copy(fontFamily=font),
        titleLarge=t.titleLarge.copy(fontFamily=font),titleMedium=t.titleMedium.copy(fontFamily=font),titleSmall=t.titleSmall.copy(fontFamily=font),
        bodyLarge=t.bodyLarge.copy(fontFamily=font),bodyMedium=t.bodyMedium.copy(fontFamily=font),bodySmall=t.bodySmall.copy(fontFamily=font),
        labelLarge=t.labelLarge.copy(fontFamily=font),labelMedium=t.labelMedium.copy(fontFamily=font),labelSmall=t.labelSmall.copy(fontFamily=font)
    )
}
