package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.13 — دو گزارش دستگاه (عکس‌های 14-24، پس از V55.11):
 * ۱) «با کلیک دوم کادر خاکستری می‌آید»: کلیک دوم توکن هندسه/نمودار (k='g' یا
 *    خالی) هنوز به refEdit → ویرایشگر مرجع (overlay خاکستری داخل WebView کوچک)
 *    می‌رفت. رفع: این انواع هم به __nativeFigEdit می‌روند؛ Kotlin آن‌ها را به
 *    FigurePickerDialog (ویرایشگر Native موجود V45.3) هدایت و خروجی را با
 *    editingWebToken جایگزین همان توکن می‌کند. refEdit فقط پشتیبان انواع
 *    ناشناخته ماند. تأیید Chromium: کلیک دوم هندسه و نمودار → onEditFigure('g')
 *    و صفر overlay مرجع.
 * ۲) «جدول تناوبی از راست به چپ است»: برنامه RTL است و Rowهای شبکهٔ لمسی
 *    PeriodicTouchGrid از راست چیده می‌شدند (گروه ۱ سمت راست). جدول تناوبی
 *    استاندارد همیشه LTR است (مرجع هم .ptb را direction:ltr می‌کند)؛
 *    CompositionLocalProvider(LayoutDirection.Ltr) دور شبکه اضافه شد.
 *    (رندر SVG دانش‌آموز/چاپ مختصات مطلق دارد و از قبل LTR بود.)
 */
class V55_13GeoNativeLtrTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val periodic by lazy { source("app/src/main/java/ir/exam/app/ui/figure/PeriodicEditorDialog.kt") }

    @Test
    fun `periodic touch grid is forced left-to-right`() {
        assertTrue("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)" in periodic)
        val grid = periodic.substringAfter("private fun PeriodicTouchGrid")
        assertTrue("CompositionLocalProvider" in grid)
    }
}
