package ir.exam.app

import ir.exam.app.ui.math.FormulaEditorPool
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
        // V214/V216 — چند ثانیه پس از شروع، WebView ویرایشگر فرمول از قبل ساخته و بارگذاری می‌شود (Chromium هم
        // گرم می‌شود): باز شدن ویرایشگر فرمول و اولین پیش‌نمایش چاپ دیگر هزینهٔ راه‌اندازی/parse را نمی‌پردازند.
        window.decorView.postDelayed({
            if (!isFinishing && !isDestroyed) FormulaEditorPool.prepare(this)
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // V216 — در کمبود حافظه، WebView پارک‌شدهٔ ویرایشگر فرمول آزاد می‌شود (دفعهٔ بعد مثل قبل تازه ساخته می‌شود).
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) FormulaEditorPool.release()
    }

    override fun onDestroy() {
        FormulaEditorPool.release()
        super.onDestroy()
    }
}

/** V214 — تأخیر گرم کردن WebView پس از شروع */
private const val WEBVIEW_WARMUP_DELAY_MS = 3_000L
