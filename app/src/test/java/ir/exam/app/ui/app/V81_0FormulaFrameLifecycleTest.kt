package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V81.0 — چرا آیکن فرمول همچنان هیچ پنجره‌ای باز نمی‌کرد.
 *
 * V79.0 ویرایشگر را به بارگذاریِ هم‌مبدأ با src برد و V80.0 ریستِ سرگردان را
 * بست. هر دو لازم بودند، اما دو باگِ چرخهٔ عمرِ فریم باقی مانده بود:
 *
 *  ۱) `hideExactMathFrame()` فریم را با `f.remove()` **حذف** می‌کرد. این با
 *     doc.write سازگار بود (چون هر بار از نو نوشته می‌شد) ولی با src نه:
 *     دفعهٔ بعد یک فریمِ خالیِ بدونِ src ساخته می‌شد و ویرایشگر هرگز نمی‌آمد.
 *
 *  ۲) گاردِ ضدِ دابل‌کلیکِ R11 شرطِ آزادسازی‌اش `f.style.display === 'none'`
 *     بود؛ اما فریم در خودِ HTML است و با CSS پنهان می‌شود، پس سبکِ inline
 *     رشتهٔ خالی است و هرگز 'none' نمی‌شد. یک بوتِ ناتمام گارد را تا ۶۰ ثانیه
 *     قفل می‌کرد و همهٔ کلیک‌های بعدی بی‌صدا رد می‌شدند.
 */
class V81_0FormulaFrameLifecycleTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val assetText by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }

    @Test
    fun `closing the editor hides the frame instead of removing it`() {
        val hide = assetText.substringAfter("function hideExactMathFrame()").substringBefore("\n}")
        assertTrue("پنهان‌سازی نیست", "setProperty('display', 'none', 'important')" in hide)
        assertFalse("فریم نباید حذف شود", "f.remove()" in hide)
    }

    @Test
    fun `the double-click gate reads the computed style`() {
        assertTrue("getComputedStyle(f).display !== 'none'" in assetText)
        // شرطِ شکستهٔ قبلی نباید برگردد
        assertFalse("f.style.display === 'none') __r11MathBusy = false" in assetText)
    }

    @Test
    fun `a failed boot cannot lock the gate for a minute`() {
        assertTrue("__r11MathBusyTimer" in assetText)
        assertTrue("__r11MathBusy = false; }, 8000)" in assetText)
    }

    @Test
    fun `a stale boot is retried instead of being permanent`() {
        assertTrue("if (booted && !ready)" in assetText)
        assertTrue("if (stale) booted = false" in assetText)
        // مهلتِ ۶ ثانیه هم باید اجازهٔ تلاش دوباره بدهد
        assertTrue("clearInterval(poll); booted = false;" in assetText)
    }

    @Test
    fun `opening uses important so it beats the important hide`() {
        assertTrue("setProperty('display', 'block', 'important')" in assetText)
    }

    @Test
    fun `a rebuilt frame resets the boot state`() {
        assertTrue("__qmfMathFrameReset" in assetText)
        assertTrue("booted = false; ready = false;" in assetText)
    }

    @Test
    fun `there is an xhr fallback when src does not deliver`() {
        assertTrue("xhr.open('GET', MATH_EDITOR_URL, true)" in assetText)
        assertTrue("dd.open(); dd.write(body); dd.close();" in assetText)
        assertTrue("__qmfMathBootError" in assetText)
    }

    @Test
    fun `the app can report why the editor failed`() {
        assertTrue("window.__qmfFormulaDiag" in assetText)
        listOf("hasOpenMath", "docBytes", "computedDisplay", "bootError", "editorUrl")
            .forEach { assertTrue("کلید $it در تشخیص نیست", it in assetText) }
        assertTrue("🩺 بررسی فرمول" in dialog)
        assertTrue("mathAssetProbe" in dialog)
        assertTrue("assets.open(\"print/math_editor.html\")" in dialog)
    }

    @Test
    fun `earlier fixes are all still in place`() {
        assertTrue("f.src = MATH_EDITOR_URL" in assetText)      // V79.0
        assertFalse("doc.write(MATH_EDITOR_HTML)" in assetText) // V79.0
        assertTrue("if (url != MAIN_PAGE_URL) return" in dialog) // V80.0
        assertTrue("data.reset && !data.force" in assetText)     // V80.0
        assertTrue("activeExactTool !== 'formula'" in assetText) // V78.0
    }
}
