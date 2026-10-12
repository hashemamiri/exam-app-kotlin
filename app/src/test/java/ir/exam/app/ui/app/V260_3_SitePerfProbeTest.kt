package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V260.3 — سایت: پنل سنجش سرعت با ?perf=1 (زمان صفحه، منابع، هر درخواست سرور) و تعویق پیش‌بارگذاری موتورها. */
class V260_3_SitePerfProbeTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `perf probe logs server calls and defers engine prefetch`() {
        val a = source("site/src/app.js")
        assertTrue("var PERF_ON = /[?&]perf=1/.test(location.search), perfRows = [];" in a)
        assertTrue("perfLog(path, performance.now() - _t0, res.status + ' ' + text.length + 'B');" in a)
        assertTrue("performance.getEntriesByType('navigation')" in a && "performance.getEntriesByType('resource')" in a)
        assertTrue("if (c.saveData || /(^|-)2g$|^3g$/.test(String(c.effectiveType || ''))) return;" in a)
        assertTrue("setTimeout(function () { idle(function () { loadEngines().then(function () { prewarmFormula();" in a)
    }
}
