package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V152 — سازندهٔ آزمون در گوشی به سبک اپ (FAB ذخیره/+، منوی شعاعی نوع سؤال با رنگ‌های پاستلی اپ) + CI اپ فقط دستی. */
class V152_SiteMobileBuilderTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `mobile builder mirrors app fabs and radial menu`() {
        val m = source("site/src/mobile.js")
        assertTrue("essay: '#FFD1DC', multiple: '#AEC6CF', truefalse: '#B4EEB4', fill: '#FDFD96', numeric: '#C3B1E1', matching: '#FFDAB9', import: '#98FF98', bank: '#E6E6FA'" in m)
        for (t in listOf("['essay', 'تشریحی', '✎']", "['multiple', 'چندگزینه‌ای', '◉']", "['truefalse', 'صحیح/غلط', '✓']", "['fill', 'جای خالی', '＿']", "['numeric', 'عددی', '۱۲']", "['matching', 'جورکردنی', '↔']", "['import', 'وارد کردن', '⇩']", "['bank', 'بانک سؤال', '▤']")) assertTrue(t, t in m)
        assertTrue("function builderFabs(c)" in m && "'aria-label': 'ذخیره آزمون'" in m && "text: 'مشخصات آزمون'" in m)
        val css = source("site/src/site.css")
        assertTrue(".m-bfab .m-fab.save{background:#27A86B" in css && ".m-builder .b-add{display:none}" in css)
    }

    @Test
    fun `android CI runs only manually for now`() {
        val ci = source(".github/workflows/android.yml")
        assertTrue("  # push:\n  #   branches: [main]" in ci && "  workflow_dispatch:" in ci)
    }
}
