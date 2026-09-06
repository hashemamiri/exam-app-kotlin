package ir.exam.app.ui.app

import ir.exam.app.core.math.MathNode
import ir.exam.app.core.math.NativeMathParser
import ir.exam.app.core.math.NativeMathSvgRenderer
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های جامع نسخه V97 — رفع ۱۱ مورد چاپی، فرمول‌ساز، متون فارسی و عملکرد استودیو تصویر:
 * ۱) هماهنگی عرض جدول سؤالات و سربرگ در exam_print.html (بدون بیرون‌زدگی).
 * ۲) تصحیح خطای TypeError در pointerup هنگام درگ شکل‌های چاپی (بررسی ایمن drag.raf).
 * ۳) رفع بریدگی اعلان‌ها (Notification) در ویرایشگر فرمول.
 * ۴) منوی ۴ ستونی پرانتزها و براکت‌ها (تفکیک ستون نام از علائم).
 * ۵) نمایش وسط‌چین و کامل راهنمای دست‌نویس فرمول با پس‌زمینهٔ مات.
 * ۶) ردیابی بازه و پاکسازی وضعیت فرمول قبلی هنگام درج فرمول جدید.
 * ۷) تجزیه و نمایش صحیح کاراکترهای فارسی در فرمول‌ها با فونت استاندارد و bidi isolate.
 */
class V97_PrintAndFormulaFixesTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val printHtml by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    private val formulaHtml by lazy {
        File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
    }

    @Test
    fun `print table width aligns with 100 percent and zero margin`() {
        assertTrue(".questions-print-table { width:100%; margin:0; border-collapse:collapse; table-layout:fixed; direction:rtl; box-sizing:border-box; }" in printHtml)
        assertTrue(".exam-header, .scores-table, .exam-header2" in printHtml)
        assertTrue("width: 100%;\n    margin-right: 0;\n    margin-left: 0;\n    box-sizing: border-box;" in printHtml)
    }

    @Test
    fun `print html drag raf is safe and canceled on endDrag`() {
        assertTrue("if (drag) drag.raf = 0;" in printHtml)
        assertTrue("if (drag.raf) {\n      try { (window.cancelAnimationFrame || clearTimeout)(drag.raf); } catch(_) {}\n      drag.raf = 0;\n    }" in printHtml)
    }

    @Test
    fun `formula html notification styling allows multiline and proper zindex`() {
        assertTrue("z-index: 35000 !important;" in formulaHtml || "z-index:35000!important" in formulaHtml)
        assertTrue("word-break: break-word" in formulaHtml || "word-break:break-word" in formulaHtml)
        assertTrue("max-width: 90vw;" in formulaHtml || "max-width:90vw" in formulaHtml)
    }

    @Test
    fun `formula html parenthesis menu has 4 columns including name column`() {
        assertTrue("mbv-par4" in formulaHtml)
        assertTrue("mbv-par-col-name" in formulaHtml)
        assertTrue("<div class=\"mbv-par-col mbv-par-col-name\"><div class=\"mbv-par-col-t\">نام</div>" in formulaHtml)
    }

    @Test
    fun `formula html help modal is centered with backdrop`() {
        assertTrue("#helpBackdrop{position:fixed;top:0;left:0;right:0;bottom:0;width:100%;height:100%;z-index:30015;background:rgba(0,0,0,.55)}" in formulaHtml)
        assertTrue("#helpCard{position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);z-index:30020;" in formulaHtml)
    }

    @Test
    fun `formula html tracks selection and cleans up previous state`() {
        assertTrue("window.MF_SEL_START =" in formulaHtml)
        assertTrue("window.MF_SEL_END =" in formulaHtml)
        assertTrue("MB.root = [];" in formulaHtml)
    }

    @Test
    fun `native math ast parses persian characters as composite rtl atom`() {
        val root = NativeMathParser.parse("آزمون ریاضی")
        assertTrue(root is MathNode.Symbol)
        val symbol = root as MathNode.Symbol
        assertEquals("آزمون ریاضی", symbol.value)

        val mixed = NativeMathParser.parse("x + آزمون") as MathNode.Sequence
        val persianSymbol = mixed.children.last() as MathNode.Symbol
        assertEquals("آزمون", persianSymbol.value)
    }

    @Test
    fun `native math svg renderer renders persian with isolate bidi and rtl direction`() {
        val doc = NativeMathSvgRenderer.render("تست")
        val svg = doc.xml
        assertTrue("direction=\"rtl\" unicode-bidi=\"isolate\"" in svg)
        assertTrue("font-family=\"Tahoma, Arial, sans-serif\"" in svg)
        assertTrue("تست" in svg)
    }
}
