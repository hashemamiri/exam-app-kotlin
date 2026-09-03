package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V54.3 — مرحلهٔ پایانی کتابخانهٔ نمودار Native (۲۲ نوع) + رگرسیون کل
 * نقشهٔ V54: هر ۶۱ نوع TYPES مرجع اکنون رندر و قالب Native دارند.
 */
class V54_3ChartLibraryFinalTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val stage3 by lazy { source("app/src/main/java/ir/exam/app/core/figure/ChartSvgRendererStage3.kt") }
    private val stage1 by lazy { source("app/src/main/java/ir/exam/app/core/figure/ChartSvgRenderer.kt") }
    private val gallery by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureGallery.kt") }
    private val picker by lazy { source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt") }

    private val stage3Types = listOf(
        "plot", "flow", "gantt", "time", "dumb", "slope", "spark", "stream",
        "viol", "strip", "stem", "smat", "dend", "sank", "chrd", "netw",
        "map", "bmap", "surf", "calh", "rose", "word"
    )

    @Test
    fun `all twenty two final chart types exist in renderer and gallery`() {
        stage3Types.forEach { t ->
            assertTrue("stage3 renderer missing: $t", "\"$t\"" in stage3)
            assertTrue("gallery missing: $t", "FigureTemplate(\"$t\"" in gallery)
        }
        listOf("محور مختصات", "فلوچارت", "گانت", "تایملاین", "دمبل", "شیب", "اسپارک‌لاین",
            "جریانی", "ویولن", "نوار نقطه‌ای", "ساقه و برگ", "ماتریس پراکندگی", "دندروگرام",
            "سنکی", "کورد", "شبکه‌ای", "نقشه‌ای", "نقشه حبابی", "سطحی", "تقویم حرارتی",
            "گل رز / قطبی", "ابر واژه").forEach {
            assertTrue("missing farsi label: $it", it in gallery)
        }
    }

    @Test
    fun `stage three routes through the shared supported set`() {
        assertTrue("STAGE1 + ChartSvgRendererStage2.SUPPORTED + ChartSvgRendererStage3.SUPPORTED" in stage1)
        assertTrue("in ChartSvgRendererStage3.SUPPORTED -> ChartSvgRendererStage3.body(spec)" in stage1)
    }

    @Test
    fun `editor fields cover reference keys for stage three`() {
        listOf(
            "مراحل (با ویرگول)", "فعالیت‌ها", "رویدادها", "مقدار شروع / قبل",
            "عددها (با ویرگول)", "متغیر X", "برگ‌ها / نام‌ها",
            "جریان‌ها (مثل A-C:8,B-D:5)", "ماتریس سطری", "یال‌ها (مثل A-B,B-C)",
            "نام مناطق", "تعداد ردیف", "ارتفاع‌ها (سطری)",
            "مقدار روزها (از شنبه، سطری)", "وزن / فراوانی"
        ).forEach { assertTrue("missing field label: $it", it in picker) }
        assertTrue("\"nrows\"" in picker && "\"ncols\"" in picker)
    }

    @Test
    fun `stage three svg stays safe`() {
        assertFalse("<script" in stage3)
        assertFalse("href=" in stage3)
        assertFalse("<foreignObject" in stage3)
        assertFalse("<style" in stage3)
        assertFalse("android.webkit" in stage3)
        assertTrue("faFloat" in stage3)
    }
}
