package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V99.1 — چهار ایرادِ گزارش‌شدهٔ جریانِ چاپ:
 * ۱) آیکن پرینترِ کارت‌های آزمون در «چاپ آزمون» دیگر پنجرهٔ آزمون‌ساز
 *    چاپی را باز نمی‌کند؛ نسخهٔ دانش‌آموز/پاسخ‌نامه انتخاب می‌شود و چاپ
 *    مستقیم انجام می‌گردد.
 * ۲) چاپِ مستقیم (دانش‌آموز/پاسخ‌نامه) پنجرهٔ کارت‌های سؤال را نمایش
 *    نمی‌دهد؛ فقط برگهٔ خالصِ A4 و پنجرهٔ چاپِ اندروید.
 * ۳) جابه‌جاییِ اشیا در پیش‌نمایش، خطوطِ کادرِ سؤال را جابه‌جا نمی‌کند:
 *    فضای اصلیِ شیء رزرو می‌شود (slot) و سلول در حینِ درگ رشد نمی‌کند.
 * ۴) جابه‌جاییِ خطوطِ کادرِ متنِ سؤال فقط با انتخابِ صریحِ کاربر
 *    (تعداد/فاصلهٔ خطوط، خطِ جداکننده، ویرایشِ سؤال، یا بازگرداندنِ شیء
 *    به جریان) رخ می‌دهد.
 */
class V99_1PrintFlowLineStabilityTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val center by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt").readText()
    }

    // ---------- ۱) پرینتر = چاپِ مستقیم، نه آزمون‌ساز چاپی ----------

    @Test
    fun `printer icon on server exam card opens direct print`() {
        assertTrue("printTarget = PrintTarget.ServerExam(exam.id)" in center)
        assertTrue("fun startPrint(target: PrintTarget, mode: String)" in center)
        assertTrue("private sealed class PrintTarget" in center)
        assertTrue("is PrintTarget.ServerExam -> portability.printableExam(" in center)
        // آیکن پرینتر دیگر به openBuilder30 وصل نیست:
        val printerIdx = center.indexOf("printTarget = PrintTarget.ServerExam(exam.id)")
        assertTrue(printerIdx > 0)
        assertTrue(!("openBuilder30(exam.id)" in center.substring(printerIdx, printerIdx + 400)))
    }

    @Test
    fun `printer icon on local exam card opens direct print`() {
        assertTrue("printTarget = PrintTarget.LocalExam(rec)" in center)
        assertTrue("is PrintTarget.LocalExam -> {" in center)
        // ویرایش (مداد) دست‌نخورده می‌ماند:
        assertTrue("onOpenLocalPrintExam(rec.id)" in center)
        assertTrue("openBuilder30(exam.id)" in center)
    }

    @Test
    fun `print menu offers student and answer key versions`() {
        assertTrue("startPrint(target, \"student\")" in center)
        assertTrue("startPrint(target, \"teacher\")" in center)
        assertTrue("🖨 چاپ آزمون (دانش‌آموز)" in center)
        assertTrue("✅ چاپ با کلید (پاسخ‌نامه)" in center)
        // حالتِ پنجرهٔ چاپ به دیالوگ می‌رسد:
        assertTrue("initialPrintMode = printModeFor" in center)
        // مسیرِ ویرایش حالت را صریح null می‌کند:
        assertTrue("printModeFor = null" in center)
    }

    // ---------- ۲) چاپِ مستقیم بدون پنجرهٔ کارت‌ها ----------

    @Test
    fun `direct print hides the question cards window`() {
        // فهرستِ بومیِ کارت‌ها فقط در حالتِ عادی (بدون چاپِ مستقیم):
        assertTrue("cardDetails.isNotEmpty() && !previewOpen && initialPrintMode == null" in dialog)
        // سربرگ و کنترل‌های پنجرهٔ آزمون‌ساز هم در چاپِ مستقیم پنهان‌اند (V97/V98.9):
        assertTrue("initialPrintMode == null" in dialog)
    }

    @Test
    fun `direct print runs the print pipeline without the preview overlay`() {
        // پنجرهٔ پیش‌نمایشِ HTML فقط با initialPreview باز می‌شود (آیکن چشم)؛
        // در چاپِ مستقیم باز نمی‌شود، چون overlay هنگامِ چاپ پنهان است و
        // محتوای چاپ داخلش منتقل شده و خروجی خالی می‌ماند. برگهٔ خالصِ A4
        // را خودِ پنجرهٔ چاپِ اندروید نمایش می‌دهد.
        assertTrue("if (initialPreview) {" in dialog)
        assertTrue("__qmfShowPreview" in dialog)
        assertTrue("printStudent()" in dialog)
        assertTrue("printTeacher()" in dialog)
        assertTrue("previewOpen = true" in dialog)
    }

    // ---------- ۳) جابه‌جاییِ اشیا، خطوطِ کادر را نمی‌زند ----------

    @Test
    fun `floating figure reserves its slot so lines do not jump`() {
        // رزروِ slot هنگامِ شناور شدن (درگ و کلیدِ دستی):
        assertTrue("function syncFigFlowSlot(qId, figIndex, makingFree) {" in asset)
        assertTrue("syncFigFlowSlot(qid0, idx0, true);" in asset)
        assertTrue("syncFigFlowSlot(qId, figIndex, !cur.free)" in asset)
        // slot در رندر ساخته می‌شود ⇒ چاپ با پیش‌نمایش یکی می‌ماند:
        assertTrue("fig-flow-slot" in asset)
        assertTrue(".fig-flow-slot { display:block;" in asset)
        assertTrue("fig-flow-slot\" style=\"width:" in asset)
    }

    @Test
    fun `the question cell no longer grows during drag`() {
        // رشدِ minHeight (همان چیزی که خطوط را در هر فریم جابه‌جا می‌کرد) حذف شد:
        assertTrue("parent.style.minHeight = need + 'px';" !in asset)
        assertTrue("dataset.figMinHeight" !in asset)
        // کفِ پایینِ سلول همچنان صفر است (شیء از بالای سؤال بیرون نمی‌رود):
        assertTrue("y = Math.max(0, y);" in asset)
    }

    @Test
    fun `unfloating the figure removes the slot explicitly`() {
        // فقط انتخابِ صریحِ کاربر (بازگشت به جریان) جایِ خالی را حذف می‌کند:
        assertTrue("} else if (!makingFree && cur.slot) {" in asset)
        assertTrue("setFigLayout(qId, figIndex, { slot: null });" in asset)
        // و رندرِ شیءِ درجریانِ عادی بدونِ slot:
        assertTrue("if (_fl991.free && _fl991.slot && Number.isFinite(+( _fl991.slot.h )) && +(_fl991.slot.h) > 0) {" in asset)
    }
}
