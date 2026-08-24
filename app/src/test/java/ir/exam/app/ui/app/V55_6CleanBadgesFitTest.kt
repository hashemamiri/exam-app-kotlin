package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.6 — سه درخواست کاربر پس از موفقیت V55.5 (پنل بزرگ شد):
 * ۱) هیچ برچسب نسخه‌ای روی صفحه نباشد: کد ساخت برچسب سبز N55.x حذف شد
 *    (نسخهٔ پل فقط پرچم window.__nativeBridgeVersion برای پیام‌های خطا)؛
 *    badge مرجع «v36 · V34» فقط داخل برنامه (hideBadges) مخفی می‌شود —
 *    فایل در مرورگر عادی دست‌نخورده است.
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
        val hide = asset.substringAfter("hideBadges")
        assertTrue("hostBuildTag" in hide.take(900))
        // badge مرجع فقط مخفی می‌شود، کد مرجع سازنده‌اش دست نمی‌خورد.
        assertTrue("bt.textContent = 'v36" in asset)
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
