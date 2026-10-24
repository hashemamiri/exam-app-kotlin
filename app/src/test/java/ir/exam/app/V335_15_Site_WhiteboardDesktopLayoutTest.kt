package ir.exam.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V335.15 — چیدمان دسکتاپ تختهٔ سفید: ستون‌های عمودی راست و چپ به جای نوار افقی بالا. */
class V335_15_Site_WhiteboardDesktopLayoutTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun whiteboardUsesRightAndLeftSidebarsAroundCenterCanvas() {
        val student = File(root(), "site/src/student.js").readText()
        assertFalse("engine-bar wb-bar" in student)
        assertTrue("class: 'wb-side wb-right'" in student)
        assertTrue("class: 'wb-side wb-left'" in student)
        assertTrue("class: 'wb-tools-grid'" in student)
        assertTrue("class: 'wb-swatches'" in student)
        assertTrue("bg.appendChild(rightSide); bg.appendChild(area); bg.appendChild(leftSide);" in student)
    }

    @Test
    fun desktopWhiteboardCssUsesRowLayoutAndTouchMediaQueryForCompactStack() {
        val css = File(root(), "site/src/site.css").readText()
        assertTrue(".engine-bg.wb{display:flex;flex-direction:row;align-items:stretch;" in css)
        assertTrue(".wb-right{border-inline-end:1px solid var(--line)}" in css)
        assertTrue(".wb-left{border-inline-start:1px solid var(--line)}" in css)
        assertTrue(".wb-canvas{background:#FFFFFF;" in css)
        assertTrue("@media (max-width:860px) and (pointer:coarse){\n  .engine-bg.wb{flex-direction:column}" in css)
    }
}
