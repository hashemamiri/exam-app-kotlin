package ir.exam.app.data.repository

import ir.exam.app.core.ui.AppFont
import ir.exam.app.core.ui.AppearancePreferences
import ir.exam.app.core.ui.AppearanceSettings
import ir.exam.app.core.ui.DeviceLayoutMode
import ir.exam.app.core.ui.NeumorphicPalette
import ir.exam.app.core.ui.ThemeMode
import ir.exam.app.data.local.PrintHeaderStore
import ir.exam.app.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * V327 — همگام‌سازی تنظیمات با حساب.
 *
 * تا امروز تنظیمات ظاهر فقط در DataStore همین دستگاه بود؛ معلمی که هم با گوشی و هم با دسکتاپ
 * کار می‌کند، هر جا از نو پالت/قلم/سربرگ را می‌چید. این کلاس همان ردیف JSON سرور
 * (جدول user_settings، RPCهای native_settings_get_v327 / native_settings_put_v327) را
 * می‌خواند و می‌نویسد. تعارض = «آخرین نوشتن برنده»؛ pull فقط یک‌بار در شروع صفحهٔ تنظیمات.
 */
class SettingsSyncRepository(
    private val appearance: AppearancePreferences,
    private val header: PrintHeaderStore
) {

    data class Snapshot(
        val appearance: JsonObject? = null,
        val printHeader: JsonObject? = null,
        val pageSetup: JsonObject? = null
    )

    suspend fun pull(): Snapshot? = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_settings_get_v327").decodeAs<JsonObject>()
        if ((raw["ok"]?.jsonPrimitive?.booleanOrNull) == false) return null
        Snapshot(
            appearance = raw["appearance"] as? JsonObject,
            printHeader = raw["print_header"] as? JsonObject,
            pageSetup = raw["page_setup"] as? JsonObject
        )
    }.getOrNull()

    /** اعمال تنظیمات سرور روی دستگاه (فقط اگر مقداری آمده باشد). */
    suspend fun applyRemote(snapshot: Snapshot?) {
        snapshot ?: return
        val a = snapshot.appearance ?: JsonObject(emptyMap())
        fun str(k: String) = (a[k] as? JsonPrimitive)?.contentOrNull
        fun bool(k: String) = (a[k] as? JsonPrimitive)?.booleanOrNull
        fun num(k: String) = (a[k] as? JsonPrimitive)?.floatOrNull

        if (a.isNotEmpty()) {
            str("themeMode")?.let { v -> runCatching { ThemeMode.valueOf(v) }.getOrNull()?.let { appearance.setTheme(it) } }
            num("fontScale")?.let { appearance.setFontScale(it) }
            bool("dynamicColors")?.let { appearance.setDynamicColors(it) }
            str("appFont")?.let { v -> runCatching { AppFont.valueOf(v) }.getOrNull()?.let { appearance.setAppFont(it) } }
            str("neumorphicPalette")?.let { v -> runCatching { NeumorphicPalette.valueOf(v) }.getOrNull()?.let { appearance.setNeumorphicPalette(it) } }
            num("neumorphicDepth")?.let { appearance.setNeumorphicDepth(it) }
            str("deviceLayoutMode")?.let { v -> runCatching { DeviceLayoutMode.valueOf(v) }.getOrNull()?.let { appearance.setDeviceLayoutMode(it) } }
            bool("persianDigits")?.let { appearance.setPersianDigits(it) }
            bool("highContrast")?.let { appearance.setHighContrast(it) }
        }
        snapshot.printHeader?.takeIf { it.isNotEmpty() }?.let { obj ->
            val map = obj.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { k to it } }.toMap()
            if (map.isNotEmpty()) runCatching { header.write(map) }
        }
    }

    /** ارسال تنظیمات ظاهر فعلی به سرور (پس از هر تغییر). */
    suspend fun pushAppearance() {
        val s = appearance.settings.first()
        val body = appearanceJson(s)
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "native_settings_put_v327",
                buildJsonObject {
                    put("p_appearance", body)
                    put("p_print_header", headerJson())
                }
            )
        }
    }

    private fun headerJson(): JsonObject? = runCatching {
        val map = header.read()
        if (map.isEmpty()) null else buildJsonObject { map.forEach { (k, v) -> put(k, v) } }
    }.getOrNull()

    private fun appearanceJson(s: AppearanceSettings): JsonObject = buildJsonObject {
        put("themeMode", s.themeMode.name)
        put("fontScale", s.fontScale)
        put("dynamicColors", s.dynamicColors)
        put("appFont", s.appFont.name)
        put("neumorphicPalette", s.neumorphicPalette.name)
        put("neumorphicDepth", s.neumorphicDepth)
        put("deviceLayoutMode", s.deviceLayoutMode.name)
        put("persianDigits", s.persianDigits)
        put("highContrast", s.highContrast)
    }
}
