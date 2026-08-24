package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.9 — سه گزارش دستگاه (عکس photo_2026-08-24_11-43-36.jpg):
 * ۱) «با کلیک روی توکن آناتومی، کل کادر انتخاب می‌شود و کادر خاکستری ظاهر
 *    می‌شود»: در Chromium بازتولید شد — کلیک تکی روی .qmf-fig ویرایشگر مرجع
 *    (anOverlay با پس‌زمینهٔ خاکستری) را داخل همان WebView کوچک باز می‌کرد؛
 *    شنوندهٔ dblclick قبلی ما دیر ثبت می‌شد و کلیک تکی را نمی‌گرفت.
 *    رفع: گیرندهٔ click+dblclick در بلوک boot ابتدای head (capture، مقدم بر
 *    شنونده‌های مرجع) برای انواع t/p/a/s؛ مسیر مرجع کاملاً قطع و به
 *    __nativeFigEdit → ExamEditorNative.onEditFigure سپرده می‌شود. توکن‌های
 *    مرجع (مثل هندسه) به ویرایشگر مرجع می‌روند (تست Chromium: gfOverlay باز شد).
 *    دو رویداد کلیک متوالی (click,click,dblclick واقعی) با پنجرهٔ ۷۰۰ms یک
 *    ویرایش حساب می‌شود.
 * ۲) «کادر خاکستری هنوز وجود دارد»: همان overlay مرجع بود که با کلیک باز می‌شد
 *    و WebView را با ارتفاع بزرگ (overlayOpen=560dp) خاکستری می‌کرد؛ با قطع
 *    مسیر بالا دیگر باز نمی‌شود.
 * ۳) «هرچه در سؤال ۱ بنویسم در همهٔ سؤال‌ها ظاهر می‌شود»: اشتراک state از مسیر
 *    localStorage/HTML نیست (تست Chromium با دو صفحهٔ هم‌context: مقدار منتقل
 *    نشد)؛ ریشه بازیافت AndroidView در LazyColumn است که WebView یک سؤال را با
 *    closureهای سؤال دیگر نگه می‌داشت. رفع: key(controller) دور AndroidView —
 *    با تعویض سؤال، WebView قبلی دور انداخته و از نو ساخته می‌شود.
 */
class V55_9NativeFigEditIsolationTest {
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
    fun `single click on native token kinds is captured before reference editors`() {
        val boot = asset.substringAfter("exam-editor-native-boot").substringBefore("</script>")
        assertTrue("['click', 'dblclick']" in boot)
        assertTrue("stopImmediatePropagation" in boot)
        assertTrue("__nativeFigEdit" in boot)
        // فقط انواع دارای ویرایشگر Native؛ بقیه به مرجع می‌روند.
        assertTrue("kind !== 't' && kind !== 'p' && kind !== 'a' && kind !== 's'" in boot)
        // V55.10 — ضدتکرار زمانی جایگزین شد: dblclick کاربر بلعیده می‌شود چون
        // توالی click,click خودش انتخاب→ویرایش را انجام داده است.
        assertTrue("if (type === 'dblclick') return;" in boot)
    }

    @Test
    fun `native fig edit delivers through the kotlin bridge`() {
        assertTrue("window.__nativeFigEdit = function (raw)" in asset)
        assertTrue("ExamEditorNative.onEditFigure(raw)" in asset)
    }

    @Test
    fun `each question gets its own webview instance`() {
        assertTrue("key(controller)" in webField)
        assertTrue("import androidx.compose.runtime.key" in webField)
    }
}
