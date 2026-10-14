package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V286.4 — دسکتاپ سایت: راهنمای هاور (title) روی همهٔ دکمه‌های ریل؛ ریل شمارهٔ سؤال‌ها سؤال فعال را خودکار نمایان می‌کند. */
class V286_SiteRailHoverTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun railItemsHaveTitle() {
        val app = src("site/src/app.js")
        assertTrue("'aria-label': label, title: label," in app)
        assertTrue("'aria-label': 'افزودن', title: 'افزودن'," in app)
    }

    @Test
    fun questionRailKeepsScrollAndRevealsActive() {
        val b = src("site/src/builder.js")
        assertTrue("prevScroll = prevNums ? prevNums.scrollTop : 0;" in b)
        assertTrue("nums.scrollTop = prevScroll;" in b)
        assertTrue("function railRevealActive(nums) {" in b)
        assertTrue("if (top < 0 || bottom > nr.height) nums.scrollTop += top - (nr.height - ar.height) / 2;" in b)
    }
}
