package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V257.2 — سازندهٔ دسکتاپ: فاصلهٔ پنل‌ها نصف شد (۱۲px) و به کادر سؤال رسید؛ کشیدن در ریل با رویدادهای window و مختصات چیدمان (بدون لرزش/لگ هنگام خروج نشانگر از ریل). */
class V257_2_SiteRailDragGapTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `panel gaps halved to 12px`() {
        val css = source("site/src/site.css")
        assertTrue(".dk .builder{position:fixed;top:22px;bottom:22px;left:110px;right:104px;" in css)
        assertTrue(".dk .builder .b-body.has-settings{column-gap:12px}" in css)
        assertFalse("left:122px;right:116px" in css)
        assertTrue(".dk .b-settings.card>h3{display:flex;justify-content:center;text-align:center;width:100%}" in css) // V257.3
        assertTrue(".dk .b-rail-nums{position:relative}" in css && "body.rail-dragging{user-select:none;cursor:grabbing}" in css)
    }

    @Test
    fun `rail drag listens on window and uses layout offsets`() {
        val js = source("site/src/builder.js")
        assertTrue("window.addEventListener('pointermove', onMove, true); window.addEventListener('pointerup', finish, true); window.addEventListener('pointercancel', finish, true);" in js)
        assertTrue("window.addEventListener('blur', finish); document.addEventListener('keydown', onKey, true);" in js)
        assertTrue("var py = last.clientY - nr.top - nums.clientTop + nums.scrollTop;" in js)
        assertTrue("var mid = list[j].offsetTop + list[j].offsetHeight / 2;" in js)
        assertTrue("before[r.dataset.k] = r.offsetTop;" in js)
        assertTrue("ghost.style.transform = 'translate(' + Math.round(e.clientX - offX) + 'px,' + Math.round(e.clientY - offY) + 'px)';" in js)
        assertFalse("grip.addEventListener('pointermove', onMove)" in js)
        assertTrue("if (e.key === 'Escape') finish(true);" in js)
    }
}
