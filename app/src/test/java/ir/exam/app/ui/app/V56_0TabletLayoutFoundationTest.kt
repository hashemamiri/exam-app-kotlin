package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V56.0 — زیرساخت بهینه‌سازی تبلت (پچ ۱ از ۳):
 * ۱) DeviceLayoutMode جدید (AUTO/PHONE/TABLET) در تنظیمات ظاهر با ذخیرهٔ
 *    ماندگار DataStore؛ پیش‌فرض AUTO.
 * ۲) LocalTabletLayout در ExamAppTheme برای کل درخت UI فراهم می‌شود؛
 *    AUTO از smallestScreenWidthDp >= 600 (استاندارد اندروید) تشخیص می‌دهد.
 * ۳) بخش «ظاهر» کارت «چیدمان دستگاه» با سه چیپ خودکار/گوشی/تبلت دارد.
 */
class V56_0TabletLayoutFoundationTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val prefs by lazy { source("app/src/main/java/ir/exam/app/core/ui/AppearancePreferences.kt") }
    private val layout by lazy { source("app/src/main/java/ir/exam/app/core/ui/DeviceLayout.kt") }
    private val theme by lazy { source("app/src/main/java/ir/exam/app/core/ui/ExamAppTheme.kt") }
    private val settings by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt") }
    private val settingsVm by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt") }

    @Test
    fun `device layout mode is a persisted appearance setting`() {
        assertTrue("enum class DeviceLayoutMode { AUTO, PHONE, TABLET }" in prefs)
        assertTrue("val deviceLayoutMode: DeviceLayoutMode = DeviceLayoutMode.AUTO" in prefs)
        assertTrue("suspend fun setDeviceLayoutMode(mode: DeviceLayoutMode)" in prefs)
        assertTrue("stringPreferencesKey(\"device_layout\")" in prefs)
        // خواندن مقاوم: مقدار نامعتبر ذخیره‌شده به AUTO برمی‌گردد
        assertTrue("runCatching { DeviceLayoutMode.valueOf(it) }.getOrNull()" in prefs)
    }

    @Test
    fun `tablet detection follows the android 600dp standard and user override`() {
        assertTrue("val LocalTabletLayout = staticCompositionLocalOf { false }" in layout)
        assertTrue("const val TABLET_MIN_SMALLEST_WIDTH_DP = 600" in layout)
        assertTrue("smallestScreenWidthDp >= TABLET_MIN_SMALLEST_WIDTH_DP" in layout)
        assertTrue("DeviceLayoutMode.AUTO -> isTabletDevice()" in layout)
        assertTrue("DeviceLayoutMode.PHONE -> false" in layout)
        assertTrue("DeviceLayoutMode.TABLET -> true" in layout)
    }

    @Test
    fun `theme provides the tablet layout to the whole tree`() {
        assertTrue("resolveTabletLayout(appearance.deviceLayoutMode)" in theme)
        assertTrue("LocalTabletLayout provides tabletLayout" in theme)
    }

    @Test
    fun `appearance section offers the auto phone tablet choice`() {
        assertTrue("چیدمان دستگاه" in settings)
        assertTrue("DeviceLayoutMode.AUTO to \"خودکار\"" in settings)
        assertTrue("DeviceLayoutMode.PHONE to \"گوشی\"" in settings)
        assertTrue("DeviceLayoutMode.TABLET to \"تبلت\"" in settings)
        assertTrue("viewModel.setDeviceLayoutMode(mode)" in settings)
        assertTrue("fun setDeviceLayoutMode(mode: DeviceLayoutMode)" in settingsVm)
    }
}
