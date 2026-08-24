package ir.exam.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

/**
 * V56.0 — چیدمان تبلت/گوشی.
 *
 * true یعنی چیدمان تبلت فعال است؛ در کل درخت UI از طریق ExamAppTheme فراهم
 * می‌شود تا هر صفحه بدون دسترسی به تنظیمات، چیدمان درست را انتخاب کند.
 */
val LocalTabletLayout = staticCompositionLocalOf { false }

/** آستانهٔ استاندارد اندروید برای تبلت: کوچک‌ترین بُعد صفحه دست‌کم ۶۰۰dp. */
const val TABLET_MIN_SMALLEST_WIDTH_DP = 600

/** آیا این دستگاه از نظر سخت‌افزار تبلت شمرده می‌شود؟ (مستقل از انتخاب کاربر) */
@Composable
fun isTabletDevice(): Boolean =
    LocalConfiguration.current.smallestScreenWidthDp >= TABLET_MIN_SMALLEST_WIDTH_DP

/**
 * تصمیم نهایی چیدمان بر اساس انتخاب کاربر در بخش ظاهر:
 * AUTO → تشخیص خودکار از اندازهٔ صفحه؛ PHONE/TABLET → انتخاب صریح کاربر.
 */
@Composable
fun resolveTabletLayout(mode: DeviceLayoutMode): Boolean = when (mode) {
    DeviceLayoutMode.AUTO -> isTabletDevice()
    DeviceLayoutMode.PHONE -> false
    DeviceLayoutMode.TABLET -> true
}
