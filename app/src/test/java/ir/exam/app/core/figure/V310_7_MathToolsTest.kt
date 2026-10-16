package ir.exam.app.core.figure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V310.7 — ابزار ریاضی: ارقام فارسی، چوب‌خط تا ۷۵، خط‌کش تا ۵۰، جدول ارزش مکانی طبق تصویر،
 * دسته‌های ده‌تایی سه‌بعدی تا یک میلیون، کسر شکلی با منوی کشویی، ساعت قابل جابه‌جایی.
 */
class V310_7_MathToolsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    private val web = "app/src/main/assets/print/web/graph_fig.js"
    private val renderer = "app/src/main/java/ir/exam/app/core/figure/PrimaryMathSvgRenderer.kt"

    @Test
    fun `persian digits convert to latin digits`() {
        assertEquals("15", faEnDigits("۱۵"))
        assertEquals("75", faEnDigits("٧٥"))
        assertEquals("2457", faEnDigits("۲۴۵۷"))
        assertEquals("ab12", faEnDigits("ab۱۲"))
    }

    @Test
    fun `web and native renderers accept persian digits and new limits`() {
        val js = File(root(), web).readText()
        assertTrue(js.contains("function faEn(v)"))
        assertTrue(js.contains("clamp(Math.floor(num(X.n, 7)), 0, 75)"))
        assertTrue(js.contains("clamp(Math.floor(num(X.len, 10)), 2, 50)"))
        assertTrue(js.contains("'میلیارد', '#d62828'") || js.contains("['میلیارد', '#d62828']"))
        assertTrue(js.contains("if (shape === 'square')"))
        assertTrue(js.contains("if (shape === 'ring')"))
        assertTrue(js.contains("function bindClock(box)"))

        val kt = File(root(), renderer).readText()
        assertTrue(kt.contains("coerceIn(0, 75)"))
        assertTrue(kt.contains("coerceIn(2, 50)"))
        assertTrue(kt.contains("val per = listOf(\"میلیارد\" to \"#d62828\""))
        assertTrue(kt.contains("\"ring\" -> {"))
        assertTrue(kt.contains("\"square\" -> {"))
    }

    @Test
    fun `native clock is draggable and fraction shape is a dropdown`() {
        val adj = File(root(), "app/src/main/java/ir/exam/app/ui/figure/AdjustableFigure.kt").readText()
        assertTrue(adj.contains("detectDragGestures"))
        assertTrue(adj.contains("fun AdjustableFigure("))
        val picker = File(root(), "app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").readText()
        assertTrue(picker.contains("FigureShapeDropdown("))
        assertTrue(picker.contains("AdjustableFigure("))
        assertTrue(picker.contains("faEnDigits(value)"))
        val js = File(root(), web).readText()
        assertTrue(js.contains("z-index:200000"))
        assertTrue(js.contains("gf-field gf-span"))
        assertTrue(js.contains("select data-k=\"X.shape\" class=\"gf-sel\""))
    }
}
