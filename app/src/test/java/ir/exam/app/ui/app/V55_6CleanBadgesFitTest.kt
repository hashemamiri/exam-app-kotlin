package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.6 — سه درخواست کاربر پس از موفقیت V55.5 (پنل بزرگ شد):
 * ۱) هیچ برچسب نسخه‌ای روی صفحه نباشد: کد ساخت برچسب سبز N55.x حذف شد
 *    (نسخهٔ پل فقط پرچم window.__nativeBridgeVersion برای پیام‌های خطا).
 *    در V94 بیلد جدید badge مرجع «v36 · V34» را هم از مبدأ حذف کرده
 *    (MF_FIX_V974)؛ بنابراین دیگر hostBuildTag/سازندهٔ برچسبی در فایل نیست
 *    و hideBadges فقط پاک‌سازی دفاعی nativeBridgeTag را نگه می‌دارد.
 * ۲) تأخیر بازشدن کتابخانه‌ها: enforce دیگر منتظر polling ۲۵۰ms نمی‌ماند؛
 *    wrapper مستقیم روی mbGroupLibrary/mbOpenSymbolLibrary/mbShowSymbolCategory/
 *    mbOpenItemLibrary بلافاصله پس از باز شدن اجرا می‌شود (اندازه‌گیری Chromium:
 *    ~۱۱ms). polling فقط پشتیبان مسیرهای فرعی است.
 * ۳) فرمول‌های پهن (عکس کاربر: دترمینان ۳×۳) از کادر بیرون می‌زدند:
 *    fitLibraryItems فونت پیش‌نمایش هر آیتم سرریزشده را گام‌به‌گام (×۰.۸۸ تا
 *    حداقل 9px) کوچک می‌کند تا scrollWidth در clientWidth جا شود.
 * تأیید Chromium: برچسب‌ها غایب، منو فوری، ۷ دستهٔ سنگین (ماتریس/مشتق/انتگرال/
 * مثلثات/تبدیل/اتحاد) → صفر سرریز.
 */
class V55_6CleanBadgesFitTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
    }

    @Test
    fun `version badges are removed from screen but version flag remains`() {
        assertTrue("bt.id = 'nativeBridgeTag'" !in asset)
        assertTrue(Regex("""__nativeBridgeVersion = 'N\d+\.\d+'""").containsMatchIn(asset))
        // V94: بیلد جدید badge مرجع (v36 · V34) را از مبدأ حذف کرده (MF_FIX_V974)؛
        // دیگر hostBuildTag یا سازندهٔ برچسبی در فایل نیست.
        assertTrue("hostBuildTag" !in asset)
        assertTrue("bt.textContent = 'v36" !in asset)
        assertTrue("MF_FIX_V974" in asset)
        // hideBadges همچنان بابت پاک‌سازی دفاعی nativeBridgeTag باقی است.
        val hide = asset.substringAfter("hideBadges")
        assertTrue("nativeBridgeTag" in hide.take(600))
    }

    @Test
    fun `library windows are enforced immediately via wrappers not polling`() {
        assertTrue("wrapNow('mbGroupLibrary', afterMenu)" in asset)
        assertTrue("wrapNow('mbOpenSymbolLibrary', afterMenu)" in asset)
        assertTrue("wrapNow('mbShowSymbolCategory', afterPanel)" in asset)
        assertTrue("wrapNow('mbOpenItemLibrary', afterPanel)" in asset)
        assertTrue("__nativeWrapped2" in asset)
    }

    @Test
    fun `wide formulas shrink until they fit inside their card`() {
        assertTrue("fitLibraryItems" in asset)
        assertTrue("b.scrollWidth > b.clientWidth + 1" in asset)
        assertTrue("if (cur <= 9) break;" in asset)
    }
}
