package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V54.7 — fallback مخصوص WebView برای boot ویرایشگر فرمول:
 * تست اجرایی jsdom ثابت کرد مسیر مرجع (doc.write) در مرورگر استاندارد سالم است
 * (display=block، mfModal open، editorReady=true)؛ پس شکست دستگاه مخصوص
 * document.write در WebView است. اگر تا ۲.۵ ثانیه ویرایشگر داخل iframe آماده
 * نشود، همان MATH_EDITOR_HTML مرجع با srcdoc بازسازی و __openMathEditor مرجع
 * دوباره صدا زده می‌شود؛ boot مرجع (ready=false، booted=true) poll تازه روی
 * iframe جدید می‌سازد و ادامهٔ مسیر کاملاً کد مرجع است.
 */
class V54_7SrcdocFallbackTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/question_editor/question_editor.html").readText()
    }

    @Test
    fun `fallback rebuilds the editor iframe from the reference source via srcdoc`() {
        assertTrue("iframeReady" in asset)
        assertTrue("fresh.srcdoc = MATH_EDITOR_HTML" in asset)
        assertTrue("fEl.parentNode.replaceChild(fresh, fEl)" in asset)
        // پس از آماده‌شدن، همان مسیر مرجع دوباره اجرا می‌شود (نه مسیر موازی).
        val fallback = asset.substringAfter("V54.7 — fallback مخصوص WebView")
        assertTrue("window.__openMathEditor()" in fallback)
        assertTrue("__mathHostTheme.on()" in fallback)
        // خطاهای مرحله‌بندی‌شده برای تشخیص در صورت شکست دوباره.
        listOf("FALLBACK_UNAVAILABLE", "SRCDOC_BOOT_TIMEOUT", "OPEN_MATH_RETRY", "FORMULA_BOOT_TIMEOUT").forEach {
            assertTrue("missing diagnostic: $it", it in asset)
        }
    }

    @Test
    fun `normal path is untouched and fallback fires only when the editor is not ready`() {
        val fallback = asset.substringAfter("V54.7 — fallback مخصوص WebView")
        assertTrue("if (iframeReady()) return;" in fallback)
        // کد مرجع boot دست‌نخورده است.
        assertTrue("doc.open(); doc.write(MATH_EDITOR_HTML); doc.close();" in asset)
    }
}
