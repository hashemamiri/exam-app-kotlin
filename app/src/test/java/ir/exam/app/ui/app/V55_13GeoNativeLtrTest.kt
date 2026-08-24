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

    private val asset by lazy { source("app/src/main/assets/question_editor/question_editor.html") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val periodic by lazy { source("app/src/main/java/ir/exam/app/ui/figure/PeriodicEditorDialog.kt") }

    @Test
    fun `second click on geometry and graph tokens goes to the native editor`() {
        val boot = asset.substringAfter("exam-editor-native-boot").substringBefore("</script>")
        assertTrue("&& kind !== 'g' && kind !== ''" in boot.replace("\n", " ").replace("            ", " ")
                || "kind !== 'g'" in boot)
        // Kotlin: توکن g/خالی به FigurePickerDialog می‌رود و جایگزینی توکن دارد.
        assertTrue("\"g\", \"\" -> figureTarget = FigureTarget(" in builder)
        assertTrue("GRAPH_FIGURES.any { it.id == spec.type }" in builder)
        // خروجی ویرایش، توکن همان شکل را جایگزین می‌کند (نه درج دوباره).
        val figBlock = builder.substringAfter("figureTarget?.let").substringBefore("fun deliverFigure")
        assertTrue("applyEditedFigureJson" in figBlock)
        assertTrue("cancelEditFigure" in figBlock)
    }

    @Test
    fun `periodic touch grid is forced left-to-right`() {
        assertTrue("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)" in periodic)
        val grid = periodic.substringAfter("private fun PeriodicTouchGrid")
        assertTrue("CompositionLocalProvider" in grid)
    }
}
