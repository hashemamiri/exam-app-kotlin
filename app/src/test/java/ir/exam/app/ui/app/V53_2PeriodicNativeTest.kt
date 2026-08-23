package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V53.2 — جدول تناوبی Native:
 * ۱) دادهٔ کامل ۱۱۸ عنصر با نام فارسی و ۱۱ دستهٔ رنگی مرجع.
 * ۲) رندر SVG امن با قرارداد X مرجع (Z/hid/hidZ/hideCols/hideRows/hideF).
 * ۳) ویرایشگر کاملاً Native با ۴ preset و دو حالت لمس مرجع.
 * ۴) اتصال آیکن تناوبی به ویرایشگر Native به‌جای ابزار WebView مرجع.
 */
class V53_2PeriodicNativeTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val elements by lazy { source("app/src/main/java/ir/exam/app/core/figure/PeriodicElements.kt") }
    private val renderer by lazy { source("app/src/main/java/ir/exam/app/core/figure/PeriodicSvgRenderer.kt") }
    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/figure/PeriodicEditorDialog.kt") }
    private val figureRenderer by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureSvgRenderer.kt") }
    private val figureSpec by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureSpec.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val webSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }

    @Test
    fun `all 118 elements with farsi names and reference categories exist`() {
        assertEquals(118, Regex("""E\(\d+, """).findAll(elements).count())
        // نمونه‌های نقطه‌ای از ابتدای/میانه/انتهای داده مرجع
        assertTrue("E(1, \"H\", \"هیدروژن\", 1, 1, \"nm\")" in elements)
        assertTrue("E(57, \"La\", \"لانتان\", 3, 8, \"lan\")" in elements)
        assertTrue("E(118, \"Og\", \"اوگانسون\", 18, 7, \"ng\")" in elements)
        // ۱۱ دستهٔ رنگی مرجع
        listOf("alk", "ae", "tm", "ptm", "met", "nm", "hal", "ng", "lan", "act", "un").forEach {
            assertTrue("missing category: $it", "\"$it\" to \"#" in elements)
        }
        assertTrue("قلیایی" in elements && "اکتینید" in elements)
    }

    @Test
    fun `renderer honors reference X contract and stays svg safe`() {
        listOf("\"Z\", \"1\"", "xIntList(\"hid\")", "xIntList(\"hidZ\")",
            "\"hideF\", \"0\"", "visibleGroups", "visiblePeriods").forEach {
            assertTrue("missing contract: $it", it in renderer)
        }
        // ستارهٔ بلوک f مثل مرجع
        assertTrue("\"*\"" in renderer && "\"**\"" in renderer)
        // امنیت SVG: بدون tag/attr خطرناک
        assertFalse("<script" in renderer)
        assertFalse("href=" in renderer)
        assertFalse("<foreignObject" in renderer)
        assertFalse("<style" in renderer)
        // ارقام فارسی مثل مرجع
        assertTrue("faNum" in renderer)
    }

    @Test
    fun `periodic tokens render through the shared svg path`() {
        assertTrue("if (spec.kind == \"p\") return PeriodicSvgRenderer.render(spec)" in figureRenderer)
        // پلاک موقت فقط برای a/s باقی مانده است.
        assertTrue("setOf(\"a\", \"s\")" in figureRenderer)
        assertTrue("buildPeriodic" in figureSpec)
    }

    @Test
    fun `native editor has reference presets and touch modes`() {
        listOf("\"full\"", "\"main\"", "\"noF\"", "\"noZ\"").forEach {
            assertTrue("missing preset: $it", it in editor)
        }
        listOf("کامل", "گروه اصلی", "بدون f", "بدون عدد اتمی").forEach {
            assertTrue("missing preset label: $it", it in editor)
        }
        assertTrue("حذف عنصر" in editor && "حذف عدد اتمی" in editor)
        assertTrue("نمایش عدد اتمی" in editor && "لانتانید و اکتینید" in editor)
        assertTrue("بازگردانی همه" in editor)
        // preset «گروه اصلی» مرجع: حذف گروه‌های ۳..۱۲ و مخفی‌کردن بلوک f.
        assertTrue("(3..12).toSet()" in editor)
        // ویرایشگر WebView ندارد.
        assertFalse("android.webkit" in editor)
        assertFalse("WebView" in editor)
    }

    @Test
    fun `periodic icon opens the native editor instead of the webview tool`() {
        assertTrue("NativeToolButton(QuestionToolIcons.Periodic, \"درج جدول تناوبی\", onInsertPeriodic)" in webSection)
        assertFalse("openTool(\"periodic\")" in webSection)
        assertTrue("PeriodicEditorDialog(" in builder)
        assertTrue("onInsertPeriodic = { periodicTarget = TableTarget() }" in builder)
        // V53.3 — درج/ویرایش هر چهار ابزار از مسیر متمرکز deliverFigure می‌گذرد؛
        // همان درج در محل مکان‌نمای WebView با fallback به ViewModel.
        val block = builder.substringAfter("periodicTarget?.let")
        assertTrue("deliverFigure(spec, target.occurrenceIndex)" in block)
        val deliver = builder.substringAfter("fun deliverFigure")
        assertTrue("insertFigureJson(spec.toJson())" in deliver)
        assertTrue("viewModel.insertFigure(question.id, spec)" in deliver)
    }
}
