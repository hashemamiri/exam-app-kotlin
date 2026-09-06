package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V95 — دو گزارش کاربر پس از جایگزینیِ بایت‌به‌بایتِ ویرایشگر (V94):
 * ۱) «بخش دست‌نویس کار نمی‌کند» — مودالِ دست‌نویس (hwModal) روی دستگاهِ کاربر
 *    با همان باگِ compositingِ شناخته‌شده (WebView + backdrop-filter + انیمیشن)
 *    ممکن بود باز ولی خالی/سفید paint شود؛ nativePaintFix حالا backdrop-filter
 *    و انیمیشنِ مودالِ دست‌نویس را هم فقط داخل برنامه خنثی می‌کند. علاوه بر آن
 *    openHw به‌صورت window.mbHwOpen بیرون داده شد تا دکمهٔ مداد در هر حالتی
 *    (حتی اگر override پایانی hwUiJs اجرا نشود) مودال را باز کند.
 * ۲) «پنجرهٔ دکمهٔ () باریک است» — منوی سه‌ستونیِ پرانتز (mbParMenu) زیر
 *    enforcement دستگاه‌محورِ V55.5/V55.8 (style مستقیم، بدون min()/dvh) نبود؛
 *    حالا wrap شده و دکمه‌هایش لمس‌پذیر و خوانا می‌شوند (min-height 46px و
 *    فونت بزرگ‌تر)، و عرض منو تا 520px/96vw باز می‌شود.
 */
class V95_1HandwritingParenFixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
    }

    @Test
    fun `handwriting modal compositing hazards are neutralized only in-app`() {
        // backdrop-filter مودال دست‌نویس (#mback) و انیمیشن #hwModal خنثی می‌شوند.
        assertTrue("'#mback{backdrop-filter:none !important;-webkit-backdrop-filter:none !important;}'" in asset)
        assertTrue("'#hwModal{animation:none !important;will-change:auto !important;}'" in asset)
    }

    @Test
    fun `pencil button falls back to the real handwriting modal`() {
        // V96 — اتصالِ زودهنگام: window.mbHwOpen بلافاصله پس از guard ثبت می‌شود
        // و stub اولیهٔ دکمهٔ مداد در نبودِ آن، همین قلاب را صدا می‌زند.
        assertTrue("window.mbHwOpen = function () {" in asset)
        assertTrue("window.mbPencilAction = function (ev)" in asset)
        assertTrue("if (typeof window.mbHwOpen === 'function') { window.mbHwOpen(); return; }" in asset)
    }

    @Test
    fun `paren menu is wired into the device-proof inline enforcement`() {
        assertTrue("wrapNow('mbParMenu', afterMenu)" in asset)
        assertTrue(".mbv-cat, .mbv-i, .mbv-q, .mbv-parbtn" in asset)
        assertTrue("var pars = pop.querySelectorAll('.mbv-parbtn')" in asset)
        // عرضِ بیشتر برای منوی سه‌ستونی، بدون وابستگی به min()
        assertTrue("pop.querySelector('.mbv-par3')" in asset)
    }

    @Test
    fun `paren menu buttons are enlarged for touch and readability`() {
        assertTrue("gap: 8px; min-height: 46px; padding: 6px 10px;" in asset)
        assertTrue(".mbv-par-lab { font-size: .8rem;" in asset)
        assertTrue(".mbv-par-g { font-size: 1.6rem;" in asset)
    }
}
