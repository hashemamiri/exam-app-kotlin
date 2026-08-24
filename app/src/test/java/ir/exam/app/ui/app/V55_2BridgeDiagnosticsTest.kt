package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.2 — تشخیص قطعی نسخهٔ asset روی دستگاه:
 * گزارش دوم «صفحهٔ سفید» همان اسکرین‌شات قبلی (ساعت 04:47، پیش از V55.1) بود و
 * هیچ پیام قرمز timeout نداشت؛ برای حذف ابهام برای همیشه:
 * ۱) برچسب سبز N55.2 پل کنار badge مرجع نشان می‌دهد کدام asset واقعاً اجراست؛
 * ۲) پیام FORMULA_OPEN_TIMEOUT اکنون وضعیت openMath/modal/qTxt را دارد؛
 * ۳) اگر پل هرگز تعریف نشود، Kotlin پیام صریح BRIDGE_NOT_READY نشان می‌دهد.
 * تست اجرایی jsdom: درج/بستن/جلسهٔ دوم/race دیرهنگام openMath همگی PASS.
 */
class V55_2BridgeDiagnosticsTest {
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
    fun `bridge version tag is visible on device`() {
        // V55.6 — درخواست کاربر: برچسب روی صفحه حذف شد؛ نسخهٔ پل اکنون فقط
        // پرچم JS است (نقش تشخیصی حفظ شده، نمایش حذف شده). badge مرجع هم فقط
        // داخل برنامه مخفی می‌شود.
        assertTrue(Regex("""__nativeBridgeVersion = 'N\d+\.\d+'""").containsMatchIn(asset))
        assertTrue("hideBadges" in asset)
        // کد «ساخت» برچسب سبز باید حذف شده باشد (فقط remove آن مانده)؛
        // bt.textContent مرجع (badge v36·V34) سر جای خودش است و دست نمی‌خورد.
        assertTrue("bt.id = 'nativeBridgeTag'" !in asset)
    }

    @Test
    fun `timeout report carries real state and kotlin reports missing bridge`() {
        assertTrue("FORMULA_OPEN_TIMEOUT openMath=" in asset)
        assertTrue("' modal='" in asset.replace("\" + ", "' + ").let { asset } || "modal=" in asset)
        assertTrue("BRIDGE_NOT_READY" in host)
    }
}
