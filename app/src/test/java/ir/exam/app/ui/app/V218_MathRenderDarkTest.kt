package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V218 — توان‌های بلند مثل TeX، رادیکال در جعبهٔ سؤال دسکتاپ/دانش‌آموز سایت، و متن/فرمول سفید در حالت تیره. */
class V218_MathRenderDarkTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun superscriptUsesTexLikeShift() {
        val r = src("app/src/main/java/ir/exam/app/core/math/NativeMathSvgRenderer.kt")
        assertTrue("val supShift = upper?.let { max(size * .42f, (it.height - it.baseline) + size * .16f) } ?: 0f" in r)
        assertTrue("import kotlin.math.min" in r)
    }

    @Test
    fun builderFormulaColorFollowsTheme() {
        val q = src("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt")
        assertTrue("MaterialTheme.colorScheme.onSurface.toArgb()" in q)
        assertTrue("NativeMathSvgRenderer.render(tex = part.tex, fontSizePx = mathPx, color = mathColorHex)" in q)
    }

    @Test
    fun siteFitsSurdsAndKeepsRadicalCss() {
        val b = src("site/src/builder.js")
        val s = src("site/src/student.js")
        assertTrue("msurd|surd-svg|root-line|mrad" in b)
        assertTrue("msurd|surd-svg|root-line|mrad" in s)
        assertTrue("w.fitMathStretchers(root)" in b)
        assertTrue("fitMath(w, c)" in b)
        assertTrue("fitMath(w, box)" in b)
        assertTrue("fitMath: fitMath" in s)
        assertTrue("fitMath(txt)" in s)
    }

    @Test
    fun siteDesktopDarkPalette() {
        val css = src("site/src/site.css")
        assertTrue(":root.m-dark{--bg:#1F2530;--card:#2A3242;--ink:#E6EAF2" in css)
        assertTrue(!css.contains("background:#fff;") && !css.contains("background:#fff}"))
    }
}
