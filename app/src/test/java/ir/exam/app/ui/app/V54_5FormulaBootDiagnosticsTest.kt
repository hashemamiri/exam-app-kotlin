package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V54.5 — رفع boot نشدن ویرایشگر فرمول در پنجرهٔ تمام‌صفحه + تشخیص امن:
 * ۱) علت قطعی: shouldOverrideUrlLoading با «true» برای همهٔ ناوبری‌ها، ناوبری
 *    داخلی iframe ویرایشگر مرجع (document.open روی فریم فرعی) را در WebView
 *    بی‌صدا می‌شکست؛ اکنون فقط ناوبری خارجیِ main frame مسدود است.
 * ۲) خطاهای واقعی JS/console دیگر بی‌صدا گم نمی‌شوند: پل onError پاک‌سازی‌شده
 *    (بدون URL/Token)، WebChromeClient برای console و نگهبان timeout مخصوص boot.
 */
class V54_5FormulaBootDiagnosticsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val host by lazy { source("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt") }

    @Test
    fun `formula host surfaces console and bridge errors for the user`() {
        assertTrue("onConsoleMessage" in host)
        assertTrue("MessageLevel.ERROR" in host)
        assertTrue("onJsError" in host)
        assertTrue("خطای ویرایشگر" in host)
    }
}
