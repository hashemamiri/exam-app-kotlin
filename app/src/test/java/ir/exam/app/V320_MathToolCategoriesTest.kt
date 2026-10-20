package ir.exam.app

import ir.exam.app.core.figure.AXIS_FIGURES
import ir.exam.app.core.figure.MATH_CATEGORIES
import ir.exam.app.core.figure.MATH_TOOL_IDS
import ir.exam.app.core.figure.PRIMARY_MATH_FIRST_ID
import ir.exam.app.core.figure.mathCategoryOf
import ir.exam.app.core.figure.mathTemplatesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V320.0 — دسته‌بندی ابزارهای ریاضی در پنجرهٔ انتخاب:
 * لمس آیکن «ابزار ریاضی» اول یک پنجرهٔ چهارتایی (دبستان / متوسطهٔ اول / متوسطهٔ دوم و کنکور /
 * انسانی و دانشگاه) باز می‌کند و بعد ابزارهای همان دسته.
 */
class V320_MathToolCategoriesTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun categoriesAreFourAndCoverEveryToolExactlyOnce() {
        assertEquals(4, MATH_CATEGORIES.size)
        assertEquals(listOf("elementary", "middle", "high", "university"), MATH_CATEGORIES.map { it.id })
        // هیچ ابزاری دو بار یا جاافتاده نباشد
        assertEquals(MATH_TOOL_IDS.size, MATH_TOOL_IDS.toSet().size)
        val galleryMath = AXIS_FIGURES.map { it.id }
            .let { ids -> ids.subList(ids.indexOf(PRIMARY_MATH_FIRST_ID), ids.size) }
        assertEquals(galleryMath.toSet(), MATH_TOOL_IDS.toSet())
        assertEquals(MATH_TOOL_IDS.size, galleryMath.size)
    }

    @Test
    fun everyCategoryHasTitleHintAndTools() {
        for (c in MATH_CATEGORIES) {
            assertTrue("title ${c.id}", c.title.isNotBlank())
            assertTrue("hint ${c.id}", c.hint.isNotBlank())
            assertTrue("emoji ${c.id}", c.emoji.isNotBlank())
            assertTrue("empty ${c.id}", c.count >= 5)
            assertEquals(c.count, mathTemplatesOf(c).size)
            assertTrue("same of ${c.id}", mathCategoryOf(c.ids.first())?.id == c.id)
        }
    }

    @Test
    fun pickerShowsCategoryStepBeforeTools() {
        val picker = File(root(), "app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").readText()
        assertTrue("category state", "var mathCategory by remember" in picker)
        assertTrue("category grid", "items(MATH_CATEGORIES, key = { it.id })" in picker)
        assertTrue("tools of category", "items(mathTemplatesOf(current), key = { it.id })" in picker)
        assertTrue("back button", "↩ دسته‌بندی‌ها" in picker)
        assertTrue("step title", "🧮 ابزار ریاضی — انتخاب دسته" in picker)
        assertTrue("category card", "private fun MathCategoryCell(" in picker)
    }

    @Test
    fun categoryNamesMatchTheRequestedGrouping() {
        val titles = MATH_CATEGORIES.map { it.title }
        assertTrue(titles[0].contains("دبستان"))
        assertTrue(titles[1].contains("متوسطهٔ اول"))
        assertTrue(titles[2].contains("متوسطهٔ دوم"))
        assertTrue(titles[3].contains("دانشگاه"))
    }
}
