package ir.exam.app.core.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "native_appearance")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppFont { SYSTEM, VAZIRMATN, SHABNAM, SAHEL }

/** چهار پالت اصلی طرح Native نئومورفیک ۶۹. */
enum class NeumorphicPalette { INDIGO_MINT, BLUE_CYAN, PINK_ORANGE, PURPLE_PINK }

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: Float = 1f,
    val dynamicColors: Boolean = true,
    val appFont: AppFont = AppFont.VAZIRMATN,
    val neumorphicPalette: NeumorphicPalette = NeumorphicPalette.INDIGO_MINT,
    val neumorphicDepth: Float = 14f
)

/** تنظیمات ظاهر فقط روی دستگاه ذخیره می‌شوند و هیچ دادهٔ حساب یا token در آن نیست. */
class AppearancePreferences(context: Context) {
    private val store = context.applicationContext.appearanceDataStore

    val settings: Flow<AppearanceSettings> = store.data
        .map { values ->
            AppearanceSettings(
                themeMode = values[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                fontScale = (values[FONT_SCALE] ?: 1f).coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE),
                dynamicColors = values[DYNAMIC_COLORS] ?: true,
                appFont = values[APP_FONT]?.let { runCatching { AppFont.valueOf(it) }.getOrNull() }
                    ?: AppFont.VAZIRMATN,
                neumorphicPalette = values[NEUMORPHIC_PALETTE]?.let {
                    runCatching { NeumorphicPalette.valueOf(it) }.getOrNull()
                } ?: NeumorphicPalette.INDIGO_MINT,
                neumorphicDepth = (values[NEUMORPHIC_DEPTH] ?: DEFAULT_NEO_DEPTH)
                    .coerceIn(MIN_NEO_DEPTH, MAX_NEO_DEPTH)
            )
        }
        .catch { emit(AppearanceSettings()) }

    suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    suspend fun setFontScale(scale: Float) {
        store.edit { it[FONT_SCALE] = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE) }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        store.edit { it[DYNAMIC_COLORS] = enabled }
    }

    suspend fun setAppFont(font: AppFont) {
        store.edit { it[APP_FONT] = font.name }
    }

    suspend fun setNeumorphicPalette(palette: NeumorphicPalette) {
        store.edit { it[NEUMORPHIC_PALETTE] = palette.name }
    }

    suspend fun setNeumorphicDepth(depth: Float) {
        store.edit { it[NEUMORPHIC_DEPTH] = depth.coerceIn(MIN_NEO_DEPTH, MAX_NEO_DEPTH) }
    }

    suspend fun reset() {
        store.edit { it.clear() }
    }

    companion object {
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.30f
        const val MIN_NEO_DEPTH = 8f
        const val MAX_NEO_DEPTH = 22f
        const val DEFAULT_NEO_DEPTH = 14f
        private val THEME = stringPreferencesKey("theme")
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val APP_FONT = stringPreferencesKey("app_font")
        private val NEUMORPHIC_PALETTE = stringPreferencesKey("neumorphic_palette")
        private val NEUMORPHIC_DEPTH = floatPreferencesKey("neumorphic_depth")
    }
}
