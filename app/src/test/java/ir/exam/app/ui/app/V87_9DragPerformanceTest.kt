package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V87.9 — جابه‌جاییِ اشیا در پیش‌نمایش لگ داشت و فقط افقی حرکت می‌کرد.
 */
class V87_9DragPerformanceTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `the parent is measured once at the start of a drag`() {
        assertTrue("function measureParent(el)" in asset)
        assertTrue("drag.cache = measureParent(fig);" in asset)
        assertTrue("clampToParent(drag.fig, x, y, w, h, drag.cache)" in asset)
    }

    @Test
    fun `pointermove no longer reads layout synchronously`() {
        // خواندن و نوشتنِ درهم در هر فریم = بازچینشِ اجباری = همان لگ
        val from = asset.indexOf("area.addEventListener('pointermove'")
        val to = asset.indexOf("function applyFigGeometry")
        assertTrue(from in 1 until to)
        val body = asset.substring(from, to)
        listOf("getBoundingClientRect", "getComputedStyle", "offsetTop").forEach {
            assertEquals("$it هنوز در pointermove خوانده می‌شود", 0, Regex(it).findAll(body).count())
        }
    }

    @Test
    fun `writes are batched into one animation frame`() {
        assertTrue("drag.raf = (window.requestAnimationFrame" in asset)
        assertTrue("function applyFigGeometry(d, c)" in asset)
    }

    @Test
    fun `the parent only grows, never rewritten on every frame`() {
        assertTrue("if (need > old) {" in asset)
        assertTrue("if (need > old || !parent.style.minHeight)" !in asset)
    }

    @Test
    fun `locking the scroll no longer flattens the container`() {
        // overflow:hidden ارتفاعِ اسکرول را صفر می‌کرد، پس شیء نمی‌توانست
        // پایین برود و حرکت فقط افقی به نظر می‌رسید
        assertTrue("sc.style.overflow = 'hidden'" !in asset)
        assertTrue("fig.style.touchAction = 'none';" in asset)
        assertTrue("sc.style.overscrollBehavior = 'contain';" in asset)
    }

    @Test
    fun `both locks are released`() {
        assertTrue("removeProperty('overscroll-behavior')" in asset)
        assertTrue("fg.style.removeProperty('touch-action')" in asset)
    }

    @Test
    fun `the vertical axis is still computed from the pointer`() {
        assertTrue("y = drag.y + (e.clientY - drag.sy);" in asset)
        assertTrue("y = Math.max(0, y);" in asset)
    }
}
