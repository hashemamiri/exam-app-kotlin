package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.5 — اجرای «قطعی» اندازهٔ منوی دسته‌ها با style مستقیم روی عنصر
 * (گزارش دستگاه + عکس N55.4: منوی «اعداد و محاسبات» با وجود CSS تزریقی V55.4
 * همچنان نوار باریک یک‌ردیفه بود؛ در Chromium همان فایل درست بود یعنی WebView
 * دستگاه cascade تزریقی/min()/dvh را اعمال نکرده است).
 * راه‌حل: بلوک nativeMenuEnforce با polling 250ms — به محض بازشدن:
 * - منوی دسته‌ها (#mbVar با .mbv-cat): عرض ۹۴٪ (سقف 480px)، ارتفاع تا ۸۰٪،
 *   ردیف‌های ۴۸px، وسط‌چین؛ همه با el.style.setProperty(..., 'important').
 * - پنل کتابخانه (.mb-library-panel): عرض ۹۶٪ (سقف 720px)، ارتفاع تا ۹۲٪.
 * - منوهای کوچک variants (.mbv-i بدون .mbv-cat) دست‌نخورده؛ پس از بستن منو
 *   styleهای inline پاک می‌شوند (unsize) تا اندازهٔ مرجع برگردد.
 * - اگر ۵۰۰ms پس از اعمال هنوز کوچک بود: گزارش قرمز MENU_RECT/PANEL_RECT با
 *   ابعاد واقعی + نسخهٔ Chrome (ادامهٔ اصل «حدس ممنوع»).
 * تأیید اجرایی با Chromium واقعی (playwright): مسیر عادی، مسیر بدون CSSهای
 * تزریقی (فقط enforce)، پنل کتابخانه، و برگشت منوی variants — همه PASS.
 */
class V55_5MenuEnforceTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
    }

    @Test
    fun `menu enforcement runs only inside the app and uses inline important styles`() {
        assertTrue("nativeMenuEnforce" in asset)
        val block = asset.substringAfter("nativeMenuEnforce()").substringBefore("V55.3")
        assertTrue("if (!window.ExamEditorNative) return;" in block)
        assertTrue("setProperty(k, map[k], 'important')" in block)
        // V55.8 — به درخواست کاربر، منوهای حالت‌های کیپد (mbv-i) و پرانتز (mbv-q)
        // هم بزرگ می‌شوند؛ پس از بستن، unsize اندازهٔ مرجع را برمی‌گرداند.
        assertTrue("querySelector('.mbv-cat, .mbv-i, .mbv-q') && !pop.__nativeSized" in block)
        assertTrue("unsize(pop)" in block)
    }

    @Test
    fun `diagnostics report real rect when the menu stays small`() {
        assertTrue("MENU_RECT" in asset)
        assertTrue("PANEL_RECT" in asset)
        assertTrue("r.height >= 180 && r.width >= 220" in asset)
    }

    @Test
    fun `library panel is enforced too`() {
        val block = asset.substringAfter("nativeMenuEnforce()").substringBefore("V55.3")
        assertTrue("mb-library-panel" in block)
        assertTrue("Math.min(720" in block)
    }
}
