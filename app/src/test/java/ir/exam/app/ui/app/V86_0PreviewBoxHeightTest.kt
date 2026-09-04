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
        assertTrue(".pwo-box{width:98vw;height:auto;max-height:100%;min-height:320px}" in asset)
        assertTrue(".pwo-box{width:98vw;height:94vh}" !in asset)
        assertTrue(".pwo-box{height:96vh !important}" !in asset)
    }

    @Test
    fun `the body cannot be squeezed to nothing by its flex parent`() {
        assertTrue(".pwo-body{flex:1 1 auto;min-height:0;" in asset)
        assertTrue("min-height:320px" in asset)
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
        assertTrue("Math.ceil(natH * k)" in asset)
    }
}
