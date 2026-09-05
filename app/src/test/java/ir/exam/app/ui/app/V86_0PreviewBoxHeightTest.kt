package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V86.0 — کادر پیش‌نمایش نباید به واحد vh تکیه کند.
 *
 * تشخیص روی دستگاه: bodyW:344 (=98vw، سالم) ولی bodyH:12 و bodyScrollH:241.
 * عرض از vw درست درآمد و ارتفاع از vh صفر شد، پس کادر flex جمع شد و برگهٔ
 * ۲۲۳ پیکسلی داخل نواری ۱۲ پیکسلی رفت.
 */
class V86_0PreviewBoxHeightTest {

    /* تست‌ها با دایرکتوری کاری `app/` اجرا می‌شوند، نه ریشهٔ ریپو. */
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset: String by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `the box height is pinned in pixels not viewport units`() {
        assertTrue("qmfFixPreviewBox" in asset)
        // ارتفاع با عدد پیکسلی و اولویت important بسته می‌شود
        assertTrue("box.style.setProperty('height', target + 'px', 'important')" in asset)
        assertTrue("bd.style.setProperty('flex', '0 0 auto', 'important')" in asset)
    }

    @Test
    fun `the height falls back through every source before giving up`() {
        assertTrue("ov.clientHeight" in asset)
        assertTrue("window.innerHeight" in asset)
        assertTrue("document.documentElement.clientHeight" in asset)
        assertTrue("screen.height" in asset)
        // هیچ‌گاه زیر یک کف معقول نرود
        assertTrue("Math.max(320" in asset)
    }

    @Test
    fun `the mobile rule no longer sets a vh height on the box`() {
        assertTrue(".pwo-box{width:100%;height:100%;max-height:100%;min-height:0}" in asset)
        assertTrue(".pwo-box{width:98vw;height:94vh}" !in asset)
        assertTrue(".pwo-box{height:96vh !important}" !in asset)
    }

    @Test
    fun `the body cannot be squeezed to nothing by its flex parent`() {
        assertTrue(".pwo-body{flex:1 1 auto;min-height:0;" in asset)
    }

    @Test
    fun `the fit runs again while the viewport is still settling`() {
        assertTrue("setInterval" in asset)
        assertTrue("bd.offsetHeight > 100" in asset)
        // نشت نکند
        assertTrue("clearInterval(ov.__qmfRetry)" in asset)
        assertTrue("removeEventListener('resize', window.qmfFixPreviewBox)" in asset)
    }

    @Test
    fun `the diagnostic reports the numbers that decide this bug`() {
        assertTrue("out.innerH" in asset)
        assertTrue("out.docClientH" in asset)
        assertTrue("out.ovH" in asset)
        assertTrue("out.boxH" in asset)
    }

    @Test
    fun `the scale fix from V85 is still in place`() {
        assertTrue("qmf-pv-wrap" in asset)
        assertTrue("transform-origin" in asset)
        assertTrue("Math.ceil(natH * kz)" in asset)
    }

    @Test
    fun `the sheet is measured at its natural width not the narrow parent`() {
        // printContent عرض صریح ندارد و عرض والد باریک را می‌گیرد؛
        // اگر همان را مقیاس کنیم برگه دو بار کوچک می‌شود.
        assertTrue("pc.style.setProperty('width', natural + 'px', 'important')" in asset)
        assertTrue("Math.max(paperW, pc.scrollWidth || 0, pc.offsetWidth || 0) || 794" in asset)
        // ۷۹۴ فقط پشتیبانِ نبودِ اندازه است، نه کفِ اجباری
        assertTrue("Math.max(pc.scrollWidth || 0, pc.offsetWidth || 0, 794)" !in asset)
    }

    @Test
    fun `the forced width is dropped when the window closes`() {
        assertTrue("pc.style.removeProperty('width')" in asset)
        assertTrue("out.forcedW" in asset)
    }

    @Test
    fun `the preview window covers the whole screen`() {
        // کادر تا لبه‌های دیدگاه: بدون حاشیه، گوشهٔ گرد یا سایه
        assertTrue(".pwo-box{background:#fff;border-radius:0;width:100%;height:100%" in asset)
        assertTrue("box-shadow:none;overflow:hidden}" in asset)
        assertTrue("padding:0;direction:rtl}" in asset)
        // پس‌زمینهٔ نیمه‌شفافِ همین اورلی دیگر دیده نمی‌شود، پس رنگِ کاغذ بگیرد.
        // (قانونِ qtype-overlay همان رنگ را دارد و باید دست‌نخورده بماند.)
        assertTrue("#previewWinOverlay{position:fixed;inset:0;z-index:125000;background:#eef2f7;" in asset)
    }

    @Test
    fun `the box takes the full viewport height not a fraction`() {
        assertTrue("var target = Math.max(320, h);" in asset)
        assertTrue("Math.round(h * 0.94)" !in asset)
    }

    @Test
    fun `the header clears the device notch`() {
        assertTrue("env(safe-area-inset-top,0px)" in asset)
    }

    @Test
    fun `the paper width comes from a real ruler not a clamped measurement`() {
        // .live-preview عرض 200mm دارد؛ scrollWidth زیر والدِ باریک فشرده است
        assertTrue("width:210mm;height:0" in asset)
        assertTrue("paperW = ruler.offsetWidth" in asset)
        assertTrue("Math.max(paperW, pc.scrollWidth || 0, pc.offsetWidth || 0) || 794" in asset)
        assertTrue("out.paperW" in asset)
    }

    @Test
    fun `the user can zoom the sheet on top of the automatic fit`() {
        assertTrue("window.qmfSetPreviewZoom = function (z)" in asset)
        // زوم روی مقیاسِ خودکار ضرب می‌شود تا جا-شدن از دست نرود
        assertTrue("var kz = k * (window.__qmfPvZoom || 1)" in asset)
        assertTrue("z = Math.min(4, Math.max(1, z || 1))" in asset)
        assertTrue("qmfInstallPinchZoom" in asset)
        assertTrue("e.touches.length === 2" in asset)
    }

    @Test
    fun `the zoom controls are reachable and reset on close`() {
        assertTrue("pwo-zoom-val" in asset)
        assertTrue("qmfSetPreviewZoom(1)" in asset)
        assertTrue("min-width:34px;min-height:34px" in asset)
        assertTrue("window.__qmfPvZoom = 1;" in asset)
    }

    @Test
    fun `the sheet inside the window is a full A4 page`() {
        // قانونِ موبایل عرضِ برگه را به 100% می‌شکند؛ داخلِ پنجره برگردانده می‌شود
        assertTrue("#previewWinOverlay #printContent.live-preview{" in asset)
        assertTrue("width:210mm !important;" in asset)
        assertTrue("min-height:297mm !important;" in asset)
        assertTrue("padding:5mm !important;" in asset)
    }

    @Test
    fun `the print rules keep the printable area untouched`() {
        // @media print همچنان ناحیهٔ چاپ است: 210 منهای دو حاشیهٔ 5mm
        assertTrue("width:200mm !important; min-height:287mm !important;" in asset)
        assertTrue("@page { size: A4; margin: 5mm; }" in asset)
    }
}
