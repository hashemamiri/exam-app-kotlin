package ir.exam.app.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ir.exam.app.R

/** رنگ‌های اصلی طرح ۶۹؛ در حالت dynamic، پالت دستگاه همچنان اولویت دارد. */
fun NeumorphicPalette.accentColors(): Pair<Color, Color> = when (this) {
    NeumorphicPalette.INDIGO_MINT -> Color(0xFF6C63F5) to Color(0xFF27C4A8)
    NeumorphicPalette.BLUE_CYAN -> Color(0xFF1877D2) to Color(0xFF32B7C6)
    NeumorphicPalette.PINK_ORANGE -> Color(0xFFE96D8A) to Color(0xFFFFA14E)
    NeumorphicPalette.PURPLE_PINK -> Color(0xFF8C5AD7) to Color(0xFFEC6DA7)
}

/** پوستهٔ واقعی برنامه با تم، پالت نئومورفیک و اندازه متن ماندگار. */
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
    val (accent, accent2) = appearance.neumorphicPalette.accentColors()
    val colors = when {
        appearance.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        appearance.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> neumorphicDarkColorScheme(accent, accent2)
        else -> neumorphicLightColorScheme(accent, accent2)
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
        AppFont.VAZIRMATN -> FontFamily(
            Font(R.font.vazirmatn_regular, FontWeight.Normal),
            Font(R.font.vazirmatn_medium, FontWeight.Medium),
            Font(R.font.vazirmatn_medium, FontWeight.SemiBold),
            Font(R.font.vazirmatn_bold, FontWeight.Bold)
        )
        AppFont.SHABNAM -> FontFamily(Font(R.font.shabnam_regular))
        AppFont.SAHEL -> FontFamily(Font(R.font.sahel_regular))
    }

    // V56.0: چیدمان تبلت/گوشی طبق انتخاب کاربر (یا تشخیص خودکار از اندازهٔ صفحه)
    // برای کل درخت UI فراهم می‌شود.
    val tabletLayout = resolveTabletLayout(appearance.deviceLayoutMode)
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalTabletLayout provides tabletLayout
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = appTypography(fontFamily),
            shapes = NeumorphicShapes,
            content = content
        )
    }
}

private val NeumorphicShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private fun neumorphicLightColorScheme(accent: Color, accent2: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = lerp(Color(0xFFE9EEF5), accent, .16f),
    onPrimaryContainer = Color(0xFF263142),
    secondary = accent2,
    onSecondary = Color.White,
    secondaryContainer = lerp(Color(0xFFE9EEF5), accent2, .17f),
    onSecondaryContainer = Color(0xFF263142),
    tertiary = accent2,
    background = Color(0xFFE9EEF5),
    onBackground = Color(0xFF263142),
    surface = Color(0xFFE9EEF5),
    onSurface = Color(0xFF263142),
    surfaceVariant = Color(0xFFDCE3EC),
    onSurfaceVariant = Color(0xFF667386),
    outline = Color(0xFF8995A6),
    error = Color(0xFFC85B6B),
    onError = Color.White
)

private fun neumorphicDarkColorScheme(accent: Color, accent2: Color): androidx.compose.material3.ColorScheme {
    val background = Color(0xFF20252E)
    val lightAccent = lerp(accent, Color.White, .16f)
    val lightAccent2 = lerp(accent2, Color.White, .13f)
    return darkColorScheme(
        primary = lightAccent,
        onPrimary = Color(0xFF171A20),
        primaryContainer = lerp(background, accent, .34f),
        onPrimaryContainer = Color(0xFFF0F3F8),
        secondary = lightAccent2,
        onSecondary = Color(0xFF171A20),
        secondaryContainer = lerp(background, accent2, .30f),
        onSecondaryContainer = Color(0xFFF0F3F8),
        tertiary = lightAccent2,
        background = background,
        onBackground = Color(0xFFF0F3F8),
        surface = background,
        onSurface = Color(0xFFF0F3F8),
        surfaceVariant = Color(0xFF2A313C),
        onSurfaceVariant = Color(0xFFB8C2D0),
        outline = Color(0xFF7E8998),
        error = Color(0xFFFF8FA0),
        onError = Color(0xFF2B1116)
    )
}

private fun appTypography(font: FontFamily): Typography {
    val t = Typography()
    return t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = font),
        displayMedium = t.displayMedium.copy(fontFamily = font),
        displaySmall = t.displaySmall.copy(fontFamily = font),
        headlineLarge = t.headlineLarge.copy(fontFamily = font),
        headlineMedium = t.headlineMedium.copy(fontFamily = font),
        headlineSmall = t.headlineSmall.copy(fontFamily = font),
        titleLarge = t.titleLarge.copy(fontFamily = font),
        titleMedium = t.titleMedium.copy(fontFamily = font),
        titleSmall = t.titleSmall.copy(fontFamily = font),
        bodyLarge = t.bodyLarge.copy(fontFamily = font),
        bodyMedium = t.bodyMedium.copy(fontFamily = font),
        bodySmall = t.bodySmall.copy(fontFamily = font),
        labelLarge = t.labelLarge.copy(fontFamily = font),
        labelMedium = t.labelMedium.copy(fontFamily = font),
        labelSmall = t.labelSmall.copy(fontFamily = font)
    )
}
