package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V137.5 — کارایی پیش‌نمایش، نوار ابزار آیکونی تخته، دستگیرهٔ جابه‌جایی، برچسب ۰/۱۸۰ نقاله، اعداد فارسی. */
class V137_5_PerfIconsDigitsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `pagination waves only re-run when content height changed and drag is rAF-throttled`() {
        val pgs = source("app/src/main/assets/print/web/pgs_engine.js")
        assertTrue("function contentSignature()" in pgs)
        assertTrue("function paginateIfChanged()" in pgs)
        assertTrue("setTimeout(paginateIfChanged, 160);" in pgs)
        assertFalse("setTimeout(function () { if (!pgsState.paginating) paginate(); }, 160);" in pgs)
        assertTrue("if (over && trimSepPadToFit(row, ctx.sheet)) over = overflows(ctx.sheet);" in pgs)
        assertTrue("if (key === lastThumbsKey && box.children.length === list.length) return;" in pgs)
        val js = source("app/src/main/assets/print/web/mainscript.js")
        assertTrue("dragRaf = requestAnimationFrame(function () {" in js)
        assertTrue("function dragStep(e) {" in js)
        assertTrue("function clampToParent(el, x, y, w, h, cache) {" in js)
        assertTrue("clampToParent(drag.fig, x, y, w, h, drag)" in js)
    }

    @Test
    fun `whiteboard toolbar uses icons, grip handle moves instruments and teacher button is add-to-question`() {
        val board = source("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("internal fun BoardIconChip(" in board)
        assertTrue("PEN -> Icons.Outlined.Draw" in board)
        assertTrue("RULER -> Icons.Outlined.Straighten" in board)
        assertTrue("COMPASS -> Icons.Outlined.Architecture" in board)
        assertTrue("internal fun instrumentGrip(st: InstrumentState, density: Float): Offset" in board)
        assertTrue("(p - instrumentGrip(inst, density)).getDistance() < 30f * density -> \"move\"" in board)
        assertFalse("instrumentBodyHit(inst, p, density) -> \"move\"" in board)
        assertTrue("val onBase = a == 0 || a == 180" in board)
        assertTrue("teacherMode: Boolean = false" in board)
        assertTrue("\"افزودن تصویر تخته به سؤال\"" in board)
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("teacherMode = true" in builder)
    }

    @Test
    fun `persian digits toggle reaches canvas, svg renderers and web figures`() {
        assertTrue(File(root(), "app/src/main/java/ir/exam/app/core/figure/FigureDigits.kt").isFile)
        val prefs = source("app/src/main/java/ir/exam/app/core/ui/AppearancePreferences.kt")
        assertTrue("val persianDigits: Boolean = false" in prefs)
        assertTrue("suspend fun setPersianDigits(enabled: Boolean)" in prefs)
        assertTrue("FigureDigits.persian = appearance.persianDigits" in source("app/src/main/java/ir/exam/app/MainActivity.kt"))
        assertTrue("Switch(settings.persianDigits, viewModel::setPersianDigits)" in source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt"))
        listOf(
            "AxisSvgRenderer", "ChartSvgRenderer", "ChartSvgRendererStage2", "ChartSvgRendererStage3",
            "FigureSvgRenderer", "PeriodicSvgRenderer", "TableSvgRenderer"
        ).forEach { assertTrue(it, "FigureDigits.apply(s)" in source("app/src/main/java/ir/exam/app/core/figure/$it.kt")) }
        val board = source("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("FigureDigits.apply((i / 10).toString())" in board)
        assertTrue("put(\"persianDigits\", ir.exam.app.core.figure.FigureDigits.persian)" in source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt"))
        assertTrue("window.__figPersianDigits = !!data.persianDigits" in source("app/src/main/assets/print/web/webhost.js"))
        assertTrue("function faDigitsInFigHtml(html)" in source("app/src/main/assets/print/web/mainscript.js"))
    }
}
