package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V137.4 — شیء داخل کادر ۸دستگیره برای همهٔ انواع؛ جابه‌جایی کارت‌ها بدون لگ. */
class V137_4_PreviewBoxCardsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `natural size is measured per object kind and atlas images refit after load`() {
        val js = source("app/src/main/assets/print/web/mainscript.js")
        assertTrue("function figColumnWidth(fig)" in js)
        assertTrue("img.naturalWidth > 0 && img.naturalHeight > 0" in js)
        assertTrue("fig.querySelectorAll('.qmf-fig > svg')" in js)
        assertTrue("function watchFigImages(fig)" in js)
        assertTrue("fig.dataset.natPending = '1'" in js)
        assertFalse("const w = inner.offsetWidth, h = inner.offsetHeight;" in js)
    }

    @Test
    fun `pagination keeps nested tables and scaled object is not re-centred`() {
        val pgs = source("app/src/main/assets/print/web/pgs_engine.js")
        val css = source("app/src/main/assets/print/web/webhost.css")
        assertTrue("node.querySelectorAll(':scope > tbody > tr')" in pgs)
        assertFalse("node.querySelectorAll('tbody > tr')" in pgs)
        assertTrue("#pgsViewer #previewArea .interactive-figure.fig-scaled[style] .qmf-fig{margin:0 !important;}" in css)
        assertTrue(".interactive-figure.fig-measure .qmf-fig img.an-svg{width:100% !important" in css)
        assertTrue(".interactive-figure.fig-scaled[style] .qmf-fig .an-frame img.an-svg" in css)
    }

    @Test
    fun `management cards drag detector is stable and stack offset is drawn not laid out`() {
        val cards = source("app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt")
        assertTrue(".pointerInput(Unit) {" in cards)
        assertFalse(".pointerInput(activeIndex, settling)" in cards)
        assertTrue("val stackTopPx by animateFloatAsState(" in cards)
        assertTrue("translationY = stackTopPx + when {" in cards)
        assertFalse("animateDpAsState" in cards)
    }
}
