package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V320.0 — هم‌ترازی سایت با اپ برای دسته‌بندی ابزارهای ریاضی (چهار دسته، سپس ابزارها). */
class V320_Site_MathToolCategoriesTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun webPickerHasCategoryStepAndBackButton() {
        val g = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()
        assertTrue("catalog", "var MATH_CATS = [" in g)
        assertTrue("four categories", listOf("id: 'elementary'", "id: 'middle'", "id: 'high'", "id: 'university'").all { it in g })
        assertTrue("category cards", "card.className = 'gf-cat'" in g)
        assertTrue("back button", "↩ دسته‌بندی‌ها" in g)
        assertTrue("reset on open", "mathCat = presel ? presel.id : null;" in g)
        assertTrue("wide mode", "gf-cats-mode" in g)
        assertTrue("tools of category", "mathCatById(mathCat)" in g)
    }

    @Test
    fun webCategoryIdsMatchTheAppCatalog() {
        val g = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()
        val kt = File(root(), "app/src/main/java/ir/exam/app/core/figure/MathToolCatalog.kt").readText()
        val catLine = g.substringAfter("var MATH_CATS = [").substringBefore("\n  ];")
        for (id in Regex("\"(ax[a-z0-9]+)\"").findAll(kt).map { it.groupValues[1] }.toSet()) {
            assertTrue("category id missing in web: $id", "'$id'" in catLine)
        }
    }

    @Test
    fun siteStylesDefineCategoryCards() {
        val css = File(root(), "app/src/main/assets/print/web/tools_styles.css").readText()
        assertTrue(".gf-cat{" in css && ".gf-cats .gf-cats" !in css)
        assertTrue(".gf-shapes .gf-cats{grid-column:1/-1" in css)
        assertTrue(".gf-back{" in css)
        assertTrue(".gf-3.gf-cats-mode .gf-types{" in css)
    }

    @Test
    fun siteToolButtonIsRenamedForAllGrades() {
        val b = File(root(), "site/src/builder.js").readText()
        assertTrue("title: 'ابزار ریاضی (دبستان تا دانشگاه)'" in b)
        assertTrue("insertFigure('math', ta, q)" in b)
    }
}
