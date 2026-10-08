package ir.exam.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import ir.exam.app.core.ui.AppearancePreferences
import ir.exam.app.core.ui.AppearanceSettings
import ir.exam.app.core.ui.ExamAppTheme
import ir.exam.app.ui.app.ExamApp

/** نقطهٔ ورود Native؛ ظاهر ماندگار پیش از رندر کل برنامه اعمال می‌شود. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // V214 — گرم کردن موتور WebView (Chromium) چند ثانیه پس از شروع، در زمان بیکاری: اولین باز شدن
        // پیش‌نمایش چاپ/ویرایشگر فرمول دیگر هزینهٔ راه‌اندازی WebView (~۰٫۵ تا ۱ ثانیه) را نمی‌پردازد.
        window.decorView.postDelayed({
            if (!isFinishing && !isDestroyed) runCatching { android.webkit.WebView(this).destroy() }
        }, WEBVIEW_WARMUP_DELAY_MS)
        setContent {
            val appearancePreferences = remember { AppearancePreferences(applicationContext) }
            val appearance by appearancePreferences.settings.collectAsState(initial = AppearanceSettings())
            // V137.5 — کلید «اعداد فارسی» برای رندرکننده‌های Canvas/SVG (خارج از Compose)
            ir.exam.app.core.figure.FigureDigits.persian = appearance.persianDigits
            ExamAppTheme(appearance) {
                ExamApp(appearance = appearance)
            }
        }
    }
}

/** V214 — تأخیر گرم کردن WebView پس از شروع */
private const val WEBVIEW_WARMUP_DELAY_MS = 3_000L
