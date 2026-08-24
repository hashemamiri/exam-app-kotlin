package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.10 — رفتار جدید توکن‌های درج‌شده (درخواست صریح کاربر):
 * ۱) کلیک اول = «انتخاب» + دکمهٔ ✕ قرمز گوشهٔ توکن برای حذف؛
 * ۲) کلیک دوم = رفتن به ویرایشگر (t/p/a/s → Native از مسیر __nativeFigEdit؛
 *    بقیه مثل هندسه/نمودار → ویرایشگر مرجع با رویداد synthetic دارای پرچم
 *    __nativeAllow که از گیرندهٔ capture ما عبور می‌کند)؛
 * ۳) حذف با ✕: توکن %%FIG:...%% از textarea حذف و input واقعی dispatch می‌شود؛
 * ۴) کلیک بیرون = لغو انتخاب بدون بلعیدن رویداد → caret برای تایپ قبل/بعد توکن
 *    سر جای خودش می‌نشیند؛
 * ۵) «کادر اسکرول‌پذیر نیست»: LazyColumn ژست عمودی WebView را می‌قاپید؛ HTML
 *    پرچم onScrollableChanged می‌فرستد و WebView سفارشی هنگام لمس، با
 *    requestDisallowInterceptTouchEvent والد را کنار می‌زند.
 * تأیید Chromium (۷ سناریو در یک صفحه): انتخاب+✕ بدون overlay؛ ویرایش Native
 * آناتومی؛ انتخاب و ویرایشگر مرجع هندسه؛ حذف با ✕ (توکن از متن رفت)؛ لغو با
 * کلیک بیرون؛ تایپ قبل/بعد با حفظ توکن؛ پرچم اسکرول true — همه PASS.
 */
class V55_10SelectDeleteTypeTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/question_editor/question_editor.html").readText()
    }
    private val webField by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt").readText()
    }

    @Test
    fun `first click selects with delete button and second click edits`() {
        val boot = asset.substringAfter("exam-editor-native-boot").substringBefore("</script>")
        assertTrue("native-fig-x" in boot)
        assertTrue("selectFig(fig)" in boot)
        assertTrue("removeToken(fig)" in boot)
        assertTrue("__nativeAllow" in boot)
        // کلیک دوم: t/p/a/s به Native؛ بقیه به مرجع.
        assertTrue("kind !== 't' && kind !== 'p' && kind !== 'a' && kind !== 's'" in boot)
        assertTrue("refEdit(fig)" in boot)
    }

    @Test
    fun `delete rewrites the source token and outside click clears selection`() {
        val boot = asset.substringAfter("exam-editor-native-boot").substringBefore("</script>")
        assertTrue("'%%FIG:' + raw + '%%'" in boot)
        assertTrue("new Event('input', { bubbles: true })" in boot)
        assertTrue("if (type === 'click' && sel.fig) clearSel();" in boot)
    }

    @Test
    fun `inner scroll is unlocked from the parent list gesture`() {
        assertTrue("onScrollableChanged" in asset)
        assertTrue("fun onScrollableChanged(scrollable: Boolean)" in webField)
        assertTrue("requestDisallowInterceptTouchEvent(true)" in webField)
        assertTrue("innerScrollable" in webField)
    }
}
