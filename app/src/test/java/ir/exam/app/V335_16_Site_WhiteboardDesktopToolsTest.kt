package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V335.16 — ابزارهای ضروری تختهٔ وایت‌برد دسکتاپ روی شاخهٔ آزمایشی (بدون اجرای CI اپ اندروید). */
class V335_16_Site_WhiteboardDesktopToolsTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun desktopWhiteboardIncludesEssentialShapesHighlighterRulerRedoAndLayering() {
        val student = File(root(), "site/src/student.js").readText()
        // لایهٔ مجزای جوهر برای پاک‌کن بدون تخریب محورها و تغییر زمینه بدون پاک شدن رسم‌ها
        assertTrue("var inkCanvas = document.createElement('canvas');" in student)
        assertTrue("inkCtx.globalCompositeOperation = 'destination-out';" in student)
        // قلم‌ها و اشکال ضروری جدید
        assertTrue("tb('highlighter', '🖍', 'ماژیک هایلایت')" in student)
        assertTrue("tb('dblarrow', '↔', 'پیکان دوسر', true)" in student)
        assertTrue("tb('ellipse', '⬭', 'بیضی', true)" in student)
        assertTrue("tb('triangle', '△', 'مثلث', true)" in student)
        assertTrue("tb('rtriangle', '◿', 'قائم‌الزاویه', true)" in student)
        assertTrue("tb('diamond', '◇', 'لوزی', true)" in student)
        assertTrue("tb('axes', '✚', 'محور سریع', true)" in student)
        // سبک خط، پرکردن ملایم، زمینه‌های جدید، خط‌کش، انجام مجدد، درج تصویر، بزرگ‌نمایی، کپی و ذخیره
        assertTrue("['dashed', '- - خط‌چین']" in student)
        assertTrue("['dotted', '··· نقطه‌چین']" in student)
        assertTrue("'aria-label': 'توپر ملایم'" in student)
        assertTrue("['dots', 'نقطه‌ای (Dot Grid)']" in student)
        assertTrue("['mm', 'کاغذ میلی‌متری']" in student)
        assertTrue("['iso', 'نقطه‌ای سه‌بعدی (ایزومتریک)']" in student)
        assertTrue("'aria-label': 'انجام مجدد'" in student)
        assertTrue("'aria-label': 'خط‌کش کمکی'" in student)
        assertTrue("'aria-label': 'درج تصویر'" in student)
        assertTrue("text: '⧉ کپی صفحه'" in student)
        assertTrue("text: '⬇ ذخیرهٔ عکس'" in student)
    }

    @Test
    fun desktopWhiteboardCssSupportsStageRulerHudSegmentedStrokeAndZoom() {
        val css = File(root(), "site/src/site.css").readText()
        assertTrue(".wb-stage{" in css)
        assertTrue(".wb-ruler{" in css)
        assertTrue(".wb-hud{" in css)
        assertTrue(".wb-seg{" in css)
        assertTrue(".wb-zoom-row{" in css)
        assertTrue(".wb-dk-only{display:none!important}" in css)
    }
}
