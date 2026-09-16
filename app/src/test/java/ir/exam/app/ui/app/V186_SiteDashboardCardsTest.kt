package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V186 — داشبورد دسکتاپ: کارت‌های آمار هم‌اندازه و کلیک‌پذیر؛ عملیات «آخرین آزمون‌ها» با متن. */
class V186_SiteDashboardCardsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `stat cards are buttons with targets and equal height`() {
        val a = source("site/src/app.js")
        assertTrue("function statCard(v, l, target)" in a && "class: 'card stat stat-link', type: 'button'" in a)
        assertTrue("statCard(fa(r[0].length), 'آزمون', {panel: 'exams'})" in a && "'موجودی کیف پول', {panel: 'wallet'}" in a)
        val css = source("site/src/site.css")
        assertTrue(".grid4 .stat,.grid3 .stat{min-height:112px" in css && "button.stat{font:inherit" in css)
    }

    @Test
    fun `desktop exam actions show text labels`() {
        val a = source("site/src/app.js")
        assertTrue("class: 'acts acts-text'" in a && "<i>✎</i><span>ویرایش</span>" in a && "<i>🗑</i><span>حذف</span>" in a)
        val css = source("site/src/site.css")
        assertTrue(".dk .acts-text .icon-btn i{display:none}" in css && ".dk .acts-text .icon-btn span{display:inline}" in css)
    }
}
