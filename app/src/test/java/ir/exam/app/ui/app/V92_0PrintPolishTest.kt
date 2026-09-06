package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V92 — شش گزارشِ کاربر از پنجرهٔ آزمون‌سازِ چاپی:
 *  ۱) ویرایشگرِ فرمول پس از دابل‌کلیک خالی بود (بازهٔ انتخاب گم می‌شد)
 *  ۲) اشیاءِ غیرفرمولی ۳۰٪ و فرمول‌ها ۷۰٪ اندازهٔ طبیعی
 *  ۳) با لمس، شیء در پیش‌نمایش یک‌باره کوچک می‌شد
 *  ۴) ستونِ بارم در چاپ محو می‌شد
 *  ۵) جابه‌جایی آزاد نبود و شیءِ دیگر جلویش را می‌گرفت
 *  ۶) با وجودِ سؤال، اول سازندهٔ قدیمیِ WebView باز می‌شد
 */
class V92_0PrintPolishTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val cards by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintQuestionCards.kt").readText()
    }

    @Test
    fun `the formula selection range is carried to the formula editor`() {
        // کارتِ بومی بازهٔ شروع/پایان را رد می‌کند
        assertTrue("onOpenFormula = { _, selStart, selEnd ->" in cards)
        assertTrue("onOpenTool(FigureToolRequest.FORMULA, selStart, selEnd)" in cards)
        // دیالوگ بازه را جدا نگه می‌دارد و به FormulaHostDialog می‌دهد
        assertTrue("var formulaEnd by remember { mutableIntStateOf(-1) }" in dialog)
        assertTrue("formulaEnd = endCursor" in dialog)
        assertTrue("val endCaret = if (formulaEnd in caret..text.length) formulaEnd else caret" in dialog)
        assertTrue("selectionEnd = endCaret," in dialog)
    }

    @Test
    fun `non-formula objects shrink to 30 percent and formulas stay at 70 percent`() {
        assertTrue("zoom:.30 !important" in asset)
        assertTrue("max-width:30% !important" in asset)
        // جبرانِ دکمه‌های × و ✎ با ۱/۰٫۳۰
        assertTrue("transform:scale(3.3333) !important" in asset)
        assertTrue("transform: scale(3.3333) !important" in asset)
        // فرمول (اتم) دست‌نخورده در ۷۰٪
        assertTrue(".qmf-surface .qmf-atom{position:relative;font-size:.7em;line-height:1.2;cursor:pointer;user-select:none}" in asset)
    }

    @Test
    fun `pure move no longer rewrites the height so touching cannot shrink the object`() {
        assertTrue("storedH: Number.isFinite(+layout.h)," in asset)
        assertTrue("if (d.resizing || d.storedH) {" in asset)
        assertTrue("d.fig.style.removeProperty('height');" in asset)
    }

    @Test
    fun `the score column is protected in print from covering white boxes`() {
        assertTrue("<style id=\"qmf-print-box-clean-v92\">" in asset)
        assertTrue("background:transparent !important;" in asset)
        assertTrue(".interactive-figure.fig-free{" in asset)
        assertTrue("max-width:100% !important;" in asset)
    }

    @Test
    fun `the touched object always rises to the top layer`() {
        assertTrue("z: z0" in asset)
        assertTrue("fig.style.zIndex = String(z0);" in asset)
        assertTrue("fig.style.zIndex = String(zz);" in asset)
        assertTrue("setFigLayout(qid, idx, { z: zz })" in asset)
    }

    @Test
    fun `the native card list opens first even when questions exist`() {
        // سطحِ مات روی WebView تا آماده‌شدنِ فهرستِ بومی
        assertTrue("var cardsLoaded by remember { mutableStateOf(false) }" in dialog)
        assertTrue("background(Color(0xFFEEF2F7))" in dialog)
        // حالتِ خالی فقط بعد از بارگذاریِ واقعیِ فهرست
        assertTrue("cardDetails.isEmpty() && !previewOpen && !loading && cardsLoaded" in dialog)
        assertTrue("cardsLoaded = true" in dialog)
    }
}
