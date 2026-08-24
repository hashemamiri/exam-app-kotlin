package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.11 — دو گزارش دستگاه پس از V55.10:
 * ۱) «ضربدر نصفه است و فرمول ضربدر ندارد»:
 *    - ✕ قبلاً با top:-8px بیرون مرز توکن بود و overflow مرجع (auto/hidden روی
 *      جدول/اطلس) آن را می‌برید (اندازه‌گیری Chromium: xFullyVisible=false).
 *      اکنون داخل مرز (top:2px;left:2px) و با zoom معکوس ضدکوچک‌سازی V55.8.
 *    - فرمول‌ها (.qmf-atom) ✕ نداشتند؛ ناظر ۳۰۰ms اکنون ✕ را روی «هر» عنصر
 *      انتخاب‌شده (اتم فرمول یا توکن شکل) می‌گذارد؛ حذف فرمول از مسیر مرجع
 *      (Backspace روی اتم انتخاب‌شده) انجام می‌شود تا منبع دقیق اصلاح شود.
 * ۲) «کادر کلیک‌پذیر نیست؛ مکان‌نما نمی‌آید»:
 *    ریشه (Chromium): کل کادر داخل <label> مرجع است؛ کلیک روی سطح تایپ، فوکوس
 *    را به textarea مخفی (کنترلِ label) هدیه می‌داد و caret سطح از بین می‌رفت.
 *    رفع: click حبابی روی .qmf-surface → preventDefault (لغو هدیهٔ label) +
 *    focus سطح + caretRangeFromPoint از نقطهٔ لمس.
 * تأیید Chromium (تپ لمسی واقعی): caret روی متن و ناحیهٔ خالی؛ تایپ واقعی در
 * منبع نشست؛ ✕ فرمول و جدول هر دو کامل دیده و حذف هر دو کار کرد.
 */
class V55_11XCaretFixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/question_editor/question_editor.html").readText()
    }

    @Test
    fun `delete button stays inside token bounds and covers formulas too`() {
        val boot = asset.substringAfter("exam-editor-native-boot").substringBefore("</script>")
        assertTrue("function attachX(el2)" in boot)
        assertTrue("top:2px;left:2px" in boot)
        // خنثی‌سازی کوچک‌سازی V55.8 تا ✕ اندازهٔ لمس واقعی بماند.
        assertTrue("zoom:' + (1 / z)" in boot)
        // ناظر، ✕ را روی اتم فرمول انتخاب‌شدهٔ مرجع هم می‌گذارد.
        assertTrue("'.qmf-atom.is-on, .qmf-fig.is-on'" in boot)
        // حذف فرمول از مسیر مرجع (Backspace) است، نه دستکاری مستقیم منبع.
        assertTrue("new KeyboardEvent('keydown', { key: 'Backspace'" in boot)
    }

    @Test
    fun `surface click keeps focus and caret for typing around tokens`() {
        val boot = asset.substringAfter("exam-editor-native-boot").substringBefore("</script>")
        assertTrue("closest('.qmf-surface')" in boot)
        assertTrue("caretRangeFromPoint" in boot)
        assertTrue("surf.focus({ preventScroll: true })" in boot)
    }

    @Test
    fun `x button always deletes even when attached by the watcher`() {
        val boot = asset.substringAfter("exam-editor-native-boot").substringBefore("</script>")
        assertTrue("if (xBtn) { removeToken(fig); return; }" in boot)
    }
}
