package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V123 — خطِ کادرِ متنِ سؤال در پیش‌نمایش قابل‌جابه‌جایی باشد (خطِ آبیِ واضح
 * با دستگیرهٔ مرکزی؛ درگ = کم/زیاد فضای همان سؤال) و کادرِ چیدمانِ آزادِ
 * تصاویر (کادرِ خط‌چینِ جایِ اصلی) از پیش‌نمایش حذف شده باشد.
 */
class V123_SepGripFreeSlotTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val renderer by lazy { File(root(), "app/src/main/assets/print/exam_print_renderer_legacy.html").readText() }
    private val dialog by lazy { File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText() }

    @Test
    fun `separator grip is a clearly visible line with a centered handle and 28px touch target`() {
        // هدفِ لمس ۲۸px (۱۴px بالاتر و پایین‌تر از خط) و درگِ عمودی
        assertTrue("هدفِ لمسِ خط نیست", "bottom:-14px;height:28px" in renderer)
        assertTrue("cursor درگ عمودی نیست", "cursor:ns-resize" in renderer)
        assertTrue("خطِ آبیِ واضح نیست", "rgba(37,99,235,.55)" in renderer)
        assertTrue("دستگیرهٔ مرکزیِ کپسولی نیست", "width:34px;height:14px;border-radius:999px;background:#2563eb" in renderer)
        assertTrue("نمادِ دستگیره نیست", ".question-sep-drag::after{content:\"≡\"" in renderer)
        assertTrue("حالتِ in-drag خط روشن‌تر نمی‌شود", ".question-sep-drag.is-dragging::before{background:rgba(37,99,235,.95)" in renderer)
        // فقط در حالتِ پیش‌نمایش ظاهر می‌شود
        assertTrue("خط فقط در پیش‌نمایش نیست", ".preview-open .question-sep-drag{display:flex}" in renderer)
    }

    @Test
    fun `separator drag still changes sepExtraPx, repaginates and persists`() {
        assertTrue("درگِ separator نیست", "kind:'separator'" in renderer)
        assertTrue("محاسبهٔ sepExtraPx با مقیاس برگه نیست", "activeDrag.question.sepExtraPx = Math.round(clamp(activeDrag.base + (event.clientY - activeDrag.startY) / k, 0, 1500))" in renderer)
        assertTrue("ارتفاعِ pad زنده به‌روز نمی‌شود", "if (activeDrag.pad) activeDrag.pad.style.height = activeDrag.question.sepExtraPx + 'px'" in renderer)
        assertTrue("پس از درگ صفحه‌بندی دوباره انجام نمی‌شود", "if (kind === 'separator' || kind === 'move' || kind === 'resize')" in renderer)
        assertTrue("درگ روی ادامهٔ سطرِ بریده‌شده سیم‌کشی نمی‌شود", "wireSeparator(cloneGrip, pad, q)" in renderer)
        assertTrue("sepExtraPx به snapshot نمی‌رسد", "sepExtraPx:question.sepExtraPx" in renderer)
        // persistence chain در میزبانِ بومی دست‌نخورده
        assertTrue("snapshot به میزبان نمی‌رسد", "onFigLayouts?.invoke(json)" in dialog)
        assertTrue("میزبان snapshot را نمی‌خواند", "ExamPrintRenderer.layoutSnapshot" in dialog)
    }

    @Test
    fun `one-time hint about the draggable line is shown on preview open`() {
        assertTrue("پرچمِ یک‌بارهٔ راهنما نیست", "var sepHintDone = false" in renderer)
        assertTrue("راهنما در showPreview صدا نمی‌شود", "if (!sepHintDone) { sepHintDone = true; notify(" in renderer)
        assertTrue("متنِ راهنمای فارسی نیست", "فاصلهٔ هر سؤال را با کشیدنِ خطِ آبیِ پایینِ کادرش کم یا زیاد کنید" in renderer)
        // راهنما فقط با toastِ محوشونده است، نه متنِ ثابت روی صفحه
        assertFalse("متنِ راهنمای ثابت برگشته است", "sep-hint-banner" in renderer)
    }

    @Test
    fun `free image slot frame is removed from the preview`() {
        assertFalse("کادرِ خط‌چینِ جایِ اصلی برگشته است", "border:1px dashed #b9c4d3" in renderer)
        assertFalse("قاعدهٔ نمایشِ free-slot در پیش‌نمایش برگشته است", ".preview-open .figure-slot.free-slot" in renderer)
        // مکانیزمِ رزروِ جا در جریانِ متن (بدونِ کادر) دست‌نخورده
        assertTrue("slot آزاد نامرئی نمی‌ماند", ".figure-slot.free-slot{opacity:0;pointer-events:none}" in renderer)
        assertTrue("slot در درگِ آزاد علامت‌گذاری نمی‌شود", "slot.classList.add('free-slot')" in renderer)
        assertTrue("slot در چاپ هم نامرئی است", ".figure-slot.free-slot{border:0!important;opacity:0!important}" in renderer)
    }

    @Test
    fun `grip and frame stay hidden in print mode`() {
        assertTrue("خط در حالتِ چاپ نمایش داده می‌شود", ".exam-print-mode .figure-handle,.exam-print-mode .question-sep-drag{display:none!important}" in renderer)
        assertTrue("خط در @media print پنهان نمی‌شود", ".figure-handle,.question-sep-drag{display:none!important}" in renderer)
    }
}
