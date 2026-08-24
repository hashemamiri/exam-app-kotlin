package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.3 — رفع «صفحهٔ سفید با مودالِ باز» (عکس دستگاه با برچسب N55.2):
 * برچسب سبز دیده می‌شد یعنی asset درست بود؛ هیچ خطای FORMULA_OPEN_TIMEOUT هم
 * نیامد یعنی مودال کلاس open را گرفته بود؛ و رنگ کل صفحه دقیقاً --bg1 تم روشن
 * بود یعنی پس‌زمینهٔ خود مودال رسم می‌شد ولی بچه‌هایش paint نمی‌شدند.
 * این علامتِ باگ compositing در WebView اندروید است: WebView شفاف +
 * backdrop-filter مودال مرجع + will-change:transform روی .modal-box.
 * رفع سه‌لایه:
 * ۱) asset: تزریق nativePaintFix فقط داخل برنامه (backdrop-filter و will-change
 *    مودال خنثی؛ ظاهر تمام‌صفحه تغییری نمی‌کند چون پس‌زمینهٔ آن مات است)؛
 * ۲) Kotlin: پس‌زمینهٔ WebView مات (#E9EEF5) به‌جای شفاف؛
 * ۳) تشخیص FORMULA_BLANK_LAYOUT: پس از باز شدن مودال، ابعاد واقعی عناصر و
 *    نسخهٔ Chrome دستگاه اندازه‌گیری و در صورت خالی‌بودن، قرمز گزارش می‌شود.
 */
class V55_3PaintFixTest {
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
    fun `native paint fix neutralizes compositing hazards only inside the app`() {
        assertTrue("nativePaintFix" in asset)
        assertTrue("backdrop-filter:none !important" in asset)
        assertTrue("will-change:auto !important" in asset)
        // فقط وقتی پل Native هست تزریق می‌شود؛ مرورگر عادی دست‌نخورده می‌ماند.
        assertTrue("if (!window.ExamEditorNative) return;" in asset)
    }

    @Test
    fun `webview background is opaque and matches the reference theme`() {
        assertTrue("Color.parseColor(\"#E9EEF5\")" in host)
        assertTrue("Color.TRANSPARENT" !in host)
    }

    @Test
    fun `blank layout diagnostic measures real geometry after the modal opens`() {
        assertTrue("FORMULA_BLANK_LAYOUT" in asset)
        assertTrue("getBoundingClientRect" in asset)
        assertTrue("layoutCheck()" in asset)
        // V55.3.1 — نسخهٔ Chrome «قبل از» رشتهٔ خطا استخراج می‌شود (var ua = ...)؛
        // پس کل بلوک تشخیص بررسی می‌شود، نه فقط متن بعد از marker.
        assertTrue("Chrome\\/[\\d.]+" in asset)
        assertTrue("window.innerWidth" in asset.substringAfter("FORMULA_BLANK_LAYOUT").take(600))
    }
}
