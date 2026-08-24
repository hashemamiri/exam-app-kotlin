package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.1 — رفع «صفحهٔ سفید» پنجرهٔ فرمول (گزارش دستگاه پس از V55):
 * badge نسخهٔ فایل در اسکرین‌شات دیده می‌شد یعنی JS کامل اجرا شده بود؛ اما
 * race زمان‌بندی WebView (onPageFinished قبل از پایان parse یا load دیرهنگام
 * سند intercepted) باعث می‌شد begin قبل از آماده‌شدن openMath صدا شود و مودال
 * هرگز باز نشود. رفع دوطرفه:
 * ۱) JS: begin تا بازشدن واقعی مودال هر ۱۲۰ms تلاش می‌کند (سقف ۱۰s + خطای
 *    تشخیصی FORMULA_OPEN_TIMEOUT)؛
 * ۲) Kotlin: تا تعریف‌شدن پل، فراخوانی begin با evaluateJavascript callback
 *    تکرار می‌شود.
 * صحت با تست اجرایی jsdom (دزدیدن openMath و برگرداندن دیرهنگام آن) تأیید شد.
 */
class V55_1FormulaOpenRetryTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
    }
    private val host by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt").readText()
    }

    @Test
    fun `begin retries until the modal really opens`() {
        assertTrue("modal.classList.contains('open')" in asset)
        assertTrue("setInterval" in asset.substringAfter("V55.1"))
        assertTrue("FORMULA_OPEN_TIMEOUT" in asset)
        assertTrue("window.__examFormulaHostReady = true" in asset)
    }

    @Test
    fun `kotlin retries begin until the bridge exists`() {
        assertTrue("fun tryBegin()" in host)
        // V55.2 — retry به when با شاخهٔ خطای صریح BRIDGE_NOT_READY ارتقا یافت.
        assertTrue("result?.contains(\"ok\") == true" in host)
        assertTrue("postDelayed({ tryBegin() }, 150)" in host)
    }
}
