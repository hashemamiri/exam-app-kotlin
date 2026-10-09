package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V249 — رادیکال و پرانتزهای کشسان در پیش‌نمایش چاپ با zoom≠۱ (transform: scale روی pgsCanvas) درست اندازه می‌گیرند. */
class V249_SurdZoomScaleTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, path).isFile) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `math stretchers divide measured height by css scale`() {
        val m = source("app/src/main/assets/print/web/math_host.js")
        assertTrue("function mbScaleOf(el)" in m)
        assertTrue("const K = mbScaleOf(msqrt);" in m && "const H = msqrt.getBoundingClientRect().height / K;" in m)
        assertTrue("h = (pe.getBoundingClientRect().height || 0) / mbScaleOf(pe);" in m)
        assertTrue("canvas.style.transform = 'scale(' + z + ')';" in source("app/src/main/assets/print/web/pgs_engine.js"))
    }
}
