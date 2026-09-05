package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V86.5 — اسکرولِ مشروط، قفلِ اسکرول هنگام جابه‌جایی، و چیدمانِ آزادِ اشیاء.
 */
class V86_5FigureLayoutTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset: String by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `the preview only scrolls sideways once it is zoomed`() {
        assertTrue("overflow-y:auto;overflow-x:hidden" in asset)
        assertTrue(".pwo-body.qmf-zoomed{overflow-x:auto}" in asset)
        assertTrue("pb.classList.toggle('qmf-zoomed', z > 1.001)" in asset)
    }

    @Test
    fun `dragging a figure does not scroll the page underneath it`() {
        assertTrue("sc.style.overflow = 'hidden'; window.__qmfDragLock = sc;" in asset)
        assertTrue("function qmfReleaseScrollLock()" in asset)
        // قفل باید در هر پایانی آزاد شود، وگرنه صفحه برای همیشه بی‌حرکت می‌ماند
        assertTrue("window.addEventListener('pointerup', qmfReleaseScrollLock)" in asset)
        assertTrue("window.addEventListener('blur', qmfReleaseScrollLock)" in asset)
        assertTrue("area.addEventListener('pointercancel', function (e) { qmfReleaseScrollLock(); endDrag(e); })" in asset)
    }

    @Test
    fun `pinch zoom stands down while a figure is being dragged`() {
        assertTrue("e.touches.length === 2 && !window.__qmfDragLock" in asset)
    }

    @Test
    fun `a floating figure is positioned absolutely so it can overlap`() {
        assertTrue("if (l.free) {" in asset)
        assertTrue("`position:absolute`, `right:\${x}px`, `left:auto`, `top:\${yEff}px`" in asset)
        assertTrue("`z-index:\${zi}`" in asset)
    }

    @Test
    fun `the flowing layout is unchanged for every existing exam`() {
        // بدونِ free دقیقاً همان clamp قبلی
        assertTrue("margin-right:clamp(0px, \${x}px, calc(100% - \${wEff}px - 18px))" in asset)
        assertTrue("width:min(\${w}px, calc(100% - 18px))" in asset)
    }

    @Test
    fun `dragging a floating figure writes a real offset not a clamped margin`() {
        assertTrue("if (drag.fig.classList.contains('fig-free'))" in asset)
        assertTrue("drag.fig.style.removeProperty('margin-right')" in asset)
        assertTrue("drag.fig.style.right = c.x + 'px'" in asset)
    }

    @Test
    fun `the user can switch a figure to floating and restack it`() {
        assertTrue("function qmfToggleFigFree(qId, figIndex)" in asset)
        assertTrue("function qmfFigLayer(qId, figIndex, dir)" in asset)
        assertTrue("function qmfNextFigZ(q)" in asset)
        // هنگام شناور شدن باید از جای فعلی شروع کند تا شیء نپرد
        assertTrue("patch.x = Math.max(0, Math.round(hr.right - r.right))" in asset)
    }

    @Test
    fun `the layout survives into the printed sheet`() {
        assertTrue("style=\"\${figLayoutStyle(q, figIndex)}\"" in asset)
        // ولی ابزارهای ویرایش چاپ نشوند
        assertTrue("@media print { .fig-tools, .fig-move-hint { display:none !important; } }" in asset)
    }
}
