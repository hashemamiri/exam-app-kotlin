package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.8 — چهار گزارش دستگاه پس از V55.7:
 * ۱) «خود کادر اسکرول شود» (انتخاب صریح کاربر: ارتفاع ثابت + اسکرول داخل کادر):
 *    سقف ثابت 260px با overflow-y:auto جایگزین رشد نامحدود V55.7 شد.
 * ۲) «درج‌شده‌ها کوچک‌تر شوند تا جای متن باز بماند» (انتخاب: ~۶۰٪):
 *    پیش‌نمایش‌ها فقط داخل برنامه فشرده می‌شوند — qmf-fig با zoom:.6 و
 *    qmf-atom (فرمول‌ها) با zoom:.75؛ مقدار واقعی توکن/TeX دست نمی‌خورد.
 * ۳) «برای یک لحظه کل question_editor.html رندر و بعد غیب می‌شود»:
 *    بلوک exam-editor-native-boot در ابتدای head (فقط حالت nativeTools) بدنه
 *    را تا اجرای بلوک Native انتهایی مخفی می‌کند (nativeBootHide)؛ پشتیبان
 *    ۶ ثانیه‌ای برای برداشتن پرده اگر بلوک انتهایی نشکفت.
 * ۴) «پنجره‌های دکمه‌های کیپد هنوز باریک‌اند»: enforce منو به mbVarShow/
 *    mbParPicker/mbLogMenu/mbIntegralMenu/mbPercentMenu/mbTrigMenu هم وصل شد
 *    و آیتم‌های mbv-i (ارتفاع 52px) و دکمه‌های mbv-q/o/c (46px) بزرگ می‌شوند.
 * تأیید Chromium: پردهٔ boot هنگام load دیده شد و بعد برداشته شد؛ سقف 260 و
 * اسکرول داخلی؛ zoom .6/.75 اعمال شد؛ منوی log عرض 387 و آیتم 52px؛ پرانتزها
 * 46x46 — همه در اجرای واقعی.
 */
class V55_8FixedBoxShrinkTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val formulaAsset by lazy {
        File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
    }

    @Test
    fun `keypad variant and paren menus are enlarged too`() {
        assertTrue("wrapNow('mbVarShow', afterMenu)" in formulaAsset)
        assertTrue("wrapNow('mbParPicker', afterMenu)" in formulaAsset)
        assertTrue("wrapNow('mbTrigMenu', afterMenu)" in formulaAsset)
        assertTrue("'.mbv-cat, .mbv-i, .mbv-q'" in formulaAsset)
        assertTrue(".mbv-q, .mbv-o, .mbv-c" in formulaAsset)
    }
}
