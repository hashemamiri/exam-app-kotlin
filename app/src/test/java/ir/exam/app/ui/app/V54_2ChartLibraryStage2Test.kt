package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V54.2 — مرحلهٔ دوم کتابخانهٔ نمودار Native (۱۴ نوع):
 * box/ohlc/fall/ctrl/venn/tree/sun/waff/pict/heat/hmap/bull/pyra/mekko.
 */
class V54_2ChartLibraryStage2Test {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val stage2 by lazy { source("app/src/main/java/ir/exam/app/core/figure/ChartSvgRendererStage2.kt") }
    private val stage1 by lazy { source("app/src/main/java/ir/exam/app/core/figure/ChartSvgRenderer.kt") }
    private val gallery by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureGallery.kt") }
    private val picker by lazy { source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt") }

    private val newTypes = listOf(
        "box", "ohlc", "fall", "ctrl", "venn", "tree", "sun",
        "waff", "pict", "heat", "hmap", "bull", "pyra", "mekko"
    )

    @Test
    fun `all fourteen stage two chart types exist in renderer and gallery`() {
        newTypes.forEach { t ->
            assertTrue("stage2 renderer missing: $t", "\"$t\"" in stage2)
            assertTrue("gallery missing: $t", "FigureTemplate(\"$t\"" in gallery)
        }
        listOf("جعبه‌ای", "سهام", "آبشاری", "کنترلی", "ون", "نقشه درختی", "خورشیدی",
            "وافل", "پیکتوگرام", "کانتور", "حرارتی", "گلوله‌ای", "هرم جمعیت", "مکّو").forEach {
            assertTrue("missing farsi label: $it", it in gallery)
        }
    }

    @Test
    fun `stage two routes through the shared supported set`() {
        assertTrue("SUPPORTED: Set<String> = STAGE1 + ChartSvgRendererStage2.SUPPORTED" in stage1)
        assertTrue("in ChartSvgRendererStage2.SUPPORTED -> ChartSvgRendererStage2.body(spec)" in stage1)
    }

    @Test
    fun `editor fields cover reference keys for stage two`() {
        listOf(
            "چارک ۱", "میانه", "بیشینه", "کمینه", "مقدارها (منفی مجاز)",
            "میانگین (خالی=خودکار)", "تعداد مجموعه (۲ یا ۳)", "A∩B∩C",
            "حلقهٔ داخلی", "هر نماد برابر است با", "مقدارها سطری",
            "گروه‌های سنی", "دسته‌ها (پهنای ستون)"
        ).forEach { assertTrue("missing field label: $it", it in picker) }
        // کلیدهای چندمقداری مرحلهٔ دوم متنی ذخیره می‌شوند (قرارداد رشته‌ای مرجع).
        listOf("\"mins\"", "\"opens\"", "\"rows\"", "\"mean\"", "\"abc\"").forEach {
            assertTrue("missing text key: $it", it in picker)
        }
    }

    @Test
    fun `stage two svg stays safe and parses farsi digits`() {
        assertFalse("<script" in stage2)
        assertFalse("href=" in stage2)
        assertFalse("<foreignObject" in stage2)
        assertFalse("<style" in stage2)
        assertFalse("android.webkit" in stage2)
        // اعداد فارسی و ممیز فارسی مرجع (۰٫۴) پشتیبانی می‌شوند.
        assertTrue("faFloat" in stage2)
        assertTrue("'٫'" in stage2)
    }
}
