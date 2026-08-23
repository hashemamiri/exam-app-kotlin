package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V54.1 — مرحلهٔ اول تکمیل کتابخانهٔ نمودار Native:
 * ۱) ۲۰ نوع جدید در رندرگر و گالری با شناسه‌ها و کلیدهای X مرجع.
 * ۲) مسیر مشترک SVG (isGeometry/renderBody) انواع جدید را نمودار می‌شناسد.
 * ۳) فیلدهای ویرایشگر برچسب فارسی مرجع دارند و کلیدهای متنی/عددی تفکیک شده‌اند.
 * ۴) SVG امن: بدون script/href/foreignObject/style.
 */
class V54_1ChartLibraryStage1Test {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val chart by lazy { source("app/src/main/java/ir/exam/app/core/figure/ChartSvgRenderer.kt") }
    private val renderer by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureSvgRenderer.kt") }
    private val gallery by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureGallery.kt") }
    private val picker by lazy { source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt") }

    private val newTypes = listOf(
        "pie", "donut", "lchr", "area", "sarea", "hbar", "cmp", "hcmp",
        "stack", "st100", "scat", "bub", "hist", "pareto", "gauge",
        "radar", "combo", "step", "lolli", "funn"
    )

    @Test
    fun `all twenty stage one chart types exist in renderer and gallery`() {
        newTypes.forEach { t ->
            assertTrue("renderer missing: $t", "\"$t\"" in chart)
            assertTrue("gallery missing: $t", "FigureTemplate(\"$t\"" in gallery)
        }
        // نام‌های فارسی مرجع نمونه‌ای
        listOf("دایره‌ای", "دوناتی", "میله‌ای", "هیستوگرام", "پارتو", "عقربه‌ای", "راداری", "قیفی").forEach {
            assertTrue("missing farsi label: $it", it in gallery)
        }
    }

    @Test
    fun `shared svg path routes new chart types away from geometry`() {
        assertTrue("in ChartSvgRenderer.SUPPORTED -> ChartSvgRenderer.body(spec)" in renderer)
        assertTrue("!ChartSvgRenderer.supports(spec.type)" in renderer)
    }

    @Test
    fun `editor fields use reference farsi labels with text and numeric keys`() {
        assertTrue("paramFields" in picker)
        listOf("شیب m", "دامنه A", "مقدار عقربه", "اندازه حباب‌ها", "نام سری ۱", "نام ستون").forEach {
            assertTrue("missing field label: $it", it in picker)
        }
        assertTrue("TEXT_PARAM_KEYS" in picker)
        assertTrue("paramLabel(graphType, key)" in picker)
        // نام‌های سری (s1..s3) و محورها (xs/ys/zs) متنی می‌مانند.
        assertTrue("\"s1\", \"s2\", \"s3\"" in picker || "\"s3\")" in picker)
    }

    @Test
    fun `chart svg stays safe`() {
        assertFalse("<script" in chart)
        assertFalse("href=" in chart)
        assertFalse("<foreignObject" in chart)
        assertFalse("<style" in chart)
        assertFalse("android.webkit" in chart)
    }
}
