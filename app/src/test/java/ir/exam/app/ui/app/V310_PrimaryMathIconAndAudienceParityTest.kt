package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V310 — دو درخواست همگام‌سازی اپ و سایت:
 *  1) «ابزار ریاضی» آیکن جدا دارد (نه داخل پنجرهٔ «محور»)؛ در اپ و سایت.
 *     V320 — پس از لمس آیکن، اول دسته (دبستان/متوسطه/دانشگاه) و بعد ابزارهای همان دسته.
 *  2) مخاطبان آزمون در سایت مثل اپ: فقط «همه / مدارس / کلاس‌ها».
 */
class V310_PrimaryMathIconAndAudienceParityTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, path).isFile) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `app gives the math tool its own icon next to axis`() {
        val icons = source("app/src/main/java/ir/exam/app/ui/math/QuestionToolIcons.kt")
        val tools = source("app/src/main/java/ir/exam/app/ui/builder/OptionInsertTools.kt")
        val web = source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt")
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val picker = source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt")
        assertTrue("val MathTools: ImageVector" in icons)
        assertTrue("MATH(\"ابزار ریاضی\")" in tools && "OptionInsertTool.MATH -> QuestionToolIcons.MathTools" in tools)
        assertTrue("onInsertMath: (insertOffset: Int) -> Unit" in web)
        assertTrue("NativeToolButton(QuestionToolIcons.MathTools, \"درج ابزار ریاضی (دبستان تا دانشگاه)\") { onInsertMath(insertAtOffset) }" in web)
        assertTrue("OptionInsertTool.MATH -> {" in builder && "onInsertMath = {" in builder)
        assertTrue("mathOnly = target.mathOnly" in builder && "val mathOnly: Boolean = false" in builder)
        // V320 — پنجرهٔ ابزار ریاضی دو مرحله‌ای شد: اول دسته، بعد ابزارها.
        assertTrue("mathOnly: Boolean = false" in picker && "🧮 ابزار ریاضی — انتخاب دسته" in picker)
        assertTrue("items(MATH_CATEGORIES, key = { it.id })" in picker && "↩ دسته‌بندی‌ها" in picker)
    }

    @Test
    fun `axis picker shows only base axes and math picker only the primary tools`() {
        val picker = source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt")
        assertTrue("math -> all.subList(split, all.size)" in picker)
        assertTrue("else -> all.subList(0, split)" in picker)
        assertFalse("v304-head" in picker)
    }

    @Test
    fun `site has the math tool button and insert mapping like the app`() {
        val b = source("site/src/builder.js")
        val g = source("app/src/main/assets/print/web/graph_fig.js")
        // V320 — عنوان دکمه به «ابزار ریاضی (دبستان تا دانشگاه)» تغییر کرد (پنجره دو مرحله‌ای شد)
        assertTrue("class: 'tool-btn is-math', title: 'ابزار ریاضی (دبستان تا دانشگاه)'" in b && "insertFigure('math', ta, q)" in b)
        assertTrue("math: w.GraphFig" in b && "kind === 'math' ? 'math' : kind === 'axis' ? 'axis' : sciDom" in b)
        assertTrue("function fillShapes(mode, forceCat)" in g && "function isMathType(t)" in g) // V320 — امضای تابع با مرحلهٔ دسته‌بندی گسترش یافت
        assertTrue("var math = mode === 'math' || !!(spec && isMathType(spec.t));" in g)
        assertTrue("fillShapes(math ? 'math' : axis ? 'axis' : 'graph')" in g)
        assertTrue(".tool-btn.is-math{" in source("site/src/site.css"))
    }

    @Test
    fun `exam audience choices on site match the app`() {
        val app = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val site = source("site/src/builder.js")
        assertTrue("listOf(\"all\" to \"همه\", \"schools\" to \"مدارس\", \"classes\" to \"کلاس‌ها\")" in app)
        assertTrue("[['all', 'همه'], ['schools', 'مدارس'], ['classes', 'کلاس‌ها']]" in site)
        assertFalse("همهٔ دانش‌آموزان من" in site)
        assertFalse("کلاس‌های خاص" in site)
        assertFalse("دانش‌آموزان خاص" in site)
    }
}
