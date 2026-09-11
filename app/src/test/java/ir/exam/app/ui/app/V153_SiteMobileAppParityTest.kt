package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V153 — مقایسهٔ کامل اپ با سایت گوشی: افزودن سریع ضربدری، دستهٔ کارت‌های مدیریت، منوی شعاعی دایره‌ای وسط صفحه، عنوان سازنده. */
class V153_SiteMobileAppParityTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `quick add mirrors Design69QuickAddOverlay cross layout`() {
        val m = source("site/src/mobile.js")
        assertTrue("class: 'm-qa-stage'" in m && "class: 'm-qa-x'" in m && "class: 'm-qa-item'" in m)
        val css = source("site/src/site.css")
        assertTrue(".m-qa-stage{" in css && "height:466px" in css && "width:88px;height:88px" in css && "border-radius:29px" in css)
        assertTrue("rotate(135deg)" in css && "clamp(94px, 29vw, 124px)" in css && "108px" in css)
        assertTrue(".m-quick-item{" !in css)
    }

    @Test
    fun `management cards are a swipeable deck like TeacherManagementCardsScreen`() {
        val m = source("site/src/mobile.js")
        assertTrue("function cardsDeck(c, key, cards)" in m && "Math.abs(dx) > 52" in m && "cardsDeck(c, 'teacher', cards)" in m && "cardsDeck(c, 'manager', cards)" in m)
        assertTrue("ui.cycle = true" in m)
        for (g in listOf("#E0587F,#7D6CF4", "#4D5B74,#273247", "#7D6CF4,#E0587F", "#0EA5E9,#6366F1", "#2878DB,#24B8C8", "#25BFA4,#45D7BD")) assertTrue(g, "linear-gradient(135deg,$g)" in m)
        val css = source("site/src/site.css")
        assertTrue(".m-deck-card{" in css && "width:90%;height:190px" in css && "rotate(5deg)" in css && "rotate(-6deg)" in css && ".m-deck-dots i.on{width:23px" in css)
        assertTrue(".m-cards{" !in css)
    }

    @Test
    fun `builder radial menu is a full circle centred like BuilderRadialMenuOverlay`() {
        val m = source("site/src/mobile.js")
        assertTrue("Math.max(104, Math.min(138, window.innerWidth * 0.31))" in m && "(-90 + i * 45) * Math.PI / 180" in m && "class: 'm-radial-x'" in m)
        assertTrue("builder: 'ساخت آزمون'" in m && "ttl = 'ویرایش آزمون'" in m)
        val css = source("site/src/site.css")
        assertTrue(".m-radial{position:fixed;left:50%;top:50%" in css && "border:2px dashed" in css && "#E5484D,#B91C35" in css && ".m-bfab.radial-open .m-fab.add{visibility:hidden}" in css)
    }
}
