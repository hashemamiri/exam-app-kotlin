package ir.exam.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V215 — تولید Baseline Profile با اندازه‌گیری واقعی: شروع سرد برنامه تا پایدار شدن صفحهٔ اول
 * (بازیابی نشست/صفحهٔ ورود) + چند پیمایش. بدون ورود به حساب (هیچ رمزی در CI نیست)، پس مسیرهای
 * داخل پنل پوشش داده نمی‌شوند؛ آن‌ها را پروفایل دستی `app/src/main/baseline-prof.txt` پوشش می‌دهد.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "ir.exam.app",
        includeInStartupProfile = true,
        maxIterations = 8
    ) {
        pressHome()
        startActivityAndWait()
        // صبر تا صفحهٔ اول (پس از بازیابی نشست) پایدار شود
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 10_000)
        device.waitForIdle(3_000)
        // چند پیمایش عمودی روی هر فهرست قابل‌اسکرول (اگر باشد)
        repeat(2) {
            val scrollable = device.findObject(By.scrollable(true))
            if (scrollable != null) {
                scrollable.setGestureMargin(device.displayWidth / 5)
                scrollable.fling(Direction.DOWN)
                device.waitForIdle()
                scrollable.fling(Direction.UP)
                device.waitForIdle()
            }
        }
    }
}
